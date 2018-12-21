package org.opentripplanner.netex.mapping;

import net.opengis.gml._3.LinearRingType;
import org.opentripplanner.model.Area;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.HailAndRideArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FlexibleStopPlaceMapper extends StopMapper {

    private static final Logger LOG = LoggerFactory.getLogger(FlexibleStopPlaceMapper.class);

    private FlexibleStopPlaceTypeMapper flexibleStopPlaceTypeMapper = new FlexibleStopPlaceTypeMapper();

    public void mapFlexibleStopPlaceWithQuay(FlexibleStopPlace flexibleStopPlace, OtpTransitBuilder transitBuilder) {
        Stop quay = new Stop();
        Area area = new Area();
        quay.setStopType(Stop.stopTypeEnumeration.FLEXIBLE_AREA);
        quay.setId(AgencyAndIdFactory.createAgencyAndId(createQuayIdForStopPlaceId(flexibleStopPlace.getId())));
        // StopPlace maps to parent stop (location type 1)
        quay.setLocationType(0);
        quay.setLat(58.2176318);
        quay.setLon(10.9356465);

        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null){
            quay.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            quay.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.warn(flexibleStopPlace.getId() + " does not contain any coordinates.");
        }

        quay.setVehicleType(flexibleStopPlaceTypeMapper.getTransportMode(flexibleStopPlace));
        quay.setTimezone(super.DEFAULT_TIMEZONE);
        quay.setName(flexibleStopPlace.getName().getValue());
        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null){
            quay.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            quay.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else {
            LOG.warn(flexibleStopPlace.getId() + " does not contain any coordinates.");
        }
        if (flexibleStopPlace.getAreas() != null && flexibleStopPlace.getAreas().getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea() != null) {
            List<Object> areas = flexibleStopPlace.getAreas().getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea();
            if (areas.size() != 1) {
                LOG.info("{} areas found. Only the first area (if present) will be mapped.");
            }
            if (!areas.isEmpty()) {
                if (areas.get(0) instanceof FlexibleArea) {
                    FlexibleArea flexibleArea = (FlexibleArea)areas.get(0);
                    area = mapFlexibleArea(flexibleArea);
                } else if (areas.get(0) instanceof HailAndRideArea) {
                    LOG.info("StopPlace {} contains a hail and ride area which is not supported.", flexibleStopPlace.getId());
                }
            }
        }

        Stop stopPlace = new Stop();
        stopPlace.setName(flexibleStopPlace.getName().getValue());
        // Map coordinates
        if(flexibleStopPlace.getCentroid() != null){
            stopPlace.setLat(flexibleStopPlace.getCentroid().getLocation().getLatitude().doubleValue());
            stopPlace.setLon(flexibleStopPlace.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.warn(flexibleStopPlace.getId() + " does not contain any coordinates.");
        }
        stopPlace.setId(AgencyAndIdFactory.createAgencyAndId(flexibleStopPlace.getId()));
        stopPlace.setLocationType(1);
        quay.setParentStation(stopPlace.getId().toString());
        quay.setArea(area);

        transitBuilder.getFlexibleQuayWithArea().add(new FlexibleQuayWithArea(quay, area));
        transitBuilder.getAreas().add(area);
        transitBuilder.getStops().add(quay);
        transitBuilder.getStops().add(stopPlace);
    }

    private Area mapFlexibleArea(FlexibleArea flexibleArea) {
        Area area = new Area();
        area.setId(AgencyAndIdFactory.createAgencyAndId(flexibleArea.getId()));
        area.setWkt(mapPolygon(((LinearRingType)(flexibleArea.getPolygon().getExterior().getAbstractRing().getValue())).getPosList().getValue()));
        return area;
    }

    private String mapPolygon(List<Double> coordinates) {
        StringBuilder wktPolygon = new StringBuilder();
        wktPolygon.append("POLYGON((");
        for (int i = 0; i < coordinates.size(); i += 2) {
            wktPolygon.append(coordinates.get(i));
            wktPolygon.append(" ");
            wktPolygon.append(coordinates.get(i + 1));
            if (i < coordinates.size() - 2) {
                wktPolygon.append(", ");
            }
        }
        wktPolygon.append("))");
        return wktPolygon.toString();
    }

    private String createQuayIdForStopPlaceId(String stopPlaceId) {
        return stopPlaceId.replace("FlexibleStopPlace", "FlexibleQuay");
    }
}
