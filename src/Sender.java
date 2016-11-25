import java.io.*;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Date;

/**
 * Created by EvgeniyShilov on 23.11.2016 at 11:51
 */
public class Sender extends Transmitter {

    private BufferedReader user;

    public Sender() {
        super(Constants.SENDER_PORT);
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
                        if (ip.equals("")) return;
                        InetAddress inetAddress = InetAddress.getByName(ip);
                        Integer port = Constants.RECEIVER_PORT;
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
        Date startTime = new Date();
        EnumeratedPacket packet;
        int retryCount = 0;
        while (true) {
            String fileParams = filename + " " + file.length();
            send(fileParams);
            System.out.println("<<< " + fileParams);
            try {
                packet = receive(Constants.LOW_TIMEOUT);
                break;
            } catch (SocketTimeoutException e) {
                retryCount++;
                if(retryCount == Constants.MAX_RETRY_COUNT) {
                    System.out.println("No response from receiver");
                    return;
                }
            }
        }
        while (true) {
            if (packet.getNumber() == Constants.CODE_IMPORTANT_MESSAGE) {
                System.out.println(">>> " + packet.getDataAsString());
                double speed = (double)(((new Date()).getTime() - startTime.getTime()) * 1000) == 0
                        ? Double.MAX_VALUE
                        :(double) (file.length() * 8) / (double)(((new Date()).getTime() - startTime.getTime()) * 1000);
                System.out.println("Speed: " + speed + " Mbps");
                return;
            }
            long[] packetNumbers = packet.getDataAsLongArray();
            System.out.print(">>> No packets: ");
            for(int i = 0; i < packetNumbers.length; i++) {
                System.out.print(packetNumbers[i]);
                if(i != packetNumbers.length - 1) System.out.print(", ");
                else System.out.println(".");
            }
            RandomAccessFile fileReader = new RandomAccessFile(file, "r");
            for (long packetNumber : packetNumbers) {
                fileReader.seek(packetNumber * Constants.BUFFER_SIZE);
                byte[] bytes = new byte[Constants.BUFFER_SIZE];
                int countBytes = fileReader.read(bytes);
                send(packetNumber, bytes, countBytes);
                System.out.println("<<< packet#" + packetNumber);
            }
            fileReader.close();
            retryCount = 0;
            while (true) {
                System.out.println("<<< Last packet was sent");
                send(Constants.CODE_IMPORTANT_MESSAGE, "Last packet was sent");
                try {
                    packet = receive(Constants.LOW_TIMEOUT);
                    break;
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    if(retryCount == Constants.MAX_RETRY_COUNT) {
                        System.out.println("No response from receiver");
                        return;
                    }
                }
            }
        }
    }
}
