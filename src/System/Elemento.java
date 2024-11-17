package System;

import Network.MulticastReceiver;
import Network.MulticastSender;
import RMISystem.ListInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.UUID;

public class Elemento {
    private int lider; // 1 para líder, 0 para não-líder
    private final String uuid;

    public Elemento(int lider) {
        this.lider = lider;
        this.uuid = UUID.randomUUID().toString();
        System.out.println("UUID do elemento: " + this.uuid);

        if (this.lider == 1) {
            System.out.println("Processo iniciado como líder. A enviar mensagens...");
            try {
                // Conecta ao RMI Registry e obtém a instância do ListManager
                Registry registry = LocateRegistry.getRegistry("localhost");
                ListInterface listManager = (ListInterface) registry.lookup("ListManager");

                MulticastSender sender = new MulticastSender(listManager);
                sender.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Processo iniciado como não-líder. A receber mensagens...");
            MulticastReceiver receiver = new MulticastReceiver(this.uuid);
            receiver.start();
        }
    }

    public void setLider(int lider) {
        this.lider = lider;
    }

    public String getUuid() {
        return uuid;
    }
}