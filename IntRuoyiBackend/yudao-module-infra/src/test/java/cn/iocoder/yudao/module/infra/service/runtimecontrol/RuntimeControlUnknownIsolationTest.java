package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.*;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlUnknownIsolationTest {
    @TempDir Path tempDir;
    private final List<RuntimeControlServiceImpl> instances = new ArrayList<>();

    @AfterEach
    void awaitOnlyTestOwnedDispatchersBeforeTemporaryDirectoryCleanup() throws Exception {
        var field = RuntimeControlServiceImpl.class.getDeclaredField("operationExecutor");
        field.setAccessible(true);
        for (var instance : instances) {
            var executor = (ExecutorService) field.get(instance);
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void restartCannotIgnoreUnknownOlderThanTwoHundredRecentOperations() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = historyWithOldUnknown(properties, "backup");
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = runtime(properties, operations, executor);
        var failure = assertThrows(RuntimeException.class, () -> runtime.restart(restart("backup"), "operator"));
        assertTrue(failure.getMessage().contains("UNKNOWN"), failure.getMessage());
        verifyNoInteractions(executor);
        assertEquals("unknown", operations.findById("op-old-unknown").getStatus());
    }

    @Test
    void actionGuardAlsoChecksCompleteUnknownHistory() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = historyWithOldUnknown(properties, "test");
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = runtime(properties, operations, executor);
        Path sql = tempDir.resolve("readonly-test-fixture.sql");
        Files.writeString(sql, "SELECT 1;", StandardCharsets.UTF_8);
        var request = new RuntimeControlActionReqVO();
        request.setAction("apply-test-db-sql");
        request.setReason("verify unknown isolation");
        request.setSqlPath(sql.toString());
        var failure = assertThrows(RuntimeException.class, () -> runtime.executeAction(request, "operator"));
        assertTrue(failure.getMessage().contains("UNKNOWN"), failure.getMessage());
        verifyNoInteractions(executor);
        assertEquals("unknown", operations.findById("op-old-unknown").getStatus());
    }

    @ParameterizedTest
    @CsvSource({"test,backup", "backup,test"})
    void unrelatedEnvironmentUnknownDoesNotBlockTarget(String unknownEnvironment, String target) throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = runtime(properties, historyWithOldUnknown(properties, unknownEnvironment), executor);
        assertDoesNotThrow(() -> runtime.restart(restart(target), "operator"));
        verify(executor).restart(argThat(command -> target.equals(command.getEnvironment())));
    }

    private RuntimeControlOperationStore historyWithOldUnknown(RuntimeControlProperties properties, String environment)
            throws Exception {
        var operations = new RuntimeControlOperationStore(properties);
        var unknown = new RuntimeControlOperationRespVO();
        unknown.setOperationId("op-old-unknown");
        unknown.setEnvironment(environment);
        unknown.setStatus("unknown");
        operations.save(unknown);
        Files.setLastModifiedTime(operations.getOperationPath(unknown.getOperationId()), FileTime.fromMillis(1000));
        for (int i = 0; i < 205; i++) {
            var completed = new RuntimeControlOperationRespVO();
            completed.setOperationId("op-history-" + i);
            completed.setEnvironment(environment);
            completed.setStatus("succeeded");
            operations.save(completed);
            Files.setLastModifiedTime(operations.getOperationPath(completed.getOperationId()), FileTime.fromMillis(2000 + i));
        }
        return operations;
    }

    private RuntimeControlRestartReqVO restart(String environment) {
        var request = new RuntimeControlRestartReqVO();
        request.setEnvironment(environment);
        request.setComponent("intruoyi-backend");
        request.setReason("verify unknown isolation");
        request.setProdConfirmText("PROD");
        return request;
    }

    private RuntimeControlServiceImpl runtime(RuntimeControlProperties properties,
                                               RuntimeControlOperationStore operations,
                                               RuntimeControlCommandExecutor executor) {
        var service = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class),
                mock(NasBrowserService.class));
        instances.add(service);
        return service;
    }
}
