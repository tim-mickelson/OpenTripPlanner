package org.opentripplanner.graph_builder.module;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.model.TripPattern;

import java.util.HashMap;
import java.util.Map;

public class BestStopAtDistancePerPatternCombination {
  private Map<Pair<TripPattern, TripPattern>, NearbyStopFinder.StopAtDistance> bestTransfers = new HashMap<>();

  public void putStopAtDistance(TripPattern fromPattern, NearbyStopFinder.StopAtDistance stopAtDistance) {
    Pair<TripPattern, TripPattern> patternPair = new ImmutablePair<>(fromPattern, stopAtDistance.tripPattern);
    NearbyStopFinder.StopAtDistance previous = bestTransfers.get(patternPair);
    if (previous == null || stopAtDistance.distance < previous.distance) {
      bestTransfers.put(patternPair, stopAtDistance);
    }
  }

  public Map<Pair<TripPattern, TripPattern>, NearbyStopFinder.StopAtDistance> getBestTransfers() {
    return bestTransfers;
  }
}
