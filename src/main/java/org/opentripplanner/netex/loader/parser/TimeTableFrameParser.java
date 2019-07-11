package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.mapping.NoticeAssignmentMapper;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class TimeTableFrameParser {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTableFrameParser.class);

    private final ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById;

    private final Set<DayTypeRefsToServiceIdAdapter> dayTypeRefs = new HashSet<>();

    private final Multimap<String, ServiceJourney> serviceJourneyByPatternId = ArrayListMultimap.create();

    private final Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();

    TimeTableFrameParser(ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById) {
        this.journeyPatternById = journeyPatternById;
    }

    void parse(TimetableFrame timetableFrame) {
        JourneysInFrame_RelStructure vehicleJourneys;
        Collection<Journey_VersionStructure> journeys;

        vehicleJourneys = timetableFrame.getVehicleJourneys();
        journeys = vehicleJourneys.getDatedServiceJourneyOrDeadRunOrServiceJourney();

        for (Journey_VersionStructure it : journeys) {
            if (it instanceof ServiceJourney) {
                parseServiceJourney((ServiceJourney)it);
            }
        }

        parseNoticeAssignments(timetableFrame.getNoticeAssignments());
    }

    void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.serviceJourneyByPatternId.addAll(serviceJourneyByPatternId);
        netexIndex.dayTypeRefs.addAll(dayTypeRefs);
        netexIndex.noticeAssignmentById.addAll(noticeAssignments);
    }

    private void parseServiceJourney(ServiceJourney sj) {
        dayTypeRefs.add(new DayTypeRefsToServiceIdAdapter(sj.getDayTypes()));

        String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

        JourneyPattern journeyPattern = journeyPatternById.lookup(journeyPatternId);

        if (journeyPattern != null) {
            if (journeyPattern.getPointsInSequence().
                    getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                    .size() == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                serviceJourneyByPatternId.put(journeyPatternId, sj);
            } else {
                LOG.warn(
                        "Mismatch between ServiceJourney and JourneyPattern. " +
                        "ServiceJourney will be skipped. - " + sj.getId()
                );
            }
        } else {
            LOG.warn("JourneyPattern not found. " + journeyPatternId);
        }
    }

    private void parseNoticeAssignments(NoticeAssignmentsInFrame_RelStructure na) {
        if (na == null) return;

        for (JAXBElement<? extends DataManagedObjectStructure> noticeAssignmentElement : na.getNoticeAssignment_()) {
            NoticeAssignment noticeAssignment = (NoticeAssignment) noticeAssignmentElement.getValue();

            if (noticeAssignment.getNoticeRef() != null && noticeAssignment.getNoticedObjectRef() != null) {
                this.noticeAssignments.add(noticeAssignment);
            }
        }
    }
}