package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class TripPattern {
    private final int id;

    private final List<TripSchedule> tripSchedules;

    private final TraverseMode transitMode;

    private final TransmodelTransportSubmode transitSubMode;

    private final int[] stopPattern;

    private final org.opentripplanner.routing.edgetype.TripPattern originalTripPattern;

    public TripPattern(int id, List<TripSchedule> tripSchedules, TraverseMode transitMode, TransmodelTransportSubmode transitSubMode, int[] stopPattern, org.opentripplanner.routing.edgetype.TripPattern originalTripPattern) {
        this.id = id;
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.transitSubMode = transitSubMode;
        this.stopPattern = stopPattern;
        this.originalTripPattern = originalTripPattern;
    }

    public int getId() { return id; }

    public List<TripSchedule> getTripSchedules() {
        return tripSchedules;
    }

    public TraverseMode getTransitMode() {
        return transitMode;
    }

    public TransmodelTransportSubmode getTransitSubMode() {
        return transitSubMode;
    }

    public org.opentripplanner.routing.edgetype.TripPattern getOriginalTripPattern() { return this.originalTripPattern; }

    public int[] getStopPattern() {
        return stopPattern;
    }
}
