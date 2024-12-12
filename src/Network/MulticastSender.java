package Network;

import RMISystem.ListInterface;
import RMISystem.NodeRegistryInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MulticastSender extends Thread {
    private static final int HEARTBEAT_INTERVAL = 5000; // Intervalo de 5 segundos
    private static final int ACK_TIMEOUT = 2000; // Timeout para esperar ACKs em milissegundos
    private final String uuid;
    private final ListInterface listManager;

    public MulticastSender(ListInterface listManager, String uuid) {
        this.listManager = listManager;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        // Thread para monitorar heartbeats sem ACKs
        new Thread(() -> {
            while (true) {
                try {
                    // Estabelecer conexão com o NodeRegistry remoto
                    Registry registry = LocateRegistry.getRegistry("localhost"); // Substitua "localhost" pelo IP adequado se necessário
                    NodeRegistryInterface nodeRegistry = (NodeRegistryInterface) registry.lookup("NodeRegistry");

                    // Obter todos os IDs dos nós registrados
                    Set<String> nodeIds = nodeRegistry.getNodeIds();
                    nodeIds.remove(this.uuid); // Remover o ID do líder

                    // Verificar a contagem de heartbeats sem ACKs para cada nó
                    Map<String, Integer> heartbeatsSemAcksMap = listManager.heartbeatsSemAcks(nodeIds);
                    for (Map.Entry<String, Integer> entry : heartbeatsSemAcksMap.entrySet()) {
                        //System.out.println("Heartbeats sem ACKs para o nó " + entry.getKey() + ": " + entry.getValue());
                    }
                    
                    // Espera antes de verificar novamente
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (RemoteException | InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {
            while (true) {
                List<String> docs = listManager.allMsgs();

                if (!docs.isEmpty()) {
                    for (String doc : docs) {
                        String requestId = UUID.randomUUID().toString(); // Gera um novo UUID para cada heartbeat
                        System.out.println("UUID do líder: " + uuid);
                        listManager.sendSyncMessage(doc, requestId);

                        // Processar ACKs de forma síncrona
                        boolean ackReceived = waitForAcks(requestId, ACK_TIMEOUT);

                        if (ackReceived) {
                            listManager.sendCommitMessage(doc); // Envia o commit
                            listManager.commit(); // Realiza o commit
                            listManager.addClone(); // Atualiza clones, se necessário
                            System.out.println("Commit realizado para o requestId: " + requestId);
                            listManager.clearAcks(requestId);
                        } else {
                            System.out.println("Nenhum ACK recebido para o requestId: " + requestId);
                        }

                        // Aguardar antes de enviar o próximo documento
                        Thread.sleep(HEARTBEAT_INTERVAL);
                    }
                } else {
                    System.out.println("Nenhum documento para sincronizar. Heartbeat de sync não enviado.");
                }

                Thread.sleep(HEARTBEAT_INTERVAL);
            }
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean waitForAcks(String requestId, int timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        Set<String> acks;
        int teste;

        boolean majorityReceived = false;

        while (System.currentTimeMillis() < endTime) {
            try {
                teste = listManager.getAckCounts(requestId);
                acks = listManager.getAcksForHeartbeat(requestId);
                //print do numero de acks recebidos
                System.out.println(teste);
                if (acks.size() >= 2) { // Verifica se pelo menos dois elementos enviaram um ACK
                    System.out.println("Majority of ACKs received for requestId: " + requestId);
                    majorityReceived = true;
                    break;
                }
                long timeLeft = endTime - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    break;
                }
                // Espera um tempo antes de verificar novamente
                Thread.sleep(timeLeft);
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return majorityReceived;
    }

    public String getLiderId() {
        return uuid;
    }
}