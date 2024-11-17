package RMISystem;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class RMIRegistrySetup {
    public static void main(String[] args) {
        try {
            // Cria uma instância do ListManager
            ListManager listManager = new ListManager();

            // Registra o ListManager no registro RMI
            Registry registry = LocateRegistry.createRegistry(1099);  // Porta padrão 1099 para RMI
            registry.rebind("ListManager", listManager);

            System.out.println("Servidor RMI iniciado e ListManager registrado.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}