package org.opentripplanner.routing.algorithm.raptor;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.street_router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.OtpRRDataProvider;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.RaptorWorkerTransitDataProvider;
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

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class RaptorTravelSearch {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorTravelSearch.class);

    private final OtpRRDataProvider otpRRDataProvider;
    private final Graph graph;

    public RaptorTravelSearch(OtpRRDataProvider otpRRDataProvider, Graph graph) {
        this.otpRRDataProvider = otpRRDataProvider;
        this.graph = graph;
    }

    private List<Itinerary> search(RoutingRequest request) {
            TransitLayer transitLayer = otpRRDataProvider.getTransitLayer();

            TObjectDoubleMap<Vertex> accessTimesToStopVertices =
                AccessEgressRouter.streetSearch(request, false, Integer.MAX_VALUE);

            TObjectDoubleMap<Vertex> egressTimeToStopVertices =
                AccessEgressRouter.streetSearch(request, true, Integer.MAX_VALUE);

            TIntIntMap accessTimeToStops = AccessEgressMapper.map(accessTimesToStopVertices, transitLayer.indexByStop);

            TIntIntMap egressTimesFromStops = AccessEgressMapper.map(accessTimesToStopVertices, transitLayer.indexByStop);

            LocalDate fromTime = Instant.ofEpochMilli(request.dateTime).atZone(ZoneId.systemDefault()).toLocalDate();

            RaptorWorkerTransitDataProvider transitData = new OtpRRDataProvider(
                    transitLayer, fromTime, request.modes
            );

            McRaptorStateImpl stateImpl = new McRaptorStateImpl(
                    transitLayer.getStopCount(),
                    request.maxTransfers + 1,
                    8 * 60, // hardcoded
                    100 // TODO: fix
            );

            RangeRaptorWorker worker = new RangeRaptorWorker(
                    transitData,
                    stateImpl.newWorkerState(),
                    new PathBuilder(stateImpl),
                    (int)request.dateTime,
                    (int)request.dateTime,
                    (float)request.walkSpeed,
                    1000,
                    accessTimeToStops,
                    egressTimesFromStops.keys()
            );

            worker.route();

            ParetoSet<PathParetoSortableWrapper> paths = new ParetoSet<>(PathParetoSortableWrapper.paretoDominanceFunctions());

            for (Path[] pathArray : worker.pathsPerIteration) {
                for (Path path : pathArray ) {
                    paths.add(new PathParetoSortableWrapper(path, 1000));
                }
            }

            ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, graph);

            Itinerary itinerary = itineraryMapper.createItinerary(request, paths.paretoSet().iterator().next().path, accessTimeToStops, egressTimesFromStops);

            List<Itinerary> itineraries = new ArrayList<>();
            itineraries.add(itinerary);

            return itineraries;
    }
}
