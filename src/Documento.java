import java.util.concurrent.ConcurrentHashMap;

public class Documento {
    private Utente creatore;
    private int numSezioni;
    private String nomeDocumento;
    private String pathFile;
    private String ipChat;

    public Documento(String nomeDocumento, Utente creatore, int numSezioni, String ipChat) {
        this.nomeDocumento = nomeDocumento;
        this.creatore = creatore;
        this.numSezioni = numSezioni;
        this.ipChat = ipChat;
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

    public String getNomeDocumento() {
        return nomeDocumento;
    }

    public void setNomeDocumento(String nomeDocumento) {
        this.nomeDocumento = nomeDocumento;
    }

    public String getPathFile() {
        return pathFile;
    }

    public void setPathFile(String pathFile) {
        this.pathFile = pathFile;
    }

    public String getIpChat() {
        return ipChat;
    }

    public void setIpChat(String ipChat) {
        this.ipChat = ipChat;
    }
}
