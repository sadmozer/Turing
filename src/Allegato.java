import java.nio.file.Path;
import java.util.LinkedList;

public class Allegato {
    private Messaggio messaggio = null;
    private Utente utente = null;
    private LinkedList<Path> fileDaInviare = null;

    public Allegato() {
        this.fileDaInviare = new LinkedList<>();
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

    public boolean haFileDaInviare() {
        return !fileDaInviare.isEmpty();
    }
    public void pushFileDaInviare(Path pathFile) {
        fileDaInviare.push(pathFile);
    }

    public Path popFileDaInviare() {
        return fileDaInviare.pop();
    }
}
