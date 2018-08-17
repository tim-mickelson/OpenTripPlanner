package org.opentripplanner.routing.algorithm.raptor.itinerary;

import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

public class SimpleTransferGenerator {
    private final Graph graph;
    private final TransitLayer transitLayer;

    public SimpleTransferGenerator(Graph graph, TransitLayer transitLayer) {
        this.graph = graph;
        this.transitLayer = transitLayer;
    }

    /** Create SimpleTransfers from stop indices. Transfers from/to index 0 and 1 are reserved for access/egress legs
     * and require special handling
     */
    public SimpleTransfer createSimpleTransfer(int fromStopIndex, int toStopIndex) {
        if (fromStopIndex != 0 && toStopIndex != 1) {
            PatternStopVertex fromStopVertex = (PatternStopVertex)graph.index.vertexForId.get(transitLayer.stopsByIndex[fromStopIndex]);
            PatternStopVertex toStopVertex = (PatternStopVertex)graph.index.vertexForId.get(transitLayer.stopsByIndex[toStopIndex]);
            return (SimpleTransfer)fromStopVertex.getOutgoing().stream().filter(e -> e.getToVertex().equals(toStopVertex)).findFirst().get();
        }
        else {
            // TODO: Do street routing a create a simpleTransfer
            return null;
        }
    }
}
