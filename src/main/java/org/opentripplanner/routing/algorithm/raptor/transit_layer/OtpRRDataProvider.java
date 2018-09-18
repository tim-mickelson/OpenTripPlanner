package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.DurationToStop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.Pattern;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.TransitDataProvider;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.TripScheduleInfo;
import org.opentripplanner.routing.algorithm.raptor.mcrr.util.BitSetIterator;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;

public class OtpRRDataProvider implements TransitDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OtpRRDataProvider.class);

    private static boolean PRINT_REFILTERING_PATTERNS_INFO = true;

    private TransitLayer transitLayer;

    /** Array mapping from original pattern indices to the filtered scheduled indices */
    private int[] scheduledIndexForOriginalPatternIndex;

    /** Schedule-based trip patterns running on a given day */
    private TripPattern[] runningScheduledPatterns;

    /** Map from internal, filtered pattern indices back to original pattern indices for scheduled patterns */
    private int[] originalPatternIndexForScheduledIndex;

    /** Services active on the date of the search */
    private final BitSet servicesActive;

    /** Allowed transit modes */
    private final TraverseModeSet transitModes;

    public OtpRRDataProvider(TransitLayer transitLayer, LocalDate date, TraverseModeSet transitModes) {
        this.transitLayer = transitLayer;
        this.servicesActive  = transitLayer.getActiveServicesForDate(date);
        this.transitModes = transitModes;
    }

    public TransitLayer getTransitLayer() {
        return this.transitLayer;
    }

    public TIntList getPatternsForStop(int stop) {
        return transitLayer.getPatternsForStop(stop);
    }

    /** Prefilter the patterns to only ones that are running */
    public void init() {
        TIntList scheduledPatterns = new TIntArrayList();
        scheduledIndexForOriginalPatternIndex = new int[transitLayer.getTripPatterns().length];
        Arrays.fill(scheduledIndexForOriginalPatternIndex, -1);

        int patternIndex = -1; // first increment lands at 0
        int scheduledIndex = 0;

        for (TripPattern pattern : transitLayer.getTripPatterns()) {
            patternIndex++;

            TraverseMode mode = pattern.transitModesSet;
            if (pattern.containsServices.intersects(servicesActive) && transitModes.contains(mode)) {
                // at least one trip on this pattern is relevant, based on the profile request's date and modes
                if (pattern.hasSchedules) { // NB not else b/c we still support combined frequency and schedule patterns.
                    scheduledPatterns.add(patternIndex);
                    scheduledIndexForOriginalPatternIndex[patternIndex] = scheduledIndex++;
                }
            }
        }

        originalPatternIndexForScheduledIndex = scheduledPatterns.toArray();

        // TODO check this
        runningScheduledPatterns = transitLayer.getTripPatterns();

        if (PRINT_REFILTERING_PATTERNS_INFO) {
            LOG.info("Prefiltering patterns based on date active reduced {} patterns to {} scheduled patterns",
                    transitLayer.getTripPatterns().length, scheduledPatterns.size());
            PRINT_REFILTERING_PATTERNS_INFO = false;
        }
    }

    @Override
    public Iterator<DurationToStop> getTransfers(int fromStop) {
        return null;
    }

    @Override public Iterator<Pattern> patternIterator(BitSetIterator stops) {
        return new InternalPatternIterator(getPatternsTouchedForStops(stops));
    }

    @Override
    public boolean isTripScheduleInService(TripScheduleInfo trip) {
        TripSchedule t = (TripSchedule)trip;
        return t.headwaySeconds == null && servicesActive.get(t.serviceCode);
    }

    private BitSet getPatternsTouchedForStops(BitSetIterator stops) {
        BitSet patternsTouched = new BitSet();

        for (int stop = stops.next(); stop >= 0; stop = stops.next()) {

            getPatternsForStop(stop).forEach(originalPattern -> {
                int filteredPattern = scheduledIndexForOriginalPatternIndex[originalPattern];

                if (filteredPattern < 0) {
                    return true; // this pattern does not exist in the local subset of patterns, continue iteration
                }

                patternsTouched.set(filteredPattern);
                return true; // continue iteration
            });
        }
        return patternsTouched;
    }

    class InternalPatternIterator implements Iterator<Pattern>, org.opentripplanner.routing.algorithm.raptor.mcrr.api.Pattern {
        private int nextPatternIndex;
        private int originalPatternIndex;
        private BitSet patternsTouched;
        private TripPattern pattern;

        InternalPatternIterator(BitSet patternsTouched) {
            this.patternsTouched = patternsTouched;
            this.nextPatternIndex = 0;
        }

        /*  PatternIterator interface implementation */

        @Override public boolean hasNext() {
            return nextPatternIndex >=0;
        }

        @Override public org.opentripplanner.routing.algorithm.raptor.mcrr.api.Pattern next() {
            pattern = runningScheduledPatterns[nextPatternIndex];
            originalPatternIndex = originalPatternIndexForScheduledIndex[nextPatternIndex];
            nextPatternIndex = patternsTouched.nextSetBit(nextPatternIndex + 1);
            return this;
        }


        /*  Pattern interface implementation */

        @Override public int originalPatternIndex() {
            return originalPatternIndex;
        }

        @Override
        public int currentPatternStop(int stopPositionInPattern) {
            return pattern.stopPattern[stopPositionInPattern];
        }

        @Override
        public int currentPatternStopsSize() {
            return pattern.stopPattern.length;
        }

        @Override
        public TripScheduleInfo getTripSchedule(int index) {
            return pattern.tripSchedules.get(index);
        }

        @Override
        public int getTripScheduleSize() {
            return pattern.tripSchedules.size();
        }
    }


}
