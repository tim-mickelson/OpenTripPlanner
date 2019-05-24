package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.PresentationStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Maps NeTEx line to OTP Route.
 */
class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
    private final TransportModeMapper transportModeMapper = new TransportModeMapper();

    org.opentripplanner.model.Route mapRoute(
            Line line,
            OtpTransitServiceBuilder transitBuilder,
            NetexImportDataIndex netexIndex, String timeZone
    ){
        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        otpRoute.setId(FeedScopedIdFactory.createFeedScopedId(line.getId()));
        otpRoute.setAgency(findAuthorityForRoute(transitBuilder, netexIndex, line, timeZone));
        otpRoute.setOperator(findLineOperator(line, transitBuilder));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportModeMapper.getTransportMode(line.getTransportMode(), line.getTransportSubmode()));

        if (line.getPresentation() != null) {
            PresentationStructure presentation = line.getPresentation();
            if (presentation.getColour() != null) {
                otpRoute.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            }
            if (presentation.getTextColour() != null) {
                otpRoute.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
            }
        }

        return otpRoute;
    }

    /**
     * Find an agency by mapping the GroupOfLines/Network Authority. If no authority is found
     * a default agency is created and returned.
     */
    private Agency findAuthorityForRoute(
            OtpTransitServiceBuilder transitBuilder,
            NetexImportDataIndex netexIndex,
            Line line,
            String timeZone
    ) {
        String lineId = line.getId();
        // Find authority, first in *Network* and then if not found look in *GroupOfLines*
        Network network = netexIndex.networkByLineId.lookup(lineId);
        GroupOfLines groupOfLines = netexIndex.groupOfLinesByLineId.lookup(lineId);

        // TODO OTP2 - verify that search order: groupOfLines, then network i correct
        Authority authority = netexIndex.lookupAuthority(groupOfLines, network);

        if(authority != null) {
            return transitBuilder.getAgenciesById().get(authority.getId());
        }

        // No authority found in Network or GroupOfLines.
        return getDummyAgency(transitBuilder, lineId, timeZone);
    }

    private Operator findLineOperator(Line line, OtpTransitServiceBuilder transitBuilder) {
        OperatorRefStructure opeRef = line.getOperatorRef();

        if(opeRef != null) {
            return transitBuilder.getOperatorsById().get(FeedScopedIdFactory.createFeedScopedId(opeRef.getRef()));

        }
        return null;
    }

    /**
     * If no authority can be found, use a dummy authority with time zone set, create if necessary.
     */
    private Agency getDummyAgency(OtpTransitServiceBuilder transitBuilder, String lineId, String timeZone) {
        LOG.warn("No authority found for " + lineId);
        Agency agency = AuthorityToAgencyMapper.createDummyAgency(timeZone);
        EntityById<String, Agency> agenciesById = transitBuilder.getAgenciesById();

        if (!agenciesById.containsKey(agency.getId())) {
            agenciesById.add(agency);
        }
        return agency;
    }
}