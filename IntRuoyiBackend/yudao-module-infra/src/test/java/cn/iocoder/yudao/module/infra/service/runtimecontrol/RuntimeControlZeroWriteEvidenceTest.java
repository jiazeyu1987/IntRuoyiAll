package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlZeroWriteEvidenceTest {
    @TempDir Path tempDir;
    private static final String PROOF = "RELEASE_WORKFLOW_ZERO_WRITE_REJECTED=RESOURCE_ACQUIRE";

    @Test
    void guardRejectionPersistsStructuredProofOfNoExecutorDispatch() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = new RuntimeControlOperationStore(properties);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class),
                mock(NasBrowserService.class));
        var request = new RuntimeControlActionReqVO();
        request.setAction("backup-now");
        request.setPreassignedOperationId("op-zero-12345678");
        assertThrows(RuntimeException.class, () -> runtime.executeAction(request, "operator"));
        var record = new ObjectMapper().findAndRegisterModules().valueToTree(operations.findById("op-zero-12345678"));
        assertTrue(record.hasNonNull("zeroWriteEvidence") && record.get("zeroWriteEvidence").booleanValue(),
                "guard rejection must carry structured zero-write evidence, not an ambiguous failed status");
        verifyNoInteractions(executor);
    }

    @Test
    void approvedStructuredRejectionCanBePersistedAsZeroWrite() throws Exception {
        Path output = tempDir.resolve("executor-output.log");
        Files.writeString(output, PROOF + "\r\n", StandardCharsets.UTF_8);
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO();
        operation.setOperationId("op-proof-12345678");
        operation.setZeroWriteEvidence(false);
        assertTrue(operations.createIfAbsent(operation));
        operations.updateResult(operation.getOperationId(), "failed", "resource rejected",
                RuntimeControlExecutionEvidence.confirmedZeroWriteRejection(output));
        assertEquals(Boolean.TRUE, operations.findById(operation.getOperationId()).getZeroWriteEvidence());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "SSH failed", "prefix ", "owner", "acquired", "duplicate"})
    void humanPartialAmbiguousOrPostAcquireOutputCannotProveZeroWrites(String scenario) throws Exception {
        String outputText = switch (scenario) {
            case "owner" -> PROOF + "\nRUNTIME_RESOURCE_LEASE_TOKEN=owner123\n";
            case "acquired" -> "RUNTIME_LEASE_ACQUIRED\n" + PROOF;
            case "duplicate" -> PROOF + "\n" + PROOF;
            case "prefix " -> scenario + PROOF;
            default -> scenario;
        };
        Path output = tempDir.resolve("unknown-output.log");
        Files.writeString(output, outputText, StandardCharsets.UTF_8);
        assertFalse(RuntimeControlExecutionEvidence.confirmedZeroWriteRejection(output));
    }

    @Test
    void oldOperationWithoutEvidenceRemainsUnknown() throws Exception {
        var old = new ObjectMapper().readValue("{\"operationId\":\"op-legacy-12345678\"}",
                cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO.class);
        assertNull(old.getZeroWriteEvidence());
    }

    @Test
    void possibleWriteIsPersistedBeforeExecutorCanRun() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        var operations = new RuntimeControlOperationStore(properties);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), mock(NasBrowserService.class));
        var observed = new java.util.concurrent.atomic.AtomicReference<Boolean>();
        var invoked = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            observed.set(operations.findById("op-dispatch-12345678").getZeroWriteEvidence());
            invoked.countDown();
            return null;
        }).when(executor).executeOperation(any(), any());
        Path sql = tempDir.resolve("no-database-executor-fixture.sql");
        Files.writeString(sql, "SELECT 1;", StandardCharsets.UTF_8);
        var request = new RuntimeControlActionReqVO();
        request.setAction("apply-test-db-sql");
        request.setReason("unit dispatch proof");
        request.setPreassignedOperationId("op-dispatch-12345678");
        request.setSqlPath(sql.toString());
        try {
            runtime.executeAction(request, "operator");
            assertTrue(invoked.await(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(Boolean.FALSE, observed.get());
        } finally {
            var field = RuntimeControlServiceImpl.class.getDeclaredField("operationExecutor");
            field.setAccessible(true);
            var pool = (java.util.concurrent.ExecutorService) field.get(runtime);
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
        }
    }
}
