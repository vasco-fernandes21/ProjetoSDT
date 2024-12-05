package Network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AckProcessor extends Thread {
    private final int ackPort;
    private final String leaderUuid;
    private MulticastSocket multicastSocket;
    private InetAddress group;

    // Estruturas para rastrear ACKs
    private final ConcurrentHashMap<String, Set<String>> heartbeatAcks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> ackCounts = new ConcurrentHashMap<>();
    private Set<String> maxAckSenders = new HashSet<>(); // Conjunto para rastrear os IDs dos nós que enviaram o maior número de ACKs
    private int maxAcks = 0; // Maximum number of ACKs received
    private int missedCounter = 0; // Counter for missed ACKs
    private static final int MISSED_THRESHOLD = 3; // Threshold for missed ACKs

    public AckProcessor(int ackPort, String leaderUuid, MulticastSocket multicastSocket, InetAddress group) {
        this.ackPort = ackPort;
        this.leaderUuid = leaderUuid;
        this.multicastSocket = multicastSocket;
        this.group = group;
    }

    public synchronized void processAck(String requestId, String nodeId) {
        if (!nodeId.equals(leaderUuid)) { // Ignorar ACKs do líder
            heartbeatAcks.computeIfAbsent(requestId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
            ackCounts.merge(requestId, 1, Integer::sum);
            System.out.println("ACK recebido de: " + nodeId + " para requestId: " + requestId);
            notifyAll(); // Notificar threads aguardando por ACKs
        }
    }

    public synchronized Set<String> getAcksForHeartbeat(String requestId) {
        return heartbeatAcks.getOrDefault(requestId, ConcurrentHashMap.newKeySet());
    }

    public synchronized int getAckCount(String requestId) {
        return ackCounts.getOrDefault(requestId, 0);
    }

    public synchronized void logAcks(String requestId) {
        Set<String> acks = getAcksForHeartbeat(requestId);
        System.out.println(acks.size() + " ACKs recebidos para requestId " + requestId + ": " + acks);
    }

    public synchronized boolean checkAckCounts(String requestId, Map<String, InetAddress> nodeAddressMap) {
        int currentAcks = getAckCount(requestId);
        Set<String> currentAckSenders = getAcksForHeartbeat(requestId);
        System.out.println("estás no checkAckCounts");
        System.out.println(currentAcks);
        System.out.println(maxAcks);
        if (currentAcks > maxAcks) {
            maxAcks = currentAcks;
            maxAckSenders = new HashSet<>(currentAckSenders); // Atualizar o conjunto de IDs dos nós que enviaram o maior número de ACKs
            missedCounter = 0; // Reset missed counter when new max is reached
            System.out.println("novo máximo");
        } else if (currentAcks < maxAcks) {
            missedCounter++;
            System.out.println("falhou uma");
            System.out.println("Número de ACKs recebidos: " + currentAcks + " (Máximo esperado: " + maxAcks + ")");
            if (missedCounter >= MISSED_THRESHOLD) {
                System.out.println("Número de ACKs recebidos é menor que o máximo esperado por " + MISSED_THRESHOLD + " vezes consecutivas.");
                // Take action, e.g., remove unresponsive nodes
                removeUnresponsiveNodes(currentAckSenders, nodeAddressMap);
                missedCounter = 0; // Reset missed counter after taking action
                return false;
            }
        } else {
            missedCounter = 0; // Reset missed counter if currentAcks equals maxAcks
        }
        return true;
    }

    private void removeUnresponsiveNodes(Set<String> currentAckSenders, Map<String, InetAddress> nodeAddressMap) {
        Set<String> unresponsiveNodes = new HashSet<>(maxAckSenders);
        unresponsiveNodes.removeAll(currentAckSenders); // Identificar nós que não enviaram ACKs
        System.out.println("Removendo nós não responsivos: " + unresponsiveNodes);
        // Remover nós não responsivos do grupo de multicast
        for (String nodeId : unresponsiveNodes) {
            InetAddress nodeAddress = nodeAddressMap.get(nodeId);
            if (nodeAddress != null) {
                try {
                    multicastSocket.leaveGroup(nodeAddress);
                    System.out.println("Nó removido do grupo de multicast: " + nodeId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Reset maxAcks and maxAckSenders after removing unresponsive nodes
        maxAcks = 0;
        maxAckSenders.clear();
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(ackPort)) {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (message.startsWith("ACK:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String nodeId = parts[1];
                        String requestId = parts[2];
                        processAck(requestId, nodeId);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Map<String, Set<String>> getHeartbeatAcks() {
        return heartbeatAcks;
    }

    public synchronized Map<String, Integer> getAckCounts() {
        return ackCounts;
    }

    public synchronized void resetAckCounts() {
        ackCounts.clear();
    }
}