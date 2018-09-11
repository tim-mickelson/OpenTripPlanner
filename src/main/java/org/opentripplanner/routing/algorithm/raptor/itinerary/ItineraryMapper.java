package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor_old.Path;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.RaptorWorkerTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor_old.transit_layer.TripSchedule;
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
    private final RaptorWorkerTransitDataProvider raptorWorkerTransitDataProvider;
    private final TransitLayer transitLayer;

    private final Graph graph;

    public ItineraryMapper(RaptorWorkerTransitDataProvider raptorWorkerTransitDataProvider, TransitLayer transitLayer, Graph graph) {
        this.raptorWorkerTransitDataProvider = raptorWorkerTransitDataProvider;
        this.transitLayer = transitLayer;
        this.graph = graph;
    }

    public Itinerary createItinerary(RoutingRequest request, Path path) {
        Itinerary itinerary = new Itinerary();
        if (path == null) {
            return null;
        }

        int walkSpeedMillimetersPerSecond = (int)(request.walkSpeed * 1000);
        int startTimeSeconds = path.boardTimes[1] - transitLayer.getTransfer(0, path.boardStops[1]).distance / walkSpeedMillimetersPerSecond;
        itinerary.startTime = createCalendar(request.getDateTime(), startTimeSeconds);
        itinerary.endTime = createCalendar(request.getDateTime(), path.transferTimes[path.transferTimes.length - 1]);
        itinerary.duration = (long)path.transferTimes[path.transferTimes.length - 1] - startTimeSeconds;
        int walkingTime = 0;

        itinerary.transfers = path.patterns.length - 1;

        for (int i = 0; i < path.patterns.length; i++) {
            // Get stops for transit leg
            int boardStopIndex = path.boardStops[i];
            int alightStopIndex = path.alightStops[i];
            Stop boardStop = transitLayer.getStopByIndex(boardStopIndex);
            Stop alightStop = transitLayer.getStopByIndex(alightStopIndex);

            // Create transit leg if present
            if (boardStopIndex != -1) {
                Leg transitLeg = new Leg();

                int patternIndex = raptorWorkerTransitDataProvider.getScheduledIndexForOriginalPatternIndex()[path.patterns[i]];

                TripSchedule tripSchedule = transitLayer.getTripPatterns()[patternIndex].tripSchedules.get(path.trips[i]);
                TripPattern tripPattern = transitLayer.getTripPatternByIndex(patternIndex);
                Route route = tripPattern.route;

                itinerary.transitTime += path.alightTimes[i] - path.boardTimes[i];
                itinerary.waitingTime += path.boardTimes[i] - path.transferTimes[i];

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
                    if (!boarded && tripSchedule.departures[j] == path.boardTimes[i]) {
                        boarded = true;
                    }
                    if (boarded) {
                        transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                                tripPattern.stopPattern.stops[j].getLat()));
                    }
                    if (boarded && tripSchedule.arrivals[j] == path.alightTimes[i]) {
                        break;
                    }
                }

                transitLeg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
                transitLeg.startTime = createCalendar(request.getDateTime(), path.boardTimes[i]);
                transitLeg.endTime = createCalendar(request.getDateTime(), path.alightTimes[i]);
                walkingTime += path.boardTimes[i] - path.alightTimes[i];
                itinerary.addLeg(transitLeg);
            }

            // Get stops for transfer leg
            int transferFromIndex = path.alightStops[i];
            int transferToIndex = i < path.boardStops.length - 1 ? path.boardStops[i + 1] : 1;
            Stop transferFromStop = transitLayer.getStopByIndex(transferFromIndex);
            Stop transferToStop = transitLayer.getStopByIndex(transferToIndex);

            // Create transfer leg if present
            if (transferFromIndex != transferToIndex) {
                Transfer transfer = transitLayer.getTransfer(transferFromIndex, transferToIndex);
                Leg transferLeg = new Leg();
                transferLeg.startTime = i == 0 ? itinerary.startTime : createCalendar(request.getDateTime(), path.alightTimes[i]);
                transferLeg.endTime = createCalendar(request.getDateTime(), path.transferTimes[i]);
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
