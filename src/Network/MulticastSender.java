package Network;

import RMISystem.ListInterface;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int HEARTBEAT_INTERVAL = 5000; // Intervalo de 5 segundos
    private static final int ACK_TIMEOUT = 2000; // Timeout para esperar ACKs em milissegundos

    private final ListInterface listManager;
    private final AckProcessor ackProcessor;
    private final InetAddress group;

    public MulticastSender(ListInterface listManager, AckProcessor ackProcessor) throws IOException {
        this.listManager = listManager;
        this.ackProcessor = ackProcessor;
        this.group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
        this.ackProcessor.start(); // Iniciar a thread AckProcessor no construtor
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            socket.joinGroup(group);
            System.out.println("Socket de Multicast unido ao grupo " + MULTICAST_GROUP_ADDRESS);

            while (true) {
                List<String> docs = listManager.allMsgs();

                if (!docs.isEmpty()) {
                    for (String doc : docs) {
                        String requestId = UUID.randomUUID().toString(); // Gera um novo UUID para cada heartbeat
                        sendSyncMessage(socket, doc, requestId);

                        // Processar ACKs de forma síncrona
                        boolean majorityReceived = waitForAcks(requestId, ACK_TIMEOUT);
                        if (majorityReceived) {
                            sendCommitMessage(socket);
                            listManager.commit();
                            listManager.addClone();
                            ackProcessor.clearAcks(requestId); // Limpar ACKs após processamento
                        } else {
                            System.out.println("Não foi possível receber ACKs suficientes para o requestId: " + requestId);
                            removeUnresponsiveNodes(socket);
                        }

                        // Aguardar 5 segundos antes de enviar o próximo documento
                        Thread.sleep(HEARTBEAT_INTERVAL);
                    }
                } else {
                    // Não enviar heartbeat de sincronização quando a lista está vazia
                    System.out.println("Nenhum documento para sincronizar. Heartbeat de sync não enviado.");
                }

                // Aguardar 5 segundos antes de verificar novamente a lista de documentos
                Thread.sleep(HEARTBEAT_INTERVAL);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendSyncMessage(MulticastSocket socket, String doc, String requestId) throws IOException {
        String syncMessage = "HEARTBEAT:sync:" + doc + ":" + requestId;
        byte[] syncBuffer = syncMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(syncBuffer, syncBuffer.length, group, PORT);
        socket.send(packet);
        System.out.println("Sync Message enviado: " + syncMessage);
    }

    private boolean waitForAcks(String requestId, int timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        synchronized (ackProcessor) {
            while (System.currentTimeMillis() < endTime) {
                Set<String> acks = ackProcessor.getAcksForHeartbeat(requestId);
                if (acks.size() >= 2) { // Verifica se pelo menos dois elementos enviaram um ACK
                    return true;
                }
                long timeLeft = endTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    break;
                }
                try {
                    ackProcessor.wait(timeLeft);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private void sendCommitMessage(MulticastSocket socket) throws IOException {
        String commitMessage = "HEARTBEAT:commit:" + UUID.randomUUID().toString(); // Opcional: incluir requestId do commit
        byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, PORT);
        socket.send(commitPacket);
        System.out.println("Commit enviado: " + commitMessage);
    }

    /**
     * Remove nós que não responderam adequadamente.
     */
    private void removeUnresponsiveNodes(MulticastSocket socket) {
        Map<String, Integer> ackCounts = ackProcessor.getNodeAckCounts();
        // Definir um limiar de ACKs para considerar um nó como responsivo
        int requiredAcks = 3; // Ajustar conforme necessário

        for (Map.Entry<String, Integer> entry : ackCounts.entrySet()) {
            if (entry.getValue() < requiredAcks) {
                String nodeId = entry.getKey();
                try {
                    // Remover o nó do grupo de multicast
                    InetAddress nodeAddress = InetAddress.getByName(nodeId);
                    socket.leaveGroup(nodeAddress);
                    System.out.println("Removendo nó não responsivo: " + nodeId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        ackProcessor.resetNodeAckCounts(); // Resetar contagens após a verificação
    }
}