package org.opentripplanner.routing.algorithm.raptor.mcrr.api;

public interface Path2 {

    PathLeg accessLeg();

    Iterable<? extends PathLeg> legs();

    PathLeg egressLeg();

}
