package Network;

import RMISystem.ListInterface;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastReceiver extends Thread {
    private final String uuid;
    private final ListInterface listManager;
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final List<String> pendingUpdates = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = true;

    public MulticastReceiver(String uuid, List<String> initialSnapshot, ListInterface listManager) {
        this.uuid = uuid;
        this.listManager = listManager;
        // Initialize documentTable with the received snapshot
        for (String doc : initialSnapshot) {
            if (!doc.equals("none")) {
                documentTable.put(uuid, doc.trim());
                System.out.println("Initial document synchronized: " + doc.trim());
            }
        }
    }

    @Override
    public void run() {
        // Configurações do grupo multicast
        String multicastAddress = MulticastConfig.MULTICAST_ADDRESS;
        int multicastPort = MulticastConfig.MULTICAST_PORT;

        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            InetAddress group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group); // Junta-se ao grupo multicast
            System.out.println("Aguardando mensagens multicast no grupo " + multicastAddress + ":" + multicastPort);

            byte[] buffer = new byte[1024]; // Buffer para receber mensagens
            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Escuta mensagens do grupo multicast

                String message = new String(packet.getData(), 0, packet.getLength());

                // Decide qual método chamar com base no conteúdo da mensagem
                if (message.startsWith("HEARTBEAT:sync:")) {
                    listManager.receiveSyncMessage(message, uuid);
                } else if (message.startsWith("HEARTBEAT:commit:")) {
                    receiveCommitMessage(message); // Chama o método local para processar a mensagem de commit
                } else {
                    System.out.println("Mensagem desconhecida recebida: " + message);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao receber mensagens multicast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver stopped running.");
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }

    // Método local para processar a mensagem de commit
    private synchronized void receiveCommitMessage(String commitMessage) {
        System.out.println("Commit Message recebido: " + commitMessage);

        // Processa a mensagem de commit, que tem a estrutura: HEARTBEAT:commit:{uuid}:{doc}
        String[] parts = commitMessage.split(":");
        if (parts.length >= 4) {
            String commitId = parts[2]; // UUID da mensagem de commit
            String doc = parts[3]; // Documento a ser armazenado

            // Adiciona o documento na documentTable local
            if (!documentTable.containsValue(doc)) {
                documentTable.put(commitId, doc);
                System.out.println("Documento armazenado no receiver: " + doc + " com ID: " + commitId);
            }
        } else {
            System.out.println("Mensagem de commit inválida: " + commitMessage);
        }
    }
}