package org.opentripplanner.routing.algorithm.raptor_old;

public interface PathBuilder {
    Path extractPathForStop(int maxRound, int egressStop);
}
