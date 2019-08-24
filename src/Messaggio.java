import java.nio.ByteBuffer;

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

    void appendBuffer(ByteBuffer buffer) {
        ByteBuffer nuovoBuffer = ByteBuffer.allocate(this.buffer.capacity() + buffer.capacity());
        nuovoBuffer.put(this.buffer);
        nuovoBuffer.put(buffer);
        nuovoBuffer.flip();
        this.buffer = nuovoBuffer;
    }

    @Override
    public String toString() {
        return new String(this.buffer.array());
    }
}
