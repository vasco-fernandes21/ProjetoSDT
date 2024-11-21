package System;

import Network.MulticastReceiver;
import RMISystem.ListInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import java.util.ArrayList;
public class Elemento {
    private final String uuid;

    public Elemento(int lider) {
        this.uuid = UUID.randomUUID().toString();
        System.out.println("UUID do elemento: " + this.uuid);

        if (lider == 1) {
            System.out.println("Processo iniciado como líder.");
        } else {
            System.out.println("Processo iniciado como não-líder. A sincronizar...");

            try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            ListInterface listManager = (ListInterface) registry.lookup("ListManager");

            // Solicitar snapshot ao líder
            ArrayList<String> snapshot = listManager.getSnapshot();
            System.out.println("Snapshot recebido do líder: " + snapshot);

            // Inicializar lista local com o snapshot
            MulticastReceiver receiver = new MulticastReceiver(this.uuid, snapshot);
            receiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
}