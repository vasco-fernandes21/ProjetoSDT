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
        applyPendingUpdates();
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
                    receiveHeartbeat(message);
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

    private void receiveHeartbeat(String message) {
        if (!isRunning) {
            return;
        }

        lastHeartbeat = System.currentTimeMillis(); // Resetar o lastHeartbeat ao receber um heartbeat

        String[] parts = message.split(":");
        if (parts.length >= 4) {
            String type = parts[1];
            String doc = parts[2];
            String requestId = parts[3];

            switch (type) {
                case "sync":
                    handleSync(doc, requestId);
                    break;
                case "commit":
                    handleCommit(doc, requestId);
                    break;
                case "delete":
                    handleDelete(doc, requestId);
                    break;
                case "update":
                    handleUpdate(doc, requestId);
                    break;
                default:
                    System.out.println("Tipo de heartbeat desconhecido: " + type);
            }
        } else if (parts.length == 3) {
            String type = parts[0];
            switch (type) {
                case "REQUEST_VOTE":
                    handleRequestVote(message);
                    break;
                case "VOTE_RESPONSE":
                    handleVoteResponse(message);
                    break;
                default:
                    System.out.println("Tipo de mensagem de votação desconhecido: " + type);
            }
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + message);
        }
    }

    private void handleSync(String doc, String requestId) {
        String tempFileId = UUID.randomUUID().toString();
        tempFiles.put(tempFileId, doc.trim());
        System.out.println("Documento temporário armazenado no receiver: " + doc.trim() + " com ID: " + tempFileId);

        try {
            listManager.sendAck(uuid, requestId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void handleCommit(String doc, String requestId) {
        if (tempFiles.containsValue(doc)) {
            documentTable.put(requestId, doc);
            System.out.println("Documento confirmado no receiver: " + doc + " com ID: " + requestId);
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
    }

    private void handleDelete(String doc, String requestId) {
        documentTable.values().remove(doc);
        System.out.println("Documento removido durante a sincronização: " + doc);

        try {
            listManager.sendAck(uuid, requestId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void handleUpdate(String doc, String requestId) {
        String[] updateParts = doc.split(",");
        if (updateParts.length == 2) {
            String oldDoc = updateParts[0].trim();
            String newDoc = updateParts[1].trim();
            documentTable.values().remove(oldDoc);
            documentTable.put(UUID.randomUUID().toString(), newDoc);
            System.out.println("Documento atualizado durante a sincronização: " + oldDoc + " para " + newDoc);

            try {
                listManager.sendAck(uuid, requestId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Mensagem de atualização inválida: " + doc);
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
        if (parts.length == 3) {
            String senderId = parts[1];
            boolean voteGranted = Boolean.parseBoolean(parts[2]);

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
        } else {
            System.out.println("Mensagem de resposta de voto inválida: " + message);
        }
    }

    private void sendVoteResponse(String candidateId, boolean voteGranted) {
        String message = "VOTE_RESPONSE:" + uuid + ":" + voteGranted;
        sendMessage(message);
    }

    private synchronized void startElection() {
        try {
            boolean electionInProgress = listManager.isElectionInProgress();
            if (!electionInProgress) {
                // Adquirir o bloqueio de eleição
                boolean lockAcquired = nodeRegistry.acquireElectionLock();
                if (!lockAcquired) {
                    System.out.println("Bloqueio de eleição já adquirido por outro nó.");
                    return;
                }
    
                // Se este nó for o líder atual, elimine o líder
                if (state == State.LEADER) {
                    state = State.FOLLOWER;
                }
    
                // Iniciar eleição
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void applyPendingUpdates() {
        try {
            List<String> pendingUpdates = listManager.getPendingUpdates(); // Obtém atualizações pendentes do ListManager
            for (String update : pendingUpdates) {
                //print updates
                System.out.println("Aplicando atualização pendente: " + update);
                if (update.startsWith("DELETE:")) {
                    // Processa mensagens DELETE
                    String docToRemove = update.substring("DELETE:".length()).trim();
                    if (documentTable.containsValue(docToRemove)) {
                        documentTable.values().remove(docToRemove); // Remove o documento da tabela
                        System.out.println("Documento removido durante a verificação de atualizações: " + docToRemove);
                    } else {
                        System.out.println("Documento para remoção não encontrado: " + docToRemove);
                    }
                } else if (update.startsWith("UPDATE:")) {
                    // Processa mensagens UPDATE
                    String[] parts = update.substring("UPDATE:".length()).split(",", 2); // Divide em oldDoc e newDoc
                    if (parts.length == 2) {
                        String oldDoc = parts[0].trim();
                        String newDoc = parts[1].trim();
    
                        if (documentTable.containsValue(oldDoc)) {
                            // Remove o documento antigo
                            documentTable.values().remove(oldDoc);
                            // Adiciona o novo documento
                            String newDocId = UUID.randomUUID().toString();
                            documentTable.put(newDocId, newDoc);
                            System.out.println("Documento atualizado durante a verificação de atualizações: " + oldDoc + " para " + newDoc);
                        } else {
                            System.out.println("Documento antigo não encontrado para atualização: " + oldDoc);
                        }
                    } else {
                        System.out.println("Formato inválido para mensagem de atualização: " + update);
                    }
                } else {
                    // Verificar se o documento já está na tabela de documentos
                    if (!documentTable.containsValue(update.trim())) {
                        String docId = UUID.randomUUID().toString();
                        documentTable.put(docId, update.trim());
                        System.out.println("Documento adicionado durante a verificação de atualizações: " + update.trim() + " com ID: " + docId);
                    } else {
                        System.out.println("Documento já existe na tabela de documentos: " + update.trim());
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver parado.");
    }

    private void resetElectionTimeout() {
        // Define um timeout de eleição entre 0 e 5 segundos
        this.electionTimeout = ThreadLocalRandom.current().nextInt(0, 5001);
        System.out.println("Timeout de eleição definido para: " + electionTimeout + "ms");
    }
}