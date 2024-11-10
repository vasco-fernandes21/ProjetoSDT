public class TestMulticast {
    public static void main(String[] args) {
        // Cria e inicia o líder numa nova thread
        Thread liderThread = new Thread(() -> {
            System.out.println("A iniciar líder...");
            Elemento lider = new Elemento(1); // Inicia o MulticastSender
        });

        // Cria e inicia múltiplos não-líderes numa nova thread
        Thread naoLiderThread1 = new Thread(() -> {
            System.out.println("A iniciar não-líder 1...");
            Elemento naoLider1 = new Elemento(0); // Inicia o MulticastReceiver
        });

        Thread naoLiderThread2 = new Thread(() -> {
            System.out.println("A iniciar não-líder 2...");
            Elemento naoLider2 = new Elemento(0); // Inicia o MulticastReceiver
        });

        Thread naoLiderThread3 = new Thread(() -> {
            System.out.println("A iniciar não-líder 3...");
            Elemento naoLider3 = new Elemento(0); // Inicia o MulticastReceiver
        });

        // Inicia todas as threads simultaneamente
        liderThread.start();
        naoLiderThread1.start();
        naoLiderThread2.start();
        naoLiderThread3.start();

        // Mantém o processo principal a correr
        try {
            // Mantém a execução indefinidamente para monitorizar a comunicação
            liderThread.join();
            naoLiderThread1.join();
            naoLiderThread2.join();
            naoLiderThread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Teste de multicast concluído.");
    }
}