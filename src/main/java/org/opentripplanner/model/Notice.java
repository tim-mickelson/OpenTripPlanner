package org.opentripplanner.model;

/**
 * TODO OTP2 - Add JavaDoc
 */
public class Notice extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private String text;

    private String publicCode;

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String publicCode) {
        this.publicCode = publicCode;
    }
}
