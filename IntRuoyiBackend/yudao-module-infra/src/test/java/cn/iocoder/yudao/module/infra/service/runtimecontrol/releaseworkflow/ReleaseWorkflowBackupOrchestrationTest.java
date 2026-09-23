package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.*;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReleaseWorkflowBackupOrchestrationTest {
    @TempDir Path directory;

    @Test void remoteConfirmationDoesNotHoldTheGlobalWorkflowMonitor() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review", "approved-source", "monitor-12345678");
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        workflow = service.bindArtifacts(workflow.workflowId(), workflow.stateVersion(), "a".repeat(64), "b".repeat(64));
        for (var state : List.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.BACKUP_DEPLOYING)) {
            workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), state, state.name(), true, false);
        }
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        var legacy = service.create("operator", "legacy pending", "approved-source");
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("publish-backup");
        operation.setEnvironment("backup"); operation.setStatus("awaiting-confirmation"); operations.save(operation);
        var runtime = mock(RuntimeControlService.class);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(runtime.inspectBackupReceipt(any())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("TEST_RECEIPT_TIMEOUT");
            return Optional.of(receiptFor(invocation.getArgument(0)));
        });
        when(runtime.acknowledgeBackupReceipt(any(), any(), any())).thenAnswer(invocation -> {
            RuntimeControlBackupPublicationReceipt receipt = invocation.getArgument(1);
            return new RuntimeControlBackupPublicationReceipt(1, "CONFIRMED", receipt.binding(), receipt.runtime(),
                    receipt.receiptDigest(), receipt.receiptBase64(), invocation.getArgument(2));
        });
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        String id = workflow.workflowId();
        var pending = pool.submit(() -> orchestrator.reconcile(id));
        try {
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> orchestrator.reconcile(legacy.workflowId()),
                    "A bounded SSH wait must not block unrelated workflow reads or actions");
        } finally {
            release.countDown(); pending.get(10, java.util.concurrent.TimeUnit.SECONDS);
            waitForState(service, id, ReleaseWorkflowRecord.State.BACKUP_DEPLOYED);
            pool.shutdownNow(); assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
            orchestrator.shutdownSourcePreparation();
        }
    }

    @Test void lowLevelSuccessWithoutDurableRuntimeReceiptCannotCompletePublication() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review", "approved-source", "receipt-12345678");
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        workflow = service.bindArtifacts(workflow.workflowId(), workflow.stateVersion(), "a".repeat(64), "b".repeat(64));
        for (var state : List.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.BACKUP_DEPLOYING)) {
            workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), state, state.name(), true, true);
        }
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("publish-backup");
        operation.setEnvironment("backup"); operation.setStatus("succeeded");
        operation.setParameters(Map.of("releaseTag", workflow.releaseTag(), "workflowId", workflow.workflowId()));
        operations.save(operation);
        var runtime = mock(RuntimeControlService.class);
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        orchestrator.reconcile(workflow.workflowId());
        waitForState(service, workflow.workflowId(), ReleaseWorkflowRecord.State.RECOVERY_REQUIRED);
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, service.require(workflow.workflowId()).state(),
                "Script exit success alone cannot release the final publication decision");
        orchestrator.shutdownSourcePreparation();
    }

    @Test void cancelingOwnedSourcePreparationNeverDispatchesRemoteBuild() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var runtime = mock(RuntimeControlService.class);
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service,
                new RuntimeControlOperationStore(properties), runtime);
        var preview = orchestrator.previewBackup("operator", "review", "approved-source", "cancel-12345678");
        var grant = orchestrator.authorizeBackup(preview.previewId(), "operator", "PROD");
        var workflow = orchestrator.startBackup("operator", preview.previewId(), grant.authorizationId(), "cancel-12345678", "PROD");
        try {
            orchestrator.reconcileActiveWorkflows(Instant.now());
            var canceled = orchestrator.cancel(workflow.workflowId());
            assertEquals(ReleaseWorkflowRecord.State.CANCELED, canceled.state());
            assertTrue(canceled.zeroWriteEvidence());
        } finally {
            orchestrator.shutdownSourcePreparation();
            var executor = (java.util.concurrent.ExecutorService) org.springframework.test.util.ReflectionTestUtils
                    .getField(orchestrator, "sourceExecutor");
            assertTrue(executor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        verify(runtime, never()).dispatchWorkflowAction(any(), any());
        assertEquals(ReleaseWorkflowRecord.State.CANCELED, service.require(workflow.workflowId()).state());
    }

    @Test void verifiedZeroWriteRejectionFailsWithoutKeepingRecoveryIsolation() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review", "approved-source", "request-12345678");
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        for (var state : List.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.BACKUP_DEPLOYING)) {
            workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), state, state.name(), true, true);
        }
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("publish-backup");
        operation.setEnvironment("backup"); operation.setStatus("failed"); operation.setZeroWriteEvidence(true);
        operation.setRequestedBy(workflow.requestedBy());
        operation.setParameters(Map.of("workflowId", workflow.workflowId(), "releaseTag", workflow.releaseTag(),
                "authorizationId", workflow.backupIntent().authorizationId(), "sourceSelectionId", workflow.sourceSelectionId(),
                "maintenanceCommit", workflow.maintenanceCommit(), "applicationCommit", workflow.applicationCommit(),
                "frontendCommit", workflow.frontendCommit()));
        operations.save(operation);
        var runtime = mock(RuntimeControlService.class);
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        var failed = orchestrator.reconcile(workflow.workflowId());
        assertEquals(ReleaseWorkflowRecord.State.FAILED, failed.state());
        assertTrue(failed.zeroWriteEvidence());
        verifyNoInteractions(runtime);
    }

    @Test void silentButLiveExecutorRefreshesHeartbeatWithoutCanceling() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review", "approved-source", "request-12345678");
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, false);
        service.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(3600));
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("build-release");
        operation.setStatus("running"); operations.save(operation);
        var runtime = mock(RuntimeControlService.class);
        when(runtime.isOperationExecutorAlive(workflow.operationId())).thenReturn(true);
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        Instant now = Instant.now();
        Instant progressAt = service.require(workflow.workflowId()).updatedAt();
        assertTrue(orchestrator.recoverStaleWorkflows(now).isEmpty());
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, service.require(workflow.workflowId()).state());
        assertEquals(now, service.require(workflow.workflowId()).lastHeartbeatAt());
        assertEquals(progressAt, service.require(workflow.workflowId()).updatedAt(), "heartbeat must not invent meaningful progress");
        verify(runtime, never()).cancelOperation(any());
    }

    @Test void confirmedRequestQueuesThenBackgroundBuildsAndDeploysOnceAfterReload() throws Exception {
        var properties = fixture();
        var operations = new RuntimeControlOperationStore(properties);
        var service = new ReleaseWorkflowService(properties);
        var runtime = mock(RuntimeControlService.class);
        var requests = new java.util.concurrent.CopyOnWriteArrayList<RuntimeControlActionReqVO>();
        when(runtime.dispatchWorkflowAction(any(), eq("operator"))).thenAnswer(invocation -> {
            RuntimeControlActionReqVO request = invocation.getArgument(0);
            requests.add(request);
            var operation = new RuntimeControlOperationRespVO();
            operation.setOperationId(request.getPreassignedOperationId());
            operation.setAction(request.getAction());
            operation.setEnvironment("backup");
            operation.setStatus("running");
            operation.setParameters(Map.of("releaseTag", request.getReleaseTag()));
            operations.save(operation);
            return operation;
        });
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        var preview = orchestrator.previewBackup("operator", "review", "approved-source", "request-12345678");
        var grant = orchestrator.authorizeBackup(preview.previewId(), "operator", "PROD");
        var queued = orchestrator.startBackup("operator", preview.previewId(), grant.authorizationId(),
                "request-12345678", "PROD");
        assertEquals(ReleaseWorkflowRecord.State.SOURCE_FREEZING, queued.state());
        verify(runtime, never()).dispatchWorkflowAction(any(), any());
        assertEquals(queued.workflowId(), orchestrator.startBackup("operator", preview.previewId(),
                grant.authorizationId(), "request-12345678", "PROD").workflowId());
        assertTimeout(java.time.Duration.ofMillis(500), () -> orchestrator.reconcileActiveWorkflows(Instant.now()),
                "scheduler must queue source preparation without blocking page reads");
        assertTimeout(java.time.Duration.ofMillis(500), () -> orchestrator.reconcile(queued.workflowId()));
        long waitUntil = System.nanoTime() + java.time.Duration.ofSeconds(45).toNanos();
        while (requests.isEmpty() && System.nanoTime() < waitUntil) {
            if (service.require(queued.workflowId()).state().isTerminal()) break;
            Thread.sleep(25);
        }
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, service.require(queued.workflowId()).state(),
                service.require(queued.workflowId()).toString());
        assertEquals(1, requests.size());
        assertEquals("build-release", requests.get(0).getAction());
        assertTrue(Files.isDirectory(Path.of(requests.get(0).getFrozenBackendRoot())));
        assertEquals("backup", requests.get(0).getTargetEnvironment());
        var building = service.require(queued.workflowId());
        Path log = operations.getOperationLogPath(building.operationId());
        Files.createDirectories(log.getParent());
        Files.writeString(log, "RELEASE_WORKFLOW_STAGE=TESTING\nRELEASE_WORKFLOW_STAGE=BUILDING\n", StandardCharsets.UTF_8);
        var build = operations.findById(building.operationId());
        build.setStatus("succeeded"); operations.save(build);
        var artifact = new RuntimeControlReleasePackageRespVO();
        artifact.setPackageDigest("a".repeat(64)); artifact.setManifestDigest("b".repeat(64));
        when(runtime.getReleasePackage(queued.releaseTag())).thenReturn(Optional.of(artifact));
        // Recreate services to exercise persisted intent and operation binding across a restart.
        var reloadedService = new ReleaseWorkflowService(properties);
        var reloaded = new ReleaseWorkflowOrchestrator(properties, reloadedService, operations, runtime);
        reloaded.reconcileActiveWorkflows(Instant.now());
        assertEquals(2, requests.size());
        assertEquals("publish-backup", requests.get(1).getAction());
        assertEquals("a".repeat(64), requests.get(1).getExpectedPackageDigest());
        assertNull(requests.get(1).getTestOperationId());
        reloaded.reconcileActiveWorkflows(Instant.now());
        assertEquals(2, requests.size());
        var deploying = reloadedService.require(queued.workflowId());
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, deploying.state());
        when(runtime.inspectBackupReceipt(any())).thenAnswer(invocation -> Optional.of(receiptFor(invocation.getArgument(0))));
        when(runtime.acknowledgeBackupReceipt(any(), any(), any())).thenAnswer(invocation -> {
            RuntimeControlBackupPublicationReceipt receipt = invocation.getArgument(1);
            return new RuntimeControlBackupPublicationReceipt(1, "CONFIRMED", receipt.binding(), receipt.runtime(),
                    receipt.receiptDigest(), receipt.receiptBase64(), invocation.getArgument(2));
        });
        doAnswer(invocation -> {
            ReleaseWorkflowRecord verified = invocation.getArgument(0);
            var operation = operations.findById(verified.operationId());
            operation.setStatus("succeeded"); operations.save(operation);
            return null;
        }).when(runtime).completeBackupConfirmation(any(), any());
        var deployment = operations.findById(deploying.operationId());
        deployment.setStatus("awaiting-confirmation"); operations.save(deployment);
        reloaded.reconcileActiveWorkflows(Instant.now());
        waitForState(reloadedService, queued.workflowId(), ReleaseWorkflowRecord.State.BACKUP_DEPLOYED);
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, reloadedService.require(queued.workflowId()).state());
        assertTrue(reloadedService.readJournal(queued.workflowId()).stream()
                .anyMatch(event -> event.toState() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING));
        assertTrue(Files.isRegularFile(Path.of(properties.getStateDir()).resolve("backup-finalization")
                .resolve(queued.workflowId() + ".decision.json")));
        assertEquals(2, requests.size());
        reloaded.reconcileActiveWorkflows(Instant.now());
        long cleanupDeadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
        while (!java.util.Set.of("SUCCEEDED", "FAILED").contains(reloaded.getSourceCleanupStatus(queued.workflowId()).status())
                && System.nanoTime() < cleanupDeadline) Thread.sleep(25);
        assertEquals("SUCCEEDED", reloaded.getSourceCleanupStatus(queued.workflowId()).status(),
                reloaded.getSourceCleanupStatus(queued.workflowId()).toString());
        assertFalse(Files.exists(Path.of(requests.get(0).getFrozenBackendRoot())));
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, reloadedService.require(queued.workflowId()).state());
        orchestrator.shutdownSourcePreparation(); reloaded.shutdownSourcePreparation();
    }

    @Test void unknownDispatchedStateRequiresRecoveryWithoutRedispatch() throws Exception {
        var properties = fixture();
        var service = new ReleaseWorkflowService(properties);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review", "approved-source", "request-12345678");
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, false);
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId()); operation.setAction("build-release");
        operation.setStatus("unknown"); operations.save(operation);
        var runtime = mock(RuntimeControlService.class);
        var orchestrator = new ReleaseWorkflowOrchestrator(properties, service, operations, runtime);
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, orchestrator.reconcile(workflow.workflowId()).state());
        verifyNoInteractions(runtime);
    }

    private RuntimeControlProperties fixture() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(directory.resolve("state"));
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        Path maintenance = directory.resolve("maintenance"), application = directory.resolve("application");
        Files.createDirectories(maintenance.resolve("ops/deploy"));
        byte[] script = "Write-Output 'unit managed executor'\n".getBytes(StandardCharsets.UTF_8);
        Files.write(maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1"), script);
        Files.createDirectories(application.resolve("IntRuoyiBackend"));
        Files.createDirectories(application.resolve("IntRuoyiFronted"));
        Files.writeString(application.resolve("IntRuoyiBackend/source.txt"), "backend");
        Files.writeString(application.resolve("IntRuoyiFronted/source.txt"), "frontend");
        Files.writeString(application.resolve(".gitignore"), "node_modules/\n", StandardCharsets.UTF_8);
        Files.writeString(application.resolve("IntRuoyiFronted/package.json"),
                "{\"name\":\"review-orchestration-test\",\"version\":\"1.0.0\",\"private\":true,\"packageManager\":\"pnpm@10.25.0\"}\n", StandardCharsets.UTF_8);
        Files.writeString(application.resolve("IntRuoyiFronted/pnpm-lock.yaml"),
                "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n", StandardCharsets.UTF_8);
        var settings = properties.getReleaseWorkflow();
        settings.setMaintenanceRepoRoot(maintenance.toString()); settings.setApplicationRepoRoot(application.toString());
        settings.setApprovedMaintenanceCommit(initialize(maintenance));
        settings.setApprovedApplicationCommit(initialize(application));
        settings.setApprovedFrontendCommit(settings.getApprovedApplicationCommit());
        settings.setExpectedPublishScriptSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(script)));
        return properties;
    }
    private RuntimeControlBackupPublicationReceipt receiptFor(ReleaseWorkflowRecord workflow) throws Exception {
        var binding = new RuntimeControlBackupPublicationReceipt.Binding(workflow.workflowId(), workflow.operationId(),
                workflow.releaseTag(), workflow.packageDigest(), workflow.manifestDigest(), "backup", "172.30.30.59",
                "/opt/intruoyi/runtime", "c".repeat(32));
        var evidence = new RuntimeControlBackupPublicationReceipt.RuntimeEvidence(workflow.releaseTag(),
                "intruoyi-backend:" + workflow.releaseTag(), "intruoyi-frontend:" + workflow.releaseTag(), "UP", 200);
        byte[] original = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(
                Map.of("schemaVersion", 1, "binding", binding, "runtime", evidence));
        return new RuntimeControlBackupPublicationReceipt(1, "AWAITING_CONFIRMATION", binding, evidence,
                RuntimeControlBackupPublicationReceipt.sha256(original), Base64.getEncoder().encodeToString(original), null);
    }
    private static void waitForState(ReleaseWorkflowService service, String id, ReleaseWorkflowRecord.State state) throws Exception {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(15).toNanos();
        while (service.require(id).state() != state && System.nanoTime() < deadline) Thread.sleep(25);
        assertEquals(state, service.require(id).state(), service.require(id).toString());
    }
    private String initialize(Path root) throws Exception {
        git(root, "init"); git(root, "config", "core.autocrlf", "false");
        git(root, "config", "user.name", "review-test"); git(root, "config", "user.email", "test@example.invalid");
        git(root, "add", "."); git(root, "-c", "commit.gpgsign=false", "commit", "-m", "fixture");
        return git(root, "rev-parse", "HEAD");
    }
    private String git(Path root, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", root.toString())); command.addAll(List.of(arguments));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, process.waitFor(), output); return output;
    }
}
