package Network;

import RMISystem.ListInterface;
import RMISystem.NodeRegistryInterface;
import System.Elemento;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MulticastReceiver extends Thread implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String uuid;
    private final ListInterface listManager;
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final Hashtable<String, String> tempFiles = new Hashtable<>();
    private final NodeRegistryInterface nodeRegistry;
    private final Elemento elemento;

    private transient volatile boolean isRunning = true;
    private transient MulticastSocket socket;
    private transient InetAddress group;

    private enum State { FOLLOWER, CANDIDATE, LEADER }
    private State state = State.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private int electionTimeout;
    private long lastHeartbeat = System.currentTimeMillis();
    private int voteCount = 0;
    private static final int LEADER_DETECTION_TIMEOUT = 10000; // 10 segundos

    public MulticastReceiver(String uuid, Hashtable<String, String> initialSnapshot, ListInterface listManager, NodeRegistryInterface nodeRegistry, Elemento elemento) {
        this.uuid = uuid;
        this.listManager = listManager;
        this.nodeRegistry = nodeRegistry;
        this.elemento = elemento;
        for (Map.Entry<String, String> entry : initialSnapshot.entrySet()) {
            documentTable.put(entry.getKey(), entry.getValue().trim());
            System.out.println("Receiver sincronizado: " + entry.getValue().trim());
        }
        resetElectionTimeout();
    }

    @Override
    public void run() {
        String multicastAddress = MulticastConfig.MULTICAST_ADDRESS;
        int multicastPort = MulticastConfig.MULTICAST_PORT;

        try {
            socket = new MulticastSocket(multicastPort);
            group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);
            System.out.println("Aguardando mensagens multicast no grupo " + multicastAddress + ":" + multicastPort);

            byte[] buffer = new byte[1024];
            long lastCheck = System.currentTimeMillis();

            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(1000); // Timeout de 1 segundo para a chamada receive
                try {
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    // System.out.println("Mensagem recebida: " + message);
                    if (message.startsWith("HEARTBEAT:sync:")) {
                        receiveSyncMessage(message);
                    } else if (message.startsWith("HEARTBEAT:commit:")) {
                        receiveCommitMessage(message);
                    } else if (message.startsWith("REQUEST_VOTE:")) {
                        handleRequestVote(message);
                    } else if (message.startsWith("VOTE_RESPONSE:")) {
                        handleVoteResponse(message);
                    } else {
                        System.out.println("Mensagem desconhecida recebida: " + message);
                    }
                } catch (IOException e) {
                    // Timeout da chamada receive, continuar a execução
                }

                // Verificação periódica do timeout de detecção de falha do líder
                if (System.currentTimeMillis() - lastCheck >= 1000) { // Verifica a cada 1 segundo
                    lastCheck = System.currentTimeMillis();
                    try {
                        if (System.currentTimeMillis() - lastHeartbeat > (LEADER_DETECTION_TIMEOUT + electionTimeout)) {
                            if (state == State.FOLLOWER) {
                                boolean electionInProgress = listManager.isElectionInProgress();
                                if (!electionInProgress) {
                                    System.out.println("Timeout de detecção de líder. A iniciar eleição...");
                                    startElection();
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao receber mensagens multicast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRequestVote(String message) {
        String[] parts = message.split(":");
        int term = Integer.parseInt(parts[1]);
        String candidateId = parts[2];

        if (term > currentTerm) {
            currentTerm = term;
            votedFor = null;
            state = State.FOLLOWER;
        }

        if (votedFor == null || votedFor.equals(candidateId)) {
            votedFor = candidateId;
            sendVoteResponse(candidateId, true);
            resetElectionTimeout();
        } else {
            sendVoteResponse(candidateId, false);
        }

        // Resetar o lastHeartbeat ao receber uma solicitação de voto
        lastHeartbeat = System.currentTimeMillis();
    }

    private void handleVoteResponse(String message) {
        String[] parts = message.split(":");
        boolean voteGranted = Boolean.parseBoolean(parts[1]);

        if (state == State.CANDIDATE && voteGranted) {
            voteCount++;
            try {
                Set<String> nodeIds = nodeRegistry.getNodeIds();
                int totalNodes = nodeIds.size();
                if (voteCount > totalNodes / 2) {
                    state = State.LEADER;
                    elemento.promoteToLeader();
                    listManager.endElection();
                    System.out.println("Eu sou o líder: " + uuid);
                    lastHeartbeat = System.currentTimeMillis(); // Resetar o lastHeartbeat ao se tornar líder
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendVoteResponse(String candidateId, boolean voteGranted) {
        String message = "VOTE_RESPONSE:" + voteGranted;
        sendMessage(message);
    }

    private synchronized void startElection() {
        try {
            boolean electionInProgress = listManager.isElectionInProgress();
            System.out.println("Election in progress: " + electionInProgress);
            if (!electionInProgress) {
                // Mark the election as in progress
                listManager.startElection();

                state = State.CANDIDATE;
                currentTerm++;
                votedFor = uuid;
                lastHeartbeat = System.currentTimeMillis();
                voteCount = 1;

                String message = "REQUEST_VOTE:" + currentTerm + ":" + uuid;
                sendMessage(message);
                System.out.println("Eleição iniciada pelo nó: " + uuid);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MulticastConfig.MULTICAST_PORT);
            socket.send(packet);
            System.out.println("Mensagem enviada: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void receiveSyncMessage(String syncMessage) {
        if (!isRunning) {
            return;
        }

        lastHeartbeat = System.currentTimeMillis(); // Resetar o lastHeartbeat ao receber um heartbeat
        System.out.println("Sync Message recebido: " + syncMessage);

        String[] parts = syncMessage.split(":");
        if (parts.length >= 4) {
            String doc = parts[2];
            String requestId = parts[3];

            String tempFileId = UUID.randomUUID().toString();
            tempFiles.put(tempFileId, doc.trim());
            System.out.println("Documento temporário armazenado no receiver: " + doc.trim() + " com ID: " + tempFileId);

            try {
                listManager.sendAck(uuid, requestId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + syncMessage);
        }
    }

    private synchronized void receiveCommitMessage(String commitMessage) {
        if (!isRunning) {
            return;
        }

        lastHeartbeat = System.currentTimeMillis(); // Resetar o lastHeartbeat ao receber um commit
        System.out.println("Commit Message recebido: " + commitMessage);

        String[] parts = commitMessage.split(":");
        if (parts.length >= 4) {
            String commitId = parts[2];
            String doc = parts[3];

            if (tempFiles.containsValue(doc)) {
                documentTable.put(commitId, doc);
                System.out.println("Documento confirmado no receiver: " + doc + " com ID: " + commitId);
                tempFiles.values().remove(doc);
            } else {
                System.out.println("Documento não encontrado na tempFiles: " + doc);
            }

            for (Map.Entry<String, String> entry : tempFiles.entrySet()) {
                if (!documentTable.containsValue(entry.getValue())) {
                    documentTable.put(entry.getKey(), entry.getValue());
                    System.out.println("Documento confirmado no receiver: " + entry.getValue() + " com ID: " + entry.getKey());
                }
            }

            tempFiles.clear();
           // System.out.println("tempFiles limpo após o commit.");
        } else {
            System.out.println("Mensagem de commit inválida: " + commitMessage);
        }
    }

    public void applyPendingUpdates(List<String> pendingUpdates) {
        for (String update : pendingUpdates) {
            if (update.startsWith("REMOVE:")) {
                String docToRemove = update.substring("REMOVE:".length()).trim();
                documentTable.values().remove(docToRemove);
                System.out.println("Documento removido durante a sincronização: " + docToRemove);
            } else {
                // Verificar se o documento já está na tabela de documentos
                if (!documentTable.containsValue(update.trim())) {
                    String docId = UUID.randomUUID().toString();
                    documentTable.put(docId, update.trim());
                    System.out.println("Documento adicionado durante a sincronização: " + update.trim() + " com ID: " + docId);
                } else {
                    System.out.println("Documento já existe na tabela de documentos: " + update.trim());
                }
            }
        }
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver parado.");
    }

    public void endReceiver() {
        isRunning = false;
        if (socket != null && group != null) {
            try {
                socket.leaveGroup(group);
                socket.close();
                System.out.println("MulticastReceiver removido do grupo multicast e thread terminada.");
            } catch (Exception e) {
                System.out.println("Erro ao sair do grupo multicast: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }

    public Hashtable<String, String> getTempFiles() {
        return tempFiles;
    }

    private void resetElectionTimeout() {
        // Define um timeout de eleição entre 0 e 5 segundos
        this.electionTimeout = ThreadLocalRandom.current().nextInt(0, 5001);
        System.out.println("Timeout de eleição definido para: " + electionTimeout + "ms");
    }
}