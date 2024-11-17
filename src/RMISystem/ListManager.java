package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private final ArrayList<String> messageList;  // Lista de documentos

    public ListManager() throws RemoteException {
        this.messageList = new ArrayList<>();
    }

    // Adiciona um novo documento à lista de forma sincronizada
    @Override
    public synchronized void addElement(String s) throws RemoteException {
        messageList.add(s);
        System.out.println("Documento adicionado no líder: " + s);
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
        ArrayList<String> clonedList = new ArrayList<>(messageList);
        messageList.addAll(clonedList);
        System.out.println("Lista clonada e adicionada no líder: " + clonedList);
    }
}