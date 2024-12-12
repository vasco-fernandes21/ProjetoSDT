package System;

import Network.MulticastReceiver;
import Network.MulticastSender;
import RMISystem.ListInterface;
import RMISystem.NodeRegistryInterface;

import java.rmi.RemoteException; // Adicione esta linha
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Elemento {
    private static final Map<String, MulticastReceiver> receiverMap = new ConcurrentHashMap<>();
    private final String uuid;
    private MulticastReceiver receiver;
    private final NodeRegistryInterface nodeRegistry;

    public Elemento(int lider) {
        this.uuid = UUID.randomUUID().toString();
        System.out.println("UUID do elemento: " + this.uuid);

        // Conectar ao NodeRegistry remoto uma vez e reutilizar a instância
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost"); // Substitua "localhost" pelo IP adequado se necessário
            nodeRegistry = (NodeRegistryInterface) registry.lookup("NodeRegistry");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar ao NodeRegistry", e);
        }

        if (lider == 1) {
            System.out.println("Processo iniciado como líder.");
            try {
                ListInterface listManager = (ListInterface) registry.lookup("ListManager");

                MulticastSender sender = new MulticastSender(listManager, this.uuid);
                sender.start();  // Inicia a thread do sender

                // Registrar o nó no NodeRegistry
                nodeRegistry.registerNode(this.uuid, listManager);

                System.out.println("Líder iniciado com sucesso.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Processo iniciado como não-líder. A sincronizar...");

            try {
                ListInterface listManager = (ListInterface) registry.lookup("ListManager");

                // Solicitar snapshot ao líder
                Hashtable<String, String> snapshot = listManager.getSnapshot();
                System.out.println("Snapshot recebido do líder: " + snapshot);

                // Inicializar receiver com o snapshot e listManager
                receiver = new MulticastReceiver(this.uuid, snapshot, listManager);
                receiver.start();  // Inicia a thread do receiver

                // Adicionar o receiver ao mapa
                receiverMap.put(this.uuid, receiver);
                nodeRegistry.addReceiver(this.uuid, receiver);

                // Imprimir o mapa
                System.out.println("Mapa de receivers: " + receiverMap);

                // Registrar o nó no NodeRegistry
                nodeRegistry.registerNode(this.uuid, listManager);

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

          
        }
    }

    public static void removeReceiver(String uuid) {
        MulticastReceiver receiver = receiverMap.get(uuid);
        // Imprimir o receiverMap
        System.out.println("Mapa de receivers antes da remoção: " + receiverMap);
        if (receiver != null) {
            receiver.endReceiver();
            receiverMap.remove(uuid);
            System.out.println("Elemento " + uuid + " removido do grupo de multicast.");

            // Remover o nó do NodeRegistry
            try {
                Registry registry = LocateRegistry.getRegistry("localhost");
                NodeRegistryInterface nodeRegistry = (NodeRegistryInterface) registry.lookup("NodeRegistry");
                nodeRegistry.removeReceiver(uuid);
                nodeRegistry.unregisterNode(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Elemento " + uuid + " não encontrado.");
        }
        // Imprimir o receiverMap após a remoção
        System.out.println("Mapa de receivers após a remoção: " + receiverMap);
    }

    public void promoteToLeader() {
        if (receiver != null) {
            receiver.stopRunning();
            receiverMap.remove(this.uuid);
            try {
                nodeRegistry.removeReceiver(this.uuid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            System.out.println("Receiver stopped for node: " + this.uuid);
        }

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            ListInterface listManager = (ListInterface) registry.lookup("ListManager");

            MulticastSender sender = new MulticastSender(listManager, this.uuid);
            sender.start();  // Inicia a thread do sender

            // Registrar o nó no NodeRegistry
            nodeRegistry.registerNode(this.uuid, listManager);

            System.out.println("Node " + this.uuid + " promoted to leader.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUuid() {
        return uuid;
    }

    public MulticastReceiver getReceiver() {
        return receiver;
    }

    public Map<String, MulticastReceiver> getReceiverMap() {
        return receiverMap;
    }
}