package System;

import Network.MulticastReceiver;
import Network.MulticastSender;
import RMISystem.ListInterface;
import RMISystem.NodeRegistryInterface;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Elemento implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<String, MulticastReceiver> receiverMap = new ConcurrentHashMap<>();
    private static MulticastSender currentLeaderSender; 
    private final String uuid;
    private transient MulticastReceiver receiver;
    private transient MulticastSender sender;
    private final transient NodeRegistryInterface nodeRegistry;

    public Elemento() {
        this.uuid = UUID.randomUUID().toString();
        System.out.println("UUID do elemento: " + this.uuid);

        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost"); 
            nodeRegistry = (NodeRegistryInterface) registry.lookup("NodeRegistry");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar ao NodeRegistry", e);
        }

        System.out.println("Processo iniciado como não-líder. A sincronizar...");

        try {
            ListInterface listManager = (ListInterface) registry.lookup("ListManager");

            // Solicitar snapshot ao líder
            Hashtable<String, String> snapshot = listManager.getSnapshot();
            System.out.println("Snapshot recebido do líder: " + snapshot);

            // Solicitar atualizações pendentes ao líder
            List<String> pendingUpdates = listManager.getPendingUpdates();
            System.out.println("Atualizações pendentes recebidas do líder: " + pendingUpdates);

            // Inicializar receiver com o snapshot e listManager
            receiver = new MulticastReceiver(this.uuid, snapshot, listManager, nodeRegistry, this);
            new Thread(receiver).start();  // Inicia a thread do receiver


            // Adicionar o receiver ao NodeRegistry
            nodeRegistry.addReceiver(this.uuid, receiver);
            nodeRegistry.registerNode(this.uuid, listManager);
            receiverMap.put(this.uuid, receiver);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopReceiver() {
        if (receiver != null) {
            receiver.stopRunning();
            System.out.println("Elemento " + this.uuid + " parou de receber pacotes.");
        }
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
            System.out.println("Receiver interrompido para o nó: " + this.uuid);
        }
    
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            ListInterface listManager = (ListInterface) registry.lookup("ListManager");
    
            if (currentLeaderSender != null) {
                currentLeaderSender.stopSender();
                currentLeaderSender = null; 
            }
    
            nodeRegistry.setLeaderID(this.uuid);
    
            sender = new MulticastSender(listManager, this.uuid);
            new Thread(sender).start();  
            currentLeaderSender = sender; 
    
            System.out.println("Nó " + this.uuid + " foi promovido para líder.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                nodeRegistry.releaseElectionLock();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUuid() {
        return uuid;
    }

    public MulticastReceiver getReceiver() {
        return receiver;
    }

    public static Map<String, MulticastReceiver> getReceiverMap() {
        return receiverMap;
    }
}