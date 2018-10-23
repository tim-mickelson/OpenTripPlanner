package org.opentripplanner.routing.algorithm.raptor.router;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;

import java.util.List;

public class DirectStreetRouter {
    private final double MINIMUM_WALK_DISTANCE = 5000;

    public Itinerary route(RoutingRequest request, Router router, TraverseMode traverseMode) {
         if (SphericalDistanceLibrary.distance(request.from.getCoordinate(), request.to.getCoordinate())
                 < MINIMUM_WALK_DISTANCE) {

             RoutingRequest walkRequest = request.clone();
             walkRequest.modes = new TraverseModeSet(traverseMode);

             GraphPathFinder gpFinder = new GraphPathFinder(router);
             List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(walkRequest);
             TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, walkRequest);

             if (!plan.itinerary.isEmpty()) {
                 return plan.itinerary.get(0);
             }
         }
         return null;
    }
}
