package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlBackupDiagnosticLogTest {
    @TempDir Path tempDir;

    @ParameterizedTest
    @CsvSource({"publish-backup,操作完成：成功", "publish-backup,操作完成：失败",
            "build-release,操作完成：成功", "build-release,操作完成：失败"})
    void reviewWorkflowLogReadsCannotChangeStructuredOperationStatus(String action, String humanText) throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId("op-diagnostic-12345678"); operation.setAction(action);
        operation.setEnvironment("backup"); operation.setStatus("running");
        Path log = operations.getOperationLogPath(operation.getOperationId());
        operation.setResultLogPath(log.toString()); operations.save(operation); operations.initializeLog(log);
        Files.writeString(log, humanText + "\n", StandardCharsets.UTF_8);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), mock(NasBrowserService.class));
        var visibleLog = assertDoesNotThrow(() -> runtime.getOperationLog(operation.getOperationId(), 4096));
        assertEquals("running", visibleLog.getStatus());
        assertTrue(visibleLog.getContent().contains(humanText));
        assertEquals("running", runtime.getOperations().get(0).getStatus());
        assertEquals("running", operations.findById(operation.getOperationId()).getStatus());
        verifyNoInteractions(executor);
    }
}
