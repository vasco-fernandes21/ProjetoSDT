public class TestMulticast {
    public static void main(String[] args) {
        // Cria e inicia o líder numa nova thread
        Thread liderThread = new Thread(() -> {
            System.out.println("A iniciar líder...");
            Elemento lider = new Elemento(1); // Inicia o MulticastSender
        });

        // Cria e inicia o não-líder numa nova thread
        Thread naoLiderThread = new Thread(() -> {
            System.out.println("A iniciar não-líder...");
            Elemento naoLider = new Elemento(0); // Inicia o MulticastReceiver
        });

        // Inicia ambas as threads simultaneamente
        liderThread.start();
        naoLiderThread.start();

        // Mantém o processo principal a correr
        try {
            // Mantém a execução indefinidamente para monitorizar a comunicação
            liderThread.join();
            naoLiderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Teste de multicast concluído.");
    }
}