package RMISystem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface NodeRegistryInterface extends Remote {
    void registerNode(String nodeId, ListInterface node) throws RemoteException;
    void unregisterNode(String nodeId) throws RemoteException;
    ListInterface getNode(String nodeId) throws RemoteException;
    Map<String, ListInterface> getNodes() throws RemoteException;
}