import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by EvgeniyShilov on 24.11.2016 at 0:38
 */
public class EnumeratedPacket {

    private DatagramPacket packet;

    private EnumeratedPacket(int size) {
        byte[] buffer = new byte[size + Long.BYTES];
        packet = new DatagramPacket(buffer, buffer.length);
    }

    private EnumeratedPacket(long number, byte[] data, int dataLength, InetAddress address, int port) {
        byte[] encapsulatedData = new byte[dataLength + Long.BYTES];
        System.arraycopy(ByteBuffer.allocate(Long.BYTES).putLong(number).array(), 0, encapsulatedData, 0, Long.BYTES);
        System.arraycopy(data, 0, encapsulatedData, Long.BYTES, dataLength);
        packet = new DatagramPacket(encapsulatedData, dataLength + Long.BYTES, address, port);
    }

    public byte[] getData() {
        return Arrays.copyOfRange(packet.getData(), Long.BYTES, packet.getLength());
    }

    public String getDataAsString() {
        byte[] data = getData();
        return (new String(data, 0, data.length)).trim();
    }

    public long[] getDataAsLongArray() {
        byte[] data = getData();
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
        long[] longArray = new long[data.length / Long.BYTES];
        for (int i = 0; i < data.length / Long.BYTES; i++) {
            try {
                longArray[i] = inputStream.readLong();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return longArray;
    }

    public long getNumber() {
        ByteBuffer numberBytes = ByteBuffer.allocate(Long.BYTES)
                .put(Arrays.copyOfRange(packet.getData(), 0, Long.BYTES));
        numberBytes.flip();
        return numberBytes.getLong();
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public static DatagramPacket getPacketForSend(long number, byte[] data, int dataLength,
                                                  InetAddress address, int port) {
        return (new EnumeratedPacket(number, data, dataLength, address, port)).packet;
    }

    public static EnumeratedPacket getPacketForReceive(int size) {
        return new EnumeratedPacket(size);
    }
}
