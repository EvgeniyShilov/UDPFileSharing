import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

/**
 * Created by EvgeniyShilov on 24.11.2016 at 2:52
 */
public class Transmitter {

    protected DatagramSocket connection;
    protected InetAddress address;
    protected int port;

    protected Transmitter() {
        try {
            connection = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    protected void setRemote(InetAddress address, int port) {
        this.address = address;
        this.port = port;
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        for(Long value : data) outputStream.write(value.byteValue());
        byte[] numbersBytes = outputStream.toByteArray();
        send(number, numbersBytes, numbersBytes.length);
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
