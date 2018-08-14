package org.opentripplanner.gtfs;

import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.model.impl.OtpTransitBuilder;

public interface GtfsContext {
    GtfsFeedId getFeedId();
    OtpTransitBuilder getTransitBuilder();
}
