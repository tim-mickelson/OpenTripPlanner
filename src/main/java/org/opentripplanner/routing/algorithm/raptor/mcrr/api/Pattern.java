package org.opentripplanner.routing.algorithm.raptor.mcrr.api;

// TODO TGR - add JavaDoc
public interface Pattern {
    // TODO TGR - add JavaDoc
    int originalPatternIndex();

    // TODO TGR - add JavaDoc
    int currentPatternStop(int stopPositionInPattern);

    // TODO TGR - add JavaDoc
    int currentPatternStopsSize();

    // TODO TGR - add JavaDoc
    TripScheduleInfo getTripSchedule(int index);

    // TODO TGR - add JavaDoc
    int getTripScheduleSize();
}
