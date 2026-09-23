package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlFrozenBackupToolchainTest {
    @TempDir Path tempDir;

    @Test
    void matchingFileAndRequestedHashCannotReplaceApprovedExecutorDigest() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        String workflowId = "rw-backup-" + "a".repeat(32);
        Path workflow = Path.of(properties.getReleaseWorkflow().getWorktreeRoot()).resolve(workflowId);
        Path maintenance = Files.createDirectories(workflow.resolve("maintenance"));
        Path backend = Files.createDirectories(workflow.resolve("application/IntRuoyiBackend"));
        Path frontend = Files.createDirectories(workflow.resolve("application/IntRuoyiFronted"));
        Path script = maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "Write-Output 'unapproved executor'", StandardCharsets.UTF_8);
        String actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(script)));
        assertNotEquals(properties.getReleaseWorkflow().getExpectedPublishScriptSha256(), actualHash);
        var request = new RuntimeControlActionReqVO();
        request.setReleaseWorkflowId(workflowId);
        request.setFrozenMaintenanceRoot(maintenance.toString());
        request.setFrozenBackendRoot(backend.toString());
        request.setFrozenFrontendRoot(frontend.toString());
        request.setFrozenPublishScriptSha256(actualHash);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, new RuntimeControlOperationStore(properties),
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        var binder = RuntimeControlServiceImpl.class.getDeclaredMethod("bindFrozenBackupToolchain",
                RuntimeControlOperationAction.class, RuntimeControlCommand.class, RuntimeControlActionReqVO.class);
        binder.setAccessible(true);
        var command = new RuntimeControlCommand("backup", "ops", script.toString(), new ArrayList<>());
        var failure = assertThrows(InvocationTargetException.class,
                () -> binder.invoke(runtime, RuntimeControlOperationAction.BUILD_RELEASE, command, request));
        assertTrue(failure.getCause().getMessage().contains("DIGEST"), failure.getCause().getMessage());
        verifyNoInteractions(executor, nas);
    }
}
