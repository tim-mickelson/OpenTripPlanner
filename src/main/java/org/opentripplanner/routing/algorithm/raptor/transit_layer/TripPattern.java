package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripPatternInfo;
import com.conveyal.r5.profile.entur.api.TripScheduleInfo;
import org.opentripplanner.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class TripPattern {
    private final List<TripSchedule> tripSchedules;

    private final TraverseMode transitMode;

    private final TransmodelTransportSubmode transitSubMode;

    private final int[] stopPattern;

    private final org.opentripplanner.routing.edgetype.TripPattern originalTripPattern;

    public TripPattern(List<TripSchedule> tripSchedules, TraverseMode transitMode, TransmodelTransportSubmode transitSubMode, int[] stopPattern, org.opentripplanner.routing.edgetype.TripPattern originalTripPattern) {
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.transitSubMode = transitSubMode;
        this.stopPattern = stopPattern;
        this.originalTripPattern = originalTripPattern;
    }

    public List<TripSchedule> getTripSchedules() {
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
