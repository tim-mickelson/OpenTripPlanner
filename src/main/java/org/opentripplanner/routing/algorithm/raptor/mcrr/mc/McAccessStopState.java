package org.opentripplanner.routing.algorithm.raptor.mcrr.mc;


import org.opentripplanner.routing.algorithm.raptor.mcrr.api.PathLeg;
import org.opentripplanner.routing.algorithm.raptor.mcrr.util.DebugState;

import static org.opentripplanner.routing.algorithm.raptor.mcrr.util.DebugState.Type.Access;


public class McAccessStopState extends McStopState {
    private int fromTime;

    McAccessStopState(int stopIndex, int fromTime,  int accessTime) {
        super(null, 0, 0, stopIndex, fromTime + accessTime);
        this.fromTime = fromTime;
    }

    @Override
    PathLeg mapToLeg() {
        return McPathLeg.createAccessLeg(this, fromTime);
    }

    @Override
    DebugState.Type type() { return Access; }


    public int getFromTime() {
        return fromTime;
    }
}
