package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.Set;

/**
 * This maps a NeTEx ServiceJourney to an OTP Trip. A ServiceJourney can be connected to a Line (OTP
 * Route) in two ways. Either directly from the ServiceJourney or through JourneyPattern → Route.
 * The former has precedent over the latter.
 */
class TripMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

    private final FeedScopedIdFactory idFactory;
    private EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById;
    private ReadOnlyHierarchicalMap<String, Route> routeById;
    private ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternsById;
    private TransportModeMapper transportModeMapper = new TransportModeMapper();
    private final Set<FeedScopedId> shapePointIds;

    TripMapper(
            FeedScopedIdFactory idFactory,
            EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById,
            ReadOnlyHierarchicalMap<String, Route> routeById,
      ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternsById,
      Set<FeedScopedId> shapePointIds
    ) {
        this.idFactory = idFactory;
        this.otpRouteById = otpRouteById;
        this.routeById = routeById;
        this.journeyPatternsById = journeyPatternsById;
    this.shapePointIds = shapePointIds;
    }

    /**
     * Map a service journey to a trip.
     * <p>
   *
     * @return valid trip or {@code null} if unable to map to a valid trip.
     */
    Trip mapServiceJourney(ServiceJourney serviceJourney){
        org.opentripplanner.model.Route route = resolveRoute(serviceJourney);
        String serviceId = resolveServiceId(serviceJourney);

        if(serviceId == null || route == null) {
            return null;
        }

        Trip trip = new Trip();
        trip.setId(idFactory.createId(serviceJourney.getId()));
        trip.setRoute(route);
        trip.setServiceId(idFactory.createId(serviceId));
        trip.setShapeId(getShapeId(serviceJourney));

        TransitMode transitMode = new TransitMode(
            route.getTransitMode() != null ? route.getTransitMode().getMode() : null,
            transportModeMapper.getTransportSubmode(serviceJourney.getTransportSubmode())
        );

        // Only map ModeAndSubmode to Trip if it differs from the Route
        if (transitMode.getSubmode() != null &&
            !transitMode.getSubmode().equals(route.getTransitMode().getSubmode())) {
            trip.setTransitMode(transitMode);
        }

        return trip;
    }

    private FeedScopedId getShapeId(ServiceJourney serviceJourney) {
        JourneyPattern journeyPattern = journeyPatternsById.lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
    FeedScopedId serviceLinkId = journeyPattern != null ? idFactory.createId(journeyPattern
        .getId().replace("JourneyPattern", "ServiceLink")) : null;

    return shapePointIds.contains(serviceLinkId) ? serviceLinkId : null;
    }

    private String resolveServiceId(ServiceJourney serviceJourney) {
        String serviceId =  DayTypeRefsToServiceIdAdapter.createServiceId(serviceJourney.getDayTypes());
        if(serviceId == null) {
            LOG.warn(
                    "Not able to generate serviceId for ServiceJourney, dayTypes is empty. ServiceJourney id: {}",
                    serviceJourney.getId()
            );
        }
        return serviceId;
    }

    private org.opentripplanner.model.Route resolveRoute(ServiceJourney serviceJourney) {
        String lineRef = null;
        // Check for direct connection to Line
        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        if (lineRefStruct != null){
            // Connect to Line referenced directly from ServiceJourney
            lineRef = lineRefStruct.getValue().getRef();
        } else if(serviceJourney.getJourneyPatternRef() != null){
            // Connect to Line referenced through JourneyPattern->Route
      JourneyPattern journeyPattern = journeyPatternsById.lookup(serviceJourney
          .getJourneyPatternRef()
          .getValue()
          .getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = routeById.lookup(routeRef).getLineRef().getValue().getRef();
        }
        org.opentripplanner.model.Route route = otpRouteById.get(idFactory.createId(lineRef));

        if(route == null) {
            LOG.warn(
                    "Unable to link ServiceJourney to Route. ServiceJourney id: "
                    + serviceJourney.getId()
                    + ", Line ref: " + lineRef
            );
        }
        return route;
    }
}
