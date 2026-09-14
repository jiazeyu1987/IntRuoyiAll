package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.RuntimeControlController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeOpsTrustedTimeEndpointContractTest {

    @Test
    void controllerShouldExposeReadonlyTimeEvidenceZipForSavedInspection() throws Exception {
        Method method = RuntimeControlController.class.getDeclaredMethod(
                "exportInspectionTimeEvidence", Long.class, jakarta.servlet.http.HttpServletResponse.class);

        assertArrayEquals(new String[]{"/inspection-runs/{id}/time-evidence.zip"},
                method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('infra:runtime-control:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals(void.class, method.getReturnType());
    }
}
