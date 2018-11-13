package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.*;
import org.opentripplanner.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer, but filters it by
 * dates and modes per request. Transfers durations are pre-calculated per request based on walk speed.
 */

public class OtpRRDataProvider implements TransitDataProvider<TripSchedule> {

    private static final Logger LOG = LoggerFactory.getLogger(OtpRRDataProvider.class);

    private TransitLayer transitLayer;

    /** Active trip patterns by stop index */
    private List<List<TripPatternForDates>> activeTripPatternsPerStop;

    /** Transfers by stop index */
    private List<List<StopArrival>> transfers;

    public OtpRRDataProvider(TransitLayer transitLayer, LocalDate startDate, int dayRange, TraverseModeSet transitModes,
                             HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes, double walkSpeed) {
        this.transitLayer = transitLayer;
        List<List<TripPatternForDate>> tripPatternForDates = getTripPatternsForDateRange(startDate, dayRange, transitModes, transportSubmodes);
        List<TripPatternForDates> tripPatternForDateList = MergeTripPatternForDates.merge(tripPatternForDates);
        setTripPatternsPerStop(tripPatternForDateList);
        calculateTransferDuration(walkSpeed);
    }

    /** Gets all the transfers starting at a given stop */
    @Override
    public Iterator<StopArrival> getTransfers(int stopIndex) {
        return transfers.get(stopIndex).iterator();
    }

    /** Gets all the unique trip patterns touching a set of stops */
    @Override
    public Iterator<TripPatternInfo<TripSchedule>> patternIterator(UnsignedIntIterator stops) {
        Set<TripPatternInfo<TripSchedule>> activeTripPatternsForGivenStops = new HashSet<>();
        int stopIndex = stops.next();
        while (stopIndex > 0) {
            activeTripPatternsForGivenStops.addAll(activeTripPatternsPerStop.get(stopIndex));
            stopIndex = stops.next();
        }
        return activeTripPatternsForGivenStops.iterator();
    }

    @Override
    public int numberOfStops() {
        return transitLayer.getStopCount();
    }

    private List<TripPatternForDate> setActiveTripPatterns(LocalDate date, TraverseModeSet transitModes, HashMap<TraverseMode,
            Set<TransmodelTransportSubmode>> transportSubmodes) {

        return transitLayer.getTripPatternsForDate(date).stream()
                .filter(p -> transitModes.contains(p.getTripPattern().getTransitMode())) // TODO: Fix submode per main mode
                .collect(toList());
    }

    private List<List<TripPatternForDate>> getTripPatternsForDateRange(LocalDate startDate, int dayRange, TraverseModeSet transitModes, HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes) {
        List<List<TripPatternForDate>> tripPatternForDates = new ArrayList<>();
        for (LocalDate currentDate = startDate; currentDate.isBefore(startDate.plusDays(dayRange)); currentDate = currentDate.plusDays(1)) {
            tripPatternForDates.add(setActiveTripPatterns(currentDate, transitModes, transportSubmodes));
        }
        return tripPatternForDates;
    }

    private void setTripPatternsPerStop(List<TripPatternForDates> tripPatternsForDate) {

        this.activeTripPatternsPerStop = Stream.generate(ArrayList<TripPatternForDates>::new)
                .limit(numberOfStops()).collect(Collectors.toList());

        for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
            for (int i : tripPatternForDateList.getTripPattern().getStopPattern()) {
                this.activeTripPatternsPerStop.get(i).add(tripPatternForDateList);
            }
        }
    }

    private void calculateTransferDuration(double walkSpeed) {
        this.transfers = Arrays.stream(transitLayer.getTransferByStop())
                .map(t ->  t.stream().map(s -> new StopArrivalImpl(s, walkSpeed)).collect(Collectors.<StopArrival>toList()))
                .collect(toList());
    }
}
