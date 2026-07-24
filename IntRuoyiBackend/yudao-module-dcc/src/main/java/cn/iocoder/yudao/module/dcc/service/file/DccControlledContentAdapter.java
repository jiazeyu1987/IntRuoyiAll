package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DccControlledContentAdapter {

    @Resource
    private ControlledContentLifecycleCoreService lifecycleCoreService;

    public void recordSubmitted(DccControlledFileDO file, Long actorId, String processInstanceId) {
        requireFile(file);
        ControlledContentKey key = dccKey(file);
        ControlledContentVersionRefDO activeRef = lifecycleCoreService.getActiveRef(key);
        lifecycleCoreService.createCandidateRef(key, file.getMasterId(), file.getId(), file.getVersionNo(),
                file.getStatus(), activeRef == null ? null : activeRef.getId(),
                activeRef == null ? null : activeRef.getNativeVersionId(), actorId,
                "dcc controlled file submitted");
        lifecycleCoreService.transitionVersionRef(key, file.getId(), ControlledContentCanonicalStatus.IN_REVIEW,
                file.getStatus(), ControlledContentTransitionAction.SUBMIT, actorId,
                "dcc controlled file submitted", processInstanceId);
    }

    public void recordWithdrawn(DccControlledFileDO file, Long actorId, String reason) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRef(dccKey(file), file.getId(), ControlledContentCanonicalStatus.WITHDRAWN,
                DccControlledFileStatusEnum.WITHDRAWN.getStatus(), ControlledContentTransitionAction.WITHDRAW,
                actorId, StrUtil.blankToDefault(reason, "dcc controlled file withdrawn"),
                file.getProcessInstanceId());
    }

    public void recordResubmitted(DccControlledFileDO withdrawnFile, Long newFileId) {
        requireFile(withdrawnFile);
        if (newFileId == null) {
            throw new IllegalArgumentException("newFileId must not be null");
        }
        lifecycleCoreService.linkSuccessorRef(dccKey(withdrawnFile), withdrawnFile.getId(), newFileId);
    }

    public void recordRejected(DccControlledFileDO file, Long actorId, String reason, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.REJECTED,
                DccControlledFileStatusEnum.REJECTED.getStatus(), ControlledContentTransitionAction.REJECT,
                actorId, StrUtil.blankToDefault(reason, "dcc controlled file rejected"), file.getProcessInstanceId(),
                requireEventKey(eventKey, "reject"));
    }

    public void recordFinalizationStarted(DccControlledFileDO file, Long actorId, String eventKey) {
        requireFile(file);
        String normalizedEventKey = requireEventKey(eventKey, "finalization");
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.APPROVE,
                actorId, "dcc controlled file approved", file.getProcessInstanceId(),
                normalizedEventKey + ":approve");
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(), ControlledContentTransitionAction.START_FINALIZATION,
                actorId, "dcc controlled file finalization started", file.getProcessInstanceId(),
                normalizedEventKey + ":start-finalization");
    }

    public void recordApprovedReadyToPublish(DccControlledFileDO file, Long actorId, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), ControlledContentTransitionAction.APPROVE,
                actorId, "dcc controlled file approved and waiting for publish",
                file.getProcessInstanceId(), requireEventKey(eventKey, "approval") + ":approve");
    }

    public void recordPublishFinalizationStarted(DccControlledFileDO file, Long actorId, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(),
                ControlledContentTransitionAction.START_FINALIZATION,
                actorId, "dcc controlled file publish finalization started", file.getProcessInstanceId(),
                requireEventKey(eventKey, "publish finalization") + ":start-finalization");
    }

    public void recordApprovedUploadFinalizationStarted(DccControlledFileDO candidate, Long actorId,
                                                        String approvalProcessInstanceId, String eventKey) {
        requireFile(candidate);
        String normalizedEventKey = requireEventKey(eventKey, "form-center upload finalization");
        ControlledContentKey key = dccKey(candidate);
        ControlledContentVersionRefDO activeRef = lifecycleCoreService.getActiveRef(key);
        lifecycleCoreService.createCandidateRef(key, candidate.getMasterId(), candidate.getId(),
                candidate.getVersionNo(), candidate.getStatus(),
                activeRef == null ? null : activeRef.getId(),
                activeRef == null ? null : activeRef.getNativeVersionId(),
                actorId, "dcc form-center upload approved");
        lifecycleCoreService.transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, candidate.getStatus(),
                ControlledContentTransitionAction.SUBMIT, actorId,
                "dcc form-center upload approved", approvalProcessInstanceId);
        lifecycleCoreService.transitionVersionRefByDomainEvent(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                candidate.getStatus(), ControlledContentTransitionAction.APPROVE,
                actorId, "dcc form-center upload approved", approvalProcessInstanceId,
                normalizedEventKey + ":approve");
        lifecycleCoreService.transitionVersionRefByDomainEvent(key, candidate.getId(),
                ControlledContentCanonicalStatus.READY_TO_PUBLISH, ControlledContentCanonicalStatus.FINALIZING,
                candidate.getStatus(), ControlledContentTransitionAction.START_FINALIZATION,
                actorId, "dcc form-center upload finalization started", approvalProcessInstanceId,
                normalizedEventKey + ":start-finalization");
    }

    public void recordApprovedUploadReadyToPublish(DccControlledFileDO candidate, Long actorId,
                                                   String approvalProcessInstanceId, String eventKey) {
        requireFile(candidate);
        String normalizedEventKey = requireEventKey(eventKey, "form-center upload ready to publish");
        ControlledContentKey key = dccKey(candidate);
        ControlledContentVersionRefDO activeRef = lifecycleCoreService.getActiveRef(key);
        lifecycleCoreService.createCandidateRef(key, candidate.getMasterId(), candidate.getId(),
                candidate.getVersionNo(), candidate.getStatus(),
                activeRef == null ? null : activeRef.getId(),
                activeRef == null ? null : activeRef.getNativeVersionId(),
                actorId, "dcc form-center upload approved");
        lifecycleCoreService.transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, candidate.getStatus(),
                ControlledContentTransitionAction.SUBMIT, actorId,
                "dcc form-center upload approved", approvalProcessInstanceId);
        lifecycleCoreService.transitionVersionRefByDomainEvent(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                candidate.getStatus(), ControlledContentTransitionAction.APPROVE,
                actorId, "dcc form-center upload approved and waiting for publish", approvalProcessInstanceId,
                normalizedEventKey + ":approve");
    }

    public void recordFinalizationFailed(DccControlledFileDO file, Long actorId, String reason, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.FINALIZING, ControlledContentCanonicalStatus.FINALIZATION_FAILED,
                DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(),
                ControlledContentTransitionAction.FINALIZE_FAILED, actorId,
                StrUtil.blankToDefault(reason, "dcc controlled file finalization failed"),
                file.getProcessInstanceId(),
                requireEventKey(eventKey, "finalization") + ":finalize-failed");
    }

    public void recordFinalizationRetried(DccControlledFileDO file, Long actorId, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.FINALIZATION_FAILED, ControlledContentCanonicalStatus.FINALIZING,
                DccControlledFileStatusEnum.FINALIZING.getStatus(),
                ControlledContentTransitionAction.RETRY_FINALIZATION, actorId,
                "dcc controlled file finalization retried", file.getProcessInstanceId(),
                requireEventKey(eventKey, "finalization") + ":retry-finalization");
    }

    public void recordFinalized(DccControlledFileDO previousActive, DccControlledFileDO candidate,
                                Long actorId, String eventKey) {
        requireFile(candidate);
        String normalizedEventKey = requireEventKey(eventKey, "finalization");
        if (previousActive != null && previousActive.getId() != null
                && !previousActive.getId().equals(candidate.getId())) {
            lifecycleCoreService.finalizeVersionRefs(dccKey(candidate), previousActive.getId(), candidate.getId(),
                    DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
                    DccControlledFileStatusEnum.ACTIVE.getStatus(), actorId,
                    "dcc controlled file finalization succeeded", normalizedEventKey + ":finalize-success");
            return;
        }
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(candidate), candidate.getId(),
                ControlledContentCanonicalStatus.FINALIZING, ControlledContentCanonicalStatus.ACTIVE,
                DccControlledFileStatusEnum.ACTIVE.getStatus(), ControlledContentTransitionAction.FINALIZE_SUCCESS,
                actorId, "dcc controlled file finalization succeeded", candidate.getProcessInstanceId(),
                normalizedEventKey + ":finalize-success");
    }

    public void recordObsoleted(DccControlledFileDO file, Long actorId, String reason, String eventKey) {
        requireFile(file);
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(file), file.getId(),
                ControlledContentCanonicalStatus.ACTIVE, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.OBSOLETE_ACTIVE,
                actorId, StrUtil.blankToDefault(reason, "dcc controlled file obsoleted"), null,
                requireEventKey(eventKey, "obsolete"));
    }

    public void recordWorkflowObsoleted(DccControlledFileDO activeFile, DccControlledFileDO obsoleteCandidate,
                                        Long actorId, String reason, String eventKey) {
        requireFile(activeFile);
        requireFile(obsoleteCandidate);
        String normalizedEventKey = requireEventKey(eventKey, "obsolete-finalization");
        String normalizedReason = StrUtil.blankToDefault(reason, "dcc controlled file obsoleted");
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(obsoleteCandidate), activeFile.getId(),
                ControlledContentCanonicalStatus.ACTIVE, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.OBSOLETE_ACTIVE,
                actorId, normalizedReason, null, normalizedEventKey + ":active-obsolete");
        lifecycleCoreService.transitionVersionRefByDomainEvent(dccKey(obsoleteCandidate), obsoleteCandidate.getId(),
                ControlledContentCanonicalStatus.FINALIZING, ControlledContentCanonicalStatus.OBSOLETE,
                DccControlledFileStatusEnum.OBSOLETE.getStatus(), ControlledContentTransitionAction.FINALIZE_SUCCESS,
                actorId, normalizedReason, obsoleteCandidate.getProcessInstanceId(),
                normalizedEventKey + ":candidate-obsolete");
    }

    private ControlledContentKey dccKey(DccControlledFileDO file) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenant is required for DCC controlled content lifecycle");
        }
        if (file.getMasterId() == null) {
            throw new IllegalArgumentException("masterId is required for DCC controlled content lifecycle");
        }
        return ControlledContentKey.of(tenantId, ControlledContentType.DCC_CONTROLLED_FILE,
                String.valueOf(file.getMasterId()));
    }

    private void requireFile(DccControlledFileDO file) {
        if (file == null || file.getId() == null) {
            throw new IllegalArgumentException("DCC controlled file is required");
        }
    }

    private String requireEventKey(String eventKey, String eventType) {
        String normalized = StrUtil.trim(eventKey);
        if (StrUtil.isBlank(normalized)) {
            throw new IllegalArgumentException("DCC " + eventType + " event key must not be blank");
        }
        return normalized;
    }

}
