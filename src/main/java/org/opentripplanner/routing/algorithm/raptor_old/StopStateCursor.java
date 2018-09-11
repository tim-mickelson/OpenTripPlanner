package org.opentripplanner.routing.algorithm.raptor_old;

public interface StopStateCursor {

    StopState stop(int round, int stop);

    boolean stopNotVisited(int round, int stop);
}
