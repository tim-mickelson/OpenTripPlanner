package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ServiceFrameParser {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFrameParser.class);

    private final ReadOnlyHierarchicalVersionMapById<Quay> quayById;

    private final Collection<Network> networks = new ArrayList<>();

    private final Collection<GroupOfLines> groupOfLines = new ArrayList<>();

    private final Collection<Route> routes = new ArrayList<>();

    private final Collection<Line> lines = new ArrayList<>();

    private final Map<String, String> networkIdByGroupOfLineId = new HashMap<>();

    private final Collection<JourneyPattern> journeyPatterns = new ArrayList<>();

    private final Collection<DestinationDisplay> destinationDisplays = new ArrayList<>();

    private final Map<String, String> quayIdByStopPointRef = new HashMap<>();

    private final Collection<Notice> notices = new ArrayList<>();

    private final Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();

    private final Map<String, JourneyPattern> journeyPatternByStopPointId = new HashMap<>();

    private final Collection<StopPointInJourneyPattern> stopPointsInJourneyPattern = new ArrayList<>();

    ServiceFrameParser(ReadOnlyHierarchicalVersionMapById<Quay> quayById) {
        this.quayById = quayById;
    }

    void parse(ServiceFrame sf) {
        parseStopAssignments(sf.getStopAssignments());
        parseRoutes(sf.getRoutes());
        parseNetwork(sf.getNetwork());
        parseLines(sf.getLines());
        parseJourneyPatterns(sf.getJourneyPatterns());
        parseDestinationDisplays(sf.getDestinationDisplays());
        parseNotices(sf.getNotices());
        parseNoticeAssignments(sf.getNoticeAssignments());
    }

    void setResultOnIndex(NetexImportDataIndex index) {
        // update entities
        index.destinationDisplayById.addAll(destinationDisplays);
        index.groupOfLinesById.addAll(groupOfLines);
        index.journeyPatternsById.addAll(journeyPatterns);
        index.lineById.addAll(lines);
        index.networkById.addAll(networks);
        index.quayIdByStopPointRef.addAll((quayIdByStopPointRef));
        index.routeById.addAll(routes);
        index.noticeById.addAll(notices);
        index.noticeAssignmentById.addAll(noticeAssignments);
        index.journeyPatternsByStopPointId.addAll(journeyPatternByStopPointId);
        index.stopPointsInJourneyPatternById.addAll(stopPointsInJourneyPattern);

        // update references
        index.networkIdByGroupOfLineId.addAll(networkIdByGroupOfLineId);
    }

    private void parseStopAssignments(StopAssignmentsInFrame_RelStructure stopAssignments) {
        if (stopAssignments == null) return;

        for (JAXBElement stopAssignment : stopAssignments.getStopAssignment()) {
            if (stopAssignment.getValue() instanceof PassengerStopAssignment) {
                PassengerStopAssignment assignment = (PassengerStopAssignment) stopAssignment
                        .getValue();
                String quayRef = assignment.getQuayRef().getRef();
                Quay quay = quayById.lookupLastVersionById(quayRef);
                if (quay != null) {
                    quayIdByStopPointRef
                            .put(assignment.getScheduledStopPointRef().getValue()
                                    .getRef(), quay.getId());
                } else {
                    LOG.warn("Quay " + quayRef + " not found in stop place file.");
                }
            }
        }
    }

    private void parseRoutes(RoutesInFrame_RelStructure routes) {
        if (routes == null) return;

        for (JAXBElement element : routes.getRoute_()) {
            if (element.getValue() instanceof Route) {
                Route route = (Route) element.getValue();
                this.routes.add(route);
            }
        }
    }

    private void parseNetwork(Network network) {
        if (network == null) return;

        networks.add(network);

        GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();

        if (groupsOfLines != null) {
            parseGroupOfLines(groupsOfLines.getGroupOfLines(), network);
        }
    }

    private void parseGroupOfLines(Collection<GroupOfLines> groupOfLines, Network network) {
        for (GroupOfLines group : groupOfLines) {
            networkIdByGroupOfLineId.put(network.getId(), group.getId());
        }
    }

    private void parseLines(LinesInFrame_RelStructure lines) {
        if (lines == null) return;

        for (JAXBElement element : lines.getLine_()) {
            if (element.getValue() instanceof Line) {
                this.lines.add((Line) element.getValue());
            }
        }
    }

    private void parseJourneyPatterns(JourneyPatternsInFrame_RelStructure journeyPatterns) {
        if (journeyPatterns == null) return;

        for (JAXBElement pattern : journeyPatterns.getJourneyPattern_OrJourneyPatternView()) {
            if (pattern.getValue() instanceof JourneyPattern) {
                JourneyPattern journeyPattern = (JourneyPattern) pattern.getValue();
                this.journeyPatterns.add(journeyPattern);
                for (PointInLinkSequence_VersionedChildStructure pointInLinkSequence_versionedChildStructure
                        : journeyPattern.getPointsInSequence()
                        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()) {
                    if (pointInLinkSequence_versionedChildStructure instanceof StopPointInJourneyPattern) {
                        StopPointInJourneyPattern stopPointInJourneyPattern =
                                (StopPointInJourneyPattern) pointInLinkSequence_versionedChildStructure;
                        journeyPatternByStopPointId.put(stopPointInJourneyPattern.getId(), journeyPattern);
                        stopPointsInJourneyPattern.add(stopPointInJourneyPattern);
                    }
                }
            }
        }
    }

    private void parseDestinationDisplays(DestinationDisplaysInFrame_RelStructure destDisplays) {
        if (destDisplays == null) return;

        this.destinationDisplays.addAll(destDisplays.getDestinationDisplay());
    }


    private void parseNotices(NoticesInFrame_RelStructure notices) {
        if (notices == null) return;

        this.notices.addAll(notices.getNotice());
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
