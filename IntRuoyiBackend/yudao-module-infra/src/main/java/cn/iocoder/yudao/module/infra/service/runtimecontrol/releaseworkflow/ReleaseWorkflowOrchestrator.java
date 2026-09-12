package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
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

    public synchronized ReleaseWorkflowRecord cancel(String workflowId) {
        ReleaseWorkflowRecord canceled = workflowService.cancel(workflowId);
        releaseLease(workflowId);
        return canceled;
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
