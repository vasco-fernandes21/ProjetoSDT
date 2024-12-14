package RMISystem;

import Network.MulticastReceiver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface NodeRegistryInterface extends Remote {
    void registerNode(String nodeId, ListInterface node) throws RemoteException;
    Set<String> getNodeIds() throws RemoteException;

    // Métodos para gerenciar receivers
    void addReceiver(String nodeId, MulticastReceiver receiver) throws RemoteException;
    void deleteReceiver(String nodeId) throws RemoteException;
    void removeReceiver(String nodeId) throws RemoteException;
    Set<String> getReceivers() throws RemoteException;

    // Métodos para gerir o líder
    void setLeaderID(String leaderID) throws RemoteException;

    // Métodos para gerenciar o bloqueio de eleição
    boolean acquireElectionLock() throws RemoteException;
    void releaseElectionLock() throws RemoteException;
}