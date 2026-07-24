package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.springframework.beans.factory.SmartInitializingSingleton;

public class JmReportPdfFontRegistryInitializer implements SmartInitializingSingleton {

    private final JmReportPdfFontRegistry fontRegistry;

    public JmReportPdfFontRegistryInitializer(JmReportPdfFontRegistry fontRegistry) {
        this.fontRegistry = fontRegistry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        fontRegistry.ensurePdfFontsReady();
    }

}
