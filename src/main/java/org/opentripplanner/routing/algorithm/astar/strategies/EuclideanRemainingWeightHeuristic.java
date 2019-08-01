package org.opentripplanner.routing.algorithm.astar.strategies;

import com.google.common.collect.Iterables;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A Euclidean remaining weight strategy for non-transit searches.
 * 
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = -5172878150967231550L;

    private double lat;
    private double lon;
    private double maxSpeed;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        Vertex target = options.rctx.target;
        target = getConnectedStreetVertex(target);

        lat = target.getLat();
        lon = target.getLon();

        maxSpeed = options.getStreetSpeedUpperBound();
    }

    private Vertex getConnectedStreetVertex(Vertex target) {
        if (target.getDegreeIn() == 1) {
            Edge edge = Iterables.getOnlyElement(target.getIncoming());
            if (edge instanceof FreeEdge) {
                target = edge.getFromVertex();
            }
        }
        return target;
    }

    /**
     * On a non-transit trip, the remaining weight is simply distance / street speed.
     */
    @Override
    public double estimateRemainingWeight (State s) {
        Vertex sv = s.getVertex();
        double euclideanDistance = SphericalDistanceLibrary.fastDistance(sv.getLat(), sv.getLon(), lat, lon);
        // all travel is on-street, no transit involved
        return euclideanDistance / maxSpeed;
    }
}
