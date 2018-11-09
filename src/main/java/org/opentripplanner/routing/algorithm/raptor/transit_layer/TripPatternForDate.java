package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripPatternInfo;
import com.conveyal.r5.profile.entur.api.TripScheduleInfo;

import java.util.List;

public class TripPatternForDate implements TripPatternInfo<TripSchedule> {
    private final TripPattern tripPattern;
    private final List<TripSchedule> tripSchedules;

    TripPatternForDate(TripPattern tripPattern, List<TripSchedule> tripSchedules) {
        this.tripPattern = tripPattern;
        this.tripSchedules = tripSchedules;
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }

    @Override
    public int currentPatternStop(int i) {
        return this.tripPattern.getStopPattern()[i];
    }

    @Override
    public int numberOfStopsInPattern() {
        return tripPattern.getStopPattern().length;
    }

    @Override
    public TripSchedule getTripSchedule(int i) {
        return tripSchedules.get(i);
    }

    @Override
    public int numberOfTripSchedules() {
        return tripSchedules.size();
    }
}
