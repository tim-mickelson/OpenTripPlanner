package org.opentripplanner.graph_builder.services;

import com.goebl.simplify.Point;
import com.goebl.simplify.Simplify;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.util.I18NString;

import java.util.Arrays;
import java.util.Collections;

public class DouglasPeuckerStreetEdgeFactory implements StreetEdgeFactory {

    private boolean useElevationData = false;

    @Override
    public StreetEdge createEdge(IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
            LineString geometry, I18NString name, double length, StreetTraversalPermission permissions,
            boolean back) {
        geometry = simplifyGeometry(geometry, back);
        StreetEdge pse;
        if (useElevationData) {
            pse = new StreetWithElevationEdge(startEndpoint, endEndpoint, geometry, name, length,
                    permissions, back);
        } else {
            pse = new StreetEdge(startEndpoint, endEndpoint, geometry, name, length, permissions,
                    back);
        }
        return pse;
    }

    @Override
    public AreaEdge createAreaEdge(IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, LineString geometry, I18NString name, double length,
            StreetTraversalPermission permissions, boolean back, AreaEdgeList area) {
        // By default AreaEdge are elevation-capable so nothing to do.
        AreaEdge ae = new AreaEdge(startEndpoint, endEndpoint, geometry, name, length, permissions,
                back, area);
        return ae;
    }

    private LineString simplifyGeometry(LineString originalGeometry, boolean reverse) {
        Simplify<Point> simplify = new Simplify<>(new MyPoint[0]);
        MyPoint[] allPoints = Arrays
                .stream(originalGeometry.getCoordinates()).map(t -> new MyPoint(t.y, t.x)).toArray(MyPoint[]::new);

        if (reverse) {
            Collections.reverse(Arrays.asList(allPoints));
        }

        Point[] lessPoints = simplify.simplify(allPoints, 0.0001, true);
        Coordinate[] coordinates = Arrays
                .stream(lessPoints).map(t -> new Coordinate(t.getX(), t.getY())).toArray(Coordinate[]::new);

        if (reverse) {
            Collections.reverse(Arrays.asList(coordinates));
        }

        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    @Override public void setUseEleveationData(boolean useEleveationData) {
        this.useElevationData = useEleveationData;
    }
}
