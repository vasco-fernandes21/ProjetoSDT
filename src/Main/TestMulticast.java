package Main;

import System.Elemento;

public class TestMulticast {
    public static void main(String[] args) {
        // Inicializa o líder e os não-líderes diretamente, sem criar threads extras

        System.out.println("A iniciar líder...");
        Elemento lider = new Elemento(1); // O líder já inicia o MulticastSender

        // Inicializa os não-líderes
        System.out.println("A iniciar não-líder 1...");
        Elemento naoLider1 = new Elemento(0); // Não-líder 1 já inicia o receiver

        System.out.println("A iniciar não-líder 2...");
        Elemento naoLider2 = new Elemento(0); // Não-líder 2 já inicia o receiver

        System.out.println("A iniciar não-líder 3...");
        Elemento naoLider3 = new Elemento(0); // Não-líder 3 já inicia o receiver

        // Simular a falha de um elemento após 15 segundos
        new Thread(() -> {
            try {
                Thread.sleep(15000); // Espera 15 segundos antes de simular a falha
                System.out.println("A simular falha do não-líder 2...");
                naoLider2.stopReceiver(); // Chama o método que interrompe a recepção de pacotes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}