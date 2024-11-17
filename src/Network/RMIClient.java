package RMISystem;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    public static void main(String[] args) {
        try {
            // Conectar ao RMI Registry no host local
            Registry registry = LocateRegistry.getRegistry("localhost");

            // Localizar o objeto remoto (ListManager) no registry
            ListInterface listManager = (ListInterface) registry.lookup("ListManager");

            // Adicionando um documento
            String document = "Documento 1";
            listManager.addElement(document);
            System.out.println("Documento enviado para o líder: " + document);

            // Verificar se o documento foi adicionado
            System.out.println("Documentos no líder: " + listManager.allMsgs());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}