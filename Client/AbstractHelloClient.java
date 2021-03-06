import java.util.Objects;

public abstract class AbstractHelloClient implements HelloClient {

    public void checkAndRun(String[] args) throws IllegalArgumentException, NumberFormatException {
        if (!Objects.nonNull(args) || args.length != 5) {
            System.err.println("Expected: (String)host, (int)port, (String)prefix, (int)threads, (int)requests");
            throw new IllegalArgumentException("Expected: (String)host, (int)port, (String)prefix, (int)threads, (int)requests");
        }
        try {
            run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.printf("Expected an integer: %s\n", e.getLocalizedMessage());
            throw new NumberFormatException("Expected: (String)host, (int)port, (String)prefix, (int)threads, (int)requests. But: " + e.getLocalizedMessage());
        }
    }
}
