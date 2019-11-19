/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.siri;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.entur.protobuf.mapper.SiriMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.ReadinessBlockingUpdater;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.Siri;
import uk.org.siri.www.siri.SiriType;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class starts a Google PubSub subscription
 *
 * Usage example ('websocket' name is an example) in the file 'Graph.properties':
 *
 * <pre>
 * websocket.type = websocket-siri-et-updater
 * websocket.defaultAgencyId = agency
 * websocket.url = ws://localhost:8088/tripUpdates
 * </pre>
 *
 */
public class SiriEstimatedTimetableGooglePubsubUpdater extends ReadinessBlockingUpdater implements GraphUpdater {

    /**
     * Number of seconds to wait before checking again whether we are still connected
     */
    private static final int CHECK_CONNECTION_PERIOD_SEC = 1;

    private static final int DEFAULT_RECONNECT_PERIOD_SEC = 300; // Five minutes

    private static Logger LOG = LoggerFactory.getLogger(SiriEstimatedTimetableGooglePubsubUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private GraphUpdaterManager updaterManager;

    /**
     * The ID for the static feed to which these TripUpdates are applied
     */
    private String feedId;

    /**
     * The number of seconds to wait before reconnecting after a failed connection.
     */
    private int reconnectPeriodSec;

    private SubscriptionAdminClient subscriptionAdminClient;
    private ProjectSubscriptionName subscriptionName;
    private ProjectTopicName topic;
    private PushConfig pushConfig;

    public SiriEstimatedTimetableGooglePubsubUpdater() {

        try {
            if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null &&
                    !System.getenv("GOOGLE_APPLICATION_CREDENTIALS").isEmpty()) {

                /*
                  Google libraries expects path to credentials json-file is stored in environment variable "GOOGLE_APPLICATION_CREDENTIALS"
                 */

                subscriptionAdminClient = SubscriptionAdminClient.create();
            } else {
                throw new RuntimeException("Google Pubsub updater is configured, but no environment variable 'GOOGLE_APPLICATION_CREDENTIALS' is not defined");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    public static void main(String[] args) {
        SiriEstimatedTimetableGooglePubsubUpdater s = new SiriEstimatedTimetableGooglePubsubUpdater();
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {

        feedId = config.path("feedId").asText("");
        reconnectPeriodSec = config.path("reconnectPeriodSec").asInt(DEFAULT_RECONNECT_PERIOD_SEC);

        blockReadinessUntilInitialized = config.path("blockReadinessUntilInitialized").asBoolean(false);

        // set subscriber
        String subscriptionId = System.getenv("HOSTNAME");
        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = "otp-"+UUID.randomUUID().toString();
        }
        String projectName = config.path("projectName").asText();

        String topicName = config.path("topicName").asText();

        subscriptionName = ProjectSubscriptionName.of(
                projectName, subscriptionId);
        topic = ProjectTopicName.of(projectName, topicName);

        pushConfig = PushConfig.getDefaultInstance();

    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
        // Create a realtime data snapshot source and wait for runnable to be executed
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                // Only create a realtime data snapshot source if none exists already
                if (graph.timetableSnapshotSource == null) {
                    TimetableSnapshotSource snapshotSource = new TimetableSnapshotSource(graph);
                    // Add snapshot source to graph
                    graph.timetableSnapshotSource = (snapshotSource);
                }
            }
        });
    }

    @Override
    public void run() throws IOException {

        if (subscriptionAdminClient == null) {
            throw new RuntimeException("Unable to initialize Google Pubsub-updater: System.getenv('GOOGLE_APPLICATION_CREDENTIALS') = " + System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
        }

        Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topic, pushConfig, 10);
        startTime = now();

        Subscriber subscriber = null;
        while (true) {
            try {
                subscriber =
                        Subscriber.newBuilder(subscription.getName(), new MessageReceiverExample()).build();
                subscriber.startAsync().awaitRunning();

                subscriber.awaitTerminated();
            } catch (IllegalStateException e) {

                if (subscriber != null) {
                    subscriber.stopAsync();
                }
            }
            try {
                Thread.sleep(reconnectPeriodSec * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long now() {
        return ZonedDateTime.now().toInstant().toEpochMilli();
    }

    @Override
    public void teardown() {
        subscriptionAdminClient.deleteSubscription(subscriptionName);
    }


    private static transient final AtomicLong messageCounter = new AtomicLong(0);
    private static transient final AtomicLong updateCounter = new AtomicLong(0);
    private static transient final AtomicLong sizeCounter = new AtomicLong(0);
    private static transient long startTime;

    class MessageReceiverExample implements MessageReceiver {
        @Override
        public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {

            Siri siri = null;
            try {
                sizeCounter.addAndGet(message.getData().size());

                final ByteString data = message.getData();

                final SiriType siriType = SiriType.parseFrom(data);
                siri = SiriMapper.map(siriType);

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            if (siri != null) {
                if (siri.getServiceDelivery() != null) {
                    // Handle trip updates via graph writer runnable
                    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries = siri.getServiceDelivery().getEstimatedTimetableDeliveries();

                    int numberOfUpdatedTrips = 0;
                    try {
                        numberOfUpdatedTrips = estimatedTimetableDeliveries.get(0).getEstimatedJourneyVersionFrames().get(0).getEstimatedVehicleJourneies().size();
                    } catch (Throwable t) {
                        //ignore
                    }
                    long numberOfUpdates = updateCounter.getAndAdd(numberOfUpdatedTrips);
                    long numberOfMessages = messageCounter.incrementAndGet();

                    if (numberOfMessages % 1000 == 0) {
                        LOG.info("Pubsub stats: [messages: {},  updates: {}, total size: {}, current delay {} ms, time since startup: {}]", numberOfMessages, numberOfUpdates, FileUtils.byteCountToDisplaySize(sizeCounter.get()),
                                (now() - siri.getServiceDelivery().getResponseTimestamp().toInstant().toEpochMilli()),
                                DurationFormatUtils.formatDuration((now() - startTime), "HH:mm:ss"));
                    }

                    EstimatedTimetableGraphWriterRunnable runnable =
                            new EstimatedTimetableGraphWriterRunnable(false,
                                    estimatedTimetableDeliveries);

                    updaterManager.execute(runnable);
                } else if (siri.getDataReadyNotification() != null) {
                    // NOT its intended use, but the current implementation sends a DataReadyNotification when initial delivery is complete.
                    LOG.info("WS initialized after {} ms - processed {} messages with {} updates and {} bytes",
                            (System.currentTimeMillis()-startTime),
                            messageCounter.get(),
                            updateCounter.get(),
                            FileUtils.byteCountToDisplaySize(sizeCounter.get()));
                    isInitialized = true;
                }
            }

            // Ack only after all work for the message is complete.
            consumer.ack();
        }
    }
}
