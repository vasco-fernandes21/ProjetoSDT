import java.util.ArrayList;
import java.util.List;

public class TestMulticast {

    public static void runTest() {
        List<String> listaMsgs = new ArrayList<>();
        listaMsgs.add("Mensagem teste");

        SendTransmitter transmitter = new SendTransmitter(listaMsgs);
        transmitter.start();

        int numReceivers = 3;
        for (int i = 0; i < numReceivers; i++) {
            new Thread(new Receiver()).start();
        }

        System.out.println("Teste de multicast iniciado com " + numReceivers + " receptores.");
    }

    public static void main(String[] args) {
        runTest();
    }
}