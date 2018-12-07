package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import java.util.Objects;

public class OrderedIndexPair {
    OrderedIndexPair(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    private final int fromIndex;
    private final int toIndex;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderedIndexPair that = (OrderedIndexPair) o;
        return fromIndex == that.fromIndex &&
                toIndex == that.toIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromIndex, toIndex);
    }
}