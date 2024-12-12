package RMISystem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map;

public interface ListInterface extends Remote {
    void addElement(String s) throws RemoteException;
    void removeElement(String s) throws RemoteException;
    ArrayList<String> allMsgs() throws RemoteException;
    Hashtable<String, String> getSnapshot() throws RemoteException;
    void commit() throws RemoteException;
    void addClone() throws RemoteException;
    Hashtable<String, String> getDocumentTable() throws RemoteException;
    List<String> getPendingUpdates() throws RemoteException;
    void clearPendingUpdates() throws RemoteException;

    // Métodos para comunicação distribuída
    void sendSyncMessage(String doc, String requestId) throws RemoteException;
    void sendCommitMessage(String doc) throws RemoteException;
    void sendAck(String uuid, String requestId) throws RemoteException;
    Set<String> getAcksForHeartbeat(String requestId) throws RemoteException;
    void clearAcks(String requestId) throws RemoteException;
    int getAckCounts(String requestId) throws RemoteException;

    // Novo método para contar quantos heartbeats passaram desde o último ACK do receiver
    Map<String, Integer> removeFailures() throws RemoteException;


    // Novo método para obter todos os IDs dos nós do NodeRegistry
    Set<String> getNodeIds() throws RemoteException;
    void printHeartbeatAcks() throws RemoteException;

    Set<String> getReceivers() throws RemoteException;
}
