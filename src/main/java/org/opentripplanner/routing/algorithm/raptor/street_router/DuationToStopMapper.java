package org.opentripplanner.routing.algorithm.raptor.street_router;

import org.opentripplanner.model.Stop;
import com.conveyal.r5.profile.entur.api.StopArrival;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DuationToStopMapper {
    private TransitLayer transitLayer;

    public DuationToStopMapper(TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
    }

    public Collection<StopArrival> map(Map<Stop, Transfer> input, double walkSpeed) {
        List result = new ArrayList();
        for (Map.Entry<Stop, Transfer> entry : input.entrySet()) {
            Stop stop = entry.getKey();
            int duration = (int)Math.floor((entry.getValue()).distance / 1000.0 / walkSpeed); //TODO: Avoid hard coding walk speed
            int stopIndex = transitLayer.getIndexByStop(stop);
            StopArrival arrivalTimeAtStop = new StopArrival() {
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
