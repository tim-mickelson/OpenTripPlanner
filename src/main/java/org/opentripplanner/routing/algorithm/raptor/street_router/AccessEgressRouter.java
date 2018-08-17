package org.opentripplanner.routing.algorithm.raptor.street_router;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessEgressRouter {
    private static Logger LOG = LoggerFactory.getLogger(InterleavedBidirectionalHeuristic.class);

    public static TObjectDoubleMap<Vertex> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime) {
        BinHeap<Vertex> transitQueue = new BinHeap<>();
        double maxWeightSeen = 0;
        LOG.debug("Heuristic street search around the {}.", fromTarget ? "target" : "origin");
        rr = rr.clone();
        if (fromTarget) {
            rr.setArriveBy(!rr.arriveBy);
        }
        boolean stopReached = false;
        // Create a map that returns Infinity when it does not contain a vertex.
        TObjectDoubleMap<Vertex> vertices = new TObjectDoubleHashMap<>(100, 0.5f, Double.POSITIVE_INFINITY);
        ShortestPathTree spt = new DominanceFunction.MinimumWeight().getNewShortestPathTree(rr);
        // TODO use normal OTP search for this.
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                return null;
            }
            State s = pq.extract_min();
            Vertex v = s.getVertex();

            boolean initialStop = v instanceof TransitStop && s.backEdge != null && s.backEdge instanceof StationStopEdge;

            // At this point the vertex is closed (pulled off heap).
            // This is the lowest cost we will ever see for this vertex. We can record the cost to reach it.
            if (v instanceof TransitStop) {
                // We don't want to continue into the transit network yet, but when searching around the target
                // place vertices on the transit queue so we can explore the transit network backward later.
                if (fromTarget) {
                    double weight = s.getWeight();
                    transitQueue.insert(v, weight);
                    if (weight > maxWeightSeen) {
                        maxWeightSeen = weight;
                    }
                }
                if (!stopReached) {
                    stopReached = true;
                    rr.softWalkLimiting = false;
                    rr.softPreTransitLimiting = false;
                    if (s.walkDistance > rr.maxWalkDistance) {
                        // Add 300 meters in order to search for nearby stops
                        rr.maxWalkDistance = s.walkDistance + 300;
                    }
                }
                if (!initialStop) continue;
            }
            // We don't test whether we're on an instanceof StreetVertex here because some other vertex types
            // (park and ride or bike rental related) that should also be explored and marked as usable.
            // Record the cost to reach this vertex.
            if (!vertices.containsKey(v) && !(v instanceof TransitStop)) {
                vertices.put(v, (int) s.getWeight()); // FIXME time or weight? is RR using right mode?
            }
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                if (v instanceof TransitStop && !(e instanceof StreetTransitLink)) {
                    continue;
                }
                // arriveBy has been set to match actual directional behavior in this subsearch.
                // Walk cutoff will happen in the street edge traversal method.
                State s1 = e.traverse(s);
                if (s1 == null) {
                    continue;
                }
                if (spt.add(s1)) {
                    pq.insert(s1,  s1.getWeight());
                }
            }
        }
        LOG.debug("Heuristric street search hit {} vertices.", vertices.size());
        LOG.debug("Heuristric street search hit {} transit stops.", transitQueue.size());
        return vertices;
    }
}
