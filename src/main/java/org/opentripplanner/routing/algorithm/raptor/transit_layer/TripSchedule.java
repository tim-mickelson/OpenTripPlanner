package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.TripScheduleInfo;
import org.opentripplanner.model.Trip;

/**
 * Extension of TripScheduleInfo passed through Range Raptor searches to be able to retrieve the original trip
 * from the path when creating itineraries.
 */

public interface TripSchedule extends TripScheduleInfo {
    public Trip getOriginalTrip();
}
