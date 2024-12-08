package Network;

import RMISystem.ListInterface;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastReceiver extends Thread {
    private final String uuid;
    private final String leaderUuid;
    private final ListInterface listManager;
    private final Map<String, List<String>> documentVersions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> documentTable = new ConcurrentHashMap<>();
    private final List<String> pendingUpdates = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = true;

    public MulticastReceiver(String uuid, List<String> initialSnapshot, String leaderUuid, ListInterface listManager) {
        this.uuid = uuid;
        this.leaderUuid = leaderUuid;
        this.listManager = listManager;
        // Initialize documentVersions with the received snapshot
        for (String doc : initialSnapshot) {
            if (!doc.equals("none")) {
                documentVersions.put(uuid, new CopyOnWriteArrayList<>(Arrays.asList(doc.trim())));
                System.out.println("Initial document synchronized: " + doc.trim());
            }
        }
    }

    @Override
    public void run() {
        while (isRunning) { // Check the control flag
            try {
                // Placeholder for any RMI-based listening or processing
                Thread.sleep(1000); // Sleep to prevent tight loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void receiveSyncMessage(String syncMessage) throws RemoteException {
        System.out.println("Sync Message received: " + syncMessage);
        listManager.receiveSyncMessage(syncMessage);
    }

    public synchronized void receiveCommitMessage(String commitMessage) throws RemoteException {
        System.out.println("Commit Message received: " + commitMessage);
        listManager.receiveCommitMessage(commitMessage);
    }

    public synchronized void receiveAck(String uuid, String requestId) throws RemoteException {
        System.out.println("ACK received from: " + uuid + " for requestId: " + requestId);
        listManager.receiveAck(uuid, requestId);
    }

    public void stopRunning() {
        isRunning = false;
        System.out.println("MulticastReceiver stopped running.");
    }

    public Map<String, String> getDocumentTable() {
        return documentTable;
    }
}