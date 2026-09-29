package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Service
public class GxpAuditServiceImpl implements GxpAuditService {

    static final String HASH_ALGORITHM = "SHA-256";

    @Resource
    private GxpAuditEventMapper auditEventMapper;
    @Resource
    private GxpAuditLedgerSequenceMapper ledgerSequenceMapper;
    @Resource
    private GxpAuditPolicyOperationMapper policyOperationMapper;
    @Resource
    private GxpAuditPolicyActivationMapper policyActivationMapper;
    @Resource
    private GxpAuditEventRelationMapper eventRelationMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void acquireLedgerLock() {
        Long tenantId = resolveTenantId();
        if (ledgerSequenceMapper.selectByTenantIdForUpdate(tenantId) == null) {
            throw exception(GXP_AUDIT_APPEND_FAILED, "ledger-sequence-watermark-not-initialized");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public GxpAuditAppendResult append(GxpAuditCommand command) {
        validateRequired(command);
        Long tenantId = resolveTenantId();
        GxpAuditLedgerSequenceDO ledger = lockLedgerSequence(tenantId);
        GxpAuditPolicyActivationDO activation = policyActivationMapper.selectLatestForUpdate(tenantId);
        if (activation == null) {
            throw exception(GXP_AUDIT_POLICY_NOT_FOUND, command.getOperationId());
        }
        GxpAuditPolicyOperationDO policy = policyOperationMapper.selectByPolicyVersionForUpdate(
                tenantId, activation.getPolicyVersion(), command.getOperationId());
        if (policy == null || !Boolean.TRUE.equals(policy.getActive())
                || !"GXP".equals(policy.getApplicability())) {
            throw exception(GXP_AUDIT_POLICY_NOT_FOUND, command.getOperationId());
        }
        validatePolicyRequirements(command, policy);

        // Normalize a private copy before hashing; callers may replay the same instance.
        GxpAuditCommand normalized = GxpAuditCommand.builder().build();
        BeanUtils.copyProperties(command, normalized);
        command = normalized;
        LoginUser actor = resolveActor();
        enrichServerOwnedFields(command, actor, tenantId);
        String idempotencyPayloadHash = sha256(canonicalPayload(command, policy));
        GxpAuditEventDO existing = auditEventMapper.selectByIdempotencyKeyForUpdate(tenantId, command.getIdempotencyKey());
        if (existing != null) {
            if (!Objects.equals(existing.getIdempotencyPayloadHash(), idempotencyPayloadHash)) {
                throw exception(GXP_AUDIT_IDEMPOTENCY_CONFLICT, command.getOperationId());
            }
            return new GxpAuditAppendResult(existing.getId(), existing.getLedgerSequence(),
                    existing.getEventHash(), true);
        }

        GxpAuditEventDO previous = auditEventMapper.selectLatestByTenantForUpdate(tenantId);
        long ledgerSequence = allocateLedgerSequence(tenantId, ledger);
        LocalDateTime serverOccurredAt = LocalDateTime.now(ZoneOffset.UTC);

        GxpAuditEventDO event = new GxpAuditEventDO();
        event.setTenantId(tenantId);
        event.setLedgerSequence(ledgerSequence);
        event.setEventSchemaVersion(2);
        event.setCanonicalizationVersion("GXP_CANONICAL_V2");
        event.setOperationId(command.getOperationId());
        event.setDomain(policy.getDomain());
        event.setSubjectType(policy.getSubjectType());
        event.setSubjectId(command.getSubjectId());
        event.setSubjectVersion(command.getSubjectVersion());
        event.setAction(policy.getActionType());
        event.setReason(command.getReason());
        event.setActorId(actor.getId());
        event.setActorUsername(resolveActorUsername(actor));
        event.setActorDisplayName(resolveActorDisplayName(actor));
        event.setServerOccurredAt(serverOccurredAt);
        event.setBeforeState(command.getBeforeState().getState());
        event.setBeforeObjectVersion(command.getBeforeState().getObjectVersion());
        event.setBeforeStateJson(command.getBeforeState().getCanonicalJson());
        event.setAfterState(command.getAfterState().getState());
        event.setAfterObjectVersion(command.getAfterState().getObjectVersion());
        event.setAfterStateJson(command.getAfterState().getCanonicalJson());
        event.setPolicyVersion(policy.getPolicyVersion());
        event.setIdempotencyKey(command.getIdempotencyKey());
        event.setRequestId(command.getRequestId());
        event.setSignatureRecordId(command.getSignatureRecordId());
        event.setSignatureContentHash(command.getSignatureContentHash());
        event.setIdempotencyPayloadHash(idempotencyPayloadHash);
        event.setPreviousEventHash(previous == null ? null : previous.getEventHash());
        event.setAlgorithm(HASH_ALGORITHM);
        event.setResultStatus(command.getResultStatus());
        event.setReasonCode(command.getReasonCode());
        event.setReasonSource(command.getReasonSource());
        event.setTransactionId(command.getTransactionId());
        event.setAuthenticatedActorJson(command.getAuthenticatedActor());
        event.setPerformedByJson(command.getPerformedBy());
        event.setSourceType(command.getSourceType());
        event.setSourceLocator(command.getSourceLocator());
        event.setTraceId(command.getTraceId());
        event.setErrorCode(command.getErrorCode());
        event.setAttemptedOperationId(command.getAttemptedOperationId());
        event.setRelationManifestJson(command.getRelationManifest());
        event.setEvidenceManifestJson(command.getEvidenceManifest());
        event.setStatePayloadHash(command.getStatePayloadHash());
        event.setCanonicalEventJson(canonicalEvent(event));
        event.setEventHash(sha256(event.getCanonicalEventJson()));

        if (auditEventMapper.insert(event) != 1) {
            throw exception(GXP_AUDIT_APPEND_FAILED, command.getOperationId());
        }
        for (GxpAuditRelation relation : command.getLinks()) {
            GxpAuditEventRelationDO relationDO = new GxpAuditEventRelationDO();
            relationDO.setTenantId(tenantId);
            relationDO.setEventId(event.getId());
            relationDO.setRelationType(relation.relationType());
            relationDO.setTargetType(relation.objectType());
            relationDO.setTargetId(relation.objectId());
            relationDO.setTargetVersion(relation.objectVersion());
            relationDO.setTargetHash(relation.objectHash());
            relationDO.setCreatedAtUtc(serverOccurredAt);
            if (eventRelationMapper.insert(relationDO) != 1) {
                throw exception(GXP_AUDIT_APPEND_FAILED, command.getOperationId() + ":relation");
            }
        }
        return new GxpAuditAppendResult(event.getId(), event.getLedgerSequence(), event.getEventHash(), false);
    }

    private GxpAuditLedgerSequenceDO lockLedgerSequence(Long tenantId) {
        GxpAuditLedgerSequenceDO sequence = ledgerSequenceMapper.selectByTenantIdForUpdate(tenantId);
        if (sequence == null) {
            throw exception(GXP_AUDIT_APPEND_FAILED, "ledger-sequence-watermark-not-initialized");
        }
        Long nextLedgerSequence = sequence.getNextLedgerSequence();
        if (nextLedgerSequence == null || nextLedgerSequence < 1L) {
            throw exception(GXP_AUDIT_APPEND_FAILED, "invalid-ledger-sequence-watermark");
        }
        return sequence;
    }

    private long allocateLedgerSequence(Long tenantId, GxpAuditLedgerSequenceDO sequence) {
        Long nextLedgerSequence = sequence.getNextLedgerSequence();
        GxpAuditLedgerSequenceDO updated = new GxpAuditLedgerSequenceDO();
        updated.setTenantId(tenantId);
        updated.setNextLedgerSequence(nextLedgerSequence + 1L);
        int updatedRows = ledgerSequenceMapper.updateById(updated);
        if (updatedRows != 1) {
            throw exception(GXP_AUDIT_APPEND_FAILED, "ledger-sequence-watermark-update");
        }
        return nextLedgerSequence;
    }

    private void validateRequired(GxpAuditCommand command) {
        if (command == null || StrUtil.isBlank(command.getOperationId())) {
            throw exception(GXP_AUDIT_POLICY_NOT_FOUND, "operationId");
        }
        if (StrUtil.isBlank(command.getSubjectId()) || StrUtil.isBlank(command.getSubjectVersion())
                || StrUtil.isBlank(command.getIdempotencyKey())) {
            throw exception(GXP_AUDIT_BEFORE_AFTER_REQUIRED, command.getOperationId());
        }
        if (command.getIdempotencyKey().length() > 96) {
            throw exception(GXP_AUDIT_V2_CONTRACT_INVALID, "idempotencyKey");
        }
    }

    private void validatePolicyRequirements(GxpAuditCommand command, GxpAuditPolicyOperationDO policy) {
        if (StrUtil.startWith(policy.getReasonPolicy(), "REQUIRED") && StrUtil.isBlank(command.getReason())) {
            throw exception(GXP_AUDIT_REASON_REQUIRED, command.getOperationId());
        }
        if (command.getBeforeState() == null || command.getAfterState() == null
                || StrUtil.isBlank(command.getBeforeState().getState())
                || StrUtil.isBlank(command.getAfterState().getState())
                || StrUtil.isBlank(command.getBeforeState().getCanonicalJson())
                || StrUtil.isBlank(command.getAfterState().getCanonicalJson())) {
            throw exception(GXP_AUDIT_BEFORE_AFTER_REQUIRED, command.getOperationId());
        }
        if ("REQUIRED".equals(policy.getSignaturePolicy()) && StrUtil.isBlank(command.getSignatureRecordId())) {
            throw exception(GXP_AUDIT_SIGNATURE_REQUIRED, command.getOperationId());
        }
    }

    private void enrichServerOwnedFields(GxpAuditCommand command, LoginUser actor, Long tenantId) {
        command.setEventSchemaVersion(2);
        command.setTransactionId(StrUtil.blankToDefault(command.getTransactionId(),
                GxpAuditTransactionContext.requireTransactionId()));
        command.setResultStatus(StrUtil.blankToDefault(command.getResultStatus(), "SUCCESS"));
        command.setReasonCode(StrUtil.blankToDefault(command.getReasonCode(),
                command.getOperationId().toUpperCase().replace('.', '_').replace('-', '_')));
        command.setReasonSource(StrUtil.blankToDefault(command.getReasonSource(),
                StrUtil.isBlank(command.getReason()) ? "SYSTEM" : "USER"));
        command.setAuthenticatedActor(StrUtil.blankToDefault(command.getAuthenticatedActor(), actorJson(actor, tenantId)));
        command.setPerformedBy(StrUtil.blankToDefault(command.getPerformedBy(), actorJson(actor, tenantId)));
        command.setSourceType(StrUtil.blankToDefault(command.getSourceType(), "SERVICE_METHOD"));
        command.setSourceLocator(StrUtil.blankToDefault(command.getSourceLocator(), command.getSource()));
        if (StrUtil.isBlank(command.getSourceLocator())) {
            throw exception(GXP_AUDIT_V2_CONTRACT_INVALID, "sourceLocator");
        }
        command.setLinks(command.getLinks() == null ? List.of() : List.copyOf(command.getLinks()));
        command.setEvidences(command.getEvidences() == null ? List.of() : List.copyOf(command.getEvidences()));
        command.setRelationManifest(JsonUtils.toJsonString(command.getLinks()));
        command.setEvidenceManifest(JsonUtils.toJsonString(command.getEvidences()));
        command.setStatePayloadHash(sha256(command.getBeforeState().getCanonicalJson()
                + "\u001f" + command.getAfterState().getCanonicalJson()));
        if (!List.of("SUCCESS", "FAILED", "DENIED").contains(command.getResultStatus())) {
            throw exception(GXP_AUDIT_V2_CONTRACT_INVALID, "resultStatus");
        }
        if (List.of("FAILED", "DENIED").contains(command.getResultStatus())
                && StrUtil.isBlank(command.getErrorCode())) {
            throw exception(GXP_AUDIT_V2_CONTRACT_INVALID, "errorCode");
        }
    }

    private String actorJson(LoginUser actor, Long tenantId) {
        TreeMap<String, Object> json = new TreeMap<>();
        json.put("actorId", actor.getId());
        json.put("displayName", resolveActorDisplayName(actor));
        json.put("tenantId", tenantId);
        json.put("userType", actor.getUserType());
        json.put("username", resolveActorUsername(actor));
        return JsonUtils.toJsonString(json);
    }

    private Long resolveTenantId() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getTenantId() != null) {
            return loginUser.getTenantId();
        }
        return TenantContextHolder.getRequiredTenantId();
    }

