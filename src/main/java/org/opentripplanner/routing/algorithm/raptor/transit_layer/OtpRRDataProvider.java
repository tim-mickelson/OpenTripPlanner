package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.StopArrival;
import com.conveyal.r5.profile.entur.api.TransitDataProvider;
import com.conveyal.r5.profile.entur.api.TripPatternInfo;
import com.conveyal.r5.profile.entur.api.UnsignedIntIterator;
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

public class OtpRRDataProvider implements TransitDataProvider<TripSchedule> {

    private static final Logger LOG = LoggerFactory.getLogger(OtpRRDataProvider.class);

    private TransitLayer transitLayer;

    /** Active trip patterns by stop index */
    private List<List<TripPatternForDate>> activeTripPatternsPerStop;

    /** Transfers by stop index */
    private List<List<StopArrival>> transfers;

    double startTime = System.currentTimeMillis();

    public OtpRRDataProvider(TransitLayer transitLayer, LocalDate date, int dayRange, TraverseModeSet transitModes,
                             HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes, double walkSpeed) {
        this.transitLayer = transitLayer;
        setActiveTripPatterns(date, transitModes, transportSubmodes);
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

    private void setTripPatternsPerStop(List<TripPatternForDate> tripPatternsForDate) {

        this.activeTripPatternsPerStop = Stream.generate(ArrayList<TripPatternForDate>::new)
                .limit(numberOfStops()).collect(Collectors.toList());

        for (TripPatternForDate tripPatternForDate : tripPatternsForDate) {
            for (int i : tripPatternForDate.getTripPattern().getStopPattern()) {
                this.activeTripPatternsPerStop.get(i).add(tripPatternForDate);
            }
        }
    }

    private void calculateTransferDuration(double walkSpeed) {
        this.transfers = Arrays.stream(transitLayer.getTransferByStop())
                .map(t ->  t.stream().map(s -> new StopArrivalImpl(s, walkSpeed)).collect(Collectors.<StopArrival>toList()))
                .collect(toList());
    }
}
