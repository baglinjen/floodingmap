package dk.itu.common.utils.extensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayExtensions {
    public static <T> List<T> appendSingle(List<T> list, T t){
        List<T> newList = new ArrayList<>(list);
        newList.add(t);
        return Collections.unmodifiableList(newList);
    }
}
