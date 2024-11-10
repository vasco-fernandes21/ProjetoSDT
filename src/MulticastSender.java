import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447; 

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
            while (true) {
                ListManager list = new ListManager();
                list.addMsg("Teste 1");
                list.addMsg("Teste 2");

                String message = String.join(",", list.allMsgs());

                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                System.out.println("Mensagem enviada: " + message);

                // Envia a cada 5 segundos
                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}