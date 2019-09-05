import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Si occupa della registrazione dei Client
 *
 * @author Niccolo' Cardelli 534015
 */
public class Registratore extends UnicastRemoteObject implements IRegistratore {
    private ConcurrentHashMap<String, Utente> utentiRegistrati;

    Registratore() throws RemoteException {
        utentiRegistrati = new ConcurrentHashMap<>();
    }

    @Override
    public boolean registra(String username, String password) {
        return utentiRegistrati.putIfAbsent(username, new Utente(username, password)) == null;
    }

    @Override
    public boolean isRegistrato(String username) {
        return utentiRegistrati.containsKey(username);
    }

    Utente getUtente(String username) {
        return utentiRegistrati.getOrDefault(username, null);
    }

}
