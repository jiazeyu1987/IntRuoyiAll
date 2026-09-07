package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.module.dcc.service.file.DccPublicationFollowupQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DccPublicationFollowupControllerTest {

    @Test
    void detailUsesCurrentFileAccessAndManagementEndpointsHaveControllerGuards() throws Exception {
        DccPublicationFollowupController controller = new DccPublicationFollowupController(
                mock(DccPublicationFollowupQueryService.class),
                mock(DccPublicationNotificationService.class));
        Method detail = method(controller, "getFileFollowup");
        Method manage = method(controller, "getManagementPage");
        Method retry = method(controller, "retryNotification");

        assertTrue(detail.getAnnotation(PreAuthorize.class).value().contains("isAuthenticated"));
        for (Method method : new Method[]{manage, retry}) {
            String guard = method.getAnnotation(PreAuthorize.class).value();
            assertTrue(guard.contains("hasRole('doc_control')"));
            assertTrue(guard.contains("dcc:controlled-file:approve"));
            assertTrue(guard.contains("dcc:controlled-file:publication-followup:manage"));
        }
    }

    private Method method(Object controller, String name) {
        return Arrays.stream(controller.getClass().getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
    }
}
