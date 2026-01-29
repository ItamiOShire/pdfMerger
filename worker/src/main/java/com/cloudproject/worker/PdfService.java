package com.cloudproject.worker;

import com.google.cloud.storage.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.util.UUID;

public class PdfService {

    private static String BUCKET_NAME = System.getenv("STORAGE_BUCKET");
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public static void mergeFiles(String[] files) throws Exception {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        PDFMergerUtility merger = new PDFMergerUtility();

        // Wczytujemy całe PDF-y do pamięci
        for (String f : files) {
            Blob blob = storage.get(BUCKET_NAME, f.trim());
            if (blob == null) continue;

            byte[] pdfBytes = blob.getContent(); // 🔹 wczytanie całego pliku
            merger.addSource(new ByteArrayInputStream(pdfBytes));
        }

        // Strumień docelowy do scalonego PDF-a
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outStream);

        // Scalanie dokumentów
        merger.mergeDocuments(null);

        // Zapis scalonego PDF z powrotem do Cloud Storage
        String outputName = "merged/" + UUID.randomUUID() + ".pdf";
        BlobInfo outInfo = BlobInfo.newBuilder(BUCKET_NAME, outputName)
                .setContentType("application/pdf")
                .build();
        storage.create(outInfo, outStream.toByteArray());

        logger.info("Merged PDF saved to: {}", outputName);
        System.out.println("Merged PDF saved to: " + outputName);

        // Zwiększenie metryki
        MetricService.incrementMergedPdfCount();
    }

}