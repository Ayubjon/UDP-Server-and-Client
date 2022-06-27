import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer extends AbstractHelloServer {
    private ExecutorService executorService;
    private DatagramSocket socket;

    public static void main(String[] args) {
        new HelloUDPServer().checkAndStart(args);
    }

    @Override
    public void start(int port, int threads) {
        try {
            executorService = Executors.newFixedThreadPool(threads);
            socket = new DatagramSocket(port);
            for (int i = 0; i < threads; ++i) {
                executorService.submit(() -> {
                    try {
                        while (!socket.isClosed()) {
                            DatagramPacket packet = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                            socket.receive(packet);
                            String response = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                            packet.setData(("Hello, " + response).getBytes(StandardCharsets.UTF_8));
                            socket.send(packet);
                        }
                    } catch (IOException e) {
                        System.err.printf("Problem with sending or receiving message: %s\n", e.getMessage());
                    }
                });
            }
        } catch (SocketException e) {
            System.err.printf("Problems with opening socket: %s\n", e.getMessage());
        }
    }

    @Override
    public void close() {
        socket.close();
        terminationWait(executorService, 1);
    }
}
