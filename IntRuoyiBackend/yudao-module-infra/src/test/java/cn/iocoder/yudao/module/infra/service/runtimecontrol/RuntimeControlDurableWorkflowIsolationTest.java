package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.*;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlDurableWorkflowIsolationTest {
    @TempDir Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"failed", "succeeded"})
    void freshRuntimeBlocksRecoveryRequiredEvenWithoutUnknownOperation(String operationStatus) {
        var fixture = fixture(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, operationStatus);
        var failure = assertThrows(RuntimeException.class,
                () -> fixture.runtime().restart(restart("backup"), "operator"));
        assertTrue(failure.getMessage().contains("RECOVERY_REQUIRED"), failure.getMessage());
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void ordinaryRestoreCannotBypassRecoveryBarrierBySupplyingItsWorkflowId() {
        var fixture = fixture(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "failed");
        var request = new RuntimeControlActionReqVO();
        request.setAction("restore-data");
        request.setReason("unit isolation check");
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setSelectedRecoverySetCandidateId("candidate-12345678");
        request.setReleaseWorkflowId(fixture.workflowId());
        var failure = assertThrows(RuntimeException.class,
                () -> fixture.runtime().executeAction(request, "operator"));
        assertTrue(failure.getMessage().contains("RECOVERY_REQUIRED"), failure.getMessage());
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void persistedBackupRecoveryDoesNotBlockLegacyTestTarget() {
        var fixture = fixture(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "failed");
        assertDoesNotThrow(() -> fixture.runtime().restart(restart("test"), "operator"));
        verify(fixture.executor()).restart(argThat(command -> "test".equals(command.getEnvironment())));
    }

    @Test
    void unacceptedDeploymentBlocksRestartAfterItsLowLevelOperationSucceeded() {
        var fixture = fixture(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, "succeeded");
        var failure = assertThrows(RuntimeException.class,
                () -> fixture.runtime().restart(restart("backup"), "operator"));
        assertTrue(failure.getMessage().contains("BACKUP_DEPLOYING"), failure.getMessage());
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void ordinaryRestoreCannotBorrowActivePublisherIdentity() {
        var fixture = fixture(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, "succeeded");
        var request = new RuntimeControlActionReqVO();
        request.setAction("restore-data");
        request.setReason("unit active isolation check");
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setSelectedRecoverySetCandidateId("candidate-12345678");
        request.setReleaseWorkflowId(fixture.workflowId());
        var failure = assertThrows(RuntimeException.class,
                () -> fixture.runtime().executeAction(request, "operator"));
        assertTrue(failure.getMessage().contains("BACKUP_DEPLOYING"), failure.getMessage());
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void validatedPublisherPassesItsOwnBarrierAndReachesThePackageGate() {
        var fixture = fixture(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, null);
        var packageGateReached = new IllegalStateException("unit package gate reached; no NAS call performed");
        doThrow(packageGateReached).when(fixture.nasSettings()).getRequiredNasConfig();
        assertSame(packageGateReached, assertThrows(RuntimeException.class,
                () -> fixture.runtime().executeAction(publishRequest(fixture.workflow()), "operator")));
        verify(fixture.nasSettings()).getRequiredNasConfig();
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void anotherValidatedPublisherStillCannotPassTheActiveWorkflowBarrier() {
        fixture(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, null, "request-first-12345678");
        var second = fixture(ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, null, "request-second-12345678");
        var failure = assertThrows(RuntimeException.class,
                () -> second.runtime().executeAction(publishRequest(second.workflow()), "operator"));
        assertTrue(failure.getMessage().contains("BACKUP_DEPLOYING"), failure.getMessage());
        verifyNoInteractions(second.executor(), second.nasSettings());
    }

    private Fixture fixture(ReleaseWorkflowRecord.State state, String operationStatus) {
        return fixture(state, operationStatus, "request-12345678");
    }

    private Fixture fixture(ReleaseWorkflowRecord.State state, String operationStatus, String idempotencyKey) {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var authorization = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = authorization.preview("operator", "unit durable isolation", "approved-source", idempotencyKey);
        var grant = authorization.authorize(preview.previewId(), "operator", "PROD");
        var store = new ReleaseWorkflowStore(properties);
        var workflow = new ReleaseWorkflowService(properties, store).createBackup(grant);
        workflow = store.bindArtifacts(workflow, "a".repeat(64), "b".repeat(64), "verifier:unit");
        workflow = store.assignOperation(workflow, workflow.stateVersion(), "op-" + java.util.UUID.randomUUID(), "verifier:unit");
        workflow = store.update(workflow, workflow.stateVersion(), state, "verifier:unit",
                "UNVERIFIED_WRITE", "deploy", false, List.of(), false);
        var operations = new RuntimeControlOperationStore(properties);
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(workflow.operationId());
        operation.setEnvironment("backup");
        operation.setStatus(operationStatus);
        if (operationStatus != null) {
            operations.save(operation);
        }
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nasSettings = mock(NasSettingsService.class);
        // A new runtime instance has no process/OS lease from the original executor.
        var runtime = new RuntimeControlServiceImpl(properties, executor, new RuntimeControlOperationStore(properties),
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), nasSettings, mock(NasBrowserService.class));
        return new Fixture(runtime, executor, workflow, nasSettings);
    }

    private RuntimeControlActionReqVO publishRequest(ReleaseWorkflowRecord workflow) {
        var request = new RuntimeControlActionReqVO();
        request.setAction("publish-backup");
        request.setReason(workflow.reason());
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setReleaseWorkflowId(workflow.workflowId());
        request.setReleaseWorkflowExpectedStateVersion(workflow.stateVersion());
        request.setPreassignedOperationId(workflow.operationId());
        request.setReleaseTag(workflow.releaseTag());
        request.setSourceSelectionId(workflow.sourceSelectionId());
        request.setReleaseAuthorizationId(workflow.backupIntent().authorizationId());
        request.setExpectedMaintenanceCommit(workflow.maintenanceCommit());
        request.setExpectedApplicationCommit(workflow.applicationCommit());
        request.setExpectedFrontendCommit(workflow.frontendCommit());
        request.setExpectedPackageDigest(workflow.packageDigest());
        request.setExpectedManifestDigest(workflow.manifestDigest());
        return request;
    }

    private RuntimeControlRestartReqVO restart(String target) {
        var request = new RuntimeControlRestartReqVO();
        request.setEnvironment(target);
        request.setComponent("intruoyi-backend");
        request.setReason("unit isolation check");
        request.setProdConfirmText("PROD");
        return request;
    }

    private record Fixture(RuntimeControlServiceImpl runtime, RuntimeControlCommandExecutor executor,
                           ReleaseWorkflowRecord workflow, NasSettingsService nasSettings) {
        String workflowId() { return workflow.workflowId(); }
    }
}
