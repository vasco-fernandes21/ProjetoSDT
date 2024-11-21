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
    private Map<String, List<String>> documentVersions = new HashMap<>();
    private Hashtable<String, String> documentTable = new Hashtable<>();

    public MulticastReceiver(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (message.startsWith("HEARTBEAT:sync:")) {
                    String[] parts = message.split(":");
                    if (parts.length >= 3) {
                        List<String> docs = parts[2].equals("none") ? new ArrayList<>() : Arrays.asList(parts[2].split(","));
                        documentVersions.put(uuid, docs);
                        System.out.println("Heartbeat recebido com documentos: " + docs);

                        // Enviar mensagem de confirmação (ACK)
                        try (DatagramSocket ackSocket = new DatagramSocket()) {
                            String ackMessage = "ACK:" + uuid;
                            byte[] ackBuffer = ackMessage.getBytes(StandardCharsets.UTF_8);
                            InetAddress leaderAddress = packet.getAddress();
                            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, leaderAddress, ACK_PORT);
                            ackSocket.send(ackPacket);
                            System.out.println("Mensagem de confirmação enviada: " + ackMessage);
                        }
                    } else {
                        System.out.println("Mensagem de heartbeat inválida: " + message);
                    }
                } else if (message.equals("HEARTBEAT:commit")) {
                    // Processar a mensagem de commit
                    System.out.println("Commit recebido. Versão do documento confirmada.");
                    savePermanentVersion();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePermanentVersion() {
        // Lógica para guardar a versão permanente dos documentos
        List<String> docs = documentVersions.get(uuid);
        if (docs != null) {
            for (String doc : docs) {
                String docId = UUID.randomUUID().toString();
                documentTable.put(docId, doc);
                System.out.println("Documento guardado permanentemente: " + doc + " com ID: " + docId);
            }
        }
        System.out.println("Versão permanente guardada: " + documentTable);
        // Lógica para uma base de dados ou documento
    }

    // Métodos para acessar o conteúdo da Hashtable e do HashMap
    public Hashtable<String, String> getDocumentTable() {
        return documentTable;
    }

    public Map<String, List<String>> getDocumentVersions() {
        return documentVersions;
    }
}