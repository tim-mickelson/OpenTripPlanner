/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public class Area extends IdentityBean<AgencyAndId> {
    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private String wkt;

    public Area() {

    }

    public Area(Area a) {
        this.id = a.id;
        this.wkt = a.wkt;
    }


    public String getAreaId() {
        return id.getId();
    }

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId areaId) {
        this.id = areaId;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

}
