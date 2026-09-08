package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RuntimeControlAuditServerTerminologyTest {

    @TempDir
    private Path tempDir;

    @Test
    void backendUserFacingLabelsShouldUseAuditServerWhileTechnicalKeysRemainStable() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);

        assertEquals("审查服", properties.getEnvironments().get("backup").getLabel());
        assertEquals("上线审查服", RuntimeControlOperationAction.PROMOTE_BACKUP.getLabel());
        assertEquals("promote-backup", RuntimeControlOperationAction.PROMOTE_BACKUP.getAction());
        assertEquals("backup", RuntimeControlOperationAction.PROMOTE_BACKUP.getEnvironment());
        assertEquals("172.30.30.59", RuntimeControlProperties.BACKUP_SERVER_HOST);

        RuntimeRemoteRootDiskService service = new RuntimeRemoteRootDiskServiceImpl(
                properties, mock(RuntimeControlCommandExecutor.class));
        RuntimeControlRemoteRootCleanupReqVO request = new RuntimeControlRemoteRootCleanupReqVO();
        request.setTargetEnvironment("backup");
        request.setReason("验证审查服清理确认口径");
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.cleanup(request, "1001"));
        assertTrue(exception.getMessage().contains("正式服/审查服务器根分区清理必须输入 PROD"));

        Schema schema = RuntimeControlRemoteRootCleanupReqVO.class
                .getDeclaredField("prodConfirmText").getAnnotation(Schema.class);
        assertTrue(schema.description().contains("正式服或审查服务器"));
        assertFalse(schema.description().contains("备用服务器"));
        assertFalse(schema.description().contains("备份服务器"));
    }
}
