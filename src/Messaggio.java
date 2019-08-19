import java.nio.ByteBuffer;

public class Messaggio {
    private ByteBuffer buffer;
    private int dimBuffer;

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
        this.buffer.putInt(num);
    }
    int getDimBuffer() {
        return this.buffer.capacity();
    }

    @Override
    public String toString() {
        return new String(this.buffer.array());
    }
}
