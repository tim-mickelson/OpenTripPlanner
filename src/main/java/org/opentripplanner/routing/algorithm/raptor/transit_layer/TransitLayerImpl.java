package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class TransitLayerImpl implements TransitLayer {

    /** Transit data required for routing */
    public TripPattern[] patterns;
    public List<Service> services;
    public TIntList[] patternsByStop;
    public TIntList[] transfers;

    /** Maps to original graph to retrieve additional data */
    public Stop[] stopsByIndex; // Index 0 and 1 are reserved
    public Map<Stop, Integer> indexByStop;
    public org.opentripplanner.routing.edgetype.TripPattern[] tripPatternByIndex;
    public List<Trip>[] tripByIndex;

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
        return transfers[stop];
    }

    public TIntList getPatternsForStop(int stop) {
        return patternsByStop[stop];
    }

    public TripPattern[] getTripPatterns() {
        return patterns;
    }

    public int getStopCount() { return stopsByIndex.length; }

    public void addTransfer(int fromStopId, int toStopId, int timeInSeconds) {
        transfers[fromStopId].add(toStopId);
        transfers[fromStopId].add(timeInSeconds);
    }

    public int getIndexByStop(Stop stop) {
        return indexByStop.get(stop);
    }
}
