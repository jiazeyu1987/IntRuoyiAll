package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlOperationClaimTest {
    @TempDir Path tempDir;

    @Test
    void anotherActionCannotOverwriteAnExistingDispatchJournal() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var store = new RuntimeControlOperationStore(properties);
        var original = new RuntimeControlOperationRespVO();
        original.setOperationId("op-review-12345678");
        original.setAction("publish-backup");
        original.setEnvironment("backup");
        original.setStatus("running");
        store.save(original);
        byte[] bytes = Files.readAllBytes(store.getOperationPath(original.getOperationId()));
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var service = new RuntimeControlServiceImpl(properties, executor, store,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        var request = new RuntimeControlActionReqVO();
        request.setAction("backup-now");
        request.setPreassignedOperationId(original.getOperationId());
        assertThrows(RuntimeException.class, () -> service.executeAction(request, "another-operator"));
        assertArrayEquals(bytes, Files.readAllBytes(store.getOperationPath(original.getOperationId())),
                "initial dispatch must atomically claim its journal instead of replacing another operation");
        verifyNoInteractions(executor, nas);
    }

    @Test
    void distinctStoreInstancesHaveExactlyOnePermanentClaimWinner() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 16; i++) {
                final String actor = "operator-" + i;
                futures.add(pool.submit(() -> {
                    start.await();
                    var operation = new RuntimeControlOperationRespVO();
                    operation.setOperationId("op-review-12345678");
                    operation.setRequestedBy(actor);
                    return new RuntimeControlOperationStore(properties).createIfAbsent(operation);
                }));
            }
            start.countDown();
            int winners = 0;
            for (var future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) winners++;
            }
            assertEquals(1, winners);
            assertNotNull(new RuntimeControlOperationStore(properties).findById("op-review-12345678").getRequestedBy());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void interruptedClaimIsNeverReplacedOrRedispatched() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var store = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId("op-review-12345678");
        Files.createDirectories(store.getOperationPath(operation.getOperationId()).getParent());
        Files.writeString(store.getOperationPath(operation.getOperationId()), "{", java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(new RuntimeControlOperationStore(properties).createIfAbsent(operation));
        assertThrows(RuntimeException.class, () -> store.findById(operation.getOperationId()));
        assertEquals("{", Files.readString(store.getOperationPath(operation.getOperationId()), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void previewPrerequisitesExposeMissingOwnersWithoutExternalIo() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var owners = mock(RuntimeOpsResponsibilityService.class);
        var expected = new IllegalArgumentException("review owners missing");
        doThrow(expected).when(owners).validateRequiredOwners("backup", "publish-backup");
        var service = new RuntimeControlServiceImpl(properties, executor, new RuntimeControlOperationStore(properties),
                owners, mock(RuntimeOpsCandidateService.class), mock(RuntimeControlReleasePackageConfigService.class),
                mock(NasSettingsService.class), nas);
        assertSame(expected, assertThrows(IllegalArgumentException.class, service::validateBackupPublishPrerequisites));
        verifyNoInteractions(executor, nas);
    }

    @Test
    void missingRestoreIsolationConfigurationBlocksPreviewBeforeBuild() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        properties.getBackupOps().setConfigPath(tempDir.resolve("missing-backup-config.json").toString());
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var service = new RuntimeControlServiceImpl(properties, executor, new RuntimeControlOperationStore(properties),
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        assertThrows(IllegalStateException.class, service::validateBackupPublishPrerequisites);
        verifyNoInteractions(executor, nas);
    }
}
