package Network;

import RMISystem.ListInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientTest {
    private ListInterface listManager;

    public ClientTest() {
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

    public void updateDoc(String oldDocument, String newDocument) {
        try {
            listManager.updateElement(oldDocument, newDocument);
            System.out.println("Documento atualizado para o líder: " + oldDocument + " para " + newDocument);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeDoc(String document) {
        try {
            listManager.removeElement(document);
            System.out.println("Documento removido do líder: " + document);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientTest client = new ClientTest();


        // Atualizando um documento
        client.updateDoc("Documento 2", "Documento 2 Atualizado");

        // Removendo um documento
        client.removeDoc("Documento 3");

        // Verificar se os documentos foram adicionados
        try {
            System.out.println("Documentos no líder: " + client.listManager.allMsgs());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}