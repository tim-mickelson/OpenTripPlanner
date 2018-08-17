package org.opentripplanner.routing.algorithm.raptor.transit_layer;

/**
 * This represents the arrival and departure times of a single GTFS trip within a TripPattern.
 * If this is a frequency trip, it also records the different headways throughout the day, and when those headways
 * begin and end.
 */
public class TripSchedule {

    public String tripId;

    /**
     * Arrival times in seconds from midnight by stop index
     */
    public int[] arrivals;
    /**
     * Departure times in seconds from midnight by stop index
     */
    public int[] departures;

    public int serviceCode;

    public Integer headwaySeconds;
}
