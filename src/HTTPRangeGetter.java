import java.io.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;

/**
 * A runnable class which downloads a given url.
 * It reads CHUNK_SIZE at a time and writs it into a BlockingQueue.
 * It supports downloading a range of data, and limiting the download rate using a token bucket.
 */
public class HTTPRangeGetter implements Runnable {
    static final int CHUNK_SIZE = 4096;
    private static final int CONNECT_TIMEOUT = 500;
    private static final int READ_TIMEOUT = 2000;
    // signal to keep try to download a cunk when failed.
    private static final boolean KEEP_TRYING = true;
    private final String url;
    private final Range range;
    private final BlockingQueue<Chunk> outQueue;
    private TokenBucket tokenBucket;

    public HTTPRangeGetter(
            String url,
            Range range,
            BlockingQueue<Chunk> outQueue,
            TokenBucket tokenBucket) {
        this.url = url;
        this.range = range;
        this.outQueue = outQueue;
        this.tokenBucket = tokenBucket;
    }

    private  synchronized void  downloadRange() throws IOException, InterruptedException {
        URL fileUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) fileUrl.openConnection();
        checkResponseCode(200); //TODO: fix Already connected bug
        httpURLConnection.setRequestProperty("Range", "bytes=" + range.getStart() + " - " + range.getEnd());
        httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
        httpURLConnection.setReadTimeout(READ_TIMEOUT);




        httpURLConnection.connect();

        BufferedInputStream  dataInputStream = new  BufferedInputStream(httpURLConnection.getInputStream());
        streamToChunkQueue(dataInputStream);
        httpURLConnection.disconnect(); //TODO: add finally
        dataInputStream.close();


    }

    private void takeChunk() throws IOException {
        if(tokenBucket != null){
            tokenBucket.take(CHUNK_SIZE);
        }else{
            throw  new IOException("Token bucket is null, Download failed");
        }
    }

    private void streamToChunkQueue(BufferedInputStream i_DataInputStream) throws IOException {
        byte data[] = new byte[CHUNK_SIZE];
        int numOfBytesRead = 0;
        long offset = range.getStart();

        takeChunk();
        while ((numOfBytesRead = i_DataInputStream.read(data, 0, CHUNK_SIZE)) != -1)
        {
            outQueue.add(new Chunk(data, offset, numOfBytesRead));
            offset += numOfBytesRead;
            takeChunk();

        }
    }

    private void checkResponseCode(int i_ResponseCode) throws IOException {
        if (i_ResponseCode / 100 != 2) {
            throw new IOException("bad response code");
        }
    }

    @Override
    public void run() {
        while(KEEP_TRYING){
            try {
                this.downloadRange();
                break;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                System.err.println("Download range "+ this.range +" failed");
            }
        }
    }
}
