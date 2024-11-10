import java.util.UUID;

public class Elemento {
    private int lider;
    private final String uuid;

    public Elemento(int lider) {
        this.lider = lider;
        this.uuid = UUID.randomUUID().toString(); // Gera um UUID fixo para o elemento
        System.out.println("UUID do elemento: " + this.uuid);

        if (this.lider == 1) {
            System.out.println("Processo iniciado como líder. A enviar mensagens...");
            MulticastSender sender = new MulticastSender();
            sender.start();
        } else {
            System.out.println("Processo iniciado como não-líder. A receber mensagens...");
            MulticastReceiver receiver = new MulticastReceiver(this.uuid);
            receiver.start();
        }
    }

    public void setLider(int lider) {
        this.lider = lider;
    }

    public String getUuid() {
        return uuid;
    }
}