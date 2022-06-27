import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends AbstractHelloServer {

    private Selector selector;
    private DatagramChannel channel;
    private int bufferSize;
    private Queue<MyData> responses;

    public static void main(String[] args) {
        new HelloUDPNonblockingServer().checkAndStart(args);
    }

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            bufferSize = channel.socket().getReceiveBufferSize();
            channel.register(selector, SelectionKey.OP_READ);
            channel.bind(new InetSocketAddress(port));
            executorService = Executors.newFixedThreadPool(threads);
            responses = new ConcurrentLinkedDeque<>();
            Executors.newSingleThreadExecutor().submit(this::startServer);
        } catch (IOException e) {
            System.err.printf("Server setup error: %s%n On port: %d%n", e.getLocalizedMessage(), port);
        }
    }

    private void startServer() {
        while (!Thread.interrupted() && !channel.socket().isClosed()) {
            try {
                selector.select(key -> {
                    if (key.isWritable()) {
                        if (!responses.isEmpty()) {
                            MyData data = responses.poll();
                            ByteBuffer buffer = ByteBuffer.wrap(data.getResponse().getBytes(StandardCharsets.UTF_8));
                            try {
                                channel.send(buffer, data.getSocket());
                            } catch (final IOException e) {
                                System.err.println("exp");
                            }
                            key.interestOpsOr(SelectionKey.OP_READ);
                        } else {
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } else if (key.isReadable()) {
                        try {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
                            SocketAddress socketAddress = channel.receive(byteBuffer);
                            executorService.submit(() -> {
                                byteBuffer.flip();
                                String receive = StandardCharsets.UTF_8.decode(byteBuffer).toString();
                                String response = String.format("Hello, %s", receive);
                                responses.add(new MyData(response, socketAddress));
                                key.interestOps(SelectionKey.OP_WRITE);
                                selector.wakeup();
                            });
                        } catch (final IOException e) {
                            System.err.printf("Read I/O exception: %s", e.getLocalizedMessage());
                        }
                    }
                });
            } catch (IOException e) {
                System.err.println("exp");
            }
        }
    }

    @Override
    public void close() {
        try {
            if (!Objects.isNull(selector)) selector.close();
            if (!Objects.isNull(channel)) channel.close();
            terminationWait(executorService, 1);
        } catch (final IOException e) {
            System.err.printf("Resources close exception: %s%n", e.getLocalizedMessage());
        }
    }

    private static class MyData {
        private final String response;
        private final SocketAddress socket;

        public MyData(String response, SocketAddress socket) {
            this.response = response;
            this.socket = socket;
        }

        public SocketAddress getSocket() {
            return socket;
        }

        public String getResponse() {
            return response;
        }
    }
}
