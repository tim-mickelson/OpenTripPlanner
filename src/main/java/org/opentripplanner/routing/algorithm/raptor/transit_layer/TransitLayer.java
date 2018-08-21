package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.util.BitSet;

public interface TransitLayer {
    int getIndexByStop(Stop stop);
    BitSet getActiveServicesForDate(LocalDate date);
    TIntList getTransfersForStop(int stop);
    TIntList getPatternsForStop(int stop);
    TripPattern[] getTripPatterns();
    int getStopCount();
    void addTransfer(int fromStopId, int toStopId, int timeInSeconds);
}
