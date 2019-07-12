package org.opentripplanner.graph_builder.services;

import com.goebl.simplify.Point;

public class MyPoint implements Point {
        private final double lat;
        private final double lng;

        public MyPoint(double lat, double lng) {
                this.lat = lat;
                this.lng = lng;
        }

        public double getY() {
                return lat;
        }

        public double getX() {
                return lng;
        }
}
