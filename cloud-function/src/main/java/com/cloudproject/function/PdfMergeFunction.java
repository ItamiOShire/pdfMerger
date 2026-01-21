package com.cloudproject.function;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.protobuf.ByteString;
import java.io.BufferedWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PdfMergeFunction implements HttpFunction {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
    private static final String TOPIC_ID = "pdf-merge-topic";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();
        List<String> files = request.getQueryParameters().get("file");

        if (files == null || files.isEmpty()) {
            response.setStatusCode(400);
            writer.write("Missing ?file parameters, e.g. ?file=a.pdf&file=b.pdf");
            return;
        }

        String messageData = String.join(",", files);
        ProjectTopicName topic = ProjectTopicName.of(PROJECT_ID, TOPIC_ID);

        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topic).build();

            PubsubMessage msg = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(messageData))
                    .build();

            publisher.publish(msg);
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }

        response.setStatusCode(200);
        writer.write("Queued merge job for: " + messageData);
    }
}
