package Network;

import RMISystem.ListInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MulticastSender extends Thread {
    private static final int HEARTBEAT_INTERVAL = 5000; // Intervalo de 5 segundos
    private static final int ACK_TIMEOUT = 2000; // Timeout para esperar ACKs em milissegundos

    private final ListInterface listManager;

    public MulticastSender(ListInterface listManager) {
        this.listManager = listManager;
    }

    @Override
    public void run() {
        try {
            while (true) {
                List<String> docs = listManager.allMsgs();

                if (!docs.isEmpty()) {
                    for (String doc : docs) {
                        String requestId = UUID.randomUUID().toString(); // Gera um novo UUID para cada heartbeat
                        listManager.sendSyncMessage(doc, requestId);

                        // Processar ACKs de forma síncrona
                        boolean majorityReceived = waitForAcks(requestId, ACK_TIMEOUT);
                        processFinalAcks(requestId); // Processar ACKs finais e verificar contagens
                        if (majorityReceived) {
                            listManager.sendCommitMessage();
                            listManager.commit();
                            listManager.addClone();
                            System.out.println("Commit realizado para o requestId: " + requestId);
                        } else {
                            System.out.println("Não foi possível receber ACKs suficientes para o requestId: " + requestId);
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
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean waitForAcks(String requestId, int timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        Set<String> acks;
        boolean majorityReceived = false;

        while (System.currentTimeMillis() < endTime) {
            try {
                acks = listManager.getAcksForHeartbeat(requestId);
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

    private void processFinalAcks(String requestId) {
        try {
            Set<String> acks = listManager.getAcksForHeartbeat(requestId); // Obter os ACKs finais após o timeout
            System.out.println("Número de ACKs recebidos para requestId " + requestId + ": " + acks.size());
            // Você pode fazer mais verificações ou atualizações aqui
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}