package RMISystem;

import RMISystem.ListInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private static final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();

    public static synchronized void registerNode(String nodeId, ListInterface node) {
        nodes.put(nodeId, node);
        System.out.println("Nó registrado: " + nodeId);
    }

    public static synchronized void unregisterNode(String nodeId) {
        nodes.remove(nodeId);
        System.out.println("Nó removido: " + nodeId);
    }

    public static synchronized ListInterface getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public static synchronized Map<String, ListInterface> getNodes() {
        return new ConcurrentHashMap<>(nodes); // Retorna uma cópia para evitar problemas de concorrência
    }
}