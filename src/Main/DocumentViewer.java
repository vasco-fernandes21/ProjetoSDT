package Network;

public class DocumentViewer {
    public static void main(String[] args) {
        // Supondo que você tenha uma instância do MulticastReceiver
        MulticastReceiver receiver = new MulticastReceiver("some-uuid");

        // Exibir o conteúdo da Hashtable
        System.out.println("Conteúdo da Hashtable (documentTable):");
        receiver.getDocumentTable().forEach((key, value) -> {
            System.out.println("ID: " + key + ", Documento: " + value);
        });

        // Exibir o conteúdo do HashMap
        System.out.println("\nConteúdo do HashMap (documentVersions):");
        receiver.getDocumentVersions().forEach((key, value) -> {
            System.out.println("UUID: " + key + ", Documentos: " + value);
        });
    }
}