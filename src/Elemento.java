import java.util.List;

public class Elemento {
    private int lider = 1;
    private SendTransmitter transmitter;

    public Elemento(List<String> listaMsgs) {
        this.transmitter = new SendTransmitter(listaMsgs);
    }

    public int getLider() {
        return lider;
    }

    public void enviarMensagens() {
        if (lider == 1) { // Supondo que lider == 1 significa que este elemento é o líder
            transmitter.start();
        }
    }
}