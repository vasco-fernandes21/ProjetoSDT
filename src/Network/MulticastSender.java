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
        try {
            while (true) {
                List<String> docs = listManager.allMsgs();

                if (!docs.isEmpty()) {
                    for (String doc : docs) {
                        String requestId = UUID.randomUUID().toString(); // Gera um novo UUID para cada heartbeat
                        System.out.println("UUID do líder: " + uuid);
                        listManager.getReceivers();
                        listManager.sendSyncMessage(doc, requestId);

                        // Processar ACKs de forma síncrona
                        boolean ackReceived = waitForAcks(requestId, ACK_TIMEOUT);

                        if (ackReceived) {
                            listManager.sendCommitMessage(doc); // Envia o commit
                            listManager.commit(); // Realiza o commit
                            listManager.printHeartbeatAcks(); // Printa os ACKs recebidos
                            listManager.removeFailures(); // Printa os heartbeats sem ACKs
                            System.out.println("Commit realizado para o requestId: " + requestId);
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

        boolean majorityReceived = false;

        try {
            // Obtenha o número total de elementos
            Set<String> receivers = listManager.getReceivers();
            int totalElements = receivers.size();

            // Calcula o valor inteiro mais próximo de 2/3 do total de elementos
            int majorityThreshold = (int) Math.ceil((2.0 / 3.0) * totalElements);

            while (System.currentTimeMillis() < endTime) {
                try {
                    int ackCount = listManager.getAckCounts(requestId);
                    acks = listManager.getAcksForHeartbeat(requestId);

                    if (ackCount >= majorityThreshold) { // Verifica se a maioria dos ACKs foi recebida
                        System.out.println("Maioria dos ACKs recebidos para: " + requestId);
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
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return majorityReceived;
    }

    public String getLiderId() {
        return uuid;
    }
}