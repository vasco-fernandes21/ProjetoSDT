package System;

import Network.MulticastReceiver;
import Network.MulticastSender;
import RMISystem.ListInterface;
import RMISystem.NodeRegistry;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Elemento {
    private static final Map<String, MulticastReceiver> receiverMap = new HashMap<>();
    private final String uuid;
    private MulticastReceiver receiver;

    public Elemento(int lider) {
        this.uuid = UUID.randomUUID().toString();
        System.out.println("UUID do elemento: " + this.uuid);

        if (lider == 1) {
            System.out.println("Processo iniciado como líder.");
            try {
                Registry registry = LocateRegistry.getRegistry("localhost");
                ListInterface listManager = (ListInterface) registry.lookup("ListManager");

                MulticastSender sender = new MulticastSender(listManager);
                sender.start();  // Inicia a thread do sender

                // Registrar o nó no NodeRegistry
                NodeRegistry.registerNode(this.uuid, listManager);

                System.out.println("Líder iniciado com sucesso.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Processo iniciado como não-líder. A sincronizar...");

            try {
                Registry registry = LocateRegistry.getRegistry("localhost");
                ListInterface listManager = (ListInterface) registry.lookup("ListManager");

                // Solicitar snapshot ao líder
                ArrayList<String> snapshot = listManager.getSnapshot();
                System.out.println("Snapshot recebido do líder: " + snapshot);

                // Inicializar receiver com o snapshot e listManager
                receiver = new MulticastReceiver(this.uuid, snapshot, null, listManager);
                receiver.start();  // Inicia a thread do receiver

                // Adicionar o receiver ao mapa
                receiverMap.put(this.uuid, receiver);

                // Registrar o nó no NodeRegistry
                NodeRegistry.registerNode(this.uuid, listManager);

                System.out.println("Não-líder sincronizado com sucesso.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopReceiver() {
        if (receiver != null) {
            receiver.stopRunning();
            System.out.println("Elemento " + this.uuid + " parou de receber pacotes.");

            // Remover o nó do NodeRegistry
            NodeRegistry.unregisterNode(this.uuid);
        }
    }

    public static void stopReceiverById(String uuid) {
        MulticastReceiver receiver = receiverMap.get(uuid);
        if (receiver != null) {
            receiver.stopRunning();
            receiverMap.remove(uuid);
            System.out.println("Elemento " + uuid + " removido do grupo de multicast.");

            // Remover o nó do NodeRegistry
            NodeRegistry.unregisterNode(uuid);
        } else {
            System.out.println("Elemento " + uuid + " não encontrado.");
        }
    }

    public String getUuid() {
        return uuid;
    }

    public MulticastReceiver getReceiver() {
        return receiver;
    }
}