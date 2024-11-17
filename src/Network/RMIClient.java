package RMISystem;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    private ListInterface listManager;

    public RMIClient() {
        try {
            // Conectar ao RMI Registry no host local
            Registry registry = LocateRegistry.getRegistry("localhost");
            // Localizar o objeto remoto (ListManager) no registry
            listManager = (ListInterface) registry.lookup("ListManager");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDoc(String document) {
        try {
            listManager.addElement(document);
            System.out.println("Documento enviado para o líder: " + document);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        RMIClient client = new RMIClient();
        // Adicionando um documento
        String document = "Documento 1";
        client.addDoc(document);

        // Verificar se o documento foi adicionado
        try {
            System.out.println("Documentos no líder: " + client.listManager.allMsgs());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}