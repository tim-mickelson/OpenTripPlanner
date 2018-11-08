package org.opentripplanner.routing.algorithm.raptor.router;

import com.conveyal.r5.profile.entur.RangeRaptorService;
import com.conveyal.r5.profile.entur.api.*;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import com.conveyal.r5.profile.entur.util.paretoset.*;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ParetoItinerary;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.street_router.StopArrivalMapper;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int SEARCH_RANGE_SECONDS = 60;
    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        double startTime = System.currentTimeMillis();
        this.otpRRDataProvider = new OtpRRDataProvider(transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), 4, request.modes, request.transportSubmodes, request.walkSpeed);
        LOG.info("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);
        this.transitLayer = transitLayer;
    }

    public TripPlan route(RoutingRequest request) {

        /**
         * Prepare access/egress transfers
         */

        double startTimeAccessEgress = System.currentTimeMillis();

        Map<Stop, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);
        Map<Stop, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

        StopArrivalMapper stopArrivalMapper = new StopArrivalMapper(transitLayer);
        Collection<StopArrival> accessTimes = stopArrivalMapper.map(accessTransfers, request.walkSpeed);
        Collection<StopArrival> egressTimes = stopArrivalMapper.map(egressTransfers, request.walkSpeed);

        LOG.info("Access/egress routing took {} ms", System.currentTimeMillis() - startTimeAccessEgress);

        /**
         * Prepare transit search
         */

        double startTimeRouting = System.currentTimeMillis();

        RangeRaptorService rangeRaptorService = new RangeRaptorService(new TuningParameters() {
            @Override
            public int maxNumberOfTransfers() {
                return 12;
            }
        });

        int departureTime = Instant.ofEpochMilli(request.dateTime * 1000).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();

        RangeRaptorRequest rangeRaptorRequest = new RangeRaptorRequest.Builder(departureTime, departureTime + SEARCH_RANGE_SECONDS)
        .addAccessStops(accessTimes)
        .addEgressStops(egressTimes)
        .departureStepInSeconds(60)
        .boardSlackInSeconds(60)
        .profile(RaptorProfiles.MULTI_CRITERIA)
        .build();


        /**
         * Route transit
         */

        Collection<Path2> paths = new ArrayList<>(rangeRaptorService.route(rangeRaptorRequest, this.otpRRDataProvider));

        LOG.info("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

        /**
         * Create itineraries
         */

        double startItineraries = System.currentTimeMillis();

        ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, request);

        List<Itinerary> itineraries = paths.stream()
                .map(p -> itineraryMapper.createItinerary(request, p, accessTransfers, egressTransfers))
                .collect(Collectors.toList());

        filterByParetoSet(itineraries);

        TripPlan tripPlan = itineraryMapper.createTripPlan(request, itineraries);

        LOG.info("Creating itineraries took {} ms", System.currentTimeMillis() - startItineraries);

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
        for (ParetoItinerary p : paretoSet) {
            itineraries.add(p);
        }
    }
}
