package cn.iocoder.yudao.module.report.framework.jmreport.config;

import cn.iocoder.yudao.module.report.framework.jmreport.core.pdf.JmReportPdfExportBeanPostProcessor;
import cn.iocoder.yudao.module.report.framework.jmreport.core.pdf.JmReportPdfExportFieldDecorator;
import cn.iocoder.yudao.module.report.framework.jmreport.core.pdf.JmReportPdfExportSingletonDecorator;
import cn.iocoder.yudao.module.report.framework.jmreport.core.pdf.JmReportPdfFontRegistry;
import cn.iocoder.yudao.module.report.framework.jmreport.core.pdf.JmReportPdfFontRegistryInitializer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JmReportPdfExportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JmReportPdfFontRegistry jmReportPdfFontRegistry() {
        return new JmReportPdfFontRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public JmReportPdfFontRegistryInitializer jmReportPdfFontRegistryInitializer(
            JmReportPdfFontRegistry fontRegistry) {
        return new JmReportPdfFontRegistryInitializer(fontRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public JmReportPdfExportBeanPostProcessor jmReportPdfExportBeanPostProcessor(
            JmReportPdfFontRegistry fontRegistry) {
        return new JmReportPdfExportBeanPostProcessor(fontRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public JmReportPdfExportSingletonDecorator jmReportPdfExportSingletonDecorator(
            ListableBeanFactory beanFactory, JmReportPdfFontRegistry fontRegistry) {
        return new JmReportPdfExportSingletonDecorator(beanFactory, new JmReportPdfExportFieldDecorator(fontRegistry));
    }

}
