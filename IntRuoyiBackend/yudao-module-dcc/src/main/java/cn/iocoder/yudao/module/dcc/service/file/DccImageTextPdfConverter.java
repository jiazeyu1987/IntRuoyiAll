package cn.iocoder.yudao.module.dcc.service.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Actual PNG pixels and UTF-8 text; the caller retains the original and stamps the resulting PDF. */
public final class DccImageTextPdfConverter {
    static final long MAX_PNG_PIXELS = 16_000_000L;
    static final int MAX_TEXT_BYTES = 4 * 1024 * 1024;
    static final int MAX_PAGES = 200;
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final float MARGIN = 36, TOP = 160, FONT_SIZE = 12, LEADING = 18;
    private static final int LINES_PER_PAGE = (int) ((PDRectangle.A4.getHeight() - TOP - MARGIN) / LEADING);

    private DccImageTextPdfConverter() { }

    public static void validateSource(String name, String contentType, byte[] bytes) throws IOException {
        requireContentType(name, contentType);
        if (DccControlledFileUploadTypePolicy.isPngName(name)) readPng(bytes);
        else decodeText(bytes);
    }

    public static DccConvertedPdf convert(String name, String contentType, byte[] bytes, Resource textFont) throws IOException {
        requireContentType(name, contentType);
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (DccControlledFileUploadTypePolicy.isPngName(name)) {
                BufferedImage image = readPng(bytes);
                PDPage page = new PDPage(PDRectangle.A4);
                pdf.addPage(page);
                float scale = Math.min((PDRectangle.A4.getWidth() - 2 * MARGIN) / image.getWidth(),
                        (PDRectangle.A4.getHeight() - TOP - MARGIN) / image.getHeight());
                float width = image.getWidth() * scale, height = image.getHeight() * scale;
                try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                    // The existing controlled stamp occupies the top-right area; content must not overlap it.
                    stream.drawImage(LosslessFactory.createFromImage(pdf, image), MARGIN,
                            PDRectangle.A4.getHeight() - TOP - height, width, height);
                }
            } else {
                String text = decodeText(bytes);
                PDType0Font font = loadFont(pdf, textFont);
                List<String> lines = wrap(text, font);
                for (int offset = 0; offset < lines.size(); offset += LINES_PER_PAGE) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    pdf.addPage(page);
                    try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                        stream.beginText(); stream.setFont(font, FONT_SIZE);
                        stream.newLineAtOffset(MARGIN, PDRectangle.A4.getHeight() - TOP);
                        for (int i = offset; i < Math.min(offset + LINES_PER_PAGE, lines.size()); i++) {
                            if (i > offset) stream.newLineAtOffset(0, -LEADING);
                            stream.showText(lines.get(i));
                        }
                        stream.endText();
                    }
                }
            }
            pdf.save(output);
            return new DccConvertedPdf(name.substring(0, name.lastIndexOf('.')) + ".pdf", output.toByteArray());
        }
    }

    private static void requireContentType(String name, String contentType) throws IOException {
        String type = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean png = DccControlledFileUploadTypePolicy.isPngName(name);
        if ((!png && !DccControlledFileUploadTypePolicy.isTextName(name))
                || !(png ? "image/png" : "text/plain").equals(type))
            throw new IOException("PNG/TXT source extension and content type do not match");
    }

    private static BufferedImage readPng(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) throw new IOException("PNG source is empty or invalid");
        for (int i = 0; i < PNG_SIGNATURE.length; i++)
            if (bytes[i] != PNG_SIGNATURE[i]) throw new IOException("PNG source signature is invalid");
        var readers = ImageIO.getImageReadersByFormatName("png");
        if (!readers.hasNext()) throw new IOException("PNG decoder is unavailable");
        var reader = readers.next();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(input, true, true);
            int width = reader.getWidth(0), height = reader.getHeight(0);
            if (width <= 0 || height <= 0 || (long) width * height > MAX_PNG_PIXELS)
                throw new IOException("PNG pixel count exceeds " + MAX_PNG_PIXELS);
            BufferedImage image = reader.read(0);
            if (image == null) throw new IOException("PNG source cannot be decoded");
            return image;
        } finally { reader.dispose(); }
    }

    private static String decodeText(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) throw new IOException("TXT source is empty");
        if (bytes.length > MAX_TEXT_BYTES) throw new IOException("TXT layout input exceeds " + MAX_TEXT_BYTES + " bytes");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException failure) {
            throw new IOException("TXT source must contain valid UTF-8", failure);
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (text.isBlank()) throw new IOException("TXT source has no visible text");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\t')
                throw new IOException("TXT source contains unsupported control character U+" + Integer.toHexString(c));
        }
        return text;
    }

    private static PDType0Font loadFont(PDDocument pdf, Resource resource) throws IOException {
        if (resource == null || !resource.exists() || !resource.isReadable())
            throw new IOException("yudao.dcc.conversion.text-font is missing or unreadable");
        try (var input = resource.getInputStream()) {
            return PDType0Font.load(pdf, input, true);
        } catch (IOException | IllegalArgumentException failure) {
            throw new IOException("yudao.dcc.conversion.text-font cannot be loaded as an embeddable TrueType font", failure);
        }
    }

    private static List<String> wrap(String text, PDType0Font font) throws IOException {
        List<String> lines = new ArrayList<>();
        Map<Integer, Float> widths = new HashMap<>();
        StringBuilder line = new StringBuilder();
        float width = 0, available = PDRectangle.A4.getWidth() - 2 * MARGIN;
        int column = 0;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index); index += Character.charCount(codePoint);
            if (codePoint == '\n') { addLine(lines, line); width = 0; column = 0; continue; }
            int repeats = codePoint == '\t' ? 4 - column % 4 : 1;
            int glyph = codePoint == '\t' ? ' ' : codePoint;
            Float glyphWidth = widths.get(glyph);
            if (glyphWidth == null) {
                try { glyphWidth = font.getStringWidth(new String(Character.toChars(glyph))) * FONT_SIZE / 1000; }
                catch (IllegalArgumentException failure) {
                    throw new IOException("yudao.dcc.conversion.text-font has no glyph for U+" + Integer.toHexString(glyph).toUpperCase(Locale.ROOT), failure);
                }
                if (!Float.isFinite(glyphWidth) || glyphWidth < 0 || glyphWidth > available)
                    throw new IOException("TXT glyph cannot fit on the PDF page");
                widths.put(glyph, glyphWidth);
            }
            for (int repeat = 0; repeat < repeats; repeat++) {
                if (width + glyphWidth > available && !line.isEmpty()) { addLine(lines, line); width = 0; }
                line.appendCodePoint(glyph); width += glyphWidth; column++;
                if (line.length() > 16384) throw new IOException("TXT line has too many zero-width characters");
            }
        }
        addLine(lines, line);
        return lines;
    }

    private static void addLine(List<String> lines, StringBuilder line) throws IOException {
        if (lines.size() >= MAX_PAGES * LINES_PER_PAGE) throw new IOException("TXT PDF exceeds " + MAX_PAGES + " pages");
        lines.add(line.toString()); line.setLength(0);
    }
}
