package Network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AckProcessor extends Thread {
    private final int ackPort;
    private final Map<String, Integer> nodeMissCounts;
    private final Set<String> activeNodes;
    private final String leaderUuid;

    // Estruturas para rastrear ACKs por requestId
    private final ConcurrentHashMap<String, Set<String>> heartbeatAcks = new ConcurrentHashMap<>();

    public AckProcessor(Set<String> activeNodes, int ackTimeoutMillis, int maxMissedHeartbeats, int ackPort, String leaderUuid) {
        this.activeNodes = activeNodes;
        this.ackPort = ackPort;
        this.leaderUuid = leaderUuid;
        this.nodeMissCounts = new ConcurrentHashMap<>();
        // Inicialização adicional se necessário
    }

    public synchronized void addNode(String nodeAddress) {
        activeNodes.add(nodeAddress);
    }

    public synchronized void removeNode(String nodeAddress) {
        activeNodes.remove(nodeAddress);
        nodeMissCounts.remove(nodeAddress);
    }

    public boolean hasAck(String nodeAddress) {
        return heartbeatAcks.values().stream().anyMatch(acks -> acks.contains(nodeAddress));
    }

    public int getMissedCount(String nodeAddress) {
        return nodeMissCounts.getOrDefault(nodeAddress, 0);
    }

    public synchronized void processAck(String requestId, String nodeId) {
        if (!nodeId.equals(leaderUuid)) { // Ignorar ACKs do líder
            heartbeatAcks.computeIfAbsent(requestId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
            System.out.println("ACK recebido de: " + nodeId + " para requestId: " + requestId);
            notifyAll(); // Notificar threads aguardando por ACKs
        }
    }

    public synchronized boolean allAcksReceived(String requestId) {
        Set<String> acks = heartbeatAcks.get(requestId);
        if (acks == null) {
            return false;
        }
        int majority = (activeNodes.size() / 2) + 1;
        return acks.size() >= majority;
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

    public void markFirstHeartbeatSent() {
        // Implementação, se necessário
    }

    private synchronized void handleMissedAcks() {
        for (String node : activeNodes) {
            if (!heartbeatAcks.values().stream().anyMatch(acks -> acks.contains(node))) {
                int misses = nodeMissCounts.getOrDefault(node, 0) + 1;
                nodeMissCounts.put(node, misses);
                System.out.println("Nó " + node + " falhou no heartbeat. Total de falhas: " + misses);

                if (misses >= 3) { // maxMissedHeartbeats fixado em 3
                    System.out.println("Nó " + node + " excedeu o limite de falhas e será removido.");
                    removeNode(node);
                }
            }
        }
    }

    public synchronized void checkForFailures() {
        handleMissedAcks();
    }

    public void clearAcks(String requestId) {
        heartbeatAcks.remove(requestId);
    }
}