package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Vertex;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.Map;

public interface TransitLayer {
    int getIndexByStop(Stop stop);
    Stop getStopByIndex(int stopIndex);
    BitSet getActiveServicesForDate(LocalDate date);
    TIntList getTransfersForStop(int stop);
    TIntList getPatternsForStop(int stop);
    TripPattern[] getTripPatterns();
    int getStopCount();
    void addTransfer(int fromStopId, int toStopId, int timeInSeconds, Transfer transfer);
    org.opentripplanner.routing.edgetype.TripPattern getTripPatternByIndex(int tripPatternsIndex);
    Transfer getTransfer(int fromIndex, int toIndex);
    void addAccessEgressTransfers(Map<Vertex, Transfer> transferMap, boolean mapAccess);
    void setAccessEgressStops(Stop accessStop, Stop egressStop);
}
