package cn.iocoder.yudao.module.dcc.service.file;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccPdfStampServiceTest {

    @Test
    void stampPdf_appendsControlledStampImageAndKeepsPdfReadable() throws Exception {
        byte[] source = buildSimplePdf("original");

        DccPdfStampService stampService = new DccPdfStampService();
        byte[] stamped = stampService.stamp(source);

        assertTrue(stamped.length > source.length);
        try (PDDocument document = PDDocument.load(stamped)) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("original"));
            Iterator<COSName> xObjectNames = document.getPage(0).getResources().getXObjectNames().iterator();
            assertTrue(xObjectNames.hasNext());
            PDXObject xObject = document.getPage(0).getResources().getXObject(xObjectNames.next());
            assertTrue(xObject instanceof PDImageXObject);
        }
    }

    private static byte[] buildSimplePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText(text);
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
