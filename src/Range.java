import java.io.Serializable;
import java.util.Comparator;

/**
 * Describes a simple range, with a start, an end, and a length
 */
public class Range implements Serializable {
    //the id of the Serializable in order to avoid warnings
    private static final long serialVersionUID = 1;
    private Long start;
    private Long end;

    Range(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Long getStart() {
        return start;
    }

    public Long getEnd() {
        return end;
    }

    public Long getLength() {
        return end - start + 1;
    }
}
