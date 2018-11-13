package org.opentripplanner.routing.algorithm.raptor.transit_layer;

/**
 * This represents a single trip within a TripPattern, but with a time offset in seconds. This is used to represent
 * a trip on a subsequent service day than the first one in the date range used.
 */

public class TripScheduleWithOffset extends TripSchedule {

    private final int secondsOffset;

    TripScheduleWithOffset(TripSchedule tripSchedule, int offset) {
        super(tripSchedule);
        this.secondsOffset = offset;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return super.arrival(stopPosInPattern) + secondsOffset;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return super.departure(stopPosInPattern) + secondsOffset;
    }
}
