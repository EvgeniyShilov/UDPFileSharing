import java.io.*;
import java.net.InetAddress;
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

    public void start() {
        loop:
        while (true) {
            System.out.println("1) Set remote");
            System.out.println("2) Upload file");
            System.out.println("3) Quit");
            String line;
            try {
                line = user.readLine();
                if (line.equals("")) continue;
            } catch (IOException e) {
                continue;
            }
            switch (line.charAt(0)) {
                case '1':
                    try {
                        System.out.println("IP?");
                        String ip = user.readLine();
                        System.out.println("port?");
                        String portLine = user.readLine();
                        if (portLine.equals("") || ip.equals("")) return;
                        InetAddress inetAddress = InetAddress.getByName(ip);
                        Integer port = Integer.parseInt(portLine);
                        setRemote(inetAddress, port);
                    } catch (IOException | NumberFormatException ignored) {
                    }
                    break;
                case '2':
                    String filename = readLineFromUser();
                    try {
                        upload(filename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case '3':
                    break loop;
            }
        }
    }

    private String readLineFromUser() {
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
