package Main;

import System.Elemento;

public class Main {
    public static void main(String[] args) {
        // Inicia quatro elementos
        Elemento elemento1 = new Elemento();
        Elemento elemento2 = new Elemento();
        Elemento elemento3 = new Elemento();
        Elemento elemento4 = new Elemento();

        // Adiciona os elementos ao mapa de receivers
        Elemento.getReceiverMap().put(elemento1.getUuid(), elemento1.getReceiver());
        Elemento.getReceiverMap().put(elemento2.getUuid(), elemento2.getReceiver());
        Elemento.getReceiverMap().put(elemento3.getUuid(), elemento3.getReceiver());
        Elemento.getReceiverMap().put(elemento4.getUuid(), elemento4.getReceiver());

        System.out.println("Quatro elementos foram iniciados.");

        // Adiciona um novo elemento após 30 segundos
        new Thread(() -> {
            try {
                Thread.sleep(30000); // Espera 30 segundos
                Elemento elemento5 = new Elemento();
                Elemento.getReceiverMap().put(elemento5.getUuid(), elemento5.getReceiver());
                System.out.println("Novo elemento foi adicionado após 30 segundos.");

                // Remove o novo elemento após 15 segundos
                Thread.sleep(15000); // Espera 15 segundos
                elemento5.stopReceiver();
                Elemento.getReceiverMap().remove(elemento5.getUuid());
                System.out.println("Novo elemento foi removido após 15 segundos.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}