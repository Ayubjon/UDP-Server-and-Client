import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractHelloServer implements HelloServer {
    protected ExecutorService executorService;

    protected void checkAndStart(String[] args) {
        if (!Objects.nonNull(args) || args.length != 2) {
            System.err.println("Expected: (int)port, (int)threads");
            return;
        }
        try {
            start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.printf("Expected an integer: %s\n", e.getMessage());
        }
    }

    void terminationWait(ExecutorService executorService, long waitTermination) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(waitTermination, TimeUnit.SECONDS)) {
                System.err.println("Can not close ExecutorService");
            }
        } catch (InterruptedException e) {
            System.err.printf("Interrupted when closing executors: %s\n", e.getMessage());
        }
    }
}
