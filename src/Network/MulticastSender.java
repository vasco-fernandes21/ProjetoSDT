package Network;

import RMISystem.ListInterface;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;

    private final ListInterface listManager;
    private List<String> previousDocs;
    private final Set<String> receivedAcks;

    public MulticastSender(ListInterface listManager) {
        this.listManager = listManager;
        this.previousDocs = null;
        this.receivedAcks = new HashSet<>();
    }

    @Override
    public void run() {
        // Iniciar a thread AckProcessor
        AckProcessor ackProcessor = new AckProcessor(receivedAcks);
        ackProcessor.start();

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

                        // Confirmar o commit no ListManager
                        listManager.commit();

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
        synchronized (receivedAcks) {
            try {
                receivedAcks.wait(2000); // Espera por ACKs ou timeout
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return receivedAcks.size() >= 1; // Espera por pelo menos 1 ACK
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