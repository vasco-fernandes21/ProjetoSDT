package RMISystem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList; // Importação adicionada
import java.util.Hashtable;
import java.util.List;
import java.util.Map; // Importação adicionada
import java.util.Set;

public interface ListInterface extends Remote {
    void addElement(String s) throws RemoteException;
    void updateElement(String oldDoc, String newDoc) throws RemoteException;
    void deleteElement(String s) throws RemoteException;
    ArrayList<String> allMsgs() throws RemoteException;
    void addClone() throws RemoteException;
    Hashtable<String, String> getSnapshot() throws RemoteException;
    Hashtable<String, String> getDocumentTable() throws RemoteException;
    List<String> getPendingUpdates() throws RemoteException;
    void clearPendingUpdates() throws RemoteException;
    void sendHeartbeat(String type, String doc, String requestId) throws RemoteException;
    void commit(String doc) throws RemoteException;
    void sendAck(String id, String requestId) throws RemoteException;
    void clearAcks(String requestId) throws RemoteException;
    Set<String> getAcksForHeartbeat(String requestId) throws RemoteException;
    void printHeartbeatAcks() throws RemoteException;
    int getAckCounts(String requestId) throws RemoteException;
    Map<String, Integer> removeFailures() throws RemoteException;
    Set<String> getNodeIds() throws RemoteException;
    Set<String> getReceivers() throws RemoteException;

    // Adicione os métodos para controle de eleição
    boolean isElectionInProgress() throws RemoteException;
    void startElection() throws RemoteException;
    void endElection() throws RemoteException;
    
}