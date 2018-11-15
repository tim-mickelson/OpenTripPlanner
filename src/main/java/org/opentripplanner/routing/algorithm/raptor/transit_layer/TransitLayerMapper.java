package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.joda.time.DateTime;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.TimeToStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed, with service days at
 * the top level, which contains TripPatternForDate objects that contain only TripSchedules running on that particular
 * date. This makes it faster to filter out TripSchedules when doing Range Raptor searches.
 */

public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private Graph graph;
    private TransitLayer transitLayer;

    private boolean ignoreRealtime;

    private final int RESERVED_STOPS = 2;

    private final LocalDate today = LocalDate.now();
    private final int REALTIME_NO_OF_DAYS = 7;

    public TransitLayer map(Graph graph, boolean ignoreRealtime) {
        this.graph = graph;
        this.transitLayer = new TransitLayer();
        this.ignoreRealtime = ignoreRealtime;
        LOG.info("Mapping transitLayer from Graph...");
        createStopMaps();
        mapTripPatterns();
        mapTransfers();
        LOG.info("Mapping complete.");
        return this.transitLayer;
    }

    /** Create maps between stop indices used by Raptor and stop objects in original graph */
    private void createStopMaps() {
        ArrayList<Stop> stops = new ArrayList<>(graph.index.stopForId.values());
        transitLayer.stopsByIndex = new Stop[stops.size() + RESERVED_STOPS];
        transitLayer.indexByStop = new HashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop currentStop = stops.get(i);
            transitLayer.stopsByIndex[i + RESERVED_STOPS] = currentStop;
            transitLayer.indexByStop.put(currentStop, i + RESERVED_STOPS);
        }
    }

    /** Map trip tripPatterns and trips to Raptor classes */
    private void mapTripPatterns() {
        List<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns = new ArrayList<>(graph.index.patternForId.values());
        List<TripPattern>[] tripPatternForStop = new ArrayList[transitLayer.stopsByIndex.length];
        Arrays.setAll(tripPatternForStop, a -> new ArrayList<>());

        int realTimeServiceCodeIndex = graph.serviceCodes.size();

        Multimap<LocalDate, Integer> serviceCodesByLocalDates = HashMultimap.create();

        Iterator<AgencyAndId> serviceIdIterator = graph.getCalendarService().getServiceIds().iterator();
        while (serviceIdIterator.hasNext()) {
            AgencyAndId serviceId = serviceIdIterator.next();
            Set<LocalDate> localDates = graph.getCalendarService().getServiceDatesForServiceId(serviceId)
                    .stream().map(this::localDateFromServiceDate).collect(Collectors.toSet());
            int serviceIndex = graph.serviceCodes.get(serviceId);

            for (LocalDate date : localDates) {
                serviceCodesByLocalDates.put(date, serviceIndex);
            }
        }

        TimetableSnapshot timetableSnapShot = graph.timetableSnapshotSource.getTimetableSnapshot();

        Multimap<Integer, TripPattern> patternsByServiceCode = HashMultimap.create();

        Set<TripPatternAndDate> excludeFromCalendar = new HashSet<>();

        int patternId = 0;
        for (org.opentripplanner.routing.edgetype.TripPattern tripPattern : originalTripPatterns) {
            List<TripSchedule> tripSchedules = new ArrayList<>();
            int[] stopPattern = new int[tripPattern.stopPattern.size];

            TripPattern newTripPattern = new TripPattern(
                    patternId++,
                    tripSchedules,
                    tripPattern.mode,
                    tripPattern.getTransportSubmode(),
                    stopPattern,
                    tripPattern
            );

            if (!ignoreRealtime) {
                for (int day = 0; day < REALTIME_NO_OF_DAYS; day++) {
                    ServiceDate serviceDate = new ServiceDate(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
                    Timetable timetable = timetableSnapShot.resolve(tripPattern, serviceDate);
                    if (timetable != tripPattern.scheduledTimetable) {
                        excludeFromCalendar.add(new TripPatternAndDate(newTripPattern, today.plusDays(day)));

                        for (TripTimes tripTimes : timetable.tripTimes) {
                            TripScheduleImpl tripSchedule = new TripScheduleImpl(
                                    new int[stopPattern.length],
                                    new int[stopPattern.length],
                                    tripTimes.trip,
                                    tripPattern,
                                    realTimeServiceCodeIndex + day
                            );

                            for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                                tripSchedule.setArrival(i, tripTimes.getArrivalTime(i));
                                tripSchedule.setDeparture(i, tripTimes.getDepartureTime(i));
                            }

                            tripSchedules.add(tripSchedule);
                        }
                    }
                }
            }

            List<TripTimes> sortedTripTimes = tripPattern.scheduledTimetable.tripTimes.stream()
                    .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                    .collect(Collectors.toList());

            for (TripTimes tripTimes : sortedTripTimes) {
                TripScheduleImpl tripSchedule = new TripScheduleImpl(
                    new int[stopPattern.length],
                    new int[stopPattern.length],
                    tripTimes.trip,
                    tripPattern,
                    tripTimes.serviceCode
                );

                for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                    tripSchedule.setArrival(i, tripTimes.getArrivalTime(i));
                    tripSchedule.setDeparture(i, tripTimes.getDepartureTime(i));
                }

                patternsByServiceCode.put(tripTimes.serviceCode, newTripPattern);
                tripSchedules.add(tripSchedule);
            }

            for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                int stopIndex = transitLayer.indexByStop.get(tripPattern.getStop(i));
                newTripPattern.getStopPattern()[i] = stopIndex;
                tripPatternForStop[stopIndex].add(newTripPattern);
            }
        }

        transitLayer.tripPatternsForDate = new HashMap<>();

        for (Map.Entry<LocalDate, Collection<Integer>> serviceEntry : serviceCodesByLocalDates.asMap().entrySet()) {
            Set<Integer> servicesForDate = new HashSet<>(serviceEntry.getValue());
            List<TripPattern> filteredPatterns = serviceEntry.getValue().stream().map(s -> patternsByServiceCode.get(s))
                    .flatMap(s -> s.stream()) .collect(Collectors.toList());

            List<TripPatternForDate> tripPatternsForDate = new ArrayList();

            for (TripPattern tripPattern : filteredPatterns) {
                List<TripSchedule> tripSchedules = new ArrayList<>(tripPattern.getTripSchedules());

                if (!ignoreRealtime && excludeFromCalendar.contains(new TripPatternAndDate(tripPattern, serviceEntry.getKey()))) {
                    // Filter trips by real time service code
                    int realTimeServiceCode = (realTimeServiceCodeIndex + (int)ChronoUnit.DAYS.between(today, serviceEntry.getKey()));
                    tripSchedules = tripSchedules.stream().filter(t -> t.getServiceCode() == realTimeServiceCode).collect(Collectors.toList());
                } else {
                    // Filter trips by regular service code
                    tripSchedules = tripSchedules.stream().filter(t -> servicesForDate.contains(t.getServiceCode()))
                            .collect(Collectors.toList());
                }

                TripPatternForDate tripPatternForDate = new TripPatternForDate(
                        tripPattern,
                        tripSchedules);

                    tripPatternsForDate.add(tripPatternForDate);
            }

            transitLayer.tripPatternsForDate.put(serviceEntry.getKey(), tripPatternsForDate);
        }

        // Sort by TripPattern for easier merging in OtpRRDataProvider
        transitLayer.tripPatternsForDate.entrySet().stream()
                .forEach(t -> t.getValue()
                        .sort((p1, p2) -> Integer.compare(p1.getTripPattern().getId(), p2.getTripPattern().getId())));
    }

    /** Copy pre-calculated transfers from the original graph */
    private void mapTransfers() {
        transitLayer.transferByStopPair = new HashMap<>();
        transitLayer.transferByStop = new ArrayList[transitLayer.stopsByIndex.length];
        Arrays.setAll(transitLayer.transferByStop, a -> new ArrayList<>());
        for (int i = 0; i < transitLayer.stopsByIndex.length; i++) {
            if (i < RESERVED_STOPS) continue;
            for (Edge edge : graph.index.stopVertexForStop.get(transitLayer.stopsByIndex[i]).getOutgoing()) {
                if (edge instanceof SimpleTransfer) {
                    int stopIndex = transitLayer.indexByStop.get(((TransitStop)edge.getToVertex()).getStop());
                    double distance = edge.getDistance();

                    Transfer transfer = new Transfer(
                            stopIndex,
                            (int)distance,
                            0, // TODO: Calculate cost
                            Arrays.asList(edge.getGeometry().getCoordinates()));

                    transitLayer.transferByStopPair.put(new OrderedIndexPair(i, stopIndex), transfer);
                    transitLayer.transferByStop[i].add(transfer);
                }
            }
        }
    }

    private LocalDate localDateFromServiceDate(ServiceDate serviceDate) {
        return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay());
    }

    private class TripPatternAndDate {
        TripPatternAndDate(TripPattern tripPattern, LocalDate date) {
            this.tripPattern = tripPattern;
            this.date = date;
        }

        LocalDate date;
        TripPattern tripPattern;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TripPatternAndDate that = (TripPatternAndDate) o;
            return Objects.equals(date, that.date) &&
                    Objects.equals(tripPattern, that.tripPattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, tripPattern);
        }
    }
}
