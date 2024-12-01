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
    private final String leaderUuid;

    // Estruturas para rastrear ACKs
    private final ConcurrentHashMap<String, Set<String>> heartbeatAcks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nodeAckCounts = new ConcurrentHashMap<>();

    public AckProcessor(int ackPort, String leaderUuid) {
        this.ackPort = ackPort;
        this.leaderUuid = leaderUuid;
    }

    public synchronized void processAck(String requestId, String nodeId) {
        if (!nodeId.equals(leaderUuid)) { // Ignorar ACKs do líder
            heartbeatAcks.computeIfAbsent(requestId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
            nodeAckCounts.merge(nodeId, 1, Integer::sum);
            System.out.println("ACK recebido de: " + nodeId + " para requestId: " + requestId);
            notifyAll(); // Notificar threads aguardando por ACKs
        }
    }

    public synchronized Set<String> getAcksForHeartbeat(String requestId) {
        return heartbeatAcks.getOrDefault(requestId, ConcurrentHashMap.newKeySet());
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

    public synchronized void clearAcks(String requestId) {
        heartbeatAcks.remove(requestId);
    }

    public synchronized Map<String, Integer> getNodeAckCounts() {
        return nodeAckCounts;
    }

    public synchronized void resetNodeAckCounts() {
        nodeAckCounts.clear();
    }
}