package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowAuthorizationTest {

    private static final String PACKAGE_DIGEST = "a".repeat(64);
    private static final String MANIFEST_DIGEST = "b".repeat(64);

    @TempDir
    Path tempDir;

    @Test
    void previewRejectsCrossWorkflowDigestAndEnvironmentWithoutWrite() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowAuthorizationService service = new ReleaseWorkflowAuthorizationService(properties);
        ReleaseAuthorizationGrant grant = service.issue("rw-authorization-1", "release-authorization-1",
                PACKAGE_DIGEST, MANIFEST_DIGEST, "preset-app-release", "1", "operator");

        ReleaseWorkflowAuthorizationService.Validation validation = service.preview(grant.grantId(),
                new ReleaseWorkflowAuthorizationService.WorkflowTuple("rw-other", "release-authorization-1",
                        "c".repeat(64), MANIFEST_DIGEST, "prod", "preset-app-release", "1", "app-release",
                        ReleaseWorkflowRecord.State.TESTED), Instant.now());

        assertEquals("AUTHORIZATION_BINDING_MISMATCH", validation.errorCode());
        assertEquals(null, service.require(grant.grantId()).consumedAt());
    }

    @Test
    void executeRequiresProdAndConsumesGrantExactlyOnce() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowAuthorizationService service = new ReleaseWorkflowAuthorizationService(properties);
        ReleaseAuthorizationGrant grant = service.issue("rw-authorization-2", "release-authorization-2",
                PACKAGE_DIGEST, MANIFEST_DIGEST, "preset-app-release", "1", "operator");
        ReleaseWorkflowAuthorizationService.WorkflowTuple tuple = new ReleaseWorkflowAuthorizationService.WorkflowTuple(
                grant.workflowId(), grant.releaseTag(), grant.packageDigest(), grant.manifestDigest(), "prod",
                grant.presetId(), grant.presetVersion(), grant.approvedScope(), ReleaseWorkflowRecord.State.TESTED);

        ReleaseAuthorizationGrant consumed = service.execute(grant.grantId(), tuple, "PROD", Instant.now());
        assertEquals(true, consumed.consumedAt() != null);
        assertThrows(ReleaseWorkflowAuthorizationService.AuthorizationException.class,
                () -> service.execute(grant.grantId(), tuple, "PROD", Instant.now()));
    }

    @Test
    void expiredRevokedOrDisabledGrantNeverStartsProductionWrite() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowAuthorizationService service = new ReleaseWorkflowAuthorizationService(properties);
        ReleaseAuthorizationGrant grant = service.issue("rw-authorization-3", "release-authorization-3",
                PACKAGE_DIGEST, MANIFEST_DIGEST, "preset-app-release", "1", "operator",
                Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));

        assertThrows(ReleaseWorkflowAuthorizationService.AuthorizationException.class,
                () -> service.execute(grant.grantId(), new ReleaseWorkflowAuthorizationService.WorkflowTuple(
                        grant.workflowId(), grant.releaseTag(), grant.packageDigest(), grant.manifestDigest(), "prod",
                        grant.presetId(), grant.presetVersion(), grant.approvedScope(), ReleaseWorkflowRecord.State.TESTED),
                        "PROD", Instant.now()));
        assertEquals(null, service.require(grant.grantId()).consumedAt());
    }

    @Test
    void concurrentProcessesCanConsumeGrantOnlyOnce() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowAuthorizationService issuer = new ReleaseWorkflowAuthorizationService(properties);
        ReleaseAuthorizationGrant grant = issuer.issue("rw-authorization-4", "release-authorization-4",
                PACKAGE_DIGEST, MANIFEST_DIGEST, "preset-app-release", "1", "operator");
        ReleaseWorkflowAuthorizationService.WorkflowTuple tuple = new ReleaseWorkflowAuthorizationService.WorkflowTuple(
                grant.workflowId(), grant.releaseTag(), grant.packageDigest(), grant.manifestDigest(), "prod",
                grant.presetId(), grant.presetVersion(), grant.approvedScope(), ReleaseWorkflowRecord.State.TESTED);
        ReleaseWorkflowAuthorizationService first = new ReleaseWorkflowAuthorizationService(properties);
        ReleaseWorkflowAuthorizationService second = new ReleaseWorkflowAuthorizationService(properties);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(() -> consume(first, grant.grantId(), tuple, start));
            Future<Boolean> secondResult = executor.submit(() -> consume(second, grant.grantId(), tuple, start));
            start.countDown();
            int successCount = (firstResult.get() ? 1 : 0) + (secondResult.get() ? 1 : 0);
            assertEquals(1, successCount);
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean consume(ReleaseWorkflowAuthorizationService service, String grantId,
                                   ReleaseWorkflowAuthorizationService.WorkflowTuple tuple,
                                   CountDownLatch start) throws Exception {
        start.await();
        try {
            service.execute(grantId, tuple, "PROD", Instant.now());
            return true;
        } catch (ReleaseWorkflowAuthorizationService.AuthorizationException ex) {
            return false;
        }
    }
}
