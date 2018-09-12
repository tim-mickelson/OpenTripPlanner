package org.opentripplanner.routing.algorithm.raptor.street_router;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.mcrr.api.ArrivalTimeAtStop;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AccessEgressToStopStatesMapper {
    private TransitLayer transitLayer;

    public void AccessEgressToStopStatesMapper(TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
    }

    public Collection<ArrivalTimeAtStop> map(Map<TransitStop, Transfer> input) {
        List result = new ArrayList();
        for (Map.Entry entry : input.entrySet()) {
            Stop stop = ((TransitStop)entry.getKey()).getStop();
            int time = ((Transfer)entry.getValue()).distance;
            int stopIndex = transitLayer.getIndexByStop(stop);
            ArrivalTimeAtStop arrivalTimeAtStop = new ArrivalTimeAtStop(stopIndex, time);
            result.add(arrivalTimeAtStop);
        }
        return result;
    }
}
