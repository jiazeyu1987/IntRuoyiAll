package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmReportPdfExportBeanPostProcessorTest {

    @Test
    void postProcessAfterInitialization_shouldDecorateDesignReportControllerPdfExporter() throws Exception {
        Object controller = Class.forName("org.jeecg.modules.jmreport.desreport.b.a")
                .getDeclaredConstructor().newInstance();
        Class<?> exportInterface = Class.forName("org.jeecg.modules.jmreport.desreport.pdf.a.a.a");
        AtomicBoolean delegated = new AtomicBoolean(false);
        Object delegate = Proxy.newProxyInstance(exportInterface.getClassLoader(), new Class<?>[]{exportInterface},
                (proxy, method, args) -> {
                    delegated.set(true);
                    return null;
                });
        baseExportPdfField(controller).set(controller, delegate);
        RecordingFontRegistry fontRegistry = new RecordingFontRegistry();

        new JmReportPdfExportBeanPostProcessor(fontRegistry)
                .postProcessAfterInitialization(controller, "thirdPartyControllerBeanName");

        Object decorated = baseExportPdfField(controller).get(controller);
        assertNotSame(delegate, decorated);
        invokePdfExport(exportInterface, decorated);
        assertTrue(fontRegistry.called);
        assertTrue(delegated.get());
    }

    private void invokePdfExport(Class<?> exportInterface, Object decorated) throws Exception {
        Method method = exportInterface.getMethod("a",
                Class.forName("org.jeecg.modules.jmreport.desreport.entity.JimuReport"),
                com.alibaba.fastjson.JSONArray.class,
                float.class,
                jakarta.servlet.http.HttpServletResponse.class,
                com.alibaba.fastjson.JSONObject.class);
        method.invoke(decorated, null, null, 96.0f, null, null);
    }

    private Field baseExportPdfField(Object controller) throws Exception {
        Field field = controller.getClass().getDeclaredField("baseExportPdf");
        field.setAccessible(true);
        return field;
    }

    private static class RecordingFontRegistry extends JmReportPdfFontRegistry {

        private boolean called;

        @Override
        public void ensurePdfFontsReady() {
            called = true;
        }

    }

}
