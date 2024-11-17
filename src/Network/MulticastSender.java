package Network;

import RMISystem.ListManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;
    private static final int REQUIRED_ACKS = 3;
    private final ListManager listManager;

    public MulticastSender(ListManager listManager) {
        this.listManager = listManager;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                // Enviar heartbeats periodicamente
                List<String> docs = listManager.allMsgs();
                System.out.println("Lista de documentos no líder: " + docs);
                String heartbeatMessage = "HEARTBEAT:sync:" + (docs.isEmpty() ? "none" : String.join(",", docs));
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
                        receivedAcks.add(ackMessage.split(":")[1]);
                        System.out.println("ACK recebido: " + ackMessage);
                    }

                    // Enviar mensagem de commit após receber ACKs suficientes
                    String commitMessage = "HEARTBEAT:commit";
                    byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, PORT);
                    socket.send(commitPacket);
                    System.out.println("Commit enviado: " + commitMessage);
                }

                Thread.sleep(5000); // Espera 5 segundos antes de enviar o próximo heartbeat
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}