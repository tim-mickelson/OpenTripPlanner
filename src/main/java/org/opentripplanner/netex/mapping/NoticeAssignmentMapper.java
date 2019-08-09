package org.opentripplanner.netex.mapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx NoticeAssignment, which is the connection between a Notice and the object it refers
 * to. In the case of a notice referring to a StopPointInJourneyPattern, which has no OTP equivalent,
 * it will be assigned to its corresponding TimeTabledPassingTimes for each ServiceJourney in the
 * same JourneyPattern.
 *
 * In order to maintain this connection to TimeTabledPassingTime (StopTime in OTP), it is necessary
 * to assign the TimeTabledPassingTime id to its corresponding StopTime.
 */
public class NoticeAssignmentMapper {

    private final HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId;

    private final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId;

    private final HierarchicalMapById<StopPointInJourneyPattern> stopPointInJourneyPatternById;

    private final HierarchicalMapById<org.rutebanken.netex.model.Notice> noticesById;

    /** Note! The notce mapper cashes notices, making sure duplicates are not created. */
    private final NoticeMapper noticeMapper = new NoticeMapper();

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    NoticeAssignmentMapper(
            HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId,
            HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId,
            HierarchicalMapById<StopPointInJourneyPattern> stopPointInJourneyPatternById,
            HierarchicalMapById<org.rutebanken.netex.model.Notice> noticesById
    ) {
        this.journeyPatternsByStopPointId = journeyPatternsByStopPointId;
        this.serviceJourneyByPatternId = serviceJourneyByPatternId;
        this.stopPointInJourneyPatternById = stopPointInJourneyPatternById;
        this.noticesById = noticesById;
    }

    Multimap<FeedScopedId, Notice> map(NoticeAssignment noticeAssignment){
        Multimap<FeedScopedId, Notice> noticesByElementId = HashMultimap.create();

        String noticedObjectId = noticeAssignment.getNoticedObjectRef().getRef();
        Notice otpNotice = getOrMapNotice(noticeAssignment);

        if(otpNotice == null) {
            LOG.warn("Notice in notice assignment is missing for assignment {}", noticeAssignment);
            return noticesByElementId;
        }

        JourneyPattern journeyPattern = journeyPatternsByStopPointId.lookup(noticedObjectId);

        if (journeyPattern != null) {
            boolean serviceJourneyFound = false;
            // Map notice from StopPointInJourneyPattern to corresponding TimeTabledPassingTimes
            for (ServiceJourney serviceJourney : serviceJourneyByPatternId.lookup(journeyPattern.getId())) {
                serviceJourneyFound = true;

                int order = stopPointInJourneyPatternById.lookup(noticedObjectId).getOrder().intValue();
                TimetabledPassingTime passingTime = serviceJourney.getPassingTimes().getTimetabledPassingTime().get(order - 1);

                noticesByElementId.put(
                        createFeedScopedId(passingTime.getId()),
                        otpNotice
                );
            }
            if(!serviceJourneyFound){
                LOG.warn("ServiceJourney for journeyPatternRef " + noticedObjectId + " not found when mapping notices.");
            }
        } else {
            noticesByElementId.put(createFeedScopedId(noticedObjectId), otpNotice);
        }
        return noticesByElementId;
    }

    private Notice getOrMapNotice(NoticeAssignment assignment) {
        org.rutebanken.netex.model.Notice notice = assignment.getNotice() != null
                ? assignment.getNotice()
                : noticesById.lookup(assignment.getNoticeRef().getRef());

        return notice == null ? null : noticeMapper.map(notice);
    }
}