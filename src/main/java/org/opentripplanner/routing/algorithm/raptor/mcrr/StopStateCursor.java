package org.opentripplanner.routing.algorithm.raptor.mcrr;

public interface StopStateCursor {

    StopState stop(int round, int stop);

    boolean stopNotVisited(int round, int stop);
}
