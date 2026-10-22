package cn.iocoder.yudao.module.dcc.service.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DccImageTextPdfConverterTest {
    private static final byte[] TEXT = "R06 中文正文\n完整末行 END-MARKER".getBytes(StandardCharsets.UTF_8);

    @Test
    void pngEmbedsOriginalPixelsAndRendererPreservesAspectAndStampClearanceWithoutFont() throws Exception {
        byte[] original = png(), unchanged = original.clone();
        var result = DccImageTextPdfConverter.convert("R06.png", "image/png", original, null);
        assertEquals("R06.pdf", result.fileName());
        assertArrayEquals(unchanged, original);
        try (var pdf = PDDocument.load(result.content())) {
            assertEquals(1, pdf.getNumberOfPages());
            BufferedImage rendered = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            assertEquals(0xFFFFFF, rendered.getRGB(rendered.getWidth() - 80, 80) & 0xFFFFFF,
                    "Actual source content must leave the existing stamp area blank");
            int minX = rendered.getWidth(), minY = rendered.getHeight(), maxX = -1, maxY = -1;
            int red = 0, blue = 0;
            for (int y = 0; y < rendered.getHeight(); y++) for (int x = 0; x < rendered.getWidth(); x++) {
                int pixel = rendered.getRGB(x, y) & 0xFFFFFF;
                if (pixel == 0xFF0000 || pixel == 0x0000FF) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    if (pixel == 0xFF0000) red++; else blue++;
                }
            }
            assertTrue(red > 100 && blue > 100, "Renderer must show actual colored source pixels");
            assertEquals(2.0, (maxX - minX + 1.0) / (maxY - minY + 1.0), 0.03);
            assertTrue(minY >= 159, "The PNG must remain below the stamp reserve");
            var names = pdf.getPage(0).getResources().getXObjectNames().iterator();
            var embedded = (PDImageXObject) pdf.getPage(0).getResources().getXObject(names.next());
            assertEquals(80, embedded.getWidth()); assertEquals(40, embedded.getHeight());
        }
    }

    @Test
    void utf8ChineseLongLinesAndPagesRemainFullyExtractableWithExplicitFont() throws Exception {
        Resource font = requiredTestFont();
        String longLine = "中文长行R06".repeat(60);
        String text = "\uFEFFBEGIN 中文\r\n\r\n列一\t列二\n" + longLine + "\n"
                + "完整内容不会截断 R06-LINE\n".repeat(100) + "END 最终标记";
        byte[] original = text.getBytes(StandardCharsets.UTF_8), unchanged = original.clone();
        var result = DccImageTextPdfConverter.convert("R06.txt", "text/plain; charset=UTF-8", original, font);
        assertArrayEquals(unchanged, original);
        try (var pdf = PDDocument.load(result.content())) {
            assertTrue(pdf.getNumberOfPages() >= 3);
            String extracted = new PDFTextStripper().getText(pdf);
            assertEquals(text.replace("\uFEFF", "").replaceAll("\\s", ""), extracted.replaceAll("\\s", ""),
                    "Every source character must survive wrapping and pagination in order");
            assertTrue(extracted.contains("END 最终标记"));
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(i, 72);
                long nonWhite = 0;
                for (int y = 0; y < image.getHeight(); y += 2) for (int x = 0; x < image.getWidth(); x += 2)
                    if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) nonWhite++;
                assertTrue(nonWhite > 0, "Each PDF page must render actual text");
                assertEquals(0xFFFFFF, image.getRGB(image.getWidth() - 80, 80) & 0xFFFFFF);
            }
            var resources = pdf.getPage(0).getResources();
            var fontNames = resources.getFontNames().iterator();
            assertTrue(resources.getFont(fontNames.next()).isEmbedded());
        }
    }

    @Test
    void corruptOrOversizedPngFailsBeforeImageAllocation() throws Exception {
        assertThrows(IOException.class, () -> DccImageTextPdfConverter.validateSource("R06.png", "image/png", new byte[]{1, 2, 3}));
        byte[] huge = png();
        java.nio.ByteBuffer.wrap(huge).putInt(16, 100_000).putInt(20, 100_000);
        var crc = new java.util.zip.CRC32(); crc.update(huge, 12, 17);
        java.nio.ByteBuffer.wrap(huge).putInt(29, (int) crc.getValue());
        IOException failure = assertThrows(IOException.class,
                () -> DccImageTextPdfConverter.convert("R06.png", "image/png", huge, null));
        assertTrue(failure.getMessage().contains("pixel count"));
    }

    @Test
    void malformedUtf8ControlsEmptyAndOversizedTextAreRejectedWithoutReplacement() {
        for (byte[] bytes : new byte[][]{new byte[]{(byte) 0xC3, 0x28}, new byte[0], "\uFEFF \r\n\t".getBytes(StandardCharsets.UTF_8),
                "text\u0000hidden".getBytes(StandardCharsets.UTF_8), new byte[DccImageTextPdfConverter.MAX_TEXT_BYTES + 1]})
            assertThrows(IOException.class, () -> DccImageTextPdfConverter.validateSource("R06.txt", "text/plain", bytes));
    }

    @Test
    void textRequiresExplicitLoadableFontAndNeverUsesAnOsDefault() {
        IOException missing = assertThrows(IOException.class,
                () -> DccImageTextPdfConverter.convert("R06.txt", "text/plain", TEXT, null));
        assertTrue(missing.getMessage().contains("yudao.dcc.conversion.text-font"));
        for (Resource resource : new Resource[]{new FileSystemResource("r06-font-does-not-exist.ttf"), new ByteArrayResource(new byte[]{1, 2, 3})}) {
            IOException invalid = assertThrows(IOException.class,
                    () -> DccImageTextPdfConverter.convert("R06.txt", "text/plain", TEXT, resource));
            assertTrue(invalid.getMessage().contains("yudao.dcc.conversion.text-font"));
        }
    }

    @Test
    void absentGlyphIsAnErrorInsteadOfSilentReplacement() throws Exception {
        IOException failure = assertThrows(IOException.class, () -> DccImageTextPdfConverter.convert("R06.txt", "text/plain",
                "R06 \uDBFF\uDFFF".getBytes(StandardCharsets.UTF_8), requiredTestFont()));
        assertTrue(failure.getMessage().contains("text-font") && failure.getMessage().contains("glyph"));
    }

    @Test
    void tooManyTextPagesFailsInsteadOfTruncatingTheTail() throws Exception {
        IOException failure = assertThrows(IOException.class, () -> DccImageTextPdfConverter.convert("R06.txt", "text/plain",
                "R06\n".repeat(20_000).getBytes(StandardCharsets.UTF_8), requiredTestFont()));
        assertTrue(failure.getMessage().contains("pages"));
    }

    @Test
    void mimeMismatchAndUnapprovedMediaRemainRejected() throws Exception {
        assertThrows(IOException.class, () -> DccImageTextPdfConverter.validateSource("R06.png", "image/jpeg", png()));
        assertThrows(IOException.class, () -> DccImageTextPdfConverter.validateSource("R06.txt", "application/pdf", TEXT));
        for (String name : Arrays.asList("R06.webm", "R06.wav", "R06.exe", "R06.svg")) {
            assertFalse(DccControlledFileUploadTypePolicy.isAllowedEditableSourceName(name));
            assertThrows(IOException.class, () -> DccImageTextPdfConverter.convert(name, "text/plain", TEXT, null));
        }
    }

    private static Resource requiredTestFont() {
        // Root supplies a local existing font explicitly; no repository font or platform default is assumed.
        String path = System.getProperty("dcc.r06.test-font");
        assertNotNull(path, "Pass -Ddcc.r06.test-font=<existing embeddable CJK TTF>; no implicit OS font fallback");
        assertTrue(Files.isReadable(Path.of(path)), "Configured CJK test font is unreadable");
        return new FileSystemResource(path);
    }

    private static byte[] png() throws IOException {
        var image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) for (int x = 0; x < 80; x++) image.setRGB(x, y, x < 40 ? 0xFF0000 : 0x0000FF);
        var out = new ByteArrayOutputStream(); assertTrue(ImageIO.write(image, "png", out)); return out.toByteArray();
    }
}
