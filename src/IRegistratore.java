import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IRegistratore extends Remote {
    boolean register(String username, String password) throws RemoteException;
    String getUtentiRegistrati() throws RemoteException;
}
