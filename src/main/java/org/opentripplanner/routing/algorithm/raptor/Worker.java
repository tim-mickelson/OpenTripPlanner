package org.opentripplanner.routing.algorithm.raptor;

import java.util.Collection;

public interface Worker {
    Collection<Path> route();
}
