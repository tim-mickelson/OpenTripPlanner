package org.opentripplanner.routing.algorithm.raptor.mcrr.api;


import java.util.Collection;

public interface Worker<P> {
    Collection<P> route(RangeRaptorRequest request);
}
