import java.util.concurrent.ConcurrentHashMap;

public class GestoreSessioni {
    private static final ConcurrentHashMap<String, Utente> utentiLoggati = new ConcurrentHashMap<>();

    public GestoreSessioni() {
    }

    public boolean isLoggato(String username) {
        return utentiLoggati.containsKey(username);
    }

    public boolean login(String username, Utente utente) {
         return (utentiLoggati.putIfAbsent(username, utente) == null);
    }
    public boolean logout(String username) {
        return (utentiLoggati.remove(username) != null);
    }
}
