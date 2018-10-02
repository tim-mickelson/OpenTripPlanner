package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import java.time.LocalDate;
import java.util.Set;

public class Service {
    private int serviceId;
    private Set<LocalDate> activeDates;

    public Service(int serviceId, Set<LocalDate> activeDates) {
        this.serviceId = serviceId;
        this.activeDates = activeDates;
    }

    public boolean activeOn(LocalDate date) {
        return activeDates.contains(date);
    }
}
