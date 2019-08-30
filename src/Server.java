import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

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
        GestoreDocumenti gestoreDocumenti = new GestoreDocumenti(DEFAULT_DOCS_DIRECTORY, "239.0.0.0");

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
                    SocketChannel client;
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
                    Allegato allegato;
                    if ((allegato = (Allegato) key.attachment()) == null) {
                        allegato = new Allegato();
                    }

                    if (allegato.getPathFileDaRicevere() != null) {
                        if (!Connessione.riceviFile(client, allegato.getDimFileDaRicevere(), allegato.getPathFileDaRicevere())) {
                            System.err.println("[SERVER-ERROR]: Errore riceviFile.");
                            msgRisposta.setBuffer(203);
                        }
                        else {
                            allegato.setPathFileDaRicevere(null);
                            System.out.println("[SERVER]: File ricevuto con successo.");
                            msgRisposta.setBuffer(200);
                        }
                    }
                    else if((Connessione.riceviDati(client, msgRicevuto)) == -1) {
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
                                Utente utente;

                                allegato.setMessaggio(msgRisposta);

                                if ((utente = registratore.getUtente(username)) == null) {
                                    msgRisposta.setBuffer(201);
                                    System.err.println("[SERVER-ERROR]: Utente non registrato.");
                                }
                                else if (!registratore.getUtente(username).getPassword().equals(password)){
                                    msgRisposta.setBuffer(202);
                                    System.err.println("[SERVER-ERROR]: Password errata.");
                                }
                                else if (gestoreSessioni.isLoggato(username)) {
                                    msgRisposta.setBuffer(203);
                                    allegato.setUtente(utente);
                                    System.err.printf("[SERVER-ERROR]: Utente %s gia' loggato.%n", utente.getUsername());
                                }
                                else if (!gestoreSessioni.login(username, registratore.getUtente(username))){
                                    msgRisposta.setBuffer(199);
                                    System.err.printf("[SERVER-ERROR]: Errore login %s%n.", utente.getUsername());
                                }
                                else {
                                    if (!gestoreSessioni.addAllegato(allegato, utente)) {
                                        System.out.println("[SERVER]: Allegato gia' presente.");
                                    }
                                    msgRisposta.setBuffer(200);
                                    allegato.setUtente(utente);
                                    System.out.printf("[SERVER]: Login %s effettuato.%n", utente.getUsername());
                                }
                            } break;
                            case "logout": {
                                Utente utente = allegato.getUtente();
                                String username = utente.getUsername();
                                allegato.setMessaggio(msgRisposta);
                                if (!gestoreSessioni.logout(username)){
                                    System.err.printf("[SERVER-ERROR]: Errore logout %s.%n", username);
                                    msgRisposta.setBuffer(205);
                                }
                                else {
                                    System.out.printf("[SERVER]: Logout %s effettuato.%n", username);
                                    msgRisposta.setBuffer(200);
                                }
                            } break;
                            case "create": {
                                Utente utente = allegato.getUtente();
                                String username = utente.getUsername();
                                String nomeDoc = comandi[1];
                                int numSezioni = Integer.parseInt(comandi[2]);
                                allegato.setMessaggio(msgRisposta);

                                if (!gestoreDocumenti.creaDirectoryDocumenti(utente)) {
                                    System.err.println("[SERVER-ERROR]: Impossibile creare directory documenti.");
                                    msgRisposta.setBuffer(205);
                                }
                                else if (gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Documento gia presente.");
                                    msgRisposta.setBuffer(201);
                                }
                                else if (!gestoreDocumenti.creaDocumento(nomeDoc, utente, numSezioni)){
                                    System.err.println("[SERVER-ERROR]: Errore crea documento.");
                                    msgRisposta.setBuffer(203);
                                }
                                else {
                                    System.out.printf("[SERVER]: Documento %s di %s creato.%n", nomeDoc, username);
                                    msgRisposta.setBuffer(200);


                                }
                            } break;
                            case "list": {
                                Utente utente = allegato.getUtente();
                                String listaDoc;
                                allegato.setMessaggio(msgRisposta);

                                if ((listaDoc = gestoreDocumenti.getListaDocumenti(utente)) == null) {
                                    System.out.println("[SERVER-ERROR]: Non ha documenti.");
                                    msgRisposta.setBuffer(201);
                                }
                                else {
                                    System.out.println("[SERVER]: Lista inviata.");
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
                                String username = utente.getUsername();
                                String nomeDoc = comandi[1];
                                String usernameInvitato = comandi[2];
                                allegato.setMessaggio(msgRisposta);

                                if (username.equals(usernameInvitato)) {
                                    System.err.println("[SERVER-ERROR]: Autoinvito.");
                                    msgRisposta.setBuffer(206);
                                }
                                else if (!registratore.isRegistrato(usernameInvitato)) {
                                    System.err.printf("[SERVER-ERROR]: Errore utenteInvitato %s non registrato.%n", usernameInvitato);
                                    msgRisposta.setBuffer(205);
                                }
                                else if (!gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Non esiste documento.");
                                    msgRisposta.setBuffer(207);
                                }
                                else if (!gestoreDocumenti.isCreatore(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Utente non creatore non puo' invitare.");
                                    msgRisposta.setBuffer(210);
                                }
                                else if (gestoreDocumenti.haDocumento(nomeDoc, registratore.getUtente(usernameInvitato))) {
                                    System.err.println("[SERVER-ERROR]: Documento con stesso nome.");
                                    msgRisposta.setBuffer(209);
                                }
                                else if (gestoreDocumenti.isCollaboratore(usernameInvitato, nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Utente gia' collaboratore.");
                                    msgRisposta.setBuffer(208);
                                }
                                else if (!gestoreDocumenti.condividiDocumento(nomeDoc, utente, registratore.getUtente(usernameInvitato))) {
                                    System.err.println("[SERVER-ERROR]: Errore condivisione documento");
                                    msgRisposta.setBuffer(203);
                                }
                                else {
                                    ByteBuffer bufferNotifica = ByteBuffer.allocate(4*Integer.BYTES + utente.getUsername().getBytes().length + nomeDoc.getBytes().length);
                                    bufferNotifica.putInt(100);
                                    bufferNotifica.putInt(utente.getUsername().getBytes().length);
                                    bufferNotifica.put(utente.getUsername().getBytes());
                                    bufferNotifica.putInt(nomeDoc.getBytes().length);
                                    bufferNotifica.put(nomeDoc.getBytes());
                                    bufferNotifica.putInt(gestoreDocumenti.getNumSezioni(nomeDoc, utente));
                                    bufferNotifica.flip();

                                    Messaggio invito = new Messaggio();
                                    invito.setBuffer(bufferNotifica);
                                    gestoreSessioni.addNotifica(invito, registratore.getUtente(usernameInvitato));

                                    msgRisposta.setBuffer(200);
                                }
                            } break;
                            case "show1": {
                                Utente utente = allegato.getUtente();
                                Utente occupante;
                                String nomeDoc = comandi[1];
                                int numSezione = Integer.parseInt(comandi[2]);
                                allegato.setMessaggio(msgRisposta);

                                if (!gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Non ha documento.%n");
                                    msgRisposta.setBuffer(204);
                                }
                                else if (numSezione >= gestoreDocumenti.getNumSezioni(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: numSezione out of bound.%n");
                                    msgRisposta.setBuffer(206);
                                }
                                else if ((occupante = gestoreDocumenti.isEditing(nomeDoc, numSezione, utente)) != null) {
                                    System.err.printf("[SERVER]: %s sta occupando documento %d .%n", occupante.getUsername(), occupante.getUsername().getBytes().length);
                                    msgRisposta.setBuffer(200);

                                    ByteBuffer dimBuf = ByteBuffer.allocate(Integer.BYTES);
                                    dimBuf.putInt(occupante.getUsername().getBytes().length);
                                    dimBuf.flip();
                                    msgRisposta.appendBuffer(dimBuf);

                                    msgRisposta.appendBuffer(ByteBuffer.wrap(occupante.getUsername().getBytes()));

                                    ByteBuffer dimensioneSezione = ByteBuffer.allocate(Long.BYTES);
                                    dimensioneSezione.putLong(gestoreDocumenti.getDimSezione(nomeDoc, numSezione, utente));
                                    dimensioneSezione.flip();

                                    msgRisposta.appendBuffer(dimensioneSezione);

                                    allegato.pushFileDaInviare(Paths.get(gestoreDocumenti.getPathSezione(nomeDoc, numSezione, utente)));
                                }
                                else {
                                    System.out.printf("[SERVER]: Show1 documento %s sez %d utente %s.%n", nomeDoc, numSezione, utente.getUsername());

                                    msgRisposta.setBuffer(201);

                                    ByteBuffer dimensioneSezione = ByteBuffer.allocate(Long.BYTES);
                                    dimensioneSezione.putLong(gestoreDocumenti.getDimSezione(nomeDoc, numSezione, utente));
                                    dimensioneSezione.flip();

                                    msgRisposta.appendBuffer(dimensioneSezione);

                                    allegato.pushFileDaInviare(Paths.get(gestoreDocumenti.getPathSezione(nomeDoc, numSezione, utente)));
                                }
                            } break;
                            case "show2": {
                                Utente utente = allegato.getUtente();
                                Utente occupante;
                                String nomeDoc = comandi[1];
                                int numSezioni;
                                allegato.setMessaggio(msgRisposta);

                                if (!gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Non ha documento.%n");
                                    msgRisposta.setBuffer(204);
                                }
                                else {
                                    numSezioni = gestoreDocumenti.getNumSezioni(nomeDoc, utente);
                                    LinkedList<String> utentiAttivi = new LinkedList<>();
                                    LinkedList<Integer> sezEditata = new LinkedList<>();
                                    LinkedList<Integer> dimUtenti = new LinkedList<>();
                                    int dimTotale = 0;
                                    for (int i = 0; i < numSezioni; i++) {
                                        if ((occupante = gestoreDocumenti.isEditing(nomeDoc, i, utente)) != null) {
                                            utentiAttivi.add(occupante.getUsername());
                                            dimUtenti.add(occupante.getUsername().length());
                                            dimTotale += occupante.getUsername().length();
                                            sezEditata.add(i);
                                        }
                                    }

                                    ByteBuffer buf = ByteBuffer.allocate((utentiAttivi.size() + 2) * Integer.BYTES + dimTotale);
                                    Iterator<String> itUtenti = utentiAttivi.iterator();
                                    Iterator<Integer> itDim = dimUtenti.iterator();
                                    Iterator<Integer> itSez = sezEditata.iterator();

                                    // Numero di utenti attivi
                                    buf.putInt(utentiAttivi.size());

                                    while (itUtenti.hasNext()) {
                                        // Dimensione del nome utente
                                        buf.putInt(itDim.next());

                                        // Nome Utente
                                        buf.put(itUtenti.next().getBytes());

                                        // Numero di sezione editata
                                        buf.putInt(itSez.next());
                                    }

                                    // Numero di sezioni del documento
                                    buf.putInt(numSezioni);
                                    buf.flip();

                                    System.out.printf("[SERVER]: Show2 documento %s utente %s.%n", nomeDoc, utente.getUsername());

                                    msgRisposta.setBuffer(200);
                                    msgRisposta.appendBuffer(buf);

                                    for (int i = 1; i <= numSezioni; i++) {
                                        ByteBuffer dimensioneSezione = ByteBuffer.allocate(Long.BYTES);
                                        dimensioneSezione.putLong(gestoreDocumenti.getDimSezione(nomeDoc, i, utente));
                                        dimensioneSezione.flip();

                                        msgRisposta.appendBuffer(dimensioneSezione);

                                        allegato.pushFileDaInviare(Paths.get(gestoreDocumenti.getPathSezione(nomeDoc, i, utente)));
                                    }
                                }
                            } break;
                            case "edit": {
                                Utente utente = allegato.getUtente();
                                Utente occupante;
                                String nomeDoc = comandi[1];
                                int numSezione = Integer.parseInt(comandi[2]);
                                allegato.setMessaggio(msgRisposta);

                                if (!gestoreDocumenti.haDocumento(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: Non ha documento.%n");
                                    msgRisposta.setBuffer(204);
                                }
                                else if (numSezione >= gestoreDocumenti.getNumSezioni(nomeDoc, utente)) {
                                    System.err.println("[SERVER-ERROR]: numSezione out of bound.%n");
                                    msgRisposta.setBuffer(206);
                                }
                                else if ((occupante = gestoreDocumenti.isEditing(nomeDoc, numSezione, utente)) != null && !occupante.equals(utente)) {
                                    System.err.printf("[SERVER-ERROR]: %s sta occupando documento %d .%n", occupante.getUsername(), occupante.getUsername().getBytes().length);
                                    msgRisposta.setBuffer(207);

                                    ByteBuffer dimBuf = ByteBuffer.allocate(Integer.BYTES);
                                    dimBuf.putInt(occupante.getUsername().getBytes().length);
                                    dimBuf.flip();
                                    msgRisposta.appendBuffer(dimBuf);

                                    msgRisposta.appendBuffer(ByteBuffer.wrap(occupante.getUsername().getBytes()));
                                }
                                else if (occupante != null && occupante.equals(utente)) {
                                    System.err.println("[SERVER-ERROR]: Utente sta gia' modificando documento.%n");
                                    msgRisposta.setBuffer(205);
                                }
                                else if (!gestoreDocumenti.inizioEditing(nomeDoc, numSezione, utente)) {
                                    System.err.println("[SERVER-ERROR]: Errore inizioEditing.%n");
                                    msgRisposta.setBuffer(203);
                                }
                                else {
                                    System.out.printf("[SERVER]: InizioEditing documento %s sez %d utente %s.%n", nomeDoc, numSezione, utente.getUsername());

                                    msgRisposta.setBuffer(200);

                                    String striIpChat = gestoreDocumenti.getChatIpDocumento(nomeDoc, utente);
                                    ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + striIpChat.getBytes().length);
                                    buf.putInt(striIpChat.getBytes().length);
                                    buf.put(striIpChat.getBytes());
                                    buf.flip();

                                    msgRisposta.appendBuffer(buf);

                                    ByteBuffer dimensioneSezione = ByteBuffer.allocate(Long.BYTES);
                                    dimensioneSezione.putLong(gestoreDocumenti.getDimSezione(nomeDoc, numSezione, utente));
                                    dimensioneSezione.flip();

                                    msgRisposta.appendBuffer(dimensioneSezione);

                                    allegato.pushFileDaInviare(Paths.get(gestoreDocumenti.getPathSezione(nomeDoc, numSezione, utente)));
                                }
                            } break;
                            case "end-edit": {
                                allegato = (Allegato) key.attachment();
                                Utente utente = allegato.getUtente();
                                String nomeDoc = comandi[1];
                                int numSezione = Integer.parseInt(comandi[2]);
                                long dimFile = Long.parseLong(comandi[3]);
                                String strPathFile = gestoreDocumenti.getPathSezione(nomeDoc, numSezione, utente);
                                System.out.printf("[SERVER]: Verra' sovrascritto il file %s.%n", strPathFile);
                                allegato.setMessaggio(msgRisposta);

                                msgRisposta.setBuffer(200);
                                allegato.setPathFileDaRicevere(Paths.get(strPathFile));
                                allegato.setDimFileDaRicevere(dimFile);

                                System.out.printf("[SERVER]: FineEditing documento %s sez %d utente %s.%n", nomeDoc, numSezione, utente.getUsername());
                                gestoreDocumenti.fineEditing(nomeDoc, numSezione, allegato.getUtente());
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
                    Utente utente;

                    if (allegato == null) {
                        System.err.println("[SERVER-ERROR]: Allegato vuoto. Chiudo la connessione.");
                        key.cancel();
                    }
                    else {
                        if ((utente = allegato.getUtente()) != null){
                            while (gestoreSessioni.haNotifiche(utente)) {
                                allegato.getMessaggio().prependBuffer(gestoreSessioni.popNotifica(utente).getBuffer());
                                System.out.println("[SERVER]: Notifica aggiunta al messaggio.");
                            }
                        }

                        if(Connessione.inviaDati(client, allegato.getMessaggio()) == -1) {
                            System.err.println("[SERVER-ERROR]: Errore riceviDati. Chiudo la connessione.");
                            key.cancel();
                        }
                        else {
                            System.out.println("[SERVER]: Messaggio inviato con successo!");
                            allegato.setMessaggio(null);
                        }

                        boolean erroreInvio = false;
                        while (allegato.haFileDaInviare() && !erroreInvio) {
                            if ((erroreInvio = !Connessione.inviaFile(client, allegato.popFileDaInviare()))) {
                                System.err.println("[SERVER-ERROR]: Errore inviaFile");
                            }
                        }
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
