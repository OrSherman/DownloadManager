/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range {
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
