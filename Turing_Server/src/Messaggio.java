import java.nio.ByteBuffer;

/**
 * @author Niccolo' Cardelli 534015
 */
public class Messaggio {
    private ByteBuffer buffer;

    Messaggio() {
        this.buffer = null;
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    void setBuffer(String str) {
        this.buffer = ByteBuffer.wrap(str.getBytes());
    }

    void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    void setBuffer(int num) {
        this.buffer = ByteBuffer.allocate(Integer.BYTES);
        this.buffer.putInt(num);
        this.buffer.flip();
    }
    int getDimBuffer() {
        return this.buffer.capacity();
    }

    void appendBuffer(ByteBuffer buf) {
        ByteBuffer nuovoBuffer = ByteBuffer.allocate(this.buffer.capacity() + buf.capacity());
        nuovoBuffer.put(this.buffer);
        nuovoBuffer.put(buf);
        nuovoBuffer.flip();
        this.buffer = nuovoBuffer;
    }

    void prependBuffer(ByteBuffer buf) {
        ByteBuffer nuovoBuffer = ByteBuffer.allocate(this.buffer.capacity() + buf.capacity());
        nuovoBuffer.put(buf);
        nuovoBuffer.put(this.buffer);
        nuovoBuffer.flip();
        this.buffer = nuovoBuffer;
    }

    @Override
    public String toString() {
        return new String(this.buffer.array());
    }
}
