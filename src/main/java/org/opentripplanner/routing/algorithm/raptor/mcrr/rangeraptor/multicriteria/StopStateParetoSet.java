package org.opentripplanner.routing.algorithm.raptor.mcrr.rangeraptor.multicriteria;

import org.opentripplanner.routing.algorithm.raptor.mcrr.util.ParetoDominanceFunctions;
import org.opentripplanner.routing.algorithm.raptor.mcrr.util.ParetoSet;

import java.util.Iterator;
import java.util.function.Predicate;

class StopStateParetoSet extends ParetoSet<McStopState> {

    StopStateParetoSet(ParetoDominanceFunctions.Builder function) {
        super(function);
    }



    Iterable<? extends McStopState> listRound(int round) {
        return list(it -> it.round() == round);
    }

    Iterable<? extends McStopState> list(Predicate<McStopState> test) {
        return () -> new Iterator<McStopState>() {
            private int index = 0;
            private McStopState it;


            @Override
            public boolean hasNext() {
                while (index < size() ) {
                    it = get(index);
                    ++index;
                    if (test.test(it)) {
                        return true;
                    }
                }
                return false;
            }
            @Override public McStopState next() { return it; }
        };
    }
}
