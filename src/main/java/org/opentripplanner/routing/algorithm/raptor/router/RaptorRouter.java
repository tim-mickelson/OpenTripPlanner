package org.opentripplanner.routing.algorithm.raptor.router;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor_old.Path;
import org.opentripplanner.routing.algorithm.raptor_old.PathBuilderCursorBased;
import org.opentripplanner.routing.algorithm.raptor_old.PathParetoSortableWrapper;
import org.opentripplanner.routing.algorithm.raptor_old.RangeRaptorWorker;
import org.opentripplanner.routing.algorithm.raptor_old.RangeRaptorWorkerState;
import org.opentripplanner.routing.algorithm.raptor_old.RangeRaptorWorkerStateImpl;
import org.opentripplanner.routing.algorithm.raptor_old.StopStateCollection;
import org.opentripplanner.routing.algorithm.raptor_old.StopStatesIntArray;
import org.opentripplanner.routing.algorithm.raptor_old.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor_old.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.RaptorWorkerTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptor_old.util.ParetoSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    private final OtpRRDataProvider otpRRDataProvider;
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
        TransitLayer transitLayer = otpRRDataProvider.getTransitLayer();

        /**
         * Prepare access/egress transfers
         */

        Map<Vertex, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);
        Map<Vertex, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

        Stop accessStop = new Stop();
        accessStop.setName("Access stop");
        accessStop.setLat(request.from.lat);
        accessStop.setLon(request.from.lng);

        Stop egressStop = new Stop();
        egressStop.setName("Egress stop");
        egressStop.setLat(request.from.lat);
        egressStop.setLon(request.from.lng);

        transitLayer.setAccessEgressStops(accessStop, egressStop);

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

        int departureTime = Instant.ofEpochMilli(request.dateTime*1000).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();

        RaptorWorkerTransitDataProvider transitData = this.otpRRDataProvider;

        StopStateCollection stopStatesIntArray = new StopStatesIntArray(10, transitLayer.getStopCount());

        RangeRaptorWorkerState stateImpl = new RangeRaptorWorkerStateImpl(
                request.maxTransfers + 5,
                transitLayer.getStopCount(),
                departureTime,
                MAX_DURATION_SECONDS,
                new StopStatesIntArray(10, transitLayer.getStopCount())
        );

        RangeRaptorWorker worker = new RangeRaptorWorker(
                transitData,
                stateImpl,
                new PathBuilderCursorBased(stopStatesIntArray.newCursor()),
                departureTime,
                departureTime + 60,
                (float)request.walkSpeed,
                23,
                accessTimeToReservedStop,
                egressTimeFromReservedStop.keys()
        );

        /**
         * Route transit
         */

        Collection<Path> workerPaths = worker.route();

        /**
         * Extract paths
         */

        ParetoSet<PathParetoSortableWrapper> paths = new ParetoSet<>(PathParetoSortableWrapper.paretoDominanceFunctions());

            for (Path path : workerPaths ) {
                if (path != null) {
                    paths.add(new PathParetoSortableWrapper(path, 1000));
                }
            }

        /**
         * Create itineraries
         */

        ItineraryMapper itineraryMapper = new ItineraryMapper(otpRRDataProvider, transitLayer, graph);
        List<Itinerary> itineraries = new ArrayList<>();

        Iterator<PathParetoSortableWrapper> iterator = paths.paretoSet().iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next().path;
            Itinerary itinerary = itineraryMapper.createItinerary(request, path);
            itineraries.add(itinerary);
        }

        TripPlan tripPlan = new TripPlan();
        tripPlan.itinerary = itineraries;

        return tripPlan;
    }
}
