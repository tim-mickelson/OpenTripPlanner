package org.opentripplanner.routing.algorithm.raptor;

public interface PathBuilder {
    Path extractPathForStop(int maxRound, int egressStop);
}
