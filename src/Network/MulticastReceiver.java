package Network;

import RMISystem.ListInterface;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class MulticastReceiver extends Thread implements Serializable {
    private static final long serialVersionUID = 1L; // Adicione um serialVersionUID
    private final String uuid;
    private final ListInterface listManager;
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final Hashtable<String, String> tempFiles = new Hashtable<>();

    private transient volatile boolean isRunning = true; // Marque como transient
    private transient MulticastSocket socket; // Marque como transient
    private transient InetAddress group; // Marque como transient

    public MulticastReceiver(String uuid, Hashtable<String, String> initialSnapshot, ListInterface listManager) {
        this.uuid = uuid;
        this.listManager = listManager;
        // Initialize documentTable with the received snapshot
        for (Map.Entry<String, String> entry : initialSnapshot.entrySet()) {
            documentTable.put(entry.getKey(), entry.getValue().trim());
            System.out.println("Receiver sincronizado: " + entry.getValue().trim());
        }
    }

    @Override
    public void run() {
        // Configurações do grupo multicast
        String multicastAddress = MulticastConfig.MULTICAST_ADDRESS;
        int multicastPort = MulticastConfig.MULTICAST_PORT;

        try {
            socket = new MulticastSocket(multicastPort);
            group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group); // Junta-se ao grupo multicast
            System.out.println("Aguardando mensagens multicast no grupo " + multicastAddress + ":" + multicastPort);

            byte[] buffer = new byte[1024]; // Buffer para receber mensagens
            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Escuta mensagens do grupo multicast

                String message = new String(packet.getData(), 0, packet.getLength());

                // Decide qual método chamar com base no conteúdo da mensagem
                if (message.startsWith("HEARTBEAT:sync:")) {
                    receiveSyncMessage(message);
                } else if (message.startsWith("HEARTBEAT:commit:")) {
                    receiveCommitMessage(message); // Chama o método local para processar a mensagem de commit
                } else {
                    System.out.println("Mensagem desconhecida recebida: " + message);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao receber mensagens multicast: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver parado.");
    }

    public void endReceiver() {
        isRunning = false;
        if (socket != null && group != null) {
            try {
                socket.leaveGroup(group); // Sai do grupo multicast
                socket.close(); // Fecha o socket
                System.out.println("MulticastReceiver removido do grupo multicast e thread terminada.");
            } catch (Exception e) {
                System.out.println("Erro ao sair do grupo multicast: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }

    public Hashtable<String, String> getTempFiles() {
        return tempFiles;
    }

    private synchronized void receiveSyncMessage(String syncMessage) {
        if (!isRunning) {
            return; // Não processa a mensagem se o receiver não estiver rodando
        }

        System.out.println("Sync Message recebido: " + syncMessage);

        // Processa a mensagem de sincronização
        String[] parts = syncMessage.split(":");
        if (parts.length >= 4) {
            String doc = parts[2]; // Documento a ser sincronizado
            String requestId = parts[3]; // ID do request para o ACK

            // Armazena o documento na estrutura tempFiles
            String tempFileId = UUID.randomUUID().toString();
            tempFiles.put(tempFileId, doc.trim());
            System.out.println("Documento temporário armazenado no receiver: " + doc.trim() + " com ID: " + tempFileId);

            // Envia ACK para o líder confirmando o recebimento
            try {
                listManager.sendAck(uuid, requestId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Mensagem de heartbeat inválida: " + syncMessage);
        }
    }

    // Método local para processar a mensagem de commit
    private synchronized void receiveCommitMessage(String commitMessage) {
        if (!isRunning) {
            return; // Não processa a mensagem se o receiver não estiver rodando
        }

        System.out.println("Commit Message recebido: " + commitMessage);

        // Processa a mensagem de commit, que tem a estrutura: HEARTBEAT:commit:{uuid}:{doc}
        String[] parts = commitMessage.split(":");
        if (parts.length >= 4) {
            String commitId = parts[2]; // UUID da mensagem de commit
            String doc = parts[3]; // Documento a ser armazenado

            // Verifica se o documento está na tempFiles antes de adicioná-lo à documentTable
            if (tempFiles.containsValue(doc)) {
                documentTable.put(commitId, doc);
                System.out.println("Documento confirmado no receiver: " + doc + " com ID: " + commitId);
                // Remove o documento da tempFiles após adicioná-lo à documentTable
                tempFiles.values().remove(doc);
            } else {
                System.out.println("Documento não encontrado na tempFiles: " + doc);
            }

            // Mover documentos restantes de tempFiles para documentTable
            for (Map.Entry<String, String> entry : tempFiles.entrySet()) {
                if (!documentTable.containsValue(entry.getValue())) {
                    documentTable.put(entry.getKey(), entry.getValue());
                    System.out.println("Documento confirmado no receiver: " + entry.getValue() + " com ID: " + entry.getKey());
                }
            }

            // Limpar tempFiles após o commit
            tempFiles.clear();
            System.out.println("tempFiles limpo após o commit.");
        } else {
            System.out.println("Mensagem de commit inválida: " + commitMessage);
        }
    }
}