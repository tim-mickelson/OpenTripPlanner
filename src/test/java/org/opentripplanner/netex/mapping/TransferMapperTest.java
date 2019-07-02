package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

import static org.junit.Assert.*;

public class TransferMapperTest {

        private static final String FROM_TRIP = "RUT:ServiceJourney:1";
        private static final String TO_TRIP = "RUT:ServiceJourney:2";
        private static final String FROM_STOP = "NSR:Quay:1";
        private static final String TO_STOP = "NSR:Quay:2";
        private static final String FROM_STOP_POINT = "NSR:StopPointInJourneyPattern:1";
        private static final String TO_STOP_POINT = "NSR:StopPointInJourneyPattern:2";

        @Test
        public void mapTimedTransfer() {
                ServiceJourneyInterchange interchange =
                        new ServiceJourneyInterchange()
                                .withFromJourneyRef(
                                        new VehicleJourneyRefStructure().withRef(FROM_TRIP))
                                .withToJourneyRef(
                                        new VehicleJourneyRefStructure().withRef(TO_TRIP))
                                .withFromPointRef(
                                        new ScheduledStopPointRefStructure().withRef(FROM_STOP_POINT))
                                .withToPointRef(
                                        new ScheduledStopPointRefStructure().withRef(TO_STOP_POINT)
                                );

                NetexImportDataIndex netexIndex = new NetexImportDataIndex();
                netexIndex.quayIdByStopPointRef.add(FROM_STOP_POINT, FROM_STOP);
                netexIndex.quayIdByStopPointRef.add(TO_STOP_POINT, TO_STOP);

                OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

                Stop fromStop = new Stop();
                fromStop.setId(FeedScopedIdFactory.createFeedScopedId(FROM_STOP));
                transitBuilder.getStops().add(fromStop);
                Stop toStop = new Stop();
                toStop.setId(FeedScopedIdFactory.createFeedScopedId(TO_STOP));
                transitBuilder.getStops().add(toStop);

                Trip fromTrip = new Trip();
                fromTrip.setId(FeedScopedIdFactory.createFeedScopedId(FROM_TRIP));
                transitBuilder.getTripsById().add(fromTrip);
                Trip toTrip = new Trip();
                toTrip.setId(FeedScopedIdFactory.createFeedScopedId(TO_TRIP));
                transitBuilder.getTripsById().add(toTrip);

                Transfer transfer = TransferMapper.map(
                        interchange,
                        transitBuilder,
                        netexIndex
                );

                assertEquals(transfer.getFromTrip().getId().getId(), FROM_TRIP);
                assertEquals(transfer.getToTrip().getId().getId(), TO_TRIP);
                assertEquals(transfer.getFromStop().getId().getId(), FROM_STOP);
                assertEquals(transfer.getToStop().getId().getId(), TO_STOP);
                assertEquals(1, transfer.getTransferType());
        }
}