package org.opentripplanner.transit.raptor.api;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class TestLeg implements RaptorTransfer {
    private final int stop;
    private final int durationInSeconds;

    public TestLeg(int stop, int durationInSeconds) {
        this.stop = stop;
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    public int stop() {
        return stop;
    }

    @Override
    public int durationInSeconds() {
        return durationInSeconds;
    }
}
