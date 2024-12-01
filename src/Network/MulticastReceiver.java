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
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final List<String> pendingUpdates = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = true;
    private MulticastSocket socket;
    private InetAddress group;

    public MulticastReceiver(String uuid, List<String> initialSnapshot) {
        this.uuid = uuid;
        // Inicializar documentVersions com o snapshot recebido
        for (String doc : initialSnapshot) {
            if (!doc.equals("none")) {
                documentVersions.put(uuid, new ArrayList<>(Arrays.asList(doc.trim())));
                System.out.println("Documento inicial sincronizado: " + doc.trim());
            }
        }
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(PORT);
            group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            while (isRunning) { // Verifica a flag de controle
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (message.startsWith("HEARTBEAT:sync:")) {
                    handleSyncMessage(message, packet);
                } else if (message.startsWith("HEARTBEAT:commit:")) {
                    handleCommitMessage();
                } else if (message.startsWith("HEARTBEAT")) {
                    sendAck(packet, message.split(":")[3]); // Ajustar se commit tiver requestId
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
        if (parts.length >= 4) { // Garantir que há requestId
            String doc = parts[2];
            String requestId = parts[3];

            List<String> docs = doc.equals("none") ? new ArrayList<>() : Arrays.asList(doc.split(","));

            if (!pendingUpdates.isEmpty()) {
                pendingUpdates.add(message);
                System.out.println("Mensagem de sincronização pendente armazenada: " + message);
            } else {
                for (String d : docs) {
                    if (!d.equals("none")) {
                        documentVersions.put(uuid, new ArrayList<>(Arrays.asList(d.trim())));
                        System.out.println("Heartbeat sincronizado com documento: " + d.trim());
                    }
                }
            }

            sendAck(packet, requestId);
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + message);
        }
    }

    private void handleCommitMessage() {
        System.out.println("Commit recebido. Confirmando e aplicando atualizações.");
        savePermanentVersion();
        applyPendingUpdates();
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
        for (Map.Entry<String, List<String>> entry : documentVersions.entrySet()) {
            String doc = String.join(",", entry.getValue());
            if (!doc.equals("none")) {
                String docId = UUID.randomUUID().toString();
                documentTable.put(docId, doc);
                System.out.println("Documento guardado permanentemente: " + doc + " com ID: " + docId);
            }
        }
        System.out.println("Versão permanente guardada: " + documentTable);
    }

    private void applyPendingUpdates() {
        if (!pendingUpdates.isEmpty()) {
            System.out.println("Aplicando atualizações pendentes...");
            for (String update : pendingUpdates) {
                String[] parts = update.split(":");
                if (parts.length >= 4) {
                    String doc = parts[2];
                    String requestId = parts[3];
                    List<String> docs = doc.equals("none") ? new ArrayList<>() : Arrays.asList(doc.split(","));
                    for (String d : docs) {
                        if (!d.equals("none")) {
                            documentVersions.put(uuid, new ArrayList<>(Arrays.asList(d.trim())));
                            System.out.println("Atualização aplicada: " + d.trim());

                            // Salva o documento permanentemente após a atualização
                            String docId = UUID.randomUUID().toString();
                            documentTable.put(docId, d.trim());
                            System.out.println("Documento guardado permanentemente: " + d.trim() + " com ID: " + docId);
                        }
                    }
                }
            }
            pendingUpdates.clear();
        }
    }

    public void stopRunning() {
        isRunning = false;
        try {
            if (socket != null && group != null) {
                socket.leaveGroup(group);
                socket.close();
                System.out.println("Socket de Multicast removido do grupo " + MULTICAST_GROUP_ADDRESS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }
}