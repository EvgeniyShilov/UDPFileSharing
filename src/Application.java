import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by EvgeniyShilov on 23.11.2016 at 11:34
 */
public class Application {

    public static void main(String[] args) {
        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
        loop:
        while (true) {
            System.out.println("Type 's' to send file");
            System.out.println("Type 'r' to receive file");
            System.out.println("Type 'q' to quit");
            String line;
            try {
                line = user.readLine();
                if (line.equals("")) continue;
            } catch (IOException e) {
                continue;
            }
            switch (line.charAt(0)) {
                case 's':
                    Sender sender = new Sender();
                    sender.start();
                    break;
                case 'r':
                    Receiver receiver = new Receiver();
                    break;
                case 'q':
                    break loop;
            }
        }
    }
}
