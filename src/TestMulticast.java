import java.util.ArrayList;
import java.util.List;

public class TestMulticast {
    public static void runTest() {
        List<String> listaMsgs = new ArrayList<>();
        listaMsgs.add("Mensagem teste");

        Elemento elemento = new Elemento(listaMsgs);
        elemento.enviarMensagens();

        // Iniciar apenas um receptor
        new Thread(new Receiver()).start();

        System.out.println("Teste de multicast iniciado com 1 receptor.");
    }

    public static void main(String[] args) {
        runTest();
    }
}