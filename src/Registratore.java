import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class Registratore extends UnicastRemoteObject implements IRegistratore {
    private ConcurrentHashMap<String, Utente> utentiRegistrati;

    public Registratore() throws RemoteException {
        utentiRegistrati = new ConcurrentHashMap<>();
    }

    @Override
    public Operazione registra(String username, String password) {
        // Controllo argomenti
        if (username == null) {
            return Operazione.UsernameMancante;
        }
        if (password == null) {
            return Operazione.PasswordMancante;
        }

        // Registro il nuovo utente, se non lo è gia'
        Utente res;
        try {
            res = utentiRegistrati.putIfAbsent(username, new Utente(username, password));
        } catch (Exception e) {
            e.printStackTrace();
            return Operazione.ErroreGenerico;
        }
        if (res != null) {
            return Operazione.GiaRegistrato;
        } else {
            return Operazione.Successo;
        }
    }

    @Override
    public boolean isRegistrato(String username) {
        // Controllo argomenti
        if(username == null) {
            return false;
        }

        // Controllo se l'utente è registrato
        boolean res;
        try {
            res = utentiRegistrati.containsKey(username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }
}
