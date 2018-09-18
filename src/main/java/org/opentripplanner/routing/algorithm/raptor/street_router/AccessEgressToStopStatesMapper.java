package org.opentripplanner.routing.algorithm.raptor.street_router;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.DurationToStop;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AccessEgressToStopStatesMapper {
    private TransitLayer transitLayer;

    public AccessEgressToStopStatesMapper(TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
    }

    public Collection<DurationToStop> map(Map<TransitStop, Transfer> input) {
        List result = new ArrayList();
        for (Map.Entry entry : input.entrySet()) {
            Stop stop = ((TransitStop)entry.getKey()).getStop();
            int duration = (int)Math.floor(((Transfer)entry.getValue()).distance / 1000.0 / 1.33);
            int stopIndex = transitLayer.getIndexByStop(stop);
            DurationToStop arrivalTimeAtStop = new DurationToStop() {
                @Override
                public int stop() {
                    return stopIndex;
                }

                @Override
                public int durationInSeconds() {
                    return duration;
                }
            };
            result.add(arrivalTimeAtStop);
        }
        return result;
    }
}
