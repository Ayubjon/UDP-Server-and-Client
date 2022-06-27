import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient extends AbstractHelloClient {

    public static void main(String[] args) {
        new HelloUDPClient().checkAndRun(args);
    }

    public void checkAndRun(String[] args) {
        if (!Objects.nonNull(args) || args.length != 3) {
            System.err.println("Expected: (String) client's name, (String) host, (int) port");
            return;
        }
        try {
            run(args[0], args[1], Integer.parseInt(args[2]));
        } catch (NumberFormatException e) {
            System.err.printf("Expected an integer: %s\n", e.getMessage());
        }
    }

    @Override
    public void run(String name, String host, int port, String prefix, int threads, int requests) {
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                Sender sender = new Sender(address, prefix, i, requests);
                executorService.submit(sender::send);
            }
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10L * threads * requests, TimeUnit.SECONDS))
                    System.err.println("Can not close ExecutorService");
            } catch (InterruptedException e) {
                System.err.printf("Got interrupted exception: %s\n", e.getMessage());
            }
        } catch (UnknownHostException e) {
            System.err.printf("There is no such host: %s\n", e.getMessage());
        }
    }


    private static class Sender {
        private final String prefix;
        private final int requests;
        private final InetSocketAddress address;
        private final int myNumber;

        public Sender(InetSocketAddress address, String prefix, int myNumber, int requests) {
            this.prefix = prefix;
            this.requests = requests;
            this.myNumber = myNumber;
            this.address = address;
        }

        public void send() {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(10);
                for (int i = 0; i < requests; ++i) {
                    DatagramPacket receivedPacket = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                    String textToSend;
                    String response = "";
                    do {
                        textToSend = String.format("%s%s_%s", prefix, myNumber, i);
                        byte[] text = textToSend.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(text, text.length, address.getAddress(), address.getPort());
                        try {
                            socket.send(packet);
                            socket.receive(receivedPacket);
                            response = new String(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength(), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            System.err.printf("Problem with sending or receiving: %s\n", e.getMessage());
                        }
                    } while (!response.contains(textToSend));
                    System.out.printf("Sent: %s, Received: %s\n", textToSend, response);
                }
            } catch (SocketException e) {
                System.err.printf("Problem with opening socket: %s\n", e.getMessage());
            }
        }
    }
}
