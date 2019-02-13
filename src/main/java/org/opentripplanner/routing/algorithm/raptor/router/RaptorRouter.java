package org.opentripplanner.routing.algorithm.raptor.router;

import com.conveyal.r5.profile.entur.RangeRaptorService;
import com.conveyal.r5.profile.entur.api.TuningParameters;
import com.conveyal.r5.profile.entur.api.path.Path;
import com.conveyal.r5.profile.entur.api.request.RangeRaptorRequest;
import com.conveyal.r5.profile.entur.api.request.RequestBuilder;
import com.conveyal.r5.profile.entur.api.transit.TransferLeg;
import com.conveyal.r5.profile.entur.api.transit.TransitDataProvider;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import com.conveyal.r5.profile.entur.util.paretoset.*;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ParetoItinerary;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.street_router.TransferToAccessEgressLegMapper;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.*;
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
    private final TransitDataProvider<TripSchedule> otpRRDataProvider;
    private final TransitLayer transitLayer;
    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        double startTime = System.currentTimeMillis();
        this.otpRRDataProvider = new OtpRRDataProvider(transitLayer, request.getDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), request.raptorSearchDays, request.modes, request.transportSubmodes, request.walkSpeed);
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

        TransferToAccessEgressLegMapper mapper = new TransferToAccessEgressLegMapper(transitLayer);
        Collection<TransferLeg> accessTimes = mapper.map(accessTransfers, request.walkSpeed);
        Collection<TransferLeg> egressTimes = mapper.map(egressTransfers, request.walkSpeed);

        LOG.info("Access/egress routing took {} ms", System.currentTimeMillis() - startTimeAccessEgress);

        /**
         * Prepare transit search
         */

        double startTimeRouting = System.currentTimeMillis();

        RangeRaptorService<TripSchedule> rangeRaptorService = new RangeRaptorService<>(new TuningParameters() {});

        int departureTime = Instant.ofEpochMilli(request.dateTime * 1000).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();



        RangeRaptorRequest rangeRaptorRequest = new RequestBuilder()
                .earliestDepartureTime(departureTime)
                .searchWindowInSeconds(request.raptorSearchRange * 60)
                .addAccessStops(accessTimes)
                .addEgressStops(egressTimes)
                .boardSlackInSeconds(60)
                .profile(request.raptorProfile)
                .build();

        /**
         * Route transit
         */

        Collection<Path<TripSchedule>> paths = new ArrayList<>(rangeRaptorService.route(rangeRaptorRequest, this.otpRRDataProvider));

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

    private void filterByParetoSet(Collection<Itinerary> itineraries) {
        ParetoSet<ParetoItinerary> paretoSet = new ParetoSet<>(ParetoItinerary.paretoComperator());
        List<ParetoItinerary> paretoItineraries = itineraries.stream()
                .map(ParetoItinerary::new)
                .collect(Collectors.toList());

        paretoItineraries.forEach(p -> {
            p.initParetoVector();
            paretoSet.add(p);
        });
        itineraries.clear();
        itineraries.addAll(paretoSet);
    }
}
