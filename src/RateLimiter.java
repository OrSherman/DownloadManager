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
    private final Long maxBytesPerSecond;

    RateLimiter(TokenBucket tokenBucket, Long maxBytesPerSecond) {
        this.tokenBucket = tokenBucket;
        this.maxBytesPerSecond = maxBytesPerSecond;
    }

    @Override
    /**
     * "Hard" rate limiter implementation
     */
    public void run() {
        while(true) {
            try {
                this.tokenBucket.set(maxBytesPerSecond);
                Thread.sleep(1000); // Adding BPS to the token bucket every second
            } catch (InterruptedException e) {
                System.err.println("Download failed:" + e);
            }
        }
    }

    }
