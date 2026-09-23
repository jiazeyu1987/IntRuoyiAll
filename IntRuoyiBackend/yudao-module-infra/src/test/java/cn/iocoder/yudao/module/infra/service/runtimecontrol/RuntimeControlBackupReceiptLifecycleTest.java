package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlBackupReceiptLifecycleTest {
    @TempDir Path tempDir;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptedRemoteReceiptSurvivesLocalNasCleanupFailureWithoutClaimingAck() throws Exception {
        var f = fixture();
        var receipt = receipt(f.workflow(), "valid");
        doAnswer(call -> {
            Files.writeString(f.log(), "REVIEW_PUBLISH_RECEIPT_JSON=" + receipt + "\n", StandardCharsets.UTF_8);
            return null;
        }).when(f.executor()).executeOperation(any(), any());
        Path undeletableDirectory = Files.createDirectories(tempDir.resolve("cleanup-failure"));
        Files.writeString(undeletableDirectory.resolve("owned-fixture.txt"), "unit cleanup failure", StandardCharsets.UTF_8);
        invokeCompletion(f, undeletableDirectory);
        var operation = mapper.readTree(f.operations().getOperationPath(f.workflow().operationId()).toFile());
        assertEquals(receipt.get("receiptDigest").asText(), operation.path("backupReceiptDigest").asText());
        assertEquals("AWAITING_CONFIRMATION", operation.path("backupConfirmationState").asText());
        assertFalse(operation.path("localCleanupError").asText().isBlank());
        assertNotEquals("succeeded", operation.path("status").asText());
        assertTrue(Files.isRegularFile(tempDir.resolve("backup-receipts").resolve(f.workflow().operationId() + ".json")));
        verify(f.executor(), never()).executeForOutput(any(), any());
    }

    @Test
    void receiptRemainsRecoverableWhenExecutorFailsAfterReceivingIt() throws Exception {
        var f = fixture();
        var receipt = receipt(f.workflow(), "valid");
        doAnswer(call -> {
            Files.writeString(f.log(), "REVIEW_PUBLISH_RECEIPT_JSON=" + receipt + "\n", StandardCharsets.UTF_8);
            throw new IllegalStateException("unit transport failed after durable receipt");
        }).when(f.executor()).executeOperation(any(), any());
        invokeCompletion(f, null);
        var operation = mapper.readTree(f.operations().getOperationPath(f.workflow().operationId()).toFile());
        assertEquals(receipt.get("receiptDigest").asText(), operation.path("backupReceiptDigest").asText());
        assertEquals("AWAITING_CONFIRMATION", operation.path("backupConfirmationState").asText());
        assertNotEquals("succeeded", operation.path("status").asText());
        assertFalse(operation.path("zeroWriteEvidence").asBoolean());
    }

    @Test
    void separateStoreInstancesSerializeLateCleanupWithConfirmation() throws Exception {
        var f = fixture();
        var awaiting = mapper.treeToValue(receipt(f.workflow(), "valid"), RuntimeControlBackupPublicationReceipt.class);
        awaiting.verifyFor(f.workflow());
        var confirmed = new RuntimeControlBackupPublicationReceipt(1, "CONFIRMED", awaiting.binding(), awaiting.runtime(),
                awaiting.receiptDigest(), awaiting.receiptBase64(), "e".repeat(64));
        var secondStore = new RuntimeControlOperationStore(RuntimeControlProperties.createDefaultForTests(tempDir));
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var worker = pool.submit(() -> {
                start.await();
                secondStore.archiveBackupReceipt(f.workflow().operationId(), awaiting, "late worker error");
                secondStore.recordLocalCleanupError(f.workflow().operationId(), "local cleanup error");
                return null;
            });
            var finalizer = pool.submit(() -> {
                start.await();
                f.operations().archiveBackupReceipt(f.workflow().operationId(), confirmed, null);
                f.operations().completeBackupPublication(f.workflow().operationId(), confirmed);
                return null;
            });
            start.countDown();
            worker.get(10, java.util.concurrent.TimeUnit.SECONDS);
            finalizer.get(10, java.util.concurrent.TimeUnit.SECONDS);
            var operation = f.operations().findById(f.workflow().operationId());
            assertEquals("succeeded", operation.getStatus());
            assertEquals("CONFIRMED", operation.getBackupConfirmationState());
            assertEquals("local cleanup error", operation.getLocalCleanupError());
            assertEquals("late worker error", operation.getExecutionError());
            assertEquals(confirmed, f.operations().findBackupReceipt(f.workflow().operationId()).orElseThrow());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"digest", "runtime", "tuple", "missing"})
    void exitZeroCannotReplaceBoundRuntimeAcceptanceEvidence(String fault) throws Exception {
        var f = fixture();
        var receipt = receipt(f.workflow(), fault);
        doAnswer(call -> {
            Files.writeString(f.log(), "missing".equals(fault) ? "ordinary output\n"
                    : "REVIEW_PUBLISH_RECEIPT_JSON=" + receipt + "\n", StandardCharsets.UTF_8);
            return null;
        }).when(f.executor()).executeOperation(any(), any());
        invokeCompletion(f, null);
        var operation = mapper.readTree(f.operations().getOperationPath(f.workflow().operationId()).toFile());
        assertEquals("failed", operation.path("status").asText(), "receipt validation must gate success");
        assertFalse(operation.hasNonNull("backupReceiptDigest"), "invalid receipt must not be archived");
        assertFalse(Files.exists(f.operations().backupReceiptPath(f.workflow().operationId())));
        String rejection = switch (fault) {
            case "digest" -> "RECEIPT_DIGEST_MISMATCH";
            case "runtime" -> "RUNTIME_ACCEPTANCE_FAILED";
            case "tuple" -> "RECEIPT_BINDING_MISMATCH";
            default -> "RECEIPT_OUTPUT_REQUIRED";
        };
        assertTrue(operation.path("summary").asText().contains(rejection), operation.path("summary").asText());
        assertFalse(operation.path("zeroWriteEvidence").asBoolean());
    }

    private Fixture fixture() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var authorization = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = authorization.preview("operator", "unit receipt lifecycle", "approved-source", "receipt-request-12345678");
        var grant = authorization.authorize(preview.previewId(), "operator", "PROD");
        var workflows = new ReleaseWorkflowStore(properties);
        var workflow = new ReleaseWorkflowService(properties, workflows).createBackup(grant);
        workflow = workflows.bindArtifacts(workflow, "a".repeat(64), "b".repeat(64), "verifier:unit");
        workflow = workflows.assignOperation(workflow, workflow.stateVersion(), "op-receipt-12345678", "verifier:unit");
        workflow = workflows.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.BACKUP_DEPLOYING,
                "verifier:unit", null, null, false, List.of(), false);
        var request = new RuntimeControlActionReqVO();
        request.setAction("publish-backup"); request.setTargetEnvironment("backup"); request.setProdConfirmText("PROD");
        request.setReleaseWorkflowId(workflow.workflowId()); request.setReleaseWorkflowExpectedStateVersion(workflow.stateVersion());
        request.setPreassignedOperationId(workflow.operationId()); request.setReleaseTag(workflow.releaseTag());
        request.setReleaseAuthorizationId(grant.authorizationId()); request.setReason(workflow.reason());
        request.setSourceSelectionId(workflow.sourceSelectionId()); request.setExpectedMaintenanceCommit(workflow.maintenanceCommit());
        request.setExpectedApplicationCommit(workflow.applicationCommit()); request.setExpectedFrontendCommit(workflow.frontendCommit());
        request.setExpectedPackageDigest(workflow.packageDigest()); request.setExpectedManifestDigest(workflow.manifestDigest());
        var operation = new RuntimeControlOperationRespVO(); operation.setOperationId(workflow.operationId());
        operation.setAction("publish-backup"); operation.setEnvironment("backup"); operation.setRequestedBy("operator");
        operation.setReason(workflow.reason());
        operation.setParameters(java.util.Map.of("workflowId", workflow.workflowId(), "releaseTag", workflow.releaseTag(),
                "authorizationId", grant.authorizationId(), "sourceSelectionId", workflow.sourceSelectionId(),
                "maintenanceCommit", workflow.maintenanceCommit(), "applicationCommit", workflow.applicationCommit(),
                "frontendCommit", workflow.frontendCommit(), "packageDigest", workflow.packageDigest(),
                "manifestDigest", workflow.manifestDigest()));
        operation.setStatus("running"); operation.setZeroWriteEvidence(false);
        var operations = new RuntimeControlOperationStore(properties);
        operation.setResultLogPath(operations.getOperationLogPath(workflow.operationId()).toString());
        operations.createIfAbsent(operation);
        Path log = operations.getOperationLogPath(workflow.operationId()); operations.initializeLog(log);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), mock(NasBrowserService.class));
        return new Fixture(runtime, operations, executor, workflow, request, log);
    }

    private ObjectNode receipt(ReleaseWorkflowRecord workflow, String fault) throws Exception {
        var core = mapper.createObjectNode(); core.put("schemaVersion", 1);
        var binding = core.putObject("binding"); binding.put("workflowId", workflow.workflowId());
        binding.put("operationId", workflow.operationId()); binding.put("releaseTag", workflow.releaseTag());
        binding.put("packageDigest", "tuple".equals(fault) ? "c".repeat(64) : workflow.packageDigest());
        binding.put("manifestDigest", workflow.manifestDigest()); binding.put("targetEnvironment", "backup");
        binding.put("targetHost", "172.30.30.59"); binding.put("runtimeDir", "/opt/intruoyi/runtime");
        binding.put("leaseToken", "d".repeat(32));
        var runtime = core.putObject("runtime"); runtime.put("imageTag", workflow.releaseTag());
        runtime.put("backendImage", "intruoyi-backend:" + workflow.releaseTag());
        runtime.put("frontendImage", "intruoyi-frontend:" + workflow.releaseTag());
        runtime.put("healthStatus", "runtime".equals(fault) ? "DOWN" : "UP"); runtime.put("frontendHttp", 200);
        byte[] bytes = mapper.writeValueAsBytes(core);
        var envelope = core.deepCopy(); envelope.put("state", "AWAITING_CONFIRMATION");
        envelope.put("receiptBase64", Base64.getEncoder().encodeToString(bytes));
        envelope.put("receiptDigest", "digest".equals(fault) ? "c".repeat(64)
                : HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        return envelope;
    }

    private void invokeCompletion(Fixture f, Path cleanupPath) throws Exception {
        var completion = RuntimeControlServiceImpl.class.getDeclaredMethod("executeActionCommand", String.class,
                RuntimeControlOperationAction.class, RuntimeControlCommand.class, Path.class, Path.class,
                RuntimeControlActionReqVO.class, String.class);
        completion.setAccessible(true);
        try {
            completion.invoke(f.runtime(), f.workflow().operationId(), RuntimeControlOperationAction.PUBLISH_BACKUP,
                    new RuntimeControlCommand("backup", "ops", "unit-controlled-publish.ps1", new ArrayList<>()),
                    f.log(), cleanupPath, f.request(), "operator");
        } catch (InvocationTargetException expectedRecordedFailure) {
            assertInstanceOf(RuntimeException.class, expectedRecordedFailure.getCause());
        }
    }

    private record Fixture(RuntimeControlServiceImpl runtime, RuntimeControlOperationStore operations,
                           RuntimeControlCommandExecutor executor, ReleaseWorkflowRecord workflow,
                           RuntimeControlActionReqVO request, Path log) { }
}
