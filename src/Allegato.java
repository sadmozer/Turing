public class Allegato {
    private Messaggio messaggio = null;
    private Utente utente = null;
    private boolean utenteSconosciuto = true;

    public Allegato(Messaggio messaggio, Utente utente, boolean utenteSconosciuto) {
        this.messaggio = messaggio;
        this.utente = utente;
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

}
