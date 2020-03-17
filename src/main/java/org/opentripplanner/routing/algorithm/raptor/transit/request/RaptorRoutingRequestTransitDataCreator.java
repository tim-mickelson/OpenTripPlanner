package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.apache.commons.collections.CollectionUtils;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper.secondsSinceStartOfTime;


/**
 * This class is responsible for creating the internal data structure of
 * {@link RaptorRoutingRequestTransitData}. The code is messy so it is nice to NOT have it in
 * the transit data class itself, to keep it clean. Another benefit of isolating this code
 * is that these methods are only available at construction time.
 */
class RaptorRoutingRequestTransitDataCreator {

  private final TransitLayer transitLayer;
  private final ZonedDateTime searchStartTime;
  private final LocalDate departureDate;


  RaptorRoutingRequestTransitDataCreator(
      TransitLayer transitLayer, Instant departureTime
  ) {
    this.transitLayer = transitLayer;
    this.departureDate = LocalDate.ofInstant(departureTime, transitLayer.getTransitDataZoneId());
    this.searchStartTime = DateMapper.asStartOfService(departureDate, transitLayer.getTransitDataZoneId());
  }

  ZonedDateTime getSearchStartTime() {
    return searchStartTime;
  }

  List<List<TripPatternForDates>> createTripPatternsPerStop(
      int dayRange,
      TraverseModeSet transitModes,
      HashMap<TraverseMode,
      Set<TransmodelTransportSubmode>> subModesForMode,
      Set<FeedScopedId> bannedRoutes
  ) {

    List<Map<FeedScopedId, TripPatternForDate>> tripPatternForDates = getTripPatternsForDateRange(
        dayRange,
        transitModes,
        subModesForMode,
        bannedRoutes
    );

    List<TripPatternForDates> tripPatternForDateList = merge(searchStartTime, tripPatternForDates);

    return createTripPatternsPerStop(tripPatternForDateList, transitLayer.getStopCount());
  }

  private List<Map<FeedScopedId, TripPatternForDate>> getTripPatternsForDateRange(
      int dayRange,
      TraverseModeSet transitModes,
      HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes,
      Set<FeedScopedId> bannedRoutes
  ) {
    List<Map<FeedScopedId, TripPatternForDate>> tripPatternForDates = new ArrayList<>();

    // Start at yesterdays date to account for trips that cross midnight. This is also
    // accounted for in TripPatternForDates.
    for (int d = -1; d < dayRange - 1; ++d) {
      tripPatternForDates.add(
        filterActiveTripPatterns(
          transitLayer,
          departureDate.plusDays(d),
          transitModes,
          transportSubmodes,
          bannedRoutes
        )
      );
    }

    return tripPatternForDates;
  }

  /**
   * This method merges several list of TripPatterns for several consecutive dates into a single
   * list of TripPatternsForDates. The purpose of doing this is so that TripSchedules for several
   * dates are combined by TripPattern instead of having their own TripPattern. This is to improve
   * performance for searching, as each TripPattern is searched only once per round.
   */
  static List<TripPatternForDates> merge(
      ZonedDateTime searchStartTime, List<Map<FeedScopedId, TripPatternForDate>> tripPatternForDateList
  ) {
    List<TripPatternForDates> combinedList = new ArrayList<>();

    // Extract all distinct TripPatterns across the search days
    Map<FeedScopedId, TripPattern> allTripPatternsById = tripPatternForDateList
        .stream()
        .flatMap(t -> t.values().stream())
        .map(TripPatternForDate::getTripPattern)
        .distinct()
        .collect(Collectors.toMap(TripPattern::getId, t -> t));

    // For each TripPattern, time expand each TripPatternForDate and merge into a single TripPatternForDates
    for (Map.Entry<FeedScopedId, TripPattern> patternEntry : allTripPatternsById.entrySet()) {
      List<TripPatternForDate> tripPatterns = new ArrayList<>();
      List<Integer> offsets = new ArrayList<>();

      for (Map<FeedScopedId, TripPatternForDate> tripPatternById : tripPatternForDateList) {
        TripPatternForDate tripPatternForDate = tripPatternById.get(patternEntry.getKey());
        if (tripPatternForDate != null) {
          tripPatterns.add(tripPatternForDate);
          offsets.add(secondsSinceStartOfTime(searchStartTime, tripPatternForDate.getLocalDate()));
        }
      }

      combinedList.add(new TripPatternForDates(patternEntry.getValue(), tripPatterns, offsets));
    }

    return combinedList;
  }

  private static List<List<TripPatternForDates>> createTripPatternsPerStop(
      List<TripPatternForDates> tripPatternsForDate, int numberOfStops
  ) {
    List<List<TripPatternForDates>> result = Stream
        .generate(ArrayList<TripPatternForDates>::new)
        .limit(numberOfStops)
        .collect(Collectors.toList());

    for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
      for (int i : tripPatternForDateList.getTripPattern().getStopIndexes()) {
        result.get(i).add(tripPatternForDateList);
      }
    }
    return result;
  }

  private static Map<FeedScopedId, TripPatternForDate> filterActiveTripPatterns(
      TransitLayer transitLayer,
      LocalDate date,
      TraverseModeSet transitModes,
      HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes,
      Set<FeedScopedId> bannedRoutes
  ) {

    return transitLayer
        .getTripPatternsForDate(date)
        .stream()
        .filter(p -> modeIsAllowed(p, transitModes, transportSubmodes))
        .filter(p -> !bannedRoutes.contains(p.getTripPattern()
            .getOriginalTripPattern().route.getId()))
        .collect(toMap(p -> p.getTripPattern().getId(), p -> p));
  }

  List<List<TransferLeg>> calculateTransferDuration(double walkSpeed) {
    return transitLayer
        .getTransferByStopIndex()
        .stream()
        .map(t -> t
            .stream()
            .map(s -> new TransferWithDuration(s, walkSpeed))
            .collect(Collectors.<TransferLeg>toList()))
        .collect(toList());
  }

  private static boolean modeIsAllowed(
      TripPatternForDate tripPatternForDate,
      TraverseModeSet transitModes,
      HashMap<TraverseMode, Set<TransmodelTransportSubmode>> transportSubmodes
  ) {
    TransitMode transitModeForCurrentPattern =
        tripPatternForDate.getTripPattern().getTransitMode();

    if (!transitModes.contains(transitModeForCurrentPattern.getMode())) {
      return false;
    } else
    if (!transportSubmodes.isEmpty()) {
      Set<TransmodelTransportSubmode> allowedSubmodesForMode = transportSubmodes.get(
          transitModeForCurrentPattern.getMode()
      );
      return CollectionUtils.isEmpty(allowedSubmodesForMode)
          || allowedSubmodesForMode.contains(transitModeForCurrentPattern.getSubmode());
    }
    return true;
  }
}
