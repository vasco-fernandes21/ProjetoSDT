package RMISystem;

import Network.MulticastConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private final String uuid = UUID.randomUUID().toString();
    private final ArrayList<String> messageList;  // Lista de documentos
    private final Hashtable<String, String> documentTable; // Tabela de documentos
    private final Hashtable<String, String> tempFiles = new Hashtable<>();
    private final List<String> pendingUpdates; // Lista de atualizações pendentes
    private static final ConcurrentHashMap<String, Set<String>> heartbeatAcks = new ConcurrentHashMap<>(); // ACKs para heartbeats
    private static final ConcurrentHashMap<String, Long> requestTimestamps = new ConcurrentHashMap<>(); // Timestamps para requestIds
    private final NodeRegistryInterface nodeRegistry;
    private boolean electionInProgress = false;

    public ListManager() throws RemoteException {
        super();
        this.messageList = new ArrayList<>();
        this.documentTable = new Hashtable<>();
        this.pendingUpdates = new ArrayList<>();

        // Conectar ao NodeRegistry remoto
        try {
            Registry registry = LocateRegistry.getRegistry("localhost"); // Substitua "localhost" pelo IP adequado se necessário
            nodeRegistry = (NodeRegistryInterface) registry.lookup("NodeRegistry");
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao NodeRegistry", e);
        }
    }

    // Adiciona novos documentos à lista de forma sincronizada
    @Override
    public synchronized void addElement(String s) throws RemoteException {
        String[] messages = s.split(",");
        for (String message : messages) {
            messageList.add(message.trim()); // Adiciona apenas na lista de mensagens
            pendingUpdates.add(message.trim()); // Marca como atualização pendente
            System.out.println("Documento adicionado no líder via RMI: " + message.trim());
        }
        System.out.println("Lista de documentos no líder via RMI: " + messageList);
    }

    // Remove um elemento da lista de forma sincronizada
    @Override
    public synchronized void removeElement(String s) throws RemoteException {
        if (messageList.contains(s)) {
            messageList.remove(s);
            pendingUpdates.add("REMOVE:" + s); // Adiciona a atualização pendente de remoção
            System.out.println("Documento removido no líder via RMI: " + s);
        } else {
            System.out.println("Documento não encontrado para remoção no líder via RMI: " + s);
        }
    }

    // Retorna todos os documentos
    @Override
    public synchronized ArrayList<String> allMsgs() throws RemoteException {
        return new ArrayList<>(messageList);
    }

    // Adiciona uma clonagem da lista
    @Override
    public synchronized void addClone() throws RemoteException {
        ArrayList<String> clonedList = new ArrayList<>(messageList);  // Clonando a lista
        System.out.println("Lista clonada via RMI: " + clonedList);
    }

    // Fornece o estado atual da lista (snapshot) para um novo elemento
    @Override
    public synchronized Hashtable<String, String> getSnapshot() throws RemoteException {
        System.out.println("Snapshot solicitado por um novo elemento via RMI.");
        return new Hashtable<>(documentTable);
    }

    // Retorna a tabela de documentos confirmados
    @Override
    public synchronized Hashtable<String, String> getDocumentTable() throws RemoteException {
        return new Hashtable<>(documentTable);
    }

    // Retorna as atualizações pendentes
    @Override
    public synchronized List<String> getPendingUpdates() throws RemoteException {
        return new ArrayList<>(pendingUpdates);
    }

    // Limpa as atualizações pendentes
    @Override
    public synchronized void clearPendingUpdates() throws RemoteException {
        pendingUpdates.clear();
        System.out.println("Atualizações pendentes limpas via RMI.");
    }

    // Envia uma mensagem de sincronização
    @Override
    public synchronized void sendSyncMessage(String doc, String requestId) throws RemoteException {
        String syncMessage = "HEARTBEAT:sync:" + doc + ":" + requestId;
        System.out.println("Sync Message enviado: " + syncMessage);

        // Armazena o timestamp do requestId
        requestTimestamps.put(requestId, System.currentTimeMillis());

        // Configurações do grupo multicast
        String multicastAddress = MulticastConfig.MULTICAST_ADDRESS;
        int multicastPort = MulticastConfig.MULTICAST_PORT;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(multicastAddress);

            // Constrói o pacote de mensagem
            byte[] buffer = syncMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);

            // Envia o pacote para o grupo multicast
            socket.send(packet);

        } catch (Exception e) {
            System.out.println("Erro ao enviar mensagem em multicast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void sendCommitMessage(String doc) throws RemoteException {
        String commitMessage = "HEARTBEAT:commit:" + UUID.randomUUID().toString() + ":" + doc;
        System.out.println("Commit Message enviado: " + commitMessage);

        // Configurações do grupo multicast
        String multicastAddress = MulticastConfig.MULTICAST_ADDRESS;
        int multicastPort = MulticastConfig.MULTICAST_PORT;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(multicastAddress);

            // Constrói o pacote de mensagem
            byte[] buffer = commitMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);

            // Envia o pacote para o grupo multicast
            socket.send(packet);

            System.out.println("Mensagem de commit enviada: " + commitMessage);
        } catch (Exception e) {
            System.out.println("Erro ao enviar mensagem de commit em multicast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Confirma o commit e adiciona os documentos à tabela de documentos
    @Override
    public synchronized void commit(String doc) throws RemoteException {
        // Verifica se o documento já está na tabela
        if (!documentTable.containsValue(doc)) {
            String docId = UUID.randomUUID().toString();
            documentTable.put(docId, doc);
            System.out.println("Documento confirmado no líder via RMI: " + doc + " com ID: " + docId);
        }
        
        pendingUpdates.remove(doc); // Remove o documento da lista de atualizações pendentes

        // Remove o documento da lista de mensagens
        if (messageList.remove(doc)) {
            System.out.println("Documento removido da lista de mensagens: " + doc);
        } else {
            System.out.println("Documento não encontrado na lista de mensagens: " + doc);
        }
    }

    @Override
    public synchronized void sendAck(String id, String requestId) throws RemoteException {
        // Adiciona o UUID do remetente ao conjunto de ACKs para o requestId
        heartbeatAcks.computeIfAbsent(requestId, k -> new CopyOnWriteArraySet<>()).add(id);

        // Log para depuração: confirma o envio do ACK
      // System.out.println("ACK recebido do sender: " + id + " para o requestId: " + requestId);

        // Log adicional: imprime todos os UUIDs que enviaram ACK para este requestId
      // System.out.println("ACKs acumulados para o requestId " + requestId + " -> " + heartbeatAcks.get(requestId));
    }

    @Override
    public synchronized void clearAcks(String requestId) throws RemoteException {
        heartbeatAcks.remove(requestId);
        requestTimestamps.remove(requestId); // Remove o timestamp associado ao requestId
    }

    @Override
    public synchronized Set<String> getAcksForHeartbeat(String requestId) throws RemoteException {
        Set<String> acks = heartbeatAcks.get(requestId);
        if (acks == null) {
            acks = new CopyOnWriteArraySet<>();
        }
        return acks;
    }

    //get ackCounts que devolve o numero de acks por heartbeat
    @Override
    public synchronized int getAckCounts(String requestId) throws RemoteException {
        Set<String> acks = heartbeatAcks.get(requestId);
        if (acks == null) {
            acks = new CopyOnWriteArraySet<>();
        }
        return acks.size();
    }

    @Override
    public synchronized Map<String, Integer> removeFailures() throws RemoteException {
        // Obter todos os IDs dos nós registrados
        Set<String> nodeIds = getReceivers();

        // Resultado final: Map com o número de heartbeats sem ACK para cada nó
        Map<String, Integer> heartbeatsMissed = new HashMap<>();
    
        // Lista ordenada de requestIds por ordem de chegada com base nos timestamps
        List<String> requestIds = requestTimestamps.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Itera sobre cada nó fornecido
        for (String nodeId : nodeIds) {
            int heartbeatsMissedCount = 0; // Contador de heartbeats perdidos
            boolean ackFound = false; // Flag para indicar se algum ACK foi encontrado
    
            // Percorre os requestIds do mais recente para o mais antigo
            for (int i = requestIds.size() - 1; i >= 0; i--) {
                String requestId = requestIds.get(i);
                Set<String> acksForRequest = heartbeatAcks.getOrDefault(requestId, Collections.emptySet());
    
                if (acksForRequest.contains(nodeId)) {
                    // Se o nó respondeu neste heartbeat, sai do loop
                    ackFound = true;
                    break;
                } else {
                    // Incrementa o contador se o nó não respondeu
                    heartbeatsMissedCount++;
                }
            }
    
            // Se não foi encontrado nenhum ACK, todos os heartbeats são contados
            if (!ackFound) {
                heartbeatsMissedCount = requestIds.size();
            }
    
            // Adiciona o resultado no mapa
            heartbeatsMissed.put(nodeId, heartbeatsMissedCount);
    
            // Log do estado após cada iteração
            System.out.println("ID do Nó: " + nodeId + ", Heartbeats desde último ACK: " + heartbeatsMissedCount);
    
            // Se o contador atingir 4, remove o receiver
            if (heartbeatsMissedCount >= 4) {
                nodeRegistry.removeReceiver(nodeId);
            }
        }
    
        return heartbeatsMissed;
    }

    @Override
    public synchronized Set<String> getNodeIds() throws RemoteException {
        return nodeRegistry.getNodeIds();
    }

   @Override
    public synchronized Set<String> getReceivers() throws RemoteException {
        return nodeRegistry.getReceivers();
    }

    @Override
    public synchronized void printHeartbeatAcks() {
        System.out.println("Conteúdo do mapa heartbeatAcks:");
        for (Map.Entry<String, Set<String>> entry : heartbeatAcks.entrySet()) {
            System.out.println("Request ID: " + entry.getKey() + ", ACKs: " + entry.getValue());
        }
    }

    @Override
    public synchronized boolean isElectionInProgress() {
        return electionInProgress;
    }

    @Override
    public synchronized void startElection() {
        electionInProgress = true;
    }

    @Override
    public synchronized void endElection() {
        electionInProgress = false;
    }

    
}