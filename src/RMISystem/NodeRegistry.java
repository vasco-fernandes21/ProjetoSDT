package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry extends UnicastRemoteObject implements NodeRegistryInterface {
    private final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();

    protected NodeRegistry() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerNode(String nodeId, ListInterface node) throws RemoteException {
        nodes.put(nodeId, node);
        System.out.println("Nó registado: " + nodeId);
    }

    @Override
    public synchronized void unregisterNode(String nodeId) throws RemoteException {
        nodes.remove(nodeId);
        System.out.println("Nó removido: " + nodeId);
    }

    @Override
    public synchronized ListInterface getNode(String nodeId) throws RemoteException {
        return nodes.get(nodeId);
    }

    @Override
    public synchronized Map<String, ListInterface> getNodes() throws RemoteException {
        return new ConcurrentHashMap<>(nodes);
    }

    @Override
    public synchronized Set<String> getNodeIds() throws RemoteException {
        return nodes.keySet();
    }

    private void printRegisteredNodes() {
        System.out.println("Nós registados atualmente: ");
        for (Map.Entry<String, ListInterface> entry : nodes.entrySet()) {
            System.out.println("Nó: " + entry.getKey());
        }
    }
}