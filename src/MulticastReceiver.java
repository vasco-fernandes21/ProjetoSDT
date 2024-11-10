import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

public class MulticastReceiver extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;
    private static final int COMMIT_PORT = 4449;
    private static final int HEARTBEAT_INTERVAL = 5000;
    private final String uuid;
    private ListManager listManager;

    public MulticastReceiver(String uuid) {
        this.uuid = uuid;
        try {
            this.listManager = new ListManager();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            socket.joinGroup(group);

            try (MulticastSocket commitSocket = new MulticastSocket(COMMIT_PORT)) {
                commitSocket.joinGroup(group);

                // Thread para enviar heartbeats periodicamente
                Thread heartbeatThread = new Thread(() -> {
                    try {
                        while (true) {
                            String heartbeatMessage = "HEARTBEAT";
                            byte[] heartbeatBuffer = heartbeatMessage.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket heartbeatPacket = new DatagramPacket(heartbeatBuffer, heartbeatBuffer.length, group, COMMIT_PORT);
                            commitSocket.send(heartbeatPacket);
                            System.out.println("Heartbeat enviado: " + heartbeatMessage);
                            Thread.sleep(HEARTBEAT_INTERVAL);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                heartbeatThread.start();

                // Thread para receber mensagens de commit
                Thread commitReceiverThread = new Thread(() -> {
                    try {
                        while (true) {
                            byte[] buffer = new byte[256];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            commitSocket.receive(packet);
                            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                            if (message.equals("COMMIT")) {
                                // Processar a mensagem de commit
                                System.out.println("Commit recebido. Versão do documento confirmada.");
                                // Adicionar lógica para confirmar a versão do documento
                                savePermanentVersion();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                commitReceiverThread.start();

                while (true) {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    List<String> messages = Arrays.asList(message.split(","));
                    for (String msg : messages) {
                        listManager.addMsg(msg);
                    }

                    System.out.println("Mensagens recebidas: " + listManager.allMsgs());

                    // Enviar mensagem de confirmação (ACK)
                    try (DatagramSocket ackSocket = new DatagramSocket()) {
                        String ackMessage = "ACK:" + uuid;
                        byte[] ackBuffer = ackMessage.getBytes(StandardCharsets.UTF_8);
                        InetAddress leaderAddress = packet.getAddress();
                        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, leaderAddress, ACK_PORT);
                        ackSocket.send(ackPacket);
                        System.out.println("Mensagem de confirmação enviada: " + ackMessage);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePermanentVersion() {
        // Lógica para salvar a versão permanente dos documentos
        List<String> permanentVersion = listManager.getClone();
        System.out.println("Versão permanente salva: " + permanentVersion);
        // Aqui você pode adicionar lógica para salvar a versão permanente em um arquivo ou banco de dados
    }
}