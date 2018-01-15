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
        RandomAccessFile file = new RandomAccessFile(downloadableMetadata.getFilename(), "rw");
        Chunk chunk;

        try {
            while (!isRangeFinished(chunk = chunkQueue.take())) {
                // if one of the current range chunk's failed, stop writing and don't mark the range as written.
                if (isChunkFailed(chunk)) {
                    file.close();
                    return;
                }

                writeChunkToFile(file, chunk);
            }
            // notify the metadata that the current range was written.
            downloadableMetadata.addRange(downloadableMetadata.getMissingRange());
            // after writing all the range to the dick, save it to the metadata file.
            downloadableMetadata.SaveMetadataToDisc();

        } catch (InterruptedException e){
            System.err.println("take from chunk failed"+ e); //TODO: handle the exception.
        }
    }

    private void writeChunkToFile(RandomAccessFile i_File, Chunk i_Chunk) throws IOException {
        i_File.seek(i_Chunk.getOffset());
        i_File.write(i_Chunk.getData(), 0, i_Chunk.getSize_in_bytes());
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
            System.err.println("file write failed..."+ e); //TODO: handle the exception

        }
    }
}
