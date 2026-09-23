package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Connects the durable workflow to the existing low-level release executor. */
@Service
public class ReleaseWorkflowOrchestrator {

    private final RuntimeControlProperties properties;
    private final ReleaseWorkflowService workflowService;
    private final RuntimeControlOperationStore operationStore;
    private final RuntimeControlService runtimeControlService;
    private final ReleaseWorkflowAuthorizationService authorizationService;
    private final ReleaseWorkflowProductionPreviewService productionPreviewService;
    private final ReleaseWorkflowTestEvidenceStore testEvidenceStore;
    private final ReleaseWorkflowSourceCleanupService sourceCleanupService;
    private final ReleaseWorkflowBackupFinalizationService backupFinalization;
    private final Map<String, ReleaseWorkflowService.OptionalLease> activeLeases = new ConcurrentHashMap<>();
    private final Map<String, String> prodIdempotencyOperations = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.FutureTask<Void>> sourcePreparations = new ConcurrentHashMap<>();
    private final Map<String, Thread> activeSourceThreads = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.FutureTask<Void>> sourceCleanups = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.FutureTask<Void>> confirmations = new ConcurrentHashMap<>();
    private final Map<String, Thread> activeConfirmationThreads = new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService confirmationExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "release-backup-confirmation"); thread.setDaemon(true); return thread;
    });
    private final java.util.concurrent.ExecutorService sourceExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "release-source-preparation"); thread.setDaemon(true); return thread;
    });

    public ReleaseWorkflowBackupAuthorizationService.Preview previewBackup(String actor, String reason,
                                                                           String sourceSelectionId, String idempotencyKey) {
        runtimeControlService.validateBackupPublishPrerequisites();
        return backupAuthorization().preview(actor, reason, sourceSelectionId, idempotencyKey);
    }

    public ReleaseWorkflowBackupAuthorizationService.Grant authorizeBackup(String previewId, String actor, String confirm) {
        return backupAuthorization().authorize(previewId, actor, confirm);
    }

    public synchronized ReleaseWorkflowRecord startBackup(String actor, String previewId, String grantId,
                                                          String idempotencyKey, String confirm) {
        boolean existing = workflowService.list().stream().anyMatch(w -> w.backupIntent() != null
                && actor.equals(w.requestedBy()) && idempotencyKey.equals(w.backupIntent().idempotencyKey())
                && grantId.equals(w.backupIntent().authorizationId()));
        var grant = backupAuthorization().requireGrant(grantId, previewId, actor, idempotencyKey, confirm, existing);
        // SOURCE_FREEZING is a durable queue entry. Only the background reconciler prepares sources and dispatches.
        return workflowService.createBackup(grant);
    }

    private ReleaseWorkflowBackupAuthorizationService backupAuthorization() {
        return new ReleaseWorkflowBackupAuthorizationService(properties);
    }

    /** Called only after the controlled recovery verifier has completed its fresh remote CAS. */
    public synchronized ReleaseWorkflowRecord completeBackupRecovery(String workflowId, long expectedVersion,
                                                                     String actor, String previewDigest) {
        ReleaseWorkflowRecord current = workflowService.require(workflowId);
        if (current.backupIntent() == null || current.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                || actor == null || actor.isBlank() || previewDigest == null || !previewDigest.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("RELEASE_WORKFLOW_VERIFIED_BACKUP_RECOVERY_REQUIRED");
        }
        ReleaseWorkflowRecord recovered = workflowService.verifyAdvance(workflowId, expectedVersion,
                ReleaseWorkflowRecord.State.FAILED, "VERIFIED_ZERO_WRITE_RECOVERY", false, true,
                List.of("recovery-preview:" + previewDigest, "recovery-actor:" + actor));
        releaseAllLeases(workflowId);
        return recovered;
    }

    private ReleaseWorkflowRecord dispatchBackupBuild(ReleaseWorkflowRecord workflow) {
        var lease = workflowService.acquireEnvironmentLease("build", workflow.workflowId());
        if (!lease.acquired()) { lease.close(); return workflowService.require(workflow.workflowId()); }
        activeLeases.put(leaseKey(workflow.workflowId(), "build"), lease);
        try {
            backupAuthorization().requireWorkflowBinding(workflow);
            String operationId = newOperationId();
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            ReleaseWorkflowRecord claimed = workflow;
            var preparation = new java.util.concurrent.FutureTask<Void>(() -> {
                activeSourceThreads.put(claimed.workflowId(), Thread.currentThread());
                prepareBackupBuild(claimed);
                return null;
            });
            sourcePreparations.put(workflow.workflowId(), preparation);
            sourceExecutor.execute(preparation);
            return claimed;
        } catch (ReleaseWorkflowStore.CasConflictException ex) {
            releaseLease(workflow.workflowId(), "build");
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            sourcePreparations.remove(workflow.workflowId());
            releaseLease(workflow.workflowId(), "build");
            failIfActive(workflow.workflowId(), "SOURCE_PREPARATION", true, ex);
            throw ex;
        }
    }

    private void prepareBackupBuild(ReleaseWorkflowRecord claimed) {
        boolean dispatchAttempted = false;
        try {
            ReleaseWorkflowRecord initial = workflowService.require(claimed.workflowId());
            if (initial.state() != ReleaseWorkflowRecord.State.SOURCE_FREEZING
                    || !claimed.operationId().equals(initial.operationId()) || Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("RELEASE_WORKFLOW_SOURCE_PREPARATION_CANCELED");
            }
            new ReleaseWorkflowSourcePreflight().verify();
            var frozen = new ReleaseWorkflowWorktreeFactory(properties).prepare(claimed);
            new ReleaseWorkflowSourceDependencies(properties).prepare(frozen);
            synchronized (this) {
                ReleaseWorkflowRecord workflow = workflowService.require(claimed.workflowId());
                if (workflow.state() != ReleaseWorkflowRecord.State.SOURCE_FREEZING
                        || !claimed.operationId().equals(workflow.operationId()) || Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_SOURCE_PREPARATION_CANCELED");
                }
                workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
            RuntimeControlActionReqVO request = backupRequest(workflow, workflow.operationId(), "build-release");
            request.setPublishScope(ReleaseWorkflowContract.PUBLISH_SCOPE);
            request.setIncludeOnlyOffice(false);
            request.setIncludeShowroomBuildPackage(false);
            request.setEnableSmartReleaseReport(false);
            request.setFrozenMaintenanceRoot(frozen.maintenanceRoot().toString());
            request.setFrozenBackendRoot(frozen.backendRoot().toString());
            request.setFrozenFrontendRoot(frozen.frontendRoot().toString());
            request.setFrozenPublishScriptSha256(frozen.publishScriptSha256());
                dispatchAttempted = true;
                requireOperationBinding(workflow.operationId(), runtimeControlService.dispatchWorkflowAction(request, workflow.requestedBy()));
            }
        } catch (RuntimeException ex) {
            synchronized (this) {
                if (dispatchAttempted) recordBackupDispatchFailure(claimed.workflowId(), "BACKUP_BUILD_DISPATCH", true, ex);
                else {
                    failIfActive(claimed.workflowId(), "SOURCE_PREPARATION", true, ex);
                    releaseLease(claimed.workflowId(), "build");
                }
            }
        } finally {
            activeSourceThreads.remove(claimed.workflowId());
            sourcePreparations.remove(claimed.workflowId());
        }
    }

    private boolean sourcePreparationAlive(String workflowId) {
        var task = sourcePreparations.get(workflowId);
        var thread = activeSourceThreads.get(workflowId);
        return task != null && !task.isDone() && thread != null && thread.isAlive();
    }

    @jakarta.annotation.PreDestroy
    public void shutdownSourcePreparation() {
        sourceExecutor.shutdownNow();
        confirmationExecutor.shutdownNow();
    }

    public ReleaseWorkflowSourceCleanupService.CleanupStatus getSourceCleanupStatus(String workflowId) {
        ReleaseWorkflowRecord workflow = workflowService.require(workflowId);
        if (workflow.backupIntent() == null) throw new IllegalArgumentException("RELEASE_WORKFLOW_NOT_BACKUP");
        return sourceCleanupService.getStatus(workflowId);
    }

    private void queueSourceCleanup(ReleaseWorkflowRecord workflow) {
        var status = sourceCleanupService.getStatus(workflow.workflowId());
        if ("SUCCEEDED".equals(status.status()) || "FAILED".equals(status.status())
                || sourceCleanups.containsKey(workflow.workflowId())) return;
        var task = new java.util.concurrent.FutureTask<Void>(() -> {
            try { sourceCleanupService.cleanup(workflow); }
            catch (RuntimeException error) {
                org.slf4j.LoggerFactory.getLogger(ReleaseWorkflowOrchestrator.class).error(
                        "Release source cleanup failed for {}: {}", workflow.workflowId(), safeCauseReference(error));
            } finally { sourceCleanups.remove(workflow.workflowId()); }
            return null;
        });
        sourceCleanups.put(workflow.workflowId(), task);
        sourceExecutor.execute(task);
    }

    private ReleaseWorkflowRecord dispatchBackupDeploy(ReleaseWorkflowRecord workflow) {
        var lease = workflowService.acquireEnvironmentLease("backup", workflow.workflowId());
        if (!lease.acquired()) { lease.close(); return workflowService.require(workflow.workflowId()); }
        activeLeases.put(leaseKey(workflow.workflowId(), "backup"), lease);
        boolean dispatchAttempted = false;
        try {
            backupAuthorization().requireWorkflowBinding(workflow);
            String operationId = newOperationId();
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.BACKUP_DEPLOYING, "BACKUP_DEPLOYING", true, false);
            RuntimeControlActionReqVO request = backupRequest(workflow, operationId, "publish-backup");
            request.setExpectedPackageDigest(workflow.packageDigest());
            request.setExpectedManifestDigest(workflow.manifestDigest());
            var frozen = new ReleaseWorkflowWorktreeFactory(properties).prepare(workflow);
            request.setFrozenMaintenanceRoot(frozen.maintenanceRoot().toString());
            request.setFrozenBackendRoot(frozen.backendRoot().toString());
            request.setFrozenFrontendRoot(frozen.frontendRoot().toString());
            request.setFrozenPublishScriptSha256(frozen.publishScriptSha256());
            dispatchAttempted = true;
            requireOperationBinding(operationId, runtimeControlService.dispatchWorkflowAction(request, workflow.requestedBy()));
            return workflowService.require(workflow.workflowId());
        } catch (ReleaseWorkflowStore.CasConflictException ex) {
            releaseLease(workflow.workflowId(), "backup");
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            recordBackupDispatchFailure(workflow.workflowId(), "BACKUP_DEPLOY_DISPATCH", dispatchAttempted, ex);
            throw ex;
        }
    }

    private RuntimeControlActionReqVO backupRequest(ReleaseWorkflowRecord workflow, String operationId, String action) {
        RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
        request.setAction(action);
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setReleaseAuthorizationId(workflow.backupIntent().authorizationId());
        request.setReason(workflow.reason());
        request.setReleaseTag(workflow.releaseTag());
        request.setSourceSelectionId(workflow.sourceSelectionId());
        request.setExpectedMaintenanceCommit(workflow.maintenanceCommit());
        request.setExpectedApplicationCommit(workflow.applicationCommit());
        request.setExpectedFrontendCommit(workflow.frontendCommit());
        attachWorkflowContext(request, workflow, operationId);
        return request;
    }

    @Autowired
    public ReleaseWorkflowOrchestrator(RuntimeControlProperties properties,
                                       ReleaseWorkflowService workflowService,
                                       RuntimeControlOperationStore operationStore,
                                       RuntimeControlService runtimeControlService,
                                       ReleaseWorkflowAuthorizationService authorizationService,
                                       ReleaseWorkflowProductionPreviewService productionPreviewService,
                                       ReleaseWorkflowTestEvidenceStore testEvidenceStore) {
        this.properties = properties;
        this.workflowService = workflowService;
        this.operationStore = operationStore;
        this.runtimeControlService = runtimeControlService;
        this.authorizationService = authorizationService;
        this.productionPreviewService = productionPreviewService;
        this.testEvidenceStore = testEvidenceStore;
        this.sourceCleanupService = new ReleaseWorkflowSourceCleanupService(properties, new ReleaseWorkflowWorktreeFactory(properties));
        this.backupFinalization = new ReleaseWorkflowBackupFinalizationService(properties, workflowService, runtimeControlService);
    }

    public ReleaseWorkflowOrchestrator(RuntimeControlProperties properties,
                                       ReleaseWorkflowService workflowService,
                                       RuntimeControlOperationStore operationStore,
                                       RuntimeControlService runtimeControlService) {
        this(properties, workflowService, operationStore, runtimeControlService,
                new ReleaseWorkflowAuthorizationService(properties),
                new ReleaseWorkflowProductionPreviewService(properties, runtimeControlService,
                        new ReleaseWorkflowTestEvidenceStore(properties, operationStore)),
                new ReleaseWorkflowTestEvidenceStore(properties, operationStore));
    }

    public synchronized ReleaseWorkflowRecord startBuild(String requestedBy, String reason,
                                                         String sourceSelectionId) {
        properties.getReleaseWorkflow().validate();
        if (!properties.getReleaseWorkflow().getApprovedSourceSelectionId().equals(sourceSelectionId)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_SOURCE_SELECTION_NOT_APPROVED");
        }
        ReleaseWorkflowRecord workflow = workflowService.create(requestedBy, reason, sourceSelectionId);
        if (workflow.operationId() != null) {
            return reconcile(workflow.workflowId());
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("build",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("build");
        }
        activeLeases.put(leaseKey(workflow.workflowId(), "build"), lease);
        boolean dispatched = false;
        try {
            String operationId = newOperationId();
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("build-release");
            request.setReason(reason);
            request.setPublishScope(ReleaseWorkflowContract.PUBLISH_SCOPE);
            request.setIncludeOnlyOffice(false);
            request.setIncludeShowroomBuildPackage(false);
            request.setEnableSmartReleaseReport(false);
            request.setReleaseTag(workflow.releaseTag());
            request.setExpectedMaintenanceCommit(workflow.maintenanceCommit());
            request.setExpectedApplicationCommit(workflow.applicationCommit());
            request.setExpectedFrontendCommit(workflow.frontendCommit());
            request.setSourceSelectionId(workflow.sourceSelectionId());
            attachWorkflowContext(request, workflow, operationId);
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            dispatched = true;
            requireOperationBinding(operationId, operation);
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            if (dispatched) {
                isolateAfterDispatchFailure(workflow.workflowId(), "BUILD_DISPATCH", ex);
            } else {
                releaseLease(workflow.workflowId(), "build");
                ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                if (!current.state().isTerminal()) {
                    workflowService.verifyAdvance(current.workflowId(), current.stateVersion(),
                            ReleaseWorkflowRecord.State.FAILED, "SOURCE_FREEZING", false, true);
                }
            }
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord startTestPublish(String workflowId, String requestedBy,
                                                               String reason) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        if (workflow.backupIntent() != null) throw new IllegalStateException("RELEASE_WORKFLOW_TARGET_MISMATCH");
        if (workflow.state() != ReleaseWorkflowRecord.State.READY) {
            throw new IllegalStateException("RELEASE_WORKFLOW_NOT_READY");
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("test",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("test");
        }
        activeLeases.put(leaseKey(workflow.workflowId(), "test"), lease);
        boolean dispatched = false;
        try {
            String operationId = newOperationId();
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("publish-test");
            request.setReason(reason);
            request.setReleaseTag(workflow.releaseTag());
            request.setExpectedPackageDigest(workflow.packageDigest());
            request.setExpectedManifestDigest(workflow.manifestDigest());
            attachWorkflowContext(request, workflow, operationId);
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            dispatched = true;
            requireOperationBinding(operationId, operation);
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            if (dispatched) {
                isolateAfterDispatchFailure(workflow.workflowId(), "TEST_DEPLOYING_DISPATCH", ex);
            } else {
                releaseLease(workflow.workflowId(), "test");
                failIfActive(workflow.workflowId(), "TEST_DEPLOYING", false, ex);
            }
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord acceptTest(String workflowId, String requestedBy,
                                                         String result, String conclusion) {
        ReleaseWorkflowTestResult testResult = ReleaseWorkflowTestResult.fromApi(result);
        String normalizedConclusion = requireAcceptanceConclusion(conclusion);
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        if (workflow.state() != ReleaseWorkflowRecord.State.TEST_DEPLOYED) {
            throw new IllegalStateException("RELEASE_WORKFLOW_TEST_NOT_DEPLOYED");
        }
        String publishTestOperationId = workflow.testOperationId();
        if (publishTestOperationId == null || workflow.testOperationEvidencePath() == null) {
            throw new IllegalStateException("RELEASE_WORKFLOW_TEST_OPERATION_EVIDENCE_MISSING");
        }
        if (hasRunningTestAcceptanceOperation(workflow)) {
            throw new IllegalStateException("RELEASE_WORKFLOW_TEST_ACCEPTANCE_IN_PROGRESS");
        }
        if (testResult == ReleaseWorkflowTestResult.FAIL) {
            ReleaseWorkflowRecord failed = workflowService.failTestAcceptance(workflow.workflowId(),
                    workflow.stateVersion(), testResult, normalizedConclusion, requestedBy);
            releaseLeasesForState(failed);
            return failed;
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("test",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("test");
        }
        activeLeases.put(leaseKey(workflow.workflowId(), "test"), lease);
        boolean dispatched = false;
        try {
            String operationId = newOperationId();
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("mark-release-tested");
            request.setReason("记录程序包测试验收");
            request.setReleaseTag(workflow.releaseTag());
            request.setTestResult(testResult.name());
            request.setTestConclusion(normalizedConclusion);
            request.setTestOperationId(publishTestOperationId);
            request.setTestOperationEvidencePath(workflow.testOperationEvidencePath());
            request.setExpectedPackageDigest(workflow.packageDigest());
            request.setExpectedManifestDigest(workflow.manifestDigest());
            attachWorkflowContext(request, workflow, operationId);
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            dispatched = true;
            requireOperationBinding(operationId, operation);
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            if (dispatched) {
                isolateAfterDispatchFailure(workflow.workflowId(), "TEST_ACCEPTANCE_DISPATCH", ex);
            } else {
                releaseLease(workflow.workflowId(), "test");
                failIfActive(workflow.workflowId(), "TEST_ACCEPTANCE", true, ex);
            }
            throw ex;
        }
    }

    public synchronized ReleaseAuthorizationGrant authorizeProduction(String workflowId, String approver,
                                                                      String previewId,
                                                                      long expectedStateVersion) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        requireTestedArtifactBinding(workflow);
        productionPreviewService.requireBinding(previewId, workflow, expectedStateVersion);
        return authorizationService.issue(workflow.workflowId(), workflow.releaseTag(), workflow.packageDigest(),
                workflow.manifestDigest(), workflow.presetId(), workflow.presetVersion(), approver,
                previewId, productionPreviewService.require(previewId).getTargetFingerprint(), expectedStateVersion);
    }

    public synchronized ReleaseWorkflowRecord startProductionPromotion(String workflowId, String requestedBy,
                                                                       String reason, String authorizationGrantId,
                                                                       String prodConfirmText, String previewId,
                                                                       long expectedStateVersion,
                                                                       String idempotencyKey) {
        String idempotencyId = requireIdempotencyKey(idempotencyKey);
        String idempotencyScope = workflowId + "|prod|" + idempotencyId;
        String existingOperationId = prodIdempotencyOperations.get(idempotencyScope);
        if (existingOperationId != null) {
            ReleaseWorkflowRecord current = workflowService.require(workflowId);
            if (existingOperationId.equals(current.operationId())) {
                return current;
            }
            RuntimeControlOperationRespVO existing = operationStore.findById(existingOperationId);
            if (existing != null) {
                return current;
            }
            throw new IllegalStateException("RELEASE_WORKFLOW_IDEMPOTENCY_CONFLICT");
        }
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        requireTestedArtifactBinding(workflow);
        productionPreviewService.requireBinding(previewId, workflow, expectedStateVersion);
        ReleaseWorkflowAuthorizationService.WorkflowTuple tuple = productionWorkflowTuple(workflow);
        ReleaseWorkflowAuthorizationService.Validation preflight =
                authorizationService.previewExecution(authorizationGrantId, tuple, prodConfirmText,
                        java.time.Instant.now());
        if (!preflight.valid()) {
            throw new ReleaseWorkflowAuthorizationService.AuthorizationException(preflight.errorCode());
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("prod",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("prod");
        }
        activeLeases.put(leaseKey(workflow.workflowId(), "prod"), lease);
        boolean dispatched = false;
        boolean executionCommitted = false;
        try {
            ReleaseWorkflowAuthorizationService.Validation validation =
                    authorizationService.preview(authorizationGrantId, tuple, java.time.Instant.now());
            if (!validation.valid()) {
                throw new ReleaseWorkflowAuthorizationService.AuthorizationException(validation.errorCode());
            }
            String operationId = newOperationId();
            prodIdempotencyOperations.put(idempotencyScope, operationId);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PROD_PREVIEW, "PROD_PREVIEW", true, true);
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(), operationId);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PROMOTING_PROD, "PROMOTING_PROD", true, false);
            authorizationService.execute(authorizationGrantId, tuple, prodConfirmText, java.time.Instant.now());
            executionCommitted = true;
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("promote-prod");
            request.setReason(reason);
            request.setProdConfirmText(prodConfirmText);
            request.setReleaseTag(workflow.releaseTag());
            request.setTestOperationId(workflow.testOperationId());
            request.setTestOperationEvidencePath(workflow.testOperationEvidencePath());
            request.setExpectedPackageDigest(workflow.packageDigest());
            request.setExpectedManifestDigest(workflow.manifestDigest());
            attachWorkflowContext(request, workflow, operationId);
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            dispatched = true;
            requireOperationBinding(operationId, operation);
            return workflowService.require(workflow.workflowId());
        } catch (RuntimeException ex) {
            if (dispatched || executionCommitted) {
                isolateAfterDispatchFailure(workflow.workflowId(), "PROMOTING_PROD_EXECUTION_COMMITTED", ex);
            } else {
                releaseLease(workflow.workflowId(), "prod");
                failIfActive(workflow.workflowId(), "PROMOTING_PROD", false, ex);
            }
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord reconcile(String workflowId) {
        ReleaseWorkflowRecord workflow = workflowService.require(workflowId);
        if (workflow.operationId() == null || workflow.state().isTerminal()) {
            return workflow;
        }
        RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
        if (shouldFinalizeBackup(workflow, operation)) {
            queueBackupConfirmation(workflowId);
            return workflowService.require(workflowId);
        }
        if (workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED) return workflow;
        if (operation != null) {
            try {
                workflow = advanceObservedBuildStages(workflow, operation);
            } catch (IllegalStateException ex) {
                if (!(ex.getCause() instanceof java.nio.charset.CharacterCodingException)
                        || !"RELEASE_WORKFLOW_STAGE_LOG_INVALID".equals(ex.getMessage())
                        || !("failed".equals(operation.getStatus()) || "blocked".equals(operation.getStatus()))) {
                    throw ex;
                }
                ReleaseWorkflowRecord failed = workflowService.verifyAdvance(workflow.workflowId(),
                        workflow.stateVersion(), ReleaseWorkflowRecord.State.FAILED, "LOG_DECODING", false, true,
                        List.of("operation/" + operation.getOperationId() + ": RELEASE_WORKFLOW_STAGE_LOG_INVALID"));
                releaseLeasesForState(failed);
                return failed;
            }
        }
        if (operation == null && workflow.backupIntent() != null) {
            if (sourcePreparationAlive(workflow.workflowId())) return workflow;
            if (workflow.lastHeartbeatAt().plus(properties.getReleaseWorkflow().getHeartbeatTimeout())
                    .isAfter(Instant.now())) return workflow;
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "OPERATION_UNKNOWN", false, false);
        }
        if (operation == null || "running".equals(operation.getStatus())) {
            return workflow;
        }
        if ("succeeded".equals(operation.getStatus())) {
            workflow = advanceSucceeded(workflow, operation);
        } else if ("failed".equals(operation.getStatus()) || "blocked".equals(operation.getStatus())) {
            boolean verifiedZeroWrite = verifiedZeroWrite(workflow, operation);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    workflow.backupIntent() != null && !verifiedZeroWrite ? ReleaseWorkflowRecord.State.RECOVERY_REQUIRED : ReleaseWorkflowRecord.State.FAILED,
                    workflow.state().name(), false, verifiedZeroWrite || (workflow.backupIntent() == null && !workflow.state().isWriteStage()));
        } else if (workflow.backupIntent() != null) {
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "OPERATION_UNKNOWN", false, false);
        }
        releaseLeasesForState(workflow);
        if (workflow.state() == ReleaseWorkflowRecord.State.READY && workflow.backupIntent() != null) {
            return dispatchBackupDeploy(workflow);
        }
        return workflow;
    }

    private boolean shouldFinalizeBackup(ReleaseWorkflowRecord workflow, RuntimeControlOperationRespVO operation) {
        if (workflow.backupIntent() == null) return false;
        if (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING) return true;
        if (workflow.state() != ReleaseWorkflowRecord.State.BACKUP_DEPLOYING
                && workflow.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED) return false;
        if (operation == null) return workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYING
                && workflow.lastHeartbeatAt().plus(properties.getReleaseWorkflow().getHeartbeatTimeout()).isBefore(Instant.now());
        return "publish-backup".equals(operation.getAction()) && !verifiedZeroWrite(workflow, operation)
                && (!"running".equals(operation.getStatus())
                    || (workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                        && !runtimeControlService.isOperationExecutorAlive(operation.getOperationId())));
    }

    private void queueBackupConfirmation(String workflowId) {
        var task = new java.util.concurrent.FutureTask<Void>(() -> {
            activeConfirmationThreads.put(workflowId, Thread.currentThread());
            try {
                var current = workflowService.require(workflowId);
                if (current.state().isTerminal()) return null;
                var finalized = backupFinalization.reconcile(workflowId);
                if (finalized.isPresent()) releaseLeasesForState(finalized.get());
                else {
                    current = workflowService.require(workflowId);
                    if (current.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYING) {
                        workflowService.verifyAdvance(workflowId, current.stateVersion(),
                                ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "BACKUP_RUNTIME_RECEIPT_MISSING", false, false);
                    }
                }
            } catch (RuntimeException error) {
                isolateAfterDispatchFailure(workflowId, "BACKUP_FINALIZATION", error);
                org.slf4j.LoggerFactory.getLogger(ReleaseWorkflowOrchestrator.class).error(
                        "Review publication confirmation blocked for {}: {}", workflowId, safeCauseReference(error));
            } finally {
                activeConfirmationThreads.remove(workflowId);
                confirmations.remove(workflowId);
            }
            return null;
        });
        if (confirmations.putIfAbsent(workflowId, task) != null) return;
        try { confirmationExecutor.execute(task); }
        catch (RuntimeException rejected) {
            confirmations.remove(workflowId, task);
            isolateAfterDispatchFailure(workflowId, "BACKUP_CONFIRMATION_WORKER_REJECTED", rejected);
            throw rejected;
        }
    }

    private boolean confirmationAlive(String workflowId) {
        var task = confirmations.get(workflowId);
        var thread = activeConfirmationThreads.get(workflowId);
        return task != null && !task.isDone() && thread != null && thread.isAlive();
    }

    /**
     * Background reconciliation for the button workflow.  User polling is not
     * the authority for state progress: terminal low-level operations advance
     * their workflow here, while running operations with fresh logs refresh the
     * workflow heartbeat so long builds are not mistaken for dead attempts.
     */
    public synchronized List<ReleaseWorkflowRecord> reconcileActiveWorkflows(Instant now) {
        List<ReleaseWorkflowRecord> changed = new ArrayList<>();
        for (ReleaseWorkflowRecord workflow : workflowService.list()) {
            if (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYED) {
                queueSourceCleanup(workflow);
                continue;
            }
            if (workflow.backupIntent() != null && (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING
                    || workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED)) {
                if (confirmationAlive(workflow.workflowId())) {
                    changed.add(workflowService.heartbeat(workflow.workflowId(), workflow.stateVersion(), now));
                    continue;
                }
                ReleaseWorkflowRecord observed = reconcile(workflow.workflowId());
                if (observed.stateVersion() != workflow.stateVersion()) changed.add(observed);
                continue;
            }
            if (workflow.backupIntent() != null && workflow.state() == ReleaseWorkflowRecord.State.SOURCE_FREEZING
                    && workflow.operationId() == null) {
                changed.add(dispatchBackupBuild(workflow));
                continue;
            }
            if (workflow.state().isTerminal()
                    || workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                    || workflow.operationId() == null) {
                continue;
            }
            RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
            if (operation == null) {
                if (sourcePreparationAlive(workflow.workflowId())) {
                    changed.add(workflowService.heartbeat(workflow.workflowId(), workflow.stateVersion(), now));
                } else if (workflow.backupIntent() != null) changed.add(reconcile(workflow.workflowId()));
                continue;
            }
            if ("running".equals(operation.getStatus())) {
                ReleaseWorkflowRecord observed = advanceObservedBuildStages(workflow, operation);
                if (observed.stateVersion() != workflow.stateVersion()) {
                    changed.add(observed);
                    continue;
                }
                if (workflow.backupIntent() != null) {
                    if (runtimeControlService.isOperationExecutorAlive(operation.getOperationId())) {
                        ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                        changed.add(workflowService.heartbeat(current.workflowId(), current.stateVersion(), now));
                    }
                    continue;
                }
                Instant progressAt = recentOperationLogProgressAt(operation.getOperationId(), now);
                if (progressAt != null) {
                    ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                    if (!current.state().isTerminal()) {
                        changed.add(workflowService.heartbeat(current.workflowId(), current.stateVersion(), progressAt));
                    }
                }
                continue;
            }
            ReleaseWorkflowRecord reconciled = reconcile(workflow.workflowId());
            if (reconciled.stateVersion() != workflow.stateVersion()) {
                changed.add(reconciled);
            }
        }
        return changed;
    }

    public synchronized ReleaseWorkflowRecord cancel(String workflowId) {
        ReleaseWorkflowRecord current = workflowService.require(workflowId);
        if (current.backupIntent() != null && (current.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING
                || backupFinalization.hasAcceptedDecision(workflowId))) {
            throw new IllegalStateException("RELEASE_WORKFLOW_ACCEPTED_PUBLICATION_CANNOT_BE_CANCELED");
        }
        var preparation = sourcePreparations.get(workflowId);
        if (preparation != null) {
            preparation.cancel(true);
            if (current.state() == ReleaseWorkflowRecord.State.SOURCE_FREEZING) {
                ReleaseWorkflowRecord canceled = workflowService.verifyAdvance(workflowId, current.stateVersion(),
                        ReleaseWorkflowRecord.State.CANCELED, "SOURCE_PREPARATION_CANCELED", false, true);
                sourcePreparations.remove(workflowId);
                releaseLease(workflowId, "build");
                return canceled;
            }
        }
        if (current.operationId() != null && !current.state().isTerminal()) {
            RuntimeControlOperationRespVO operation = operationStore.findById(current.operationId());
            if (operation != null && "running".equals(operation.getStatus())) {
                if (!runtimeControlService.cancelOperation(current.operationId())) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED");
                }
                RuntimeControlOperationRespVO terminated = operationStore.findById(current.operationId());
                if (terminated != null && "running".equals(terminated.getStatus())) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED");
                }
            }
        }
        ReleaseWorkflowRecord canceled = workflowService.cancel(workflowId);
        releaseLeasesForState(canceled);
        return canceled;
    }

    /**
     * Reconciles heartbeat-expired workflows after first terminating any
     * still-running low-level operation. Recovery is fail-closed: a workflow
     * is not advanced while its operation remains running or cannot be
     * verified as terminated.
     */
    public synchronized List<ReleaseWorkflowRecord> recoverStaleWorkflows(Instant now) {
        List<ReleaseWorkflowRecord> stale = workflowService.list().stream()
                .filter(record -> record.state().isHeartbeatMonitored())
                .filter(record -> record.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED)
                .filter(record -> record.lastHeartbeatAt()
                        .plus(properties.getReleaseWorkflow().getHeartbeatTimeout()).isBefore(now))
                .toList();
        for (ReleaseWorkflowRecord workflow : stale) {
            if (sourcePreparationAlive(workflow.workflowId()) || confirmationAlive(workflow.workflowId())) {
                workflowService.heartbeat(workflow.workflowId(), workflow.stateVersion(), now);
                continue;
            }
            if (workflow.operationId() == null) {
                continue;
            }
            RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
            if (operation != null && !"running".equals(operation.getStatus())) {
                reconcile(workflow.workflowId());
                continue;
            }
            if (operation != null && "running".equals(operation.getStatus())) {
                if (workflow.backupIntent() != null) {
                    ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                    if (runtimeControlService.isOperationExecutorAlive(operation.getOperationId())) {
                        workflowService.heartbeat(current.workflowId(), current.stateVersion(), now);
                    } else {
                        workflowService.verifyAdvance(current.workflowId(), current.stateVersion(),
                                ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, "EXECUTOR_LIVENESS_UNKNOWN", false, false);
                    }
                    continue;
                }
                Instant progressAt = recentOperationLogProgressAt(operation.getOperationId(), now);
                if (progressAt != null) {
                    ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                    if (!current.state().isTerminal()) {
                        ReleaseWorkflowRecord observed = advanceObservedBuildStages(current, operation);
                        if (observed.stateVersion() == current.stateVersion()) {
                            workflowService.heartbeat(current.workflowId(), current.stateVersion(), progressAt);
                        }
                    }
                    continue;
                }
                if (!runtimeControlService.cancelOperation(workflow.operationId())) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED");
                }
                RuntimeControlOperationRespVO terminated = operationStore.findById(workflow.operationId());
                if (terminated != null && "running".equals(terminated.getStatus())) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED");
                }
            }
        }
        List<ReleaseWorkflowRecord> recovered = workflowService.recoverStaleWorkflows(now);
        recovered.forEach(this::releaseLeasesForState);
        return recovered;
    }

    private Instant recentOperationLogProgressAt(String operationId, Instant now) {
        Path logPath = operationStore.getOperationLogPath(operationId);
        try {
            if (!Files.isRegularFile(logPath)) {
                return null;
            }
            Duration timeout = properties.getReleaseWorkflow().getHeartbeatTimeout();
            Instant lastModified = Files.getLastModifiedTime(logPath).toInstant();
            return lastModified.plus(timeout).isBefore(now) ? null : lastModified;
        } catch (IOException | SecurityException ex) {
            throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_LOG_INSPECTION_FAILED", ex);
        }
    }

    private ReleaseWorkflowRecord advanceSucceeded(ReleaseWorkflowRecord workflow,
                                                   RuntimeControlOperationRespVO operation) {
        if ("build-release".equals(operation.getAction())
                && (workflow.state() == ReleaseWorkflowRecord.State.PREFLIGHTING
                || workflow.state() == ReleaseWorkflowRecord.State.TESTING
                || workflow.state() == ReleaseWorkflowRecord.State.BUILDING)) {
            workflow = advanceObservedBuildStages(workflow, operation);
            if (workflow.state() != ReleaseWorkflowRecord.State.BUILDING) {
                return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                        ReleaseWorkflowRecord.State.FAILED, workflow.state().name(), false,
                        !workflow.state().isWriteStage(),
                        List.of("operation/" + operation.getOperationId() + ": RELEASE_WORKFLOW_STAGE_MISSING"));
            }
            String releaseTag = workflow.releaseTag();
            RuntimeControlReleasePackageRespVO releasePackage = runtimeControlService.getReleasePackage(releaseTag)
                    .orElseThrow(() -> new IllegalStateException("RELEASE_PACKAGE_EVIDENCE_MISSING"));
            workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                    releasePackage.getPackageDigest(), releasePackage.getManifestDigest());
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.READY, "READY", true, true);
        }
        if (workflow.state() == ReleaseWorkflowRecord.State.TEST_DEPLOYING
                && "publish-test".equals(operation.getAction())) {
            Path evidencePath = testEvidenceStore.write(workflow.workflowId(), workflow.releaseTag(), operation);
            workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId(), evidencePath.toString());
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TEST_DEPLOYED, "TEST_DEPLOYED", true, false);
        }
        if (workflow.state() == ReleaseWorkflowRecord.State.TEST_DEPLOYED
                && "mark-release-tested".equals(operation.getAction())) {
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TESTED, "TESTED", true, false);
        }
        if (workflow.state() == ReleaseWorkflowRecord.State.PROMOTING_PROD
                && "promote-prod".equals(operation.getAction())) {
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.COMPLETED, "COMPLETED", true, false);
        }
        return workflow;
    }

    private ReleaseWorkflowRecord advanceObservedBuildStages(ReleaseWorkflowRecord workflow,
                                                             RuntimeControlOperationRespVO operation) {
        if (!"build-release".equals(operation.getAction())) {
            return workflow;
        }
        if (workflow.state() != ReleaseWorkflowRecord.State.PREFLIGHTING
                && workflow.state() != ReleaseWorkflowRecord.State.TESTING
                && workflow.state() != ReleaseWorkflowRecord.State.BUILDING) {
            return workflow;
        }
        for (ObservedWorkflowStage marker : readObservedBuildStages(operation.getOperationId())) {
            if (isObservedStageAlreadyApplied(workflow.state(), marker.state())) {
                continue;
            }
            if (!isNextObservedStage(workflow.state(), marker.state())) {
                throw new IllegalStateException("RELEASE_WORKFLOW_STAGE_SEQUENCE_INVALID: "
                        + workflow.state() + " -> " + marker.state());
            }
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    marker.state(), marker.state().name(), true, true,
                    List.of(marker.evidenceRef()));
        }
        return workflow;
    }

    private List<ObservedWorkflowStage> readObservedBuildStages(String operationId) {
        Path logPath = operationStore.getOperationLogPath(operationId);
        if (!Files.isRegularFile(logPath)) {
            return List.of();
        }
        try {
            List<ObservedWorkflowStage> result = new ArrayList<>();
            for (String line : Files.readAllLines(logPath, StandardCharsets.UTF_8)) {
                String marker = line.trim();
                if (!marker.startsWith("RELEASE_WORKFLOW_STAGE=")) {
                    continue;
                }
                String value = marker.substring("RELEASE_WORKFLOW_STAGE=".length()).split("\\s+", 2)[0];
                ReleaseWorkflowRecord.State state = ReleaseWorkflowRecord.State.valueOf(value);
                if (state != ReleaseWorkflowRecord.State.TESTING
                        && state != ReleaseWorkflowRecord.State.BUILDING) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_STAGE_UNSUPPORTED: " + value);
                }
                result.add(new ObservedWorkflowStage(state, "operation/" + operationId + ": " + marker));
            }
            return result;
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            throw new IllegalStateException("RELEASE_WORKFLOW_STAGE_LOG_INVALID", ex);
        }
    }

    private boolean isObservedStageAlreadyApplied(ReleaseWorkflowRecord.State current,
                                                  ReleaseWorkflowRecord.State observed) {
        return buildStageOrder(current) >= buildStageOrder(observed);
    }

    private boolean isNextObservedStage(ReleaseWorkflowRecord.State current,
                                        ReleaseWorkflowRecord.State observed) {
        return buildStageOrder(observed) == buildStageOrder(current) + 1;
    }

    private int buildStageOrder(ReleaseWorkflowRecord.State state) {
        return switch (state) {
            case PREFLIGHTING -> 0;
            case TESTING -> 1;
            case BUILDING, READY, TEST_DEPLOYING, TEST_DEPLOYED, TESTED, PROD_PREVIEW,
                    PROMOTING_PROD, COMPLETED -> 2;
            default -> -1;
        };
    }

    private void attachWorkflowContext(RuntimeControlActionReqVO request, ReleaseWorkflowRecord workflow,
                                        String operationId) {
        request.setReleaseWorkflowId(workflow.workflowId());
        request.setReleaseWorkflowExpectedStateVersion(workflow.stateVersion());
        request.setPreassignedOperationId(operationId);
    }

    private String newOperationId() {
        return UUID.randomUUID().toString();
    }

    private static String requireAcceptanceConclusion(String conclusion) {
        if (conclusion == null || conclusion.isBlank() || !conclusion.equals(conclusion.trim())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TEST_CONCLUSION_INVALID");
        }
        return conclusion;
    }

    private void requireOperationBinding(String expectedOperationId, RuntimeControlOperationRespVO operation) {
        if (operation == null || !expectedOperationId.equals(operation.getOperationId())) {
            throw new IllegalStateException("RELEASE_WORKFLOW_OPERATION_ID_MISMATCH");
        }
    }

    private boolean hasRunningTestAcceptanceOperation(ReleaseWorkflowRecord workflow) {
        if (workflow.operationId() == null) {
            return false;
        }
        RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
        return operation != null
                && "running".equals(operation.getStatus())
                && "mark-release-tested".equals(operation.getAction());
    }

    private void failIfActive(String workflowId, String stage, boolean zeroWriteEvidence, RuntimeException cause) {
        ReleaseWorkflowRecord current = workflowService.require(workflowId);
        if (!current.state().isTerminal()) {
            try {
                workflowService.verifyAdvance(current.workflowId(), current.stateVersion(),
                        ReleaseWorkflowRecord.State.FAILED, stage, false, zeroWriteEvidence,
                        List.of(safeCauseReference(cause)));
            } catch (ReleaseWorkflowService.InvalidTransitionException transitionFailure) {
                if (cause != null) {
                    cause.addSuppressed(transitionFailure);
                } else {
                    throw transitionFailure;
                }
            }
        }
    }

    private void isolateAfterDispatchFailure(String workflowId, String stage, RuntimeException cause) {
        try {
            ReleaseWorkflowRecord current = workflowService.require(workflowId);
            if (!current.state().isTerminal() && current.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED) {
                workflowService.verifyAdvance(current.workflowId(), current.stateVersion(),
                        ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, stage, false, false,
                        List.of(safeCauseReference(cause)));
            }
        } catch (RuntimeException recoveryFailure) {
            cause.addSuppressed(recoveryFailure);
        }
    }

    private void releaseLeasesForState(ReleaseWorkflowRecord workflow) {
        if (workflow.state().isTerminal()) {
            releaseAllLeases(workflow.workflowId());
        } else if (workflow.state() == ReleaseWorkflowRecord.State.READY) {
            releaseLease(workflow.workflowId(), "build");
        } else if (workflow.state() == ReleaseWorkflowRecord.State.TEST_DEPLOYED
                || workflow.state() == ReleaseWorkflowRecord.State.TESTED) {
            releaseLease(workflow.workflowId(), "test");
        }
    }

    private static String safeCauseReference(RuntimeException cause) {
        if (cause == null) return "failure:UNSPECIFIED";
        var matcher = java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]{2,100}")
                .matcher(String.valueOf(cause.getMessage()));
        return "failure:" + (matcher.find() ? matcher.group() : cause.getClass().getSimpleName());
    }

    private void recordBackupDispatchFailure(String workflowId, String stage, boolean dispatchAttempted, RuntimeException cause) {
        var current = workflowService.require(workflowId);
        var operation = current.operationId() == null ? null : operationStore.findById(current.operationId());
        if (!dispatchAttempted || verifiedZeroWrite(current, operation)) {
            failIfActive(workflowId, stage, true, cause);
            releaseAllLeases(workflowId);
        } else {
            isolateAfterDispatchFailure(workflowId, stage, cause);
        }
    }

    private static boolean verifiedZeroWrite(ReleaseWorkflowRecord workflow, RuntimeControlOperationRespVO operation) {
        if (operation == null || !Boolean.TRUE.equals(operation.getZeroWriteEvidence())) return false;
        if (workflow.backupIntent() == null) return true;
        var parameters = operation.getParameters();
        return workflow.operationId().equals(operation.getOperationId()) && "backup".equals(operation.getEnvironment())
                && ("build-release".equals(operation.getAction()) || "publish-backup".equals(operation.getAction()))
                && workflow.requestedBy().equals(operation.getRequestedBy()) && parameters != null
                && workflow.workflowId().equals(parameters.get("workflowId"))
                && workflow.releaseTag().equals(parameters.get("releaseTag"))
                && workflow.backupIntent().authorizationId().equals(parameters.get("authorizationId"))
                && workflow.sourceSelectionId().equals(parameters.get("sourceSelectionId"))
                && workflow.maintenanceCommit().equals(parameters.get("maintenanceCommit"))
                && workflow.applicationCommit().equals(parameters.get("applicationCommit"))
                && workflow.frontendCommit().equals(parameters.get("frontendCommit"));
    }

    private void releaseLease(String workflowId, String environment) {
        ReleaseWorkflowService.OptionalLease lease = activeLeases.remove(leaseKey(workflowId, environment));
        if (lease != null) {
            lease.close();
        }
    }

    private void releaseAllLeases(String workflowId) {
        releaseLease(workflowId, "build");
        releaseLease(workflowId, "test");
        releaseLease(workflowId, "prod");
        releaseLease(workflowId, "backup");
    }

    private String leaseKey(String workflowId, String environment) {
        return workflowId + ":" + environment;
    }

    private static String requireIdempotencyKey(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_IDEMPOTENCY_KEY_INVALID");
        }
        return value;
    }

    private ReleaseWorkflowAuthorizationService.WorkflowTuple productionWorkflowTuple(ReleaseWorkflowRecord workflow) {
        return new ReleaseWorkflowAuthorizationService.WorkflowTuple(
                workflow.workflowId(), workflow.releaseTag(), workflow.packageDigest(),
                workflow.manifestDigest(), "prod", workflow.presetId(), workflow.presetVersion(),
                workflow.publishScope(), workflow.state());
    }

    private static void requireTestedArtifactBinding(ReleaseWorkflowRecord workflow) {
        if (workflow.state() != ReleaseWorkflowRecord.State.TESTED) {
            throw new IllegalStateException("RELEASE_WORKFLOW_NOT_TESTED");
        }
        if (workflow.packageDigest() == null || workflow.manifestDigest() == null
                || workflow.testOperationId() == null || workflow.testOperationEvidencePath() == null) {
            throw new IllegalStateException("RELEASE_WORKFLOW_PROMOTION_EVIDENCE_MISSING");
        }
    }

    public static class WorkflowLeaseConflictException extends RuntimeException {
        public WorkflowLeaseConflictException(String environment) {
            super("RELEASE_WORKFLOW_LEASE_CONFLICT: " + environment);
        }
    }

    private record ObservedWorkflowStage(ReleaseWorkflowRecord.State state, String evidenceRef) { }
}
