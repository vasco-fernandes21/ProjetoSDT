package Network;

import RMISystem.ListInterface;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int HEARTBEAT_INTERVAL = 5000; // Intervalo de 5 segundos

    private final ListInterface listManager;
    private final Set<String> activeNodes;
    private final AckProcessor ackProcessor;

    public MulticastSender(ListInterface listManager, Set<String> activeNodes, AckProcessor ackProcessor) {
        this.listManager = listManager;
        this.activeNodes = activeNodes;
        this.ackProcessor = ackProcessor;
        this.ackProcessor.start(); // Iniciar a thread AckProcessor no construtor
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            System.out.println("Socket de Multicast unido ao grupo " + MULTICAST_GROUP_ADDRESS);

            while (true) {
                List<String> docs = listManager.allMsgs();
                if (!docs.isEmpty()) {
                    for (String doc : docs) {
                        String requestId = UUID.randomUUID().toString();
                        sendSyncMessage(socket, group, doc, requestId);

                        if (waitForAcks(requestId)) {
                            sendCommitMessage(socket, group);
                            listManager.commit();
                            listManager.addClone();
                        } else {
                            System.out.println("Não foi possível receber ACKs suficientes.");
                        }

                        // Aguardar o próximo ciclo de heartbeat
                        Thread.sleep(HEARTBEAT_INTERVAL);
                    }
                } else {
                    // Aguardar o próximo ciclo de heartbeat
                    Thread.sleep(HEARTBEAT_INTERVAL);
                }

                // Verificar elementos que não responderam dentro do período de tempo aceitável
                checkForFailures();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendSyncMessage(MulticastSocket socket, InetAddress group, String doc, String requestId) throws IOException {
        String syncMessage = "HEARTBEAT:sync:" + doc + ":" + requestId;
        byte[] syncBuffer = syncMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(syncBuffer, syncBuffer.length, group, PORT);
        socket.send(packet);
        System.out.println("Sync Message enviado: " + syncMessage);
    }

    private boolean waitForAcks(String requestId) {
        synchronized (ackProcessor) {
            try {
                ackProcessor.wait(2000); // Espera por ACKs ou timeout
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return ackProcessor.getMissedCount(requestId) < (activeNodes.size() / 2) + 1; // Verifica se a maioria dos elementos enviou um ACK
        }
    }

    private void sendCommitMessage(MulticastSocket socket, InetAddress group) throws IOException {
        String commitMessage = "HEARTBEAT:commit";
        byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, PORT);
        socket.send(commitPacket);
        System.out.println("Commit enviado: " + commitMessage);
    }

    private void checkForFailures() {
        Set<String> toRemove = new HashSet<>();
        synchronized (ackProcessor) {
            for (String node : activeNodes) {
                if (!ackProcessor.hasAck(node)) {
                    toRemove.add(node);
                    System.out.println("Nó marcado para remoção por falta de ACK: " + node + ". Missed ACKs: " + ackProcessor.getMissedCount(node));
                }
            }
        }
        activeNodes.removeAll(toRemove);

        for (String removedNode : toRemove) {
            System.out.println("Nó removido do grupo: " + removedNode);
        }
    }
}