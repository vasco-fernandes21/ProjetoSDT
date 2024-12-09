package RMISystem;

import RMISystem.ListInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private static final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();

    public static synchronized void registerNode(String nodeId, ListInterface node) {
        nodes.put(nodeId, node);
        System.out.println("Nó registrado: " + nodeId);
        printRegisteredNodes();
    }

    public static synchronized void unregisterNode(String nodeId) {
        nodes.remove(nodeId);
        System.out.println("Nó removido: " + nodeId);
        printRegisteredNodes();
    }

    public static synchronized ListInterface getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public static synchronized Map<String, ListInterface> getNodes() {
        Map<String, ListInterface> nodesCopy = new ConcurrentHashMap<>(nodes);
        System.out.println("getNodes() chamado. Nós registrados atualmente: " + nodesCopy.keySet());
        return nodesCopy; // Retorna uma cópia para evitar problemas de concorrência
    }

    private static void printRegisteredNodes() {
        System.out.println("Nós registrados atualmente: ");
        for (Map.Entry<String, ListInterface> entry : nodes.entrySet()) {
            System.out.println("Nó: " + entry.getKey());
        }
    }
}