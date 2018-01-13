import java.io.Serializable;
import java.util.Comparator;

/**
 * Describes a simple range, with a start, an end, and a length
 */
public class Range implements Serializable, Comparable<Range> {
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

    @Override
    public boolean 	equals(Object obj){
        boolean isEqual = false;
        Range inputRange = (Range) obj;

        if(inputRange != null && inputRange.getStart() == this.getStart() && inputRange.getEnd() == this.getEnd()){
            isEqual = true;
        }

        return isEqual;
    }

    @Override
    public int compareTo(Range i_Range) {
        return Long.signum(this.getStart() - i_Range.getStart());
    }
}
