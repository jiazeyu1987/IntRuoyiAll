package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkflowBackupRecoveryServiceTest {
    @TempDir Path directory;
    RuntimeControlProperties properties;
    ReleaseWorkflowStore store;
    RuntimeControlOperationStore operations;
    ReleaseWorkflowRecord workflow;
    AtomicInteger calls = new AtomicInteger();

    private ReleaseWorkflowBackupRecoveryService fixture() throws Exception {
        properties = RuntimeControlProperties.createDefaultForTests(directory.resolve("state"));
        properties.setRepoRoot(directory.toString());
        properties.getReleaseWorkflow().setMaintenanceRepoRoot(directory.toString());
        var target = properties.getEnvironments().get("backup");
        target.setAccessEnabled(true);
        target.setRemoteDataRoot("/mnt/intruoyi-data/runtime-data");
        target.setRemoteReleaseRoot("/mnt/intruoyi-data/intruoyi-releases");
        target.setRemoteDataDiskMount("/mnt/intruoyi-data");
        target.setRemoteDataDiskDevice("/dev/mapper/cl-home");
        target.setRemoteMinioContainer("intruoyi-minio");
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        Path script = directory.resolve("ops/deploy/recover-runtime-resource-lease.ps1");
        Files.createDirectories(script.getParent()); Files.writeString(script, "# unit runner boundary");
        Path config = directory.resolve("backup-config.json");
        Files.writeString(config, "{\"servers\":{\"test\":{\"backupPointsRoot\":\"/controlled-backups\"}}}");
        properties.getBackupOps().setConfigPath(config.toString());
        store = new ReleaseWorkflowStore(properties);
        operations = new RuntimeControlOperationStore(properties);
        workflow = ReleaseWorkflowRecord.newWorkflow("rw-review-recovery", "release-review-test", Instant.now(), "preset", "1", "source", "a".repeat(40), "b".repeat(40), "b".repeat(40))
                .withRequestContext("actor", "release", "source")
                .withBackupIntent(new ReleaseWorkflowRecord.BackupIntent("auth", "preview", "a".repeat(64), "request-key"));
        store.create(workflow);
        workflow = store.assignOperation(workflow, workflow.stateVersion(), "op-review-recovery", "actor");
        workflow = store.update(workflow, workflow.stateVersion(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                "actor", "UNKNOWN", "BACKUP_DEPLOYING", false, List.of());
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("publish-backup");
        operation.setEnvironment("backup"); operation.setStatus("failed");
        operation.setParameters(Map.of("workflowId", workflow.workflowId(), "releaseTag", workflow.releaseTag()));
        operations.save(operation);
        Path log = operations.getOperationLogPath(workflow.operationId()); Files.createDirectories(log.getParent());
        Files.writeString(log, "RUNTIME_RESOURCE_LEASE_TOKEN=" + "c".repeat(32) + " RUNTIME_RESOURCE_HOST=172.30.30.59\nRUNTIME_RESOURCE_EXECUTOR_HOST=HOST RUNTIME_RESOURCE_EXECUTOR_PID=12345 RUNTIME_RESOURCE_EXECUTOR_IDENTITY=operator\n");
        return new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { calls.incrementAndGet(); return new ReleaseWorkflowBackupRecoveryService.CommandResult(0,
                        args.contains("recover") ? "RUNTIME_RESOURCE_RECOVERED\n" : "RECOVERY_PREVIEW_DIGEST=" + "d".repeat(64) + "\nRECOVERY_RUNTIME_VERSION=old-version\n"); },
                (id, version, actor, proof) -> store.update(store.require(id), version, ReleaseWorkflowRecord.State.FAILED,
                        actor, "VERIFIED_ZERO_WRITE_RECOVERY", "RECOVERY", false, List.of(proof), true));
    }

    @Test void confirmationAndActorAreCheckedBeforeAnyRecoveryWrite() throws Exception {
        var service = fixture(); var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        assertTrue(preview.eligible());
        assertThrows(IllegalArgumentException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", ""));
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "other", "PROD"));
        assertEquals(1, calls.get());
    }

    @Test void successfulRecoveryIsDurableAndRepeatedRequestDoesNotRedispatch() throws Exception {
        var service = fixture(); var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        var recovered = service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD");
        assertEquals(ReleaseWorkflowRecord.State.FAILED, recovered.state()); assertTrue(recovered.zeroWriteEvidence());
        assertEquals(recovered, service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertEquals(2, calls.get());
    }

    @Test void missingControlledMarkerConfigBlocksBeforeRemoteInspection() throws Exception {
        var service = fixture(); Files.writeString(Path.of(properties.getBackupOps().getConfigPath()), "{}");
        assertThrows(IllegalStateException.class, () -> service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor"));
        assertEquals(0, calls.get());
    }

    @Test void ambiguousLogIdentityCannotBeSuppliedByClientOrGuessed() throws Exception {
        var service = fixture(); Files.writeString(operations.getOperationLogPath(workflow.operationId()), "no lease evidence");
        assertThrows(IllegalStateException.class, () -> service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor"));
        assertEquals(0, calls.get());
    }

    @Test void sourceVersionOrTargetDriftBlocksRecovery() throws Exception {
        var service = fixture(); var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        Files.writeString(directory.resolve("ops/deploy/recover-runtime-resource-lease.ps1"), "# changed executor");
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertEquals(1, calls.get());
    }

    @Test void unknownDispatchRetainsIsolationAndCannotBeRepeated() throws Exception {
        fixture();
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> { calls.incrementAndGet(); return new ReleaseWorkflowBackupRecoveryService.CommandResult(
                        args.contains("recover") ? -1 : 0, args.contains("recover") ? "connection lost"
                        : "RECOVERY_PREVIEW_DIGEST=" + "d".repeat(64) + "\nRECOVERY_RUNTIME_VERSION=old-version\n"); },
                (id, version, actor, proof) -> { throw new AssertionError("Unverified recovery must not finalize"); });
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertEquals(2, calls.get());
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, store.require(workflow.workflowId()).state());
    }

    @Test void legacyRestoreMarkerRemainsAnExplicitBlocker() throws Exception {
        fixture();
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> new ReleaseWorkflowBackupRecoveryService.CommandResult(75, "RECOVERY_RESTORE_ISOLATION_ACTIVE"),
                (id, version, actor, proof) -> { throw new AssertionError("Active restore cannot unlock"); });
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        assertFalse(preview.eligible()); assertEquals(List.of("RECOVERY_RESTORE_ISOLATION_ACTIVE"), preview.blockers());
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
    }

    @Test void expiredAuthorizationCannotDispatch() throws Exception {
        var service = fixture(); var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        var expired = new ReleaseWorkflowBackupRecoveryService.Preview(preview.previewId(), preview.workflowId(), preview.expectedStateVersion(),
                preview.actor(), preview.targetFingerprint(), preview.operationFingerprint(), preview.evidenceDigest(), preview.runtimeVersion(),
                preview.eligible(), preview.blockers(), preview.nextAction(), Instant.EPOCH, preview.kind());
        new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValue(Path.of(properties.getStateDir()).resolve("backup-recovery").resolve(preview.previewId() + ".json").toFile(), expired);
        assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertEquals(1, calls.get());
    }

    @Test void durablePublicationDecisionForbidsOrdinaryRecovery() throws Exception {
        var service = fixture();
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        Path decision = Path.of(properties.getStateDir()).resolve("backup-finalization").resolve(workflow.workflowId() + ".decision.json");
        Files.createDirectories(decision.getParent()); Files.writeString(decision, "{\"state\":\"ACCEPTED\"}");
        var inspected = assertThrows(IllegalStateException.class, () -> service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor"));
        assertTrue(inspected.getMessage().contains("RECOVERY_PUBLICATION_CONFIRMATION_REQUIRED"));
        var recovered = assertThrows(IllegalStateException.class, () -> service.recover(workflow.workflowId(), workflow.stateVersion(), preview.previewId(), "actor", "PROD"));
        assertTrue(recovered.getMessage().contains("RECOVERY_PUBLICATION_CONFIRMATION_REQUIRED"));
        assertEquals(1, calls.get(), "The existing publication decision must stop recovery before SSH");
    }

    @Test void successfulReceiptDirectsOperatorToConfirmationInsteadOfDataRecovery() throws Exception {
        fixture();
        var service = new ReleaseWorkflowBackupRecoveryService(properties, store, operations,
                args -> new ReleaseWorkflowBackupRecoveryService.CommandResult(75, "RECOVERY_PUBLISH_RECEIPT_REQUIRES_ACK"),
                (id, version, actor, proof) -> { throw new AssertionError("Receipt cannot use ordinary recovery"); });
        var preview = service.inspect(workflow.workflowId(), workflow.stateVersion(), "actor");
        assertFalse(preview.eligible());
        assertTrue(preview.nextAction().contains("ACK"));
        assertFalse(preview.nextAction().contains("数据恢复"));
    }

    @Test void interruptedInspectionStopsOnlyItsOwnedLocalProcessTree() throws Exception {
        var service = fixture();
        Path marker = directory.resolve("owned-processes.txt");
        String script = "$child=Start-Process powershell.exe -WindowStyle Hidden -ArgumentList @('-NoProfile','-Command','Start-Sleep -Seconds 90') -PassThru; "
                + "[IO.File]::WriteAllText('" + marker.toString().replace("'", "''") + "', $PID.ToString()+','+$child.Id.ToString()); Start-Sleep -Seconds 90";
        var execute = ReleaseWorkflowBackupRecoveryService.class.getDeclaredMethod("execute", List.class);
        execute.setAccessible(true);
        Thread worker = new Thread(() -> {
            try { execute.invoke(service, List.of("powershell.exe", "-NoProfile", "-Command", script)); }
            catch (ReflectiveOperationException expected) { /* interruption is expected; liveness is asserted below */ }
        });
        worker.start();
        List<ProcessHandle> owned = new ArrayList<>();
        try {
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            while (!Files.exists(marker) && System.nanoTime() < deadline) Thread.sleep(50);
            assertTrue(Files.exists(marker), "Owned child did not start");
            for (String pid : Files.readString(marker).split(",")) owned.add(ProcessHandle.of(Long.parseLong(pid)).orElseThrow());
            worker.interrupt(); worker.join(10000);
            assertFalse(worker.isAlive(), "Interrupted request must terminate promptly");
            assertTrue(owned.stream().noneMatch(ProcessHandle::isAlive),
                    "Interrupted recovery left its PowerShell/child process alive");
            try (var logs = Files.list(Path.of(properties.getStateDir()).resolve("backup-recovery"))) {
                Path log = logs.filter(file -> file.getFileName().toString().startsWith("run-")).findFirst().orElseThrow();
                Files.move(log, log.resolveSibling("confirmed-closed.log"));
            }
        } finally {
            worker.interrupt();
            owned.forEach(ProcessHandle::destroyForcibly);
            worker.join(10000);
        }
    }
}
