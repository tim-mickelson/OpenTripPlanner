package org.opentripplanner.model;

/**
 * TODO OTP2 - Add JavaDoc
 */
public class NoticeAssignment extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private FeedScopedId noticeId;

    private FeedScopedId elementId;

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public FeedScopedId getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(FeedScopedId noticeId) {
        this.noticeId = noticeId;
    }

    public FeedScopedId getElementId() {
        return elementId;
    }

    public void setElementId(FeedScopedId elementId) {
        this.elementId = elementId;
    }
}