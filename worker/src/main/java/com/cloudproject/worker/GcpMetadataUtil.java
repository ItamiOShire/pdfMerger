package com.cloudproject.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GcpMetadataUtil {

    private static final String METADATA_URL = "http://metadata.google.internal/computeMetadata/v1/";
    private static final String HEADER_NAME = "Metadata-Flavor";
    private static final String HEADER_VALUE = "Google";

    private static final Logger logger = LoggerFactory.getLogger(GcpMetadataUtil.class);

    public static String getInstanceId() {
        return fetchMetadata("instance/id");
    }

    public static String getZone() {
        String fullZonePath = fetchMetadata("instance/zone");
        return fullZonePath.substring(fullZonePath.lastIndexOf('/') + 1);
    }

    private static String fetchMetadata(String path) {
        try {
            URL url = new URL(METADATA_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty(HEADER_NAME, HEADER_VALUE);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            logger.error("Error fetching metadata for path: {}", path, e);
            e.printStackTrace();
            return "unknown";
        }
    }
}
