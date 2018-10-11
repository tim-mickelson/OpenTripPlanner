package org.opentripplanner.routing.algorithm.raptor.router;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.DurationToStop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.RangeRaptorRequest;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.TransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria.McRangeRaptorWorker;
import org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria.McWorkerState;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.street_router.DuationToStopMapper;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {
    private final TransitDataProvider otpRRDataProvider;
    private final TransitLayer transitLayer;
    private static final int MAX_DURATION_SECONDS = 36 * 60 * 60;
    private static final int SEARCH_RANGE = 60;

    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        this.otpRRDataProvider = new OtpRRDataProvider(transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), request.modes);
        this.transitLayer = transitLayer;
    }

    public TripPlan route(RoutingRequest request) {

        /**
         * Prepare access/egress transfers
         */

        Map<Stop, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);
        Map<Stop, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

        DuationToStopMapper duationToStopMapper = new DuationToStopMapper(transitLayer);
        Collection<DurationToStop> accessTimes = duationToStopMapper.map(accessTransfers);
        Collection<DurationToStop> egressTimes = duationToStopMapper.map(egressTransfers);

        /**
         * Prepare transit search
         */

        McRangeRaptorWorker worker = new McRangeRaptorWorker(
                this.otpRRDataProvider,
                new McWorkerState(
                request.maxTransfers + 5,
                    transitLayer.getStopCount(),
                    MAX_DURATION_SECONDS
        ));

        /**
         * Route transit
         */

        int departureTime = Instant.ofEpochMilli(request.dateTime * 1000).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();

        Collection<Path2> paths = worker.route(
                new RangeRaptorRequest(
                        departureTime,
                        departureTime + SEARCH_RANGE,
                        accessTimes,
                        egressTimes,
                        60,
                        60));

        /**
         * Create itineraries
         */

        ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, request);

        List<Itinerary> itineraries = new ArrayList<>();

        // Limit paths to number of itineraries requested
        for (Path2 p : paths.stream().sorted(Comparator.comparing(p -> p.egressLeg().toTime()))
                .limit(request.numItineraries).collect(Collectors.toList())) {
            itineraries.add(itineraryMapper.createItinerary(request, p, accessTransfers, egressTransfers));
        }

        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        tripPlan.itinerary = itineraries;

        return tripPlan;
    }
}
