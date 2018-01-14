import java.util.concurrent.*;

public class IdcDm {
    private static int percentageDownloaded = -1;
    /**
     * Receive arguments from the command-line, provide some feedback and start the download.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int numberOfWorkers = 1;
        Long maxBytesPerSecond = null;

        if (args.length < 1 || args.length > 3) {
            System.err.printf("usage:\n\tjava IdcDm URL [MAX-CONCURRENT-CONNECTIONS] [MAX-DOWNLOAD-LIMIT]\n");
            System.exit(1);
        } else if (args.length >= 2) {
            numberOfWorkers = Integer.parseInt(args[1]);
            if (args.length == 3)
                maxBytesPerSecond = Long.parseLong(args[2]);
        }

        String url = args[0];

        System.err.printf("Downloading");
        if (numberOfWorkers > 1)
            System.err.printf(" using %d connections", numberOfWorkers);
        if (maxBytesPerSecond != null)
            System.err.printf(" limited to %d Bps", maxBytesPerSecond);
        System.err.printf("...\n");

        DownloadURL(url, numberOfWorkers, maxBytesPerSecond);
    }

    /**
     * Initiate the file's metadata, and iterate over missing ranges. For each:
     * 1. Setup the Queue, TokenBucket, DownloadableMetadata, FileWriter, RateLimiter, and a pool of HTTPRangeGetters
     * 2. Join the HTTPRangeGetters, send finish marker to the Queue and terminate the TokenBucket
     * 3. Join the FileWriter and RateLimiter
     *
     * Finally, print "Download succeeded/failed" and delete the metadata as needed.
     *
     * @param url URL to download
     * @param numberOfWorkers number of concurrent connections
     * @param maxBytesPerSecond limit on download bytes-per-second
     */
    private static void DownloadURL(String url, int numberOfWorkers, Long maxBytesPerSecond) {
        DownloadableMetadata metaData = DownloadableMetadata.InitMetadata(url);
        LinkedBlockingQueue<Chunk> chunkQueue = new LinkedBlockingQueue<Chunk>();
        FileWriter fileWriter = new FileWriter(metaData, chunkQueue);
        Range currRange;
        Thread[] httpRangeGettersThreads = new Thread[numberOfWorkers];

        while(!metaData.isCompleted()){
            currRange = metaData.getMissingRange();
            printPercentage(currRange.getEnd(), metaData.getFileSize());
            Thread fileWriterThread = new Thread(fileWriter);
            fileWriterThread.start();
            TokenBucket tokenBucket = new TokenBucket();
            RateLimiter rateLimiter = new RateLimiter(tokenBucket,maxBytesPerSecond);
            Thread rateLimiterThread = new Thread(rateLimiter);
            rateLimiterThread.start();
            long length = currRange.getLength() / numberOfWorkers;
            long reminder = currRange.getLength() % numberOfWorkers;
            Long start = currRange.getStart();
            Long end = currRange.getStart() + length;

            for(int i = 0; i < httpRangeGettersThreads.length; i++){
                Range subRange = new Range(start,end);
                httpRangeGettersThreads[i] = new Thread(new HTTPRangeGetter(metaData.getUrl(),subRange,chunkQueue,tokenBucket));
                httpRangeGettersThreads[i].start();
                if((i != httpRangeGettersThreads.length -1) || reminder == 0){
                    start = end + 1;
                    end = start + length;
                }else{
                    start = end + 1;
                    end = start + reminder;
                }
            }

            for(Thread httpRangeGettersThread : httpRangeGettersThreads){
                try {
                    httpRangeGettersThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace(); //TODO: print err msg
                }
            }

            chunkQueue.add(new Chunk(null,0,-1));
            tokenBucket.terminate();
            try {
                fileWriterThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace(); //TODO: print err msg
            }

            try {
                rateLimiterThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace(); //TODO: print err msg
            }

            chunkQueue.clear();
        }

        System.out.println("download succeeded!! :)");
        metaData.delete();

    }

    private static void printPercentage(long i_sizeDownloaded, long i_FileSize) {
        double partialDownloaded =  ((double)i_sizeDownloaded / (double) i_FileSize);
        double currentPercentageDownloaded =  partialDownloaded * 100;
        if ((int)currentPercentageDownloaded != percentageDownloaded) {
            percentageDownloaded = (int)currentPercentageDownloaded;
            System.out.println(percentageDownloaded + "%");
        }

    }
}
