/**
 * @author Niccolo' Cardelli 534015
 */
public class Documento {
    private Utente creatore;
    private int numSezioni;
    private String nomeDocumento;
    private String pathFile;
    private String ipChat;

    Documento(String nomeDocumento, Utente creatore, int numSezioni, String ipChat) {
        this.nomeDocumento = nomeDocumento;
        this.creatore = creatore;
        this.numSezioni = numSezioni;
        this.ipChat = ipChat;
    }

    Utente getCreatore() {
        return creatore;
    }

    public void setCreatore(Utente creatore) {
        this.creatore = creatore;
    }

    int getNumSezioni() {
        return numSezioni;
    }

    public String getNomeDocumento() {
        return nomeDocumento;
    }

    public void setNomeDocumento(String nomeDocumento) {
        this.nomeDocumento = nomeDocumento;
    }

    String getPathFile() {
        return pathFile;
    }

    void setPathFile(String pathFile) {
        this.pathFile = pathFile;
    }

    String getIpChat() {
        return ipChat;
    }

    public void setIpChat(String ipChat) {
        this.ipChat = ipChat;
    }
}
