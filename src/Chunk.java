/**
 * A chunk of data file
 *
 * Contains an offset, bytes of data, and size
 */
class Chunk {
    private byte[] data;
    private long offset;
    private int size_in_bytes;

    Chunk(byte[] data, long offset, int size_in_bytes) {
        this.data = data != null ? data.clone() : null;
        this.offset = offset;
        this.size_in_bytes = size_in_bytes;
    }

    public byte[] getData() {
        return data;
    }

    public long getOffset() {
        return offset;
    }

    public int getSize_in_bytes() {
        return size_in_bytes;
    }
}
