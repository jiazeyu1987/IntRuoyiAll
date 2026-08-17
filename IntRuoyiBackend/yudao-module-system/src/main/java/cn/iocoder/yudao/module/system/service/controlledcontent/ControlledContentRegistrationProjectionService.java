package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
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
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.SUPERSEDED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_READY_CANDIDATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUPERSEDE_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

/**
 * Protected registration projection mutations. A domain adapter must provide both snapshots from formal facts.
 */
@Service
public class ControlledContentRegistrationProjectionService {

    private final ControlledContentVersionRefMapper versionRefMapper;
    private final ControlledContentTransitionAuditMapper transitionAuditMapper;
    private final ControlledContentStateMachine stateMachine;

    public ControlledContentRegistrationProjectionService(ControlledContentVersionRefMapper versionRefMapper,
                                                          ControlledContentTransitionAuditMapper transitionAuditMapper,
                                                          ControlledContentStateMachine stateMachine) {
        if (versionRefMapper == null || transitionAuditMapper == null || stateMachine == null) {
            throw new IllegalArgumentException("registration projection dependencies must not be null");
        }
        this.versionRefMapper = versionRefMapper;
        this.transitionAuditMapper = transitionAuditMapper;
        this.stateMachine = stateMachine;
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO registerActive(ControlledContentKey key,
                                                        ControlledContentProjectionSnapshot platformBefore,
                                                        ControlledContentProjectionSnapshot domainAfter,
                                                        Long nativeMasterId, Long nativeVersionId, String versionNo,
                                                        String domainStatus, Long actorId, String reason) {
        requireRegistrationKey(key);
        validateSnapshots(key, platformBefore, domainAfter);
        requireVersionInput(nativeMasterId, nativeVersionId, versionNo);
        validateRegisterActiveDelta(platformBefore, domainAfter, nativeVersionId);
        validateSupportedAction(REGISTER_ACTIVE);
        stateMachine.validateTransition(null, ACTIVE, REGISTER_ACTIVE);

        PlatformProjection actualBefore = readPlatformProjection(key);
        validateExpectedProjection("platformBefore", platformBefore, actualBefore.snapshot());

        LocalDateTime transitionTime = LocalDateTime.now();
        ControlledContentVersionRefDO ref = ControlledContentVersionRefDO.builder()
                .tenantId(key.getTenantId())
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .nativeMasterId(nativeMasterId)
                .nativeVersionId(nativeVersionId)
                .versionNo(versionNo.trim())
                .canonicalStatus(ACTIVE.name())
                .domainStatus(domainStatus)
                .activeUniqueFlag(1)
                .openCandidateUniqueFlag(null)
                .lastTransitionTime(transitionTime)
                .build();
        requireSingleWrite("register active ref", versionRefMapper.insert(ref));
        insertAudit(key, ref.getId(), null, ACTIVE, null, domainStatus, REGISTER_ACTIVE,
                actorId, reason, transitionTime);

        validateExpectedProjection("domainAfter", domainAfter, readPlatformProjection(key).snapshot());
        return ref;
    }

    @Transactional(rollbackFor = Exception.class)
    public ControlledContentVersionRefDO registerReadyCandidate(ControlledContentKey key,
                                                                ControlledContentProjectionSnapshot platformBefore,
                                                                ControlledContentProjectionSnapshot domainAfter,
                                                                Long nativeMasterId, Long nativeVersionId,
                                                                String versionNo, String domainStatus,
                                                                Long actorId, String reason) {
        requireRegistrationKey(key);
        validateSnapshots(key, platformBefore, domainAfter);
        requireVersionInput(nativeMasterId, nativeVersionId, versionNo);
        validateReadyCandidateDelta(platformBefore, domainAfter, nativeVersionId);
        validateSupportedAction(REGISTER_READY_CANDIDATE);
        stateMachine.validateTransition(null, READY_TO_PUBLISH, REGISTER_READY_CANDIDATE);

        PlatformProjection actualBefore = readPlatformProjection(key);
        validateExpectedProjection("platformBefore", platformBefore, actualBefore.snapshot());
        if (platformBefore.isEmpty() && actualBefore.refCount() != 0) {
            throw drift(key, "initial ready candidate requires a genuinely empty platform ref history");
        }
        ControlledContentVersionRefDO activeRef = actualBefore.activeRef();

        LocalDateTime transitionTime = LocalDateTime.now();
        ControlledContentVersionRefDO ref = ControlledContentVersionRefDO.builder()
                .tenantId(key.getTenantId())
                .contentType(key.getContentType().name())
                .contentKey(key.getContentKey())
                .nativeMasterId(nativeMasterId)
                .nativeVersionId(nativeVersionId)
                .versionNo(versionNo.trim())
                .canonicalStatus(READY_TO_PUBLISH.name())
                .domainStatus(domainStatus)
                .sourceVersionRefId(activeRef == null ? null : activeRef.getId())
                .sourceNativeVersionId(activeRef == null ? null : activeRef.getNativeVersionId())
                .activeUniqueFlag(null)
                .openCandidateUniqueFlag(1)
                .lastTransitionTime(transitionTime)
                .build();
        requireSingleWrite("register ready candidate ref", versionRefMapper.insert(ref));
        insertAudit(key, ref.getId(), null, READY_TO_PUBLISH, null, domainStatus,
                REGISTER_READY_CANDIDATE, actorId, reason, transitionTime);

        validateExpectedProjection("domainAfter", domainAfter, readPlatformProjection(key).snapshot());
        return ref;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(ControlledContentKey key, ControlledContentProjectionSnapshot platformBefore,
                        ControlledContentProjectionSnapshot domainAfter,
                        String activeDomainToStatus, String candidateDomainToStatus,
                        Long actorId, String reason) {
        requireRegistrationKey(key);
        validateSnapshots(key, platformBefore, domainAfter);
        validatePublishDelta(platformBefore, domainAfter);
        validateSupportedAction(SUPERSEDE_ACTIVE);
        validateSupportedAction(PUBLISH);

        PlatformProjection actualBefore = readPlatformProjection(key);
        validateExpectedProjection("platformBefore", platformBefore, actualBefore.snapshot());
        ControlledContentVersionRefDO activeRef = actualBefore.activeRef();
        ControlledContentVersionRefDO candidateRef = actualBefore.candidateRef();
        if (activeRef == null) {
            validateFirstPublicationCandidate(key, actualBefore);
        }
        ControlledContentCanonicalStatus activeFrom = activeRef == null ? null : parseStatus(activeRef);
        ControlledContentCanonicalStatus candidateFrom = parseStatus(candidateRef);
        if (activeRef != null) {
            stateMachine.validateTransition(activeFrom, SUPERSEDED, SUPERSEDE_ACTIVE);
        }
        stateMachine.validateTransition(candidateFrom, ACTIVE, PUBLISH);

        LocalDateTime transitionTime = LocalDateTime.now();
        if (activeRef != null) {
            requireSingleWrite("supersede registration active ref", versionRefMapper.update(null,
                    new UpdateWrapper<ControlledContentVersionRefDO>()
                            .eq("id", activeRef.getId())
                            .set("canonical_status", SUPERSEDED.name())
                            .set("domain_status", activeDomainToStatus)
                            .set("successor_version_ref_id", candidateRef.getId())
                            .set("successor_native_version_id", candidateRef.getNativeVersionId())
                            .set("active_unique_flag", null)
                            .set("open_candidate_unique_flag", null)
                            .set("last_transition_time", transitionTime)));
            insertAudit(key, activeRef.getId(), activeFrom, SUPERSEDED, activeRef.getDomainStatus(),
                    activeDomainToStatus, SUPERSEDE_ACTIVE, actorId, reason, transitionTime);
        }

        requireSingleWrite("activate registration candidate ref", versionRefMapper.update(null,
                new UpdateWrapper<ControlledContentVersionRefDO>()
                        .eq("id", candidateRef.getId())
                        .set("canonical_status", ACTIVE.name())
                        .set("domain_status", candidateDomainToStatus)
                        .set("active_unique_flag", 1)
                        .set("open_candidate_unique_flag", null)
                        .set("last_transition_time", transitionTime)));
        insertAudit(key, candidateRef.getId(), candidateFrom, ACTIVE, candidateRef.getDomainStatus(),
                candidateDomainToStatus, PUBLISH, actorId, reason, transitionTime);

        validateExpectedProjection("domainAfter", domainAfter, readPlatformProjection(key).snapshot());
    }

    private PlatformProjection readPlatformProjection(ControlledContentKey key) {
        List<ControlledContentVersionRefDO> refs = versionRefMapper.selectList(
                new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                        .eq(ControlledContentVersionRefDO::getTenantId, key.getTenantId())
                        .eq(ControlledContentVersionRefDO::getContentType, key.getContentType().name())
                        .eq(ControlledContentVersionRefDO::getContentKey, key.getContentKey())
                        .orderByAsc(ControlledContentVersionRefDO::getId)
                        .last("FOR UPDATE"));
        if (refs == null) {
            throw drift(key, "platform ref query returned null");
        }
        List<ControlledContentVersionRefDO> activeRefs = refs.stream()
                .filter(ref -> Objects.equals(ref.getActiveUniqueFlag(), 1)).toList();
        List<ControlledContentVersionRefDO> candidateRefs = refs.stream()
                .filter(ref -> Objects.equals(ref.getOpenCandidateUniqueFlag(), 1)).toList();
        if (activeRefs.size() > 1) {
            throw drift(key, "active ref count expected at most 1 but was " + activeRefs.size());
        }
        if (candidateRefs.size() > 1) {
            throw drift(key, "open candidate ref count expected at most 1 but was " + candidateRefs.size());
        }
        for (ControlledContentVersionRefDO ref : refs) {
            validateRefAndAudit(key, ref);
        }
        if (refs.isEmpty()) {
            Long auditCount = transitionAuditMapper.selectCount(
                    new LambdaQueryWrapperX<ControlledContentTransitionAuditDO>()
                            .eq(ControlledContentTransitionAuditDO::getTenantId, key.getTenantId())
                            .eq(ControlledContentTransitionAuditDO::getContentType, key.getContentType().name())
                            .eq(ControlledContentTransitionAuditDO::getContentKey, key.getContentKey()));
            if (!Objects.equals(auditCount, 0L)) {
                throw drift(key, "empty projection has " + auditCount + " transition audits");
            }
        }
        ControlledContentVersionRefDO activeRef = activeRefs.isEmpty() ? null : activeRefs.get(0);
        ControlledContentVersionRefDO candidateRef = candidateRefs.isEmpty() ? null : candidateRefs.get(0);
        return new PlatformProjection(ControlledContentProjectionSnapshot.of(key,
                activeRef == null ? null : activeRef.getNativeVersionId(),
                candidateRef == null ? null : candidateRef.getNativeVersionId()), activeRef, candidateRef, refs.size());
    }

    private void validateFirstPublicationCandidate(ControlledContentKey key, PlatformProjection projection) {
        ControlledContentVersionRefDO candidateRef = projection.candidateRef();
        if (projection.refCount() != 1 || candidateRef == null
                || candidateRef.getSourceVersionRefId() != null
                || candidateRef.getSourceNativeVersionId() != null) {
            throw drift(key, "first publication requires exactly one candidate ref without a source predecessor");
        }
    }

    private void validateRefAndAudit(ControlledContentKey key, ControlledContentVersionRefDO ref) {
        if (ref == null || ref.getId() == null || ref.getNativeVersionId() == null) {
            throw drift(key, "platform ref identity is incomplete");
        }
        if (!Objects.equals(ref.getTenantId(), key.getTenantId())
                || !Objects.equals(ref.getContentType(), key.getContentType().name())
                || !Objects.equals(ref.getContentKey(), key.getContentKey())) {
            throw drift(key, "version ref " + ref.getId() + " belongs to another controlled content");
        }
        if (!isFlag(ref.getActiveUniqueFlag()) || !isFlag(ref.getOpenCandidateUniqueFlag())
                || Objects.equals(ref.getActiveUniqueFlag(), 1)
                && Objects.equals(ref.getOpenCandidateUniqueFlag(), 1)) {
            throw drift(key, "version ref " + ref.getId() + " has contradictory unique flags");
        }
        ControlledContentCanonicalStatus status = parseStatus(ref);
        if ((status == ACTIVE) != Objects.equals(ref.getActiveUniqueFlag(), 1)) {
            throw drift(key, "version ref " + ref.getId() + " active status/flag drifted");
        }
        if ((status == READY_TO_PUBLISH) != Objects.equals(ref.getOpenCandidateUniqueFlag(), 1)) {
            throw drift(key, "version ref " + ref.getId() + " candidate status/flag drifted");
        }
        ControlledContentTransitionAuditDO latestAudit = transitionAuditMapper.selectOne(
                new LambdaQueryWrapperX<ControlledContentTransitionAuditDO>()
                        .eq(ControlledContentTransitionAuditDO::getVersionRefId, ref.getId())
                        .eq(ControlledContentTransitionAuditDO::getTenantId, key.getTenantId())
                        .eq(ControlledContentTransitionAuditDO::getContentType, key.getContentType().name())
                        .eq(ControlledContentTransitionAuditDO::getContentKey, key.getContentKey())
                        .orderByDesc(ControlledContentTransitionAuditDO::getCreateTime)
                        .orderByDesc(ControlledContentTransitionAuditDO::getId)
                        .last("LIMIT 1"));
        if (latestAudit == null) {
            throw drift(key, "version ref " + ref.getId() + " has no transition audit");
        }
        if (!Objects.equals(latestAudit.getVersionRefId(), ref.getId())
                || !Objects.equals(latestAudit.getTenantId(), key.getTenantId())
                || !Objects.equals(latestAudit.getContentType(), key.getContentType().name())
                || !Objects.equals(latestAudit.getContentKey(), key.getContentKey())) {
            throw drift(key, "version ref " + ref.getId() + " latest audit belongs to another projection");
        }
        if (!Objects.equals(latestAudit.getToStatus(), ref.getCanonicalStatus())) {
            throw drift(key, "version ref " + ref.getId() + " latest audit status "
                    + latestAudit.getToStatus() + " does not match " + ref.getCanonicalStatus());
        }
    }

    private boolean isFlag(Integer value) {
        return value == null || value == 1;
    }

    private ControlledContentCanonicalStatus parseStatus(ControlledContentVersionRefDO ref) {
        try {
            return ControlledContentCanonicalStatus.valueOf(ref.getCanonicalStatus());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("registration projection drift: version ref " + ref.getId()
                    + " has invalid canonical status " + ref.getCanonicalStatus(), exception);
        }
    }

    private void validateSnapshots(ControlledContentKey key, ControlledContentProjectionSnapshot platformBefore,
                                   ControlledContentProjectionSnapshot domainAfter) {
        if (platformBefore == null) {
            throw new IllegalArgumentException("platformBefore must not be null");
        }
        if (domainAfter == null) {
            throw new IllegalArgumentException("domainAfter must not be null");
        }
        validateSnapshotOwner("platformBefore", key, platformBefore);
        validateSnapshotOwner("domainAfter", key, domainAfter);
    }

    private void validateSnapshotOwner(String name, ControlledContentKey key,
                                       ControlledContentProjectionSnapshot snapshot) {
        if (!Objects.equals(snapshot.tenantId(), key.getTenantId())) {
            throw new IllegalArgumentException(name + " tenantId mismatch: expected " + key.getTenantId()
                    + " but was " + snapshot.tenantId());
        }
        if (snapshot.contentType() != key.getContentType()) {
            throw new IllegalArgumentException(name + " contentType mismatch: expected " + key.getContentType()
                    + " but was " + snapshot.contentType());
        }
        if (!Objects.equals(snapshot.contentKey(), key.getContentKey())) {
            throw new IllegalArgumentException(name + " contentKey mismatch: expected " + key.getContentKey()
                    + " but was " + snapshot.contentKey());
        }
    }

    private void validateRegisterActiveDelta(ControlledContentProjectionSnapshot before,
                                             ControlledContentProjectionSnapshot after,
                                             Long nativeVersionId) {
        if (!before.isEmpty() || !Objects.equals(after.activeNativeVersionId(), nativeVersionId)
                || after.openCandidateNativeVersionId() != null) {
            throw new IllegalArgumentException("invalid REGISTER_ACTIVE delta: expected empty -> active");
        }
    }

    private void validateReadyCandidateDelta(ControlledContentProjectionSnapshot before,
                                             ControlledContentProjectionSnapshot after,
                                             Long nativeVersionId) {
        if (before.openCandidateNativeVersionId() != null
                || !Objects.equals(after.activeNativeVersionId(), before.activeNativeVersionId())
                || !Objects.equals(after.openCandidateNativeVersionId(), nativeVersionId)
                || Objects.equals(nativeVersionId, before.activeNativeVersionId())) {
            throw new IllegalArgumentException(
                    "invalid REGISTER_READY_CANDIDATE delta: expected empty or active -> same active + ready candidate");
        }
    }

    private void validatePublishDelta(ControlledContentProjectionSnapshot before,
                                      ControlledContentProjectionSnapshot after) {
        if (before.openCandidateNativeVersionId() == null
                || Objects.equals(before.activeNativeVersionId(), before.openCandidateNativeVersionId())
                || !Objects.equals(after.activeNativeVersionId(), before.openCandidateNativeVersionId())
                || after.openCandidateNativeVersionId() != null) {
            throw new IllegalArgumentException(
                    "invalid PUBLISH delta: expected ready candidate with optional active predecessor -> candidate active");
        }
    }

    private void validateExpectedProjection(String boundary, ControlledContentProjectionSnapshot expected,
                                            ControlledContentProjectionSnapshot actual) {
        if (!Objects.equals(expected.activeNativeVersionId(), actual.activeNativeVersionId())) {
            throw new IllegalStateException("registration projection drift at " + boundary
                    + ": active native version ID expected " + expected.activeNativeVersionId()
                    + " but was " + actual.activeNativeVersionId());
        }
        if (!Objects.equals(expected.openCandidateNativeVersionId(), actual.openCandidateNativeVersionId())) {
            throw new IllegalStateException("registration projection drift at " + boundary
                    + ": open candidate native version ID expected " + expected.openCandidateNativeVersionId()
                    + " but was " + actual.openCandidateNativeVersionId());
        }
    }

    private void requireRegistrationKey(ControlledContentKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (key.getContentType() != DCC_REGISTRATION_CERTIFICATE) {
            throw new IllegalArgumentException("registration projection service requires DCC_REGISTRATION_CERTIFICATE");
        }
    }

    private void requireVersionInput(Long nativeMasterId, Long nativeVersionId, String versionNo) {
        if (nativeMasterId == null || nativeVersionId == null) {
            throw new IllegalArgumentException("native master/version IDs must not be null");
        }
        if (versionNo == null || versionNo.trim().isEmpty()) {
            throw new IllegalArgumentException("versionNo must not be blank");
        }
    }

    private void validateSupportedAction(ControlledContentTransitionAction action) {
        if (!ControlledContentTransitionProfile.requiredFor(DCC_REGISTRATION_CERTIFICATE).supports(action)) {
            throw new IllegalStateException("unsupported registration controlled content action: " + action);
        }
    }

    private void insertAudit(ControlledContentKey key, Long versionRefId,
                             ControlledContentCanonicalStatus fromStatus,
                             ControlledContentCanonicalStatus toStatus,
                             String domainFromStatus, String domainToStatus,
                             ControlledContentTransitionAction action,
                             Long actorId, String reason, LocalDateTime transitionTime) {
        if (versionRefId == null) {
            throw new IllegalStateException("registration version ref ID is missing after insert");
        }
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
                .actorId(actorId)
                .reason(reason)
                .createTime(transitionTime)
                .build();
        requireSingleWrite("registration transition audit", transitionAuditMapper.insert(audit));
    }

    private void requireSingleWrite(String operation, int affectedRows) {
        if (affectedRows != 1) {
            throw new IllegalStateException(operation + " expected one affected row but was " + affectedRows);
        }
    }

    private IllegalStateException drift(ControlledContentKey key, String detail) {
        return new IllegalStateException("registration projection drift for tenant " + key.getTenantId()
                + ", type " + key.getContentType() + ", key " + key.getContentKey() + ": " + detail);
    }

    private record PlatformProjection(ControlledContentProjectionSnapshot snapshot,
                                      ControlledContentVersionRefDO activeRef,
                                      ControlledContentVersionRefDO candidateRef,
                                      int refCount) {
    }

}
