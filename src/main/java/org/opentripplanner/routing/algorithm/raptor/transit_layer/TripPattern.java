package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.Pattern;
import org.opentripplanner.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class TripPattern {
    public List<TripSchedule> tripSchedules = new ArrayList<>();

    public TraverseMode transitMode;

    public TransmodelTransportSubmode transitSubMode;

    public int[] stopPattern;

    public BitSet containsServices;

    public boolean hasSchedules;
}
