import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

/**
 * Created by EvgeniyShilov on 24.11.2016 at 2:52
 */
public class Transmitter {

    protected DatagramSocket connection;
    protected InetAddress address;
    protected int port;

    protected Transmitter(int port) {
        try {
            connection = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    protected Transmitter setRemote(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        return this;
    }

    protected void send(String data) throws IOException {
        send(Constants.CODE_COMMON_MESSAGE, data);
    }

    protected void send(long number, String data) throws IOException {
        data += "\r\n";
        send(number, data.getBytes(), data.length());
    }

    protected void send(long number, byte[] data, int count) throws IOException {
        connection.send(EnumeratedPacket.getPacketForSend(number, data, count, address, port));
    }

    protected void send(List<Long> data) throws IOException {
        send(Constants.CODE_COMMON_MESSAGE, data);
    }

    protected void send(long number, List<Long> data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteStream);
        for (Long value : data) outputStream.writeLong(value);
        byte[] numbersBytes = byteStream.toByteArray();
        int bytesCount = numbersBytes.length > Constants.BUFFER_SIZE
                ? (Constants.BUFFER_SIZE / Long.BYTES) * Long.BYTES
                : numbersBytes.length;
        send(number, numbersBytes, bytesCount);
    }

    protected EnumeratedPacket receive() throws IOException {
        return receive(Constants.INFINITE_TIMEOUT);
    }

    protected EnumeratedPacket receive(int timeout) throws IOException {
        connection.setSoTimeout(timeout);
        EnumeratedPacket enumeratedPacket = EnumeratedPacket.getPacketForReceive(Constants.BUFFER_SIZE);
        connection.receive(enumeratedPacket.getPacket());
        return enumeratedPacket;
    }
}
