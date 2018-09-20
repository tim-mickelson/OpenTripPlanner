package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Path2;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.PathLeg;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripSchedule;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.util.PolylineEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class ItineraryMapper {
    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    public ItineraryMapper(TransitLayer transitLayer, RoutingRequest request) {
        this.transitLayer = transitLayer;
        this.request = request;
    }

    public Itinerary createItinerary(RoutingRequest request, Path2 path, Map<Stop, Transfer> accessPaths, Map<Stop, Transfer> egressPaths) {
        Itinerary itinerary = new Itinerary();

        // Map access leg
        Stop accessStop = transitLayer.getStopByIndex(path.accessLeg().toStop());
        Transfer accessPath = accessPaths.get(accessStop);
        Leg accessLeg = new Leg();
        accessLeg.startTime = createCalendar(path.accessLeg().fromTime());
        accessLeg.endTime = createCalendar(path.accessLeg().toTime());
        accessLeg.mode = "WALK";
        accessLeg.from = new Place(request.from.lat, request.from.lng, "Coordinate");
        accessLeg.to = new Place(accessStop.getLat(), accessStop.getLon(), accessStop.getName());
        accessLeg.legGeometry = PolylineEncoder.createEncodings(accessPath.coordinates);
        accessLeg.distance = distanceMMToMeters(accessPath.distance);
        itinerary.addLeg(accessLeg);

        // Increment counters
        itinerary.walkTime += path.accessLeg().toTime() - path.accessLeg().fromTime();

        for (PathLeg pathLeg : path.legs()) {
            // Map transit leg
            if (pathLeg.isTransit()) {
                Stop boardStop = transitLayer.getStopByIndex(pathLeg.fromStop());
                Stop alightStop = transitLayer.getStopByIndex(pathLeg.toStop());

                org.opentripplanner.routing.algorithm.raptor.transit_layer.TripPattern raptorTripPattern = transitLayer.getTripPatterns().get(pathLeg.pattern());
                TripSchedule tripSchedule = transitLayer.tripPatterns.get(pathLeg.pattern()).tripSchedules.get(pathLeg.trip());
                TripPattern tripPattern = transitLayer.getTripPatternByIndex(pathLeg.pattern());
                Route route = tripPattern.route;

                Leg transitLeg = new Leg();
                transitLeg.startTime = createCalendar(pathLeg.fromTime());
                transitLeg.endTime = createCalendar(pathLeg.toTime());
                transitLeg.mode = tripPattern.mode.toString();
                transitLeg.from = new Place(boardStop.getLat(), boardStop.getLon(), boardStop.getName());
                transitLeg.from.stopId = boardStop.getId();
                transitLeg.to = new Place(alightStop.getLat(), alightStop.getLon(), alightStop.getName());
                transitLeg.to.stopId = alightStop.getId();
                Collection<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(tripSchedule, tripPattern, raptorTripPattern, pathLeg);
                transitLeg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
                transitLeg.distance = (double)transitLeg.legGeometry.getLength();
                transitLeg.route = route.getLongName();
                transitLeg.agencyName = route.getAgency().getName();
                transitLeg.routeColor = route.getColor();
                transitLeg.tripShortName = route.getShortName();
                transitLeg.agencyId = route.getId().getAgencyId();
                transitLeg.routeShortName = route.getShortName();
                transitLeg.routeLongName = route.getLongName();
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
                transferLeg.startTime = createCalendar(pathLeg.fromTime());
                transferLeg.endTime = createCalendar(pathLeg.toTime());
                transferLeg.mode = "WALK";
                transferLeg.from = new Place(transferFromStop.getLat(), transferFromStop.getLon(), transferFromStop.getName());
                transferLeg.from.stopId = transferFromStop.getId();
                transferLeg.to = new Place(transferToStop.getLat(), transferToStop.getLon(), transferToStop.getName());
                transferLeg.to.stopId = transferToStop.getId();
                transferLeg.legGeometry = PolylineEncoder.createEncodings(transfer.coordinates);
                transferLeg.distance = distanceMMToMeters(transfer.distance);

                itinerary.addLeg(transferLeg);

                // Increment counters
                itinerary.transfers++;
                itinerary.walkTime += pathLeg.toTime() - pathLeg.fromTime();
            }
        }

        // Map egress leg
        Stop egressStop = transitLayer.getStopByIndex(path.egressLeg().fromStop());
        Transfer egressPath = egressPaths.get(egressStop);
        Leg egressLeg = new Leg();
        egressLeg.startTime = createCalendar(path.egressLeg().fromTime());

        egressLeg.endTime = createCalendar(path.egressLeg().toTime());
        egressLeg.mode = "WALK";
        egressLeg.from = new Place(egressStop.getLat(), egressStop.getLon(), egressStop.getName());
        egressLeg.to = new Place(request.to.lat, request.to.lng, "Coordinate");
        egressLeg.legGeometry = PolylineEncoder.createEncodings(egressPath.coordinates);
        egressLeg.distance = distanceMMToMeters(egressPath.distance);
        itinerary.addLeg(egressLeg);

        // Increment counters
        itinerary.walkTime += path.egressLeg().toTime() - path.egressLeg().fromTime();

        // Map general itinerary fields
        itinerary.startTime = createCalendar(path.accessLeg().fromTime());
        itinerary.endTime = createCalendar(path.egressLeg().toTime());
        itinerary.duration = (long) path.egressLeg().toTime() - path.accessLeg().fromTime();

        return itinerary;
    }

    private Calendar createCalendar(int timeinSeconds) {
        Date date = request.getDateTime();
        LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo"));
        calendar.set(localDate.getYear(), localDate.getMonth().getValue(), localDate.getDayOfMonth()
                , 0, 0, 0);
        calendar.add(Calendar.SECOND, timeinSeconds);
        return calendar;
    }

    private double distanceMMToMeters(int distanceMm) {
        return (double) (distanceMm / 1000);
    }

    private Collection<Coordinate> extractTransitLegCoordinates(TripSchedule tripSchedule, TripPattern tripPattern, org.opentripplanner.routing.algorithm.raptor.transit_layer.TripPattern raptorTripPattern, PathLeg pathLeg) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (!boarded && tripSchedule.departures[j] == pathLeg.fromTime() && raptorTripPattern.stopPattern[j] == pathLeg.fromStop()) {
                boarded = true;
            }
            if (boarded) {
                transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                        tripPattern.stopPattern.stops[j].getLat()));
            }
            if (boarded && tripSchedule.arrivals[j] == pathLeg.toTime() && raptorTripPattern.stopPattern[j] == pathLeg.toStop()) {
                break;
            }
        }
        return transitLegCoordinates;
    }
}
