package org.opentripplanner.routing.algorithm.raptor.street_router;

import com.conveyal.r5.profile.entur.api.transit.AccessLeg;
import com.conveyal.r5.profile.entur.api.transit.EgressLeg;
import com.conveyal.r5.profile.entur.api.transit.TransferLeg;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TransferToAccessEgressLegMapper {
    private TransitLayer transitLayer;

    public TransferToAccessEgressLegMapper(TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
    }

    public <T> Collection<T> map(Map<Stop, Transfer> input, double walkSpeed) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<Stop, Transfer> entry : input.entrySet()) {
            Stop stop = entry.getKey();
            Transfer transfer = entry.getValue();
            int duration = (int)Math.floor(transfer.getDistance() / walkSpeed); //TODO: Avoid hard coding walk speed
            int stopIndex = transitLayer.getIndexByStop(stop);
            // TODO - Calculate som meaningful cost
            result.add((T)new R5TransferLeg(stopIndex, duration, 0));
        }
        return result;
    }


    private static class R5TransferLeg implements AccessLeg, EgressLeg {
        private int stop;
        private int durationInSeconds;
        private int cost;

        private R5TransferLeg(int stop, int durationInSeconds, int cost) {
            this.stop = stop;
            this.durationInSeconds = durationInSeconds;
            this.cost = cost;
        }

        @Override public int stop() {
            return stop;
        }

        @Override public int durationInSeconds() {
            return durationInSeconds;
        }

        @Override public int cost() {
            return cost;
        }
    }
}
