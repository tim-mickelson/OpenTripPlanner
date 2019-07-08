package org.opentripplanner.netex.mapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;


public class NoticeAssignmentMapper {

    private final HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId;

    private final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId;

    private final EntityById<FeedScopedId, Notice> noticesByid;

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    NoticeAssignmentMapper(HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId,
                                   HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId,
                                   EntityById<FeedScopedId, Notice> noticesByid) {
        this.journeyPatternsByStopPointId = journeyPatternsByStopPointId;
        this.serviceJourneyByPatternId = serviceJourneyByPatternId;
        this.noticesByid = noticesByid;
    }

    Multimap<FeedScopedId, Notice> map(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment){
        Multimap<FeedScopedId, Notice> noticesByElementId = HashMultimap.create();

        String journeyPatternRef = netexNoticeAssignment.getNoticedObjectRef().getRef();

        if (getObjectType(netexNoticeAssignment).equals("StopPointInJourneyPattern")) {
            JourneyPattern journeyPattern = journeyPatternsByStopPointId.lookup(journeyPatternRef);

            if (journeyPattern != null) {
                boolean serviceJourneyFound = false;
                // Map notice from StopPointInJourneyPattern to corresponding TimeTabledPassingTimes
                for (ServiceJourney serviceJourney : serviceJourneyByPatternId.lookup(journeyPattern.getId())) {
                    serviceJourneyFound = true;
                    noticesByElementId.put(
                            createFeedScopedId(serviceJourney.getId()),
                            noticesByid.get(createFeedScopedId(serviceJourney.getId())));
                }
                if(!serviceJourneyFound){
                    LOG.warn("ServiceJourney for journeyPatternRef " + journeyPatternRef + " not found when mapping notices.");
                }
            }
            else {
                LOG.warn("JourneyPattern " + journeyPatternRef + " not found when mapping notices.");
            }
        } else {
            noticesByElementId.put(
                    createFeedScopedId(netexNoticeAssignment.getNoticedObjectRef().getRef()),
                    noticesByid.get(createFeedScopedId(journeyPatternRef)));
        }

        return noticesByElementId;
    }

    private static String getObjectType (org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment) {
        String objectType = "";
        if (netexNoticeAssignment.getNoticedObjectRef() != null
                && netexNoticeAssignment.getNoticedObjectRef().getRef().split(":").length >= 2) {
            objectType = netexNoticeAssignment.getNoticedObjectRef().getRef().split(":")[1];
        }
        return objectType;
    }
}
