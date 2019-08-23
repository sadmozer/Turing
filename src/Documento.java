import java.util.concurrent.ConcurrentHashMap;

public class Documento {
    private Utente creatore;
    private int numSezioni;
    private String nomeDocumento;
    private Sezione[] sezioni;

    public Documento(String nomeDocumento, Utente creatore, int numSezioni) {
        this.nomeDocumento = nomeDocumento;
        this.creatore = creatore;
        this.numSezioni = numSezioni;
    }

    public Utente getCreatore() {
        return creatore;
    }

    public void setCreatore(Utente creatore) {
        this.creatore = creatore;
    }

    public int getNumSezioni() {
        return numSezioni;
    }

    public void setNumSezioni(int numSezioni) {
        this.numSezioni = numSezioni;
    }

    public String getNomeDocumento() {
        return nomeDocumento;
    }

    public void setNomeDocumento(String nomeDocumento) {
        this.nomeDocumento = nomeDocumento;
    }
}