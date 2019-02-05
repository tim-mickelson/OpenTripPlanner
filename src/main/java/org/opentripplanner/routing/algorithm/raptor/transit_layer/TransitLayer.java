package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TransitLayer {

    /** Transit data required for routing */
    public TripPattern[] tripPatterns;
    public Map<LocalDate, List<TripPatternForDate>> tripPatternsForDate;
    public List<Transfer>[] transferByStop;

    /** Maps to original graph to retrieve additional data */
    public Stop[] stopsByIndex;
    public Map<Stop, Integer> indexByStop;
    public Map<OrderedIndexPair, Transfer> transferByStopPair;

    public int getStopCount() { return stopsByIndex.length; }

    public int getIndexByStop(Stop stop) {
        return indexByStop.get(stop);
    }

    public Stop getStopByIndex(int stopIndex) {
        return stopIndex != -1 ? stopsByIndex[stopIndex] : null;
    }

    public Transfer getTransfer(int fromIndex, int toIndex) {
        return transferByStopPair.get(new OrderedIndexPair(fromIndex, toIndex));
    }

    public Collection<TripPatternForDate> getTripPatternsForDate(LocalDate date) {
        return tripPatternsForDate.get(date);
    }

    public List<Transfer>[] getTransferByStop() {
        return this.transferByStop;
    }
}
