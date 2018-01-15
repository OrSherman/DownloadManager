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
    private static final int CONNECT_TIMEOUT = 50000;
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

    private  void  downloadRange() throws IOException {
        HttpURLConnection httpURLConnection = setUpHttpConnection(new URL(url));
        BufferedInputStream  dataInputStream = new  BufferedInputStream(httpURLConnection.getInputStream());
        streamToChunkQueue(dataInputStream);
        httpURLConnection.disconnect(); //TODO: add finally
        dataInputStream.close();
    }

    private HttpURLConnection setUpHttpConnection(URL i_Url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) i_Url.openConnection();
        checkResponseCode(200); //TODO: fix Already connected bug
        httpURLConnection.setRequestProperty("Range", "bytes=" + range.getStart() + " - " + range.getEnd());
        httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
        httpURLConnection.setReadTimeout(READ_TIMEOUT);
        connectAndExistIfFailed(httpURLConnection);

        return httpURLConnection;
    }

    private void connectAndExistIfFailed(HttpURLConnection httpURLConnection)  {
        try {
            httpURLConnection.connect();
            // if the connection fails due to timeout or any other IO exceptions exit the program.
        } catch (SocketTimeoutException e) {
            System.err.println("connection timeout. Download Failed.");
            System.exit(-1);
        } catch (IOException e){
            System.err.println("no rout to host. Download Failed.");
            System.exit(-1);
        }
    }

    private void takeChunkFromTokenBucket() throws IOException {
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

        takeChunkFromTokenBucket();
        while ((numOfBytesRead = i_DataInputStream.read(data, 0, CHUNK_SIZE)) != -1)
        {
            outQueue.add(new Chunk(data, offset, numOfBytesRead));
            offset += numOfBytesRead;
            takeChunkFromTokenBucket();
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
            } catch (IOException e) {
                System.err.println("Download range "+ this.range.getStart()+"-"+range.getEnd() +" failed. trying again.");
            }
        }
    }
}
