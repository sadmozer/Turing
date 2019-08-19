import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
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
            socket.configureBlocking(false);
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
        regex += "turing\\slogin\\s\\w+}\\s\\w+}|";
        regex += "turing\\slogout|";
        regex += "turing\\squit|";
        regex += "turing\\s--help)";

        return regex;
    }

    private static boolean sintassiInputCorretta(String input, String regex) {
        return input.matches(regex);
    }

    private static StatoClient opRegister(IRegistratore registratore, String username, String password) {
        boolean err = false;

        // Controllo argomenti
        if (username.length() < minLungUsername) {
            System.err.println("L'username deve contenere almeno " + minLungUsername + " caratteri!");
            err = true;
        }
        else if (username.length() > maxLungUsername) {
            System.err.println("L'username deve contenere al massimo " + maxLungUsername + " caratteri!");
            err = true;
        }
        if (password.length() < minLungPassword) {
            System.err.println("La password deve contenere almeno " + minLungPassword+ " caratteri!");
            err = true;
        }
        else if (password.length() > maxLungPassword) {
            System.err.println("La password deve contenere al massimo " + maxLungPassword + " caratteri!");
            err = true;
        }
        if (err)
            return StatoClient.STARTED;

        // Provo a registrare l'utente
        try {
            if (!registratore.isRegistrato(username)) {
                if(registratore.registra(username, password)) {
                    System.out.println("Registrazione eseguita con successo.");
                    return StatoClient.STARTED;
                }
                else {
                    System.err.println("Registrazione fallita. Riprova.");
                    return StatoClient.STARTED;
                }
            }
            else {
                System.err.println("Utente gia' registrato, scegli un altro username.");
                return StatoClient.STARTED;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            System.err.println("Registrazione fallita. Riprova.");
            return StatoClient.STARTED;
        }
    }

    private static StatoClient opLogin(SocketChannel socket, String username, String password) {
        String msg = "login " + username + " " + password;
        ByteBuffer byteMsg = ByteBuffer.wrap(msg.getBytes());
        if(Connessione.inviaDati(socket, byteMsg, byteMsg.capacity()) == -1) {
            System.err.println("[CLIENT]: Errore inviaDati.");
            return StatoClient.STARTED;
        }
        else {
            return StatoClient.LOGGED;
        }
    }

    private static StatoClient opCreate(SocketChannel socket, String doc, int numSezioni) {
        return StatoClient.LOGGED;
    }

    private static StatoClient opShare(SocketChannel socket, String doc, String username) {
        return StatoClient.LOGGED;
    }

    private static StatoClient opShow(SocketChannel socket, String doc) {
        return StatoClient.LOGGED;
    }

    private static StatoClient opList(SocketChannel socket) {
        return StatoClient.LOGGED;
    }

    private static StatoClient opEdit(SocketChannel socket, String doc, String sec) {
        return StatoClient.EDIT;
    }

    private static StatoClient opLogout() {
        return StatoClient.STARTED;
    }

    private static StatoClient statoStarted(String[] comandi, SocketChannel socket, IRegistratore registratore) {
        StatoClient statoClient = StatoClient.STARTED;
        switch (comandi[1]) {
            case "register": {
                statoClient = opRegister(registratore, comandi[2], comandi[3]);
            } break;
            case "login": {
                statoClient = opLogin(socket, comandi[2], comandi[3]);
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
                statoClient = StatoClient.QUIT;
            } break;
            default: {
                System.err.println("bhComando errato.");
                System.out.println("Per vedere i comandi disponibili usa: turing --help");
                statoClient = StatoClient.STARTED;
            }
        }
        return statoClient;
    }

    private static StatoClient statoLogged(String[] comandi, SocketChannel socket) {
        StatoClient statoClient = StatoClient.STARTED;
        switch (comandi[1]) {
            case "create": {
                statoClient = opCreate(socket, comandi[2], Integer.parseInt(comandi[3]));
            } break;
            case "share": {
                statoClient = opShare(socket, comandi[2], comandi[3]);
            } break;
            case "show": {
                statoClient = opShow(socket, comandi[2]);
            } break;
            case "list": {
                statoClient = opList(socket);
            } break;
            case "edit": {
                statoClient = opEdit(socket, comandi[2], comandi[3]);
            } break;
            case "--help": {
                printUsage();
            } break;
            case "logout": {
                statoClient = opLogout();
            } break;
            case "register":
            case "login": {
                System.err.println("Devi prima eseguire il logout!");
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
        return statoClient;
    }

    private static StatoClient statoEdit(String[] comandi, SocketChannel socket) {
        return StatoClient.EDIT;
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
        System.out.printf("[CLIENT]: Connesso a %s sulla porta %d%n", hostAddress, DEFAULT_CLIENT_PORT);
        System.out.println("[CLIENT]: Benvenuto su Turing CLI! Per vedere i comandi disponibili usa: turing --help");

        // Rimango in ascolto dei comandi utente in input
        Scanner inputUtente = new Scanner(System.in);
        String regex = costruisciRegex();
        String comandi[] = null;
        String currInput = "";
        boolean quit = false;
        StatoClient statoClient = StatoClient.STARTED;
        while (!(statoClient == StatoClient.QUIT)) {
            System.out.printf("$ ");
            currInput = inputUtente.nextLine();
            comandi = currInput.split(" ");

            if(!sintassiInputCorretta(currInput, regex)) {
                System.err.println("Comando errato.");
                System.out.println("Per vedere i comandi disponibili usa: turing --help");
            }
            else {
                switch (statoClient) {
                    case STARTED: {
                        statoClient = statoStarted(comandi, socket, registratoreRemoto);
                    } break;
                    case LOGGED: {
                        statoClient = statoLogged(comandi, socket);
                    } break;
                    case EDIT: {
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
