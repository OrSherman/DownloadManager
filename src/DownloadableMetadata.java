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
    private static final long k_RangeSize = 1000000;
    private long k_FileSize;
    private Range missingRange;

    public DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        k_FileSize = calcFileSize(url);
        this.missingRange = new Range((long) 0, Math.min(k_RangeSize , k_FileSize) - 1); //TODO: change start to 0

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
        Long newMissingRangeStart  = i_Range.getEnd() + 1;
        Long missingRangeFullRangeEnd = i_Range.getEnd() + k_RangeSize;
        Long newMissingRangeEnd = k_FileSize < missingRangeFullRangeEnd ? k_FileSize - 1 : missingRangeFullRangeEnd;
        this.missingRange = new Range(newMissingRangeStart, newMissingRangeEnd);
    }

    public String getFilename() {
        return filename;
    }

    public boolean isCompleted() {
        return missingRange.getLength() < 1;
    }

    public void delete() {
        try{
            File metadataFile = new File(metadataFilename);
            metadataFile.delete();}
        catch (Exception e) {
            System.err.println("Deleting metadata file failed");
        }
    }

    public Range getMissingRange() {
        return this.missingRange;
    }

    public void SaveMetadataToDisc()  {
        FileOutputStream metaDataTempStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            File metadataTempFile = new File(metadataFilename + ".temp");
            File metadataFile = new File(metadataFilename);
            metaDataTempStream = new FileOutputStream(metadataTempFile);
            objectOutputStream = new ObjectOutputStream(metaDataTempStream);
            objectOutputStream.writeObject(this);
//TODO: do it right
//            if (metadataFile.exists()) {
//               // metadataFile.delete();
//                Files.move(metadataTempFile.toPath(), metadataFile.toPath(), REPLACE_EXISTING);
//            }else{
//                metadataTempFile.renameTo(metadataFile);
//                metadataTempFile.delete();
//            }
           Files.move(metadataTempFile.toPath(), metadataFile.toPath(), REPLACE_EXISTING);
        }  catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            System.err.println("SaveMetadataToDick catch err"); // TODO: change the name
            e.printStackTrace();//TODO: handle errors
        }
        finally { //TODO: use finally on all function

            try {
                metaDataTempStream.close();
                objectOutputStream.close();
            } catch (IOException e) {
                System.err.println("unable to close metaDataTempStream"); //TODO: give real error mag
                e.printStackTrace();
            }

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

    public Long getFileSize() {
        return k_FileSize;
    }
    public String getUrl() {
        return url;
    }
}
