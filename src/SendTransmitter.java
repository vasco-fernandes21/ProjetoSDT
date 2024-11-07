import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class SendTransmitter extends Thread {
    private List<String> listaMsgs;
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int PORT = 4446;

    public SendTransmitter(List<String> listaMsgs) {
        this.listaMsgs = listaMsgs;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            while (true) {
                StringBuilder sb = new StringBuilder();
                for (String msg : listaMsgs) {
                    sb.append(msg).append(" ");
                }
                byte[] buffer = sb.toString().getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                System.out.println("Heartbeat enviado: " + sb.toString());

                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}