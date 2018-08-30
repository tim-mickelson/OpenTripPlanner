package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptor.util.OrderedIndexPair;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class TransitLayerImpl implements TransitLayer {

    /** Transit data required for routing */

    public TripPattern[] patterns;
    public List<Service> services;
    public TIntList[] patternsByStop;
    public TIntList[] transfers; // Seconds

    /** Maps to original graph to retrieve additional data */
    public Stop[] stopsByIndex; // Index 0 and 1 are reserved for access/egress
    public Map<Stop, Integer> indexByStop;
    public org.opentripplanner.routing.edgetype.TripPattern[] tripPatternByIndex;
    public List<Trip>[] tripByIndex;
    public Map<OrderedIndexPair, Transfer> transferMap;

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

    public void addTransfer(int fromStopId, int toStopId, int timeInSeconds, Transfer transfer) {
        System.out.println("From stop: " + fromStopId + " To stop: " + toStopId + " Time: " + timeInSeconds);
        transfers[fromStopId].add(toStopId);
        transfers[fromStopId].add(timeInSeconds);
        transferMap.put(new OrderedIndexPair(fromStopId, toStopId), transfer);
    }

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

    public void addAccessEgressTransfers(Map<Vertex, Transfer> transferMap, boolean mapAccess) {
        for (Map.Entry entry : transferMap.entrySet()) {
            Vertex stop = (TransitStop)entry.getKey();
            int stopIndex = this.getIndexByStop(((TransitStop) stop).getStop());
            Transfer transfer = (Transfer)entry.getValue();
            if (transfer.distance > 2000000) {
                continue;
            }

            if (mapAccess) {
                addTransfer(0, stopIndex, transfer.distance, transfer);
            } else {
                addTransfer(stopIndex, 1, transfer.distance, transfer);
            }
        }
    }

    public void setAccessEgressStops(Stop accessStop, Stop egressStop) {
        stopsByIndex[0] = accessStop;
        stopsByIndex[1] = egressStop;
    }
}
