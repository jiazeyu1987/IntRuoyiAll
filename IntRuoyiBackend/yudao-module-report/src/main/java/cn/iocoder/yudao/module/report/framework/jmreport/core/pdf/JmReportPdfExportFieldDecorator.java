package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class JmReportPdfExportFieldDecorator {

    private static final String BASE_EXPORT_PDF_FIELD = "baseExportPdf";

    private final JmReportPdfFontRegistry fontRegistry;

    public JmReportPdfExportFieldDecorator(JmReportPdfFontRegistry fontRegistry) {
        this.fontRegistry = fontRegistry;
    }

    boolean decorate(Object bean) {
        Field field = ReflectionUtils.findField(bean.getClass(), BASE_EXPORT_PDF_FIELD);
        if (field == null) {
            return false;
        }
        ReflectionUtils.makeAccessible(field);
        Object delegate = ReflectionUtils.getField(field, bean);
        if (delegate == null) {
            throw new IllegalStateException("JMReport designReportController baseExportPdf is not initialized");
        }
        if (delegate instanceof PdfFontReadyExportProxy) {
            return false;
        }
        Class<?>[] interfaces = appendMarkerInterface(delegate.getClass().getInterfaces());
        if (interfaces.length == 1) {
            throw new IllegalStateException("JMReport baseExportPdf has no export interface: "
                    + delegate.getClass().getName());
        }
        Object decorated = Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                interfaces,
                new PdfExportInvocationHandler(delegate, fontRegistry));
        ReflectionUtils.setField(field, bean, decorated);
        return true;
    }

    private Class<?>[] appendMarkerInterface(Class<?>[] interfaces) {
        Class<?>[] result = Arrays.copyOf(interfaces, interfaces.length + 1);
        result[interfaces.length] = PdfFontReadyExportProxy.class;
        return result;
    }

    private interface PdfFontReadyExportProxy {
    }

    private static class PdfExportInvocationHandler implements InvocationHandler {

        private final Object delegate;
        private final JmReportPdfFontRegistry fontRegistry;

        private PdfExportInvocationHandler(Object delegate, JmReportPdfFontRegistry fontRegistry) {
            this.delegate = delegate;
            this.fontRegistry = fontRegistry;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isPdfExportMethod(method, args)) {
                fontRegistry.ensurePdfFontsReady();
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        private boolean isPdfExportMethod(Method method, Object[] args) {
            return "a".equals(method.getName()) && args != null && args.length == 5;
        }

    }

}
