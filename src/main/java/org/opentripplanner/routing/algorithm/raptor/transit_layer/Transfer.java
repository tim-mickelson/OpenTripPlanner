package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public class Transfer {
    private int stop;

    private final int distance;

    private final int cost;

    private final List<Coordinate> coordinates;

    public Transfer(int stop, int distance, int cost, List<Coordinate> coordinates) {
        this.stop = stop;
        this.distance = distance;
        this.cost = cost;
        this.coordinates = coordinates;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public int stop() { return stop; }

    public int cost() {
        return cost;
    }

    public int getDistance() {
        return distance;
    }
}
