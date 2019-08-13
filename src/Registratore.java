import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Registratore extends UnicastRemoteObject implements IRegistratore {
    private ConcurrentHashMap<String, Utente> utentiRegistrati;
    public Registratore() throws RemoteException {
        utentiRegistrati = new ConcurrentHashMap<>();
    }

    @Override
    public boolean register(String username, String password) {
        if (username == null) {
            return false;
        }
        if (password == null) {
            return false;
        }
        try {
            utentiRegistrati.putIfAbsent(username, new Utente(username, password));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public Map<String, Utente> getUtentiRegistrati() {
        return Collections.unmodifiableMap(utentiRegistrati);
    }
}
