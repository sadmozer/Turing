import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Contiene i metodi utilizzati, sia dal server che dai client, per
 * l'invio e la ricezione messaggi e files via TCP.
 *
 * @author Niccolo' Cardelli 534015
 */
class Connessione {
    static int inviaDati(SocketChannel socketChannel, Messaggio msg) {
        // Controllo argomenti
        if (msg.getDimBuffer() == 0) {
            System.err.println("InviaDati: Buffer ha capacita 0!");
            return -1;
        }

        int bytesScritti = 0;

        // Invio la dimensione del buffer e il buffer insieme
        ByteBuffer byteTot = ByteBuffer.allocate(Integer.BYTES+msg.getDimBuffer());
        byteTot.putInt(msg.getDimBuffer());
        byteTot.put(msg.getBuffer());
        byteTot.flip();
        try {
            while (byteTot.hasRemaining()) {
                bytesScritti = bytesScritti + socketChannel.write(byteTot);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("InviaDati: Errore scrittura!");
            return -1;
        }
//        System.out.printf("InviaDati: Bytes scritti = %d\n", bytesScritti);

        return bytesScritti;
    }

    static int riceviDati(SocketChannel socketChannel, Messaggio msg) {
        int bytesLetti1 = 0;
        int bytesLetti2 = 0;

        // Prima ricevo la dimensione del buffer
        ByteBuffer byteDim = ByteBuffer.allocate(Integer.BYTES);
        try {
            bytesLetti1 = socketChannel.read(byteDim);
            byteDim.rewind();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
//        System.out.printf("RiceviDati: Bytes letti = %d\n", bytesLetti1);

        int dim = byteDim.getInt();
//        System.out.printf("RiceviDati: Dimensione msg = %d\n", dim);

        // Poi alloco un buffer di una capacita pari alla dimensione
        ByteBuffer buffer = null;
        if (bytesLetti1 > 0) {
            buffer = ByteBuffer.allocate(dim);
        }
        else {
            System.err.println("RiceviDati: Errore prima lettura!");
            return -1;
        }

        // Infine ricevo il buffer vero e proprio
        try {
            bytesLetti2 = socketChannel.read(buffer);
            buffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("RiceviDati: Errore seconda lettura!");
            return -1;
        }
//        System.out.printf("RiceviDati: Bytes letti = %d\n", bytesLetti2);

        if (bytesLetti2 > 0) {
            msg.setBuffer(buffer);
            return bytesLetti1 + bytesLetti2;
        }
        else {
            System.err.println("RiceviDati: Errore seconda lettura!");
            return -1;
        }
    }

    static boolean inviaFile(SocketChannel socket, Path pathFile) {
        FileChannel fileChannel;
        long dimFile;

        try (FileInputStream fileInputStream = new FileInputStream(pathFile.toString())) {
            fileChannel = fileInputStream.getChannel();
            dimFile = fileChannel.size();
            fileChannel.transferTo(0, dimFile, socket);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean riceviFile(SocketChannel socket, Long dimFile, Path pathFile) {
        FileChannel fileChannel;
        long totBytes = 0L;
        try (FileOutputStream fileOutputStream = new FileOutputStream(pathFile.toString())) {
            fileChannel = fileOutputStream.getChannel();
            while (totBytes < dimFile) {
                totBytes += fileChannel.transferFrom(socket, 0, dimFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
