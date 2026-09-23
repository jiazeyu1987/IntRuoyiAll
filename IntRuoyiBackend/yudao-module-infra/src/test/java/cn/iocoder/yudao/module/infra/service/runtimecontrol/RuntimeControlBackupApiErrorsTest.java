package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.*;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.*;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlBackupApiErrorsTest {
    @Test void controlledPreviewErrorReachesApiWithoutLeakingExceptionDetails() {
        var orchestrator = mock(ReleaseWorkflowOrchestrator.class);
        when(orchestrator.previewBackup("122", "review", "source", "request-12345678"))
                .thenThrow(new IllegalArgumentException("RELEASE_WORKFLOW_PNPM_VERSION_REQUIRED: token=private-value"));
        var controller = new RuntimeControlController();
        ReflectionTestUtils.setField(controller, "releaseWorkflowOrchestrator", orchestrator);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(122L);
            var error = assertThrows(ServiceException.class, () -> controller.previewBackupPublish(
                    new RuntimeControlBackupPublishRequests.Preview("review", "source", "request-12345678")));
            assertTrue(error.getMessage().contains("RELEASE_WORKFLOW_PNPM_VERSION_REQUIRED"));
            assertFalse(error.getMessage().contains("private-value"));
        }
    }
    @Test void controlledRecoveryErrorReachesApiWithoutRawRemoteStderr() {
        var service = mock(ReleaseWorkflowBackupRecoveryService.class);
        when(service.inspect("rw-backup-12345678", 1, "122"))
                .thenThrow(new IllegalStateException("RECOVERY_WORKFLOW_STATE_INVALID: password=private-value"));
        var controller = new RuntimeControlBackupRecoveryController(service);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(122L);
            var error = assertThrows(ServiceException.class, () -> controller.inspect("rw-backup-12345678",
                    new RuntimeControlBackupRecoveryRequests.Inspect(1L)));
            assertTrue(error.getMessage().contains("RECOVERY_WORKFLOW_STATE_INVALID"));
            assertFalse(error.getMessage().contains("private-value"));
        }
    }
}
