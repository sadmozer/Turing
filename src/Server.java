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

    public static Registratore setupRegistratore(int registryPort, String registryName) {
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

    public static ServerSocketChannel setupServerSocket(int serverPort) {
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

    public static Selector setupServerSelector(ServerSocketChannel serverSocketChannel) {
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

        while (true) {
            int numCanaliPronti = 0;
            try {
                numCanaliPronti = serverSelector.select(SELECTOR_TIMEOUT);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("[SERVER]: Errore select. Esco..");
                System.exit(1);
            }

            if (numCanaliPronti == 0){
                // Nessun canale pronto
                System.out.println("[SERVER]: Canali Registrati:");
                System.out.printf("[SERVER]: %s\n", serverSelector.keys().toArray());
                System.out.println("[SERVER]: Nessun canale pronto..");
            }
            else {
                // Almeno un canale è pronto
                Set<SelectionKey> selectedKeys = serverSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    keyIterator.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = null;
                        SocketAddress clientAddress = null;
                        try {
                            client = server.accept();
                            client.configureBlocking(false);
                            client.register(serverSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
                            System.err.println("[SERVER]: Errore riceviDati. Esco..");
                            System.exit(1);
                        }
                        System.out.printf("[SERVER]: Messaggio ricevuto: %s\n", new String(msg.array()));
                        System.exit(0);
                    }
                    else if (key.isValid() && key.isWritable()) {
                        System.out.println("[SERVER]: Evento lettura!");
                    }
                }
            }
        }
    }
}
