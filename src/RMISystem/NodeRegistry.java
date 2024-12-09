// src/RMISystem/NodeRegistry.java
package RMISystem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry extends UnicastRemoteObject implements NodeRegistryInterface {
    private final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();

    protected NodeRegistry() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerNode(String nodeId, ListInterface node) throws RemoteException {
        nodes.put(nodeId, node);
        System.out.println("Nó registrado: " + nodeId);
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
        Map<String, ListInterface> nodesCopy = new ConcurrentHashMap<>(nodes);
        return nodesCopy;
    }

    private void printRegisteredNodes() {
        System.out.println("Nós registrados atualmente: ");
        for (Map.Entry<String, ListInterface> entry : nodes.entrySet()) {
            System.out.println("Nó: " + entry.getKey());
        }
    }
}