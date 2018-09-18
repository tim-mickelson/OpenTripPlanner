package org.opentripplanner.routing.algorithm.raptor.router;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.DurationToStop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.RangeRaptorRequest;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.TransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria.McRangeRaptorWorker;
import org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria.McWorkerState;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressToStopStatesMapper;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayerMapper;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    private final TransitDataProvider otpRRDataProvider;
    private final Graph graph;
    private static final int MAX_DURATION_SECONDS = 36 * 60 * 60;

    public RaptorRouter(RoutingRequest request, Graph graph) {
        // Map transitLayer again to remove temporary transfers
        TransitLayerMapper transitLayerMapper = new TransitLayerMapper();
        graph.transitLayer = transitLayerMapper.map(graph);

        this.otpRRDataProvider = new OtpRRDataProvider(graph.transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), request.modes);
        this.graph = graph;
    }

    public TripPlan route(RoutingRequest request) {
        /**
         * Prepare access/egress transfers
         */

        Map<TransitStop, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);
        Map<TransitStop, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

        AccessEgressToStopStatesMapper accessEgressToStopStatesMapper = new AccessEgressToStopStatesMapper(graph.transitLayer);

        Collection<DurationToStop> accessTimes = accessEgressToStopStatesMapper.map(accessTransfers);
        Collection<DurationToStop> egressTimes = accessEgressToStopStatesMapper.map(egressTransfers);

        /**
         * Prepare transit search
         */

        int departureTime = Instant.ofEpochMilli(request.dateTime*1000).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();

        TransitDataProvider transitData = this.otpRRDataProvider;

        McWorkerState state = new McWorkerState(
                request.maxTransfers + 5,
                graph.transitLayer.getStopCount(),
                MAX_DURATION_SECONDS
        );

        McRangeRaptorWorker worker = new McRangeRaptorWorker(
                transitData,
                state
        );

        /**
         * Route transit
         */



        Collection<Path2> paths = worker.route(new RangeRaptorRequest(departureTime, departureTime + 60, accessTimes, egressTimes, 60, 60));

        /**
         * Create itineraries
         */

        ItineraryMapper itineraryMapper = new ItineraryMapper(this.graph.transitLayer, this.graph);

        List<Itinerary> itineraries = new ArrayList<>();

        for (Path2 p : paths) {
            itineraries.add(itineraryMapper.createItinerary(request, p));
        }

        TripPlan tripPlan = new TripPlan();
        tripPlan.itinerary = itineraries;

        return tripPlan;
    }
}
