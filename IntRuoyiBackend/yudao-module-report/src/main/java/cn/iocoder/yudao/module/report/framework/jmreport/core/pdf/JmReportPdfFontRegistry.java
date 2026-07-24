package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import org.jeecg.modules.jmreport.desreport.pdf.enums.PdfFontEnum;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers JMReport classpath fonts for OpenPDF before the obfuscated renderer uses its static font cache.
 */
@Component
public class JmReportPdfFontRegistry {

    private static final String OPENPDF_TABLE_RENDER = "org.jeecg.modules.jmreport.desreport.pdf.b.c";
    private static final String IDENTITY_H = "Identity-H";
    private static final Map<String, Path> EXTRACTED_FONT_FILES = new ConcurrentHashMap<>();

    public void ensurePdfFontsReady() {
        Map<String, Font> fontMap = getOpenPdfFontMap();
        ensureDefaultFontReady(fontMap);
        Font defaultFont = fontMap.get(PdfFontEnum.b.getValue());
        for (PdfFontEnum fontEnum : PdfFontEnum.a()) {
            Font existingFont = fontMap.get(fontEnum.getValue());
            if (!isUsableFont(existingFont)) {
                Font font = hasBundledFont(fontEnum) ? registerFont(fontEnum) : defaultFont;
                fontMap.put(fontEnum.getValue(), font);
            }
            registerFontFactoryAlias(fontEnum.getValue(), resolveFontFactorySource(fontEnum));
        }
    }

    private void ensureDefaultFontReady(Map<String, Font> fontMap) {
        Font defaultFont = fontMap.get(PdfFontEnum.b.getValue());
        if (!isUsableFont(defaultFont)) {
            if (!hasBundledFont(PdfFontEnum.b)) {
                throw new IllegalStateException("JMReport default PDF font is missing: " + PdfFontEnum.b.getPath());
            }
            defaultFont = registerFont(PdfFontEnum.b);
            fontMap.put(PdfFontEnum.b.getValue(), defaultFont);
        }
        registerFontFactoryAlias(PdfFontEnum.b.getValue(), PdfFontEnum.b);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Font> getOpenPdfFontMap() {
        try {
            Class<?> rendererClass = Class.forName(OPENPDF_TABLE_RENDER);
            Field field = rendererClass.getField("e");
            Object value = field.get(null);
            if (value instanceof Map<?, ?> fontMap) {
                return (Map<String, Font>) fontMap;
            }
            throw new IllegalStateException("JMReport OpenPDF font cache is not a Map: " + value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot access JMReport OpenPDF font cache", ex);
        }
    }

    private Font registerFont(PdfFontEnum fontEnum) {
        String path = normalizeFontPath(fontEnum.getPath());
        try {
            Path tempFont = extractFontFile(fontEnum);
            BaseFont baseFont = BaseFont.createFont(resolveOpenPdfFontPath(tempFont, fontEnum.getPath()),
                    IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(baseFont);
            if (font == null || font.getBaseFont() == null) {
                throw new IllegalStateException("OpenPDF did not create a usable font for " + fontEnum.getValue());
            }
            return font;
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot register JMReport PDF font: " + path, ex);
        }
    }

    private boolean isUsableFont(Font font) {
        return font != null && font.getBaseFont() != null;
    }

    private void registerFontFactoryAlias(String alias, PdfFontEnum sourceFont) {
        try {
            String fontPath = resolveOpenPdfFontPath(extractFontFile(sourceFont), sourceFont.getPath());
            FontFactory.register(fontPath, alias);
            putFontFactoryAlias(alias, fontPath);
            Font font = FontFactory.getFont(alias, IDENTITY_H, BaseFont.EMBEDDED);
            if (!isUsableFont(font)) {
                throw new IllegalStateException("OpenPDF FontFactory did not create a usable font for " + alias);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot register JMReport PDF FontFactory alias: " + alias, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void putFontFactoryAlias(String alias, String fontPath) {
        try {
            Class<?> fontFactoryImpClass = FontFactory.getFontImp().getClass();
            Field trueTypeFontsField = fontFactoryImpClass.getDeclaredField("trueTypeFonts");
            trueTypeFontsField.setAccessible(true);
            Map<String, String> trueTypeFonts = (Map<String, String>) trueTypeFontsField.get(FontFactory.getFontImp());
            trueTypeFonts.put(alias, fontPath);
            trueTypeFonts.put(alias.toLowerCase(Locale.ROOT), fontPath);
            Field fontFamiliesField = fontFactoryImpClass.getDeclaredField("fontFamilies");
            fontFamiliesField.setAccessible(true);
            Map<String, List<String>> fontFamilies =
                    (Map<String, List<String>>) fontFamiliesField.get(FontFactory.getFontImp());
            fontFamilies.put(alias.toLowerCase(Locale.ROOT), List.of(fontPath));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot access OpenPDF FontFactory font registry", ex);
        }
    }

    private PdfFontEnum resolveFontFactorySource(PdfFontEnum fontEnum) {
        if (hasBundledFont(fontEnum) && isTrueTypeFont(fontEnum)) {
            return fontEnum;
        }
        if (!hasBundledFont(PdfFontEnum.c)) {
            throw new IllegalStateException("JMReport FontFactory-compatible PDF font is missing: "
                    + PdfFontEnum.c.getPath());
        }
        return PdfFontEnum.c;
    }

    private boolean isTrueTypeFont(PdfFontEnum fontEnum) {
        return normalizeFontPath(fontEnum.getPath()).toLowerCase().endsWith(".ttf");
    }

    private Path extractFontFile(PdfFontEnum fontEnum) throws IOException {
        String path = normalizeFontPath(fontEnum.getPath());
        Path cached = EXTRACTED_FONT_FILES.get(path);
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        ClassPathResource resource = new ClassPathResource(path);
        Path tempFont = Files.createTempFile("jmreport-pdf-font-", suffixOf(path));
        try (var inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFont, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        tempFont.toFile().deleteOnExit();
        EXTRACTED_FONT_FILES.put(path, tempFont);
        return tempFont;
    }

    private boolean hasBundledFont(PdfFontEnum fontEnum) {
        return new ClassPathResource(normalizeFontPath(fontEnum.getPath())).exists();
    }

    private String normalizeFontPath(String path) {
        int ttcIndexSeparator = path.lastIndexOf(',');
        if (ttcIndexSeparator > -1) {
            return path.substring(0, ttcIndexSeparator);
        }
        return path;
    }

    private String resolveOpenPdfFontPath(Path tempFont, String sourcePath) {
        int ttcIndexSeparator = sourcePath.lastIndexOf(',');
        if (ttcIndexSeparator > -1) {
            return tempFont + sourcePath.substring(ttcIndexSeparator);
        }
        return tempFont.toString();
    }

    private String suffixOf(String path) {
        int extensionIndex = path.lastIndexOf('.');
        if (extensionIndex > -1) {
            return path.substring(extensionIndex);
        }
        return ".font";
    }

}
