import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private ArrayList<String> listaMsgs;

    protected ListManager() throws RemoteException {
        listaMsgs = new ArrayList<>();
    }

    @Override
    public synchronized ArrayList<String> allMsgs() throws RemoteException {
        return listaMsgs;
    }

    @Override
    public synchronized void addMsg(String s) throws RemoteException {
        listaMsgs.add(s);
        System.out.println("Mensagem adicionada: " + s);
    }

    public synchronized void addElement(String s) {
        listaMsgs.add(s);
        System.out.println("Elemento adicionado: " + s);
    }

    public synchronized void removeElement(String s) {
        listaMsgs.remove(s);
        System.out.println("Elemento removido: " + s);
    }

    public synchronized List<String> getClone() {
        return new ArrayList<>(listaMsgs);
    }

    public synchronized List<String> createSendStructure() {
        // Retorna uma cópia da lista de mensagens para envio
        return new ArrayList<>(listaMsgs);
    }
}