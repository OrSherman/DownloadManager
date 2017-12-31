import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 * NOTE: make sure that the file interface you choose writes every update to the file's content or metadata
 *       synchronously to the underlying storage device.
 */
public class FileWriter implements Runnable {

    private final BlockingQueue<Chunk> chunkQueue;
    private DownloadableMetadata downloadableMetadata;

    public FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.downloadableMetadata = downloadableMetadata;
    }

    private void writeChunks() throws IOException {
        //TODO
        RandomAccessFile file = new RandomAccessFile(downloadableMetadata.getFilename(), "rw");
        Chunk chunk;
        while(!chunkQueue.isEmpty()){ //TODO: what if the chunk queue is temporarily empty
            try {
                chunk = chunkQueue.take();
                file.write(chunk.getData(), (int) chunk.getOffset(),chunk.getSize_in_bytes());
               // downloadableMetadata.addRange(); TODO: how to get the right range??
            }catch (InterruptedException e){
                System.err.println(e);
            }


        }
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO
        }
    }
}
