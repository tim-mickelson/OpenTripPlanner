/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Trip extends TransitEntity<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private Route route;

    private Operator operator;

    private FeedScopedId serviceId;

    private String tripShortName;

    private String tripHeadsign;

    private String routeShortName;

    private String directionId;

    private String blockId;

    private FeedScopedId shapeId;

    private int wheelchairAccessible = 0;

    private TransitMode transitMode;

    @Deprecated private int tripBikesAllowed = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private int bikesAllowed = 0;

    /** Custom extension for KCM to specify a fare per-trip */
    private String fareId;

    public Trip() {
    }

    public Trip(Trip obj) {
        this.id = obj.id;
        this.route = obj.route;
        this.operator = obj.operator;
        this.serviceId = obj.serviceId;
        this.tripShortName = obj.tripShortName;
        this.tripHeadsign = obj.tripHeadsign;
        this.routeShortName = obj.routeShortName;
        this.directionId = obj.directionId;
        this.blockId = obj.blockId;
        this.shapeId = obj.shapeId;
        this.wheelchairAccessible = obj.wheelchairAccessible;
        this.tripBikesAllowed = obj.tripBikesAllowed;
        this.bikesAllowed = obj.bikesAllowed;
        this.fareId = obj.fareId;
        this.transitMode = obj.transitMode;
    }

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }


    /**
     * Operator running the trip. Returns operator of this trip, if it exist, or else the route operator.
     */
    public Operator getOperator() {
        return operator != null ? operator : route.getOperator();
    }

    /**
     * This method return the operator associated with the trip. If the Trip have no Operator set {@code null} is
     * returned. Note! this method do not consider the {@link Route} that the trip is part of.
     * @see #getOperator()
     */
    public Operator getTripOperator() {
        return operator;
    }

    public void setTripOperator(Operator operator) {
        this.operator = operator;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public FeedScopedId getServiceId() {
        return serviceId;
    }

    public void setServiceId(FeedScopedId serviceId) {
        this.serviceId = serviceId;
    }

    public String getTripShortName() {
        return tripShortName;
    }

    public void setTripShortName(String tripShortName) {
        this.tripShortName = tripShortName;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public void setTripHeadsign(String tripHeadsign) {
        this.tripHeadsign = tripHeadsign;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public String getDirectionId() {
        return directionId;
    }

    public void setDirectionId(String directionId) {
        this.directionId = directionId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public FeedScopedId getShapeId() {
        return shapeId;
    }

    public void setShapeId(FeedScopedId shapeId) {
        this.shapeId = shapeId;
    }

    public void setWheelchairAccessible(int wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public int getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    @Deprecated
    public void setTripBikesAllowed(int tripBikesAllowed) {
        this.tripBikesAllowed = tripBikesAllowed;
    }

    @Deprecated
    public int getTripBikesAllowed() {
        return tripBikesAllowed;
    }

    /**
     * @return 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public int getBikesAllowed() {
        return bikesAllowed;
    }

    /**
     * @param bikesAllowed 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public void setBikesAllowed(int bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public String toString() {
        return "<Trip " + getId() + ">";
    }

    public String getFareId() {
        return fareId;
    }

    public void setFareId(String fareId) {
        this.fareId = fareId;
    }

    public TransitMode getTransitMode() {
        return transitMode != null ? transitMode : route.getTransitMode();
    }

    public void setTransitMode(TransitMode transitMode) {
        this.transitMode = transitMode;
    }
}
