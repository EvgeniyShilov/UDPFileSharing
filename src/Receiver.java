import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by EvgeniyShilov on 24.11.2016 at 3:07
 */
public class Receiver extends Transmitter {

    private BufferedReader user;
    private String currentFilename;
    private Long currentFileSize;
    private List<EnumeratedPacket> packets;

    public Receiver() {
        super(Constants.RECEIVER_PORT);
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        loop:
        while (true) {
            System.out.println("1) Set remote");
            System.out.println("2) Listen");
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
                        Integer port = Constants.SENDER_PORT;
                        setRemote(inetAddress, port);
                    } catch (IOException | NumberFormatException ignored) {
                    }
                    break;
                case '2':
                    listen();
                    break;
                case '3':
                    break loop;
            }
        }
    }

    private void listen() {
        while (true) {
            try {
                EnumeratedPacket packet = receive();
                long code = packet.getNumber();
                if (code == Constants.CODE_COMMON_MESSAGE) {
                    System.out.println(">>> " + packet.getDataAsString());
                    onStartUploading(packet);
                } else if (code == Constants.CODE_IMPORTANT_MESSAGE) {
                    System.out.println(">>> " + packet.getDataAsString());
                    onEOFMessage(packet);
                } else {
                    System.out.println(">>> packet#" + packet.getNumber());
                    onPacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onStartUploading(EnumeratedPacket packet) throws IOException {
        String fileParams = packet.getDataAsString();
        int delimiterIndex = fileParams.indexOf(" ");
        String filename = "server " + fileParams.toLowerCase().substring(0, delimiterIndex).trim();
        long size = Long.valueOf(fileParams.substring(delimiterIndex).trim());
        File file = new File(filename);
        if (!file.exists()) {
            currentFilename = filename;
            currentFileSize = size;
            packets = new ArrayList<>();
            sendMissingPacketsNumbers();
        } else {
            if (currentFilename != null && currentFilename.equals(filename)) {
                sendMissingPacketsNumbers();
            } else {
                System.out.println("<<< File exists");
                send(Constants.CODE_IMPORTANT_MESSAGE, "File exists");
            }
        }
    }

    private void onEOFMessage(EnumeratedPacket packet) throws IOException {
        sendMissingPacketsNumbers();
    }

    private void onPacket(EnumeratedPacket packet) throws IOException {
        packets.add(packet);
    }

    private void sendMissingPacketsNumbers() throws IOException {
        long totalPacketCount = currentFileSize / Constants.BUFFER_SIZE;
        if (currentFileSize % Constants.BUFFER_SIZE != 0) totalPacketCount++;
        List<Long> numbers = new ArrayList<>();
        for (long i = 0; i < totalPacketCount; i++) if (!packetIsUploaded(i)) numbers.add(i);
        if (!numbers.isEmpty()) {
            System.out.print("<<< Missing packets: ");
            for(Long number : numbers) {
                System.out.print(number);
                if(numbers.indexOf(number) != numbers.size() - 1) System.out.print(", ");
                else System.out.println(".");
            }
            send(numbers);
        } else {
            System.out.println("<<< File was uploaded");
            send(Constants.CODE_IMPORTANT_MESSAGE, "File was uploaded");
            createFile();
        }
    }

    private void createFile() throws IOException {
        File file = new File(currentFilename);
        if(!file.exists()) file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        deleteDuplicatePackets();
        Collections.sort(packets, (o1, o2) -> (int)(o1.getNumber() - o2.getNumber()));
        for (EnumeratedPacket packet : packets) {
            byte[] data = packet.getData();
            fos.write(data);
        }
        fos.close();
        currentFileSize = null;
        currentFilename = null;
        packets = null;
    }

    private boolean packetIsUploaded(long packetNumber) {
        for (EnumeratedPacket packet : packets) if (packet.getNumber() == packetNumber) return true;
        return false;
    }

    private void deleteDuplicatePackets() {
        List<EnumeratedPacket> deletedPackets = new ArrayList<>();
        for (int i = 0; i < packets.size(); i++)
            for (int j = i + 1; j < packets.size(); j++)
                if (packets.get(j).getNumber() == packets.get(i).getNumber()) deletedPackets.add(packets.get(j));
        packets.removeAll(deletedPackets);
    }
}
