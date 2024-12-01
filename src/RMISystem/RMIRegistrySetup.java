package RMISystem;

import RMISystem.ListInterface;
import RMISystem.ListManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistrySetup {
    public static void main(String[] args) {
        try {
            // Criar o RMI Registry na porta padrão 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            System.out.println("RMI Registry criado na porta 1099.");

            // Criar uma instância do ListManager e registrar no registry
            ListInterface listManager = new ListManager();
            registry.rebind("ListManager", listManager);
            System.out.println("ListManager registrado no RMI Registry.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}