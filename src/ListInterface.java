import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ListInterface extends Remote {
    void addMsg(String s) throws RemoteException;
    ArrayList<String> allMsgs() throws RemoteException;
}
