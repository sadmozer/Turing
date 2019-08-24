import java.util.ArrayList;
import java.util.HashMap;

public class GestoreSessioni {
    private static final HashMap<String, Utente> utentiLoggati = new HashMap<>();
    private static final HashMap<Utente, Allegato> allegatoPerUtente = new HashMap<>();
    private HashMap<Utente, ArrayList<Messaggio>> notifichePerUtente = new HashMap<>();

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

    public boolean addAllegato(Allegato allegato, Utente utente) {
        return allegatoPerUtente.putIfAbsent(utente, allegato) == null;
    }

    public Allegato getAllegato(String username) {
        return allegatoPerUtente.getOrDefault(username, null);
    }

    public boolean haNotifiche(Utente utente) {
        if (notifichePerUtente.get(utente) == null) {
            return false;
        }
        return !notifichePerUtente.get(utente).isEmpty();
    }

    public Messaggio popNotifica(Utente utente) {
        return notifichePerUtente.get(utente).get(0);
    }
    public void addNotifica(Messaggio notificaDaAggiungere, Utente utente) {
        if (notifichePerUtente.get(utente) == null) {
            notifichePerUtente.put(utente, new ArrayList<>());
        }
        notifichePerUtente.get(utente).add(notificaDaAggiungere);
    }
}
