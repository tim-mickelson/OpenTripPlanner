package org.opentripplanner.routing.algorithm.raptor.mcrr.mc;

import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.util.DebugState;


/**
 * TODO TGR
 */
class McPath2Builder {

    Path2 extractPathsForStop(McStopState egressStop, int egressTime) {
        if (!egressStop.arrivedByTransit()) {
            return null;
        }
        debugPath(egressStop);
        return new McPath(egressStop.path(), egressTime);
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
