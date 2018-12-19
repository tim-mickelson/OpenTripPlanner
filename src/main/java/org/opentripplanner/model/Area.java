/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Area area = (Area) o;
        return id.equals(area.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
