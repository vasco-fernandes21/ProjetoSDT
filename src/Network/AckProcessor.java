package Network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class AckProcessor extends Thread {
    private static final int ACK_PORT = 4448;
    private final Set<String> receivedAcks;

    public AckProcessor(Set<String> receivedAcks) {
        this.receivedAcks = receivedAcks;
    }

    @Override
    public void run() {
        try (DatagramSocket ackSocket = new DatagramSocket(ACK_PORT)) {
            System.out.println("Aguardando ACKs...");

            while (true) {
                byte[] ackBuffer = new byte[256];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                try {
                    ackSocket.receive(ackPacket);
                    String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);
                    System.out.println("Mensagem de ACK recebida: " + ackMessage); // Log adicional
                    if (ackMessage.startsWith("ACK:")) {
                        String ackUUID = ackMessage.split(":")[1];
                        synchronized (receivedAcks) {
                            receivedAcks.add(ackUUID);
                        }
                        System.out.println("ACK processado: " + ackMessage);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout ao esperar ACKs.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}