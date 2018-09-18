package org.opentripplanner.routing.algorithm.raptor.mcrr.api;


/**
 * Tuple for destination stop and a duration to reach the stop. The denature place is
 * given by the context and not part of the class.
 */
public interface DurationToStop {

    /** The destination stop */
    int stop();

    /** The duration to reach the stop in seconds. */
    int durationInSeconds();
}
