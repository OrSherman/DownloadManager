import java.util.*;
/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 *
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 *
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
public class DownloadableMetadata {
    private final String metadataFilename;
    private String filename;
    private String url;
    private TreeSet<Range> m_WritenRanges;
    private static final int k_RangeSize = 1024;
    private static final long k_FileSize = 10000000; //TODO: get real file size
   public DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
         m_WritenRanges = new TreeSet<>();
        //TODO
    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    public void addRange(Range i_Range) {
        Range currRange = i_Range;
        Range prevRange = getPrevRange(i_Range);
        Range nextRange = getNextRange(i_Range);

        if(m_WritenRanges.contains(prevRange)){
            m_WritenRanges.remove(prevRange);
            currRange = new Range(prevRange.getStart(), currRange.getEnd());
        }

        if(m_WritenRanges.contains(nextRange)){
            m_WritenRanges.remove(nextRange);
            currRange = new Range(currRange.getStart(), nextRange.getEnd());
        }

        m_WritenRanges.add(currRange);
    }

    private Range getPrevRange(Range i_Range){
       return new Range(i_Range.getStart()- k_RangeSize, i_Range.getStart());
    }

    private Range getNextRange(Range i_Range){
        return new Range(i_Range.getEnd(), i_Range.getEnd() + k_RangeSize);
    }

    public String getFilename() {
        return filename;
    }

    public boolean isCompleted() {
        return (m_WritenRanges.size() == 1) && m_WritenRanges.contains(new Range((long)0, k_FileSize));
    }

    public void delete() {
        //TODO
    }

    public Range getMissingRange() {
        //TODO
       // Range firstRange = m_WritenRanges.;
    }

    public String getUrl() {
        return url;
    }
}
