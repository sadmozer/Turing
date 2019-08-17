import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRegistratore extends Remote {
    Operazione registra(String username, String password) throws RemoteException;
    boolean isRegistrato(String username) throws RemoteException;
//    String getUtentiRegistrati() throws RemoteException;
}
