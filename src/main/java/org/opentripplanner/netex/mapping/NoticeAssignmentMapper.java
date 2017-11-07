package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;

import java.util.ArrayList;
import java.util.Collection;


import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;


public class NoticeAssignmentMapper {

    public Collection<NoticeAssignment> mapNoticeAssignment(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment, NetexImportDataIndex netexIndex){
        Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();

        if (getObjectType(netexNoticeAssignment).equals("StopPointInJourneyPattern")) {
            JourneyPattern journeyPattern = netexIndex.journeyPatternsByStopPointId.lookup(netexNoticeAssignment.getNoticedObjectRef().getRef());

            // Map notice from StopPointInJourneyPattern to corresponding TimeTabledPassingTimes
            for (ServiceJourney serviceJourney : netexIndex.serviceJourneyByPatternId.lookup(journeyPattern.getId())) {
                org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

                otpNoticeAssignment.setId(createFeedScopedId(netexNoticeAssignment.getId()));
                otpNoticeAssignment.setNoticeId(createFeedScopedId(netexNoticeAssignment.getNoticeRef().getRef()));
                otpNoticeAssignment.setElementId(createFeedScopedId(serviceJourney.getId()));

                noticeAssignments.add(otpNoticeAssignment);
            }
        } else {
            org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

            otpNoticeAssignment.setId(createFeedScopedId(netexNoticeAssignment.getId()));
            otpNoticeAssignment.setNoticeId(createFeedScopedId(netexNoticeAssignment.getNoticeRef().getRef()));
            otpNoticeAssignment.setElementId(createFeedScopedId(netexNoticeAssignment.getNoticedObjectRef().getRef()));

            noticeAssignments.add(otpNoticeAssignment);
        }

        return noticeAssignments;
    }

    private String getObjectType (org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment) {
        String objectType = "";
        if (netexNoticeAssignment.getNoticedObjectRef() != null
                && netexNoticeAssignment.getNoticedObjectRef().getRef().split(":").length >= 2) {
            objectType = netexNoticeAssignment.getNoticedObjectRef().getRef().split(":")[1];
        }
        return objectType;
    }
}