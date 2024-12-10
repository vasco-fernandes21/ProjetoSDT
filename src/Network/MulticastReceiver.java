package Network;

import RMISystem.ListInterface;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastReceiver extends Thread {
    private final String uuid;
    private final ListInterface listManager;
    private final Map<String, List<String>> documentVersions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final List<String> pendingUpdates = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = true;

    public MulticastReceiver(String uuid, List<String> initialSnapshot, ListInterface listManager) {
        this.uuid = uuid;
        this.listManager = listManager;
        // Initialize documentVersions with the received snapshot
        for (String doc : initialSnapshot) {
            if (!doc.equals("none")) {
                documentVersions.put(uuid, new CopyOnWriteArrayList<>(Arrays.asList(doc.trim())));
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
                //se receber alguma mensagem no grupo multicast avisa que recebeu
                System.out.println("Mensagem recebida no grupo multicast");

                String syncMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Sync Message recebido: " + syncMessage);

                // Processa a mensagem de sincronização
                listManager.receiveSyncMessage(syncMessage, uuid);
            }
        } catch (Exception e) {
            System.out.println("Erro ao receber mensagens multicast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void receiveCommitMessage(String commitMessage) throws RemoteException {
        System.out.println("Commit Message received: " + commitMessage);
        listManager.receiveCommitMessage(commitMessage);
    }

    public synchronized void receiveAck(String uuid, String requestId) throws RemoteException {
        System.out.println("ACK received from: " + uuid + " for requestId: " + requestId);
        listManager.receiveAck(uuid, requestId);
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver stopped running.");
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }
}