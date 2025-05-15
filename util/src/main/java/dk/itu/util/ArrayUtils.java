package dk.itu.util;

import java.util.Arrays;

public class ArrayUtils {
    /**
     * Appends a2 to a1, excluding n indexes of a2.
     * For example a1 = [0, 1, 2, 3] and a2 = [4, 5, 6, 7], appendWithoutCommon(a1, a2, 2) = [0, 1, 2, 3, 6, 7].
     * @param a1 Array head
     * @param a2 Array tail
     * @return New array excluding tail index 0
     */
    public static double[] appendExcludingN(double[] a1, double[] a2, int indexesToIgnore) {
        var newArray = Arrays.copyOf(a1, a1.length + a2.length - indexesToIgnore);
        System.arraycopy(a2, indexesToIgnore, newArray, a1.length, a2.length - indexesToIgnore);
        return newArray;
    }
}