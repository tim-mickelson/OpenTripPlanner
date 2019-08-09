package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class NoticeAssignmentMapperTest {

    private static final String ROUTE_ID = "RUT:Route:1";
    private static final String STOP_POINT_ID = "RUT:StopPointInJourneyPattern:1";
    private static final String NOTICE_ID = "RUT:Notice:1";
    private static final String JOURNEY_PATTERN_ID = "RUT:JourneyPattern:1";
    private static final String SERVICE_JOURNEY_ID1 = "RUT:ServiceJourney:1";
    private static final String SERVICE_JOURNEY_ID2 = "RUT:ServiceJourney:2";
    private static final String TIMETABLED_PASSING_TIME1 = "RUT:TimetabledPassingTime:1";
    private static final String TIMETABLED_PASSING_TIME2 = "RUT:TimetabledPassingTime:1";

    private static final Notice NOTICE = new org.rutebanken.netex.model.Notice()
            .withId(NOTICE_ID)
            .withPublicCode("Notice Code")
            .withText(new MultilingualString().withValue("Notice text"));

    @Test
    public void mapNoticeAssignment() {
        NoticeAssignment noticeAssignment = new NoticeAssignment();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(ROUTE_ID));
        noticeAssignment.setNotice(NOTICE);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                new HierarchicalMap<>(),
                new HierarchicalMultimap<>(),
                new HierarchicalMapById<>(),
                new HierarchicalMapById<>()
        );

        Multimap<FeedScopedId, org.opentripplanner.model.Notice> noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2 = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(ROUTE_ID))
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2.getId().getId());
    }

    @Test
    public void mapNoticeAssignmentOnStopPoint() {
        HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId = new HierarchicalMap<>();
        HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId = new HierarchicalMultimap<>();
        HierarchicalMapById<StopPointInJourneyPattern> stopPointInJourneyPatternById = new HierarchicalMapById<>();
        HierarchicalMapById<Notice> noticesById = new HierarchicalMapById<>();


        journeyPatternsByStopPointId.add(
                STOP_POINT_ID, new JourneyPattern().withId(JOURNEY_PATTERN_ID));

        serviceJourneyByPatternId.add(
                JOURNEY_PATTERN_ID,
                new ServiceJourney()
                        .withId(SERVICE_JOURNEY_ID1)
                        .withPassingTimes(
                                new TimetabledPassingTimes_RelStructure().withTimetabledPassingTime(
                                        new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME1)
                                )
                        )
        );
        serviceJourneyByPatternId.add(
                JOURNEY_PATTERN_ID,
                new ServiceJourney()
                        .withId(SERVICE_JOURNEY_ID2)
                        .withPassingTimes(
                                new TimetabledPassingTimes_RelStructure().withTimetabledPassingTime(
                                        new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME2)
                                )
                        )
        );

        stopPointInJourneyPatternById.add(
                new StopPointInJourneyPattern()
                        .withId(STOP_POINT_ID)
                        .withOrder(BigInteger.valueOf(1))
        );

        noticesById.add(NOTICE);

        NoticeAssignment noticeAssignment = new NoticeAssignment();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(
                STOP_POINT_ID)
        );
        noticeAssignment.setNoticeRef(new NoticeRefStructure().withRef(NOTICE_ID));

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                journeyPatternsByStopPointId,
                serviceJourneyByPatternId,
                stopPointInJourneyPatternById,
                noticesById
        );

        Multimap<FeedScopedId, org.opentripplanner.model.Notice> noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2a = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(
                TIMETABLED_PASSING_TIME1))
                .stream().findFirst().get();

        org.opentripplanner.model.Notice notice2b = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(
                TIMETABLED_PASSING_TIME2))
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2a.getId().getId());
        assertEquals(NOTICE_ID, notice2b.getId().getId());
    }
}
