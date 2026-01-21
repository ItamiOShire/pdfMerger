package com.cloudproject.worker;


import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.cloud.pubsub.v1.AckReplyConsumer;

public class PubSubListener {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
    private static final String SUBSCRIPTION_ID = "pdf-merge-sub";

    public void start() {
        ProjectSubscriptionName subscription = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);
        Subscriber subscriber = Subscriber.newBuilder(subscription, this::processMessage).build();
        subscriber.startAsync().awaitRunning();
        System.out.println("📡 Listening for merge tasks...");
    }

    private void processMessage(PubsubMessage message, AckReplyConsumer consumer) {
        try {
            String data = message.getData().toStringUtf8();
            String[] files = data.split(",");
            System.out.println("Merging: " + String.join(", ", files));
            PdfService.mergeFiles(files);
            consumer.ack();
        } catch (Exception e) {
            e.printStackTrace();
            consumer.nack();
        }
    }
}
