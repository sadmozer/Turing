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
import java.sql.Time;
import java.util.Scanner;

public class Client {
    private static int DEFAULT_REGISTRY_PORT = 6000;
    private static String DEFAULT_REGISTRY_NAME = "Registratore";
    private static int DEFAULT_CLIENT_PORT = 9999;
    private static int minLungUsername = 3;
    private static int maxLungUsername = 20;
    private static int minLungPassword = 6;
    private static int maxLungPassword = 20;
    private static int NUM_TENTATIVI_RICONNESSIONE = 3;
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
        regex += "(turing\\sregister\\s\\w+\\s\\w+|";
        regex += "turing\\slogin\\s\\w+\\s\\w+|";
        regex += "turing\\screate\\s\\w+\\s\\w+|";
        regex += "turing\\slogout|";
        regex += "turing\\squit|";
        regex += "turing\\s--help)";

        return regex;
    }

    private static boolean sintassiInputCorretta(String input, String regex) {
        return input.matches(regex);
    }

    // Automa
    private static void opRegister(StatoClient statoClient, String username, String password) {
        IRegistratore registratore = statoClient.getRegistratore();

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

        // Controllo il formato di username e password
        if (username.length() < minLungUsername) {
            System.err.println("L'username deve contenere almeno " + minLungUsername + " caratteri!");
            return;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("L'username deve contenere al massimo " + maxLungUsername + " caratteri!");
            return;
        }
        if (password.length() < minLungPassword) {
            System.err.println("La password deve contenere almeno " + minLungPassword+ " caratteri!");
            return;
        }
        else if (password.length() > maxLungPassword) {
            System.err.println("La password deve contenere al massimo " + maxLungPassword + " caratteri!");
            return;
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

        // Controllo il formato di username e password
        if (username.length() < minLungUsername) {
            System.err.println("Username errato.");
            return;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("Username errato.");
            return;
        }
        if (password.length() < minLungPassword) {
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

        switch (msgRisposta.getBuffer().getInt()) {
            case 200: {
                System.out.println("Login eseguito con successo.");
                statoClient.setUtenteLoggato(username);
                statoClient.setStato(Stato.LOGGED);
            } break;
            case 201: {
                System.err.println("Utente inesistente. Prima devi registrarti.");
            } break;
            case 202: {
                System.err.println("Password errata.");
            } break;
            case 203: {
                System.err.printf("Sei gia' loggato come %s%nDevi prima eseguire il logout!", username);
            } break;
            default: {
                System.err.println("Impossibile effettuare login. Riprova.");
            }
        }
    }

    private static void opCreate(StatoClient statoClient, String doc, String numSezioni) {
        String username = statoClient.getUtenteLoggato();
        SocketChannel socket = statoClient.getSocket();
        int numSez = -1;

        // Controllo il formato di doc e numSezioni
        try {
            numSez = Integer.parseInt(numSezioni);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.err.println("Il numero di sezioni deve essere positivo!");
            return;
        }
        if (numSez <= 0) {
            System.err.println("Il numero di sezioni deve essere positivo!");
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

        switch (msgRisposta.getBuffer().getInt()) {
            case 200: {
                System.out.println("Documento creato con successo.");
            } break;
        }


        Path dirPath = Paths.get(DEFAULT_DOCS_DIRECTORY + File.separator + username + File.separator + doc);
        try {
            if(!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Impossibile creare directory documenti. Riprova.");
            return;
        }
        System.out.printf("Creata directory documenti %s%n", dirPath);


    }

    private static void opShare(StatoClient statoClient, String doc, String username) {
    }

    private static void opShow(StatoClient statoClient, String doc) {
    }

    private static void opList(StatoClient statoClient) {
    }

    private static void opEdit(StatoClient statoClient, String doc, String sec) {
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

        switch (msgRisposta.getBuffer().getInt()) {
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
                System.err.println("Devi prima eseguire il login!");
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
                opShow(statoClient, comandi[2]);
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
                System.err.printf("Sei loggato come %s.%nDevi prima eseguire il logout!%n", statoClient.getUtenteLoggato());
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
            System.err.println("[CLIENT]: Impossibile connettersi! Riprovo..");
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
        String comandi[] = null;
        String currInput = "";
        boolean quit = false;
        StatoClient statoClient = new StatoClient(registratoreRemoto, socket, Stato.STARTED);

        // Ciclo principale
        while (!(statoClient.getStato() == Stato.QUIT)) {
            switch (statoClient.getStato()) {
                case STARTED: {
                    if (!statoClient.getUtenteLoggato().equals("")) {
                        System.out.printf("[%s]$ ", statoClient.getUtenteLoggato());
                    }
                    else {
                        System.out.printf("$ ");
                    }
                    currInput = inputUtente.nextLine();
                    comandi = currInput.split(" ");

                    if(!sintassiInputCorretta(currInput, regex)) {
                        System.err.println("Comando errato.");
                        System.out.println("Per vedere i comandi disponibili usa: turing --help");
                    }
                    else {
                        statoStarted(comandi, statoClient);
                    }
                } break;
                case LOGGED: {
                    if (!statoClient.getUtenteLoggato().equals("")) {
                        System.out.printf("[%s]$ ", statoClient.getUtenteLoggato());
                    }
                    else {
                        System.out.printf("$ ");
                    }
                    currInput = inputUtente.nextLine();
                    comandi = currInput.split(" ");

                    if(!sintassiInputCorretta(currInput, regex)) {
                        System.err.println("Comando errato.");
                        System.out.println("Per vedere i comandi disponibili usa: turing --help");
                    }
                    else {
                        statoLogged(comandi, statoClient);
                    }
                } break;
                case EDIT: {
                } break;
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
