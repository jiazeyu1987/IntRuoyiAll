package cn.iocoder.yudao.module.dcc.service.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class DccPdfStampService {

    private static final String DEFAULT_STAMP_RESOURCE = "dcc/stamp/controlled-stamp.png";

    public byte[] stamp(byte[] source) throws IOException {
        Resource stampResource = new ClassPathResource(DEFAULT_STAMP_RESOURCE);
        if (!stampResource.exists()) {
            throw new IOException("Controlled stamp resource is missing: " + DEFAULT_STAMP_RESOURCE);
        }
        try (InputStream stampInput = stampResource.getInputStream()) {
            return stamp(source, StreamUtils.copyToByteArray(stampInput));
        }
    }

    byte[] stamp(byte[] source, byte[] stampImageBytes) throws IOException {
        try (PDDocument document = PDDocument.load(source); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages");
            }
            PDPage firstPage = document.getPage(0);
            PDImageXObject stampImage = PDImageXObject.createFromByteArray(document, stampImageBytes, "dcc-controlled-stamp");
            float imageWidth = 120f;
            float imageHeight = imageWidth * stampImage.getHeight() / Math.max(1f, stampImage.getWidth());
            float x = firstPage.getMediaBox().getWidth() - imageWidth - 24f;
            float y = firstPage.getMediaBox().getHeight() - imageHeight - 24f;
            try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, firstPage,
                    org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(stampImage, x, y, imageWidth, imageHeight);
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
