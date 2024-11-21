package Network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

public class MulticastReceiver extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;

    private final String uuid;
    private final Map<String, List<String>> documentVersions = new HashMap<>();
    private final Hashtable<String, String> documentTable = new Hashtable<>();
    private final List<String> pendingUpdates = new ArrayList<>();
    private boolean isSynced;

    public MulticastReceiver(String uuid, List<String> initialSnapshot) {
        this.uuid = uuid;
        this.isSynced = false;

        if (initialSnapshot != null) {
            documentVersions.put(uuid, new ArrayList<>(initialSnapshot));
            System.out.println("Snapshot inicial recebido: " + initialSnapshot);
        }
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);
            
            System.out.println("MulticastReceiver iniciado e a ouvir...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                System.out.println("Pacote recebido: " + message);

                if (message.startsWith("HEARTBEAT:sync:")) {
                    handleSyncMessage(message, packet);
                } else if (message.equals("HEARTBEAT:commit")) {
                    handleCommitMessage();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSyncMessage(String message, DatagramPacket packet) throws IOException {
        String[] parts = message.split(":");
        if (parts.length >= 3) {
            List<String> docs = parts[2].equals("none") ? new ArrayList<>() : Arrays.asList(parts[2].split(","));

            if (!isSynced) {
                pendingUpdates.add(message);
                System.out.println("Mensagem de sincronização pendente armazenada: " + message);
            } else {
                documentVersions.put(uuid, new ArrayList<>(docs));
                System.out.println("Heartbeat sincronizado com documentos: " + docs);
            }

            sendAck(packet);
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + message);
        }
    }

    private void handleCommitMessage() {
        System.out.println("Commit recebido. A confirmar e aplicar atualizações.");

        if (!isSynced) {
            applyPendingUpdates();
            isSynced = true;
        } else {
            savePermanentVersion();
        }
    }

    private void applyPendingUpdates() {
        System.out.println("A aplicar atualizações pendentes...");
        for (String update : pendingUpdates) {
            String[] parts = update.split(":");
            if (parts.length >= 3) {
                List<String> docs = Arrays.asList(parts[2].split(","));
                documentVersions.put(uuid, new ArrayList<>(docs));
                System.out.println("Atualização aplicada: " + docs);
            }
        }
        pendingUpdates.clear();
    }

    private void sendAck(DatagramPacket packet) throws IOException {
        try (DatagramSocket ackSocket = new DatagramSocket()) {
            String ackMessage = "ACK:" + uuid;
            byte[] ackBuffer = ackMessage.getBytes(StandardCharsets.UTF_8);
            InetAddress leaderAddress = packet.getAddress();
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, leaderAddress, ACK_PORT);
            ackSocket.send(ackPacket);
            System.out.println("Mensagem de confirmação enviada: " + ackMessage);
        }
    }

    private void savePermanentVersion() {
        List<String> docs = documentVersions.get(uuid);
        if (docs != null) {
            for (String doc : docs) {
                String docId = UUID.randomUUID().toString();
                documentTable.put(docId, doc);
                System.out.println("Documento guardado permanentemente: " + doc + " com ID: " + docId);
            }
        }
        System.out.println("Versão permanente guardada: " + documentTable);
    }

    public Hashtable<String, String> getDocumentTable() {
        return documentTable;
    }

    public Map<String, List<String>> getDocumentVersions() {
        return documentVersions;
    }
}