import java.io.IOException;
import java.net.MulticastSocket;
import java.nio.channels.MulticastChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class StatoClient {
    private IRegistratore registratore;
    private SocketChannel socket;
    private Stato stato;
    private String utenteLoggato;
    private String ipChat;
    private MulticastSocket multicastSocket;
    private HashMap<String, Integer> sezioniPerDocumentoEditati;

    public StatoClient(IRegistratore registratore, SocketChannel socket, Stato stato) {
        this.registratore = registratore;
        this.socket = socket;
        this.stato = stato;
        this.utenteLoggato = "";
        this.sezioniPerDocumentoEditati = new HashMap<>();
    }

    public Stato getStato() {
        return stato;
    }

    public void setStato(Stato stato) {
        this.stato = stato;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public IRegistratore getRegistratore() {
        return registratore;
    }

    public void setRegistratore(IRegistratore registratore) {
        this.registratore = registratore;

    }

    public String getUtenteLoggato() {
        return utenteLoggato;
    }

    public void setUtenteLoggato(String utenteLoggato) {
        this.utenteLoggato = utenteLoggato;
    }

    public int staEditando(String nomeDoc) {
        if (!sezioniPerDocumentoEditati.containsKey(nomeDoc)) {
            return -1;
        }
        return sezioniPerDocumentoEditati.get(nomeDoc);
    }

    public boolean iniziaEditing(String nomeDoc, int numSez, int portChat) {
        try {
            multicastSocket = new MulticastSocket(portChat);
            multicastSocket.setSoTimeout(500);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sezioniPerDocumentoEditati.putIfAbsent(nomeDoc, numSez) == null;
    }

    public boolean fineEditing(String nomeDoc) {
        return sezioniPerDocumentoEditati.remove(nomeDoc) != null;
    }

    public String getIpChat() {
        return ipChat;
    }

    public void setIpChat(String ipChat) {
        this.ipChat = ipChat;
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public void setMulticastSocket(MulticastSocket multicastSocket) {
        this.multicastSocket = multicastSocket;
    }
}
