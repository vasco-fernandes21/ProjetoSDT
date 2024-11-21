package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private final ArrayList<String> messageList;  // Lista de documentos
    private final Hashtable<String, String> documentTable; // Tabela de documentos

    public ListManager() throws RemoteException {
        this.messageList = new ArrayList<>();
        this.documentTable = new Hashtable<>();
    }

    // Adiciona um novo documento à lista de forma sincronizada
    @Override
    public synchronized void addElement(String s) throws RemoteException {
        String docId = UUID.randomUUID().toString();
        messageList.add(s);
        documentTable.put(docId, s);
        System.out.println("Documento adicionado no líder: " + s + " com ID: " + docId);
        System.out.println("Lista de documentos no líder: " + messageList); // Imprimir para verificar a lista
    }

    // Remove um elemento da lista de forma sincronizada
    @Override
    public synchronized void removeElement(String s) throws RemoteException {
        if (messageList.contains(s)) {
            messageList.remove(s);
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
        System.out.println("Snapshot solicitado por um novo elemento. Lista enviada: " + messageList);
        return new ArrayList<>(messageList); // Retorna uma cópia da lista atual
    }
}