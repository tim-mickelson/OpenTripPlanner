package org.opentripplanner.routing.algorithm.raptor.transit_layer;

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
