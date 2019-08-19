public class Allegato {
    private Messaggio messaggio = null;
    private Utente utente = null;
    private boolean utenteSconosciuto = true;

    public Allegato(Messaggio messaggio, Utente utente, boolean utenteSconosciuto) {
        this.messaggio = messaggio;
        this.utente = utente;
        this.utenteSconosciuto = utenteSconosciuto;
    }

    public Allegato() {
    }

    public Utente getUtente() {
        return utente;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
    }

    public Messaggio getMessaggio() {
        return messaggio;
    }

    public void setMessaggio(Messaggio messaggio) {
        this.messaggio = messaggio;
    }

    public boolean isUtenteSconosciuto() {
        return utenteSconosciuto;
    }

    public void setUtenteSconosciuto(boolean utenteSconosciuto) {
        this.utenteSconosciuto = utenteSconosciuto;
    }
}
