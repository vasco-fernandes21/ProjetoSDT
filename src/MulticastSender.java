import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class MulticastSender extends Thread {
    private static final String MULTICAST_GROUP_ADDRESS = "224.0.0.1";
    private static final int PORT = 4447;
    private static final int ACK_PORT = 4448;
    private static final int COMMIT_PORT = 4449;
    private static final int REQUIRED_ACKS = 3; // Requer 3 ACKs com UUIDs diferentes

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);

            Thread ackReceiverThread = new Thread(() -> {
                try (DatagramSocket ackSocket = new DatagramSocket(ACK_PORT)) {
                    Set<String> receivedAcks = new HashSet<>();
                    while (true) {
                        byte[] ackBuffer = new byte[256];
                        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                        ackSocket.receive(ackPacket);
                        String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);
                        String ackUuid = ackMessage.split(":")[1]; // Extrair o UUID do ACK
                        System.out.println("ACK recebido de UUID: " + ackUuid);

                        receivedAcks.add(ackUuid);

                        System.out.println("Número de ACKs únicos recebidos: " + receivedAcks.size());

                        if (receivedAcks.size() >= REQUIRED_ACKS) {
                            String commitMessage = "COMMIT";
                            byte[] commitBuffer = commitMessage.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket commitPacket = new DatagramPacket(commitBuffer, commitBuffer.length, group, COMMIT_PORT);
                            socket.send(commitPacket);
                            System.out.println("Mensagem de commit enviada: " + commitMessage);

                            receivedAcks.clear(); // Limpar para próxima rodada
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            ackReceiverThread.start();

            while (true) {
                ListManager list = new ListManager();
                list.addMsg("Teste 1");
                list.addMsg("Teste 2");

                String message = String.join(",", list.allMsgs());
                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                System.out.println("Mensagem enviada: " + message);

                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}