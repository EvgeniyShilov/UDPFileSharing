import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by EvgeniyShilov on 23.11.2016 at 11:34
 */
public class Application {

    public static void main(String[] args) throws UnknownHostException {
        (new Sender()).setRemote(InetAddress.getByName("127.0.0.1"), 1337);
    }
}
