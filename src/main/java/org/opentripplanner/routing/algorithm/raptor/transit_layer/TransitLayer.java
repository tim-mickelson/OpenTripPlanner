package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class TransitLayer {

    /** Transit data required for routing */
    public TripPattern[] patterns;
    public List<Service> services;
    public TIntList[] patternsByStop;
    public TIntList[] transferDistances;

    /** Maps to original graph to retrieve additional data */
    public Stop[] stopsByIndex;
    public Map<Stop, Integer> indexByStop;
    public org.opentripplanner.routing.edgetype.TripPattern[] tripPatternByIndex;
    // TODO map trips

    public BitSet getActiveServicesForDate(LocalDate date) {
        BitSet acticeServices = new BitSet();
        for (int i = 0; i < this.services.size(); i++) {
            if (this.services.get(i).activeOn(date)) {
                acticeServices.set(i);
            }
        }
        return acticeServices;
    }

    public TIntList getTransfersForStop(int stop) {
        return transferDistances[stop];
    }

    public TIntList getPatternsForStop(int stop) {
        return patternsByStop[stop];
    }

    public TripPattern[] getTripPatterns() {
        return patterns;
    }

    public int getStopCount() { return stopsByIndex.length; }
}