    private LoginUser resolveActor() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getId() != null) {
            return loginUser;
        }
        LoginUser systemActor = new LoginUser();
        systemActor.setId(-1L);
        systemActor.setUserType(UserTypeEnum.ADMIN.getValue());
        systemActor.setTenantId(resolveTenantId());
        systemActor.setInfo(Map.of(LoginUser.INFO_KEY_NICKNAME, "SYSTEM_ACTOR", "username", "SYSTEM_ACTOR"));
        return systemActor;
    }

    private String resolveActorUsername(LoginUser actor) {
        if (actor.getInfo() == null) {
            return "SYSTEM_ACTOR";
        }
        return actor.getInfo().getOrDefault("username", "SYSTEM_ACTOR");
    }

    private String resolveActorDisplayName(LoginUser actor) {
        if (actor.getInfo() == null) {
            return resolveActorUsername(actor);
        }
        return actor.getInfo().getOrDefault(LoginUser.INFO_KEY_NICKNAME, resolveActorUsername(actor));
    }

    private String canonicalPayload(GxpAuditCommand command, GxpAuditPolicyOperationDO policy) {
        TreeMap<String, Object> payload = new TreeMap<>();
        payload.put("operationId", command.getOperationId());
        payload.put("policyVersion", policy.getPolicyVersion());
        payload.put("subjectType", policy.getSubjectType());
        payload.put("subjectId", command.getSubjectId());
        payload.put("subjectVersion", command.getSubjectVersion());
        payload.put("actionType", policy.getActionType());
        payload.put("reason", command.getReason());
        payload.put("resultStatus", command.getResultStatus());
        payload.put("errorCode", command.getErrorCode());
        payload.put("attemptedOperationId", command.getAttemptedOperationId());
        payload.put("reasonCode", command.getReasonCode());
        payload.put("reasonSource", command.getReasonSource());
        payload.put("sourceType", command.getSourceType());
        payload.put("sourceLocator", command.getSourceLocator());
        payload.put("before", command.getBeforeState());
        payload.put("after", command.getAfterState());
        payload.put("signatureRecordId", command.getSignatureRecordId());
        payload.put("signatureContentHash", command.getSignatureContentHash());
        payload.put("authenticatedActor", command.getAuthenticatedActor());
        payload.put("performedBy", command.getPerformedBy());
        payload.put("links", command.getLinks());
        payload.put("evidences", command.getEvidences());
        return JsonUtils.toJsonString(payload);
    }

    private String canonicalEvent(GxpAuditEventDO event) {
        TreeMap<String, Object> payload = new TreeMap<>();
        payload.put("tenantId", event.getTenantId());
        payload.put("ledgerSequence", event.getLedgerSequence());
        payload.put("operationId", event.getOperationId());
        payload.put("domain", event.getDomain());
        payload.put("subjectType", event.getSubjectType());
        payload.put("subjectId", event.getSubjectId());
        payload.put("subjectVersion", event.getSubjectVersion());
        payload.put("action", event.getAction());
        payload.put("reason", event.getReason());
        payload.put("actorId", event.getActorId());
        payload.put("actorUsername", event.getActorUsername());
        payload.put("actorDisplayName", event.getActorDisplayName());
        payload.put("serverOccurredAt", event.getServerOccurredAt().toString());
        payload.put("beforeState", event.getBeforeState());
        payload.put("beforeObjectVersion", event.getBeforeObjectVersion());
        payload.put("beforeStateJson", event.getBeforeStateJson());
        payload.put("afterState", event.getAfterState());
        payload.put("afterObjectVersion", event.getAfterObjectVersion());
        payload.put("afterStateJson", event.getAfterStateJson());
        payload.put("policyVersion", event.getPolicyVersion());
        payload.put("idempotencyKey", event.getIdempotencyKey());
        payload.put("idempotencyPayloadHash", event.getIdempotencyPayloadHash());
        payload.put("requestId", event.getRequestId());
        payload.put("signatureRecordId", event.getSignatureRecordId());
        payload.put("signatureContentHash", event.getSignatureContentHash());
        payload.put("previousEventHash", event.getPreviousEventHash());
        payload.put("algorithm", event.getAlgorithm());
        payload.put("eventSchemaVersion", event.getEventSchemaVersion());
        payload.put("canonicalizationVersion", event.getCanonicalizationVersion());
        payload.put("resultStatus", event.getResultStatus());
        payload.put("reasonCode", event.getReasonCode());
        payload.put("reasonSource", event.getReasonSource());
        payload.put("transactionId", event.getTransactionId());
        payload.put("authenticatedActorJson", event.getAuthenticatedActorJson());
        payload.put("performedByJson", event.getPerformedByJson());
        payload.put("sourceType", event.getSourceType());
        payload.put("sourceLocator", event.getSourceLocator());
        payload.put("traceId", event.getTraceId());
        payload.put("errorCode", event.getErrorCode());
        payload.put("attemptedOperationId", event.getAttemptedOperationId());
        payload.put("relationManifestJson", event.getRelationManifestJson());
        payload.put("evidenceManifestJson", event.getEvidenceManifestJson());
        payload.put("statePayloadHash", event.getStatePayloadHash());
        return JsonUtils.toJsonString(payload);
    }

    private String sha256(String content) {
        return DigestUtil.sha256Hex(content);
    }

}
