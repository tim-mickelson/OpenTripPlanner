package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.model.FeedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.Path;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripSchedule;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
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
        this.transitLayer = transitLayer; this.graph = graph;
    }

    public Itinerary createItinerary(RoutingRequest request, Path path, TIntIntMap accessTimesToStops, TIntIntMap egressTimesToStops) {
        Itinerary itinerary = new Itinerary();
        if (path == null) {
            return null;
        }

        SimpleTransferGenerator simpleTransferGenerator = new SimpleTransferGenerator(graph, transitLayer);

        SimpleTransfer accessLegTransfer = simpleTransferGenerator.createSimpleTransfer(0, path.boardStops[0]);

        itinerary.walkDistance = 0.0;
        itinerary.transitTime = 0;
        itinerary.waitingTime = 0;

        // Access leg
        Leg accessLeg = new Leg();

        Stop firstStop = transitLayer.stopsByIndex[path.boardStops[0]];
        Stop lastStop = transitLayer.stopsByIndex[path.alightStops[path.alightStops.length - 1]];

        accessLeg.startTime = createCalendar(request.getDateTime(), (path.boardTimes[0] - (int)accessLegTransfer.getDistance()));
        accessLeg.endTime = createCalendar(request.getDateTime(), path.boardTimes[0]);
        accessLeg.from = new Place(request.from.lat, request.from.lng, request.from.name);
        accessLeg.from.stopId = lastStop.getId();
        accessLeg.to = new Place(request.to.lat, request.to.lng, request.to.name);
        accessLeg.to.stopId = firstStop.getId();
        accessLeg.mode = "WALK";
        accessLeg.legGeometry = PolylineEncoder.createEncodings(accessLegTransfer.getGeometry());

        accessLeg.distance = distanceMMToMeters((int)accessLegTransfer.getDistance());

        itinerary.addLeg(accessLeg);

        for (int i = 0; i < path.patterns.length; i++) {
            int boardStopIndex = path.boardStops[i];
            int alightStopIndex = path.alightStops[i];
            Stop boardStop = transitLayer.stopsByIndex[boardStopIndex];
            Stop alightStop = transitLayer.stopsByIndex[alightStopIndex];

            // Transfer leg if present
            if (i > 0 && path.transferTimes[i] != -1) {
                Stop previousAlightStop = transitLayer.stopsByIndex[path.alightStops[i - 1]];
                SimpleTransfer simpleTransfer = simpleTransferGenerator.createSimpleTransfer(boardStopIndex, alightStopIndex);
                Leg transferLeg = new Leg();
                transferLeg.startTime = createCalendar(request.getDateTime(), path.alightTimes[i - 1]);
                transferLeg.endTime = createCalendar(request.getDateTime(), path.alightTimes[i - 1] + path.transferTimes[i]);
                transferLeg.mode = "WALK";
                transferLeg.from = new Place(previousAlightStop.getLat(), previousAlightStop.getLon(), previousAlightStop.getName());
                transferLeg.to = new Place(boardStop.getLat(), boardStop.getLon(), boardStop.getName());
                transferLeg.legGeometry = PolylineEncoder.createEncodings(simpleTransfer.getGeometry());

                transferLeg.distance = distanceMMToMeters ((int)simpleTransfer.getDistance());

                itinerary.addLeg(transferLeg);
            }

            // Transit leg
            Leg transitLeg = new Leg();

            TripSchedule tripSchedule = transitLayer.getTripPatterns()[path.patterns[i]].tripSchedules.get(path.trips[i]);

            transitLeg.distance = 0.0;

            TripPattern tripPattern = transitLayer.tripPatternByIndex[path.patterns[i]];
            Route route = tripPattern.route;

            itinerary.transitTime += path.alightTimes[i] - path.boardTimes[i];
            itinerary.waitingTime += path.boardTimes[i] - path.transferTimes[i];

            transitLeg.from = new Place(boardStop.getLat(), boardStop.getLon(), boardStop.getName());
            transitLeg.from.stopId = new FeedId("RB", boardStop.getId().getId());
            transitLeg.from.stopIndex = path.boardStops[i];

            transitLeg.to = new Place(alightStop.getLat(), alightStop.getLon(), alightStop.getName());
            transitLeg.to.stopId = new FeedId("RB", alightStop.getId().getId());
            transitLeg.to.stopIndex = path.alightStops[i];

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
                if (!boarded && tripSchedule.departures[j] == path.boardTimes[i]) {
                    boarded = true;
                }
                if (boarded) {
                    transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                            tripPattern.stopPattern.stops[j].getLat() ));
                }
                if (boarded && tripSchedule.arrivals[j] == path.alightTimes[i]) {
                    break;
                }
            }

            transitLeg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);

            transitLeg.startTime = createCalendar(request.getDateTime(), path.boardTimes[i]);
            transitLeg.endTime = createCalendar(request.getDateTime(), path.alightTimes[i]);
            itinerary.addLeg(transitLeg);
        }

        // Egress leg

        SimpleTransfer egressLegTransfer = simpleTransferGenerator.createSimpleTransfer(path.alightStops[path.alightStops.length - 1], 1);

        Leg egressLeg = new Leg();
        egressLeg.startTime = createCalendar(request.getDateTime(), path.alightTimes[path.alightTimes.length - 1]);
        egressLeg.endTime = createCalendar(request.getDateTime(), path.alightTimes[path.alightTimes.length - 1] + (int)egressLegTransfer.getDistance());
        egressLeg.from = new Place(lastStop.getLat(), lastStop.getLon(), lastStop.getName());
        egressLeg.from.stopIndex = path.alightStops[path.length - 1];
        egressLeg.from.stopId = new FeedId("RB", lastStop.getId().getId());
        egressLeg.to = new Place(request.to.lat, request.to.lng, request.to.name);
        egressLeg.mode = "WALK";
        egressLeg.legGeometry = PolylineEncoder.createEncodings(egressLegTransfer.getGeometry());

        egressLeg.distance = distanceMMToMeters((int)egressLegTransfer.getDistance());

        itinerary.addLeg(egressLeg);

        itinerary.duration = (long) accessLegTransfer.getDistance() + (path.alightTimes[path.length - 1] - path.boardTimes[0]) + (long) egressLegTransfer.getDistance();
        itinerary.startTime = accessLeg.startTime;
        itinerary.endTime = egressLeg.endTime;

        itinerary.transfers = path.patterns.length - 1;
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
