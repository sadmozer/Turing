import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GestoreDocumenti {
    private HashMap<Utente, HashMap<String, Documento>> documentiPerUtente = new HashMap<>();
    private HashMap<Documento, HashSet<String>> collaboratoriPerDocumento = new HashMap<>();
    private HashMap<Documento, Utente[]> attiviPerDocumento= new HashMap<>();
    private String pathMainDirectory = "";
    private String ipChatIniziale = "";

    public GestoreDocumenti(String pathMainDirectory, String ipChatIniziale) {
        this.pathMainDirectory = pathMainDirectory;
        this.ipChatIniziale = ipChatIniziale;
    }

    public boolean creaDirectoryDocumenti(Utente utente) {
        Path mainPath = Paths.get(pathMainDirectory);
        Path dirPath = Paths.get(pathMainDirectory + File.separator + utente.getUsername());
        try {
            if (!Files.exists(mainPath)) {
                Files.createDirectory(mainPath);
                System.out.println("Creata main directory.");
            }

            if(!documentiPerUtente.containsKey(utente) && !Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
                System.out.printf("Creata directory documenti %s path %s.%n", utente.getUsername(), dirPath.toString());
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

    public int getNumSezioni(String nomeDoc, Utente utente) {
        HashMap<String, Documento> mappaDocumenti;
        if ((mappaDocumenti = documentiPerUtente.get(utente)) == null) {
            return -1;
        }
        return mappaDocumenti.get(nomeDoc).getNumSezioni();
    }

    public String getPathSezione(String nomeDoc, int numSezione, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getPathFile() + File.separator + nomeDoc + "_" + numSezione + ".txt";
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

    public boolean isCreatore(String nomeDoc, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getCreatore().getUsername().equals(utente.getUsername());
    }

    public boolean creaDocumento(String nomeDoc, Utente utenteCreatore, int numSezioni) {
        // Creo nuovo documento
        Documento nuovoDocumento = new Documento(nomeDoc, utenteCreatore, numSezioni, ipChatIniziale);

        // Genero l'ip successivo
        ipChatIniziale = IpConverter.next(ipChatIniziale);

        // Prendo la hashmap dei documenti di utenteCreatore
        HashMap<String, Documento> mappaDocumenti;

        if ((mappaDocumenti = documentiPerUtente.get(utenteCreatore)) == null) {
            documentiPerUtente.put(utenteCreatore, new HashMap<>());
            mappaDocumenti = documentiPerUtente.get(utenteCreatore);
        }

        String stringPathFile = pathMainDirectory + File.separator + utenteCreatore.getUsername() + File.separator + nomeDoc;
        nuovoDocumento.setPathFile(stringPathFile);

        Path pathFile = Paths.get(stringPathFile);
        try {
            if (Files.exists(pathFile)) {
                return false;
            }
            else if (mappaDocumenti.putIfAbsent(nomeDoc, nuovoDocumento) != null) {
                return false;
            }

            if (attiviPerDocumento.get(nuovoDocumento) == null) {
                attiviPerDocumento.put(nuovoDocumento, new Utente[numSezioni]);
            }

            Files.createDirectory(pathFile);
            for (int i = 1; i <= numSezioni; i++) {
                Path pathSezione = Paths.get(stringPathFile + File.separator + nomeDoc + "_" + i + ".txt");
                Files.createFile(pathSezione);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
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

    public Utente isEditing(String nomeDoc, int numSez, Utente utente) {
        HashMap<String, Documento> mappaDocumenti;
        if ((mappaDocumenti = documentiPerUtente.get(utente)) == null) {
            return null;
        }

        Documento doc;
        if ((doc = mappaDocumenti.get(nomeDoc)) == null) {
            return null;
        }

        Utente[] utentiAttivi;
        if ((utentiAttivi = attiviPerDocumento.get(doc)) == null) {
            return null;
        }

        Utente attivo;
        if ((attivo = utentiAttivi[numSez]) == null) {
            return null;
        }

        return attivo;
    }

    public boolean inizioEditing(String nomeDoc, int numSez, Utente utente) {
        Documento doc;
        if ((doc = documentiPerUtente.get(utente).get(nomeDoc)) == null) {
            return false;
        }

        try {
            attiviPerDocumento.get(doc)[numSez] = utente;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean fineEditing(String nomeDoc, int numSez, Utente utente) {
        Documento doc;
        if ((doc = documentiPerUtente.get(utente).get(nomeDoc)) == null) {
            return false;
        }

        try {
            attiviPerDocumento.get(doc)[numSez] = null;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public long getDimSezione(String nomeDoc, int numSez, Utente utente) {
        String stringPathSezione = documentiPerUtente.get(utente).get(nomeDoc).getPathFile() + File.separator + nomeDoc + "_" + numSez + ".txt";
        Path pathFile = Paths.get(stringPathSezione);
        long dimSezione;
        try {
            dimSezione = Files.size(pathFile);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return dimSezione;

    }

    public String getChatIpDocumento(String nomeDoc, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getIpChat();
    }
}
