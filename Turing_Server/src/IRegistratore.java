import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia remota che mostra le funzioni di registrazione
 *
 * @author Niccolo' Cardelli 534015
 */
public interface IRegistratore extends Remote {
    boolean registra(String username, String password) throws RemoteException;
    boolean isRegistrato(String username) throws RemoteException;
}
