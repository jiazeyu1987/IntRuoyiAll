package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowRecord;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowBackupAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlBackupWorkflowBindingTest {
    @TempDir Path tempDir;

    @Test
    void fabricatedWorkflowContextCannotAuthorizeReviewDeployment() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var service = new RuntimeControlServiceImpl(properties, executor, new RuntimeControlOperationStore(properties),
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        var request = new RuntimeControlActionReqVO();
        request.setAction("publish-backup");
        request.setReason("review publication");
        request.setProdConfirmText("PROD");
        request.setReleaseWorkflowId("rw-12345678");
        request.setReleaseWorkflowExpectedStateVersion(1L);
        request.setPreassignedOperationId("op-review-12345678");
        request.setReleaseTag("release-review-r1");
        request.setExpectedPackageDigest("a".repeat(64));
        request.setExpectedManifestDigest("b".repeat(64));

        var failure = assertThrows(RuntimeException.class, () -> service.executeAction(request, "operator"));
        assertTrue(failure.getMessage().contains("RELEASE_WORKFLOW"), failure.getMessage());
        verifyNoInteractions(executor, nas);
        assertNull(new RuntimeControlOperationStore(properties).findById("op-review-12345678"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"authorization", "operator", "operation", "version", "source", "commit", "package", "manifest", "target", "duplicate"})
    void persistedIntentRejectsDriftAndReplaysExistingOperation(String mutation) {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var operations = new RuntimeControlOperationStore(properties);
        var service = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        Instant now = Instant.now();
        new ReleaseWorkflowStore(properties).create(new ReleaseWorkflowRecord("rw-12345678", "release-review-r1",
                "app-release", "preset-app-release", "1", ReleaseWorkflowRecord.State.BACKUP_DEPLOYING,
                7, 1, "op-review-12345678", null, null, false, List.of(), "a".repeat(64), "b".repeat(64),
                null, null, now, now, now, false, "operator", "review publication", "source-review-r1",
                "a".repeat(40), "b".repeat(40), "b".repeat(40),
                new ReleaseWorkflowRecord.BackupIntent("auth-review-r1", "preview-review-r1",
                        ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties), "idem-review-r1")));
        var request = new RuntimeControlActionReqVO();
        request.setAction("publish-backup");
        request.setReason("review publication");
        request.setProdConfirmText("PROD");
        request.setReleaseWorkflowId("rw-12345678");
        request.setReleaseWorkflowExpectedStateVersion(7L);
        request.setPreassignedOperationId("op-review-12345678");
        request.setReleaseTag("release-review-r1");
        request.setExpectedPackageDigest("a".repeat(64));
        request.setExpectedManifestDigest("b".repeat(64));
        request.setExpectedMaintenanceCommit("a".repeat(40));
        request.setExpectedApplicationCommit("b".repeat(40));
        request.setExpectedFrontendCommit("b".repeat(40));
        request.setSourceSelectionId("source-review-r1");
        request.setReleaseAuthorizationId("auth-review-r1");
        if ("duplicate".equals(mutation)) {
            var existing = new cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO();
            existing.setOperationId(request.getPreassignedOperationId());
            existing.setAction("publish-backup");
            existing.setEnvironment("backup");
            existing.setRequestedBy("operator");
            existing.setReason(request.getReason());
            existing.setStatus("running");
            existing.setParameters(java.util.Map.of("releaseTag", request.getReleaseTag(),
                    "workflowId", request.getReleaseWorkflowId(), "authorizationId", request.getReleaseAuthorizationId(),
                    "sourceSelectionId", request.getSourceSelectionId(), "maintenanceCommit", request.getExpectedMaintenanceCommit(),
                    "applicationCommit", request.getExpectedApplicationCommit(), "frontendCommit", request.getExpectedFrontendCommit(),
                    "packageDigest", request.getExpectedPackageDigest(), "manifestDigest", request.getExpectedManifestDigest()));
            operations.save(existing);
            assertEquals(existing, service.executeAction(request, "operator"));
            verifyNoInteractions(executor, nas);
            return;
        }
        switch (mutation) {
            case "authorization" -> request.setReleaseAuthorizationId("forged");
            case "operation" -> request.setPreassignedOperationId("op-forged-12345678");
            case "version" -> request.setReleaseWorkflowExpectedStateVersion(6L);
            case "source" -> request.setSourceSelectionId("source-other");
            case "commit" -> request.setExpectedApplicationCommit("c".repeat(40));
            case "package" -> request.setExpectedPackageDigest("c".repeat(64));
            case "manifest" -> request.setExpectedManifestDigest("c".repeat(64));
            case "target" -> properties.getEnvironments().get("backup").setHost("172.30.30.57");
        }
        assertThrows(RuntimeException.class, () -> service.executeAction(request,
                "operator".equals(mutation) ? "another-operator" : "operator"));
        verifyNoInteractions(executor, nas);
        assertNull(operations.findById(request.getPreassignedOperationId()));
    }

    @Test
    void clientCannotDeserializeServerOnlyAuthorizationOrFrozenPaths() throws Exception {
        var request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                "{\"preassignedOperationId\":\"forged\",\"releaseAuthorizationId\":\"forged\",\"frozenMaintenanceRoot\":\"arbitrary\","
                        + "\"frozenBackendRoot\":\"arbitrary\",\"frozenFrontendRoot\":\"arbitrary\","
                        + "\"frozenPublishScriptSha256\":\"arbitrary\"}", RuntimeControlActionReqVO.class);
        assertNull(request.getReleaseAuthorizationId());
        assertNull(request.getPreassignedOperationId());
        assertNull(request.getFrozenMaintenanceRoot());
        assertNull(request.getFrozenBackendRoot());
        assertNull(request.getFrozenFrontendRoot());
        assertNull(request.getFrozenPublishScriptSha256());
    }
}
