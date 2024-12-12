package RMISystem;

import Network.MulticastReceiver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface NodeRegistryInterface extends Remote {
    void registerNode(String nodeId, ListInterface node) throws RemoteException;
    void unregisterNode(String nodeId) throws RemoteException;
    ListInterface getNode(String nodeId) throws RemoteException;
    Map<String, ListInterface> getNodes() throws RemoteException;
    Set<String> getNodeIds() throws RemoteException;

    // Métodos para gerenciar receivers
    void addReceiver(String nodeId, MulticastReceiver receiver) throws RemoteException;
    void removeReceiver(String nodeId) throws RemoteException;
    MulticastReceiver getReceiver(String nodeId) throws RemoteException;
    Set<String> getReceivers() throws RemoteException; // Update return type to Set<String>
}