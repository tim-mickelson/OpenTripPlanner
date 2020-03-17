package org.opentripplanner.ext.transmodelapi;

import com.google.common.base.Joiner;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.ext.transmodelapi.mapping.TransmodelMappingUtil;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.routing.RoutingResponse;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransmodelGraphQLPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

    private TransmodelMappingUtil mappingUtil;

    public TransmodelGraphQLPlanner(TransmodelMappingUtil mappingUtil) {
        this.mappingUtil = mappingUtil;
    }

    public PlanResponse plan(DataFetchingEnvironment environment) {
        PlanResponse response = new PlanResponse();
        RoutingRequest request = null;
        try {
            TransmodelRequestContext ctx = environment.getContext();
            Router router = ctx.getRouter();

            request = createRequest(environment);
            request.setRoutingContext(router.graph);

            RoutingResponse res = ctx.getRoutingService().route(request, router);

            response.plan = res.getTripPlan();
            response.metadata = res.getMetadata();

            if(response.plan.itineraries.isEmpty()) {
                response.messages.add(new PlannerError(new PathNotFoundException()).message);
            }
        }
        catch (Exception e) {
            try {
                PlannerError error = new PlannerError(e);
                if (!PlannerError.isPlanningError(e.getClass())) {
                    LOG.warn("Error while planning path: ", e);
                }
                response.messages.add(error.message);
                response.plan = TripPlanMapper.mapTripPlan(request, Collections.emptyList());
            }
            catch (RuntimeException ex) {
                LOG.error("Root cause: " + e.getMessage(), e);
                throw ex;
            }
        } finally {
            if (request != null && request.rctx != null) {
                response.debugOutput = request.rctx.debugOutput;
            }
        }
        return response;
    }

    private static <T> void call(Map<String, T> m, String name, Consumer<T> consumer) {
        if (!name.contains(".")) {
            if (hasArgument(m, name)) {
                T v = m.get(name);
                consumer.accept(v);
            }
        } else {
            String[] parts = name.split("\\.");
            if (hasArgument(m, parts[0])) {
                Map<String, T> nm = (Map<String, T>) m.get(parts[0]);
                call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
            }
        }
    }

    private static <T> void call(DataFetchingEnvironment environment, String name, Consumer<T> consumer) {
        if (!name.contains(".")) {
            if (hasArgument(environment, name)) {
                consumer.accept(environment.getArgument(name));
            }
        } else {
            String[] parts = name.split("\\.");
            if (hasArgument(environment, parts[0])) {
                Map<String, T> nm = (Map<String, T>) environment.getArgument(parts[0]);
                call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
            }
        }
    }

    private static class CallerWithEnvironment {
        private final DataFetchingEnvironment environment;

        public CallerWithEnvironment(DataFetchingEnvironment e) {
            this.environment = e;
        }

        private <T> void argument(String name, Consumer<T> consumer) {
            call(environment, name, consumer);
        }
    }

    private GenericLocation toGenericLocation(Map<String, Object> m) {
        Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
        Double lat = null;
        Double lon = null;
        if (coordinates != null) {
            lat = (Double) coordinates.get("latitude");
            lon = (Double) coordinates.get("longitude");
        }

        String placeRef = (String) m.get("place");
        FeedScopedId stopId = placeRef == null ? null : mappingUtil.fromIdString(placeRef);
        String name = (String) m.get("name");
        name = name == null ? "" : name;

        return new GenericLocation(name, stopId, lat, lon);
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        TransmodelRequestContext context = environment.getContext();
        Router router = context.getRouter();
        RoutingRequest request = router.defaultRoutingRequest.clone();

        TransmodelGraphQLPlanner.CallerWithEnvironment callWith = new TransmodelGraphQLPlanner.CallerWithEnvironment(environment);

        callWith.argument("locale", (String v) -> request.locale = Locale.forLanguageTag(v));

        callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
        callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

        if (hasArgument(environment, "dateTime")) {
            callWith.argument("dateTime", millisSinceEpoch -> request.setDateTime(new Date((long) millisSinceEpoch)));
        } else {
            request.setDateTime(new Date());
        }
        callWith.argument("searchWindow", (Integer m) -> request.searchWindow = Duration.ofMinutes(m));
        callWith.argument("wheelchair", request::setWheelchairAccessible);
        callWith.argument("numTripPatterns", request::setNumItineraries);
        callWith.argument("maximumWalkDistance", request::setMaxWalkDistance);
//        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
        callWith.argument("maxPreTransitTime", request::setMaxPreTransitTime);
//        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
//        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("walkReluctance", request::setWalkReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
//        callWith.argument("walkOnStreetReluctance", request::setWalkOnStreetReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
        callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
        callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);
