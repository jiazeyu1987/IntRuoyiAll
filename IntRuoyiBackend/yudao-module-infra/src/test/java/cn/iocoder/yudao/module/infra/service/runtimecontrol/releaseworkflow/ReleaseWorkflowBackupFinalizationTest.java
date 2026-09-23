package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real local persistence and crash boundaries; remote receipt/ACK is an explicit unit boundary. */
class ReleaseWorkflowBackupFinalizationTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void durableDecisionPrecedesAckAndSurvivesRestartBeforeLocalCompletion() throws Exception {
        var fixture = fixture();
        fixture.failCompletionOnce = true;
        var first = reconcile(subject(fixture), fixture.id);
        assertEquals("BACKUP_FINALIZING", first.orElseThrow().state().name());
        assertTrue(Files.isRegularFile(fixture.decision()));
        assertTrue(Files.isRegularFile(fixture.confirmation()));
        assertEquals(1, fixture.acks.get());
        var recovered = reconcile(subject(fixture), fixture.id).orElseThrow();
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, recovered.state());
        assertEquals(1, fixture.acks.get(), "Confirmed local evidence must not issue another remote ACK");
        assertEquals(2, fixture.completions.get());
    }

    @Test void lostAckResponseReplaysOnlyConfirmationWithTheSameDecision() throws Exception {
        var fixture = fixture();
        fixture.loseAckOnce = true;
        var first = reconcile(subject(fixture), fixture.id).orElseThrow();
        assertEquals("BACKUP_FINALIZING", first.state().name());
        assertTrue(fixture.remoteConfirmed);
        assertFalse(Files.exists(fixture.confirmation()));
        byte[] committed = Files.readAllBytes(fixture.decision());
        var second = reconcile(subject(fixture), fixture.id).orElseThrow();
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, second.state());
        assertEquals(2, fixture.acks.get());
        assertArrayEquals(committed, Files.readAllBytes(fixture.decision()));
        assertEquals(0, fixture.deployments.get(), "Restart may never rebuild or redeploy");
    }

    @Test void unpublishableLocalDecisionCannotAcknowledgeRemoteOwner() throws Exception {
        var fixture = fixture();
        Files.createDirectories(fixture.decision());
        assertThrows(RuntimeException.class, () -> reconcile(subject(fixture), fixture.id));
        assertEquals(0, fixture.acks.get());
        assertFalse(fixture.remoteConfirmed);
        assertNotEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, fixture.service.require(fixture.id).state());
    }

    @Test void ackForAnotherDecisionNeverCompletesTheWorkflow() throws Exception {
        var fixture = fixture();
        fixture.wrongAckDecision = true;
        var result = reconcile(subject(fixture), fixture.id).orElseThrow();
        assertEquals("BACKUP_FINALIZING", result.state().name());
        assertFalse(Files.exists(fixture.confirmation()));
        assertEquals(0, fixture.completions.get());
    }

    @Test void replacingOperationAfterDecisionDoesNotReplayOldAck() throws Exception {
        var fixture = fixture();
        fixture.loseAckOnce = true;
        reconcile(subject(fixture), fixture.id);
        var current = fixture.service.require(fixture.id);
        fixture.service.assignOperation(current.workflowId(), current.stateVersion(), UUID.randomUUID().toString());
        assertThrows(RuntimeException.class, () -> reconcile(subject(fixture), fixture.id));
        assertEquals(1, fixture.acks.get());
    }

    @Test void acceptedPublicationCannotBeCanceledAsAnOrdinaryInFlightWrite() throws Exception {
        var fixture = fixture();
        fixture.loseAckOnce = true;
        reconcile(subject(fixture), fixture.id);
        assertThrows(IllegalStateException.class, () -> fixture.service.cancel(fixture.id));
        var orchestrator = new ReleaseWorkflowOrchestrator(fixture.properties, fixture.service,
                new cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore(fixture.properties), fixture.runtime);
        assertThrows(IllegalStateException.class, () -> orchestrator.cancel(fixture.id));
        assertEquals("BACKUP_FINALIZING", fixture.service.require(fixture.id).state().name());
    }

    @Test void confirmedInspectionStillAcknowledgesBeforeCompleting() throws Exception {
        var fixture = fixture();
        var pending = (cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlBackupPublicationReceipt)
                fixture.receipt("AWAITING_CONFIRMATION", null);
        var decision = ReleaseWorkflowBackupFinalizationService.AcceptedDecision.accept(fixture.frozen, pending);
        fixture.inspectDecisionDigest = sha(mapper.writeValueAsBytes(decision));
        var result = reconcile(subject(fixture), fixture.id).orElseThrow();
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, result.state());
        assertEquals(1, fixture.acks.get(), "A remote confirmed marker may precede owner unlink; inspect alone is not ACK completion");
    }

    @Test void separateFinalizerInstancesSerializeTheSameLocalDecision() throws Exception {
        var fixture = fixture();
        fixture.ackEntered = new java.util.concurrent.CountDownLatch(1);
        fixture.releaseAck = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var first = pool.submit(() -> reconcile(subject(fixture), fixture.id));
            assertTrue(fixture.ackEntered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var second = reconcile(subject(fixture), fixture.id).orElseThrow();
            assertEquals(ReleaseWorkflowRecord.State.BACKUP_FINALIZING, second.state());
            assertEquals(1, fixture.acks.get());
            fixture.releaseAck.countDown();
            assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED,
                    first.get(5, java.util.concurrent.TimeUnit.SECONDS).orElseThrow().state());
        } finally {
            fixture.releaseAck.countDown(); pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    private Fixture fixture() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review receipt", "approved-source", "receipt-12345678");
        var service = new ReleaseWorkflowService(properties);
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        workflow = service.bindArtifacts(workflow.workflowId(), workflow.stateVersion(), "a".repeat(64), "b".repeat(64));
        for (var state : List.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.READY, ReleaseWorkflowRecord.State.BACKUP_DEPLOYING)) {
            workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), state, state.name(), true, false);
        }
        workflow = service.assignOperation(workflow.workflowId(), workflow.stateVersion(), UUID.randomUUID().toString());
        return new Fixture(properties, service, workflow);
    }

    private Object subject(Fixture fixture) throws Exception {
        Class<?> type = assertDoesNotThrow(() -> Class.forName(getClass().getPackageName()
                + ".ReleaseWorkflowBackupFinalizationService"), "Durable finalization decision service is required");
        return type.getConstructor(RuntimeControlProperties.class, ReleaseWorkflowService.class, RuntimeControlService.class)
                .newInstance(fixture.properties, new ReleaseWorkflowService(fixture.properties), fixture.runtime);
    }

    @SuppressWarnings("unchecked")
    private Optional<ReleaseWorkflowRecord> reconcile(Object subject, String id) throws Exception {
        try { return (Optional<ReleaseWorkflowRecord>) subject.getClass().getMethod("reconcile", String.class).invoke(subject, id); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            throw failure;
        }
    }

    private final class Fixture {
        final RuntimeControlProperties properties;
        final ReleaseWorkflowService service;
        final ReleaseWorkflowRecord frozen;
        final String id;
        final RuntimeControlService runtime;
        final AtomicInteger acks = new AtomicInteger(), completions = new AtomicInteger(), deployments = new AtomicInteger();
        boolean loseAckOnce, failCompletionOnce, wrongAckDecision, remoteConfirmed;
        String acceptedDigest, inspectDecisionDigest;
        java.util.concurrent.CountDownLatch ackEntered, releaseAck;
        Fixture(RuntimeControlProperties properties, ReleaseWorkflowService service, ReleaseWorkflowRecord frozen) {
            this.properties = properties; this.service = service; this.frozen = frozen; this.id = frozen.workflowId();
            runtime = mock(RuntimeControlService.class, invocation -> {
                switch (invocation.getMethod().getName()) {
                    case "inspectBackupReceipt": return Optional.of(receipt(
                            inspectDecisionDigest == null ? "AWAITING_CONFIRMATION" : "CONFIRMED", inspectDecisionDigest));
                    case "acknowledgeBackupReceipt":
                        acks.incrementAndGet();
                        String digest = invocation.getArgument(2);
                        assertTrue(Files.isRegularFile(decision()), "A readable durable decision must exist before ACK");
                        assertEquals(sha(Files.readAllBytes(decision())), digest);
                        assertEquals("BACKUP_FINALIZING", service.require(id).state().name());
                        if (ackEntered != null) {
                            ackEntered.countDown();
                            if (!releaseAck.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("TEST_ACK_WAIT_TIMEOUT");
                        }
                        if (acceptedDigest != null) assertEquals(acceptedDigest, digest);
                        acceptedDigest = digest; remoteConfirmed = true;
                        if (loseAckOnce) { loseAckOnce = false; throw new IllegalStateException("ACK_RESPONSE_LOST"); }
                        return receipt("CONFIRMED", wrongAckDecision ? "f".repeat(64) : digest);
                    case "completeBackupConfirmation":
                        completions.incrementAndGet();
                        assertTrue(Files.isRegularFile(confirmation()));
                        if (failCompletionOnce) { failCompletionOnce = false; throw new IllegalStateException("LOCAL_COMPLETION_INTERRUPTED"); }
                        return null;
                    case "executeAction", "dispatchWorkflowAction": deployments.incrementAndGet(); fail("Finalization must never deploy");
                    default: return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
                }
            });
        }
        Path decision() { return directory.resolve("backup-finalization").resolve(id + ".decision.json"); }
        Path confirmation() { return directory.resolve("backup-finalization").resolve(id + ".confirmation.json"); }
        Object receipt(String state, String decisionDigest) throws Exception {
            var binding = Map.of("workflowId", id, "operationId", frozen.operationId(), "releaseTag", frozen.releaseTag(),
                    "packageDigest", frozen.packageDigest(), "manifestDigest", frozen.manifestDigest(),
                    "targetEnvironment", "backup", "targetHost", "172.30.30.59", "runtimeDir", "/opt/intruoyi/runtime", "leaseToken", "c".repeat(32));
            var evidence = Map.of("imageTag", frozen.releaseTag(), "backendImage", "intruoyi-backend:" + frozen.releaseTag(),
                    "frontendImage", "intruoyi-frontend:" + frozen.releaseTag(), "healthStatus", "UP", "frontendHttp", 200);
            byte[] raw = mapper.writeValueAsBytes(Map.of("schemaVersion", 1, "binding", binding, "runtime", evidence));
            var envelope = new LinkedHashMap<String, Object>();
            envelope.put("schemaVersion", 1); envelope.put("state", state); envelope.put("binding", binding);
            envelope.put("runtime", evidence); envelope.put("receiptDigest", sha(raw));
            envelope.put("receiptBase64", Base64.getEncoder().encodeToString(raw));
            envelope.put("confirmationDecisionDigest", decisionDigest);
            return mapper.readValue(mapper.writeValueAsBytes(envelope), Class.forName(
                    "cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlBackupPublicationReceipt"));
        }
    }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
