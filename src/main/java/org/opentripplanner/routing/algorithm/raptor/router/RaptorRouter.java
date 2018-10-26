package org.opentripplanner.routing.algorithm.raptor.router;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import com.conveyal.r5.profile.entur.api.DurationToStop;
import com.conveyal.r5.profile.entur.api.Path2;
import com.conveyal.r5.profile.entur.api.RangeRaptorRequest;
import com.conveyal.r5.profile.entur.api.TransitDataProvider;
import com.conveyal.r5.profile.entur.rangeraptor.multicriteria.McRangeRaptorWorker;
import com.conveyal.r5.profile.entur.rangeraptor.multicriteria.McWorkerState;
import com.conveyal.r5.profile.entur.util.*;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ParetoItinerary;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.street_router.DuationToStopMapper;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.core.RoutingRequest;

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
    private static final int MAX_DURATION_SECONDS = 4 * 24 * 60 * 60;
    private static final int SEARCH_RANGE_SECONDS = 60;

    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        this.otpRRDataProvider = new OtpRRDataProvider(transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), 4, request.modes, request.walkSpeed);
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
        Collection<DurationToStop> accessTimes = duationToStopMapper.map(accessTransfers, request.walkSpeed);
        Collection<DurationToStop> egressTimes = duationToStopMapper.map(egressTransfers, request.walkSpeed);

        /**
         * Prepare transit search
         */

        McRangeRaptorWorker worker = new McRangeRaptorWorker(
                this.otpRRDataProvider,
                new McWorkerState(
                request.maxTransfers,
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
                        departureTime + SEARCH_RANGE_SECONDS,
                        accessTimes,
                        egressTimes,
                        60,
                        60));

        /**
         * Create itineraries
         */

        ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, request);

        List<Itinerary> itineraries = paths.stream()
                .map(p -> itineraryMapper.createItinerary(request, p, accessTransfers, egressTransfers))
                .collect(Collectors.toList());

        filterByParetoSet(itineraries);

        TripPlan tripPlan = itineraryMapper.createTripPlan(request, itineraries);

        return tripPlan;
    }

    void filterByParetoSet(Collection<Itinerary> itineraries) {
        ParetoSet<ParetoItinerary> paretoSet = new ParetoSet<>(ParetoItinerary.paretoDominanceFunctions());
        List<ParetoItinerary> paretoItineraries = itineraries.stream().map(i -> new ParetoItinerary(i)).collect(Collectors.toList());
        paretoItineraries.stream().forEach(p -> {
            p.initParetoVector();
            paretoSet.add(p);
        });
        itineraries.clear();
        for (ParetoItinerary p : paretoSet.paretoSet()) {
            itineraries.add(p);
        }
    }
}
