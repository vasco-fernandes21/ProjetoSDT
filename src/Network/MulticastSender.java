package Network;

import RMISystem.ListInterface;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;
    private static final int REQUIRED_ACKS = 2;

    private final ListInterface listManager;
    private List<String> previousDocs;

    public MulticastSender(ListInterface listManager) {
        this.listManager = listManager;
        this.previousDocs = null;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                // Obter a lista atual de documentos
                List<String> docs = listManager.allMsgs();

                // Verificar se houve mudanças na lista de documentos
                boolean hasChanges = previousDocs == null || !previousDocs.equals(docs);

                if (hasChanges && !docs.isEmpty()) {
                    // Enviar heartbeat de sincronização
                    sendSyncMessage(socket, group, docs);

                    // Esperar por ACKs
                    if (waitForAcks()) {
                        // Enviar mensagem de commit após receber ACKs suficientes
                        sendCommitMessage(socket, group);

                        // Clonar a lista de documentos após o commit
                        listManager.addClone();
                    } else {
                        System.out.println("Não foi possível receber ACKs suficientes.");
                    }

                    // Atualizar o estado anterior da lista de documentos
                    previousDocs = new ArrayList<>(docs);
                } else {
                    System.out.println("Nenhuma alteração detectada ou lista vazia, não enviando mensagens.");
                }

                // Espera 5 segundos antes de verificar novamente
                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendSyncMessage(MulticastSocket socket, InetAddress group, List<String> docs) throws IOException {
        String docsString = docs.isEmpty() ? "none" : String.join(",", docs);
        String heartbeatMessage = "HEARTBEAT:sync:" + docsString;
        byte[] heartbeatBuffer = heartbeatMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(heartbeatBuffer, heartbeatBuffer.length, group, PORT);
        socket.send(packet);
        System.out.println("Heartbeat enviado: " + heartbeatMessage);
    }

    private boolean waitForAcks() {
        Set<String> receivedAcks = new HashSet<>();

        try (DatagramSocket ackSocket = new DatagramSocket(ACK_PORT)) {
            ackSocket.setSoTimeout(2000);
            System.out.println("Aguardando ACKs...");

            while (receivedAcks.size() < REQUIRED_ACKS) {
                byte[] ackBuffer = new byte[256];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                try {
                    ackSocket.receive(ackPacket);
                    String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);
                    System.out.println("Mensagem de ACK recebida: " + ackMessage); // Log adicional
                    if (ackMessage.startsWith("ACK:")) {
                        String ackUUID = ackMessage.split(":")[1];
                        receivedAcks.add(ackUUID);
                        System.out.println("ACK processado: " + ackMessage);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout ao esperar ACKs.");
                    break;
                }
            }

            return receivedAcks.size() >= REQUIRED_ACKS;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendCommitMessage(MulticastSocket socket, InetAddress group) throws IOException {
        String commitMessage = "HEARTBEAT:commit";
        byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, PORT);
        socket.send(commitPacket);
        System.out.println("Commit enviado: " + commitMessage);
    }
}