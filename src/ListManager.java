import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ListManager extends UnicastRemoteObject implements ListInterface {
    private ArrayList<String> listaMsgs;

    protected ListManager() throws RemoteException {
        listaMsgs = new ArrayList<>();
    }

    @Override
    public ArrayList<String> allMsgs() throws RemoteException {
        return listaMsgs;
    }

    @Override
    public void addMsg(String s) throws RemoteException {
        listaMsgs.add(s);
        System.out.println("Mensagem adicionada: " + s);
    }
}
