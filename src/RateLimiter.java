/**
 * A token bucket based rate-limiter.
 * <p>
 * This class should implement a "soft" rate limiter by adding maxBytesPerSecond tokens to the bucket every second,
 * or a "hard" rate limiter by resetting the bucket to maxBytesPerSecond tokens every second.
 */
import java.util.concurrent.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RateLimiter implements Runnable {

    private final TokenBucket tokenBucket;
    // the number bytes per seconds allowed to use.
    private final Long maxBytesPerSecond;

    RateLimiter(TokenBucket tokenBucket, Long maxBytesPerSecond) {
        this.tokenBucket = tokenBucket;
        // init the max byte per second according to the input or unlimited in case of no input.
        this.maxBytesPerSecond = maxBytesPerSecond == null ? Long.MAX_VALUE : maxBytesPerSecond;
    }

    @Override
    /**
     * "Hard" rate limiter implementation
     */
    public void run() {
        while(!this.tokenBucket.terminated()) {
            try {
                this.tokenBucket.set(maxBytesPerSecond);
                Thread.sleep(1000); // Adding BPS to the token bucket every second
            } catch (InterruptedException e) {
                System.err.println("rate limiter failed due to thread sleep error. Download failed");
                System.exit(-1);
            }
        }
    }
}
