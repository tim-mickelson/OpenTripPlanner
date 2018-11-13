package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import java.util.ArrayList;
import java.util.List;

public class MergeTripPatternForDates {
    public static List<TripPatternForDates> merge(List<TripPatternForDates> left, List<TripPatternForDate> right) {
        List<TripPatternForDates> combinedList = new ArrayList<>();

        int dayOffset = !left.isEmpty() ? left.get(0).getTripSchedules().size() : 0;

        int leftIndex = 0;
        int rightIndex = 0;

        while (leftIndex < left.size() && rightIndex < right.size()) {
            TripPatternForDates leftItem = left.get(leftIndex);
            TripPatternForDate rightItem = right.get(rightIndex);

            int compareResult = Integer.compare(leftItem.getTripPattern().getId(), rightItem.getTripPattern().getId());

            if (compareResult == 0) {
                combinedList.add(combine(left.get(leftIndex), right.get(rightIndex)));
                leftIndex++;
                rightIndex++;
            } else if (compareResult > 0 ) {
                combinedList.add(combine(left.get(leftIndex), 1));
                leftIndex++;
            } else {
                combinedList.add(combine(dayOffset, right.get(rightIndex)));
                rightIndex++;
            }
        }

        for(;leftIndex<left.size();++leftIndex) {
            combinedList.add(combine(left.get(leftIndex), 1));
            leftIndex++;
        }
        for(;rightIndex<right.size();++rightIndex) {
            combinedList.add(combine(dayOffset, right.get(rightIndex)));
            rightIndex++;
        }

        return combinedList;
    }

    private static TripPatternForDates combine(TripPatternForDates tripPatternForDates, TripPatternForDate tripPatternForDate) {
        tripPatternForDates.getTripSchedules().add(tripPatternForDate.getTripSchedules());
        return tripPatternForDates;
    }

    private static TripPatternForDates combine(int dayOffset, TripPatternForDate tripPatternForDate) {
        List<List<TripSchedule>> tripSchedulesList = new ArrayList<>();
        for (int i = 0; i < dayOffset; i++) {
            tripSchedulesList.add(new ArrayList<>());
        }
        tripSchedulesList.add(tripPatternForDate.getTripSchedules());
        return new TripPatternForDates(tripPatternForDate.getTripPattern(), tripSchedulesList);
    }

    private static TripPatternForDates combine(TripPatternForDates tripPatternForDates, int dayOffset) {
        for (int i = 0; i < dayOffset; i++) {
            tripPatternForDates.getTripSchedules().add(new ArrayList<>());
        }
        return tripPatternForDates;
    }
}
