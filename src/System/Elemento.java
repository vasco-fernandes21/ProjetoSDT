package System;

import Network.MulticastReceiver;
import Network.MulticastSender;
import Network.AckProcessor;
import RMISystem.ListInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

                Set<String> activeNodes = ConcurrentHashMap.newKeySet();
                AckProcessor ackProcessor = new AckProcessor(activeNodes, 2000, 3);

                // Adicionar nós ativos ao AckProcessor
                activeNodes.forEach(ackProcessor::addNode);

                MulticastSender sender = new MulticastSender(listManager, activeNodes, ackProcessor);
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

                // Inicializar lista local com o snapshot
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
        }
    }
}