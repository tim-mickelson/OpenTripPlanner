package org.opentripplanner.model.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedId;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.IdentityBean;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OtpTransitBuilder {
    private final List<Agency> agencies = new ArrayList<>();

    private final List<ServiceCalendarDate> calendarDates = new ArrayList<>();

    private final List<ServiceCalendar> calendars = new ArrayList<>();

    private final List<FareAttribute> fareAttributes = new ArrayList<>();

    private final List<FareRule> fareRules = new ArrayList<>();

    private final List<FeedInfo> feedInfos = new ArrayList<>();

    private final List<Frequency> frequencies = new ArrayList<>();

    private final List<Pathway> pathways = new ArrayList<>();

    private final EntityMap<FeedId, Route> routesById = new EntityMap<>();

    private final List<ShapePoint> shapePoints = new ArrayList<>();

    private final EntityMap<FeedId, Stop> stopsById = new EntityMap<>();

    private final SortedMultimap<Trip, StopTime> stopTimesByTrip = new SortedMultimap<>();

    private final List<Transfer> transfers = new ArrayList<>();

    private final EntityMap<FeedId, Trip> trips = new EntityMap<>();

    private final ListMultimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();


    /* Accessors */

    public List<Agency> getAgencies() {
        return agencies;
    }

    public List<ServiceCalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<ServiceCalendar> getCalendars() {
        return calendars;
    }

    public List<FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public List<FeedInfo> getFeedInfos() {
        return feedInfos;
    }

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public EntityMap<FeedId, Route> getRoutes() {
        return routesById;
    }

    public List<ShapePoint> getShapePoints() {
        return shapePoints;
    }

    public EntityMap<FeedId, Stop> getStops() {
        return stopsById;
    }

    public SortedMultimap<Trip, StopTime> getStopTimesSortedByTrip() {
        return stopTimesByTrip;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public EntityMap<FeedId, Trip> getTrips() {
        return trips;
    }

    public Multimap<StopPattern, TripPattern> getTripPatterns() {
        return tripPatterns;
    }


    /**
     * Find all serviceIds in both CalendarServices and CalendarServiceDates.
     */
    Set<FeedId> findAllServiceIds() {
        Set<FeedId> serviceIds = new HashSet<>();
        for (ServiceCalendar calendar : getCalendars()) {
            serviceIds.add(calendar.getServiceId());
        }
        for (ServiceCalendarDate date : getCalendarDates()) {
            serviceIds.add(date.getServiceId());
        }
        return serviceIds;
    }

    public OtpTransitService build() {
        createNoneExistingIds();

        return new OtpTransitServiceImpl(this);
    }

    private void createNoneExistingIds() {
        generateNoneExistingIds(feedInfos);
    }

    static <T extends IdentityBean<Integer>> void generateNoneExistingIds(Collection<T> entities) {
        int maxId = 0;
        for (T it : entities) {
            maxId = zeroOrNull(it.getId()) ? maxId : Math.max(maxId, it.getId());
        }
        for (T it : entities) {
            if(zeroOrNull(it.getId())) {
                it.setId(++maxId);
            }
        }
    }

    private static boolean zeroOrNull(Integer id) {
        return id == null || id == 0;
    }

    public void regenerateIndexes() {
        trips.reindex();
        this.stopsById.reindex();
        this.routesById.reindex();
        this.stopTimesByTrip.reindex();
    }
}
