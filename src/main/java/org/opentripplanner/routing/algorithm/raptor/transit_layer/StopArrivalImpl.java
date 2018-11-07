package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.conveyal.r5.profile.entur.api.StopArrival;

public class StopArrivalImpl implements StopArrival {

    private final int duration;

    private final Transfer transfer;

    StopArrivalImpl(Transfer transfer, double walkSpeed) {
        this.transfer = transfer;
        this.duration = (int) Math.round(transfer.getDistance() / walkSpeed); // TODO: Fix calculation
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
        return transfer.cost(); // TODO: Fix
    }
}
