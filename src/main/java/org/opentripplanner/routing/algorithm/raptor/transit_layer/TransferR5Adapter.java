package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.transit.TransferLeg;

public class TransferR5Adapter implements TransferLeg {

    private final int duration;

    private final Transfer transfer;

    TransferR5Adapter(Transfer transfer, double walkSpeed) {
        this.transfer = transfer;
        this.duration = (int) Math.round(transfer.getDistance() / walkSpeed);

        // TODO - Add cost
    }

    @Override
    public int stop() {
        return transfer.stop();
    }

    @Override
    public int durationInSeconds() {
        return this.duration;
    }

    @Override
    public int cost() {
        return 0;
    }
}
