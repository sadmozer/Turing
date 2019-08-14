import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Time;

public class Client {
    private static int DEFAULT_REGISTRY_PORT = 6000;
    private static String DEFAULT_REGISTRY_NAME = "Registratore";
    private static int DEFAULT_CLIENT_PORT = 9999;

    public static IRegistratore setupRegistratore (int serverPort, String serverName) {
        IRegistratore registratoreRemoto = null;
        try {
            Registry reg = LocateRegistry.getRegistry(DEFAULT_REGISTRY_PORT);
            registratoreRemoto = (IRegistratore) reg.lookup(DEFAULT_REGISTRY_NAME);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return null;
        }
        return registratoreRemoto;
    }

    public static void main (String[] args) {
        System.out.println("[CLIENT]: Avvio..");

        // Eseguo il setup del Servizio Registratore
        IRegistratore registratoreRemoto = setupRegistratore(DEFAULT_REGISTRY_PORT, DEFAULT_REGISTRY_NAME);
        if (registratoreRemoto == null) {
            System.err.println("[CLIENT-ERROR]: Impossibile trovare Servizio Registratore. Esco..");
            System.exit(1);
        }
        System.out.println("[CLIENT]: Servizio Registratore trovato.");

        String hostAddress = "";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("[CLIENT-ERROR]: HostAddress non trovato. Esco..");
            System.exit(1);
        }
        SocketChannel client = null;
        InetSocketAddress serverSocketAddress = new InetSocketAddress(hostAddress, DEFAULT_CLIENT_PORT);
        try {
            client = SocketChannel.open();
            client.configureBlocking(false);
            client.connect(serverSocketAddress);
            while (!client.finishConnect()) {
                System.out.println("[CLIENT]: Mi sto connettendo..");
            }
        } catch (UnresolvedAddressException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (client.isConnected()) {
            System.out.printf("[CLIENT]: Connesso a %s sulla porta %d\n", hostAddress, DEFAULT_CLIENT_PORT);
        }
        else {
            System.out.println("[CLIENT]: Connessione fallita!");
        }
        String msg = "Ciao da Client!";
        ByteBuffer saluto = ByteBuffer.allocate(Character.BYTES * msg.length());

        try {
            System.out.println(client.write(saluto));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
//        Operazione esitoRegistra;
//        esitoRegistra = registratoreRemoto.registra("Pippo", "topolinatiamo");




    }
}
