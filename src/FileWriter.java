import java.io.*;
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
        try {
        while((chunk = chunkQueue.take()).getSize_in_bytes() != -1){
            file.seek(chunk.getOffset());
            file.write(chunk.getData(), 0, chunk.getSize_in_bytes());
        }
        }catch (InterruptedException e){
            System.err.println("file write failed..."+ e);
        }

       downloadableMetadata.SaveMetadataToDisc(); //TODO: decide if here or in idcdm
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
