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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(runtimeControlService.getReleasePackage(any())).thenReturn(Optional.empty());
    }

    @Test
    void buildButtonDispatchesOneServerOwnedAppReleaseOperation() {
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenAnswer(invocation -> {
            RuntimeControlActionReqVO request = invocation.getArgument(0);
            RuntimeControlOperationRespVO operation = operation("running");
            operation.setOperationId(request.getPreassignedOperationId());
            return operation;
        });

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
        assertEquals(first.workflowId(), request.getValue().getReleaseWorkflowId());
        assertEquals(first.operationId(), request.getValue().getPreassignedOperationId());
        assertTrue(request.getValue().getReleaseWorkflowExpectedStateVersion() >= 0);
    }

    @Test
    void buildOperationIsDurablyBoundBeforeLowLevelDispatch() {
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenAnswer(invocation -> {
            RuntimeControlActionReqVO request = invocation.getArgument(0);
            ReleaseWorkflowRecord persisted = workflowService.require(request.getReleaseWorkflowId());
            assertEquals(request.getPreassignedOperationId(), persisted.operationId());
            assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, persisted.state());
            RuntimeControlOperationRespVO operation = operation("running");
            operation.setOperationId(request.getPreassignedOperationId());
            return operation;
        });

        ReleaseWorkflowRecord started = orchestrator.startBuild("operator", "routine release", "approved-source");

        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, started.state());
        verify(runtimeControlService, times(1)).executeAction(any(), eq("operator"));
    }

    @Test
    void successfulBuildOperationAdvancesPersistedWorkflowToReady() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord started = orchestrator.startBuild("operator", "routine release", "approved-source");
        writeOperationLog(operation.getOperationId(), """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                [INFO] Release package built: release-test
        """);
        operation.setStatus("succeeded");
        operationStore.save(operation);
        when(runtimeControlService.getReleasePackage(started.releaseTag()))
                .thenReturn(Optional.of(packageFor(started.releaseTag())));

        ReleaseWorkflowRecord reconciled = orchestrator.reconcile(started.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.READY, reconciled.state());
        assertEquals(operation.getOperationId(), reconciled.operationId());
        verify(runtimeControlService, never()).getReleasePackages();
    }

    @Test
    void schedulerReconcilesSucceededOperationWithoutUserPolling() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord started = orchestrator.startBuild("operator", "routine release", "approved-source");
        writeOperationLog(operation.getOperationId(), """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                [INFO] Release package built: release-test
                """);
        operation.setStatus("succeeded");
        operationStore.save(operation);
        when(runtimeControlService.getReleasePackage(started.releaseTag()))
                .thenReturn(Optional.of(packageFor(started.releaseTag())));

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
        stubWorkflowOperations(build, publish);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "routine release", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
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
        assertEquals(workflow.workflowId(), publishRequest.getReleaseWorkflowId());
        assertEquals(publish.getOperationId(), publishRequest.getPreassignedOperationId());
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
        stubWorkflowOperations(operation);
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
        stubWorkflowOperations(operation);
        when(runtimeControlService.cancelOperation(any())).thenAnswer(invocation -> {
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
    void recoveryRequiredBuildWithObservedStageLogStaysReadable() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "recovering build", "approved-source");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                """, StandardCharsets.UTF_8);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "BUILD_DISPATCH", false, false);

        ReleaseWorkflowRecord reconciled = orchestrator.reconcile(workflow.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, reconciled.state());
        assertEquals(workflow.stateVersion(), reconciled.stateVersion());
    }

    @Test
    void cancelDuringTestDeploymentKeepsTestLeaseIsolatedForRecovery() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        stubWorkflowOperations(build, publish);
        when(runtimeControlService.cancelOperation(any())).thenAnswer(invocation -> {
            publish.setStatus("canceled");
            operationStore.save(publish);
            return true;
        });
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "cancel write stage", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        operationStore.save(publish);

        ReleaseWorkflowRecord canceled = orchestrator.cancel(workflow.workflowId());
        ReleaseWorkflowService.OptionalLease nextTestLease =
                workflowService.acquireEnvironmentLease("test", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, canceled.state());
        assertFalse(nextTestLease.acquired());
        nextTestLease.close();
    }

    @Test
    void staleWorkflowWithFreshRunningOperationLogRefreshesHeartbeatInsteadOfCancelling() throws Exception {
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
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
    void readyWorkflowDoesNotFailOnlyBecauseHumanWaitedLongerThanHeartbeatTimeout() {
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "human wait after build", "approved-source");
        writeSuccessfulBuildStages(operation.getOperationId());
        operation.setStatus("succeeded");
        operationStore.save(operation);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflowService.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(30));

        List<ReleaseWorkflowRecord> recovered = orchestrator.recoverStaleWorkflows(Instant.now());

        assertEquals(List.of(), recovered);
        assertEquals(ReleaseWorkflowRecord.State.READY, workflowService.require(workflow.workflowId()).state());
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
        stubWorkflowOperations(build, publish, mark);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "routine release", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        String publishOperationId = workflow.operationId();
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowRecord accepting = orchestrator.acceptTest(
                workflow.workflowId(), "operator", "PASS", "browser acceptance passed");
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
        assertEquals("PASS", markRequest.getTestResult());
        assertEquals("browser acceptance passed", markRequest.getTestConclusion());
        assertEquals(null, markRequest.getSelectedRecoverySetCandidateId());
    }

    @Test
    void testAcceptanceSuccessReleasesTestLeaseForNextWorkflow() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        RuntimeControlOperationRespVO mark = operation("running");
        mark.setOperationId(UUID.randomUUID().toString());
        mark.setAction("mark-release-tested");
        mark.setEnvironment("test");
        stubWorkflowOperations(build, publish, mark);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "lease release first", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());
        orchestrator.acceptTest(workflow.workflowId(), "operator", "PASS", "browser acceptance passed");
        mark.setStatus("succeeded");
        operationStore.save(mark);
        orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowService.OptionalLease nextTestLease = workflowService.acquireEnvironmentLease("test", "next-workflow");

        assertTrue(nextTestLease.acquired());
        nextTestLease.close();
    }

    @Test
    void failedTestAcceptanceFailsWorkflowWithoutWritingTestedAttestation() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        stubWorkflowOperations(build, publish);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "acceptance fail", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowRecord failed = orchestrator.acceptTest(
                workflow.workflowId(), "operator", "FAIL", "login verification failed");

        assertEquals(ReleaseWorkflowRecord.State.FAILED, failed.state());
        assertEquals("TEST_ACCEPTANCE", failed.failedStage());
        ArgumentCaptor<RuntimeControlActionReqVO> request = ArgumentCaptor.forClass(RuntimeControlActionReqVO.class);
        verify(runtimeControlService, times(2)).executeAction(request.capture(), eq("operator"));
        assertTrue(request.getAllValues().stream()
                .noneMatch(item -> "mark-release-tested".equals(item.getAction())));
    }

    @Test
    void failAcceptanceCannotOverrideRunningPassAcceptanceOrReleaseLease() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        RuntimeControlOperationRespVO mark = operation("running");
        mark.setOperationId(UUID.randomUUID().toString());
        mark.setAction("mark-release-tested");
        mark.setEnvironment("test");
        stubWorkflowOperations(build, publish, mark);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "acceptance in progress", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());
        orchestrator.acceptTest(workflow.workflowId(), "operator", "PASS", "browser acceptance passed");
        operationStore.save(mark);
        String workflowId = workflow.workflowId();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> orchestrator.acceptTest(workflowId, "auditor", "FAIL", "login still failing"));
        ReleaseWorkflowRecord current = workflowService.require(workflowId);
        ReleaseWorkflowService.OptionalLease nextTestLease =
                workflowService.acquireEnvironmentLease("test", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.TEST_DEPLOYED, current.state());
        assertEquals(mark.getOperationId(), current.operationId());
        assertFalse(nextTestLease.acquired());
        nextTestLease.close();
    }

    @Test
    void failedTestAcceptancePersistsStructuredAuditEvent() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        stubWorkflowOperations(build, publish);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "acceptance audit", "approved-source");
        writeSuccessfulBuildStages(build.getOperationId());
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        workflow = orchestrator.startTestPublish(workflow.workflowId(), "operator", "publish test");
        publish.setStatus("succeeded");
        operationStore.save(publish);
        workflow = orchestrator.reconcile(workflow.workflowId());

        ReleaseWorkflowRecord failed = orchestrator.acceptTest(
                workflow.workflowId(), "qa.lead", "FAIL", "login verification failed");
        List<ReleaseWorkflowEvent> events = workflowService.readJournal(workflow.workflowId());
        ReleaseWorkflowEvent event = events.get(events.size() - 1);

        assertEquals(ReleaseWorkflowRecord.State.FAILED, failed.state());
        assertEquals("qa.lead", event.actor());
        assertEquals("FAIL", event.details().get("testResult"));
        assertEquals("login verification failed", event.details().get("conclusion"));
        assertEquals("qa.lead", event.details().get("acceptedBy"));
        assertEquals(workflow.workflowId(), event.details().get("workflowId"));
        assertEquals(workflow.releaseTag(), event.details().get("releaseTag"));
        assertEquals(workflow.packageDigest(), event.details().get("packageDigest"));
        assertEquals(workflow.manifestDigest(), event.details().get("manifestDigest"));
        assertNotNull(event.details().get("acceptedAt"));
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
        stubWorkflowOperations(promote);

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

    @Test
    void consumedProductionAuthorizationDispatchFailureKeepsWorkflowIsolated() {
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        ReleaseWorkflowRecord workflow = testedWorkflow();
        ReleaseAuthorizationGrant grant = orchestrator.authorizeProduction(workflow.workflowId(), "approver");
        when(runtimeControlService.executeAction(any(), eq("operator")))
                .thenThrow(new IllegalStateException("operation binding store unavailable"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> orchestrator.startProductionPromotion(workflow.workflowId(), "operator",
                        "approved production release", grant.grantId(), "PROD"));
        ReleaseWorkflowRecord isolated = workflowService.require(workflow.workflowId());
        ReleaseWorkflowService.OptionalLease prodLease =
                workflowService.acquireEnvironmentLease("prod", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, isolated.state());
        assertNotNull(new ReleaseWorkflowAuthorizationService(properties).require(grant.grantId()).consumedAt());
        assertFalse(prodLease.acquired());
        prodLease.close();
    }

    @Test
    void stalePreflightFailureReleasesBuildLeaseForNextWorkflow() {
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        when(runtimeControlService.cancelOperation(any())).thenAnswer(invocation -> {
            operation.setStatus("canceled");
            operationStore.save(operation);
            return true;
        });
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "stale preflight", "approved-source");
        operationStore.save(operation);
        workflowService.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(10));

        ReleaseWorkflowRecord recovered = orchestrator.recoverStaleWorkflows(Instant.now()).get(0);
        ReleaseWorkflowService.OptionalLease nextBuildLease =
                workflowService.acquireEnvironmentLease("build", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.FAILED, recovered.state());
        assertTrue(nextBuildLease.acquired());
        nextBuildLease.close();
    }

    @Test
    void postDispatchBindingMismatchKeepsTestWorkflowIsolated() {
        RuntimeControlOperationRespVO build = operation("running");
        RuntimeControlOperationRespVO publish = operation("running");
        publish.setOperationId(UUID.randomUUID().toString());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        AtomicInteger index = new AtomicInteger();
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenAnswer(invocation -> {
            RuntimeControlActionReqVO request = invocation.getArgument(0);
            if (index.getAndIncrement() == 0) {
                build.setOperationId(request.getPreassignedOperationId());
                build.setAction(request.getAction());
                build.setEnvironment(operationEnvironment(request.getAction()));
                return build;
            }
            return publish;
        });
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "dispatch mismatch", "approved-source");
        writeOperationLog(build.getOperationId(), """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                [INFO] Release package built: release-test
                """);
        build.setStatus("succeeded");
        operationStore.save(build);
        when(runtimeControlService.getReleasePackage(workflow.releaseTag()))
                .thenReturn(Optional.of(packageFor(workflow.releaseTag())));
        workflow = orchestrator.reconcile(workflow.workflowId());
        String readyWorkflowId = workflow.workflowId();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> orchestrator.startTestPublish(readyWorkflowId, "operator", "publish test"));
        ReleaseWorkflowRecord isolated = workflowService.require(readyWorkflowId);
        ReleaseWorkflowService.OptionalLease nextTestLease =
                workflowService.acquireEnvironmentLease("test", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, isolated.state());
        assertFalse(nextTestLease.acquired());
        nextTestLease.close();
    }

    @Test
    void productionWriteDisabledRejectsBeforeStateChangeOrDispatch() {
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        ReleaseWorkflowRecord workflow = testedWorkflow();
        ReleaseAuthorizationGrant grant = orchestrator.authorizeProduction(workflow.workflowId(), "approver");

        org.junit.jupiter.api.Assertions.assertThrows(
                ReleaseWorkflowAuthorizationService.AuthorizationException.class,
                () -> orchestrator.startProductionPromotion(workflow.workflowId(), "operator",
                        "approved production release", grant.grantId(), "PROD"));
        ReleaseWorkflowService.OptionalLease prodLease =
                workflowService.acquireEnvironmentLease("prod", "next-workflow");

        assertEquals(ReleaseWorkflowRecord.State.TESTED,
                workflowService.require(workflow.workflowId()).state());
        assertNull(new ReleaseWorkflowAuthorizationService(properties).require(grant.grantId()).consumedAt());
        assertTrue(prodLease.acquired());
        prodLease.close();
        verify(runtimeControlService, never()).executeAction(any(), any());
    }

    @Test
    void buildLogStageMarkersAdvanceRunningWorkflowBeforeOperationCompletes() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "stage markers", "approved-source");
        operationStore.save(operation);
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, "RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests\n",
                StandardCharsets.UTF_8);

        ReleaseWorkflowRecord testing = orchestrator.reconcile(workflow.workflowId());
        Files.writeString(logPath, "RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        ReleaseWorkflowRecord building = orchestrator.reconcile(workflow.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.TESTING, testing.state());
        assertEquals(ReleaseWorkflowRecord.State.BUILDING, building.state());
        assertTrue(building.evidenceRefs().stream().anyMatch(ref -> ref.contains("RELEASE_WORKFLOW_STAGE=BUILDING")));
    }

    @Test
    void failedBuildWithInvalidUtf8LogBecomesExplicitFailureAndReleasesLease() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "invalid log", "approved-source");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.write(logPath, new byte[]{(byte) 0xd6, (byte) 0xd0});
        operation.setStatus("failed");
        operationStore.save(operation);

        ReleaseWorkflowRecord failed = orchestrator.reconcile(workflow.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.FAILED, failed.state());
        assertEquals("LOG_DECODING", failed.failedStage());
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("build", "next-build");
        assertTrue(lease.acquired());
        lease.close();
        assertEquals(failed, orchestrator.reconcile(workflow.workflowId()));
    }

    @Test
    void invalidUtf8CannotAdvanceRunningOrSuccessfulBuild() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "invalid active log", "approved-source");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.write(logPath, new byte[]{(byte) 0xd6, (byte) 0xd0});
        for (String status : List.of("running", "succeeded")) {
            operation.setStatus(status);
            operationStore.save(operation);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> orchestrator.reconcile(workflow.workflowId()));
            assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING,
                    workflowService.require(workflow.workflowId()).state());
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("build", "other-build");
        assertFalse(lease.acquired());
        lease.close();
    }

    @Test
    void failedBuildUsesLastObservedBuildStage() throws Exception {
        RuntimeControlOperationRespVO operation = operation("running");
        stubWorkflowOperations(operation);
        ReleaseWorkflowRecord workflow = orchestrator.startBuild("operator", "failed build stage", "approved-source");
        operationStore.save(operation);
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath, """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                """, StandardCharsets.UTF_8);
        operation.setStatus("failed");
        operationStore.save(operation);

        ReleaseWorkflowRecord failed = orchestrator.reconcile(workflow.workflowId());

        assertEquals(ReleaseWorkflowRecord.State.FAILED, failed.state());
        assertEquals("BUILDING", failed.failedStage());
    }

    private void stubWorkflowOperations(RuntimeControlOperationRespVO... operations) {
        AtomicInteger index = new AtomicInteger();
        when(runtimeControlService.executeAction(any(), eq("operator"))).thenAnswer(invocation -> {
            RuntimeControlActionReqVO request = invocation.getArgument(0);
            int current = index.getAndIncrement();
            if (current >= operations.length) {
                throw new AssertionError("unexpected workflow operation dispatch: " + request.getAction());
            }
            RuntimeControlOperationRespVO operation = operations[current];
            operation.setOperationId(request.getPreassignedOperationId());
            operation.setAction(request.getAction());
            operation.setEnvironment(operationEnvironment(request.getAction()));
            return operation;
        });
    }

    private String operationEnvironment(String action) {
        return switch (action) {
            case "build-release" -> "release";
            case "publish-test", "mark-release-tested" -> "test";
            case "promote-prod" -> "prod";
            default -> throw new AssertionError("unexpected release workflow action: " + action);
        };
    }


    private void writeOperationLog(String operationId, String content) {
        try {
            Path logPath = operationStore.getOperationLogPath(operationId);
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, content, StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private void writeSuccessfulBuildStages(String operationId) {
        writeOperationLog(operationId, """
                RELEASE_WORKFLOW_STAGE=TESTING evidence=standard-release-contract-tests
                RELEASE_WORKFLOW_STAGE=BUILDING evidence=application-artifact-build
                [INFO] Release package built: release-test
                """);
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
