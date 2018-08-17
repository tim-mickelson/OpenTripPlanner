package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class TripPattern {
    public List<TripSchedule> tripSchedules = new ArrayList<>();

    public TraverseMode transitModesSet;

    public int[] stopPattern;

    public BitSet containsServices;

    public boolean hasSchedules;
}
