package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TripPatternMapper {

  /**
   * Convert all old TripPatterns into new ones, keeping a Map between the two.
   * Do this conversion up front (rather than lazily on demand) to ensure pattern IDs match
   * the sequence of patterns in source data.
   */
  static Multimap<org.opentripplanner.model.TripPattern, TripPattern>
  mapOldTripPatternToRaptorTripPattern(
      StopIndexForRaptor stopIndex,
      Collection<org.opentripplanner.model.TripPattern> oldTripPatterns
  ) {
    Multimap<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld;
    newTripPatternForOld = HashMultimap.create();

    for (org.opentripplanner.model.TripPattern oldTripPattern : oldTripPatterns) {
      // Collect all modes for TripPattern and underlying Trips
      Set<TransitMode> transitModes = new HashSet<>();
      transitModes.add(oldTripPattern.route.getTransitMode());
      transitModes.addAll(
          oldTripPattern.scheduledTimetable.tripTimes.stream()
              .map(t -> t.trip.getTransitMode())
              .collect(Collectors.toSet())
      );

      // Create a separate TripPattern for each mode. This will make it possible to filter patterns
      // by mode without looking at the individual trips.
      for (TransitMode transitMode : transitModes) {
        TripPattern newTripPattern = new TripPattern(
            // TripPatternForDate should never access the tripTimes inside the TripPattern,
            // so I've left them null.
            // No TripSchedules in the pattern itself; put them in the TripPatternForDate
            null, transitMode,
            stopIndex.listStopIndexesForStops(oldTripPattern.stopPattern.stops),
            oldTripPattern
        );
        newTripPatternForOld.put(oldTripPattern, newTripPattern);
      }
    }
    return newTripPatternForOld;
  }
}
