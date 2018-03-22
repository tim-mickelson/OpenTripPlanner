package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.ShapePoint;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServiceLinkMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    public Collection<ShapePoint> mapServiceLink(ServiceLink serviceLink) {
        Collection<ShapePoint> shapePoints = new ArrayList<>();

        if (serviceLink.getProjections() == null || serviceLink.getProjections().getProjectionRefOrProjection() == null) {
            LOG.warn("Ignore service link without projection: " + serviceLink);
        } else {
            for (JAXBElement<?> projectionElement : serviceLink.getProjections().getProjectionRefOrProjection()) {
                Object projectionObj = projectionElement.getValue();
                if (projectionObj instanceof LinkSequenceProjection_VersionStructure) {
                    LinkSequenceProjection_VersionStructure linkSequenceProjection = (LinkSequenceProjection_VersionStructure) projectionObj;
                    if (linkSequenceProjection.getLineString() != null) {
                        List<Double> coordinates = linkSequenceProjection.getLineString().getPosList().getValue();
                        for (int i = 0; i < coordinates.size(); i += 2) {
                            ShapePoint shapePoint = new ShapePoint();
                            shapePoint.setShapeId(AgencyAndIdFactory.createAgencyAndId(serviceLink.getId()));
                            shapePoint.setLat(coordinates.get(i));
                            shapePoint.setLon(coordinates.get(i + 1));
                            shapePoint.setSequence(i / 2);
                            shapePoints.add(shapePoint);
                        }
                    } else {
                        LOG.warn("Ignore linkSequenceProjection without linestring for: " + linkSequenceProjection.toString());
                    }
                }
            }
        }

        return shapePoints;
    }
}
