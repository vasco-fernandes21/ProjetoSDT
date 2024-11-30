package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private final ArrayList<String> messageList;  // Lista de documentos
    private final Hashtable<String, String> documentTable; // Tabela de documentos
    private final List<String> pendingUpdates; // Lista de atualizações pendentes

    public ListManager() throws RemoteException {
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
        System.out.println("Documento adicionado no líder: " + message.trim());
    }
    System.out.println("Lista de documentos no líder: " + messageList); // Imprimir para verificar a lista
}

    // Remove um elemento da lista de forma sincronizada
    @Override
    public synchronized void removeElement(String s) throws RemoteException {
        if (messageList.contains(s)) {
            messageList.remove(s);
            pendingUpdates.add("REMOVE:" + s); // Adiciona a atualização pendente de remoção
            System.out.println("Documento removido no líder: " + s);
        } else {
            System.out.println("Documento não encontrado para remoção no líder: " + s);
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
        System.out.println("Lista clonada: " + clonedList);
    }

    // Fornece o estado atual da lista (snapshot) para um novo elemento
    @Override
    public synchronized ArrayList<String> getSnapshot() throws RemoteException {
        System.out.println("Snapshot solicitado por um novo elemento.");
        return new ArrayList<>(messageList);
    }

    // Confirma o commit e adiciona os documentos à tabela de documentos
    @Override
    public synchronized void commit() throws RemoteException {
        // Adiciona documentos confirmados na documentTable
        for (String doc : messageList) {
            if (!documentTable.containsValue(doc)) {  // Verifica se o documento já está na tabela
                String docId = UUID.randomUUID().toString();
                documentTable.put(docId, doc);
                System.out.println("Documento confirmado no líder: " + doc + " com ID: " + docId);
            }
        }

        // Limpa a lista de mensagens, pois os documentos foram confirmados
        messageList.clear();
        System.out.println("Commit confirmado no líder. Lista de mensagens limpa.");
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
    }
}