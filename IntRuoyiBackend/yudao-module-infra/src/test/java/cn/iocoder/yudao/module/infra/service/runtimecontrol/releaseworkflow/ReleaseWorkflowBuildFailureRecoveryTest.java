package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.*;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReleaseWorkflowBuildFailureRecoveryTest {
    private static final String TEST_HOST_IDENTITY = "a".repeat(64);
    @TempDir Path dir;
    RuntimeControlProperties properties;
    RuntimeControlOperationStore operations;
    RuntimeControlService runtime;
    ReleaseWorkflowRecord workflow;
    RuntimeControlOperationRespVO operation;
    ReleaseWorkflowWorktreeFactory factory;
    Instant requested = Instant.parse("2026-09-23T05:45:12Z");

    private ReleaseWorkflowBuildFailureRecovery fixture(Instant boot) throws Exception {
        properties = RuntimeControlProperties.createDefaultForTests(dir.resolve("state"));
        properties.getReleaseWorkflow().setPublishScriptPath("ops/deploy/publish-int-ruoyi.ps1");
        var target = properties.getEnvironments().get("backup");
        target.setAccessEnabled(true);
        target.setRemoteDataRoot("/mnt/intruoyi-data/runtime-data"); target.setRemoteReleaseRoot("/mnt/intruoyi-data/intruoyi-releases");
        target.setRemoteDataDiskMount("/mnt/intruoyi-data"); target.setRemoteDataDiskDevice("/dev/mapper/cl-home");
        target.setRemoteMinioContainer("intruoyi-minio");
        operations = new RuntimeControlOperationStore(properties);
        runtime = mock(RuntimeControlService.class);
        factory = mock(ReleaseWorkflowWorktreeFactory.class);
        workflow = ReleaseWorkflowRecord.newWorkflow("rw-build12345678", "release-review-12345678", requested,
                "preset", "1", "source", "a".repeat(40), "b".repeat(40), "b".repeat(40))
                .withRequestContext("1", "test", "source")
                .withBackupIntent(new ReleaseWorkflowRecord.BackupIntent("grant", "preview", "c".repeat(64), "request-key"));
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var authorization = new ReleaseWorkflowBackupAuthorizationService.Preview("preview", "1", "test", "source", "request-key",
                "c".repeat(64), "a".repeat(40), "b".repeat(40), "b".repeat(40), "preset", "1", "backup", target.getHost(),
                "app-release", requested, "review", target.getRemoteAppDir(), target.getRemoteDataRoot(), target.getRemoteReleaseRoot(),
                target.getRemoteDataDiskMount(), target.getRemoteDataDiskDevice(), Map.of());
        Path grantPath = Path.of(properties.getStateDir()).resolve("backup-publish-authorizations/grant.json");
        Files.createDirectories(grantPath.getParent());
        new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValue(grantPath.toFile(), new ReleaseWorkflowBackupAuthorizationService.Grant("grant", authorization, requested));
        var store = new ReleaseWorkflowStore(properties); store.create(workflow);
        workflow = store.assignOperation(workflow, workflow.stateVersion(), "op-build", "1");
        workflow = store.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.TESTING,
                "1", "TESTING_FAILED", "TESTING", false, List.of(), false);
        workflow = store.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                "1", "TESTING_FAILED", "TESTING", false, List.of(), false);
        Path maintenance = dir.resolve("maintenance");
        Path app = dir.resolve("application");
        when(factory.requirePrepared(any())).thenReturn(new ReleaseWorkflowWorktreeFactory.FrozenWorktrees(
                maintenance, app, app.resolve("IntRuoyiBackend"), app.resolve("IntRuoyiFronted"), "d".repeat(64)));
        operation = new RuntimeControlOperationRespVO();
        operation.setOperationId("op-build"); operation.setRequestedBy("1");
        operation.setRequestedAt(LocalDateTime.ofInstant(requested, ZoneId.systemDefault()));
        operation.setEnvironment("backup"); operation.setAction("build-release"); operation.setStatus("failed");
        String command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File "
                + maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1")
                + " -Mode build-release -DeployIntent independent-backup -Environment backup -Component intruoyi"
                + " -PublishScope app-release -SkipDatabaseSync -SkipMinioSync -ConfirmText PROD"
                + " -ServerHost " + target.getHost() + " -BackupServerHost " + target.getHost() + " -ServerUser " + target.getServerUser()
                + " -RemoteAppDir " + target.getRemoteAppDir() + " -RemoteDataRoot " + target.getRemoteDataRoot()
                + " -RemoteReleaseRoot " + target.getRemoteReleaseRoot() + " -RemoteDataDiskMount " + target.getRemoteDataDiskMount()
                + " -RemoteDataDiskDevice " + target.getRemoteDataDiskDevice() + " -RemoteMinioContainer " + target.getRemoteMinioContainer()
                + " -ReleaseWorkflowId " + workflow.workflowId() + " -ReleaseTag " + workflow.releaseTag()
                + " -AuthorizationId grant -SourceSelectionId source -ExpectedMaintenanceCommit " + "a".repeat(40)
                + " -ExpectedApplicationCommit " + "b".repeat(40) + " -ExpectedFrontendCommit " + "b".repeat(40)
                + " -BackendRepoRoot " + app.resolve("IntRuoyiBackend") + " -FrontendRepoRoot " + app.resolve("IntRuoyiFronted");
        String commandDigest = ReleaseWorkflowCommandFingerprint.calculate("backup", "ops", maintenance.toString(), command);
        operation.setParameters(new HashMap<>(Map.of("workflowId", workflow.workflowId(), "releaseTag", workflow.releaseTag(),
                "publishScope", "app-release", "authorizationId", "grant", "sourceSelectionId", "source",
                "maintenanceCommit", "a".repeat(40), "applicationCommit", "b".repeat(40), "frontendCommit", "b".repeat(40),
                "executorHostIdentitySha256", TEST_HOST_IDENTITY, "releaseWorkflowCommandSha256", commandDigest)));
        operations.save(operation);
        Path log = operations.getOperationLogPath("op-build"); Files.createDirectories(log.getParent());
        Files.writeString(log, "# Runtime Control Operation\n"
                + "environment=backup\ncomponent=ops\nworkingDirectory=" + maintenance + "\n"
                + "script=" + maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1") + "\n"
                + "executorHostIdentitySha256=" + TEST_HOST_IDENTITY + "\n"
                + "releaseWorkflowCommandSha256=" + commandDigest + "\ncommand=" + command + "\n");
        return new ReleaseWorkflowBuildFailureRecovery(properties, operations, runtime, factory, () -> boot,
                () -> TEST_HOST_IDENTITY);
    }

    private String commandFingerprint(String log) {
        String environment = log.lines().filter(line -> line.startsWith("environment="))
                .findFirst().orElseThrow().substring("environment=".length());
        String component = log.lines().filter(line -> line.startsWith("component="))
                .findFirst().orElseThrow().substring("component=".length());
        String workingDirectory = log.lines().filter(line -> line.startsWith("workingDirectory="))
                .findFirst().orElseThrow().substring("workingDirectory=".length());
        String command = log.lines().filter(line -> line.startsWith("command="))
                .findFirst().orElseThrow().substring("command=".length());
        return ReleaseWorkflowCommandFingerprint.calculate(environment, component, workingDirectory, command);
    }

    private void markHistoricalFailureEvidence() throws Exception {
        operation.getParameters().remove("executorHostIdentitySha256");
        operation.getParameters().remove("releaseWorkflowCommandSha256");
        Path log = operations.getOperationLogPath("op-build");
        operation.setResultLogPath(log.toAbsolutePath().normalize().toString());
        operation.setSummary("运行控制台命令执行失败：exitCode=1, log=" + log.toAbsolutePath().normalize());
        operations.save(operation);
        String historicalLog = Files.readString(log)
                .replaceFirst("(?m)^executorHostIdentitySha256=.*\\R", "")
                .replaceFirst("(?m)^releaseWorkflowCommandSha256=.*\\R", "");
        Files.writeString(log, historicalLog + "\n[FAIL] Command failed with exit code 1\n");
    }

    @Test void absentRebootProofDoesNotTreatMemoryLivenessAsTermination() throws Exception {
        var recovery = fixture(requested.minusSeconds(1));
        var proof = recovery.inspect(workflow, "1");
        assertFalse(proof.eligible());
        assertEquals(List.of("BUILD_EXECUTOR_TERMINATION_UNPROVEN"), proof.blockers());
        assertFalse(recovery.inspect(workflow, "other").eligible());
    }
    @Test void freshBootAndExactBuildBindingsAreRequired() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        assertTrue(recovery.inspect(workflow, "1").eligible());
        when(runtime.isOperationExecutorAlive("op-build")).thenReturn(true);
        assertFalse(recovery.inspect(workflow, "1").eligible());
    }

    @Test void historicalOperationWithoutHostIdentityNeverAllowsFreshBootRecovery() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        operation.getParameters().remove("executorHostIdentitySha256");
        operations.save(operation);
        Path log = operations.getOperationLogPath("op-build");
        Files.writeString(log, Files.readString(log).replaceFirst("(?m)^executorHostIdentitySha256=.*\\R", ""));
        assertFalse(recovery.inspect(workflow, "1").eligible());
        assertTrue(recovery.inspect(workflow, "1").blockers().contains("BUILD_EXECUTOR_HOST_IDENTITY_INVALID"));
    }

    @Test void verifiableHistoricalPreBuildingFailureCanBeRetiredWithoutZeroWriteClaim() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        markHistoricalFailureEvidence();
        assertFalse(operation.getParameters().containsKey("executorHostIdentitySha256"));
        assertFalse(operation.getParameters().containsKey("releaseWorkflowCommandSha256"));
        Path log = operations.getOperationLogPath("op-build");
        assertFalse(Files.readString(log).lines().anyMatch(line -> line.startsWith("executorHostIdentitySha256=")));
        assertFalse(Files.readString(log).lines().anyMatch(line -> line.startsWith("releaseWorkflowCommandSha256=")));

        var historicalProof = new ReleaseWorkflowHistoricalBuildFailureVerifier(properties, operations, runtime, factory)
                .inspect(workflow, "1");
        assertTrue(historicalProof.eligible(), historicalProof.blockers().toString());
        var proof = recovery.inspect(workflow, "1");

        assertTrue(proof.eligible(), proof.blockers().toString());
        assertNotNull(proof.digest());
    }

    @Test void historicalFailureRetirementKeepsRecoveryIsolationAndZeroWriteFalse() throws Exception {
        var build = fixture(requested.plusSeconds(10));
        markHistoricalFailureEvidence();
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var store = new ReleaseWorkflowStore(properties);
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { throw new AssertionError("historical build retirement must never run remote recovery"); },
                (id, version, actor, proof) -> { throw new AssertionError("historical build retirement is not zero-write recovery"); },
                build, new ReleaseWorkflowService(properties, store)::retireVerifiedBackupBuildFailure);

        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "1");
        var retired = service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD");

        assertTrue(preview.eligible(), preview.blockers().toString());
        assertEquals(ReleaseWorkflowRecord.State.FAILED, retired.state());
        assertFalse(retired.zeroWriteEvidence());
        assertFalse(retired.retryable());
    }

    @Test void historicalFailureWithoutOriginalFailureEventRemainsRecoveryRequired() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        operation.getParameters().remove("executorHostIdentitySha256");
        operation.getParameters().remove("releaseWorkflowCommandSha256");
        Path log = operations.getOperationLogPath("op-build");
        operation.setResultLogPath(log.toAbsolutePath().normalize().toString());
        operation.setSummary("运行控制台命令执行失败：exitCode=1, log=" + log.toAbsolutePath().normalize());
        operations.save(operation);
        String historicalLog = Files.readString(log)
                .replaceFirst("(?m)^executorHostIdentitySha256=.*\\R", "")
                .replaceFirst("(?m)^releaseWorkflowCommandSha256=.*\\R", "");
        Files.writeString(log, historicalLog);

        var proof = recovery.inspect(workflow, "1");

        assertFalse(proof.eligible());
        assertEquals(List.of("BUILD_EXECUTOR_HOST_IDENTITY_INVALID"), proof.blockers());
    }

    @Test void historicalFailureWithPublicationReceiptCannotBeRetired() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        markHistoricalFailureEvidence();
        Path receipt = operations.backupReceiptPath("op-build");
        Files.createDirectories(receipt.getParent());
        Files.writeString(receipt, "{}");

        var proof = recovery.inspect(workflow, "1");

        assertFalse(proof.eligible());
        assertTrue(proof.blockers().contains("BUILD_RECOVERY_PUBLICATION_EVIDENCE_PRESENT"));
    }

    @Test void operationAndLogBoundToDifferentCurrentHostNeverAllowRecovery() throws Exception {
        fixture(requested.plusSeconds(10));
        var recovery = new ReleaseWorkflowBuildFailureRecovery(properties, operations, runtime, factory,
                () -> requested.plusSeconds(10), () -> "b".repeat(64));
        assertEquals(TEST_HOST_IDENTITY, operation.getParameters().get("executorHostIdentitySha256"));
        assertTrue(Files.readString(operations.getOperationLogPath("op-build"))
                .contains("executorHostIdentitySha256=" + TEST_HOST_IDENTITY));
        assertFalse(recovery.inspect(workflow, "1").eligible());
        assertTrue(recovery.inspect(workflow, "1").blockers().contains("BUILD_EXECUTOR_HOST_IDENTITY_INVALID"));
    }

    @Test void buildingHistoryWithNasPublishBoundaryCannotBeRetired() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        ReleaseWorkflowStore store = new ReleaseWorkflowStore(properties);
        var testing = store.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.TESTING,
                "1", null, null, true, List.of(), false);
        var building = store.update(testing, testing.stateVersion(), ReleaseWorkflowRecord.State.BUILDING,
                "1", null, null, true, List.of(), false);
        store.update(building, building.stateVersion(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                "1", "BUILD_FAILED", "BUILDING", false, List.of(), false);
        workflow = store.require(workflow.workflowId());
        markHistoricalFailureEvidence();
        assertFalse(recovery.inspect(workflow, "1").eligible());
        assertTrue(recovery.inspect(workflow, "1").blockers().contains("BUILD_RECOVERY_NAS_WRITE_BOUNDARY"));
    }

    @Test void unboundNasAndRuntimeOptionsCannotPassExactCommandBinding() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        Path log = operations.getOperationLogPath("op-build");
        String original = Files.readString(log);
        String persistedFingerprint = operation.getParameters().get("releaseWorkflowCommandSha256");
        String logFingerprint = original.lines().filter(line -> line.startsWith("releaseWorkflowCommandSha256="))
                .map(line -> line.substring("releaseWorkflowCommandSha256=".length())).findFirst().orElse(null);
        assertNotNull(persistedFingerprint);
        assertEquals(persistedFingerprint, logFingerprint);
        assertEquals(persistedFingerprint, commandFingerprint(original));
        String commandLine = original.lines().filter(line -> line.startsWith("command=")).findFirst().orElseThrow();
        Files.writeString(log, original.replace(commandLine, commandLine
                + " -NasReleaseRoot Other/Root -BackendRuntimeBaseImage other/runtime:tag"));
        assertFalse(recovery.inspect(workflow, "1").eligible());
        assertTrue(recovery.inspect(workflow, "1").blockers().contains("BUILD_RECOVERY_COMMAND_FINGERPRINT_INVALID"));
    }

    @Test void changedCommandExecutionContextCannotPassDispatchFingerprint() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        Path log = operations.getOperationLogPath("op-build");
        String original = Files.readString(log);
        String workingDirectoryLine = original.lines()
                .filter(line -> line.startsWith("workingDirectory=")).findFirst().orElseThrow();
        Files.writeString(log, original.replace(workingDirectoryLine, "workingDirectory=" + dir.resolve("other-maintenance")));
        assertFalse(recovery.inspect(workflow, "1").eligible());
        assertTrue(recovery.inspect(workflow, "1").blockers().contains("BUILD_RECOVERY_COMMAND_FINGERPRINT_INVALID"));
    }

    @Test void sourceDriftDeployHistoryAndDuplicateCommandsBlock() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        operation.getParameters().put("applicationCommit", "e".repeat(40)); operations.save(operation);
        assertFalse(recovery.inspect(workflow, "1").eligible());
        operation.getParameters().put("applicationCommit", "b".repeat(40)); operations.save(operation);
        var deploy = new RuntimeControlOperationRespVO(); deploy.setOperationId("old-deploy");
        deploy.setAction("publish-backup"); deploy.setParameters(Map.of("releaseTag", workflow.releaseTag())); operations.save(deploy);
        assertFalse(recovery.inspect(workflow, "1").eligible());
        Files.delete(operations.getOperationPath("old-deploy"));
        Files.writeString(operations.getOperationLogPath("op-build"), "command=other\n", StandardOpenOption.APPEND);
        assertFalse(recovery.inspect(workflow, "1").eligible());
    }
    @Test void bootReadFailureAndFrozenEvidenceLossFailClosed() throws Exception {
        fixture(requested.plusSeconds(10));
        var recovery = new ReleaseWorkflowBuildFailureRecovery(properties, operations, runtime, factory,
                () -> { throw new IllegalStateException("BOOT_QUERY_FAILED"); }, () -> TEST_HOST_IDENTITY);
        assertFalse(recovery.inspect(workflow, "1").eligible());
        when(factory.requirePrepared(any())).thenThrow(new IllegalStateException("SOURCE_PREPARED_EVIDENCE_MISSING"));
        assertFalse(recovery.inspect(workflow, "1").eligible());
    }

    @Test void buildRecoveryUsesConfirmationCasAndNeverMarksZeroWriteOrRunsRemote() throws Exception {
        var build = fixture(requested.plusSeconds(10));
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var store = new ReleaseWorkflowStore(properties);
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { throw new AssertionError("build retirement must never run remote recovery"); },
                (id, version, actor, proof) -> { throw new AssertionError("not zero-write recovery"); }, build,
                new ReleaseWorkflowService(properties, store)::retireVerifiedBackupBuildFailure);
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "1");
        assertTrue(preview.eligible()); assertEquals(ReleaseWorkflowBackupRecoveryService.RecoveryKind.BUILD_FAILURE, preview.kind());
        assertThrows(IllegalArgumentException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", ""));
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "other", "PROD"));
        var result = service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD");
        assertEquals(ReleaseWorkflowRecord.State.FAILED, result.state()); assertFalse(result.zeroWriteEvidence());
        assertFalse(result.retryable());
        assertEquals(result, service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD"));
    }

    @Test void freshEvidenceDriftAndStaleVersionRejectBeforeCompletion() throws Exception {
        var build = fixture(requested.plusSeconds(10));
        var store = new ReleaseWorkflowStore(properties);
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { throw new AssertionError("no remote"); }, (id, version, actor, proof) -> { throw new AssertionError("no remote completion"); },
                build, new ReleaseWorkflowService(properties, store)::retireVerifiedBackupBuildFailure);
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "1");
        when(runtime.isOperationExecutorAlive("op-build")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD"));
        when(runtime.isOperationExecutorAlive("op-build")).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion() + 1, preview.previewId(), "1", "PROD"));
        properties.getReleaseWorkflow().setProductionWriteEnabled(false);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD"));
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, store.require(workflow.workflowId()).state());
    }

    @Test void missingKindRequiresNewInspectionAndBuildCannotUseDeploymentKind() throws Exception {
        var build = fixture(requested.plusSeconds(10)); var store = new ReleaseWorkflowStore(properties);
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { throw new AssertionError("no remote"); }, (id, version, actor, proof) -> { throw new AssertionError("no remote completion"); },
                build, new ReleaseWorkflowService(properties, store)::retireVerifiedBackupBuildFailure);
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "1");
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        Path path = Path.of(properties.getStateDir()).resolve("backup-recovery").resolve(preview.previewId() + ".json");
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(preview);
        json.remove("kind"); mapper.writeValue(path.toFile(), json);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD"));
        json.put("kind", "DEPLOYMENT_RECOVERY"); mapper.writeValue(path.toFile(), json);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "1", "PROD"));
    }

    @Test void targetCommandDriftAndPublicationReceiptBlock() throws Exception {
        var recovery = fixture(requested.plusSeconds(10));
        Path log = operations.getOperationLogPath("op-build"); String original = Files.readString(log);
        Files.writeString(log, original.replace("-ServerHost 172.30.30.59", "-ServerHost 172.30.30.60"));
        assertFalse(recovery.inspect(workflow, "1").eligible());
        Files.writeString(log, original);
        Path receipt = operations.backupReceiptPath("op-build"); Files.createDirectories(receipt.getParent()); Files.writeString(receipt, "{}");
        assertFalse(recovery.inspect(workflow, "1").eligible());
    }

    @Test void readsActualWindowsBootAsUtcInstant() {
        Instant boot = ReleaseWorkflowBuildFailureRecovery.windowsLastBoot();
        assertTrue(boot.isAfter(Instant.parse("2020-01-01T00:00:00Z")));
        assertFalse(boot.isAfter(Instant.now()));
    }
}
