package org.opentripplanner.routing.algorithm.astar.strategies;

import java.io.Serializable;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) 
 * the weight of a path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic extends Serializable {
	
    /** 
     * Perform any one-time setup and pre-computation that will be needed by later calls to
     * computeForwardWeight/computeReverseWeight. We may want to start from multiple origin states, so initialization
     * cannot depend on the origin vertex or state.
     * @param abortTime time since the Epoch in milliseconds at which we should bail out of initialization,
     *                  or Long.MAX_VALUE for no limit.
     */
    void initialize (RoutingRequest options, long abortTime);

    double estimateRemainingWeight (State s);
}


// Perhaps directionality should also be defined during the setup,
// instead of having two separate methods for the two directions.
// We might not even need a setup method if the routing options are just passed into the
// constructor.
