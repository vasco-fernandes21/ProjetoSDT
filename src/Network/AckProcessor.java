package Network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class AckProcessor extends Thread {
    private static final int ACK_PORT = 4448;
    private final Map<String, Integer> nodeMissCounts;
    private final Set<String> activeNodes;
    private final int maxMissedHeartbeats;
    private final int ackTimeoutMillis;

    public AckProcessor(Set<String> activeNodes, int ackTimeoutMillis, int maxMissedHeartbeats) {
        this.nodeMissCounts = new ConcurrentHashMap<>();
        this.activeNodes = activeNodes;
        this.ackTimeoutMillis = ackTimeoutMillis;
        this.maxMissedHeartbeats = maxMissedHeartbeats;
    }

    public synchronized void addNode(String nodeAddress) {
        activeNodes.add(nodeAddress);
        nodeMissCounts.putIfAbsent(nodeAddress, 0);
    }

    public synchronized void removeNode(String nodeAddress) {
        activeNodes.remove(nodeAddress);
        nodeMissCounts.remove(nodeAddress);
        System.out.println("Nó removido: " + nodeAddress);
    }

    public boolean hasAck(String nodeAddress) {
        return nodeMissCounts.getOrDefault(nodeAddress, maxMissedHeartbeats) < maxMissedHeartbeats;
    }

    public int getMissedCount(String nodeAddress) {
        return nodeMissCounts.getOrDefault(nodeAddress, 0);
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(ACK_PORT)) {
            socket.setSoTimeout(ackTimeoutMillis);

            while (true) {
                try {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String ackMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    processAck(ackMessage);

                } catch (SocketTimeoutException e) {
                    incrementMissCounts();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAck(String ackMessage) {
        if (ackMessage.startsWith("ACK:")) {
            String nodeId = ackMessage.split(":")[1];
            nodeMissCounts.put(nodeId, 0);
            System.out.println("ACK recebido de: " + nodeId);
        }
    }

    private void incrementMissCounts() {
        for (String node : activeNodes) {
            int misses = nodeMissCounts.getOrDefault(node, 0) + 1;
            nodeMissCounts.put(node, misses);
        }
    }

    public void checkForFailures() {
        Set<String> toRemove = new HashSet<>();
        synchronized (this) {
            for (String node : activeNodes) {
                if (!hasAck(node)) {
                    toRemove.add(node);
                    System.out.println("Nó marcado para remoção por falta de ACK: " + node + ". Missed ACKs: " + getMissedCount(node));
                }
            }
        }
        activeNodes.removeAll(toRemove);
        for (String removedNode : toRemove) {
            System.out.println("Nó removido do grupo: " + removedNode);
        }
    }
}