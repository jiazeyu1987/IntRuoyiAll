package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
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
    private GxpAuditPolicyOperationMapper policyOperationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GxpAuditAppendResult append(GxpAuditCommand command) {
        validateRequired(command);
        Long tenantId = resolveTenantId();
        GxpAuditPolicyOperationDO policy = policyOperationMapper.selectActive(tenantId, command.getOperationId());
        if (policy == null) {
            throw exception(GXP_AUDIT_POLICY_NOT_FOUND, command.getOperationId());
        }
        validatePolicyRequirements(command, policy);

        String idempotencyPayloadHash = sha256(canonicalPayload(command, policy));
        GxpAuditEventDO existing = auditEventMapper.selectByIdempotencyKey(tenantId, command.getIdempotencyKey());
        if (existing != null) {
            if (!Objects.equals(existing.getIdempotencyPayloadHash(), idempotencyPayloadHash)) {
                throw exception(GXP_AUDIT_IDEMPOTENCY_CONFLICT, command.getOperationId());
            }
            return new GxpAuditAppendResult(existing.getId(), existing.getLedgerSequence(),
                    existing.getEventHash(), true);
        }

        LoginUser actor = resolveActor();
        GxpAuditEventDO previous = auditEventMapper.selectLatestByTenant(tenantId);
        long ledgerSequence = (auditEventMapper.selectMaxLedgerSequence(tenantId) == null
                ? 0L : auditEventMapper.selectMaxLedgerSequence(tenantId)) + 1L;
        LocalDateTime serverOccurredAt = LocalDateTime.now();

        GxpAuditEventDO event = new GxpAuditEventDO();
        event.setTenantId(tenantId);
        event.setLedgerSequence(ledgerSequence);
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
        event.setCanonicalEventJson(canonicalEvent(event));
        event.setEventHash(sha256(event.getCanonicalEventJson()));

        try {
            auditEventMapper.insert(event);
        } catch (RuntimeException ex) {
            throw exception(GXP_AUDIT_APPEND_FAILED, command.getOperationId());
        }
        return new GxpAuditAppendResult(event.getId(), event.getLedgerSequence(), event.getEventHash(), false);
    }

    private void validateRequired(GxpAuditCommand command) {
        if (command == null || StrUtil.isBlank(command.getOperationId())) {
            throw exception(GXP_AUDIT_POLICY_NOT_FOUND, "operationId");
        }
        if (StrUtil.isBlank(command.getSubjectId()) || StrUtil.isBlank(command.getSubjectVersion())
                || StrUtil.isBlank(command.getIdempotencyKey())) {
            throw exception(GXP_AUDIT_BEFORE_AFTER_REQUIRED, command.getOperationId());
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
        payload.put("before", command.getBeforeState());
        payload.put("after", command.getAfterState());
        payload.put("signatureRecordId", command.getSignatureRecordId());
        payload.put("signatureContentHash", command.getSignatureContentHash());
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
        return JsonUtils.toJsonString(payload);
    }

    private String sha256(String content) {
        return DigestUtil.sha256Hex(content);
    }

}
