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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
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
    private final Map<String, ReleaseWorkflowService.OptionalLease> activeLeases = new ConcurrentHashMap<>();

    @Autowired
    public ReleaseWorkflowOrchestrator(RuntimeControlProperties properties,
                                       ReleaseWorkflowService workflowService,
                                       RuntimeControlOperationStore operationStore,
                                       RuntimeControlService runtimeControlService,
                                       ReleaseWorkflowAuthorizationService authorizationService) {
        this.properties = properties;
        this.workflowService = workflowService;
        this.operationStore = operationStore;
        this.runtimeControlService = runtimeControlService;
        this.authorizationService = authorizationService;
    }

    public ReleaseWorkflowOrchestrator(RuntimeControlProperties properties,
                                       ReleaseWorkflowService workflowService,
                                       RuntimeControlOperationStore operationStore,
                                       RuntimeControlService runtimeControlService) {
        this(properties, workflowService, operationStore, runtimeControlService,
                new ReleaseWorkflowAuthorizationService(properties));
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
        activeLeases.put(workflow.workflowId(), lease);
        try {
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
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId());
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
        } catch (RuntimeException ex) {
            releaseLease(workflow.workflowId());
            ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
            if (!current.state().isTerminal()) {
                workflowService.verifyAdvance(current.workflowId(), current.stateVersion(),
                        ReleaseWorkflowRecord.State.FAILED, "SOURCE_FREEZING", false, true);
            }
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord startTestPublish(String workflowId, String requestedBy,
                                                               String reason) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        if (workflow.state() != ReleaseWorkflowRecord.State.READY) {
            throw new IllegalStateException("RELEASE_WORKFLOW_NOT_READY");
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("test",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("test");
        }
        activeLeases.put(workflow.workflowId(), lease);
        try {
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("publish-test");
            request.setReason(reason);
            request.setReleaseTag(workflow.releaseTag());
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId());
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
        } catch (RuntimeException ex) {
            releaseLease(workflow.workflowId());
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord acceptTest(String workflowId, String requestedBy,
                                                         String conclusion) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        if (workflow.state() != ReleaseWorkflowRecord.State.TEST_DEPLOYED) {
            throw new IllegalStateException("RELEASE_WORKFLOW_TEST_NOT_DEPLOYED");
        }
        String publishTestOperationId = workflow.testOperationId();
        if (publishTestOperationId == null || workflow.testOperationEvidencePath() == null) {
            throw new IllegalStateException("RELEASE_WORKFLOW_TEST_OPERATION_EVIDENCE_MISSING");
        }
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("test",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("test");
        }
        activeLeases.put(workflow.workflowId(), lease);
        try {
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("mark-release-tested");
            request.setReason("记录程序包测试验收");
            request.setReleaseTag(workflow.releaseTag());
            request.setTestConclusion(conclusion);
            request.setTestOperationId(publishTestOperationId);
            request.setTestOperationEvidencePath(workflow.testOperationEvidencePath());
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            return workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId());
        } catch (RuntimeException ex) {
            releaseLease(workflow.workflowId());
            throw ex;
        }
    }

    public synchronized ReleaseAuthorizationGrant authorizeProduction(String workflowId, String approver) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        requireTestedArtifactBinding(workflow);
        return authorizationService.issue(workflow.workflowId(), workflow.releaseTag(), workflow.packageDigest(),
                workflow.manifestDigest(), workflow.presetId(), workflow.presetVersion(), approver);
    }

    public synchronized ReleaseWorkflowRecord startProductionPromotion(String workflowId, String requestedBy,
                                                                       String reason, String authorizationGrantId,
                                                                       String prodConfirmText) {
        ReleaseWorkflowRecord workflow = reconcile(workflowId);
        requireTestedArtifactBinding(workflow);
        ReleaseWorkflowService.OptionalLease lease = workflowService.acquireEnvironmentLease("prod",
                workflow.workflowId());
        if (!lease.acquired()) {
            lease.close();
            throw new WorkflowLeaseConflictException("prod");
        }
        activeLeases.put(workflow.workflowId(), lease);
        try {
            ReleaseWorkflowAuthorizationService.WorkflowTuple tuple =
                    new ReleaseWorkflowAuthorizationService.WorkflowTuple(
                            workflow.workflowId(), workflow.releaseTag(), workflow.packageDigest(),
                            workflow.manifestDigest(), "prod", workflow.presetId(), workflow.presetVersion(),
                            workflow.publishScope(), workflow.state());
            authorizationService.execute(authorizationGrantId, tuple, prodConfirmText, java.time.Instant.now());
            RuntimeControlActionReqVO request = new RuntimeControlActionReqVO();
            request.setAction("promote-prod");
            request.setReason(reason);
            request.setProdConfirmText(prodConfirmText);
            request.setReleaseTag(workflow.releaseTag());
            request.setTestOperationId(workflow.testOperationId());
            request.setTestOperationEvidencePath(workflow.testOperationEvidencePath());
            RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(request, requestedBy);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PROD_PREVIEW, "PROD_PREVIEW", true, true);
            workflow = workflowService.assignOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId());
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PROMOTING_PROD, "PROMOTING_PROD", true, false);
        } catch (RuntimeException ex) {
            releaseLease(workflow.workflowId());
            throw ex;
        }
    }

    public synchronized ReleaseWorkflowRecord reconcile(String workflowId) {
        ReleaseWorkflowRecord workflow = workflowService.require(workflowId);
        if (workflow.operationId() == null || workflow.state().isTerminal()) {
            return workflow;
        }
        RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
        if (operation == null || "running".equals(operation.getStatus())) {
            return workflow;
        }
        if ("succeeded".equals(operation.getStatus())) {
            workflow = advanceSucceeded(workflow, operation);
        } else if ("failed".equals(operation.getStatus()) || "blocked".equals(operation.getStatus())) {
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.FAILED, workflow.state().name(), false,
                    !workflow.state().isWriteStage());
        }
        if (workflow.state().isTerminal() || workflow.state() == ReleaseWorkflowRecord.State.READY
                || workflow.state() == ReleaseWorkflowRecord.State.TEST_DEPLOYED) {
            releaseLease(workflowId);
        }
        return workflow;
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
            if (workflow.state().isTerminal()
                    || workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                    || workflow.operationId() == null) {
                continue;
            }
            RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
            if (operation == null) {
                continue;
            }
            if ("running".equals(operation.getStatus())) {
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
        releaseLease(workflowId);
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
                .filter(record -> !record.state().isTerminal())
                .filter(record -> record.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED)
                .filter(record -> record.lastHeartbeatAt()
                        .plus(properties.getReleaseWorkflow().getHeartbeatTimeout()).isBefore(now))
                .toList();
        for (ReleaseWorkflowRecord workflow : stale) {
            if (workflow.operationId() == null) {
                continue;
            }
            RuntimeControlOperationRespVO operation = operationStore.findById(workflow.operationId());
            if (operation != null && !"running".equals(operation.getStatus())) {
                reconcile(workflow.workflowId());
                continue;
            }
            if (operation != null && "running".equals(operation.getStatus())) {
                Instant progressAt = recentOperationLogProgressAt(operation.getOperationId(), now);
                if (progressAt != null) {
                    ReleaseWorkflowRecord current = workflowService.require(workflow.workflowId());
                    if (!current.state().isTerminal()) {
                        workflowService.heartbeat(current.workflowId(), current.stateVersion(), progressAt);
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
        return workflowService.recoverStaleWorkflows(now);
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
        if (workflow.state() == ReleaseWorkflowRecord.State.PREFLIGHTING
                && "build-release".equals(operation.getAction())) {
            String releaseTag = workflow.releaseTag();
            RuntimeControlReleasePackageRespVO releasePackage = runtimeControlService.getReleasePackages().stream()
                    .filter(item -> releaseTag.equals(item.getReleaseTag()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("RELEASE_PACKAGE_EVIDENCE_MISSING"));
            workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                    releasePackage.getPackageDigest(), releasePackage.getManifestDigest());
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TESTING, "TESTING", true, true);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, true);
            return workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.READY, "READY", true, true);
        }
        if (workflow.state() == ReleaseWorkflowRecord.State.TEST_DEPLOYING
                && "publish-test".equals(operation.getAction())) {
            workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                    operation.getOperationId(), operationStore.getOperationPath(operation.getOperationId()).toString());
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

    private void releaseLease(String workflowId) {
        ReleaseWorkflowService.OptionalLease lease = activeLeases.remove(workflowId);
        if (lease != null) {
            lease.close();
        }
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
}
