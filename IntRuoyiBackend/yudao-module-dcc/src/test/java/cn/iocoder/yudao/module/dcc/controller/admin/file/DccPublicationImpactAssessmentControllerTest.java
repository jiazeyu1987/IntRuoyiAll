package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccRelatedFileImpactAssessmentService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DccPublicationImpactAssessmentControllerTest extends BaseMockitoUnitTest {

    @Mock private DccRelatedFileImpactAssessmentService impactService;
    @InjectMocks private DccPublicationImpactAssessmentController controller;

    @Test
    void managementEndpointsRequireDocControlRoleAndApprovePermission() throws Exception {
        for (String methodName : new String[]{"reassign", "reopen"}) {
            Method method = java.util.Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
            String guard = method.getAnnotation(PreAuthorize.class).value();
            assertTrue(guard.contains("hasRole('doc_control')"));
            assertTrue(guard.contains("dcc:controlled-file:approve"));
        }
        assertFalse(java.util.Arrays.stream(controller.getClass().getDeclaredMethods())
                .anyMatch(candidate -> candidate.getName().equals("createRevision")));
        Method revisionOptions = controller.getClass().getDeclaredMethod("getRevisionOptions", Long.class);
        assertTrue(revisionOptions.getAnnotation(PreAuthorize.class).value().contains("isAuthenticated"));
    }

}
