package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlBackupConfirmationApiTest {
    @TempDir Path tempDir;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void onlyExplicitMissingReceiptProducesEmptyOptional() throws Exception {
        var f = fixture();
        when(f.executor().executeForOutput(any(), any())).thenReturn("REVIEW_PUBLISH_RECEIPT_MISSING=1\n", "");
        assertTrue(f.runtime().inspectBackupReceipt(f.workflow()).isEmpty());
        assertThrows(RuntimeException.class, () -> f.runtime().inspectBackupReceipt(f.workflow()));
        assertTrue(f.operations().findBackupReceipt(f.workflow().operationId()).isEmpty());
    }

    @Test
    void inspectRecoversReceiptFromRemoteByOperationWithoutNeedingArchivedStdoutOrToken() throws Exception {
        var f = fixture();
        f.operations().updateStatus(f.workflow().operationId(), "failed", "unit lost response before local archive");
        when(f.executor().executeForOutput(any(), any())).thenAnswer(call -> {
            var command = call.getArgument(0, RuntimeControlCommand.class);
            assertEquals("backup", command.getEnvironment());
            assertEquals("172.30.30.59", value(command, "-ServerHost"));
            assertEquals(f.workflow().operationId(), value(command, "-JavaOperationId"));
            assertFalse(command.getArguments().contains("-LeaseToken"));
            assertEquals(f.frozenRoot().toString(), command.getWorkingDirectory());
            return output(f.receipt());
        });
        assertEquals(f.receipt(), f.runtime().inspectBackupReceipt(f.workflow()).orElseThrow());
        assertEquals(f.receipt(), f.operations().findBackupReceipt(f.workflow().operationId()).orElseThrow());
        var operation = f.operations().findById(f.workflow().operationId());
        assertEquals("failed", operation.getStatus());
        assertEquals("AWAITING_CONFIRMATION", operation.getBackupConfirmationState());
        assertFalse(json.writeValueAsString(operation).contains(f.receipt().binding().leaseToken()));
    }

    @Test
    void ackRequiresFinalizingAndAnExactlyBoundDurableAcceptedDecision() throws Exception {
        var f = fixture();
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(f.workflow(), f.receipt(), "e".repeat(64)));
        var finalizing = finalizing(f);
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(), "e".repeat(64)));
        String digest = decision(f, finalizing);
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(), "f".repeat(64)));
        var data = json.readTree(decisionPath(finalizing).toFile());
        ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("operationId", "op-different-12345678");
        byte[] bytes = json.writeValueAsBytes(data); durableWrite(decisionPath(finalizing), bytes);
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(),
                RuntimeControlBackupPublicationReceipt.sha256(bytes)));
        assertNotNull(digest);
        verifyNoInteractions(f.executor());
    }

    @Test
    void ackAndLocalConfirmationAreBothRequiredAndLateWorkerCannotRegressSuccess() throws Exception {
        var f = fixture(); var finalizing = finalizing(f);
        String decisionDigest = decision(f, finalizing);
        var confirmed = new RuntimeControlBackupPublicationReceipt(1, "CONFIRMED", f.receipt().binding(),
                f.receipt().runtime(), f.receipt().receiptDigest(), f.receipt().receiptBase64(), decisionDigest);
        when(f.executor().executeForOutput(any(), any())).thenAnswer(call -> {
            var command = call.getArgument(0, RuntimeControlCommand.class);
            assertEquals("ack", value(command, "-Mode"));
            assertEquals("172.30.30.59", value(command, "-ServerHost"));
            assertEquals(f.receipt().binding().leaseToken(), value(command, "-LeaseToken"));
            assertEquals(decisionDigest, value(command, "-ConfirmationDecisionDigest"));
            assertEquals("PROD", value(command, "-ConfirmText"));
            return output(confirmed);
        });
        assertEquals(confirmed, f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(), decisionDigest));
        assertNotEquals("succeeded", f.operations().findById(finalizing.operationId()).getStatus());
        assertThrows(RuntimeException.class, () -> f.runtime().completeBackupConfirmation(finalizing, confirmed));
        durableWrite(confirmationPath(finalizing), json.writeValueAsBytes(confirmed));
        f.runtime().completeBackupConfirmation(finalizing, confirmed);
        var restartedStore = new RuntimeControlOperationStore(f.properties());
        restartedStore.archiveBackupReceipt(finalizing.operationId(), f.receipt(), "late executor tail failure");
        restartedStore.recordLocalCleanupError(finalizing.operationId(), "late local cleanup failure");
        assertEquals("succeeded", restartedStore.findById(finalizing.operationId()).getStatus());
        assertEquals("CONFIRMED", restartedStore.findById(finalizing.operationId()).getBackupConfirmationState());
        assertEquals(confirmed, restartedStore.findBackupReceipt(finalizing.operationId()).orElseThrow());
        assertEquals("late local cleanup failure", restartedStore.findById(finalizing.operationId()).getLocalCleanupError());
        assertThrows(RuntimeException.class, () -> restartedStore.updateResult(finalizing.operationId(), "failed", "stale", false));
        // An observed CONFIRMED receipt does not skip the idempotent remote owner-release ACK.
        f.runtime().acknowledgeBackupReceipt(finalizing, confirmed, decisionDigest);
        verify(f.executor(), times(2)).executeForOutput(any(), eq(Duration.ofMinutes(2)));
    }

    @Test
    void changedTargetOrMissingFrozenExecutorCannotCauseAlternateExecution() throws Exception {
        var f = fixture();
        f.properties().getEnvironments().get("backup").setRemoteDataDiskDevice("/wrong-device");
        assertThrows(RuntimeException.class, () -> f.runtime().inspectBackupReceipt(f.workflow()));
        f.properties().getEnvironments().get("backup").setRemoteDataDiskDevice("/dev/mapper/cl-home");
        Path script = f.frozenRoot().resolve("ops/deploy/confirm-review-publish.ps1");
        Files.delete(script);
        assertThrows(RuntimeException.class, () -> f.runtime().inspectBackupReceipt(f.workflow()));
        assertFalse(Files.exists(script), "confirmation must never prepare/rebuild a missing worktree");
        verifyNoInteractions(f.executor());
    }

    @Test
    void lostOrUnconfirmedAckResponseDoesNotCreateSuccess() throws Exception {
        var f = fixture(); var finalizing = finalizing(f); String digest = decision(f, finalizing);
        when(f.executor().executeForOutput(any(), any())).thenReturn(output(f.receipt()))
                .thenThrow(new IllegalStateException("unit ACK response lost"));
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(), digest));
        assertThrows(RuntimeException.class, () -> f.runtime().acknowledgeBackupReceipt(finalizing, f.receipt(), digest));
        assertNotEquals("succeeded", f.operations().findById(finalizing.operationId()).getStatus());
        assertFalse(Files.exists(confirmationPath(finalizing)));
    }

    private Fixture fixture() throws Exception {
        Path maintenance = Files.createDirectories(tempDir.resolve("maintenance-source/ops/deploy")).getParent().getParent();
        Path application = Files.createDirectories(tempDir.resolve("application-source"));
        byte[] executor = "param([string]$Mode)\n".getBytes(StandardCharsets.UTF_8);
        Files.write(maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1"), executor);
        Files.write(maintenance.resolve("ops/deploy/confirm-review-publish.ps1"), executor);
        Files.createDirectories(application.resolve("IntRuoyiBackend")); Files.createDirectories(application.resolve("IntRuoyiFronted"));
        Files.writeString(application.resolve("IntRuoyiBackend/source.txt"), "unit backend", StandardCharsets.UTF_8);
        Files.writeString(application.resolve("IntRuoyiFronted/source.txt"), "unit frontend", StandardCharsets.UTF_8);
        String maintenanceCommit = initialize(maintenance), applicationCommit = initialize(application);
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir.resolve("state"));
        var config = properties.getReleaseWorkflow(); config.setProductionWriteEnabled(true);
        config.setMaintenanceRepoRoot(maintenance.toString()); config.setApplicationRepoRoot(application.toString());
        config.setWorktreeRoot(tempDir.resolve("worktrees").toString()); config.setApprovedMaintenanceCommit(maintenanceCommit);
        config.setApprovedApplicationCommit(applicationCommit); config.setApprovedFrontendCommit(applicationCommit);
        config.setExpectedPublishScriptSha256(RuntimeControlBackupPublicationReceipt.sha256(executor));
        var authorization = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = authorization.preview("operator", "unit ACK confirmation", "approved-source", "ack-request-12345678");
        var grant = authorization.authorize(preview.previewId(), "operator", "PROD");
        var workflows = new ReleaseWorkflowStore(properties);
        var workflow = new ReleaseWorkflowService(properties, workflows).createBackup(grant);
        var frozen = new ReleaseWorkflowWorktreeFactory(properties).prepare(workflow);
        workflow = workflows.bindArtifacts(workflow, "a".repeat(64), "b".repeat(64), "verifier:unit");
        workflow = workflows.assignOperation(workflow, workflow.stateVersion(), "op-api-12345678", "verifier:unit");
        workflow = workflows.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.BACKUP_DEPLOYING,
                "verifier:unit", null, null, false, List.of(), false);
        var operation = new RuntimeControlOperationRespVO(); operation.setOperationId(workflow.operationId());
        operation.setAction("publish-backup"); operation.setEnvironment("backup"); operation.setRequestedBy("operator");
        operation.setStatus("running"); operation.setZeroWriteEvidence(false);
        operation.setParameters(Map.of("workflowId", workflow.workflowId(), "releaseTag", workflow.releaseTag(),
                "authorizationId", grant.authorizationId(), "sourceSelectionId", workflow.sourceSelectionId(),
                "maintenanceCommit", workflow.maintenanceCommit(), "applicationCommit", workflow.applicationCommit(),
                "frontendCommit", workflow.frontendCommit(), "packageDigest", workflow.packageDigest(), "manifestDigest", workflow.manifestDigest()));
        var operations = new RuntimeControlOperationStore(properties); operations.createIfAbsent(operation);
        var runner = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, runner, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), mock(NasBrowserService.class));
        var binding = new RuntimeControlBackupPublicationReceipt.Binding(workflow.workflowId(), workflow.operationId(), workflow.releaseTag(),
                workflow.packageDigest(), workflow.manifestDigest(), "backup", "172.30.30.59", "/opt/intruoyi/runtime", "d".repeat(32));
        var facts = new RuntimeControlBackupPublicationReceipt.RuntimeEvidence(workflow.releaseTag(),
                "intruoyi-backend:" + workflow.releaseTag(), "intruoyi-frontend:" + workflow.releaseTag(), "UP", 200);
        byte[] raw = json.writeValueAsBytes(Map.of("schemaVersion", 1, "binding", binding, "runtime", facts));
        var receipt = new RuntimeControlBackupPublicationReceipt(1, "AWAITING_CONFIRMATION", binding, facts,
                RuntimeControlBackupPublicationReceipt.sha256(raw), Base64.getEncoder().encodeToString(raw), null);
        return new Fixture(properties, workflow, workflows, operations, runtime, runner, frozen.maintenanceRoot(), receipt);
    }

    private ReleaseWorkflowRecord finalizing(Fixture f) {
        return f.workflows().update(f.workflow(), f.workflow().stateVersion(), ReleaseWorkflowRecord.State.BACKUP_FINALIZING,
                "verifier:unit", null, null, false, List.of(), false);
    }

    private String decision(Fixture f, ReleaseWorkflowRecord workflow) throws Exception {
        var decision = new LinkedHashMap<String, Object>(); decision.put("schemaVersion", 1); decision.put("decision", "ACCEPTED");
        decision.put("workflowId", workflow.workflowId()); decision.put("operationId", workflow.operationId());
        decision.put("releaseTag", workflow.releaseTag()); decision.put("sourceSelectionId", workflow.sourceSelectionId());
        decision.put("maintenanceCommit", workflow.maintenanceCommit()); decision.put("applicationCommit", workflow.applicationCommit());
        decision.put("frontendCommit", workflow.frontendCommit()); decision.put("receipt", f.receipt().awaitingConfirmation());
        byte[] bytes = json.writeValueAsBytes(decision); durableWrite(decisionPath(workflow), bytes);
        return RuntimeControlBackupPublicationReceipt.sha256(bytes);
    }

    private Path decisionPath(ReleaseWorkflowRecord workflow) {
        return tempDir.resolve("state/backup-finalization").resolve(workflow.workflowId() + ".decision.json");
    }

    private Path confirmationPath(ReleaseWorkflowRecord workflow) {
        return tempDir.resolve("state/backup-finalization").resolve(workflow.workflowId() + ".confirmation.json");
    }

    private void durableWrite(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
    }

    private String output(RuntimeControlBackupPublicationReceipt receipt) throws Exception {
        return "REVIEW_PUBLISH_RECEIPT_JSON=" + json.writeValueAsString(receipt) + "\n";
    }

    private String value(RuntimeControlCommand command, String key) {
        assertTrue(command.getArguments().contains(key), key);
        return command.getArguments().get(command.getArguments().indexOf(key) + 1);
    }

    private String initialize(Path directory) throws Exception {
        git(directory, "init"); git(directory, "config", "core.autocrlf", "false");
        git(directory, "config", "user.name", "receipt-unit-test"); git(directory, "config", "user.email", "receipt@example.invalid");
        git(directory, "add", "."); git(directory, "-c", "commit.gpgsign=false", "commit", "-m", "unit approved executor");
        return git(directory, "rev-parse", "HEAD");
    }

    private String git(Path directory, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", directory.toString())); command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (var stream = process.getInputStream()) { output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim(); }
        assertTrue(process.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, process.exitValue(), output); return output;
    }

    private record Fixture(RuntimeControlProperties properties, ReleaseWorkflowRecord workflow, ReleaseWorkflowStore workflows,
                           RuntimeControlOperationStore operations, RuntimeControlServiceImpl runtime,
                           RuntimeControlCommandExecutor executor, Path frozenRoot, RuntimeControlBackupPublicationReceipt receipt) { }
}
