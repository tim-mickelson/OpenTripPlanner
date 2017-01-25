package org.opentripplanner.updater.siri;

import uk.org.siri.siri20.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class RuterSiriHelper {

    static List rewriteEtIds(List<EstimatedTimetableDeliveryStructure> deliveries) {
        for (EstimatedTimetableDeliveryStructure delivery : deliveries) {
            for (EstimatedVersionFrameStructure versionFrameStructure : delivery.getEstimatedJourneyVersionFrames()) {
                List<EstimatedVehicleJourney> et = versionFrameStructure.getEstimatedVehicleJourneies();
                if (et != null) {
                    for (EstimatedVehicleJourney journey : et) {
                        DestinationRef destRef = journey.getDestinationRef();
                        if (destRef != null && destRef.getValue().contains(":")) {
                            String value = destRef.getValue();
                            destRef.setValue(rewriteStopPlaceId(value));
                        }
                        JourneyPlaceRefStructure originRef = journey.getOriginRef();
                        if (originRef != null && originRef.getValue().contains(":")) {
                            String value = originRef.getValue();
                            originRef.setValue(rewriteStopPlaceId(value));
                        }

                        EstimatedVehicleJourney.EstimatedCalls estimatedCalls = journey.getEstimatedCalls();
                        if (estimatedCalls != null) {
                            List<EstimatedCall> etList = estimatedCalls.getEstimatedCalls();
                            for (EstimatedCall estimatedCall : etList) {
                                StopPointRef stopPointRef = estimatedCall.getStopPointRef();
                                String value = stopPointRef.getValue();
                                stopPointRef.setValue(rewriteStopPlaceId(value));
                            }
                        }
                    }
                }
            }
        }
        return deliveries;
    }

    static List rewriteVmIds(List<VehicleMonitoringDeliveryStructure> deliveries) {
        for (VehicleMonitoringDeliveryStructure delivery : deliveries) {
            for (VehicleActivityStructure activityStructure : delivery.getVehicleActivities()) {
                VehicleActivityStructure.MonitoredVehicleJourney mvj = activityStructure.getMonitoredVehicleJourney();
                if (mvj != null) {
                    DestinationRef destRef = mvj.getDestinationRef();
                    if (destRef != null && destRef.getValue().contains(":")) {
                        String value = destRef.getValue();
                        destRef.setValue(rewriteStopPlaceId(value));
                    }
                    JourneyPlaceRefStructure originRef = mvj.getOriginRef();
                    if (originRef != null && originRef.getValue().contains(":")) {
                        String value = originRef.getValue();
                        originRef.setValue(rewriteStopPlaceId(value));
                    }
                    MonitoredCallStructure monitoredCall = mvj.getMonitoredCall();
                    if (monitoredCall != null && monitoredCall.getStopPointRef() != null && monitoredCall.getStopPointRef().getValue().contains(":")) {
                        String value = monitoredCall.getStopPointRef().getValue();
                        monitoredCall.getStopPointRef().setValue(rewriteStopPlaceId(value));
                    }
//                            mvj.getPreviousCalls();
//                            mvj.getOnwardCalls();
                }
            }
        }
        return deliveries;
    }

    private static String rewriteStopPlaceId(String value) {
        if (value.contains(":")) {
            if (value.substring(value.indexOf(":")).length() > 2) {
                // 1234:56 -> 123456
                value = value.replaceAll(":", "");
            } else {
                // 1234:5  -> 123405
                value = value.replaceAll(":", "0");
            }
        }
        return value;
    }

    static Siri createETServiceRequest(String requestorRefValue) {
        Siri request = new Siri();
        request.setVersion("2.0");

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        EstimatedTimetableRequestStructure etRequest = new EstimatedTimetableRequestStructure();
        etRequest.setRequestTimestamp(ZonedDateTime.now());
        etRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        etRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getEstimatedTimetableRequests().add(etRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }


    static Siri createVMServiceRequest(String requestorRefValue) {
        Siri request = new Siri();
        request.setVersion("2.0");

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        VehicleMonitoringRequestStructure vmRequest = new VehicleMonitoringRequestStructure();
        vmRequest.setRequestTimestamp(ZonedDateTime.now());
        vmRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        vmRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getVehicleMonitoringRequests().add(vmRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }
}
