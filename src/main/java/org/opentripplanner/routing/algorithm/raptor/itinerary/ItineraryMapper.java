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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.PolylineEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ItineraryMapper {
    private final TransitLayer transitLayer;

    private final Graph graph;

    public ItineraryMapper(TransitLayer transitLayer, Graph graph) {
        this.transitLayer = transitLayer;
        this.graph = graph;
    }

    public Itinerary createItinerary(RoutingRequest request, Path2 path) {
        if (path == null) {
            return null;
        }

        Itinerary itinerary = new Itinerary();

        PathLeg accessPathLeg = path.accessLeg();
        PathLeg egressPathLeg = path.accessLeg();

        int startTimeSeconds = accessPathLeg.fromTime();
        itinerary.startTime = createCalendar(request.getDateTime(), startTimeSeconds);
        itinerary.endTime = createCalendar(request.getDateTime(), egressPathLeg.toTime());
        itinerary.duration = (long) egressPathLeg.toTime() - accessPathLeg.fromTime();
        int walkingTime = 0;

        itinerary.transfers = 0; // TODO count

        for (PathLeg pathLeg : path.legs()) {
            // Get stops for transit leg
            int boardStopIndex = pathLeg.fromStop();
            int alightStopIndex = pathLeg.toStop();
            Stop boardStop = transitLayer.getStopByIndex(boardStopIndex);
            Stop alightStop = transitLayer.getStopByIndex(alightStopIndex);

            // Create transit leg if present
            if (pathLeg.isTransit()) {
                Leg transitLeg = new Leg();

                int patternIndex = pathLeg.pattern();

                TripSchedule tripSchedule = transitLayer.getTripPatterns()[patternIndex].tripSchedules.get(pathLeg.trip());
                TripPattern tripPattern = transitLayer.getTripPatternByIndex(patternIndex);
                Route route = tripPattern.route;

                itinerary.transitTime += pathLeg.toTime() - pathLeg.fromTime();

                transitLeg.from = new Place(boardStop.getLat(), boardStop.getLon(), boardStop.getName());
                transitLeg.from.stopId = boardStop.getId();

                transitLeg.to = new Place(alightStop.getLat(), alightStop.getLon(), alightStop.getName());
                transitLeg.to.stopId = alightStop.getId();

                transitLeg.route = route.getLongName();
                transitLeg.agencyName = route.getAgency().getName();
                transitLeg.routeColor = route.getColor();
                transitLeg.tripShortName = route.getShortName();
                transitLeg.agencyId = route.getId().getAgencyId();
                transitLeg.routeShortName = route.getShortName();
                transitLeg.routeLongName = route.getLongName();
                transitLeg.mode = tripPattern.mode.toString();

                List<Coordinate> transitLegCoordinates = new ArrayList<>();
                boolean boarded = false;
                for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
                    if (!boarded && tripSchedule.departures[j] == pathLeg.fromTime() && pathLeg.fromStop() == j) {
                        boarded = true;
                    }
                    if (boarded) {
                        transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                                tripPattern.stopPattern.stops[j].getLat()));
                    }
                    if (boarded && tripSchedule.arrivals[j] == pathLeg.toTime() && pathLeg.toStop() == j) {
                        break;
                    }
                }

                transitLeg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
                transitLeg.startTime = createCalendar(request.getDateTime(), pathLeg.fromTime());
                transitLeg.endTime = createCalendar(request.getDateTime(), pathLeg.toTime());
                itinerary.addLeg(transitLeg);
            }

            // Get stops for transfer leg


            // Create transfer leg if present
            if (pathLeg.isTransfer()) {
                int transferFromIndex = pathLeg.fromStop();
                int transferToIndex = pathLeg.toStop();
                Stop transferFromStop = transitLayer.getStopByIndex(transferFromIndex);
                Stop transferToStop = transitLayer.getStopByIndex(transferToIndex);

                Transfer transfer = transitLayer.getTransfer(transferFromIndex, transferToIndex);
                Leg transferLeg = new Leg();
                transferLeg.startTime = createCalendar(request.getDateTime(), pathLeg.fromTime());
                transferLeg.endTime = createCalendar(request.getDateTime(), pathLeg.toTime());
                transferLeg.mode = "WALK";
                transferLeg.from = new Place(transferFromStop.getLat(), transferFromStop.getLon(), transferFromStop.getName());
                transferLeg.to = new Place(transferToStop.getLat(), transferToStop.getLon(), transferToStop.getName());
                transferLeg.legGeometry = PolylineEncoder.createEncodings(transfer.coordinates);

                transferLeg.distance = distanceMMToMeters(transfer.distance);

                itinerary.addLeg(transferLeg);
            }
        }

        itinerary.walkTime = walkingTime;
        itinerary.walkDistance = walkingTime / request.walkSpeed;

        return itinerary;
    }

    private Calendar createCalendar(Date date, int timeinSeconds) {
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
}
