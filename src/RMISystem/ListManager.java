package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private final String uuid = UUID.randomUUID().toString();
    private final ArrayList<String> messageList;  // Lista de documentos
    private final Hashtable<String, String> documentTable; // Tabela de documentos
    private final List<String> pendingUpdates; // Lista de atualizações pendentes
    private final ConcurrentHashMap<String, Set<String>> heartbeatAcks = new ConcurrentHashMap<>(); // ACKs para heartbeats

    public ListManager() throws RemoteException {
        super();
        this.messageList = new ArrayList<>();
        this.documentTable = new Hashtable<>();
        this.pendingUpdates = new ArrayList<>();
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
    public synchronized ArrayList<String> getSnapshot() throws RemoteException {
        System.out.println("Snapshot solicitado por um novo elemento via RMI.");
        return new ArrayList<>(messageList);
    }

    // Confirma o commit e adiciona os documentos à tabela de documentos
    @Override
    public synchronized void commit() throws RemoteException {
        // Clonar a lista de mensagens antes de apagá-la
        ArrayList<String> clonedList = new ArrayList<>(messageList);
        System.out.println("Lista clonada antes do commit via RMI: " + clonedList);

        // Adiciona documentos confirmados na documentTable
        for (String doc : clonedList) { // Use clonedList instead of messageList
            if (!documentTable.containsValue(doc)) {  // Verifica se o documento já está na tabela
                String docId = UUID.randomUUID().toString();
                documentTable.put(docId, doc);
                System.out.println("Documento confirmado no líder via RMI: " + doc + " com ID: " + docId);
            }
        }

        // Limpa a lista de mensagens, pois os documentos foram confirmados
        messageList.clear();
        System.out.println("Commit confirmado no líder via RMI. Lista de mensagens limpa.");
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

        // imprime o id dos nós registados
        Map<String, ListInterface> nodesBefore = NodeRegistry.getNodes();
        System.out.println("Nós registados antes do envio: " + nodesBefore);

        // Envia para todos os nós registados
        for (String nodeId : nodesBefore.keySet()) {
            ListInterface node = NodeRegistry.getNode(nodeId);
            System.out.println("Node ID: " + nodeId);
            if (node != null) {
                try {
                    node.receiveSyncMessage(syncMessage); // Chama o método do nó remoto para receber a sync message
                } catch (RemoteException e) {
                    System.out.println("Erro ao enviar SyncMessage para o nó " + nodeId + ": " + e.getMessage());
                }
            }
        }

        Map<String, ListInterface> nodesAfter = NodeRegistry.getNodes();
        System.out.println("Nós registados após o envio: " + nodesAfter);
    }

    @Override
    public synchronized void sendCommitMessage() throws RemoteException {
        String commitMessage = "HEARTBEAT:commit:" + UUID.randomUUID().toString();
        System.out.println("Commit Message enviado: " + commitMessage);

        // Envia para todos os nós registados
        for (String nodeId : NodeRegistry.getNodes().keySet()) {
            ListInterface node = NodeRegistry.getNode(nodeId);
            if (node != null) {
                try {
                    node.receiveCommitMessage(commitMessage); // Chama o método do nó remoto para receber a commit message
                } catch (RemoteException e) {
                    System.out.println("Erro ao enviar CommitMessage para o nó " + nodeId + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public synchronized void sendAck(String uuid, String requestId) throws RemoteException {
        System.out.println("ACK enviado para o líder: " + uuid + " para o requestId: " + requestId);
        heartbeatAcks.computeIfAbsent(requestId, k -> new CopyOnWriteArraySet<>()).add(uuid);
    }

    @Override
    public synchronized Set<String> getAcksForHeartbeat(String requestId) throws RemoteException {
        return heartbeatAcks.getOrDefault(requestId, new CopyOnWriteArraySet<>());
    }

    @Override
    public synchronized void receiveSyncMessage(String syncMessage) throws RemoteException {
        System.out.println("Sync Message recebido: " + syncMessage);

        // Processa a mensagem de sincronização
        String[] parts = syncMessage.split(":");
        if (parts.length >= 4) {
            String doc = parts[2]; // Documento a ser sincronizado
            String requestId = parts[3]; // ID do request para o ACK

            // Verifica se há atualizações pendentes
            if (!pendingUpdates.isEmpty()) {
                pendingUpdates.add(syncMessage);
                System.out.println("Mensagem de sincronização pendente armazenada: " + syncMessage);
            } else {
                // Adiciona o documento à lista e marca como atualização pendente
                addElement(doc.trim());
                System.out.println("Heartbeat sincronizado com documento: " + doc.trim());
            }

            // Envia ACK para o líder confirmando o recebimento
            sendAck(this.uuid, requestId);
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + syncMessage);
        }
    }

    @Override
    public synchronized void receiveCommitMessage(String commitMessage) throws RemoteException {
        System.out.println("Commit Message recebido: " + commitMessage);

        // Processa a mensagem de commit, que tem a estrutura: HEARTBEAT:commit:{uuid}
        String[] parts = commitMessage.split(":");
        if (parts.length >= 3) {
            String commitId = parts[2]; // UUID da mensagem de commit

            // Verifica se há atualizações pendentes
            if (!pendingUpdates.isEmpty()) {
                // Processa as atualizações pendentes
                for (String update : pendingUpdates) {
                    // Se for uma remoção, removemos o documento
                    if (update.startsWith("REMOVE:")) {
                        String docToRemove = update.substring(7); // Remove o prefixo "REMOVE:"
                        removeElement(docToRemove.trim());
                        System.out.println("Documento removido durante o commit: " + docToRemove);
                    } else {
                        // Se for uma adição, adicionamos o documento
                        addElement(update.trim());
                        System.out.println("Documento adicionado durante o commit: " + update);
                    }
                }

                // Limpa as atualizações pendentes após o commit
                clearPendingUpdates();
                System.out.println("Todas as atualizações pendentes processadas e limpas.");
            } else {
                System.out.println("Nenhuma atualização pendente para processar no commit.");
            }

            // Envia ACK confirmando o processamento do commit
            sendAck(this.uuid, commitId);
        } else {
            System.out.println("Mensagem de commit inválida: " + commitMessage);
        }
    }

    @Override
    public synchronized void receiveAck(String uuid, String requestId) throws RemoteException {
        System.out.println("ACK recebido de: " + uuid + " para o requestId: " + requestId);
        heartbeatAcks.computeIfAbsent(requestId, k -> new CopyOnWriteArraySet<>()).add(uuid);
    }
}