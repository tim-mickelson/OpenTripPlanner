package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransitLayer {

    /** Transit data required for routing */

    public List<TripPattern> tripPatterns;
    public List<Service> services;
    public TIntList[] patternsByStop;
    public TIntList[] transfers; // Seconds

    /** Maps to original graph to retrieve additional data */
    public Stop[] stopsByIndex; // Index 0 and 1 are reserved for access/egress
    public Map<Stop, Integer> indexByStop;
    public org.opentripplanner.routing.edgetype.TripPattern[] tripPatternByIndex;
    public List<Trip>[] tripByIndex;
    public Map<OrderedIndexPair, Transfer> transferMap;

    public BitSet[] getActiveServicesForDates(LocalDate date, int dayRange) {
        BitSet[] activeServicesPerDay = new BitSet[dayRange];
        for (int day = 0; day < dayRange; day++) {
            activeServicesPerDay[day] = new BitSet();
            for (int i = 0; i < this.services.size(); i++) {
                if (this.services.get(i).activeOn(date.plusDays(day))) {
                    activeServicesPerDay[day].set(i);
                }
            }
        }
        return activeServicesPerDay;
    }

    public TIntList getPatternsForStop(int stop) {
        return patternsByStop[stop];
    }

    public List<TripPattern> getTripPatterns() {
        return tripPatterns;
    }

    public int getStopCount() { return stopsByIndex.length; }

    public int getIndexByStop(Stop stop) {
        return indexByStop.get(stop);
    }

    public Stop getStopByIndex(int stopIndex) {
        return stopIndex != -1 ? stopsByIndex[stopIndex] : null;
    }

    public org.opentripplanner.routing.edgetype.TripPattern getTripPatternByIndex(int tripPatternsIndex) {
        return tripPatternByIndex[tripPatternsIndex];
    }

    public Transfer getTransfer(int fromIndex, int toIndex) {
        return transferMap.get(new OrderedIndexPair(fromIndex, toIndex));
    }

    public List<TIntList> transfersForStop() {
        return Arrays.stream(transfers).collect(Collectors.toList());
    }
}
