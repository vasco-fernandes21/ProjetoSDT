package RMISystem;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistrySetup {
    public static void main(String[] args) {
        try {
            // Cria uma instância do NodeRegistry
            NodeRegistry nodeRegistry = new NodeRegistry();
            
            // Cria o registro RMI na porta 1099 (padrão)
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Liga o NodeRegistry no registro RMI com o nome "NodeRegistry"
            registry.rebind("NodeRegistry", nodeRegistry);
            
            System.out.println("NodeRegistry está registado no RMI Registry.");
            
            // Opcional: Registrar o ListManager também
            ListInterface listManager = new ListManager();
            registry.rebind("ListManager", listManager);
            System.out.println("ListManager está registado no RMI Registry.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}