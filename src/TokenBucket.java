import java.util.concurrent.atomic.*;
import java.util.concurrent.Semaphore;

/**
 * A Token Bucket (https://en.wikipedia.org/wiki/Token_bucket)
 *
 * This thread-safe bucket should support the following methods:
 *
 * - take(n): remove n tokens from the bucket (blocks until n tokens are available and taken)
 * - set(n): set the bucket to contain n tokens (to allow "hard" rate limiting)
 * - add(n): add n tokens to the bucket (to allow "soft" rate limiting)
 * - terminate(): mark the bucket as terminated (used to communicate between threads)
 * - terminated(): return true if the bucket is terminated, false otherwise
 *
 */

public class TokenBucket {
    // the number of tokens in the bucket
    private AtomicLong m_NumOfTokens;
    // if true the bucket is terminated.
    private AtomicBoolean m_terminated;
    private Semaphore m_Semaphore;
    private long m_NumOfRequestedTokens;

    public TokenBucket() {
        m_NumOfTokens = new AtomicLong(0);
        m_terminated= new AtomicBoolean(false);
        m_Semaphore = new Semaphore(1);
    }

    public synchronized void take(long tokens) {
        m_NumOfRequestedTokens = tokens;
        if(m_NumOfTokens.get() - tokens < 0) {
                synchronized (m_Semaphore){
                    try {
                        m_Semaphore.wait();
                    } catch (InterruptedException e) {
                        System.err.println("waiting in token bucket semaphore failed");
                    }
                }


        }

        m_NumOfTokens.addAndGet(-tokens);
    }

    public void terminate() {
        m_terminated.getAndSet(true);
    }

    public boolean terminated() {
        return m_terminated.get();
    }

    public void set(long tokens) {
        m_NumOfTokens.getAndSet(tokens);
        notifyAvailableTokens();

    }

    public void add(long tokens){
        m_NumOfTokens.getAndAdd(tokens);
        notifyAvailableTokens();
    }

    private synchronized void notifyAvailableTokens() {
        if (m_NumOfTokens.get() - m_NumOfRequestedTokens >= 0){
            synchronized (m_Semaphore) {
                m_Semaphore.notify();
            }
        }
    }
}
