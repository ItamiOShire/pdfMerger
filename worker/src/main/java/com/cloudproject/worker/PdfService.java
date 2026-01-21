package com.cloudproject.worker;

import com.google.cloud.storage.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class PdfService {

    private static final String BUCKET_NAME = "pdf-bucket";

    public static void mergeFiles(String[] files) throws Exception {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        PDFMergerUtility merger = new PDFMergerUtility();

        for (String f : files) {
            Blob blob = storage.get(BUCKET_NAME, f.trim());
            if (blob == null) continue;
            try (InputStream in = (InputStream) blob.reader()) {
                merger.addSource(in);
            }
        }

        String outputName = "merged/" + UUID.randomUUID() + ".pdf";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outStream);
        merger.mergeDocuments(null);

        BlobInfo outInfo = BlobInfo.newBuilder(BUCKET_NAME, outputName)
                .setContentType("application/pdf")
                .build();
        storage.create(outInfo, outStream.toByteArray());

        System.out.println("Merged PDF saved to: " + outputName);
    }
}