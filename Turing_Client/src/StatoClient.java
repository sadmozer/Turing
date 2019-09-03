import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.SocketChannel;
import java.util.HashMap;


/**
 *
 * Rappresenta lo stato del client
 *
 * @author Niccolo' Cardelli 534015
 */
public class StatoClient {
    private IRegistratore registratore;
    private SocketChannel socket;
    private MulticastSocket multicastSocket;
    private String pathMainDirectory;

    private Stato stato;
    private String utenteLoggato;
    private String ipChat;
    private String docEditato;
    private int sezioneDocEditato;

    StatoClient(IRegistratore registratore, SocketChannel socket, String pathMainDirectory, Stato stato) {
        this.registratore = registratore;
        this.socket = socket;
        this.pathMainDirectory = pathMainDirectory;
        this.stato = stato;
        this.utenteLoggato = "";
    }

    Stato getStato() {
        return stato;
    }

    void setStato(Stato stato) {
        this.stato = stato;
    }

    SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    IRegistratore getRegistratore() {
        return registratore;
    }

    public void setRegistratore(IRegistratore registratore) {
        this.registratore = registratore;

    }

    String getUtenteLoggato() {
        return utenteLoggato;
    }

    void setUtenteLoggato(String utenteLoggato) {
        this.utenteLoggato = utenteLoggato;
    }

    boolean iniziaEditing(String nomeDoc, int numSez, int portChat) {
        try {
            multicastSocket = new MulticastSocket(portChat);
            multicastSocket.setSoTimeout(500);
            multicastSocket.joinGroup(InetAddress.getByName(ipChat));
        } catch (IOException e) {
            System.out.println("Errore join chat.");
            return false;
        }

        docEditato = nomeDoc;
        sezioneDocEditato = numSez;
        return true;
    }

    void fineEditing() {
        docEditato = "";
        sezioneDocEditato = -1;
    }

    String getIpChat() {
        return ipChat;
    }

    void setIpChat(String ipChat) {
        this.ipChat = ipChat;
    }

    MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public void setMulticastSocket(MulticastSocket multicastSocket) {
        this.multicastSocket = multicastSocket;
    }

    public String getPathMainDirectory() {
        return pathMainDirectory;
    }

    public void setPathMainDirectory(String pathMainDirectory) {
        this.pathMainDirectory = pathMainDirectory;
    }

    public int getSezioneDocEditato() {
        return sezioneDocEditato;
    }

    public void setSezioneDocEditato(int sezioneDocEditato) {
        this.sezioneDocEditato = sezioneDocEditato;
    }

    public String getDocEditato() {
        return docEditato;
    }

    public void setDocEditato(String docEditato) {
        this.docEditato = docEditato;
    }
}
