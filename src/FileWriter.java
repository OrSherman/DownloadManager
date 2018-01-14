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
            while (!isRangeFinished(chunk = chunkQueue.take())) {
                // if one of the current range chunk's failed, stop writing and don't mark the range as written.
                if (isChunkFailed(chunk)) {
                    file.close();
                    return;
                }

                file.seek(chunk.getOffset());
                file.write(chunk.getData(), 0, chunk.getSize_in_bytes());
            }

            downloadableMetadata.SaveMetadataToDisc(); //TODO: decide if here or in idcdm
            downloadableMetadata.addRange(downloadableMetadata.getMissingRange());
        } catch(IOException e){
            System.err.println("file write failed..."+ e);
        }catch (InterruptedException e){
            System.err.println("take from chunk..."+ e);
        }


    }

    private boolean isRangeFinished(Chunk i_Chunk){
        return i_Chunk.getSize_in_bytes() == -1;
    }

    private boolean isChunkFailed(Chunk i_Chunk){
        return i_Chunk.getOffset() == (long)-1;
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            System.err.println("write chunks failed");
            e.printStackTrace();
            //TODO
        }
    }
}
