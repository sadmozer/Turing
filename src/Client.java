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
        System.out.printf("usage: turing COMMAND [ARGS...]%n%n");
        System.out.printf("commands:%n");
        System.out.printf("\t%-40s %-40s%n", "register <username> <password>", "registra l'utente");
        System.out.printf("\t%-40s %-40s%n", "login <username> <password>","login utente");
        System.out.printf("\t%-40s %-40s%n", "logout", "effettua il logout");
        System.out.printf("%n");
        System.out.printf("\t%-40s %-40s%n", "create <doc> <numsezioni>", "crea un documento");
        System.out.printf("\t%-40s %-40s%n", "share <doc> <username>", "condivide un documento");
        System.out.printf("\t%-40s %-40s%n", "show <doc> <sec>", "mostra una sezione del documento");
        System.out.printf("\t%-40s %-40s%n", "show <doc>", "mostra l'intero documento");
        System.out.printf("\t%-40s %-40s%n", "list", "mostra la lista dei documenti");
        System.out.printf("%n");
        System.out.printf("\t%-40s %-40s%n", "edit <doc> <sec>", "modifica una sezione del documento");
        System.out.printf("\t%-40s %-40s%n", "end-edit <doc> <sec>", "fine modifica della sezione del doc");
        System.out.printf("%n");
        System.out.printf("\t%-40s %-40s%n", "send <msg>", "invia messaggi sulla chat");
        System.out.printf("\t%-40s %-40s%n", "receive", "visualizza i messaggi ricevuti sulla chat");
        System.out.printf("%n");
    }

    private static String costruisciRegex() {
        String regex = "";
        regex += "(turing\\sregister\\s\\w{"+minLungUsername+","+maxLungUsername+"}\\s\\w{"+minLungPassword+","+maxLungPassword+"}|";
        regex += "turing\\slogin\\s\\w{"+minLungUsername+","+maxLungUsername+"}\\s\\w{"+minLungPassword+","+maxLungPassword+"}|";
        regex += "turing\\slogout|";
        regex += "turing\\squit|";
        regex += "turing\\s--help)";

        return regex;
    }

    private static boolean sintassiInputCorretta(String input, String regex) {
        return input.matches(regex);
    }

    private static boolean tryIsRegistrato(IRegistratore registratoreRemoto, String username) {
        boolean isRegistrato = false;
        try {
            isRegistrato = registratoreRemoto.isRegistrato(username);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return isRegistrato;
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
        while (!quit) {
            System.out.printf("$ ");
            currInput = inputUtente.nextLine();
            comandi = currInput.split(" ");
            if(sintassiInputCorretta(currInput, regex)) {
                // fai robe
                switch (comandi[1]) {
                    case "register": {
                        String username = comandi[2];
                        String password = comandi[3];
                        switch (statoClient) {
                            case STARTED: {

                            } break;
                            case EDIT: case LOGGED: {
                                System.err.println("Devi prima eseguire il logout!");
                            } break;
                        }
                    } break;
                    case "login": {
                        switch (statoClient) {
                            case STARTED: {
                                String msg = comandi[1] + " " + comandi[2] + " " + comandi[3];
                                ByteBuffer byteMsg = ByteBuffer.wrap(msg.getBytes());
                                if(Connessione.inviaDati(socket, byteMsg, byteMsg.capacity()) == -1) {
                                    System.err.println("[CLIENT]: Errore inviaDati. Esco..");
                                    System.exit(1);
                                }
                            } break;
                            case EDIT: case LOGGED: {
                                System.err.println("[CLIENT-ERROR]: Devi prima eseguire il logout!");
                            } break;
                        }
                    } break;
                    case "quit": {
                        System.out.println("Bye!");
                        quit = true;
                    } break;
                    case "--help": {
                        printUsage();
                    } break;
                    default: {
                        System.err.println("Comando errato.");
                        System.out.println("Per vedere i comandi disponibili usa: turing --help");
                    }
                }
            }
            else {
                printUsage();
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
