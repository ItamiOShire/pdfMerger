package com.cloudproject.worker;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

public class MetricService {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
    private static final Logger logger = LoggerFactory.getLogger(MetricService.class);

    public static void incrementMergedPdfCount() {
        try (MetricServiceClient client = MetricServiceClient.create()) {

            Metric metric = Metric.newBuilder()
                    .setType("custom.googleapis.com/pdf_merger/merged_pdfs_count")
                    .build();

            String instanceId = GcpMetadataUtil.getInstanceId();
            String zone = GcpMetadataUtil.getZone();

            MonitoredResource resource = MonitoredResource.newBuilder()
                    .setType("gce_instance")
                    .putLabels("instance_id", instanceId)
                    .putLabels("zone", zone)
                    .build();

            Point point = Point.newBuilder()
                    .setInterval(
                            com.google.monitoring.v3.TimeInterval.newBuilder()
                                    .setEndTime(Timestamp.newBuilder()
                                            .setSeconds(Instant.now().getEpochSecond()))
                                    .build())
                    .setValue(TypedValue.newBuilder()
                            .setInt64Value(1)
                            .build())
                    .build();

            TimeSeries timeSeries = TimeSeries.newBuilder()
                    .setMetric(metric)
                    .setResource(resource)
                    .addPoints(point)
                    .build();

            client.createTimeSeries(ProjectName.of(PROJECT_ID), Collections.singletonList(timeSeries));

            logger.info("Metric 'merged_pdfs_count' incremented.");

            System.out.println(" Metric 'merged_pdfs_count' incremented.");


        } catch (IOException e) {
            logger.error("Error incrementing metric 'merged_pdfs_count'", e);
            e.printStackTrace();
        }
    }
}
