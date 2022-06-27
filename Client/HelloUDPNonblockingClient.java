import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HelloUDPNonblockingClient extends AbstractHelloClient {
    private String prefix;
    private int requests;

    public static void main(String[] args) {
        new HelloUDPNonblockingClient().checkAndRun(args);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        this.prefix = prefix;
        this.requests = requests;
        List<DatagramChannel> channels = new ArrayList<>();
        try (Selector selector = Selector.open()) {
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            for (int i = 0; i < threads; ++i) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(socketAddress);
                channel.register(selector, SelectionKey.OP_WRITE, new MyData(i, channel.socket().getReceiveBufferSize()));
                channels.add(channel);
            }

            while (!selector.keys().isEmpty() && !Thread.interrupted()) {
                int s = selector.select(10);
                if (s == 0) {
                    for (SelectionKey key : selector.keys()) {
                        if (key.isWritable()) {
                            send(key, socketAddress);
                        }
                    }
                    continue;
                }

                selector.select(key -> {
                    if (key.isValid()) {
                        if (key.isReadable()) {
                            receive(key);
                        } else if (key.isWritable()) {
                            send(key, socketAddress);
                        }
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("exp");
        } finally {
            for (DatagramChannel channel : channels) {
                try {
                    channel.close();
                } catch (IOException e) {
                    System.err.println("exp");
                }
            }
        }
    }

    private void receive(SelectionKey key) {
        try {
            MyData data = (MyData) key.attachment();
            DatagramChannel channel = (DatagramChannel) key.channel();
            channel.receive(data.getByteBuffer().clear());
            String response = StandardCharsets.UTF_8.decode(data.getByteBuffer().flip()).toString();
            if (response.contains(String.format("%s%s_%s", prefix, data.getThreadNum(), data.getRequestNum()))) {
                System.out.printf("Received: %s\n", response);
                data.incrementRequest();
            }
            key.interestOps(SelectionKey.OP_WRITE);
            if (data.getRequestNum() >= requests) {
                channel.close();
            }
        } catch (IOException e) {
            System.err.println("exp");
        }
    }

    private void send(SelectionKey key, InetSocketAddress socketAddress) {
        try {
            MyData data = (MyData) key.attachment();
            DatagramChannel channel = (DatagramChannel) key.channel();
            String message = String.format("%s%s_%s", prefix, data.getThreadNum(), data.getRequestNum());
            channel.send(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)), socketAddress);
            System.out.printf("Sent: %s\n", message);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("exp");
        }
    }

    private static class MyData {
        private final int threadNum;
        private final ByteBuffer byteBuffer;
        private int requestNum;

        public MyData(int threadNum, int bufferSize) {
            this.threadNum = threadNum;
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
        }

        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        public int getRequestNum() {
            return requestNum;
        }

        public int getThreadNum() {
            return threadNum;
        }

        public void incrementRequest() {
            requestNum++;
        }
    }
}
