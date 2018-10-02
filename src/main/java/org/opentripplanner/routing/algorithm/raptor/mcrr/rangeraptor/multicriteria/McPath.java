package org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria;

import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.PathLeg;

import java.util.ArrayList;
import java.util.List;


final class McPath implements Path2 {
    private final PathLeg accessLeg;
    private final List<PathLeg> legs = new ArrayList<>();
    private final PathLeg egressLeg;

    McPath(List<McStopState> states, int egressTime) {
        this.accessLeg = McPathLeg.createAccessLeg((McAccessStopState)states.get(0), states.get(1).boardTime());

        for (int i=1; i<states.size(); ++i) {
            this.legs.add(McPathLeg.createLeg(states.get(i)));
        }
        this.egressLeg = McPathLeg.createEgressLeg((McTransitStopState) states.get(states.size()-1), egressTime);
    }

    @Override
    public PathLeg accessLeg() {
        return accessLeg;
    }

    @Override
    public Iterable<? extends PathLeg> legs() {
        return legs;
    }

    @Override
    public PathLeg egressLeg() {
        return egressLeg;
    }
}
