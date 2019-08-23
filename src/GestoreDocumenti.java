import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GestoreDocumenti {
    private HashMap<Utente, HashMap<String, Documento>> documentiPerUtente = new HashMap<>();
    private HashMap<Documento, HashSet<String>> collaboratoriPerDocumento = new HashMap<>();
    private String pathMainDirectory = "";

    public GestoreDocumenti(String pathMainDirectory) {
        this.pathMainDirectory = pathMainDirectory;
    }

    public boolean creaDirectoryDocumenti(Utente utente) {
        Path mainPath = Paths.get(pathMainDirectory);
        Path dirPath = Paths.get(pathMainDirectory + File.separator + utente.getUsername());
        try {
            if (!Files.exists(mainPath)) {
                Files.createDirectory(mainPath);
            }

            if(!documentiPerUtente.containsKey(utente) && !Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
                documentiPerUtente.put(utente, new HashMap<>());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean haDocumento(String nomeDoc, Utente utente) {
        HashMap<String, Documento> hasmapDoc;
        if ((hasmapDoc = documentiPerUtente.get(utente)) == null) {
            return false;
        }
        return hasmapDoc.get(nomeDoc) != null;
    }

    public boolean isCollaboratore(Utente utenteInvitato, String nomeDoc, Utente utenteCreatore) {
        return collaboratoriPerDocumento.get(documentiPerUtente.get(utenteCreatore).get(nomeDoc)).contains(utenteInvitato);
    }

    public boolean creaDocumento(String nomeDoc, Utente utenteCreatore, int numSezioni) {
        // Creo nuovo documento
        Documento nuovoDocumento = new Documento(nomeDoc, utenteCreatore, numSezioni);

        // Prendo la hashmap dei documenti di utenteCreatore
        HashMap<String, Documento> mappaDocumenti = documentiPerUtente.get(utenteCreatore);

        if (mappaDocumenti == null) {
            return false;
        }

        Path pathFile = Paths.get(pathMainDirectory + File.separator + utenteCreatore.getUsername() + File.separator + nomeDoc);
        try {
            if (!Files.exists(pathFile)) {
                Files.createDirectory(pathFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Inserisco il nuovo documento nella hashmap
        return (mappaDocumenti.putIfAbsent(nomeDoc, nuovoDocumento) == null);
    }

    public boolean condividiDocumento(String nomeDoc, Utente utenteCreatore, String utenteInvitato) {
        HashMap<String, Documento> listaDocs;
        if ((listaDocs = documentiPerUtente.get(utenteCreatore)) == null) {
            return false;
        }

        return collaboratoriPerDocumento.get(listaDocs.get(nomeDoc)).add(utenteInvitato);
    }

    public String getListaDocumenti(Utente utente) {
        String ret = "";
        HashMap<String, Documento> hashMap = documentiPerUtente.get(utente);

        if (hashMap == null) {
            return null;
        }

        for (HashMap.Entry<String, Documento> doc: hashMap.entrySet()) {
            ret += doc.getKey() + ":%n";
            ret += "\tCreatore: " + utente.getUsername() + "%n";
            HashSet<String> listaCollab = collaboratoriPerDocumento.get(doc.getValue());
            if (listaCollab != null) {
                ret += "\tCollaboratori: ";
                for (String col: listaCollab) {
                    ret += col + ", ";
                }
                ret = ret.substring(0, ret.length() - 2) + "%n";
            }
        }

        if (ret.equals("")) {
            return null;
        }
        return ret;
    }
}
