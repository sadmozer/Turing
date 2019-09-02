import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class GestoreSessioni {
    private static final HashSet<Utente> utentiLoggati = new HashSet<>();
    private static final HashMap<Utente, Allegato> allegatoPerUtente = new HashMap<>();
    private HashMap<Utente, ArrayList<Messaggio>> notifichePerUtente = new HashMap<>();

    GestoreSessioni() {
    }

    boolean isLoggato(Utente utente) {
        return utentiLoggati.contains(utente);
    }

    boolean login(Utente utente) {
         return utentiLoggati.add(utente);
    }
    boolean logout(Utente utente) {
        return utentiLoggati.remove(utente);
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
