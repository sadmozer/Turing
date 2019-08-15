import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class Connessione {
    public static int inviaDati(SocketChannel socketChannel, ByteBuffer buffer, int dimBuffer) {
        // Controllo argomenti
        if (buffer.capacity() == 0) {
            System.err.println("InviaDati: Buffer ha capacita 0!");
            return -1;
        }
        if(dimBuffer <= 0) {
            System.err.println("InviaDati: dimBuffer non valido!");
            return -1;
        }

        int bytesScritti1 = 0;
        int bytesScritti2 = 0;

        // Prima invio la dimensione del buffer
        ByteBuffer byteDim = ByteBuffer.allocate(Integer.BYTES);
        byteDim.putInt(dimBuffer);
        byteDim.flip();
        try {
            while (byteDim.hasRemaining()) {
                bytesScritti1 = bytesScritti1 + socketChannel.write(byteDim);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("InviaDati: Errore prima scrittura!");
            return -1;
        }
        System.out.printf("InviaDati: Bytes scritti = %d\n", bytesScritti1);

        // Poi invio nel canale il buffer vero e proprio
        try {
            while (buffer.hasRemaining()) {
                bytesScritti2 = bytesScritti2 + socketChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("InviaDati: Errore seconda scrittura!");
            return -1;
        }
        System.out.printf("InviaDati: Bytes scritti = %d\n", bytesScritti2);

        return bytesScritti1 + bytesScritti2;
    }

    public static ByteBuffer riceviDati(SocketChannel socketChannel) {
        int bytesLetti1 = 0;
        int bytesLetti2 = 0;

        // Prima ricevo la dimensione del buffer
        ByteBuffer byteDim = ByteBuffer.allocate(Integer.BYTES);
        try {
            bytesLetti1 = socketChannel.read(byteDim);
            byteDim.rewind();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        System.out.printf("RiceviDati: Bytes letti = %d\n", bytesLetti1);

        int dim = byteDim.getInt();
        System.out.printf("RiceviDati: Dimensione msg = %d\n", dim);

        // Poi alloco un buffer di una capacita pari alla dimensione
        ByteBuffer buffer = null;
        if (bytesLetti1 > 0) {
            buffer = ByteBuffer.allocate(dim);
        }
        else {
            System.err.println("RiceviDati: Errore prima lettura!");
            return null;
        }

        // Infine ricevo il buffer vero e proprio
        try {
            bytesLetti2 = socketChannel.read(buffer);
            buffer.rewind();
            buffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("RiceviDati: Errore seconda lettura!");
            return null;
        }
        System.out.printf("RiceviDati: Bytes letti = %d\n", bytesLetti2);

        if (bytesLetti2 > 0) {
            return buffer;
        }
        else {
            System.err.println("RiceviDati: Errore seconda lettura!");
            return null;
        }
    }

}
