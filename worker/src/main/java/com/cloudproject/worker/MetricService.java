package com.cloudproject.worker;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

public class MetricService {

    private static final String PROJECT_ID = System.getenv("GCP_PROJECT");

    public static void incrementMergedPdfCount() {
        try (MetricServiceClient client = MetricServiceClient.create()) {

            // Definicja metryki
            Metric metric = Metric.newBuilder()
                    .setType("custom.googleapis.com/pdf_merger/merged_pdfs_count")
                    .build();

            // Określenie zasobu (Compute Engine VM)
            MonitoredResource resource = MonitoredResource.newBuilder()
                    .setType("gce_instance")
                    .putLabels("instance_id", "1234567890123456789") // lub pobrać dynamicznie z metadata server
                    .putLabels("zone", "europe-central2-a")
                    .build();

            // Punkt danych (1 merge)
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

            // Wysłanie danych do Cloud Monitoring
            client.createTimeSeries(ProjectName.of(PROJECT_ID), Collections.singletonList(timeSeries));

            System.out.println("📈 Metric 'merged_pdfs_count' incremented.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
