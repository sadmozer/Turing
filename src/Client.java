import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    private static int DEFAULT_REGISTRY_PORT = 6000;
    private static String DEFAULT_REGISTRY_NAME = "Registratore";
    private static int DEFAULT_CLIENT_PORT = 9999;
    private static int minLungUsername = 3;
    private static int maxLungUsername = 20;
    private static int minLungPassword = 6;
    private static int maxLungPassword = 20;
    private static int minLungDocumento = 3;
    private static int maxLungDocumento = 20;
    private static int minNumSezioni = 1;
    private static int maxNumSezioni = 15;
    private static String DEFAULT_DOCS_DIRECTORY = System.getProperty("user.dir") + File.separator + "data_client";

    // Setup
    private static IRegistratore setupRegistratore(int serverPort, String serverName) {
        IRegistratore registratoreRemoto = null;
        try {
            Registry reg = LocateRegistry.getRegistry(serverPort);
            registratoreRemoto = (IRegistratore) reg.lookup(serverName);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return null;
        }
        return registratoreRemoto;
    }

    private static SocketChannel setupClientSocket() {
        SocketChannel socket = null;
        try {
            socket = SocketChannel.open();
        } catch (UnresolvedAddressException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return socket;
    }

    private static boolean tryConnect(SocketChannel socket, InetSocketAddress address) {
        try {
            socket.connect(address);
            while (!socket.finishConnect()) {
                System.out.println("[CLIENT]: Mi sto connettendo..");
            }
        } catch (UnresolvedAddressException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // CLI
    private static void printUsage() {
        System.out.printf("USAGE%n");
        System.out.printf("  $ turing COMMAND [ARGS...]%n%n");
        System.out.printf("COMMANDS%n");
        System.out.printf("  %-35s %-40s%n", "register <username> <password>", "registra l'utente");
        System.out.printf("  %-35s %-40s%n", "login <username> <password>","login utente");
        System.out.printf("  %-35s %-40s%n", "logout", "effettua il logout");
        System.out.printf("%n");
        System.out.printf("  %-35s %-40s%n", "create <doc> <numsezioni>", "crea un documento");
        System.out.printf("  %-35s %-40s%n", "share <doc> <username>", "condivide un documento");
        System.out.printf("  %-35s %-40s%n", "show <doc> <sec>", "mostra una sezione del documento");
        System.out.printf("  %-35s %-40s%n", "show <doc>", "mostra l'intero documento");
        System.out.printf("  %-35s %-40s%n", "list", "mostra la lista dei documenti");
        System.out.printf("%n");
        System.out.printf("  %-35s %-40s%n", "edit <doc> <sec>", "modifica una sezione del documento");
        System.out.printf("  %-35s %-40s%n", "end-edit <doc> <sec>", "fine modifica della sezione del doc");
        System.out.printf("%n");
        System.out.printf("  %-35s %-40s%n", "send <msg>", "invia messaggi sulla chat");
        System.out.printf("  %-35s %-40s%n", "receive", "visualizza i messaggi ricevuti sulla chat");
        System.out.printf("%n");
    }

    private static String costruisciRegex() {
        String regex = "";
        regex += "(turing\\sregister\\s([^\\s]+)\\s([^\\s]+)|";
        regex += "turing\\slogin\\s([^\\s]+)\\s([^\\s]+)|";
        regex += "turing\\screate\\s([^\\s]+)\\s([^\\s]+)|";
        regex += "turing\\sshare\\s([^\\s]+)\\s([^\\s]+)|";
        regex += "turing\\slist|";
        regex += "turing\\sedit\\s([^\\s]+)\\s([^\\s]+)|";
        regex += "turing\\slogout|";
        regex += "turing\\squit|";
        regex += "turing\\s--help)";

        return regex;
    }

    private static boolean sintassiInputCorretta(String input, String regex) {
        return input.matches(regex);
    }

    private static void riceviNotifica(Messaggio msgRisposta, String utente) {
        System.out.println("[NUOVA NOTIFICA!]");
        byte[] bytesMittente = new byte[msgRisposta.getBuffer().getInt()];
        msgRisposta.getBuffer().get(bytesMittente);
        String mittente = new String(bytesMittente);
        byte[] bytesNomeDoc = new byte[msgRisposta.getBuffer().getInt()];
        msgRisposta.getBuffer().get(bytesNomeDoc);
        String nomeDoc = new String(bytesNomeDoc);
        int numSezioni = msgRisposta.getBuffer().getInt();
        System.out.printf("  %s ti ha invitato a collaborare al documento %s composto da %d sezioni.%n  Ora puoi accedere e modificare il documento.%n", mittente, nomeDoc, numSezioni);

        String pathDocumento = DEFAULT_DOCS_DIRECTORY + File.separator + utente + File.separator + nomeDoc;

        try {
            if (Files.notExists(Paths.get(pathDocumento))) {
                Files.createDirectory(Paths.get(pathDocumento));
                System.out.printf("Creata directory %s.%n", pathDocumento);
            }
            for (int i = 1; i <= numSezioni; i++) {
                if (Files.notExists(Paths.get( pathDocumento + File.separator + nomeDoc + "_" + i + ".txt"))) {
                    Files.createFile(Paths.get( pathDocumento + File.separator + nomeDoc + "_" + i + ".txt"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Impossibile creare documento. Riprova.");
        }

    }

    // Automa
    private static void opRegister(StatoClient statoClient, String username, String password) {
        IRegistratore registratore = statoClient.getRegistratore();

        // Controllo il formato di username
        boolean errore = false;

        if (!username.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("L'username deve contenere solo caratteri alfanumerici.");
            errore = true;
        }

        if (username.length() < minLungUsername) {
            System.err.println("L'username deve contenere almeno " + minLungUsername + " caratteri.");
            errore = true;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("L'username deve contenere al massimo " + maxLungUsername + " caratteri.");
            errore = true;
        }

        // Controllo il formato di password
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("La password deve contenere solo caratteri alfanumerici.");
            errore = true;
        }

        if (password.length() < minLungPassword) {
            System.err.println("La password deve contenere almeno " + minLungPassword+ " caratteri.");
            errore = true;
        }
        else if (password.length() > maxLungPassword) {
            System.err.println("La password deve contenere al massimo " + maxLungPassword + " caratteri.");
            errore = true;
        }

        if (errore) {
            return;
        }

        // Controllo che non sia gia' registrato
        try {
            if (registratore.isRegistrato(username)) {
                System.err.println("Utente gia' registrato, scegli un altro username.");
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            System.err.println("Registrazione fallita. Riprova.");
        }

        // Provo a registrare l'utente
        Path pathDatiUtente = Paths.get(DEFAULT_DOCS_DIRECTORY + File.separator + username);
        try {
            if(!registratore.registra(username, password)) {
                System.err.println("Registrazione fallita. Riprova.");
            }
            else {
                if (Files.notExists(pathDatiUtente)) {
                    Files.createDirectory(pathDatiUtente);
                    System.out.printf("Creata directory %s.%n", pathDatiUtente.toString());
                }
                System.out.println("Registrazione eseguita con successo.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Registrazione fallita. Riprova.");
        }


    }

    private static void opLogin(StatoClient statoClient, String username, String password) {
        SocketChannel socket = statoClient.getSocket();

        // Controllo il formato di username
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("L'username deve contenere solo caratteri alfanumerici.");
            return;
        }
        else if (username.length() < minLungUsername) {
            System.err.println("Username errato.");
            return;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("Username errato.");
            return;
        }

        // Controllo il formato di password
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("Password errata.");
            return;
        }
        else if (password.length() < minLungPassword) {
            System.err.println("Password errata.");
            return;
        }
        else if (password.length() > maxLungPassword) {
            System.err.println("Password errata.");
            return;
        }

        Messaggio msgInvio = new Messaggio();
        msgInvio.setBuffer("login " + username + " " + password);

        if(Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile effettuare login. Riprova.");
            return;
        }

        Messaggio msgRisposta = new Messaggio();
        if((Connessione.riceviDati(socket, msgRisposta)) == -1) {
            System.err.println("Impossibile effettuare login. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, username);
                }
                break;
                case 200: {
                    System.out.println("Login eseguito con successo.");

                    statoClient.setUtenteLoggato(username);
                    statoClient.setStato(Stato.LOGGED);
                }
                break;
                case 201: {
                    System.err.println("Utente inesistente. Prima devi registrarti.");
                }
                break;
                case 202: {
                    System.err.println("Password errata.");
                }
                break;
                case 203: {
                    if (statoClient.getStato().equals(Stato.LOGGED)) {
                        System.err.printf("Sei gia' loggato come %s%nDevi prima eseguire il logout.%n", username);
                    } else {
                        System.err.printf("Utente %s gia' loggato su un altro host.%n", username);
                    }
                }
                break;
                default: {
                    System.err.println("Impossibile effettuare login. Riprova.");
                }
            }
        }
    }

    private static void opCreate(StatoClient statoClient, String doc, String numSezioni) {
        String username = statoClient.getUtenteLoggato();
        SocketChannel socket = statoClient.getSocket();
        int numSez = -1;

        // Controllo il formato di doc
        boolean errore = false;
        if (!doc.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("Il nome documento deve contenere solo caratteri alfanumerici.");
            errore = true;
        }

        if (doc.length() < minLungDocumento) {
            System.err.printf("Il nome documento deve contenere almeno %d caratteri.%n", minLungDocumento);
            errore = true;
        }
        else if(doc.length() > maxLungDocumento) {
            System.err.printf("Il nome documento deve contenere al massimo %d caratteri.%n", maxLungDocumento);
            errore = true;
        }

        // Controllo il formato di numSezioni
        try {
            numSez = Integer.parseInt(numSezioni);
            if (numSez < minNumSezioni) {
                System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
                errore = true;
            }
            else if (numSez > maxNumSezioni) {
                System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
                errore = true;
            }
        } catch (NumberFormatException e) {
            System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
            errore = true;
        }

        if (errore) {
            return;
        }

        Messaggio msgInvio = new Messaggio();
        Messaggio msgRisposta = new Messaggio();
        msgInvio.setBuffer("create " + doc + " " + numSezioni);
        if (Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile creare documento. Riprova.");
            return;
        }

        if (Connessione.riceviDati(socket, msgRisposta) == -1) {
            System.err.println("Impossibile creare documento. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, statoClient.getUtenteLoggato());
                } break;
                case 200: {
                    String pathDocumento = DEFAULT_DOCS_DIRECTORY + File.separator + username + File.separator + doc;

                    try {
                        if (Files.notExists(Paths.get(pathDocumento))) {
                            Files.createDirectory(Paths.get(pathDocumento));
                            System.out.printf("Creata directory %s.%n", pathDocumento);
                        }
                        for (int i = 1; i <= numSez; i++) {
                            if (Files.notExists(Paths.get( pathDocumento + File.separator + doc + "_" + i + ".txt"))) {
                                Files.createFile(Paths.get( pathDocumento + File.separator + doc + "_" + i + ".txt"));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Impossibile creare documento. Riprova.");
                        return;
                    }

                    System.out.println("Documento creato con successo.");

                } break;
                case 201: {
                    System.err.println("Documento gia' presente.");
                    return;
                }
                case 203:
                case 204:
                case 205: {
                    System.err.println("Impossibile creare documento. Riprova.");
                    return;
                }
            }
        }

        // Creo la directory contentente le sezioni del documento
        Path dirPath = Paths.get(DEFAULT_DOCS_DIRECTORY + File.separator + username + File.separator + doc);
        try {
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
                System.out.printf("Creata directory documenti %s%n", dirPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Impossibile creare directory documenti. Riprova.");
            return;
        }
    }

    private static void opShare(StatoClient statoClient, String doc, String username) {
        SocketChannel socket = statoClient.getSocket();

        // Controllo il formato di doc
        boolean errore = false;
        if (!doc.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("Nome documento errato.");
            errore = true;
        }
        if (doc.length() < minLungDocumento) {
            System.err.println("Nome documento errato.");
            errore = true;
        }
        else if(doc.length() > maxLungDocumento) {
            System.err.println("Nome documento errato.");
            errore = true;
        }

        // Controllo il formato di username
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("L'username deve contenere solo caratteri alfanumerici.");
            errore = true;
        }
        else if (username.length() < minLungUsername) {
            System.err.println("Username errato.");
            errore = true;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("Username errato.");
            errore = true;
        }

        if (errore) {
            return;
        }

        Messaggio msgInvio = new Messaggio();
        msgInvio.setBuffer("share " + doc + " " + username);

        if(Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile effettuare login. Riprova.");
            return;
        }

        Messaggio msgRisposta = new Messaggio();
        if((Connessione.riceviDati(socket, msgRisposta)) == -1) {
            System.err.println("Impossibile effettuare login. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, statoClient.getUtenteLoggato());
                } break;
                case 203: {
                    System.err.println("Impossibile condividere documento.");
                } break;
                case 206: {
                    System.err.println("Non puoi autoinvitarti.");
                } break;
                case 205: {
                    System.err.println("L'utente invitato non risulta registrato.");
                } break;
                case 207: {
                    System.err.printf("Non possiedi alcun documento chiamato %s.%n", doc);
                } break;
                case 208: {
                    System.err.println("L'utente invitato e' gia' un collaboratore.");
                } break;
                case 209: {
                    System.err.println("L'utente invitato possiede un documento con lo stesso nome del tuo.");
                } break;
                case 210: {
                    System.err.printf("Non hai i permessi per condividere il documento.%nSolo il creatore puo' condividerlo.%n");
                } break;
                case 200: {
                    System.out.printf("Documento condiviso con successo.%nVerra' inviato un invito a %s.%n", username);
                } break;
            }
        }
    }

    private static void opShow1(StatoClient statoClient, String doc, String numSez) {

    }

    private static void opShow2(StatoClient statoClient, String doc) {

    }

    private static void opList(StatoClient statoClient) {
        SocketChannel socket = statoClient.getSocket();

        Messaggio msgInvio = new Messaggio();
        msgInvio.setBuffer("list");
        if (Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile ricevere lista documenti. Riprova.");
            return;
        }

        Messaggio msgRisposta = new Messaggio();
        if (Connessione.riceviDati(socket, msgRisposta) == -1) {
            System.err.println("Impossibile ricevere lista documenti. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, statoClient.getUtenteLoggato());
                } break;
                case 203:
                case 204:
                case 205: {
                    System.err.println("Impossibile ricevere lista documenti. Riprova.");
                } break;
                case 201: {
                    System.err.println("Non hai alcun documento.");
                } break;
                case 200: {
                    System.out.println("Lista ricevuta con successo.");
                    ByteBuffer buf = msgRisposta.getBuffer();
                    int dimMsg = buf.getInt();
                    byte[] bytes = new byte[dimMsg];
                    msgRisposta.getBuffer().get(bytes);
                    String lista = new String(bytes);
                    System.out.printf(lista);
                } break;
            }
        }
    }

    private static void opEdit(StatoClient statoClient, String doc, String numSezione) {
        SocketChannel socket = statoClient.getSocket();

        // Controllo il formato di doc
        boolean errore = false;
        if (!doc.matches("^[a-zA-Z0-9]+$")) {
            System.err.println("Nome documento errato.");
            errore = true;
        }
        if (doc.length() < minLungDocumento) {
            System.err.println("Nome documento errato.");
            errore = true;
        }
        else if(doc.length() > maxLungDocumento) {
            System.err.println("Nome documento errato.");
            errore = true;
        }

        // Controllo il formato di numSez
        int numSez = -1;
        try {
            numSez = Integer.parseInt(numSezione);
            if (numSez < minNumSezioni) {
                System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
                errore = true;
            }
            else if (numSez > maxNumSezioni) {
                System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
                errore = true;
            }
        } catch (NumberFormatException e) {
            System.err.printf("Il numero di sezioni deve essere un intero compreso tra %d e %d.%n", minNumSezioni, maxNumSezioni);
            errore = true;
        }

        if (errore) {
            return;
        }

        Path pathFile = Paths.get(DEFAULT_DOCS_DIRECTORY + File.separator + statoClient.getUtenteLoggato() + File.separator + doc + File.separator +doc + "_" + numSezione + ".txt");
        try {
            if (!Files.exists(pathFile)) {
                Files.createFile(pathFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Messaggio msgInvio = new Messaggio();
        Messaggio msgRisposta = new Messaggio();
        msgInvio.setBuffer("edit " + doc + " " + numSezione);
        if (Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile editare documento. Riprova.");
            return;
        }

        if (Connessione.riceviDati(socket, msgRisposta) == -1) {
            System.err.println("Impossibile editare documento. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, statoClient.getUtenteLoggato());
                } break;
                case 200: {
                    long dimSezione = msgRisposta.getBuffer().getLong();

                    if (!Connessione.riceviFile(socket, dimSezione, pathFile)) {
                        System.err.println("Errore riceviFile.");
                        return;
                    }
                    System.out.printf("Inizio editing sezione %d del documento %s.%n", numSez, doc);
                    statoClient.setStato(Stato.EDIT);
                } break;
                case 201:
                case 203: {
                    System.err.println("Impossibile iniziare editing documento. Riprova.");
                } break;
                case 204: {
                    System.err.printf("Non possiedi alcun documento chiamato %s.%n", doc);
                } break;
                case 205: {
                    System.err.printf("Stai gia' modificando la sezione %d del documento %s.%n", numSez, doc);
                } break;
                case 206: {
                    System.err.println("Numero di sezione errato.");
                } break;
                case 207: {
                    byte[] bytesMittente = new byte[msgRisposta.getBuffer().getInt()];
                    msgRisposta.getBuffer().get(bytesMittente);
                    String mittente = new String(bytesMittente);
                    System.err.printf("Impossibile modificare sezione %d del documento %s.%nL'utente %s la sta modificando.%n", numSez, doc, mittente);
                } break;
            }
        }
    }

    private static void opEndEdit(StatoClient statoClient, String doc, String numSez) {

    }

    private static void opSend(StatoClient statoClient, String msg) {

    }

    private static void opReceive(StatoClient statoClient) {

    }

    private static void opLogout(StatoClient statoClient) {
        SocketChannel socket = statoClient.getSocket();

        Messaggio msgInvio = new Messaggio();
        msgInvio.setBuffer("logout");
        if (Connessione.inviaDati(socket, msgInvio) == -1) {
            System.err.println("Impossibile effettuare logout. Riprova.");
            return;
        }

        Messaggio msgRisposta = new Messaggio();
        if (Connessione.riceviDati(socket, msgRisposta) == -1) {
            System.err.println("Impossibile effettuare logout. Riprova.");
            return;
        }

        while (msgRisposta.getBuffer().hasRemaining()) {
            switch (msgRisposta.getBuffer().getInt()) {
                case 100: {
                    riceviNotifica(msgRisposta, statoClient.getUtenteLoggato());
                } break;
                case 203:
                case 204:
                case 205: {
                    System.err.println("Impossibile effettuare logout. Riprova.");
                } break;
                case 200: {
                    System.out.println("Logout eseguito con successo.");
                    statoClient.setUtenteLoggato("");
                    statoClient.setStato(Stato.STARTED);
                }
            }
        }
    }

    private static void statoStarted(String[] comandi, StatoClient statoClient) {
        statoClient.setStato(Stato.STARTED);
        switch (comandi[1]) {
            case "register": {
                opRegister(statoClient, comandi[2], comandi[3]);
            } break;
            case "login": {
                opLogin(statoClient, comandi[2], comandi[3]);
            } break;
            case "--help": {
                printUsage();
            } break;
            case "logout":
            case "create":
            case "share":
            case "show":
            case "list":
            case "edit":
            case "end-edit":
            case "send":
            case "receive": {
                System.err.println("Devi prima eseguire il login.");
            } break;
            case "quit": {
                statoClient.setStato(Stato.QUIT);
            } break;
            default: {
                System.err.println("Comando errato.");
                System.out.println("Per vedere i comandi disponibili usa: turing --help");
                statoClient.setStato(Stato.STARTED);
            }
        }
    }

    private static void statoLogged(String[] comandi, StatoClient statoClient) {
        statoClient.setStato(Stato.LOGGED);
        switch (comandi[1]) {
            case "create": {
                opCreate(statoClient, comandi[2], comandi[3]);
            } break;
            case "share": {
                opShare(statoClient, comandi[2], comandi[3]);
            } break;
            case "show": {
                if (comandi.length == 4) {
                    opShow1(statoClient, comandi[2], comandi[3]);
                }
                else {
                    opShow2(statoClient, comandi[2]);
                }
            } break;
            case "list": {
                opList(statoClient);
            } break;
            case "edit": {
                opEdit(statoClient, comandi[2], comandi[3]);
            } break;
            case "--help": {
                printUsage();
            } break;
            case "logout": {
                opLogout(statoClient);
            } break;
            case "register":
            case "login": {
                System.err.printf("Sei loggato come %s.%nDevi prima eseguire il logout.%n", statoClient.getUtenteLoggato());
            }
            case "end-edit":
            case "send":
            case "receive": {

            } break;
            default: {
                System.err.println("Comando errato.");
                System.out.println("Per vedere i comandi disponibili usa: turing --help");
            }
        }
    }

    private static void statoEdit(String[] comandi, StatoClient statoClient) {
        statoClient.setStato(Stato.EDIT);
        switch (comandi[1]) {
            case "end-edit": {
                opEndEdit(statoClient, comandi[2], comandi[3]);
            } break;
            case "send": {
                opSend(statoClient, comandi[2]);
            } break;
            case "receive": {
                opReceive(statoClient);
            } break;
            case "register":
            case "login":
            case "create":
            case "share":
            case "show":
            case "list":
            case "edit": {
                System.err.printf("Non puoi eseguire il comando in modalità editing.%nEsegui prima il comando end-edit.%n");
            } break;
            default: {

            } break;
        }
    }

    public static void main (String[] args) {
        System.out.println("[CLIENT]: Avvio..");

        // Eseguo il setup del Servizio Registratore
        IRegistratore registratoreRemoto = setupRegistratore(DEFAULT_REGISTRY_PORT, DEFAULT_REGISTRY_NAME);
        if (registratoreRemoto == null) {
            System.err.println("[CLIENT-ERROR]: Impossibile trovare Servizio Registratore. Esco..");
            System.exit(1);
        }
        System.out.println("[CLIENT]: Servizio Registratore pronto.");

        // Cerco l'indirizzo dell'host locale
        String hostAddress = "";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("[CLIENT-ERROR]: HostAddress non trovato. Esco..");
            System.exit(1);
        }

        // Configuro il socket
        SocketChannel socket = setupClientSocket();
        if (socket == null) {
            System.err.println("[CLIENT-ERROR]: Impossibile configurare Client Socket. Esco..");
            System.exit(1);
        }
        System.out.println("[CLIENT]: Client Socket configurato.");

        // Tento di connettermi al server
        InetSocketAddress serverAddress = new InetSocketAddress(hostAddress, DEFAULT_CLIENT_PORT);
        while (!tryConnect(socket, serverAddress)) {
            System.err.println("[CLIENT]: Impossibile connettersi. Riprovo..");
        }

        // Creo cartella dei documenti
        try {
            if (Files.notExists(Paths.get(DEFAULT_DOCS_DIRECTORY))) {
                Files.createDirectory(Paths.get(DEFAULT_DOCS_DIRECTORY));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("[CLIENT]: Connesso a %s sulla porta %d%n", hostAddress, DEFAULT_CLIENT_PORT);
        System.out.println("[CLIENT]: Benvenuto su Turing CLI! Per vedere i comandi disponibili usa: turing --help");

        // Rimango in ascolto dei comandi utente in input
        Scanner inputUtente = new Scanner(System.in);
        String regex = costruisciRegex();
        String[] comandi = null;
        String currInput = "";
        StatoClient statoClient = new StatoClient(registratoreRemoto, socket, Stato.STARTED);

        // Ciclo principale
        while (!(statoClient.getStato() == Stato.QUIT)) {
            if (!statoClient.getUtenteLoggato().equals("")) {
                System.out.printf("[%s]$ ", statoClient.getUtenteLoggato());
            }
            else {
                System.out.printf("$ ");
            }
            currInput = inputUtente.nextLine();
            comandi = currInput.split("\\s+");
            if(!sintassiInputCorretta(currInput, regex)) {
                System.err.println("Comando errato.");
                System.out.println("Per vedere i comandi disponibili usa: turing --help");
            }
            else {
                switch (statoClient.getStato()) {
                    case STARTED: {
                        statoStarted(comandi, statoClient);
                    } break;
                    case LOGGED: {
                        statoLogged(comandi, statoClient);
                    } break;
                    case EDIT: {
                        statoEdit(comandi, statoClient);
                    } break;
                }
            }
        }
        inputUtente.close();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
