package org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.standard;

import org.opentripplanner.routing.algorithm.raptor.mcrr.util.BitSetIterator;

public interface WorkerState {
    void initNewDepatureForMinute(int nextMinuteDepartureTime);

    void setInitialTime(int stop, int nextMinuteDepartureTime, int accesDurationInSeconds, int boardSlackInSeconds);

    void debugStopHeader(String header);

    boolean isNewRoundAvailable();

    void gotoNextRound();

    BitSetIterator stopsTouchedByTransitCurrentRound();

    void transferToStop(int fromStop, int toStop, int time);
}
