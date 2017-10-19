package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

class NoticeAssignmentMapper {
    NoticeAssignment mapNoticeAssignment(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment){
        NoticeAssignment otpNoticeAssignment = new NoticeAssignment();

        otpNoticeAssignment.setId(createFeedScopedId(netexNoticeAssignment.getId()));
        otpNoticeAssignment.setNoticeId(createFeedScopedId(netexNoticeAssignment.getNoticeRef().getRef()));
        otpNoticeAssignment.setElementId(createFeedScopedId(netexNoticeAssignment.getNoticedObjectRef().getRef()));

        return otpNoticeAssignment;
    }
}