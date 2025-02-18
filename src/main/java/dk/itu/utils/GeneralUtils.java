package dk.itu.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneralUtils {
    /**
     * Split a List into n partitions of equal sizes, maintaining order.
     * @param originalList List to partition
     * @param n Number of partitions
     * @return Lists of lists, of equal size
     * @param <T> Generic
     */
    public static <T> List<List<T>> splitList(List<T> originalList, int n) {
        if (originalList == null || originalList.isEmpty() || n <= 0) {
            return new ArrayList<>();
        }

        int size = originalList.size();
        int sublistSize = (int) Math.ceil((double) size / n);

        return IntStream.range(0, n)
                .parallel()
                .mapToObj(i -> originalList.stream()
                        .skip((long) i * sublistSize)
                        .limit(sublistSize)
                        .collect(Collectors.toList()))
                .filter(sublist -> !sublist.isEmpty())
                .collect(Collectors.toList());
    }
}