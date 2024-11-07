import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Receiver extends Thread {
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int PORT = 4446;

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());

                synchronized (this) {  // Sincroniza o bloco de receção e impressão da mensagem
                    System.out.println("Mensagem recebida: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}