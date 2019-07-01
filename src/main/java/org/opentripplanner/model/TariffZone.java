package org.opentripplanner.model;

public class TariffZone extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private String name;

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }
}