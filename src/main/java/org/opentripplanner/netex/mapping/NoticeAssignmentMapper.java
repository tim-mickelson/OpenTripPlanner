package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;


import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;


public class NoticeAssignmentMapper {

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    public Collection<NoticeAssignment> mapNoticeAssignment(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment, NetexImportDataIndex netexIndex){
        Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();
        String journeyPatternRef = netexNoticeAssignment.getNoticedObjectRef().getRef();

        if (getObjectType(netexNoticeAssignment).equals("StopPointInJourneyPattern")) {
            JourneyPattern journeyPattern = netexIndex.journeyPatternsByStopPointId.lookup(journeyPatternRef);

            if (journeyPattern != null) {
                boolean serviceJourneyNotFound = true;
                // Map notice from StopPointInJourneyPattern to corresponding TimeTabledPassingTimes
                for (ServiceJourney serviceJourney : netexIndex.serviceJourneyByPatternId.lookup(journeyPattern.getId())) {
                    serviceJourneyNotFound = false;
                    org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

                    otpNoticeAssignment.setId(createFeedScopedId(netexNoticeAssignment.getId()));
                    otpNoticeAssignment.setNoticeId(createFeedScopedId(netexNoticeAssignment.getNoticeRef().getRef()));
                    otpNoticeAssignment.setElementId(createFeedScopedId(serviceJourney.getId()));

                    noticeAssignments.add(otpNoticeAssignment);
                }
                if(serviceJourneyNotFound){
                    LOG.warn("ServiceJourney for journeyPatternRef " + journeyPatternRef + " not found when mapping notices.");
                }
            }
            else {
                LOG.warn("JourneyPattern " + journeyPatternRef + " not found when mapping notices.");
            }
        } else {
            org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

            otpNoticeAssignment.setId(createFeedScopedId(netexNoticeAssignment.getId()));
            otpNoticeAssignment.setNoticeId(createFeedScopedId(netexNoticeAssignment.getNoticeRef().getRef()));
            otpNoticeAssignment.setElementId(createFeedScopedId(journeyPatternRef));

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