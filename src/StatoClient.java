import java.nio.channels.SocketChannel;

public class StatoClient {
    private IRegistratore registratore;
    private SocketChannel socket;
    private Stato stato;
    private String utenteLoggato;

    public StatoClient(IRegistratore registratore, SocketChannel socket, Stato stato) {
        this.registratore = registratore;
        this.socket = socket;
        this.stato = stato;
        this.utenteLoggato = "";
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
}
