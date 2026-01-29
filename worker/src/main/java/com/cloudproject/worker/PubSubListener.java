package com.cloudproject.worker;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PubSubListener {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
    private static final String SUBSCRIPTION_ID = System.getenv("PUBSUB_SUBSCRIPTION");
    private static final Logger logger = LoggerFactory.getLogger(PubSubListener.class);

    public void start() throws InterruptedException {
        ProjectSubscriptionName subscription = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);
        Subscriber subscriber = Subscriber.newBuilder(subscription, this::processMessage).build();
        subscriber.startAsync().awaitRunning();
        System.out.println(" Listening for merge tasks...");

        Thread.currentThread().join();
    }

    private void processMessage(PubsubMessage message, AckReplyConsumer consumer) {
        try {
            String data = message.getData().toStringUtf8();
            String[] files = data.split(",");

            logger.info("Merging: {}", String.join(", ", files));
            System.out.println("Merging: " + String.join(", ", files));
            PdfService.mergeFiles(files);
            consumer.ack();
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            e.printStackTrace();
            consumer.nack();
        }
    }
}
