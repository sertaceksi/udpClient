import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        Client client = new Client();
        System.out.println("Client is up and running.");
        client.start();
    }
}