//        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

        OptimizeType optimize = environment.getArgument("optimize");

        if (optimize == OptimizeType.TRIANGLE) {
            try {
                request.assertTriangleParameters(request.bikeTriangleSafetyFactor, request.bikeTriangleTimeFactor, request.bikeTriangleSlopeFactor);
                callWith.argument("triangle.safetyFactor", request::setBikeTriangleSafetyFactor);
                callWith.argument("triangle.slopeFactor", request::setBikeTriangleSlopeFactor);
                callWith.argument("triangle.timeFactor", request::setBikeTriangleTimeFactor);
            } catch (ParameterException e) {
                throw new RuntimeException(e);
            }
        }

        callWith.argument("arriveBy", request::setArriveBy);
        request.showIntermediateStops = true;
        callWith.argument("vias", (List<Map<String, Object>> v) -> request.intermediatePlaces = v.stream().map(this::toGenericLocation).collect(Collectors.toList()));
//        callWith.argument("preferred.lines", lines -> request.setPreferredRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("preferred.otherThanPreferredLinesPenalty", request::setOtherThanPreferredRoutesPenalty);
        // Deprecated organisations -> authorities
        callWith.argument("preferred.organisations", (Collection<String> organisations) -> request.setPreferredAgencies(mappingUtil.mapCollectionOfValues(organisations, in -> in)));
        callWith.argument("preferred.authorities", (Collection<String> authorities) -> request.setPreferredAgencies(mappingUtil.mapCollectionOfValues(authorities, in -> in)));
