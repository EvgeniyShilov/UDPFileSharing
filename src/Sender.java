import java.io.*;
import java.net.SocketTimeoutException;

/**
 * Created by EvgeniyShilov on 23.11.2016 at 11:51
 */
public class Sender extends Transmitter {

    private BufferedReader user;

    public Sender() {
        super();
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    private String readFilename() {
        try {
            return user.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void upload(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No file");
            return;
        }
        EnumeratedPacket packet;
        while (true) {
            String fileParams = filename + " " + file.length();
            send(fileParams);
            try {
                packet = receive(Constants.LOW_TIMEOUT);
                break;
            } catch (SocketTimeoutException ignored) {
            }
        }
        while (true) {
            if (packet.getNumber() == Constants.CODE_IMPORTANT_MESSAGE) {
                System.out.println(">>> " + packet.getDataAsString());
                return;
            }
            long[] packetNumbers = packet.getDataAsLongArray();
            RandomAccessFile fileReader = new RandomAccessFile(file, "r");
            for (long packetNumber : packetNumbers) {
                fileReader.seek(packetNumber * Constants.BUFFER_SIZE);
                byte[] bytes = new byte[Constants.BUFFER_SIZE];
                int countBytes = fileReader.read(bytes);
                send(packetNumber, bytes, countBytes);
                System.out.println("<<< byte[" + Constants.BUFFER_SIZE + "]");
            }
            fileReader.close();
            while (true) {
                send(Constants.CODE_IMPORTANT_MESSAGE, "Last packet was sent");
                try {
                    packet = receive(Constants.LOW_TIMEOUT);
                    break;
                } catch (SocketTimeoutException ignored) {
                }
            }
        }
    }
}
