import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
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

    private String currentFilename;
    private Long currentFileSize;
    private ArrayList<EnumeratedPacket> packets;

    private void listen() {
        while (true) {
            try {
                EnumeratedPacket packet = receive();
                long code = packet.getNumber();
                if (code == Constants.CODE_COMMON_MESSAGE) {
                    onCommonMessage(packet);
                } else if (code == Constants.CODE_IMPORTANT_MESSAGE) {
                    onEOFMessage(packet);
                } else {
                    onPacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onCommonMessage(EnumeratedPacket packet) throws IOException {
        String fileParams = packet.getDataAsString();
        int delimiterIndex = fileParams.indexOf(" ");
        String filename = "server " + fileParams.toLowerCase().substring(0, delimiterIndex).trim();
        long size = Long.valueOf(filename.substring(delimiterIndex).trim());
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
                send(Constants.CODE_IMPORTANT_MESSAGE, "File exists");
            }
        }
    }

    private void onEOFMessage(EnumeratedPacket packet) throws IOException {
        sendMissingPacketsNumbers();
    }

    private void onPacket(EnumeratedPacket packet) throws IOException {
        if (!packetIsUploaded(packet.getNumber())) packets.add(packet);
    }

    private void sendMissingPacketsNumbers() throws IOException {
        long totalPacketCount = currentFileSize / Constants.BUFFER_SIZE;
        if (currentFileSize % Constants.BUFFER_SIZE != 0) totalPacketCount++;
        List<Long> numbers = new ArrayList<>();
        for (long i = 0; i < totalPacketCount; i++) {
            if (!packetIsUploaded(i)) numbers.add(i);
        }
        if (!numbers.isEmpty()) {
            send(numbers);
        } else {
            send(Constants.CODE_IMPORTANT_MESSAGE, "File was uploaded");
            //TODO: create file from packets
        }
    }

    private void createFile() throws IOException {
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(currentFilename), StandardOpenOption.WRITE);
        Collections.sort(packets);
        for(EnumeratedPacket packet : packets) {
            fileChannel.write(ByteBuffer.wrap(packet.getData()), packet.getNumber() * Constants.BUFFER_SIZE);
        }
        fileChannel.close();
        currentFileSize = null;
        currentFilename = null;
    }

    private boolean packetIsUploaded(long packetNumber) {
        for (EnumeratedPacket packet : packets) if (packet.getNumber() == packetNumber) return true;
        return false;
    }
}
