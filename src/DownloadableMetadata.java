import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 *
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 *
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
public class DownloadableMetadata implements Serializable {
    private final String metadataFilename;
    private String filename;
    private String url;
    private TreeSet<Range> m_WritenRanges;
    private static final long k_RangeSize = 1000000;
    private long k_FileSize;

    public DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        m_WritenRanges = new TreeSet<>();
        k_FileSize = calcFileSize(url);
        //TODO
    }

    private static String getMetadataName(String filename) { return filename + ".metadata";}

    public String getMetadataName(){
        return metadataFilename;
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    public void addRange(Range i_Range) {
        Range currRange = i_Range;
        Range prevRange = getPrevRange(i_Range);
        Range nextRange = getNextRange(i_Range);

        if(prevRange != null && prevRange.getEnd() + 1 == currRange.getStart()){
            m_WritenRanges.remove(prevRange);
            currRange = new Range(prevRange.getStart(), currRange.getEnd());
        }

        if(nextRange != null && nextRange.getStart() - 1 == currRange.getEnd()){
            m_WritenRanges.remove(nextRange);
            currRange = new Range(currRange.getStart(), nextRange.getEnd());
        }

        m_WritenRanges.add(currRange);
    }



    private Range getPrevRange(Range i_Range){
        return m_WritenRanges.lower(i_Range);
    }

    private Range getNextRange(Range i_Range){
        return m_WritenRanges.higher(i_Range);
    }

    public String getFilename() {
        return filename;
    }

    public boolean isCompleted() {
        boolean completed = (m_WritenRanges.size() == 1) && m_WritenRanges.first().getLength() == k_FileSize;
        return completed;
    }

    public void delete() {
        //TODO
    }

    public Range getMissingRange() {
        Range missingRange = null;

        if(!isCompleted()) {
            missingRange = new Range((long) 0, k_RangeSize);
            Range firstRange = m_WritenRanges.size() == 0 ? null : m_WritenRanges.first();
            if (firstRange != null && firstRange.getStart() == 0) {
                long rangeSize = Math.min(k_RangeSize, k_FileSize - (firstRange.getEnd() + 1)); //TODO: bug??
                missingRange = new Range(firstRange.getEnd() + 1, firstRange.getEnd() + 1 + rangeSize);
            }

            this.addRange(missingRange);
        }

        return missingRange;
    }

    public void SaveMetadataToDisc() throws IOException {
        File metadataTempFile = new File(metadataFilename + ".temp");
        File metadataFile = new File(metadataFilename);
        FileOutputStream metaDataTempStream = new FileOutputStream(metadataTempFile);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(metaDataTempStream);

        try {
            objectOutputStream.writeObject(this);
//TODO: do it right
            if (metadataFile.exists()) {
                metadataFile.delete();
            }
            metadataTempFile.renameTo(metadataFile);
            metadataTempFile.delete();
          //  Files.move(metadataTempFile.toPath(), metadataFile.toPath(), REPLACE_EXISTING);

        } catch (IOException e) {
            System.err.println("SaveMetadataToDick catch err"); // TODO: change the name
            e.printStackTrace();//TODO: handle errors
        }
        finally { //TODO: use finally on all function
            metaDataTempStream.close();
            objectOutputStream.close();
        }
    }

    public static DownloadableMetadata InitMetadata(String url){
        DownloadableMetadata metadata = new DownloadableMetadata(url);
        String metadataFilename = metadata.getMetadataName();
        File metadataFile = new File(metadataFilename);

        if(metadataFile.exists()){
            try {
                FileInputStream metadataInputStream = new FileInputStream(metadataFilename);
                ObjectInputStream objectInputStream = new ObjectInputStream(metadataInputStream);
                metadata = (DownloadableMetadata) objectInputStream.readObject();
                objectInputStream.close();
                metadataInputStream.close();

            } catch (IOException i) {
                System.err.println("Download failed: load metadta failed");
            } catch (ClassNotFoundException c) {
                System.out.println("Download failed: class not found..... :(");
                c.printStackTrace();
            }

        }

        return metadata;
    }

    private long calcFileSize(String i_Url)  {
        URL url = null;
        try {
            url = new URL(i_Url);
        } catch (MalformedURLException e) {
            System.err.println("calcFileSize failed...."); //TODO: write real err mag
            e.printStackTrace();
        }
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }
    public String getUrl() {
        return url;
    }
}