//        callWith.argument("unpreferred.lines", lines -> request.setUnpreferredRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("unpreferred.organisations", (Collection<String> organisations) -> request.setUnpreferredAgencies(mappingUtil.mapCollectionOfValues(organisations, in -> in)));
        callWith.argument("unpreferred.authorities", (Collection<String> authorities) -> request.setUnpreferredAgencies(mappingUtil.mapCollectionOfValues(authorities, in -> in)));

        callWith.argument("banned.lines", lines -> request.setBannedRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("banned.organisations", (Collection<String> organisations) -> request.setBannedAgencies(mappingUtil.mapCollectionOfValues(organisations, in -> in)));
        callWith.argument("banned.authorities", (Collection<String> authorities) -> request.setBannedAgencies(mappingUtil.mapCollectionOfValues(authorities, in -> in)));
        callWith.argument("banned.serviceJourneys", (Collection<String> serviceJourneys) -> request.bannedTrips = toBannedTrips(serviceJourneys));

//        callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
//        callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));

        callWith.argument("whiteListed.lines", (List<String> lines) -> request.setWhiteListedRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("whiteListed.organisations", (Collection<String> organisations) -> request.setWhiteListedAgencies(mappingUtil.mapCollectionOfValues(organisations, in -> in)));
        callWith.argument("whiteListed.authorities", (Collection<String> authorities) -> request.setWhiteListedAgencies(mappingUtil.mapCollectionOfValues(authorities, in -> in)));

        //callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
        // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
        //callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);
        callWith.argument("allowBikeRental", (Boolean v) -> request.allowBikeRental = v);
        callWith.argument("debugItineraryFilter", (Boolean v) -> request.debugItineraryFilter = v);

        callWith.argument("transferPenalty", (Integer v) -> request.transferPenalty = v);

        //callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
        //callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

        if (optimize == OptimizeType.TRANSFERS) {
            optimize = OptimizeType.QUICK;
            request.transferPenalty += 1800;
        }

        if (optimize != null) {
            request.optimize = optimize;
        }

        if (hasArgument(environment, "modes")) {
            // Map modes to comma separated list in string first to be able to reuse logic in QualifiedModeSet
            // Remove CABLE_CAR from collection because QualifiedModeSet does not support mapping (splits on '_')
            Set<TraverseMode> modes = new HashSet<>(environment.getArgument("modes"));
            boolean cableCar = modes.remove(TraverseMode.CABLE_CAR);

            String modesAsString = modes.isEmpty() ? "" : Joiner.on(",").join(modes);
            if (!StringUtils.isEmpty(modesAsString)) {
                new QualifiedModeSet(modesAsString).applyToRoutingRequest(request);
                request.setModes(request.modes);
            } else if (cableCar) {
                // Clear default modes in case only cable car is selected
                request.clearModes();
            }

            // Apply cable car setting 
            request.modes.setCableCar(cableCar);
        }

        List<Map<String, ?>> transportSubmodeFilters = environment.getArgument("transportSubmodes");
        if (transportSubmodeFilters != null) {
            request.transportSubmodes = new HashMap<>();
            for (Map<String, ?> transportSubmodeFilter : transportSubmodeFilters) {
                TraverseMode transportMode = (TraverseMode) transportSubmodeFilter.get("transportMode");
                List<TransmodelTransportSubmode> transportSubmodes = (List<TransmodelTransportSubmode>) transportSubmodeFilter.get("transportSubmodes");
                if (!CollectionUtils.isEmpty(transportSubmodes)) {
                    request.transportSubmodes.put(transportMode, new HashSet<>(transportSubmodes));
                }
            }
        }

        if (request.allowBikeRental && !hasArgument(environment, "bikeSpeed")) {
            //slower bike speed for bike sharing, based on empirical evidence from DC.
            request.bikeSpeed = 4.3;
        }

        callWith.argument("minimumTransferTime", (Integer v) -> request.transferSlack = v);
        assertSlack(request);

        callWith.argument("maximumTransfers", (Integer v) -> request.maxTransfers = v);

        final long NOW_THRESHOLD_MILLIS = 15 * 60 * 60 * 1000;
        boolean tripPlannedForNow = Math.abs(request.getDateTime().getTime() - new Date().getTime()) < NOW_THRESHOLD_MILLIS;
        request.useBikeRentalAvailabilityInformation = (tripPlannedForNow); // TODO the same thing for GTFS-RT


        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        //callWith.argument("includePlannedCancellations", (Boolean v) -> request.includePlannedCancellations = v);
        //callWith.argument("ignoreInterchanges", (Boolean v) -> request.ignoreInterchanges = v);

        /*
        if (!request.modes.isTransit() && request.modes.getCar()) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.kissAndRide) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.rideAndKiss) {
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.parkAndRide) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.useFlexService) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        }
         */

        return request;
    }

    public void assertSlack(RoutingRequest r) {
        if (r.boardSlack + r.alightSlack > r.transferSlack) {
            // TODO thrown exception type is not consistent with assertTriangleParameters
            throw new RuntimeException("Invalid parameters: " +
                    "transfer slack must be greater than or equal to board slack plus alight slack");
        }
    }


    private HashMap<FeedScopedId, BannedStopSet> toBannedTrips(Collection<String> serviceJourneyIds) {
        Map<FeedScopedId, BannedStopSet> bannedTrips = serviceJourneyIds.stream().map(mappingUtil::fromIdString).collect(Collectors.toMap(Function.identity(), id -> BannedStopSet.ALL));
        return new HashMap<>(bannedTrips);
    }

    /*
    private String getLocationOfFirstQuay(String vertexId, GraphIndex graphIndex) {
        // TODO THIS DOES NOT WORK !!
        Vertex vertex = graphIndex.stopVertexForStop.get(vertexId);
        if (vertex instanceof TransitStopVertex) {
            TransitStopVertex stopVertex = (TransitStopVertex) vertex;

            FeedScopedId stopId = stopVertex.getStop().getId();

            return stopId.getFeedId().concat(":").concat(stopId.getId());
        } else {
            return vertexId;
        }
    }
    */

    public static boolean hasArgument(DataFetchingEnvironment environment, String name) {
        return environment.containsArgument(name) && environment.getArgument(name) != null;
    }

    public static <T> boolean hasArgument(Map<String, T> m, String name) {
        return m.containsKey(name) && m.get(name) != null;
    }
}