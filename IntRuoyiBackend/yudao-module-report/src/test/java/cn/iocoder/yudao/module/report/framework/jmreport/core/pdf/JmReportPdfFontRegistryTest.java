package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import org.jeecg.modules.jmreport.desreport.pdf.enums.PdfFontEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JmReportPdfFontRegistryTest {

    @AfterEach
    void tearDown() throws Exception {
        setOpenPdfFontMap(new HashMap<>());
    }

    @Test
    void ensurePdfFontsReady_shouldRegisterClasspathSongTiForOpenPdfRenderer() throws Exception {
        setOpenPdfFontMap(new HashMap<>());

        new JmReportPdfFontRegistry().ensurePdfFontsReady();

        Font songTi = getOpenPdfFontMap().get(PdfFontEnum.b.getValue());
        assertNotNull(songTi);
        assertDoesNotThrow(songTi::getBaseFont);
        assertNotNull(songTi.getBaseFont());
    }

    @Test
    void ensurePdfFontsReady_shouldProvideUsableFontForEveryDeclaredJimuFontKey() throws Exception {
        setOpenPdfFontMap(new HashMap<>());

        new JmReportPdfFontRegistry().ensurePdfFontsReady();

        Map<String, Font> fontMap = getOpenPdfFontMap();
        for (PdfFontEnum fontEnum : PdfFontEnum.a()) {
            Font font = fontMap.get(fontEnum.getValue());
            assertNotNull(font, "Missing font for " + fontEnum.getValue());
            assertDoesNotThrow(font::getBaseFont);
            assertNotNull(font.getBaseFont(), "Missing base font for " + fontEnum.getValue());
        }
    }

    @Test
    void ensurePdfFontsReady_shouldRegisterEveryDeclaredJimuFontKeyWithOpenPdfFontFactory() throws Exception {
        setOpenPdfFontMap(new HashMap<>());

        new JmReportPdfFontRegistry().ensurePdfFontsReady();

        for (PdfFontEnum fontEnum : PdfFontEnum.a()) {
            Font font = FontFactory.getFont(fontEnum.getValue(), "Identity-H", true);
            assertNotNull(font, "Missing FontFactory font for " + fontEnum.getValue());
            assertDoesNotThrow(font::getBaseFont);
            assertNotNull(font.getBaseFont(), "Missing FontFactory base font for " + fontEnum.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Font> getOpenPdfFontMap() throws Exception {
        return (Map<String, Font>) openPdfFontMapField().get(null);
    }

    private void setOpenPdfFontMap(Map<String, Font> fontMap) throws Exception {
        openPdfFontMapField().set(null, fontMap);
    }

    private Field openPdfFontMapField() throws Exception {
        Class<?> rendererClass = Class.forName("org.jeecg.modules.jmreport.desreport.pdf.b.c");
        Field field = rendererClass.getField("e");
        field.setAccessible(true);
        return field;
    }

}
