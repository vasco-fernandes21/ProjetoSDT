package Main;

import System.Elemento;

public class TestMulticast {
    public static void main(String[] args) {
        // Inicia três elementos
        Elemento elemento1 = new Elemento();
        Elemento elemento2 = new Elemento();
        Elemento elemento3 = new Elemento();

        // Adiciona os elementos ao mapa de receivers
        Elemento.getReceiverMap().put(elemento1.getUuid(), elemento1.getReceiver());
        Elemento.getReceiverMap().put(elemento2.getUuid(), elemento2.getReceiver());
        Elemento.getReceiverMap().put(elemento3.getUuid(), elemento3.getReceiver());

        System.out.println("Três elementos foram iniciados.");
    }
}