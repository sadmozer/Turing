import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static int DEFAULT_REGISTRY_PORT = 6000;
    private static String DEFAULT_REGISTRY_NAME = "Registratore";
    private static int DEFAULT_SERVER_PORT = 9999;
    private static long SELECTOR_TIMEOUT = 3000L;

    private static Registratore setupRegistratore(int registryPort, String registryName) {
        Registratore registratore = null;
        try {
            registratore = new Registratore();
            LocateRegistry.createRegistry(registryPort);
            Registry reg = LocateRegistry.getRegistry(registryPort);
            reg.rebind(registryName, registratore);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        return registratore;
    }

    private static ServerSocketChannel setupServerSocket(int serverPort) {
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(serverPort));
            serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return serverSocketChannel;
    }

    private static Selector setupServerSelector(ServerSocketChannel serverSocketChannel) {
        Selector serverSelector = null;
        try {
            serverSelector = Selector.open();
            serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return serverSelector;
    }

    private static int trySelect(Selector selector, long timeout) {
        int numPronti = 0;
        try {
            selector.select(timeout);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return numPronti;
    }

    public static void main(String[] args) {
        // Eseguo il setup del servizio Registratore
        Registratore registratore = setupRegistratore(DEFAULT_REGISTRY_PORT, DEFAULT_REGISTRY_NAME);
        if (registratore == null) {
            System.err.println("[SERVER-ERROR]: Impossibile avviare servizio Registratore. Esco..");
            System.exit(1);
        }
        System.out.println("[SERVER]: Servizio Registratore avviato.");

        // Eseguo il setup del Server Socket
        ServerSocketChannel serverSocketChannel = setupServerSocket(DEFAULT_SERVER_PORT);
        if (serverSocketChannel == null) {
            System.err.println("[SERVER-ERROR]: Errore configurazione Server Socket. Esco..");
            System.exit(1);
        }

        String hostAddress = "";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("[SERVER-ERROR]: HostAddress non trovato. Esco..");
            System.exit(1);
        }
        System.out.printf("[SERVER]: %s in ascolto sulla porta %d\n", hostAddress, DEFAULT_SERVER_PORT);

        // Eseguo il setup del Server Selector
        Selector serverSelector = setupServerSelector(serverSocketChannel);
        if (serverSelector == null) {
            System.err.println("[SERVER-ERROR]: Errore configurazione Server Selector. Esco..");
            System.exit(1);
        }
        System.out.println("[SERVER]: Server Selector configurato.");

        // Mi metto in ascolto
        int count = 0;
        while (true) {
            int numCanaliPronti = trySelect(serverSelector, SELECTOR_TIMEOUT);
            if (numCanaliPronti < 0) {
                System.err.println("[SERVER]: Errore select. Esco..");
                System.exit(1);
            }
            else if (numCanaliPronti == 0){
                // Nessun canale pronto
                System.out.println("[SERVER]: ("+count+") Nessun canale pronto..");
                count++;
            }
            else {
                // Almeno un canale è pronto
                Set<SelectionKey> selectedKeys = serverSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    keyIterator.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        // Si apre un socket per comunicare con il client che ha fatto richiesta
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = null;
                        SocketAddress clientAddress = null;
                        try {
                            client = server.accept();
                            client.configureBlocking(false);
                            client.register(serverSelector, SelectionKey.OP_READ);
                            clientAddress = client.getRemoteAddress();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("[SERVER-ERROR]: Impossibile accettare Client. Esco..");
                            System.exit(1);
                        }
                        System.out.printf("[SERVER]: Connesso con il client %s\n", clientAddress);
                    }
                    else if (key.isValid() && key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        System.out.println("[SERVER]: Evento scrittura!");
                        ByteBuffer msg = null;
                        if((msg = Connessione.riceviDati(client)) == null) {
                            System.err.println("[SERVER-ERROR]: Errore riceviDati. Chiudo la connessione.");
                            key.cancel();
                        }
                        else {
                            System.out.printf("[SERVER]: Messaggio ricevuto: %s\n", new String(msg.array()));
                        }
                    }
                    else if (key.isValid() && key.isWritable()) {
                        System.out.println("[SERVER]: Evento lettura!");
                    }
                }
            }
        }
    }
}
