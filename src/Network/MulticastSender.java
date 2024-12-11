package Network;

import RMISystem.ListInterface;
import RMISystem.NodeRegistry;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

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
                    listManager.sendSyncMessage(doc, requestId);
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
