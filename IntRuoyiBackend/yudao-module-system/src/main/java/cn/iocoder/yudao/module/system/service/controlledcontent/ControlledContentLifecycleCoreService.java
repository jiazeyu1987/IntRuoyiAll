package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.DRAFT;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.SUPERSEDED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.CREATE_CANDIDATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUPERSEDE_ACTIVE;

/**
 * Minimal lifecycle core for platform controlled content refs and transition audit.
 */
@Service
public class ControlledContentLifecycleCoreService {

    private final ControlledContentVersionRefMapper versionRefMapper;
    private final ControlledContentTransitionAuditMapper transitionAuditMapper;
    private final ControlledContentStateMachine stateMachine;

    public ControlledContentLifecycleCoreService(ControlledContentVersionRefMapper versionRefMapper,
                                                 ControlledContentTransitionAuditMapper transitionAuditMapper,
                                                 ControlledContentStateMachine stateMachine) {
        if (versionRefMapper == null) {
            throw new IllegalArgumentException("versionRefMapper must not be null");
        }
        if (transitionAuditMapper == null) {
            throw new IllegalArgumentException("transitionAuditMapper must not be null");
        }
        if (stateMachine == null) {
            throw new IllegalArgumentException("stateMachine must not be null");
        }
        this.versionRefMapper = versionRefMapper;
        this.transitionAuditMapper = transitionAuditMapper;
        this.stateMachine = stateMachine;
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO registerActiveRef(ControlledContentKey key, Long nativeMasterId,
                                                           Long nativeVersionId, String versionNo,
                                                           String domainStatus, Long actorId, String reason) {
        requireKey(key);
        validateSupportedAction(key, REGISTER_ACTIVE);
        ControlledContentVersionRefDO existingActiveRef = versionRefMapper.selectActive(key.getTenantId(),
                key.getContentType().name(), key.getContentKey());
        if (existingActiveRef != null) {
            throw new IllegalStateException("controlled content already has an active ref: "
                    + existingActiveRef.getVersionNo() + "/" + existingActiveRef.getCanonicalStatus());
        }
        stateMachine.validateTransition(null, ACTIVE, REGISTER_ACTIVE);

        LocalDateTime transitionTime = LocalDateTime.now();
        ControlledContentVersionRefDO ref = ControlledContentVersionRefDO.builder()
                .tenantId(key.getTenantId())
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .nativeMasterId(nativeMasterId)
                .nativeVersionId(nativeVersionId)
                .versionNo(versionNo)
                .canonicalStatus(ACTIVE.name())
                .domainStatus(domainStatus)
                .sourceVersionRefId(null)
                .sourceNativeVersionId(null)
                .openCandidateUniqueFlag(null)
                .activeUniqueFlag(1)
                .lastTransitionTime(transitionTime)
                .build();
        versionRefMapper.insert(ref);

        insertAudit(key, ref.getId(), null, ACTIVE, null, domainStatus, REGISTER_ACTIVE,
                actorId, reason, null, transitionTime);
        return ref;
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO createCandidateRef(ControlledContentKey key, Long nativeMasterId,
                                                           Long nativeVersionId, String versionNo,
                                                           String domainStatus, Long sourceVersionRefId,
                                                           Long sourceNativeVersionId, Long actorId, String reason) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        validateSupportedAction(key, CREATE_CANDIDATE);
        ControlledContentVersionRefDO existingOpenCandidate = versionRefMapper.selectOpenCandidate(key.getTenantId(),
                key.getContentType().name(), key.getContentKey());
        if (existingOpenCandidate != null) {
            throw new IllegalStateException("controlled content already has an open candidate: "
                    + existingOpenCandidate.getVersionNo() + "/" + existingOpenCandidate.getCanonicalStatus());
        }
        validateSourceActiveRef(key, sourceVersionRefId, sourceNativeVersionId);

        LocalDateTime transitionTime = LocalDateTime.now();
        ControlledContentVersionRefDO ref = ControlledContentVersionRefDO.builder()
                .tenantId(key.getTenantId())
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .nativeMasterId(nativeMasterId)
                .nativeVersionId(nativeVersionId)
                .versionNo(versionNo)
                .canonicalStatus(DRAFT.name())
                .domainStatus(domainStatus)
                .sourceVersionRefId(sourceVersionRefId)
                .sourceNativeVersionId(sourceNativeVersionId)
                .openCandidateUniqueFlag(stateMachine.isOpenCandidate(DRAFT) ? 1 : null)
                .activeUniqueFlag(null)
                .lastTransitionTime(transitionTime)
                .build();
        versionRefMapper.insert(ref);

        ControlledContentTransitionAuditDO audit = ControlledContentTransitionAuditDO.builder()
                .tenantId(key.getTenantId())
                .versionRefId(ref.getId())
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .fromStatus(null)
                .toStatus(DRAFT.name())
                .domainFromStatus(null)
                .domainToStatus(domainStatus)
                .action(CREATE_CANDIDATE.name())
                .actorId(actorId)
                .reason(reason)
                .createTime(transitionTime)
                .build();
        transitionAuditMapper.insert(audit);
        return ref;
    }

    private void validateSourceActiveRef(ControlledContentKey key, Long sourceVersionRefId,
                                         Long sourceNativeVersionId) {
        if (sourceVersionRefId == null && sourceNativeVersionId == null) {
            return;
        }
        if (sourceVersionRefId == null || sourceNativeVersionId == null) {
            throw new IllegalArgumentException("controlled content source ref and source native version must be provided together");
        }
        ControlledContentVersionRefDO sourceRef = versionRefMapper.selectById(sourceVersionRefId);
        if (sourceRef == null) {
            throw new IllegalStateException("controlled content source ref does not exist");
        }
        if (!Objects.equals(sourceRef.getTenantId(), key.getTenantId())
                || !Objects.equals(sourceRef.getContentType(), key.getContentType().name())
                || !Objects.equals(sourceRef.getContentKey(), key.getContentKey())) {
            throw new IllegalStateException("controlled content source ref belongs to another content");
        }
        if (!Objects.equals(sourceRef.getNativeVersionId(), sourceNativeVersionId)) {
            throw new IllegalStateException("controlled content source ref native version drifted");
        }
        if (parseStatus(sourceRef.getCanonicalStatus()) != ACTIVE) {
            throw new IllegalStateException("controlled content source ref is not active");
        }
    }

    @Transactional(readOnly = true)
    public ControlledContentVersionRefDO getActiveRef(ControlledContentKey key) {
        requireKey(key);
        return versionRefMapper.selectActive(key.getTenantId(), key.getContentType().name(), key.getContentKey());
    }

    @Transactional(readOnly = true)
    public ControlledContentVersionRefDO getVersionRef(ControlledContentKey key, Long nativeVersionId) {
        requireKey(key);
        if (nativeVersionId == null) {
            throw new IllegalArgumentException("nativeVersionId must not be null");
        }
        return versionRefMapper.selectByNativeVersion(key.getTenantId(), key.getContentType().name(),
                key.getContentKey(), nativeVersionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO transitionVersionRef(ControlledContentKey key, Long nativeVersionId,
                                                             ControlledContentCanonicalStatus toStatus,
                                                              String domainToStatus,
                                                              ControlledContentTransitionAction action,
                                                              Long actorId, String reason,
                                                              String approvalProcessInstanceId) {
        requireKey(key);
        ControlledContentVersionRefDO ref = requireRefByNativeVersion(key, nativeVersionId);
        return transitionVersionRef(key, ref, toStatus, domainToStatus, action, actorId, reason,
                approvalProcessInstanceId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO transitionVersionRefByDomainEvent(ControlledContentKey key,
                                                                           Long nativeVersionId,
                                                                           ControlledContentCanonicalStatus expectedFromStatus,
                                                                           ControlledContentCanonicalStatus toStatus,
                                                                            String domainToStatus,
                                                                            ControlledContentTransitionAction action,
                                                                            Long actorId, String reason,
                                                                            String approvalProcessInstanceId,
                                                                            String eventKey) {
        requireKey(key);
        String normalizedEventKey = requireEventKey(eventKey);
        validateSupportedAction(key, action);
        ControlledContentVersionRefDO ref = requireRefByNativeVersion(key, nativeVersionId);
        ControlledContentTransitionAuditDO existingAudit =
                transitionAuditMapper.selectByVersionRefIdAndActionAndEventKey(ref.getId(), action.name(),
                        normalizedEventKey);
        if (existingAudit != null) {
            return ref;
        }
        if (approvalProcessInstanceId != null
                && !Objects.equals(approvalProcessInstanceId, ref.getApprovalProcessInstanceId())) {
            throw new IllegalStateException("stale controlled content domain event: approval process drifted");
        }
        ControlledContentCanonicalStatus actualFromStatus = parseStatus(ref.getCanonicalStatus());
        if (expectedFromStatus != null && actualFromStatus != expectedFromStatus) {
            throw new IllegalStateException("stale controlled content domain event: expected "
                    + expectedFromStatus + " but was " + actualFromStatus);
        }
        return transitionVersionRef(key, ref, toStatus, domainToStatus, action, actorId, reason,
                approvalProcessInstanceId, normalizedEventKey);
    }

    private ControlledContentVersionRefDO transitionVersionRef(ControlledContentKey key, ControlledContentVersionRefDO ref,
                                                             ControlledContentCanonicalStatus toStatus,
                                                             String domainToStatus,
                                                             ControlledContentTransitionAction action,
                                                             Long actorId, String reason,
                                                             String approvalProcessInstanceId,
                                                             String eventKey) {
        validateSupportedAction(key, action);
        ControlledContentCanonicalStatus fromStatus = parseStatus(ref.getCanonicalStatus());
        stateMachine.validateTransition(fromStatus, toStatus, action);

        LocalDateTime transitionTime = LocalDateTime.now();
        Integer openCandidateUniqueFlag = stateMachine.isOpenCandidate(toStatus) ? 1 : null;
        Integer activeUniqueFlag = stateMachine.isActive(toStatus) ? 1 : null;
        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", ref.getId())
                .set("canonical_status", toStatus.name())
                .set("domain_status", domainToStatus)
                .set("approval_process_instance_id", approvalProcessInstanceId)
                .set("open_candidate_unique_flag", openCandidateUniqueFlag)
                .set("active_unique_flag", activeUniqueFlag)
                .set("last_transition_time", transitionTime));

        insertAudit(key, ref.getId(), fromStatus, toStatus, ref.getDomainStatus(), domainToStatus, action,
                actorId, reason, eventKey, transitionTime);

        ref.setCanonicalStatus(toStatus.name());
        ref.setDomainStatus(domainToStatus);
        ref.setApprovalProcessInstanceId(approvalProcessInstanceId);
        ref.setOpenCandidateUniqueFlag(openCandidateUniqueFlag);
        ref.setActiveUniqueFlag(activeUniqueFlag);
        ref.setLastTransitionTime(transitionTime);
        return ref;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishVersionRefs(ControlledContentKey key, Long activeNativeVersionId, Long candidateNativeVersionId,
                                   String activeDomainToStatus, String candidateDomainToStatus, Long actorId,
                                   String reason) {
        requireKey(key);
        validateSupportedAction(key, SUPERSEDE_ACTIVE);
        validateSupportedAction(key, PUBLISH);
        ControlledContentVersionRefDO activeRef = requireRefByNativeVersion(key, activeNativeVersionId);
        ControlledContentVersionRefDO candidateRef = requireRefByNativeVersion(key, candidateNativeVersionId);
        ControlledContentCanonicalStatus activeFromStatus = parseStatus(activeRef.getCanonicalStatus());
        ControlledContentCanonicalStatus candidateFromStatus = parseStatus(candidateRef.getCanonicalStatus());
        stateMachine.validateTransition(activeFromStatus, SUPERSEDED, SUPERSEDE_ACTIVE);
        stateMachine.validateTransition(candidateFromStatus, ACTIVE, PUBLISH);

        LocalDateTime transitionTime = LocalDateTime.now();
        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", activeRef.getId())
                .set("canonical_status", SUPERSEDED.name())
                .set("domain_status", activeDomainToStatus)
                .set("successor_version_ref_id", candidateRef.getId())
                .set("successor_native_version_id", candidateNativeVersionId)
                .set("active_unique_flag", null)
                .set("open_candidate_unique_flag", null)
                .set("last_transition_time", transitionTime));
        insertAudit(key, activeRef.getId(), activeFromStatus, SUPERSEDED, activeRef.getDomainStatus(),
                activeDomainToStatus, SUPERSEDE_ACTIVE, actorId, reason, null, transitionTime);

        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", candidateRef.getId())
                .set("canonical_status", ACTIVE.name())
                .set("domain_status", candidateDomainToStatus)
                .set("active_unique_flag", 1)
                .set("open_candidate_unique_flag", null)
                .set("last_transition_time", transitionTime));
        insertAudit(key, candidateRef.getId(), candidateFromStatus, ACTIVE, candidateRef.getDomainStatus(),
                candidateDomainToStatus, PUBLISH, actorId, reason, null, transitionTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeVersionRefs(ControlledContentKey key, Long activeNativeVersionId, Long candidateNativeVersionId,
                                    String activeDomainToStatus, String candidateDomainToStatus, Long actorId,
                                    String reason, String eventKey) {
        requireKey(key);
        validateSupportedAction(key, SUPERSEDE_ACTIVE);
        validateSupportedAction(key, ControlledContentTransitionAction.FINALIZE_SUCCESS);
        String normalizedEventKey = requireEventKey(eventKey);
        ControlledContentVersionRefDO activeRef = requireRefByNativeVersion(key, activeNativeVersionId);
        ControlledContentVersionRefDO candidateRef = requireRefByNativeVersion(key, candidateNativeVersionId);
        ControlledContentTransitionAuditDO existingAudit =
                transitionAuditMapper.selectByVersionRefIdAndActionAndEventKey(candidateRef.getId(),
                        ControlledContentTransitionAction.FINALIZE_SUCCESS.name(),
                        normalizedEventKey + ":candidate");
        if (existingAudit != null) {
            return;
        }
        ControlledContentCanonicalStatus activeFromStatus = parseStatus(activeRef.getCanonicalStatus());
        ControlledContentCanonicalStatus candidateFromStatus = parseStatus(candidateRef.getCanonicalStatus());
        stateMachine.validateTransition(activeFromStatus, SUPERSEDED, SUPERSEDE_ACTIVE);
        stateMachine.validateTransition(candidateFromStatus, ACTIVE, ControlledContentTransitionAction.FINALIZE_SUCCESS);

        LocalDateTime transitionTime = LocalDateTime.now();
        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", activeRef.getId())
                .set("canonical_status", SUPERSEDED.name())
                .set("domain_status", activeDomainToStatus)
                .set("successor_version_ref_id", candidateRef.getId())
                .set("successor_native_version_id", candidateNativeVersionId)
                .set("active_unique_flag", null)
                .set("open_candidate_unique_flag", null)
                .set("last_transition_time", transitionTime));
        insertAudit(key, activeRef.getId(), activeFromStatus, SUPERSEDED, activeRef.getDomainStatus(),
                activeDomainToStatus, SUPERSEDE_ACTIVE, actorId, reason, normalizedEventKey + ":active",
                transitionTime);

        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", candidateRef.getId())
                .set("canonical_status", ACTIVE.name())
                .set("domain_status", candidateDomainToStatus)
                .set("active_unique_flag", 1)
                .set("open_candidate_unique_flag", null)
                .set("last_transition_time", transitionTime));
        insertAudit(key, candidateRef.getId(), candidateFromStatus, ACTIVE, candidateRef.getDomainStatus(),
                candidateDomainToStatus, ControlledContentTransitionAction.FINALIZE_SUCCESS, actorId, reason,
                normalizedEventKey + ":candidate", transitionTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public void linkSuccessorRef(ControlledContentKey key, Long nativeVersionId, Long successorNativeVersionId) {
        requireKey(key);
        ControlledContentVersionRefDO ref = requireRefByNativeVersion(key, nativeVersionId);
        ControlledContentVersionRefDO successorRef = requireRefByNativeVersion(key, successorNativeVersionId);
        versionRefMapper.update(null, new UpdateWrapper<ControlledContentVersionRefDO>()
                .eq("id", ref.getId())
                .set("successor_version_ref_id", successorRef.getId())
                .set("successor_native_version_id", successorNativeVersionId));
    }

    private ControlledContentVersionRefDO requireRefByNativeVersion(ControlledContentKey key, Long nativeVersionId) {
        if (nativeVersionId == null) {
            throw new IllegalArgumentException("nativeVersionId must not be null");
        }
        ControlledContentVersionRefDO ref = versionRefMapper.selectByNativeVersion(key.getTenantId(),
                key.getContentType().name(), key.getContentKey(), nativeVersionId);
        if (ref == null) {
            throw new IllegalStateException("controlled content ref does not exist: "
                    + key.getContentType() + "/" + key.getContentKey() + "/" + nativeVersionId);
        }
        return ref;
    }

    private ControlledContentCanonicalStatus parseStatus(String status) {
        if (status == null) {
            throw new IllegalStateException("controlled content canonical status is required");
        }
        return ControlledContentCanonicalStatus.valueOf(status);
    }

    private void requireKey(ControlledContentKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
    }

    private String requireEventKey(String eventKey) {
        if (eventKey == null || eventKey.trim().isEmpty()) {
            throw new IllegalArgumentException("eventKey must not be blank");
        }
        return eventKey.trim();
    }

    private void validateSupportedAction(ControlledContentKey key, ControlledContentTransitionAction action) {
        ControlledContentTransitionProfile profile = ControlledContentTransitionProfile.requiredFor(key.getContentType());
        if (!profile.supports(action)) {
            throw new IllegalStateException("unsupported controlled content action for "
                    + key.getContentType() + ": " + action);
        }
    }

    private void insertAudit(ControlledContentKey key, Long versionRefId, ControlledContentCanonicalStatus fromStatus,
                             ControlledContentCanonicalStatus toStatus, String domainFromStatus,
                             String domainToStatus, ControlledContentTransitionAction action, Long actorId,
                             String reason, String eventKey, LocalDateTime transitionTime) {
        ControlledContentTransitionAuditDO audit = ControlledContentTransitionAuditDO.builder()
                .tenantId(key.getTenantId())
                .versionRefId(versionRefId)
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .fromStatus(fromStatus == null ? null : fromStatus.name())
                .toStatus(toStatus.name())
                .domainFromStatus(domainFromStatus)
                .domainToStatus(domainToStatus)
                .action(action.name())
                .eventKey(eventKey)
                .actorId(actorId)
                .reason(reason)
                .createTime(transitionTime)
                .build();
        transitionAuditMapper.insert(audit);
    }

}
