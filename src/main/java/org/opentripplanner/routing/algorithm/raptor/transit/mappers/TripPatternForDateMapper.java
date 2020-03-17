package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleWrapperImpl;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Maps a Timetable and a date to a TripPatternForDate. TripSchedules are then filtered according
 * to the active service codes.
 *
 * If the Timetable contains a ServiceDate that is not valid for any of its trips, a message is
 * logged.
 *
 * This class is THREAD SAFE because the collections initialized as part of the class state are
 * concurrent and because the collections passed in on the constructor or their elements are not
 * modified. The objects passed into the map method are also not modified.
 */
public class TripPatternForDateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TripPatternForDateMapper.class);

  private final ConcurrentMap<Timetable, List<TripTimes>> sortedTripTimesForTimetable =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<TripTimes, TripSchedule> tripScheduleForTripTimes =
      new ConcurrentHashMap<>();

  private final Map<ServiceDate, TIntSet> serviceCodesRunningForDate;

  private final Multimap<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld;

  /**
   * @param serviceCodesRunningForDate - READ ONLY
   * @param newTripPatternForOld - READ ONLY
   */
  TripPatternForDateMapper(
      Map<ServiceDate, TIntSet> serviceCodesRunningForDate,
      Multimap<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld
  ) {
    this.serviceCodesRunningForDate = Collections.unmodifiableMap(serviceCodesRunningForDate);
    this.newTripPatternForOld = ImmutableMultimap.copyOf(newTripPatternForOld);
  }

  /**
   * This method is THREAD SAFE.
   *
   * @param timetable The timetable to be mapped to TripPatternForDate - READ ONLY
   * @param serviceDate The date to map the TripPatternForDate for - READ ONLY
   * @return TripPatternForDate for this timetable and serviceDate
   */
  public Collection<TripPatternForDate> map(Timetable timetable, ServiceDate serviceDate) {

    Collection<TripPatternForDate> tripPatternsForDate = new ArrayList<>();

    TIntSet serviceCodesRunning = serviceCodesRunningForDate.get(serviceDate);

    org.opentripplanner.model.TripPattern oldTripPattern = timetable.pattern;

    for (TripPattern newTripPattern : newTripPatternForOld.get(oldTripPattern)) {

      List<TripSchedule> newTripSchedules = new ArrayList<>();
      // The TripTimes are not sorted by departure time in the source timetable because
      // OTP1 performs a simple/ linear search. Raptor results depend on trips being
      // sorted. We reuse the same timetables many times on different days, so cache the
      // sorted versions to avoid repeated compute-intensive sorting. Anecdotally this
      // reduces mapping time by more than half, but it is still rather slow. NL Mapping
      // takes 32 seconds sorting every timetable, 9 seconds with cached sorting, and 6
      // seconds with no timetable sorting at all.
      List<TripTimes> sortedTripTimes = sortedTripTimesForTimetable.computeIfAbsent(timetable,
          TransitLayerMapper::getSortedTripTimes
      );
      for (TripTimes tripTimes : sortedTripTimes) {
        if (!serviceCodesRunning.contains(tripTimes.serviceCode)) {
          continue;
        }
        if (tripTimes.getRealTimeState() == RealTimeState.CANCELED) {
          continue;
        }
        if (!tripTimes.trip.getTransitMode().equals(newTripPattern.getTransitMode())) {
          continue;
        }
        TripSchedule tripSchedule = tripScheduleForTripTimes.computeIfAbsent(
            tripTimes,
            // The following are two alternative implementations of TripSchedule
            tt -> new TripScheduleWrapperImpl(tt, oldTripPattern)
            // tt -> tt.toTripSchedulImpl(oldTripPattern)
        );
        newTripSchedules.add(tripSchedule);
      }

      if (newTripSchedules.isEmpty()) {
        if (timetable.serviceDate == serviceDate) {
          LOG.debug(
              "Tried to update TripPattern {}, but no service codes are valid for date {}",
              timetable.pattern.getId(),
              serviceDate
          );
        }
        return null;
      }

      tripPatternsForDate.add(new TripPatternForDate(newTripPattern,
          newTripSchedules,
          ServiceCalendarMapper.localDateFromServiceDate(serviceDate))
      );
    }

    return tripPatternsForDate;
  }
}
