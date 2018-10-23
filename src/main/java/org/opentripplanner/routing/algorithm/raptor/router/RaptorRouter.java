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
import org.opentripplanner.routing.spt.DominanceFunction;

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
    private static final int SEARCH_RANGE_SECONDS = 60;

    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        this.otpRRDataProvider = new OtpRRDataProvider(transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), request.modes, request.walkSpeed);
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

        List<ParetoItinerary> itineraries = new ArrayList<>();

        // Limit paths to number of itineraries requested
        for (Path2 p : paths.stream().sorted(Comparator.comparing(p -> p.egressLeg().toTime()))
                .collect(Collectors.toList())) {
            itineraries.add(itineraryMapper.createItinerary(request, p, accessTransfers, egressTransfers));
        }

        filterByParetoSet(itineraries);

        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        tripPlan.itinerary = itineraries.stream().map(p -> (Itinerary)p).collect(Collectors.toList());

        return tripPlan;
    }

    void filterByParetoSet(Collection<ParetoItinerary> itineraries) {
        ParetoSet<ParetoItinerary> paretoSet = new ParetoSet<>(ParetoItinerary.paretoDominanceFunctions());
        itineraries.stream().forEach(p -> {
            p.initParetoVector();
            paretoSet.add(p);
        });
        itineraries = new ArrayList<>();
        for (ParetoItinerary p : paretoSet.paretoSet()) {
            itineraries.add(p);
        }
    }
}
