package RMISystem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ListInterface extends Remote {
    void addElement(String s) throws RemoteException;    // Adiciona um novo documento
    void removeElement(String s) throws RemoteException; // Remove um documento
    ArrayList<String> allMsgs() throws RemoteException;   // Retorna todos os documentos
    void addClone() throws RemoteException;              // Adiciona uma cópia/clonagem da lista de documentos

    ArrayList<String> getSnapshot() throws RemoteException; // Retorna uma cópia da lista atual para sincronização
}