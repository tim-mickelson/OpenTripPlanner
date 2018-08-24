package org.opentripplanner.routing.algorithm.raptor.street_router;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Map;

public class AccessEgressMapper {
    public static TIntIntMap map(Map<Vertex, Transfer> transferMap, TransitLayer transitLayer) {
        TIntIntMap timeToStopInSeconds = new TIntIntHashMap();

        for (Map.Entry entry : transferMap.entrySet()) {
            Vertex stop = (TransitStop) entry.getKey();
            timeToStopInSeconds.put(transitLayer.getIndexByStop(((TransitStop) stop).getStop()), ((Transfer)entry.getValue()).duration);

            System.out.println(((Transfer)entry.getValue()).duration);
        }

        return timeToStopInSeconds;
    }
}
