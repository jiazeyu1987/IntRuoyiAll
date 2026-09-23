package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkflowBackupAuthorizationTest {
    @TempDir Path directory;

    @Test void reviewRetryRequiresFreshAuthorizationInsteadOfSwitchingToTestBuild() {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review retry", "approved-source", "request-12345678");
        var service = new ReleaseWorkflowService(properties);
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), ReleaseWorkflowRecord.State.FAILED,
                "SOURCE_PREPARATION", false, true);
        assertThrows(ReleaseWorkflowService.RetryRequiresNewWorkflowException.class,
                () -> service.retry(workflow.workflowId(), false));
        assertEquals(1, service.list().size());
    }

    @Test void oldTestBuildCannotReuseReviewWorkflowWithSameReasonAndSource() {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review release", "approved-source", "request-12345678");
        var service = new ReleaseWorkflowService(properties);
        var backup = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        var test = service.create("operator", "review release", "approved-source");
        assertNotEquals(backup.workflowId(), test.workflowId());
        assertNull(test.backupIntent());
    }

    @Test void changingExecutionModeInvalidatesGrantedPreview() {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review release", "approved-source", "request-12345678");
        properties.getBackupOps().setExecutionMode("linux-local");
        assertThrows(IllegalStateException.class, () -> auth.authorize(preview.previewId(), "operator", "PROD"));
    }

    @Test void persistedGrantBindsActorConfirmationRequestAndApprovedSource() {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review release", "approved-source", "request-12345678");
        assertThrows(IllegalArgumentException.class, () -> auth.authorize(preview.previewId(), "operator", "prod"));
        assertThrows(IllegalStateException.class, () -> auth.authorize(preview.previewId(), "other", "PROD"));
        var grant = auth.authorize(preview.previewId(), "operator", "PROD");
        var reloaded = new ReleaseWorkflowBackupAuthorizationService(properties);
        assertEquals(grant, reloaded.requireGrant(grant.authorizationId(), preview.previewId(), "operator",
                "request-12345678", "PROD", false));
        assertThrows(IllegalStateException.class, () -> reloaded.requireGrant(grant.authorizationId(), preview.previewId(),
                "operator", "request-87654321", "PROD", false));
        properties.getReleaseWorkflow().setApprovedMaintenanceCommit("f".repeat(40));
        assertThrows(IllegalStateException.class, () -> reloaded.requireGrant(grant.authorizationId(), preview.previewId(),
                "operator", "request-12345678", "PROD", false));
    }

    @Test void concurrentAndRestartedRequestsHaveOneDurableIdentity() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review release", "approved-source", "request-12345678");
        var grant = auth.authorize(preview.previewId(), "operator", "PROD");
        var service = new ReleaseWorkflowService(properties);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var results = pool.invokeAll(java.util.Collections.nCopies(8,
                    (java.util.concurrent.Callable<ReleaseWorkflowRecord>) () -> new ReleaseWorkflowService(properties).createBackup(grant)));
            String id = results.get(0).get().workflowId();
            for (var result : results) assertEquals(id, result.get().workflowId());
            assertEquals(id, new ReleaseWorkflowService(properties).createBackup(grant).workflowId());
            assertEquals(1, new ReleaseWorkflowStore(properties).list().size());
            var altered = auth.preview("operator", "other reason", "approved-source", "request-12345678");
            assertThrows(IllegalStateException.class, () -> service.createBackup(auth.authorize(altered.previewId(), "operator", "PROD")));
        } finally { pool.shutdownNow(); }
    }

    @Test void backupIdentityCannotDeserializeAsLegacyWhenIntentIsRemoved() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var auth = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = auth.preview("operator", "review release", "approved-source", "request-12345678");
        var service = new ReleaseWorkflowService(properties);
        var workflow = service.createBackup(auth.authorize(preview.previewId(), "operator", "PROD"));
        var path = new ReleaseWorkflowStore(properties).recordPath(workflow.workflowId());
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var json = mapper.readTree(path.toFile());
        ((com.fasterxml.jackson.databind.node.ObjectNode) json.get("record")).remove("backupIntent");
        mapper.writeValue(path.toFile(), json);
        assertThrows(ReleaseWorkflowStore.CorruptStateException.class, () -> service.require(workflow.workflowId()));
    }
    @Test void targetFingerprintBindsSourceAndExecutorAndRejectsTargetDrift() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(directory);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        String first = assertDoesNotThrow(() -> ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties));
        properties.getReleaseWorkflow().setApprovedApplicationCommit("e".repeat(40));
        assertNotEquals(first, ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties));
        properties.getEnvironments().get("backup").setHost("172.30.30.57");
        assertThrows(IllegalArgumentException.class, () -> ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties));
    }
}
