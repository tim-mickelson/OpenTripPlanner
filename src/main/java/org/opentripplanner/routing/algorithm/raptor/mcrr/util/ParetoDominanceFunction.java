package org.opentripplanner.routing.algorithm.raptor.mcrr.util;

public interface ParetoDominanceFunction {
    boolean dominates(int v, int u);
    boolean mutualDominance(int v, int u);
}
