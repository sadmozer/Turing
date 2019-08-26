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

    public boolean isCollaboratore(String usernameInvitato, String nomeDoc, Utente utenteCreatore) {
        Documento doc;
        if((doc = documentiPerUtente.get(utenteCreatore).get(nomeDoc)) == null) {
            return false;
        }
        HashSet<String> listaCol;
        if ((listaCol = collaboratoriPerDocumento.get(doc)) == null) {
            return false;
        }

        return listaCol.contains(usernameInvitato);
    }

    public boolean creaDocumento(String nomeDoc, Utente utenteCreatore, int numSezioni) {
        // Creo nuovo documento
        Documento nuovoDocumento = new Documento(nomeDoc, utenteCreatore, numSezioni);

        // Prendo la hashmap dei documenti di utenteCreatore
        HashMap<String, Documento> mappaDocumenti = documentiPerUtente.get(utenteCreatore);

        if (mappaDocumenti == null) {
            documentiPerUtente.put(utenteCreatore, new HashMap<>());
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
        return (documentiPerUtente.get(utenteCreatore).putIfAbsent(nomeDoc, nuovoDocumento) == null);
    }

    public boolean condividiDocumento(String nomeDoc, Utente utenteCreatore, Utente utenteInvitato) {
        HashMap<String, Documento> listaDocsCreatore;
        HashMap<String, Documento> listaDocsInvitato = null;
        HashSet<String> listaCollaboratori;
        if ((listaDocsCreatore = documentiPerUtente.get(utenteCreatore)) == null) {
            return false;
        }

        if ((listaDocsInvitato = documentiPerUtente.get(utenteInvitato)) == null) {
            documentiPerUtente.put(utenteInvitato, new HashMap<>());
            listaDocsInvitato = documentiPerUtente.get(utenteInvitato);
        }

        if (listaDocsInvitato.putIfAbsent(nomeDoc, listaDocsCreatore.get(nomeDoc)) != null) {
            return false;
        }

        if ((listaCollaboratori = collaboratoriPerDocumento.get(listaDocsCreatore.get(nomeDoc))) == null) {
            collaboratoriPerDocumento.put(listaDocsCreatore.get(nomeDoc), new HashSet<>());
            listaCollaboratori = collaboratoriPerDocumento.get(listaDocsCreatore.get(nomeDoc));
        }

        if (!listaCollaboratori.add(utenteInvitato.getUsername())) {
            return false;
        }

        return true;
    }

    public String getListaDocumenti(Utente utente) {
        String ret = "";
        HashMap<String, Documento> hashMap = documentiPerUtente.get(utente);

        if (hashMap == null) {
            return null;
        }

        for (HashMap.Entry<String, Documento> doc: hashMap.entrySet()) {
            ret += doc.getKey() + ":%n";
            ret += "  Creatore: " + doc.getValue().getCreatore().getUsername() + "%n";
            HashSet<String> listaCollab = collaboratoriPerDocumento.get(doc.getValue());
            if (listaCollab != null) {
                ret += "  Collaboratori: ";
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
