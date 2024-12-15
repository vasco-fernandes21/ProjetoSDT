package RMISystem;

import Network.MulticastReceiver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry extends UnicastRemoteObject implements NodeRegistryInterface {
    private final Map<String, ListInterface> nodes = new ConcurrentHashMap<>();
    private final Map<String, MulticastReceiver> receiverMap = new ConcurrentHashMap<>();
    private String leaderID;
    private boolean electionLock = false;

    public NodeRegistry() throws RemoteException {
        super();
    }

    @Override
    public void registerNode(String nodeId, ListInterface node) throws RemoteException {
        nodes.put(nodeId, node);
    }


    @Override
    public Set<String> getNodeIds() throws RemoteException {
        return nodes.keySet();
    }

    @Override
    public void addReceiver(String nodeId, MulticastReceiver receiver) throws RemoteException {
        receiverMap.put(nodeId, receiver);
    }

    @Override
    public void removeReceiver(String nodeId) throws RemoteException {
        MulticastReceiver receiver = receiverMap.get(nodeId);
        if (receiver != null) {
            receiverMap.remove(nodeId);
            nodes.remove(nodeId);
            System.out.println("Elemento " + nodeId + " removido do grupo de multicast.");
        }
    }

    @Override
    public void setLeaderID(String leaderID) throws RemoteException {
        this.leaderID = leaderID;
        System.out.println("Líder definido com ID: " + leaderID);
    }

    @Override
    public Set<String> getReceivers() throws RemoteException {
        return receiverMap.keySet();
    }

    @Override
    public synchronized boolean acquireElectionLock() throws RemoteException {
        if (!electionLock) {
            electionLock = true;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void releaseElectionLock() throws RemoteException {
        electionLock = false;
    }
}