import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Receiver extends Thread {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int PORT = 4446;
    private static final boolean IS_LEADER = true; // Atribuir o papel de líder estaticamente

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);

            if (IS_LEADER) {
                new Thread(() -> sendHeartbeats(socket, group)).start();
            }

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());

                synchronized (this) {
                    System.out.println("Mensagem recebida: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeats(MulticastSocket socket, InetAddress group) {
        try {
            while (true) {
                String heartbeat = "HEARTBEAT";
                byte[] buffer = heartbeat.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                System.out.println("Heartbeat enviado: " + heartbeat);

                Thread.sleep(5000); // Envia heartbeat a cada 5 segundos
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}