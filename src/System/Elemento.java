package System;

import Network.AckProcessor;
import Network.MulticastReceiver;
import Network.MulticastSender;
import RMISystem.ListInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
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

                InetAddress group = InetAddress.getByName("224.0.0.1");
                MulticastSocket multicastSocket = new MulticastSocket(4447);
                multicastSocket.joinGroup(group);

                AckProcessor ackProcessor = new AckProcessor(4448, this.uuid, multicastSocket, group);
                MulticastSender sender = new MulticastSender(listManager, ackProcessor);
                sender.start();  // Inicia a thread do sender
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

                // Inicializar receiver com o snapshot
                receiver = new MulticastReceiver(this.uuid, snapshot, null);
                receiver.start();  // Inicia a thread do receiver

                // Adicionar o receiver ao mapa
                receiverMap.put(this.uuid, receiver);

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

    public static void stopReceiverById(String uuid) {
        MulticastReceiver receiver = receiverMap.get(uuid);
        if (receiver != null) {
            receiver.leaveGroup(); // Ensure the group is left before stopping the receiver
            receiver.stopRunning();
            receiverMap.remove(uuid);
            System.out.println("Elemento " + uuid + " removido do grupo de multicast.");
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