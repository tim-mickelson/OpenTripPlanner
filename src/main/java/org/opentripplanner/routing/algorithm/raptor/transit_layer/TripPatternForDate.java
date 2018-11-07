package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripPatternInfo;
import com.conveyal.r5.profile.entur.api.TripScheduleInfo;

import java.util.List;

public class TripPatternForDate implements TripPatternInfo {
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
    public int originalPatternIndex() {
        return 0;
    }

    @Override
    public int currentPatternStop(int i) {
        return this.tripPattern.getStopPattern()[i];
    }

    @Override
    public int currentPatternStopsSize() {
        return this.tripPattern.getStopPattern().length;
    }

    @Override
    public TripScheduleInfo getTripSchedule(int i) {
        return tripSchedules.get(i);
    }

    @Override
    public int getTripScheduleSize() {
        return tripSchedules.size();
    }
}
