import java.io.*;
import java.nio.file.*;
import java.net.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 * <p>
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 * <p>
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
public class DownloadableMetadata implements Serializable {
    //the id of the Serializable in order to avoid warnings
    private static final long serialVersionUID = 0;
    private final String metadataFilename;
    private String filename;
    private String url;
    private static final long RANGE_SIZE = 1000000; // the range size
    private long FILE_SIZE; // stores the file's size
    private Range missingRange;

    public DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        FILE_SIZE = calcFileSize(url);
        this.missingRange = new Range((long) 0, Math.min(RANGE_SIZE, FILE_SIZE) - 1); //TODO: change start to 0

    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    public String getMetadataName() {
        return metadataFilename;
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    /**
     * adds a range by removing it from the missing rage
     *
     * @param i_Range the range to be added
     */
    public void addRange(Range i_Range) {
        Long newMissingRangeStart = i_Range.getEnd() + 1;
        Long missingRangeFullRangeEnd = i_Range.getEnd() + RANGE_SIZE;
        Long newMissingRangeEnd = FILE_SIZE < missingRangeFullRangeEnd ? FILE_SIZE - 1 : missingRangeFullRangeEnd;
        this.missingRange = new Range(newMissingRangeStart, newMissingRangeEnd);
    }

    public String getFilename() {
        return filename;
    }

    public boolean isCompleted() {
        return missingRange.getLength() < 1;
    }

    /**
     * deletes the metadata file
     */
    public void delete() {
        try {
            File metadataFile = new File(metadataFilename);
            metadataFile.delete();
        } catch (Exception e) {
            System.err.println("Deleting metadata file failed.");
        }
    }

    public Range getMissingRange() {
        return this.missingRange;
    }

    /**
     * saves the metadata object to disc, when done writing to disc
     * renames the file to the properly metadata ile name
     */
    public void SaveMetadataToDisc() {

        File metadataTempFile = new File(metadataFilename + ".tmp");
        File metadataFile = new File(metadataFilename);
        FileOutputStream metaDataTempStream = null;
        ObjectOutputStream objectOutputStream =null;

        try{
            metaDataTempStream = new FileOutputStream(metadataTempFile);
            objectOutputStream = new ObjectOutputStream(metaDataTempStream);
            objectOutputStream.writeObject(this);
            metaDataTempStream.close();
            objectOutputStream.close();
            Files.move(metadataTempFile.toPath(), metadataFile.toPath(), REPLACE_EXISTING);
        } catch (FileNotFoundException e) {
            System.err.println("metadata file not found, continue downloading.");
        } catch (IOException e) {
            System.err.println("Renaming to the metadata file from the .tmp failed");
        }finally {
            try {
                metaDataTempStream.close();
                objectOutputStream.close();
            } catch (IOException e) {
                System.err.println("close metadata files failed. continue downloading.");
            }
        }
    }

    /**
     * returns the metadata instance for a given url
     * It might get it from an existing metadata file
     * or to initialize it in the case it is a new download
     *
     * @param url the file's url
     * @return the metadata of the file
     */
    public static DownloadableMetadata InitMetadata(String url) {
        DownloadableMetadata metadata = new DownloadableMetadata(url);
        String metadataFilename = metadata.getMetadataName();
        File metadataFile = new File(metadataFilename);

        if (metadataFile.exists()) {
            try {
                FileInputStream metadataInputStream = new FileInputStream(metadataFilename);
                ObjectInputStream objectInputStream = new ObjectInputStream(metadataInputStream);
                metadata = (DownloadableMetadata) objectInputStream.readObject();
                objectInputStream.close();
                metadataInputStream.close();

            } catch (IOException i) {
                System.err.println("IO Exception while trying to load the metadata from file. creating a new one.");
                metadata = new DownloadableMetadata(url);
            } catch (ClassNotFoundException c) {
                System.out.println("class not found while trying to deserialize. creating a new one.");
                metadata = new DownloadableMetadata(url);
            }

        }

        return metadata;
    }

    /**
     * Calculates the size of the file in a given url
     *
     * @param i_Url the file's url
     * @return the size of the file in bytes
     */
    private long calcFileSize(String i_Url) {
        URL url = null;
        try {
            url = new URL(i_Url);
        } catch (MalformedURLException e) {
            System.err.println("Calculating file's size failed. Download failed");
            System.exit(-1);
        }

        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            System.err.println("Calculating file's size failed. Download failed");
            System.exit(-1);
            return -1;
        } finally {
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }
    }

    public Long getFileSize() {
        return FILE_SIZE;
    }

    public String getUrl() {
        return url;
    }
}
