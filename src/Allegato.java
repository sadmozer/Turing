import java.nio.file.Path;
import java.util.LinkedList;

public class Allegato {
    private Messaggio messaggio = null;
    private Utente utente = null;
    private LinkedList<Path> fileDaInviare = null;
    private Path pathFileDaRicevere = null;
    private long dimFileDaRicevere = 0L;

    Allegato() {
        this.fileDaInviare = new LinkedList<>();
    }

    Utente getUtente() {
        return utente;
    }

    void setUtente(Utente utente) {
        this.utente = utente;
    }

    Messaggio getMessaggio() {
        return messaggio;
    }

    void setMessaggio(Messaggio messaggio) {
        this.messaggio = messaggio;
    }

    boolean haFileDaInviare() {
        return !fileDaInviare.isEmpty();
    }
    void pushFileDaInviare(Path pathFile) {
        fileDaInviare.push(pathFile);
    }
    Path popFileDaInviare() {
        return fileDaInviare.pop();
    }

    Path getPathFileDaRicevere() {
        return pathFileDaRicevere;
    }

    void setPathFileDaRicevere(Path pathFileDaRicevere) {
        this.pathFileDaRicevere = pathFileDaRicevere;
    }

    long getDimFileDaRicevere() {
        return dimFileDaRicevere;
    }

    void setDimFileDaRicevere(long dimFileDaRicevere) {
        this.dimFileDaRicevere = dimFileDaRicevere;
    }
}
