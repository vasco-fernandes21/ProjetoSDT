package RMISystem;

import Network.MulticastReceiver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry extends UnicastRemoteObject implements NodeRegistryInterface {
    private final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();
    private final Map<String, MulticastReceiver> receiverMap = new ConcurrentHashMap<>();

    public NodeRegistry() throws RemoteException {
        super();
    }

    @Override
    public void registerNode(String nodeId, ListInterface node) throws RemoteException {
        nodes.put(nodeId, node);
    }

    @Override
    public void unregisterNode(String nodeId) throws RemoteException {
        nodes.remove(nodeId);
        receiverMap.remove(nodeId);
    }

    @Override
    public ListInterface getNode(String nodeId) throws RemoteException {
        return nodes.get(nodeId);
    }

    @Override
    public Map<String, ListInterface> getNodes() throws RemoteException {
        return nodes;
    }

    @Override
    public Set<String> getNodeIds() throws RemoteException {
        return nodes.keySet();
    }

    @Override
    public void addReceiver(String nodeId, MulticastReceiver receiver) throws RemoteException {
        receiverMap.put(nodeId, receiver);
    }

    @Override
    public void removeReceiver(String nodeId) throws RemoteException {
        MulticastReceiver receiver = receiverMap.get(nodeId);
        if (receiver != null) {
            receiver.endReceiver();
            receiverMap.remove(nodeId);
            nodes.remove(nodeId);
            System.out.println("Elemento " + nodeId + " removido do grupo de multicast.");
        }
    }

    @Override
    public MulticastReceiver getReceiver(String nodeId) throws RemoteException {
        return receiverMap.get(nodeId);
    }

    @Override
    public Set<String> getReceivers() throws RemoteException {
        return receiverMap.keySet(); // Update return type to Set<String>
    }
}