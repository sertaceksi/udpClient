import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        Client client = new Client();
        client.start();
    }
}
