package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class JmReportPdfExportBeanPostProcessor implements BeanPostProcessor {

    private final JmReportPdfExportFieldDecorator fieldDecorator;

    public JmReportPdfExportBeanPostProcessor(JmReportPdfFontRegistry fontRegistry) {
        this(new JmReportPdfExportFieldDecorator(fontRegistry));
    }

    JmReportPdfExportBeanPostProcessor(JmReportPdfExportFieldDecorator fieldDecorator) {
        this.fieldDecorator = fieldDecorator;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        fieldDecorator.decorate(bean);
        return bean;
    }

}
