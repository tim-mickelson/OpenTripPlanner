package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.BitSet;

public class OtpRRDataProvider implements RaptorWorkerTransitDataProvider {

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

    @Override
    public int[] getScheduledIndexForOriginalPatternIndex() {
        return scheduledIndexForOriginalPatternIndex;
    }

    @Override
    public TIntList getTransfersDistancesInMMForStop(int stop) {
        return transitLayer.getTransfersForStop(stop);
    }

    @Override
    public TIntList getPatternsForStop(int stop) {
        return transitLayer.getPatternsForStop(stop);
    }

    @Override
    public boolean skipCalendarService(int serviceCode) {
        return !servicesActive.get(serviceCode);
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

    @Override public PatternIterator patternIterator(BitSet patternsTouched) {
        return new InternalPatternIterator(patternsTouched);
    }

    class InternalPatternIterator implements PatternIterator, Pattern {
        private int nextPatternIndex;
        private int originalPatternIndex;
        private BitSet patternsTouched;
        private TripPattern pattern;

        InternalPatternIterator(BitSet patternsTouched) {
            this.patternsTouched = patternsTouched;
            this.nextPatternIndex = 0;
        }

        /*  PatternIterator interface implementation */

        @Override public boolean morePatterns() {
            return nextPatternIndex >=0;
        }

        @Override public Pattern next() {
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
        public Iterable<TripSchedule> getTripSchedules() {
            return pattern.tripSchedules;
        }

        @Override
        public int getTripSchedulesIndex(TripSchedule schedule) {
            return pattern.tripSchedules.indexOf(schedule);
        }

        @Override
        public TripSchedule getTripSchedule(int index) {
            return pattern.tripSchedules.get(index);
        }
    }


}
