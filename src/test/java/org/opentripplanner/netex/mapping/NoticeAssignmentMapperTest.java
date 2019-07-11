package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class NoticeAssignmentMapperTest {

    private static final String ROUTE_ID = "RUT:Route:1";
    private static final String STOP_POINT_ID = "RUT:StopPointInJourneyPattern:1";
    private static final String NOTICE_ID = "RUT:Notice:1";
    private static final String JOURNEY_PATTERN_ID = "RUT:JourneyPattern:1";
    private static final String SERVICE_JOURNEY_ID1 = "RUT:ServiceJourney:1";
    private static final String SERVICE_JOURNEY_ID2 = "RUT:ServiceJourney:2";
    private static final String TIMETABLED_PASSING_TIME1 = "RUT:TimetabledPassingTime:1";
    private static final String TIMETABLED_PASSING_TIME2 = "RUT:TimetabledPassingTime:1";

    @Test
    public void mapNoticeAssignment() {
        NoticeAssignment noticeAssignment = new NoticeAssignment();

        Notice notice = new Notice();
        notice.setId(FeedScopedIdFactory.createFeedScopedId(NOTICE_ID));
        EntityById<FeedScopedId, Notice> noticesById = new EntityById<>();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(ROUTE_ID));
        noticeAssignment.setNotice(new org.rutebanken.netex.model.Notice().withId(NOTICE_ID));

        noticesById.add(notice);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                new HierarchicalMap<>(),
                new HierarchicalMultimap<>(),
                new HierarchicalMapById<>(),
                noticesById);

        Multimap<FeedScopedId, Notice> noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);

        Notice notice2 = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(ROUTE_ID))
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2.getId().getId());
    }

    @Test
    public void mapNoticeAssignmentOnStopPoint() {
        HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId = new HierarchicalMap<>();
        HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId = new HierarchicalMultimap<>();
        HierarchicalMapById<StopPointInJourneyPattern> stopPointInJourneyPatternById = new HierarchicalMapById<>();

        journeyPatternsByStopPointId.add(STOP_POINT_ID, new JourneyPattern().withId(
                JOURNEY_PATTERN_ID));

        serviceJourneyByPatternId.add(JOURNEY_PATTERN_ID, new ServiceJourney().withId(
                SERVICE_JOURNEY_ID1).withPassingTimes(new TimetabledPassingTimes_RelStructure()
                .withTimetabledPassingTime(new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME1))));
        serviceJourneyByPatternId.add(JOURNEY_PATTERN_ID, new ServiceJourney().withId(
                SERVICE_JOURNEY_ID2).withPassingTimes(new TimetabledPassingTimes_RelStructure()
                .withTimetabledPassingTime(new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME2))));

        stopPointInJourneyPatternById.add(new StopPointInJourneyPattern().withId(STOP_POINT_ID).withOrder(BigInteger.valueOf(1)));

        NoticeAssignment noticeAssignment = new NoticeAssignment();

        Notice notice = new Notice();
        notice.setId(FeedScopedIdFactory.createFeedScopedId(NOTICE_ID));
        EntityById<FeedScopedId, Notice> noticesById = new EntityById<>();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(
                STOP_POINT_ID));
        noticeAssignment.setNotice(new org.rutebanken.netex.model.Notice().withId(NOTICE_ID));

        noticesById.add(notice);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                journeyPatternsByStopPointId,
                serviceJourneyByPatternId,
                stopPointInJourneyPatternById,
                noticesById);

        Multimap<FeedScopedId, Notice> noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);

        Notice notice2a = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(
                TIMETABLED_PASSING_TIME1))
                .stream().findFirst().get();

        Notice notice2b = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(
                TIMETABLED_PASSING_TIME2))
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2a.getId().getId());
        assertEquals(NOTICE_ID, notice2b.getId().getId());
    }
}
