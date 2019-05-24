package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.model.FeedScopedId.convertFromString;

class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransferMapper.class);

    Transfer mapTransfer(ServiceJourneyInterchange interchange,
            OtpTransitServiceBuilder transitBuilder, NetexImportDataIndex netexIndex) {
        Transfer transfer = new Transfer();

        transfer.setTransferType(1);

        String fromStopId = netexIndex.quayIdByStopPointRef.lookup(interchange.getFromPointRef().getRef());
        String toStopId = netexIndex.quayIdByStopPointRef.lookup(interchange.getToPointRef().getRef());

        transfer.setFromStop(
                transitBuilder.getStops().get(convertFromString(fromStopId)));
        transfer.setToStop(
                transitBuilder.getStops().get(convertFromString(toStopId)));

        transfer.setFromTrip(
                transitBuilder.getTripsById().get(convertFromString(
                        interchange.getFromJourneyRef().getRef())
                )
        );
        transfer.setToTrip(transitBuilder.getTripsById()
                .get(convertFromString(interchange.getToJourneyRef().getRef())));

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null) {
            LOG.warn("Trips not found for transfer " + interchange.getId());
        }

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null
                || transfer.getToStop() == null || transfer.getFromStop() == null) {
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        return transfer;
    }
}
