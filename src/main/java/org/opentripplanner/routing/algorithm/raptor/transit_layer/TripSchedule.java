package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripScheduleInfo;

/**
 * This represents the arrival and departure times of a single GTFS trip within a TripPattern.
 * If this is a frequency trip, it also records the different headways throughout the day, and when those headways
 * begin and end.
 */
public class TripSchedule implements TripScheduleInfo {

    public String tripId;

    /**
     * Arrival times in seconds from midnight by stop index
     */
    public int[] arrivals;
    /**
     * Departure times in seconds from midnight by stop index
     */
    public int[] departures;

    public int serviceCode;

    public int dayOffset;

    public Integer headwaySeconds;

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivals[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return departures[stopPosInPattern];
    }
}
