package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

import static org.junit.Assert.*;

public class NoticeAssignmentMapperTest {

    private static final String ELEMENT_ID = "RUT:Route:1";
    private static final String NOTICE_ID = "RUT:Notice:1";

    @Test
    public void mapNoticeAssignment() {
        NoticeAssignment noticeAssignment = new NoticeAssignment();

        Notice notice = new Notice();
        notice.setId(FeedScopedIdFactory.createFeedScopedId(NOTICE_ID));
        EntityById<FeedScopedId, Notice> noticesById = new EntityById<>();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(ELEMENT_ID));
        noticeAssignment.setNotice(new org.rutebanken.netex.model.Notice().withId(NOTICE_ID));

        noticesById.add(notice);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                new HierarchicalMap<>(),
                new HierarchicalMultimap<>(),
                noticesById);

        Multimap<FeedScopedId, Notice> noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);

        Notice notice2 = noticesByElementId.get(FeedScopedIdFactory.createFeedScopedId(ELEMENT_ID))
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2.getId().getId());
    }
}
