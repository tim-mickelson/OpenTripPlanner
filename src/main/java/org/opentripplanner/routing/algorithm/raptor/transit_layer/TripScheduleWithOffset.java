package org.opentripplanner.routing.algorithm.raptor.transit_layer;

public class TripScheduleWithOffset extends TripSchedule {

    private final int offset;
    private static final int SECONDS_OF_DAY = 86400;

    TripScheduleWithOffset(TripSchedule tripSchedule, int offset) {
        super(tripSchedule);
        this.offset = offset;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return super.arrival(stopPosInPattern) + offset * SECONDS_OF_DAY;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return super.departure(stopPosInPattern) + offset * SECONDS_OF_DAY;
    }
}
