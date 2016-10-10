package org.opentripplanner.updater;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.VehicleActivityStructure;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 *
 */
public class SiriFuzzyTripMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

    private GraphIndex index;

    private static Map<String, Set<Trip>> mappedTripsCache = new HashMap<>();
    private static Map<String, Set<Trip>> start_stop_tripCache = new HashMap<>();

    public SiriFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
        initCache(this.index);
    }

    //For testing only
    protected SiriFuzzyTripMatcher(GraphIndex index, boolean forceCacheRebuild) {
        LOG.error("For testing only");
        this.index = index;

        if (forceCacheRebuild) {
            mappedTripsCache.clear();
        }
        initCache(this.index);
    }

    /**
     * Matches VehicleActivity to a set of possible Trips based on tripId
     */
    public Set<Trip> match(VehicleActivityStructure activity) {
        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

        if (monitoredVehicleJourney != null) {

            String tripId = null;
            if (monitoredVehicleJourney.getCourseOfJourneyRef() != null) {
                tripId = monitoredVehicleJourney.getCourseOfJourneyRef().getValue();
            }
            //TripId is provided in VM-delivery
            return getCachedTripsBySiriId(tripId);
        }

        return null;
    }

    /**
     * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
     */
    public Set<Trip> match(EstimatedVehicleJourney journey) {

        List<EstimatedCall> estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
        EstimatedCall firstStop = estimatedCalls.get(0);
        EstimatedCall lastStop = estimatedCalls.get(estimatedCalls.size()-1);

        String firstStopPoint = firstStop.getStopPointRef().getValue();
        String lastStopPoint = lastStop.getStopPointRef().getValue();

        ZonedDateTime departureTime = firstStop.getAimedDepartureTime();

        return start_stop_tripCache.get(createStartStopKey(firstStopPoint, lastStopPoint, departureTime.toLocalTime().toSecondOfDay()));
    }

    private Set<Trip> getCachedTripsBySiriId(String tripId) {
        if (tripId == null) {return null;}
        return mappedTripsCache.get(tripId);
    }

    private static void initCache(GraphIndex index) {
        if (mappedTripsCache.isEmpty()) {

            Set<Trip> trips = index.patternForTrip.keySet();
            for (Trip trip : trips) {

                String currentTripId = getUnpaddedTripId(trip);

                if (mappedTripsCache.containsKey(currentTripId)) {
                    mappedTripsCache.get(currentTripId).add(trip);
                } else {
                    Set<Trip> initialSet = new HashSet<>();
                    initialSet.add(trip);
                    mappedTripsCache.put(currentTripId, initialSet);
                }

                TripPattern tripPattern = index.patternForTrip.get(trip);
                String firstStopId = tripPattern.getStops().get(0).getId().getId();
                String lastStopId = tripPattern.getStops().get(tripPattern.getStops().size()-1).getId().getId();

                TripTimes tripTimes = tripPattern.scheduledTimetable.getTripTimes(trip);
                if (tripTimes != null) {
                    int departureTime = tripTimes.getDepartureTime(0);

                    String key = createStartStopKey(firstStopId, lastStopId, departureTime);
                    if (start_stop_tripCache.containsKey(key)) {
                        start_stop_tripCache.get(key).add(trip);
                    } else {
                        Set<Trip> initialSet = new HashSet<>();
                        initialSet.add(trip);
                        start_stop_tripCache.put(key, initialSet);
                    }
                }
            }

            LOG.info("Built trips-cache [{}].", mappedTripsCache.size());
            LOG.info("Built start-stop-cache [{}].", start_stop_tripCache.size());
        }
    }

    public Trip getTrip (Route route, int direction,
                         int startTime, ServiceDate date) {
        BitSet services = index.servicesRunning(date);
        for (TripPattern pattern : index.patternsForRoute.get(route)) {
            if (pattern.directionId != direction) continue;
            for (TripTimes times : pattern.scheduledTimetable.tripTimes) {
                if (times.getScheduledDepartureTime(0) == startTime &&
                        services.get(times.serviceCode)) {
                    return times.trip;
                }
            }
        }
        return null;
    }

    private static String createStartStopKey(String firstStopId, String lastStopId, int departureTime) {
        return firstStopId + ":" + lastStopId + ":" + departureTime;
    }

    private static String getUnpaddedTripId(Trip trip) {
        String id = trip.getId().getId();
        if (id.indexOf("-") > 0) {
            return id.substring(0, id.indexOf("-"));
        } else {
            return id;
        }
    }
}
