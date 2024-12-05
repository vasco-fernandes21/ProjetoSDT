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
import java.util.UUID;

public class Elemento {
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

                // Inicializar o receiver para obter o group e o socket
                receiver = new MulticastReceiver(this.uuid, new ArrayList<>());
                receiver.start();

                InetAddress group = receiver.getGroup();
                MulticastSocket multicastSocket = receiver.getSocket();

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
                receiver = new MulticastReceiver(this.uuid, snapshot);
                receiver.start();  // Inicia a thread do receiver

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

    public String getUuid() {
        return uuid;
    }

    public MulticastReceiver getReceiver() {
        return receiver;
    }
}