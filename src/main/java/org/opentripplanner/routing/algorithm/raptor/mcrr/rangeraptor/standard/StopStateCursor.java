package org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.standard;

public interface StopStateCursor {

    StopState stop(int round, int stop);

    boolean stopNotVisited(int round, int stop);
}