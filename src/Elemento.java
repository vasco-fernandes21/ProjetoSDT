public class Elemento {
    private int lider;

    public Elemento(int lider) {
        this.lider = lider;

        if (this.lider == 1) {
            System.out.println("Processo iniciado como líder. A enviar mensagens...");
            MulticastSender sender = new MulticastSender();
            sender.start();
        } else {
            System.out.println("Processo iniciado como não-líder. A receber mensagens...");
            MulticastReceiver receiver = new MulticastReceiver();
            receiver.start();
        }
    }

    public void setLider(int lider) {
        this.lider = lider;
    }
}