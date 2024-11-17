package Network;

import RMISystem.ListInterface;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;
    private static final int REQUIRED_ACKS = 3;
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
                    // Enviar heartbeat com a lista de documentos
                    String heartbeatMessage = "HEARTBEAT:sync:" + String.join(",", docs);
                    byte[] heartbeatBuffer = heartbeatMessage.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(heartbeatBuffer, heartbeatBuffer.length, group, PORT);
                    socket.send(packet);
                    System.out.println("Heartbeat enviado: " + heartbeatMessage);

                    // Receber ACKs
                    try (DatagramSocket ackSocket = new DatagramSocket(ACK_PORT)) {
                        Set<String> receivedAcks = new HashSet<>();
                        while (receivedAcks.size() < REQUIRED_ACKS) {
                            byte[] ackBuffer = new byte[256];
                            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                            ackSocket.receive(ackPacket);
                            String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);

                            // Verifique se o ACK está no formato correto e adicione o UUID
                            if (ackMessage.startsWith("ACK:")) {
                                String ackUUID = ackMessage.split(":")[1];
                                receivedAcks.add(ackUUID);
                                System.out.println("ACK recebido: " + ackMessage);
                            }
                        }

                        // Enviar mensagem de commit após receber ACKs suficientes
                        String commitMessage = "HEARTBEAT:commit";
                        byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, PORT);
                        socket.send(commitPacket);
                        System.out.println("Commit enviado: " + commitMessage);

                        // Clonar a lista de documentos após commits
                        listManager.addClone();
                    }
                } else {
                    System.out.println("Nenhuma alteração detectada ou lista vazia, não enviando heartbeat ou commit.");
                }

                // Atualizar o estado anterior da lista de documentos
                previousDocs = new ArrayList<>(docs);

                // Espera 5 segundos antes de verificar novamente
                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}