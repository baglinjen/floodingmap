package dk.itu.data.datastructure.phtree;

import java.util.ArrayList;
import java.util.List;

public class PhNode<T> {
    // Bits 0-5 => Length of the prefix
    // Bit  6   => Negative/Positive
    // Bit  7   => IsEmpty
    private byte info = 0;
    private int prefixLength = 0;
    // Prefixes double as long min/maxLon AND min/maxLat
    private final long[] prefixes = new long[2];
    /*
     + - - - - + - - - - +
     |         |         |
     | 0,1,?,? | 1,1,?,? |
     |         |         |
     1 - - - - 1 - - - - +
     |         |         |
     | 0,0,?,? | 1,0,?,? |
     |         |         |
     0 - - - - 1 - - - - +
     */
    private final PhNode<T>[] children = new PhNode[4];
    private List<T> elements;

    public void addElement(T element) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        elements.add(element);
    }

    public void clearElements() {
        elements = null;
    }

    public void setChild(PhNode<T> child, int index) {
        children[index] = child;
    }

    public void transferElements(PhNode<T> node) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        elements.addAll(node.elements);
    }

    public PhNode<T> getChild(int index) {
        return children[index];
    }

    public int getPrefixLength() {
        return (info & 0b11111100) >>> 2;
    }
    public void setPrefixLength(int n) {
        info = (byte) ((n << 2) | (info & 0b00000011));
        prefixLength = n;
    }
    public boolean getIsPrefixPositive() {
        return ((info & 0b0000010) >>> 1) == 0;
    }
    public boolean getIsPrefixEmpty() {
        return (info & 0b0000001) == 0;
    }
    public void setIsPrefixEmpty(boolean empty) {
        info = (byte) ((empty ? 0 : 1) | (info & 0b11111110));
    }

    public long getPrefixLon() {
        return prefixes[0];
    }
    public long getPrefixLat() {
        return prefixes[1];
    }

    public void setPrefixRaw(long lon, long lat, int length) {
        prefixes[0] = lon;
        prefixes[1] = lat;
        setPrefixLength(length);
        setIsPrefixEmpty(false);
    }

    public List<T> getElements() {
        return elements;
    }

    public void setPrefix(long lon, long lat, int nBits, int offset) {
        prefixes[0] = getFirstNBitsFromLong(lon, nBits, offset);
        prefixes[1] = getFirstNBitsFromLong(lat, nBits, offset);
        setPrefixLength(nBits);
        setIsPrefixEmpty(false);
    }

    // Returns long with leftmost bits match original
    // 10101111 and 4 and 2 => 10110000
    public static long getFirstNBitsFromLong(long l, int n, int skip) {
        return getFirstNBitsFromLong(l, n+skip) << skip;
    }
    // Returns long with leftmost bits match original
    // 10101111 and 4 => 10100000
    public static long getFirstNBitsFromLong(long l, int n) {
        return l & (0xFFFFFFFFFFFFFFFFL << (64-n));
    }
    public static int getNumberAtIndex(long lon, long lat, int index) {
        long maskAtIndex = 0x8000000000000000L >>> index;
        return (int) (((lon & maskAtIndex) >>> (64-(index+2))) + ((lat & maskAtIndex) >>> (64-(index+1))));
    }

    public static void printLong(long l) {
        System.out.println(String.format("%64s", Long.toBinaryString(l)).replace(' ', '0'));
    }
}
