package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactCreateRevisionReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccImpactRevisionCommandService;
import cn.iocoder.yudao.module.dcc.service.file.DccRelatedFileImpactAssessmentService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccPublicationImpactAssessmentControllerTest extends BaseMockitoUnitTest {

    @Mock private DccRelatedFileImpactAssessmentService impactService;
    @Mock private DccImpactRevisionCommandService revisionCommandService;
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
        Method createRevision = controller.getClass().getDeclaredMethod(
                "createRevision", Long.class, DccPublicationImpactCreateRevisionReqVO.class);
        assertTrue(createRevision.getAnnotation(PreAuthorize.class).value()
                .contains("dcc:controlled-file:submit"));
    }

    @Test
    void createRevisionDelegatesToCycleFreeWorkflowCoordinator() {
        DccPublicationImpactCreateRevisionReqVO request = new DccPublicationImpactCreateRevisionReqVO();
        request.setExpectedVersion(2);
        request.setSourceControlledFileId(200L);
        request.setReason("同步关联文件");
        when(revisionCommandService.createAndLinkMajorRevision(99L, 10L, 2, 200L, "同步关联文件"))
                .thenReturn(501L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertEquals(501L, controller.createRevision(10L, request).getData());
        }

        verify(revisionCommandService).createAndLinkMajorRevision(99L, 10L, 2, 200L, "同步关联文件");
    }
}
