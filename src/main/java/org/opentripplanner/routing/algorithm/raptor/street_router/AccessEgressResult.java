package org.opentripplanner.routing.algorithm.raptor.street_router;

import gnu.trove.map.TObjectDoubleMap;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.graph.Vertex;

import java.util.List;

public class AccessEgressResult {
    public TObjectDoubleMap<Vertex> timesToStops;
    public List<Transfer> transferList;
}
