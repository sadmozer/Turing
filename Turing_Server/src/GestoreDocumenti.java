import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Gestisce i documenti e gli utenti collaboratori e attivi dei documenti
 *
 * @author Niccolo' Cardelli 534015
 */
class GestoreDocumenti {
    private HashMap<Utente, HashMap<String, Documento>> documentiPerUtente = new HashMap<>();
    private HashMap<Documento, HashSet<Utente>> collaboratoriPerDocumento = new HashMap<>();
    private HashMap<Documento, HashMap<Integer, Utente>> attiviPerDocumento = new HashMap<>();
    private String pathMainDirectory;
    private String ipChatAttuale;

    GestoreDocumenti(String pathMainDirectory, String ipChatAttuale) {
        this.pathMainDirectory = pathMainDirectory;
        this.ipChatAttuale = ipChatAttuale;
    }

    boolean creaDirectoryDocumenti(Utente utente) {
        Path mainPath = Paths.get(pathMainDirectory);
        Path dirPath = Paths.get(pathMainDirectory + File.separator + utente.getUsername());
        try {
            if (!Files.exists(mainPath)) {
                Files.createDirectory(mainPath);
                System.out.println("Creata main directory.");
            }

            if(!documentiPerUtente.containsKey(utente)) {
                documentiPerUtente.put(utente, new HashMap<>());
            }

            if (!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
                System.out.printf("Creata directory documenti %s path %s.%n", utente.getUsername(), dirPath.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean haDocumento(String nomeDoc, Utente utente) {
        HashMap<String, Documento> hasmapDoc;
        if ((hasmapDoc = documentiPerUtente.get(utente)) == null) {
            return false;
        }
        return hasmapDoc.get(nomeDoc) != null;
    }

    int getNumSezioni(String nomeDoc, Utente utente) {
        HashMap<String, Documento> mappaDocumenti;
        if ((mappaDocumenti = documentiPerUtente.get(utente)) == null) {
            return -1;
        }
        return mappaDocumenti.get(nomeDoc).getNumSezioni();
    }

    String getPathSezione(String nomeDoc, int numSezione, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getPathFile() + File.separator + nomeDoc + "_" + numSezione + ".txt";
    }


    boolean isCollaboratore(Utente utenteInvitato, String nomeDoc, Utente utenteCreatore) {
        Documento doc;
        if((doc = documentiPerUtente.get(utenteCreatore).get(nomeDoc)) == null) {
            return false;
        }
        HashSet<Utente> listaCol;
        if ((listaCol = collaboratoriPerDocumento.get(doc)) == null) {
            return false;
        }

        return listaCol.contains(utenteInvitato);
    }

    boolean isCreatore(String nomeDoc, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getCreatore().getUsername().equals(utente.getUsername());
    }

    boolean creaDocumento(String nomeDoc, Utente utenteCreatore, int numSezioni) {
        // Creo nuovo documento
        Documento nuovoDocumento = new Documento(nomeDoc, utenteCreatore, numSezioni, ipChatAttuale);

        // Genero l'ip successivo
        ipChatAttuale = IpConverter.next(ipChatAttuale);

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
                HashMap<Integer, Utente> utentiPerSezione = new HashMap<>();
                for (int i = 1; i <= numSezioni; i++) {
                    utentiPerSezione.put(i, null);
                }
                attiviPerDocumento.put(nuovoDocumento, utentiPerSezione);
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

    boolean condividiDocumento(String nomeDoc, Utente utenteCreatore, Utente utenteInvitato) {
        HashMap<String, Documento> listaDocsCreatore;
        HashMap<String, Documento> listaDocsInvitato = null;
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

        HashSet<Utente> listaCollaboratori;
        if ((listaCollaboratori = collaboratoriPerDocumento.get(listaDocsCreatore.get(nomeDoc))) == null) {
            collaboratoriPerDocumento.put(listaDocsCreatore.get(nomeDoc), new HashSet<>());
            listaCollaboratori = collaboratoriPerDocumento.get(listaDocsCreatore.get(nomeDoc));
        }

        return listaCollaboratori.add(utenteInvitato);
    }


    String getListaDocumenti(Utente utente) {
        String ret = "";
        HashMap<String, Documento> hashMap = documentiPerUtente.get(utente);

        if (hashMap == null) {
            return null;
        }

        for (HashMap.Entry<String, Documento> doc: hashMap.entrySet()) {
            ret += doc.getKey() + ":%n";
            ret += "  Creatore: " + doc.getValue().getCreatore().getUsername() + "%n";
            HashSet<Utente> listaCollab = collaboratoriPerDocumento.get(doc.getValue());
            if (listaCollab != null) {
                ret += "  Collaboratori: ";
                for (Utente col: listaCollab) {
                    ret += col.getUsername() + ", ";
                }
                ret = ret.substring(0, ret.length() - 2) + "%n";
            }
        }

        if (ret.equals("")) {
            return null;
        }
        return ret;
    }


    Utente isEditing(String nomeDoc, int numSez, Utente utente) {
        HashMap<String, Documento> mappaDocumenti;
        if ((mappaDocumenti = documentiPerUtente.get(utente)) == null) {
            return null;
        }

        Documento doc;
        if ((doc = mappaDocumenti.get(nomeDoc)) == null) {
            return null;
        }

        HashMap<Integer, Utente> utentiAttivi;
        if ((utentiAttivi = attiviPerDocumento.get(doc)) == null) {
            return null;
        }

        Utente attivo;
        if ((attivo = utentiAttivi.get(numSez)) == null) {
            return null;
        }

        return attivo;
    }

    boolean inizioEditing(String nomeDoc, int numSez, Utente utente) {
        Documento doc;
        if ((doc = documentiPerUtente.get(utente).get(nomeDoc)) == null) {
            return false;
        }

        attiviPerDocumento.get(doc).put(numSez, utente);
        return true;
    }

    void fineEditing(String nomeDoc, int numSez, Utente utente) {
        Documento doc;
        if ((doc = documentiPerUtente.get(utente).get(nomeDoc)) == null) {
            return;
        }

        attiviPerDocumento.get(doc).put(numSez,  null);
    }

    long getDimSezione(String nomeDoc, int numSez, Utente utente) {
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

    String getChatIpDocumento(String nomeDoc, Utente utente) {
        return documentiPerUtente.get(utente).get(nomeDoc).getIpChat();
    }
}
