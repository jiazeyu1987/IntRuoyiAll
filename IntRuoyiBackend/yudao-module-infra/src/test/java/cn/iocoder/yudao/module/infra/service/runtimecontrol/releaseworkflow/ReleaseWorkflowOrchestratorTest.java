package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseWorkflowOrchestratorTest {

    @TempDir
    Path tempDir;

    private RuntimeControlProperties properties;
    private RuntimeControlService runtimeControlService;
    private RuntimeControlOperationStore operationStore;
    private ReleaseWorkflowService workflowService;
    private ReleaseWorkflowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        runtimeControlService = mock(RuntimeControlService.class);
        operationStore = new RuntimeControlOperationStore(properties);
        ReleaseWorkflowStore workflowStore = new ReleaseWorkflowStore(properties);
        workflowService = new ReleaseWorkflowService(properties, workflowStore);
        orchestrator = new ReleaseWorkflowOrchestrator(properties,
                workflowService, operationStore, runtimeControlService);
        when(runtimeControlService.getReleasePackages()).thenAnswer(ignored -> java.util.List.of());
    }

    @Test
    void buildButtonDispatchesOneServerOwnedAppReleaseOperation() {
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);

        ReleaseWorkflowRecord first = orchestrator.startBuild("operator", "routine release", "approved-source");
        ReleaseWorkflowRecord duplicate = orchestrator.startBuild("operator", "routine release", "approved-source");

        assertEquals(first.workflowId(), duplicate.workflowId());
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, duplicate.state());
        ArgumentCaptor<RuntimeControlActionReqVO> request = ArgumentCaptor.forClass(RuntimeControlActionReqVO.class);
        verify(runtimeControlService, times(1)).executeAction(request.capture(), eq("operator"));
        assertEquals("build-release", request.getValue().getAction());
        assertEquals(ReleaseWorkflowContract.PUBLISH_SCOPE, request.getValue().getPublishScope());
        assertEquals(first.releaseTag(), request.getValue().getReleaseTag());
        assertFalse(Boolean.TRUE.equals(request.getValue().getIncludeShowroomBuildPackage()));
        assertEquals("a".repeat(40), request.getValue().getExpectedMaintenanceCommit());
        assertEquals("b".repeat(40), request.getValue().getExpectedApplicationCommit());
        assertEquals("b".repeat(40), request.getValue().getExpectedFrontendCommit());
        assertEquals("approved-source", request.getValue().getSourceSelectionId());
    }

    @Test
    void successfulBuildOperationAdvancesPersistedWorkflowToReady() {
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);
        ReleaseWorkflowRecord started = orchestrator.startBuild("operator", "routine release", "approved-source");
        operation.setStatus("succeeded");
        operationStore.save(operation);
        when(runtimeControlService.getReleasePackages()).thenReturn(java.util.List.of(packageFor(started.releaseTag())));

        ReleaseWorkflowRecord reconciled = orchestrator.reconcile(started.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.READY, reconciled.state());
        assertEquals(operation.getOperationId(), reconciled.operationId());
    }

    @Test
    void schedulerReconcilesSucceededOperationWithoutUserPolling() {
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);
        ReleaseWorkflowRecord started = orchestrator.startBuild("operator", "routine release", "approved-source");
        operation.setStatus("succeeded");
        operationStore.save(operation);
        when(runtimeControlService.getReleasePackages()).thenReturn(List.of(packageFor(started.releaseTag())));

        List<ReleaseWorkflowRecord> reconciled = orchestrator.reconcileActiveWorkflows(Instant.now());

        assertEquals(1, reconciled.size());
        assertEquals(ReleaseWorkflowRecord.State.READY, workflowService.require(started.workflowId()).state());
    }

    @Test
    void readyWorkflowDispatchesPublishTestWithSameServerGeneratedTag() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(build, publish);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "routine release", "approved-source");
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackages()).thenReturn(java.util.List.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowRecord deploying = orchestrator.startTestPublish(
                workflow.workflowId(), "operator", "publish accepted package");

        assertEquals(ReleaseWorkflowRecord.State.TEST_DEPLOYING, deploying.state());
        ArgumentCaptor<RuntimeControlActionReqVO> request = ArgumentCaptor.forClass(RuntimeControlActionReqVO.class);
        verify(runtimeControlService, times(2)).executeAction(request.capture(), eq("operator"));
        RuntimeControlActionReqVO publishRequest = request.getAllValues().get(1);
        assertEquals("publish-test", publishRequest.getAction());
        assertEquals(workflow.releaseTag(), publishRequest.getReleaseTag());
        assertEquals(null, publishRequest.getPublishScope());
    }

    @Test
    void unapprovedSourceSelectionBlocksBeforeOperationDispatch() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> orchestrator.startBuild("operator", "routine release", "manual-path"));
        verify(runtimeControlService, times(0)).executeAction(any(), any());
    }

    @Test
    void cancelWithRunningOperationMustBlockBeforeReleasingLease() {
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "running cancel", "approved-source");
        operationStore.save(operation);

        assertEquals("running", operationStore.findById(operation.getOperationId()).getStatus());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> orchestrator.cancel(workflow.workflowId()));
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING,
                workflowService.require(workflow.workflowId()).state());
    }

    @Test
    void staleRunningOperationIsCancelledBeforeWorkflowRecovery() {
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);
        when(runtimeControlService.cancelOperation(operation.getOperationId())).thenAnswer(invocation -> {
            operation.setStatus("cancelled");
            operationStore.save(operation);
            return true;
        });
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "stale build", "approved-source");
        operationStore.save(operation);
        workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, false);
        workflow = workflowService.require(workflow.workflowId());
        workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, false);
        workflowService.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(10));

        ReleaseWorkflowRecord recovered = orchestrator.recoverStaleWorkflows(Instant.now()).get(0);

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, recovered.state());
        verify(runtimeControlService).cancelOperation(operation.getOperationId());
    }

    @Test
    void staleWorkflowWithFreshRunningOperationLogRefreshesHeartbeatInsteadOfCancelling() throws Exception {
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        RuntimeControlOperationRespVO operation = operation("running");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "long running build", "approved-source");
        operationStore.save(operation);
        workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, false);
        workflow = workflowService.require(workflow.workflowId());
        workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, false);
        workflowService.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(10));
        Instant observedProgress = Instant.now();
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, "[INFO] frontend type check still running\n");
        Files.setLastModifiedTime(logPath, FileTime.from(observedProgress));

        orchestrator.recoverStaleWorkflows(observedProgress.plusMillis(500));

        ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
        assertEquals(ReleaseWorkflowRecord.State.BUILDING, current.state());
        assertFalse(current.lastHeartbeatAt().isBefore(observedProgress));
        verify(runtimeControlService, times(0)).cancelOperation(operation.getOperationId());
    }

    @Test
    void testAcceptanceWritesNoDataAttestationAndAdvancesAfterSuccess() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        RuntimeControlOperationRespVO mark = operation("running");
        mark.setOperationId(UUID.randomUUID().toString());
        mark.setAction("mark-release-tested");
        mark.setEnvironment("test");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(build, publish, mark);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "routine release", "approved-source");
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackages()).thenReturn(java.util.List.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        String publishOperationId = workflow.operationId();
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowRecord accepting = orchestrator.acceptTest(
                workflow.workflowId(), "operator", "browser acceptance passed");
        assertEquals(ReleaseWorkflowRecord.State.TEST_DEPLOYED, accepting.state());
        mark.setStatus("succeeded");
        operationStore.save(mark);
        ReleaseWorkflowRecord tested = orchestrator.reconcile(workflow.workflowId());
        assertEquals(ReleaseWorkflowRecord.State.TESTED, tested.state());

        ArgumentCaptor<RuntimeControlActionReqVO> request = ArgumentCaptor.forClass(RuntimeControlActionReqVO.class);
        verify(runtimeControlService, times(3)).executeAction(request.capture(), eq("operator"));
        RuntimeControlActionReqVO markRequest = request.getAllValues().get(2);
        assertEquals("mark-release-tested", markRequest.getAction());
        assertEquals(publishOperationId, markRequest.getTestOperationId());
        assertEquals(operationStore.getOperationPath(publishOperationId).toString(),
                markRequest.getTestOperationEvidencePath());
        assertEquals(null, markRequest.getSelectedRecoverySetCandidateId());
    }

    @Test
    void productionPromotionConsumesBoundGrantAndDispatchesSameArtifactOnce() {
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        ReleaseWorkflowRecord workflow = testedWorkflow();
        ReleaseAuthorizationGrant grant = orchestrator.authorizeProduction(workflow.workflowId(), "approver");
        RuntimeControlOperationRespVO promote = operation("running");
        promote.setAction("promote-prod");
        promote.setEnvironment("prod");
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenReturn(promote);

        ReleaseWorkflowRecord promoting = orchestrator.startProductionPromotion(workflow.workflowId(), "operator",
                "approved production release", grant.grantId(), "PROD");

        assertEquals(ReleaseWorkflowRecord.State.PROMOTING_PROD, promoting.state());
        ArgumentCaptor<RuntimeControlActionReqVO> request = ArgumentCaptor.forClass(RuntimeControlActionReqVO.class);
        verify(runtimeControlService).executeAction(request.capture(), eq("operator"));
        assertEquals(workflow.releaseTag(), request.getValue().getReleaseTag());
        assertEquals(workflow.testOperationId(), request.getValue().getTestOperationId());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orchestrator.startProductionPromotion(workflow.workflowId(), "operator",
                        "duplicate production release", grant.grantId(), "PROD"));
        verify(runtimeControlService, times(1)).executeAction(any(), eq("operator"));
    }

    private RuntimeControlOperationRespVO operation(String status) {
        RuntimeControlOperationRespVO result = new RuntimeControlOperationRespVO();
        result.setOperationId(UUID.randomUUID().toString());
        result.setRequestedBy("operator");
        result.setRequestedAt(LocalDateTime.now());
        result.setEnvironment("release");
        result.setComponent("ops");
        result.setAction("build-release");
        result.setReason("routine release");
        result.setStatus(status);
        return result;
    }

    private RuntimeControlReleasePackageRespVO packageFor(String releaseTag) {
        RuntimeControlReleasePackageRespVO result = new RuntimeControlReleasePackageRespVO();
        result.setReleaseTag(releaseTag);
        result.setPackageDigest("a".repeat(64));
        result.setManifestDigest("b".repeat(64));
        return result;
    }

    private ReleaseWorkflowRecord testedWorkflow() {
        ReleaseWorkflowRecord workflow = workflowService.create("operator", "tested fixture", "approved-source");
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
        workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                "a".repeat(64), "b".repeat(64));
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.READY, "READY", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
        workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                "op-publish-test-success", operationStore.getOperationPath("op-publish-test-success").toString());
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYED, "TEST_DEPLOYED", true, false);
        return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTED, "TESTED", true, false);
    }
}
