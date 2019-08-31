import java.util.ArrayList;
import java.util.HashMap;

class GestoreSessioni {
    private static final HashMap<String, Utente> utentiLoggati = new HashMap<>();
    private static final HashMap<Utente, Allegato> allegatoPerUtente = new HashMap<>();
    private HashMap<Utente, ArrayList<Messaggio>> notifichePerUtente = new HashMap<>();

    GestoreSessioni() {
    }

    boolean isLoggato(Utente utente) {
        return utentiLoggati.containsValue(utente);
    }

    boolean login(Utente utente) {
         return (utentiLoggati.putIfAbsent(utente.getUsername(), utente) == null);
    }
    boolean logout(Utente utente) {
        return (utentiLoggati.remove(utente.getUsername()) != null);
    }

    boolean addAllegato(Allegato allegato, Utente utente) {
        return allegatoPerUtente.putIfAbsent(utente, allegato) == null;
    }

    boolean haNotifiche(Utente utente) {
        if (notifichePerUtente.get(utente) == null) {
            return false;
        }
        return !notifichePerUtente.get(utente).isEmpty();
    }

    Messaggio popNotifica(Utente utente) {
        return notifichePerUtente.get(utente).remove(0);
    }
    void addNotifica(Messaggio notificaDaAggiungere, Utente utente) {
        if (notifichePerUtente.get(utente) == null) {
            notifichePerUtente.put(utente, new ArrayList<>());
        }
        notifichePerUtente.get(utente).add(notificaDaAggiungere);
    }
}
