package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class TripPattern {
    private final int id;

    private final List<TripScheduleImpl> tripSchedules;

    private final TraverseMode transitMode;

    private final TransmodelTransportSubmode transitSubMode;

    private final int[] stopPattern;

    public TripPattern(int id, List<TripScheduleImpl> tripSchedules, TraverseMode transitMode, TransmodelTransportSubmode transitSubMode, int[] stopPattern) {
        this.id = id;
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.transitSubMode = transitSubMode;
        this.stopPattern = stopPattern;
    }

    public int getId() { return id; }

    public List<TripScheduleImpl> getTripSchedules() {
        return tripSchedules;
    }

    public TraverseMode getTransitMode() {
        return transitMode;
    }

    public TransmodelTransportSubmode getTransitSubMode() {
        return transitSubMode;
    }

    public int[] getStopPattern() {
        return stopPattern;
    }
}
