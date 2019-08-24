import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int DEFAULT_REGISTRY_PORT = 6000;
    private static final String DEFAULT_REGISTRY_NAME = "Registratore";
    private static final int DEFAULT_SERVER_PORT = 9999;
    private static final long SELECTOR_TIMEOUT = 3000L;
    private static String DEFAULT_DOCS_DIRECTORY = System.getProperty("user.dir") + File.separator + "data_server";

    // Forse da spostare in Registratore
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

    // Forse da spostare in altra classe
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
            numPronti = selector.select(timeout);
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

        GestoreSessioni gestoreSessioni = new GestoreSessioni();
        GestoreDocumenti gestoreDocumenti = new GestoreDocumenti(DEFAULT_DOCS_DIRECTORY);
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
                System.out.println("\r[SERVER]: (" + count + ") Nessun canale pronto..");
                count++;
                continue;
            }

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
                    Messaggio msgRicevuto = new Messaggio();
                    Messaggio msgRisposta = new Messaggio();
                    Allegato allegato = null;

                    if((Connessione.riceviDati(client, msgRicevuto)) == -1) {
                        System.err.println("[SERVER-ERROR]: Errore riceviDati. Chiudo la connessione.");
                        key.cancel();
                    }
                    else {
                        System.out.printf("[SERVER]: Messaggio ricevuto: %s\n", msgRicevuto.toString());
                        String[] comandi = msgRicevuto.toString().split(" ");
                        String operazione = comandi[0];
                        switch (operazione) {
                            case "login": {
                                String username = comandi[1];
                                String password = comandi[2];
                                Utente utente = null;
                                allegato = new Allegato();
                                allegato.setMessaggio(msgRisposta);

                                if ((utente = registratore.getUtente(username)) == null) {
                                    msgRisposta.setBuffer(201);
                                    System.err.println("[SERVER-ERROR]: Utente non registrato.");
                                }
                                else if (!registratore.getUtente(username).getPassword().equals(password)){
                                    msgRisposta.setBuffer(202);
                                    allegato.setUtente(utente);
                                    System.err.println("[SERVER-ERROR]: Password errata.");
                                }
                                else if (gestoreSessioni.isLoggato(username)) {
                                    msgRisposta.setBuffer(203);
                                    allegato.setUtente(utente);
                                    System.err.printf("[SERVER-ERROR]: Utente %s gia' loggato.%n", utente.getUsername());
                                }
                                else if (!gestoreSessioni.login(username, registratore.getUtente(username))){
                                    msgRisposta.setBuffer(199);
                                    allegato.setUtente(utente);
                                    System.err.printf("[SERVER-ERROR]: Errore login %s%n.", utente.getUsername());
                                }
                                else {
                                    if (!gestoreSessioni.addAllegato(allegato, utente)) {
                                        System.out.println("[SERVER]: Allegato gia' presente.");
                                    }
                                    msgRisposta.setBuffer(200);
                                    while (gestoreSessioni.haNotifiche(utente)) {
                                        msgRisposta.appendBuffer(gestoreSessioni.popNotifica(utente).getBuffer());
                                    }
                                    allegato.setUtente(utente);
                                    System.out.printf("[SERVER]: Login %s effettuato.%n", utente.getUsername());
                                }
                            } break;
                            case "logout": {
                                Utente utente = null;
                                String username = null;

                                if ((allegato = (Allegato) key.attachment()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto0.");
                                    allegato = new Allegato();
                                    msgRisposta.setBuffer(203);
                                }
                                else if ((utente = allegato.getUtente()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto1.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if((username = utente.getUsername()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto2.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if (!gestoreSessioni.isLoggato(username)){
                                    System.err.printf("[SERVER-ERROR]: Errore utente %s non loggato.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(204);
                                }
                                else if (!gestoreSessioni.logout(username)){
                                    System.err.printf("[SERVER-ERROR]: Errore logout %s.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(205);
                                }
                                else {
                                    System.out.printf("[SERVER]: Logout %s effettuato.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(200);
                                }
                            } break;
                            case "create": {
                                Utente utente = null;
                                String username = "";
                                String nomeDoc = comandi[1];
                                int numSezioni = Integer.parseInt(comandi[2]);

                                if ((allegato = (Allegato) key.attachment()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto0.");
                                    allegato = new Allegato();
                                    msgRisposta.setBuffer(203);
                                }
                                else if ((utente = allegato.getUtente()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto1.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if((username = utente.getUsername()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto2.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if (!gestoreSessioni.isLoggato(username)) {
                                    System.err.printf("[SERVER-ERROR]: Errore utente %s non loggato.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(204);
                                }
                                else if (!gestoreDocumenti.creaDirectoryDocumenti(utente)) {
                                    System.err.println("[SERVER-ERROR]: Impossibile creare directory documenti.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(205);
                                }
                                else if (gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Documento gia presente.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(201);
                                }
                                else if (!gestoreDocumenti.creaDocumento(nomeDoc, utente, numSezioni)){
                                    System.err.println("[SERVER-ERROR]: Documento gia presente.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(201);
                                }
                                else {
                                    System.out.printf("[SERVER]: Documento %s di %s creato.%n", nomeDoc, username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(200);
                                }
                            } break;
                            case "list": {
                                Utente utente = null;
                                String username = "";
                                String listaDoc = "";
                                if ((allegato = (Allegato) key.attachment()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto0.");
                                    allegato = new Allegato();
                                    msgRisposta.setBuffer(203);
                                }
                                else if ((utente = allegato.getUtente()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto1.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if((username = utente.getUsername()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto2.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if (!gestoreSessioni.isLoggato(username)) {
                                    System.err.printf("[SERVER-ERROR]: Errore utente %s non loggato.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(204);
                                }
                                else if ((listaDoc = gestoreDocumenti.getListaDocumenti(utente)) == null) {
                                    System.out.println("[SERVER-ERROR]: Non ha documenti.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(201);
                                }
                                else {
                                    System.out.println("[SERVER]: Lista inviata.");
                                    allegato.setMessaggio(msgRisposta);
                                    ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES + listaDoc.getBytes().length);
                                    buf.putInt(200);
                                    buf.putInt(listaDoc.getBytes().length);
                                    buf.put(listaDoc.getBytes());
                                    buf.flip();
                                    msgRisposta.setBuffer(buf);
                                }
                            } break;
                            case "share": {
                                Utente utente = allegato.getUtente();
                                String username = null;
                                String nomeDoc = comandi[1];
                                String usernameInvitato = comandi[2];

                                if ((allegato = (Allegato) key.attachment()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto0.");
                                    allegato = new Allegato();
                                    msgRisposta.setBuffer(203);
                                }
                                else if ((utente = allegato.getUtente()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto1.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if((username = utente.getUsername()) == null) {
                                    System.err.println("[SERVER-ERROR]: Errore utente sconosciuto2.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(203);
                                }
                                else if (username.equals(usernameInvitato)) {
                                    System.err.println("[SERVER-ERROR]: Autoinvito.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(206);
                                }
                                else if (!gestoreSessioni.isLoggato(username)) {
                                    System.err.printf("[SERVER-ERROR]: Errore utente %s non loggato.%n", username);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(204);
                                }
                                else if (!registratore.isRegistrato(usernameInvitato)) {
                                    System.err.printf("[SERVER-ERROR]: Errore utenteInvitato %s non registrato.%n", usernameInvitato);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(205);
                                }
                                else if (!gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.printf("[SERVER-ERROR]: Non esiste documento.%n", usernameInvitato);
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(207);
                                }
                                else if (gestoreDocumenti.isCollaboratore(registratore.getUtente(usernameInvitato), nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Utente gia' collaboratore.");
                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(208);
                                }
                                else {
                                    ByteBuffer codNotifica = ByteBuffer.allocate(Integer.BYTES);
                                    codNotifica.putInt(100);
                                    codNotifica.flip();

                                    String invitoCollab = utente.getUsername() + " ti ha invitato a collaborare ad un suo documento!%n Ora puoi accedere e modificare il documento " + nomeDoc + ".";
                                    ByteBuffer dimInvito = ByteBuffer.allocate(Integer.BYTES);
                                    dimInvito.putInt(invitoCollab.getBytes().length);
                                    dimInvito.flip();
                                    ByteBuffer bufInvito = ByteBuffer.allocate(invitoCollab.getBytes().length);
                                    bufInvito.put(invitoCollab.getBytes());
                                    bufInvito.flip();

                                    Messaggio invito = new Messaggio();
                                    invito.setBuffer(codNotifica);
                                    invito.appendBuffer(dimInvito);
                                    invito.appendBuffer(bufInvito);

                                    if (gestoreSessioni.isLoggato(usernameInvitato)) {
                                        msgRisposta.appendBuffer(invito.getBuffer());
                                    }
                                    else {
                                        gestoreSessioni.addNotifica(invito, registratore.getUtente(usernameInvitato));
                                    }

                                    allegato.setMessaggio(msgRisposta);
                                    msgRisposta.setBuffer(210);
                                }
                            } break;
                        }

                        // Preparo il canale per l'invio della risposta
                        try {
                            client.register(serverSelector, SelectionKey.OP_WRITE, allegato);
                        } catch (ClosedChannelException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if (key.isValid() && key.isWritable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    Allegato allegato = (Allegato) key.attachment();

                    if (allegato == null) {
                        System.err.println("[SERVER-ERROR]: Allegato vuoto. Chiudo la connessione.");
                        key.cancel();
                    }
                    else if(Connessione.inviaDati(client, allegato.getMessaggio()) == -1) {
                        System.err.println("[SERVER-ERROR]: Errore riceviDati. Chiudo la connessione.");
                        key.cancel();
                    }
                    else {
                        System.out.println("[SERVER]: Messaggio inviato con successo!");
                    }

                    // Preparo il canale per ricevere nuove operazioni
                    try {
                        client.register(serverSelector, SelectionKey.OP_READ, allegato);
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    }

                }
                else {
                    key.cancel();
                }
            }

        }
    }
}
