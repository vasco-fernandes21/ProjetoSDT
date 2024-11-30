package Network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastReceiver extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;

    private final String uuid;
    private final Map<String, List<String>> documentVersions = new ConcurrentHashMap<>();
    private final Map<String, String> documentTable = new ConcurrentHashMap<>();
    private final List<String> pendingUpdates = new CopyOnWriteArrayList<>();
    private volatile boolean isSynced;
    private volatile boolean isRunning = true; // Flag de controle

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

            while (isRunning) { // Verifica a flag de controle
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (message.startsWith("HEARTBEAT:sync:")) {
                    handleSyncMessage(message, packet);
                } else if (message.equals("HEARTBEAT:commit")) {
                    handleCommitMessage();
                } else if (message.startsWith("HEARTBEAT")) {
                    sendAck(packet, message.split(":")[1]);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                e.printStackTrace();
            }
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
                for (String doc : docs) {
                    if (!doc.equals("none")) {
                        documentVersions.put(uuid, new ArrayList<>(Arrays.asList(doc.trim())));
                        System.out.println("Heartbeat sincronizado com documento: " + doc.trim());
                    }
                }
            }

            sendAck(packet, parts[3]);
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + message);
        }
    }

    private void handleCommitMessage() {
        System.out.println("Commit recebido. Confirmando e aplicando atualizações.");

        if (!isSynced) {
            applyPendingUpdates();
            isSynced = true;
        } else {
            savePermanentVersion();
        }
    }

    private void applyPendingUpdates() {
        if (!pendingUpdates.isEmpty()) {
            System.out.println("Aplicando atualizações pendentes...");
            for (String update : pendingUpdates) {
                String[] parts = update.split(":");
                if (parts.length >= 3) {
                    List<String> docs = Arrays.asList(parts[2].split(","));
                    for (String doc : docs) {
                        if (!doc.equals("none")) {
                            documentVersions.put(uuid, new ArrayList<>(Arrays.asList(doc.trim())));
                            System.out.println("Atualização aplicada: " + doc.trim());

                            // Agora, salva o documento permanentemente após a atualização
                            String docId = UUID.randomUUID().toString();
                            documentTable.put(docId, doc.trim());
                            System.out.println("Documento guardado permanentemente: " + doc.trim() + " com ID: " + docId);
                        }
                    }
                }
            }
            pendingUpdates.clear();
        }
    }

    private void sendAck(DatagramPacket packet, String requestId) throws IOException {
        try (DatagramSocket ackSocket = new DatagramSocket()) {
            String ackMessage = "ACK:" + uuid + ":" + requestId;
            byte[] ackBuffer = ackMessage.getBytes(StandardCharsets.UTF_8);
            InetAddress leaderAddress = packet.getAddress();
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, leaderAddress, ACK_PORT);
            ackSocket.send(ackPacket);
            System.out.println("ACK enviado: " + ackMessage);
        }
    }

    private void savePermanentVersion() {
        List<String> docs = documentVersions.get(uuid);
        if (docs != null) {
            for (String doc : docs) {
                if (!doc.equals("none")) {
                    String docId = UUID.randomUUID().toString();
                    documentTable.put(docId, doc);
                    System.out.println("Documento guardado permanentemente: " + doc + " com ID: " + docId);
                }
            }
        }
        System.out.println("Versão permanente guardada: " + documentTable);
    }

    public void stopRunning() {
        isRunning = false;
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }

    public Map<String, List<String>> getDocumentVersions() {
        return documentVersions;
    }
}