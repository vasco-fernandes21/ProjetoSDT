package Main;

import System.Elemento;

public class TestMulticast {
    public static void main(String[] args) {
        // Inicializa o líder numa thread
        Thread liderThread = new Thread(() -> {
            System.out.println("A iniciar líder...");
            Elemento lider = new Elemento(1); // Inicia o MulticastSender
        });

        // Inicializa os não-líderes
        Thread naoLiderThread1 = new Thread(() -> {
            System.out.println("A iniciar não-líder 1...");
            Elemento naoLider1 = new Elemento(0); // Inicia o MulticastReceiver
        });

        Thread naoLiderThread2 = new Thread(() -> {
            System.out.println("A iniciar não-líder 2...");
            Elemento naoLider2 = new Elemento(0); // Inicia o MulticastReceiver
        });

        Thread naoLiderThread3 = new Thread(() -> {
            try {
                // Delay de 10 segundos antes de iniciar o terceiro não-líder
                Thread.sleep(10000);
                System.out.println("A iniciar não-líder 3 após atraso...");
                Elemento naoLider3 = new Elemento(0); // Inicia o MulticastReceiver
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Inicia as threads
        liderThread.start();
        naoLiderThread1.start();
        naoLiderThread2.start();
        naoLiderThread3.start();

        // Mantém a execução principal até que todas as threads terminem
        try {
            liderThread.join();
            naoLiderThread1.join();
            naoLiderThread2.join();
            naoLiderThread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}