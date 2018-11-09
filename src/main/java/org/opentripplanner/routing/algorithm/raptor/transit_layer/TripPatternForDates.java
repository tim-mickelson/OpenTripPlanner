package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripPatternInfo;
import com.conveyal.r5.profile.entur.api.TripScheduleInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TripPatternForDates implements TripPatternInfo<TripSchedule> {
    private final TripPattern tripPattern;
    private final List<List<TripSchedule>> tripSchedules;
    private static final int SECONDS_OF_DAY = 86400;


    TripPatternForDates(TripPattern tripPattern, List<List<TripSchedule>> tripSchedulesPerDay) {
        this.tripPattern = tripPattern;
        this.tripSchedules = tripSchedulesPerDay;
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
        int index = i;
        int dayOffset = 0;
        for (List<TripSchedule> tripScheduleList : tripSchedules ) {
            if (i < tripScheduleList.size()) {
                return new TripScheduleWithOffset(tripScheduleList.get(index), dayOffset * SECONDS_OF_DAY);
            }
            index -= tripScheduleList.size();
            dayOffset++;
        }
        throw new IndexOutOfBoundsException("Index out of bound: " + i);
    }

    @Override
    public int numberOfTripSchedules() {
        return tripSchedules.size();
    }
}
