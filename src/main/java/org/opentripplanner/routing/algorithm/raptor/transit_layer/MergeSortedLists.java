package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MergeSortedLists {
    static <E, R> List<R> merge(List<E> left, List<E> right, Function<E,R> singleResultMapper, BiFunction<E, E, R> combiner, Comparator<E> comparator) {
        List<R> combinedList = new ArrayList<>();

        int leftIndex = 0;
        int rightIndex = 0;

        while (leftIndex < left.size() && rightIndex < right.size()) {
            E leftItem = left.get(leftIndex);
            E rightItem = right.get(rightIndex);

            int compareResult = comparator.compare(left.get(leftIndex), right.get(rightIndex));

            if (compareResult == 0) {
                combinedList.add(combiner.apply(left.get(leftIndex), right.get(rightIndex)));
                leftIndex++;
                rightIndex++;
            } else if (compareResult > 0 ) {
                combinedList.add(singleResultMapper.apply(leftItem));
                leftIndex++;
            } else {
                rightIndex++;
                combinedList.add(singleResultMapper.apply(rightItem));
            }
        }

        return combinedList;
    }
}
