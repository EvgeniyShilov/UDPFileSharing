import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.stream.Collector;

/**
 * Created by EvgeniyShilov on 24.11.2016 at 0:38
 */
public class EnumeratedPacket implements Comparable {

    private DatagramPacket packet;

    private EnumeratedPacket(int size) {
        byte[] buffer = new byte[size];
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
        LongBuffer longBuffer = ByteBuffer.wrap(getData()).asLongBuffer();
        long[] longArray = new long[longBuffer.capacity()];
        longBuffer.get(longArray);
        for (int i = 0; i < longArray.length; i++) {
            ByteBuffer valueBytes = ByteBuffer.allocate(Long.BYTES).putLong(longArray[i]);
            valueBytes.flip();
            longArray[i] = valueBytes.getLong();
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

    @Override
    public int compareTo(Object o) {
        return (int)(this.getNumber() - ((EnumeratedPacket) o).getNumber());
    }
}
