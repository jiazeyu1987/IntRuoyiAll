package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JmReportPdfFontRegistryInitializerTest {

    @Test
    void afterSingletonsInstantiated_shouldPrepareOpenPdfFontsBeforeAnyExportRequest() {
        RecordingFontRegistry fontRegistry = new RecordingFontRegistry();

        new JmReportPdfFontRegistryInitializer(fontRegistry).afterSingletonsInstantiated();

        assertTrue(fontRegistry.called);
    }

    private static class RecordingFontRegistry extends JmReportPdfFontRegistry {

        private boolean called;

        @Override
        public void ensurePdfFontsReady() {
            called = true;
        }

    }

}
