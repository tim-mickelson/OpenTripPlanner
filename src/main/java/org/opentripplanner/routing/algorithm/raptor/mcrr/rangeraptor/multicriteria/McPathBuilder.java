package org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria;

import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.util.DebugState;


/**
 * TODO TGR
 */
class McPathBuilder {

    Path2 extractPathsForStop(McStopState egressStop, int egressDurationInSeconds) {
        if (!egressStop.arrivedByTransit()) {
            return null;
        }
        debugPath(egressStop);
        return new McPath(egressStop.path(), egressDurationInSeconds);
    }

    private void debugPath(McStopState egressStop) {
        DebugState.debugStopHeader("MC - CREATE PATH FOR EGRESS STOP: " + egressStop.stopIndex());

        if(DebugState.isDebug(egressStop.stopIndex())) {
            for (McStopState p : egressStop.path()) {
                p.debug();
            }
        }
    }
}
