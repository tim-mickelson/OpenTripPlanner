package org.opentripplanner.routing.algorithm.raptor;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import javafx.util.Pair;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.RaptorWorkerTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.util.ParetoSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    private final OtpRRDataProvider otpRRDataProvider;
    private final Graph graph;
    private static final int MAX_DURATION_SECONDS = 8 * 60;

    public RaptorRouter(OtpRRDataProvider otpRRDataProvider, Graph graph) {
        this.otpRRDataProvider = otpRRDataProvider;
        this.graph = graph;
    }

    private List<Itinerary> route(RoutingRequest request) {
        TransitLayer transitLayer = otpRRDataProvider.getTransitLayer();

        /**
         * Prepare access/egress transfers
         */

        Map<Vertex, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);
        Map<Vertex, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

        transitLayer.addAccessEgressTransfers(accessTransfers, true);
        transitLayer.addAccessEgressTransfers(egressTransfers, false);

        // Temporary
        TIntIntMap accessTimeToReservedStop = new TIntIntHashMap();
        accessTimeToReservedStop.put(0, 0);
        TIntIntMap egressTimeFromReservedStop = new TIntIntHashMap();
        egressTimeFromReservedStop.put(1, 0);

        /**
         * Prepare transit search
         */

        LocalDate searchDate = Instant.ofEpochMilli(request.dateTime).atZone(ZoneId.systemDefault()).toLocalDate();
        int departureTime = Instant.ofEpochMilli(request.dateTime).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();

        RaptorWorkerTransitDataProvider transitData = new OtpRRDataProvider(
                transitLayer, searchDate, request.modes
        );

        McRaptorStateImpl stateImpl = new McRaptorStateImpl(
                transitLayer.getStopCount(),
                request.maxTransfers + 1,
                MAX_DURATION_SECONDS,
                departureTime //
        );

        RangeRaptorWorker worker = new RangeRaptorWorker(
                transitData,
                stateImpl.newWorkerState(),
                new PathBuilder(stateImpl),
                departureTime,
                departureTime + MAX_DURATION_SECONDS,
                (float)request.walkSpeed,
                1000,
                accessTimeToReservedStop,
                egressTimeFromReservedStop.keys()
        );

        /**
         * Route transit
         */

        worker.route();

        /**
         * Extract paths
         */

        ParetoSet<PathParetoSortableWrapper> paths = new ParetoSet<>(PathParetoSortableWrapper.paretoDominanceFunctions());

        for (Path[] pathArray : worker.pathsPerIteration) {
            for (Path path : pathArray ) {
                paths.add(new PathParetoSortableWrapper(path, 1000));
            }
        }

        /**
         * Create itineraries
         */

        ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, graph);

        Itinerary itinerary = itineraryMapper.createItinerary(request, paths.paretoSet().iterator().next().path);

        List<Itinerary> itineraries = new ArrayList<>();
        itineraries.add(itinerary);

        return itineraries;
    }
}
