package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.api.model.*;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import com.conveyal.r5.profile.entur.api.Path2;
import com.conveyal.r5.profile.entur.api.PathLeg;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripScheduleImpl;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.PolylineEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class ItineraryMapper {
    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    public ItineraryMapper(TransitLayer transitLayer, RoutingRequest request) {
        this.transitLayer = transitLayer;
        this.request = request;
    }

    public Itinerary createItinerary(RoutingRequest request, Path2<TripSchedule> path, Map<Stop, Transfer> accessPaths, Map<Stop, Transfer> egressPaths) {
        Itinerary itinerary = new Itinerary();

        // Map access leg
        Stop accessToStop = transitLayer.getStopByIndex(path.accessLeg().toStop());
        Transfer accessPath = accessPaths.get(accessToStop);
        Leg accessLeg = new Leg();
        accessLeg.stop = new ArrayList<>();
        accessLeg.startTime = createCalendar(path.accessLeg().fromTime());
        accessLeg.endTime = createCalendar(path.accessLeg().toTime());
        accessLeg.mode = "WALK";
        if (request.rctx.fromVertex instanceof TransitVertex) {
            accessLeg.from = new Place(request.rctx.fromVertex.getLon(), request.rctx.fromVertex.getLat(), request.rctx.fromVertex.getName());
            accessLeg.from.stopId = ((TransitVertex) request.rctx.fromVertex).getStopId();
            accessLeg.from.vertexType = VertexType.TRANSIT;
        }
        else {
            accessLeg.from = new Place(request.from.lng, request.from.lat, "Coordinate");
        }
        accessLeg.to = new Place(accessToStop.getLon(), accessToStop.getLat(), accessToStop.getName());
        accessLeg.to.stopId = accessToStop.getId();
        accessLeg.to.vertexType = VertexType.TRANSIT;
        accessLeg.legGeometry = PolylineEncoder.createEncodings(accessPath.getCoordinates());
        accessLeg.distance = distanceMMToMeters(accessPath.getDistance());
        accessLeg.walkSteps = new ArrayList<>(); //TODO: Add walk steps test
        itinerary.walkDistance += accessLeg.distance;

        if (accessLeg.distance > 0) {
            itinerary.addLeg(accessLeg);
        }

        // Increment counters
        itinerary.walkTime += path.accessLeg().toTime() - path.accessLeg().fromTime();

        // TODO: Add back this code when PathLeg interface contains object references


        for (PathLeg<TripSchedule> pathLeg : path.legs()) {
            // Map transit leg
            if (pathLeg.isTransit()) {
                Stop boardStop = transitLayer.getStopByIndex(pathLeg.fromStop());
                Stop alightStop = transitLayer.getStopByIndex(pathLeg.toStop());


                Trip trip = pathLeg.trip().getOriginalTrip();
                TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
                Route route = tripPattern.route;

                Leg transitLeg = new Leg();
                transitLeg.serviceDate = new ServiceDate(request.getDateTime()).getAsString(); // TODO: This has to be changed for multi-day searches
                transitLeg.stop = new ArrayList<>();
                transitLeg.startTime = createCalendar(pathLeg.fromTime());
                transitLeg.endTime = createCalendar(pathLeg.toTime());
                transitLeg.mode = tripPattern.mode.toString();
                transitLeg.tripId = trip.getId();
                transitLeg.from = new Place(boardStop.getLon(), boardStop.getLat(), boardStop.getName());
                transitLeg.from.stopId = boardStop.getId();
                transitLeg.from.vertexType = VertexType.TRANSIT;
                transitLeg.to = new Place(alightStop.getLon(), alightStop.getLat(), alightStop.getName());
                transitLeg.to.stopId = alightStop.getId();
                transitLeg.to.vertexType = VertexType.TRANSIT;
                List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(pathLeg, alightStop, boardStop);
                transitLeg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
                transitLeg.distance = getDistanceFromCoordinates(transitLegCoordinates);
                transitLeg.route = route.getLongName();
                transitLeg.routeId = route.getId();
                transitLeg.agencyName = route.getAgency().getName();
                transitLeg.routeColor = route.getColor();
                transitLeg.tripShortName = route.getShortName();
                transitLeg.agencyId = route.getAgency().getId();
                transitLeg.routeShortName = route.getShortName();
                transitLeg.routeLongName = route.getLongName();
                transitLeg.walkSteps = new ArrayList<>();
                itinerary.addLeg(transitLeg);

                // Increment counters
                itinerary.transitTime += pathLeg.toTime() - pathLeg.fromTime();
            }

            // Map transfer leg
            if (pathLeg.isTransfer()) {
                Stop transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
                Stop transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
                Transfer transfer = transitLayer.getTransfer(pathLeg.fromStop(), pathLeg.toStop());

                Leg transferLeg = new Leg();
                transferLeg.stop = new ArrayList<>(); // TODO: Map intermediate stops
                transferLeg.startTime = createCalendar(pathLeg.fromTime());
                transferLeg.endTime = createCalendar(pathLeg.toTime());
                transferLeg.mode = "WALK";
                transferLeg.from = new Place(transferFromStop.getLon(), transferFromStop.getLat(), transferFromStop.getName());
                transferLeg.from.stopId = transferFromStop.getId();
                transferLeg.from.vertexType = VertexType.TRANSIT;
                transferLeg.to = new Place(transferToStop.getLon(), transferToStop.getLat(), transferToStop.getName());
                transferLeg.to.stopId = transferToStop.getId();
                transferLeg.to.vertexType = VertexType.TRANSIT;
                transferLeg.legGeometry = PolylineEncoder.createEncodings(transfer.getCoordinates());
                transferLeg.distance = distanceMMToMeters(transfer.getDistance());
                transferLeg.walkSteps = new ArrayList<>(); //TODO: Add walk steps
                itinerary.walkDistance += transferLeg.distance;

                if (transferLeg.distance > 0) {
                    itinerary.addLeg(transferLeg);
                }

                // Increment counters
                itinerary.transfers++;
                itinerary.walkTime += pathLeg.toTime() - pathLeg.fromTime();
            }
        }


        // Map egress leg
        Stop egressStop = transitLayer.getStopByIndex(path.egressLeg().fromStop());
        Transfer egressPath = egressPaths.get(egressStop);
        Leg egressLeg = new Leg();
        egressLeg.stop = new ArrayList<>();
        egressLeg.startTime = createCalendar(path.egressLeg().fromTime());

        egressLeg.endTime = createCalendar(path.egressLeg().toTime());
        egressLeg.mode = "WALK";
        egressLeg.from = new Place(egressStop.getLon(), egressStop.getLat(), egressStop.getName());
        egressLeg.from.stopId = egressStop.getId();
        egressLeg.from.vertexType = VertexType.TRANSIT;
        if (request.rctx.toVertex instanceof TransitVertex) {
            egressLeg.to = new Place(request.rctx.toVertex.getLon(), request.rctx.toVertex.getLat(), request.rctx.toVertex.getName());
            egressLeg.to.stopId = ((TransitVertex) request.rctx.toVertex).getStopId();
            egressLeg.to.vertexType = VertexType.TRANSIT;
        }
        else {
            egressLeg.to = new Place(request.to.lng, request.to.lat, "Coordinate");
        }
        egressLeg.legGeometry = PolylineEncoder.createEncodings(egressPath.getCoordinates());
        egressLeg.distance = distanceMMToMeters(egressPath.getDistance());
        egressLeg.walkSteps = new ArrayList<>(); //TODO: Add walk steps
        itinerary.walkDistance = egressLeg.distance;
        if (egressLeg.distance > 0) {
            itinerary.addLeg(egressLeg);
        }

        // Increment counters
        itinerary.walkTime += path.egressLeg().toTime() - path.egressLeg().fromTime();

        // Map general itinerary fields
        itinerary.startTime = createCalendar(path.accessLeg().fromTime());
        itinerary.endTime = createCalendar(path.egressLeg().toTime());
        itinerary.duration = (long) path.egressLeg().toTime() - path.accessLeg().fromTime();
        itinerary.waitingTime = itinerary.duration - itinerary.walkTime - itinerary.transitTime;
        itinerary.distance = itinerary.legs.stream().mapToDouble(l -> l.distance).sum();

        return itinerary;
    }

    public TripPlan createTripPlan(RoutingRequest request, List<Itinerary> itineraries) {
        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        itineraries = itineraries.stream().sorted((i1, i2) -> i1.endTime.compareTo(i2.endTime))
                .limit(request.numItineraries).collect(Collectors.toList());
        tripPlan.itinerary = itineraries;
        return tripPlan;
    }

    private Calendar createCalendar(int timeinSeconds) {
        Date date = request.getDateTime();
        LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo")); // TODO: Get time zone from request
        calendar.set(localDate.getYear(), localDate.getMonth().getValue() - 1, localDate.getDayOfMonth()
                , 0, 0, 0);
        calendar.add(Calendar.SECOND, timeinSeconds);
        return calendar;
    }

    private double distanceMMToMeters(int distanceMm) {
        return (double) (distanceMm / 1000);
    }

    private List<Coordinate> extractTransitLegCoordinates(PathLeg<TripSchedule> pathLeg, Stop boardStop, Stop alightStop) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
            if (boarded) {
                transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                        tripPattern.stopPattern.stops[j].getLat()));
            }
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
                break;
            }
        }
        return transitLegCoordinates;
    }

    private double getDistanceFromCoordinates(List<Coordinate> coordinates) {
        double distance = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            distance += SphericalDistanceLibrary.distance(coordinates.get(i), coordinates.get(i - 1));
        }
        return distance;
    }
}
