package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.model.FeedId;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private Graph graph;
    private TransitLayer transitLayer;

    public TransitLayer map(Graph graph) {
        this.graph = graph;
        this.transitLayer = new TransitLayer();
        LOG.info("Creating stop maps...");
        createStopMaps();
        LOG.info("Mapping services...");
        mapServices();
        LOG.info("Mapping trip patterns...");
        mapTripPatterns();
        LOG.info("Mapping transfers...");
        mapTransfers();
        return this.transitLayer;
    }

    /** Create maps between stop indices used by Raptor and stop objects in original graph */
    private void createStopMaps() {
        ArrayList<Stop> stops = new ArrayList<>(graph.index.stopForId.values());
        transitLayer.stopsByIndex = new Stop[stops.size()];
        transitLayer.indexByStop = new HashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop currentStop = stops.get(i);
            transitLayer.stopsByIndex[i] = currentStop;
            transitLayer.indexByStop.put(currentStop, i);
        }
    }

    /** Service ids are mapped to integers in the original graph. The same mapping is kept when creating the
     * Raptor data structure */
    private void mapServices() {
        transitLayer.services = new ArrayList<>();
        CalendarService calendarService = graph.getCalendarService();
        Iterator<FeedId> serviceIdIterator = calendarService.getServiceIds().iterator();
        while (serviceIdIterator.hasNext()) {
            FeedId serviceId = serviceIdIterator.next();
            int serviceIndex = graph.serviceCodes.get(serviceId);
            Set<LocalDate> localDates = calendarService.getServiceDatesForServiceId(serviceId)
                    .stream().map(this::localDateFromServiceDate).collect(Collectors.toSet());
            Service service = new Service(serviceIndex++, localDates);
            transitLayer.services.add(service);
        }
    }

    /** Map trip patterns and trips to Raptor classes */
    private void mapTripPatterns() {
        List<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns = new ArrayList<>(graph.index.patternForId.values());
        transitLayer.tripPatternByIndex = new org.opentripplanner.routing.edgetype.TripPattern[originalTripPatterns.size()];
        transitLayer.patterns = new TripPattern[originalTripPatterns.size()];
        transitLayer.patternsByStop = new TIntList[transitLayer.stopsByIndex.length];
        for (int i = 0; i < transitLayer.patternsByStop.length; i++) {
            transitLayer.patternsByStop[i] = new TIntArrayList();
        }
        for (int patternIndex = 0; patternIndex < originalTripPatterns.size(); patternIndex++) {
            org.opentripplanner.routing.edgetype.TripPattern tripPattern = originalTripPatterns.get(patternIndex);
            transitLayer.tripPatternByIndex[patternIndex] = tripPattern;
            TripPattern newTripPattern = new TripPattern();
            int[] stopPattern = new int[tripPattern.stopPattern.size];
            for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                int stopIndex = transitLayer.indexByStop.get(tripPattern.getStop(i));
                stopPattern[i] = stopIndex;
                transitLayer.patternsByStop[stopIndex].add(patternIndex);
            }
            newTripPattern.stopPattern = stopPattern;
            for (TripTimes tripTimes : tripPattern.scheduledTimetable.tripTimes) {
                TripSchedule tripSchedule = new TripSchedule();
                tripSchedule.arrivals = new int[stopPattern.length];
                tripSchedule.departures = new int[stopPattern.length];
                for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                    tripSchedule.arrivals[i] = tripTimes.getArrivalTime(i);
                    tripSchedule.departures[i] = tripTimes.getDepartureTime(i);
                }
                tripSchedule.serviceCode = tripTimes.serviceCode;
                newTripPattern.tripSchedules.add(tripSchedule);
            }
            newTripPattern.hasSchedules = !newTripPattern.tripSchedules.isEmpty();
            transitLayer.patterns[patternIndex] = newTripPattern;
        }
    }

    /** Copy pre-calculated transfers from the original graph */
    private void mapTransfers() {
        transitLayer.transferDistances = new TIntArrayList[transitLayer.stopsByIndex.length];
        for (int i = 0; i < transitLayer.stopsByIndex.length; i++) {
            transitLayer.transferDistances[i] = new TIntArrayList();
            for (Edge edge : graph.index.stopVertexForStop.get(transitLayer.stopsByIndex[i]).getOutgoing()) {
                if (edge instanceof SimpleTransfer) {
                    int index = transitLayer.indexByStop.get(((TransitStop)edge.getToVertex()).getStop());
                    double distance = edge.getDistance();
                    transitLayer.transferDistances[i].add(index);
                    transitLayer.transferDistances[i].add((int)(distance * 1000));
                }
            }
        }
    }

    private LocalDate localDateFromServiceDate(ServiceDate serviceDate) {
        return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay());
    }
}
