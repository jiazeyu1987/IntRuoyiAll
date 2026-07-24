package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

public class JmReportPdfExportSingletonDecorator implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;
    private final JmReportPdfExportFieldDecorator fieldDecorator;

    public JmReportPdfExportSingletonDecorator(ListableBeanFactory beanFactory,
                                               JmReportPdfExportFieldDecorator fieldDecorator) {
        this.beanFactory = beanFactory;
        this.fieldDecorator = fieldDecorator;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : beanFactory.getBeanNamesForType(Object.class, false, false)) {
            fieldDecorator.decorate(beanFactory.getBean(beanName));
        }
    }

}
