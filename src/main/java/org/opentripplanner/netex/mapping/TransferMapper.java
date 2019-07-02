package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * This maps the NeTEx element ServiceJourneyInterchange to the OTP transfer object.
 * A ServiceJourneyInterchange always contains a FromPointRef, ToPointRef, FromJourneyRef, and
 * ToJourneyRef. The first two refer to ScheduledStopPoints but are mapped to stops. This means
 * that in the case of ring routes (where the same stop is passed more than once), information about
 * which passing of the stop is referred to is lost.
 *
 * Because FromJourneyRef and ToJourneyRef are provided, we map to specific transfers defined in
 * GTFS extensions.
 *
 * NeTEx interchanges and GTFS transfer have different properties that we are not able to map. We
 * are not able to map "StaySeated", "Guaranteed" and "MaximumWaitTime" without expanding the OTP
 * model. NeTEx interchanges seem to most closely resemble GTFS transfer_type 1, so we hard code
 * that value. There is no interchange value corresponding to min_transfer_time, so it is not set.
 */
class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    private TransferMapper() {}

    static Transfer map(
            ServiceJourneyInterchange interchange,
            OtpTransitServiceBuilder transitBuilder,
            NetexImportDataIndex netexIndex
    ) {
        Transfer transfer = new Transfer();

        // NeTEx interchanges are assumed to be the same as GTFS timed transfers
        transfer.setTransferType(1);

        String fromStopId = netexIndex.quayIdByStopPointRef.lookup(
                interchange.getFromPointRef().getRef()
        );
        String toStopId = netexIndex.quayIdByStopPointRef.lookup(
                interchange.getToPointRef().getRef()
        );

        transfer.setFromStop(transitBuilder.getStops().get(createFeedScopedId(fromStopId)));
        transfer.setToStop(transitBuilder.getStops().get(createFeedScopedId(toStopId)));

        transfer.setFromTrip(
                transitBuilder.getTripsById().get(
                        createFeedScopedId(interchange.getFromJourneyRef().getRef())
                )
        );
        transfer.setToTrip(
                transitBuilder.getTripsById().get(
                        createFeedScopedId(interchange.getToJourneyRef().getRef())
                )
        );

        if (
                transfer.getFromTrip() == null || transfer.getToTrip() == null
                || transfer.getToStop() == null || transfer.getFromStop() == null

        ) {
            LOG.warn("Incomplete trip or stop information for transfer: {}", interchange.getId());
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        return transfer;
    }
}
