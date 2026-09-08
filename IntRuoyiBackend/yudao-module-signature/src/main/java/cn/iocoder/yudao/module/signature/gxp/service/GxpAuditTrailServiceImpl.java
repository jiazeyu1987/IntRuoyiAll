package cn.iocoder.yudao.module.signature.gxp.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditActor;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditSubject;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditTrailAppendCommand;
import cn.iocoder.yudao.module.signature.gxp.api.GxpAuditTrailService;
import cn.iocoder.yudao.module.signature.gxp.dal.dataobject.GxpAuditEventDO;
import cn.iocoder.yudao.module.signature.gxp.dal.mysql.GxpAuditEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.GXP_AUDIT_COMMAND_INVALID;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.GXP_AUDIT_IDEMPOTENCY_CONFLICT;

@Service
public class GxpAuditTrailServiceImpl implements GxpAuditTrailService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Set<String> ALLOWED_STATES = Set.of("ABSENT", "PRESENT", "VOIDED");

    private final GxpAuditEventMapper eventMapper;
    private final Clock clock;

    @Autowired
    public GxpAuditTrailServiceImpl(GxpAuditEventMapper eventMapper) {
        this(eventMapper, Clock.systemDefaultZone());
    }

    public GxpAuditTrailServiceImpl(GxpAuditEventMapper eventMapper, Clock clock) {
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long append(GxpAuditTrailAppendCommand command) {
        validateCommand(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime occurredAt = LocalDateTime.now(clock);
        String canonicalEventJson = canonicalEventJson(command, occurredAt);
        GxpAuditEventDO existing = eventMapper.selectByTenantIdAndIdempotencyKey(tenantId, command.idempotencyKey());
        if (existing != null) {
            if (!canonicalEventJson.equals(existing.getCanonicalEventJson())) {
                throw exception(GXP_AUDIT_IDEMPOTENCY_CONFLICT);
            }
            return existing.getId();
        }
        Long maxLedgerSequence = eventMapper.selectMaxLedgerSequence(tenantId);
        long nextLedgerSequence = maxLedgerSequence == null ? 1L : maxLedgerSequence + 1L;
        String eventHash = hash(canonicalEventJson);

        GxpAuditSubject subject = command.subject();
        GxpAuditActor actor = command.actor();
        GxpAuditStateEnvelope before = command.beforeState();
        GxpAuditStateEnvelope after = command.afterState();
        GxpAuditEventDO event = GxpAuditEventDO.builder()
                .tenantId(tenantId)
                .ledgerSequence(nextLedgerSequence)
                .operationId(command.operationId())
                .domain(subject.domain())
                .subjectType(subject.subjectType())
                .subjectId(subject.subjectId())
                .subjectVersion(subject.subjectVersion())
                .action(command.action().name())
                .reason(command.reason())
                .actorId(actor.actorId())
                .actorUsername(actor.username())
                .actorDisplayName(actor.displayName())
                .serverOccurredAt(occurredAt)
                .beforeState(before.state())
                .beforeObjectVersion(before.objectVersion())
                .beforeStateJson(before.canonicalJson())
                .afterState(after.state())
                .afterObjectVersion(after.objectVersion())
                .afterStateJson(after.canonicalJson())
                .policyVersion(command.policyVersion())
                .idempotencyKey(command.idempotencyKey())
                .requestId(command.requestId())
                .signatureRecordId(command.signatureRecordId())
                .signatureContentHash(command.signatureContentHash())
                .canonicalEventJson(canonicalEventJson)
                .previousEventHash(null)
                .eventHash(eventHash)
                .algorithm(HASH_ALGORITHM)
                .createTime(occurredAt)
                .build();
        eventMapper.insert(event);
        return event.getId();
    }

    private void validateCommand(GxpAuditTrailAppendCommand command) {
        if (command == null) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, "命令不能为空");
        }
        if (command.action() == null) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, "动作不能为空");
        }
        validateSubject(command.subject());
        validateActor(command.actor());
        validateState("beforeState", command.beforeState());
        validateState("afterState", command.afterState());
        if (StrUtil.hasBlank(command.operationId(), command.reason(), command.policyVersion(),
                command.idempotencyKey())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, "operationId、原因、策略版本和幂等键不能为空");
        }
    }

    private void validateSubject(GxpAuditSubject subject) {
        if (subject == null || StrUtil.hasBlank(subject.domain(), subject.subjectType(), subject.subjectId(),
                subject.subjectVersion())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, "审计对象不能为空");
        }
    }

    private void validateActor(GxpAuditActor actor) {
        if (actor == null || actor.actorId() == null || StrUtil.hasBlank(actor.username(), actor.displayName())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, "操作人不能为空");
        }
    }

    private void validateState(String label, GxpAuditStateEnvelope state) {
        if (state == null || StrUtil.isBlank(state.state()) || !ALLOWED_STATES.contains(state.state())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, label + " 状态必须为 ABSENT、PRESENT 或 VOIDED");
        }
        if ("PRESENT".equals(state.state()) && StrUtil.hasBlank(state.objectVersion(), state.canonicalJson())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, label + " 为 PRESENT 时版本和规范化内容不能为空");
        }
        if ("VOIDED".equals(state.state()) && StrUtil.isBlank(state.objectVersion())) {
            throw exception(GXP_AUDIT_COMMAND_INVALID, label + " 为 VOIDED 时版本不能为空");
        }
    }

    private String canonicalEventJson(GxpAuditTrailAppendCommand command, LocalDateTime occurredAt) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        GxpAuditSubject subject = command.subject();
        GxpAuditActor actor = command.actor();
        GxpAuditStateEnvelope before = command.beforeState();
        GxpAuditStateEnvelope after = command.afterState();
        return "{"
                + jsonPair("tenantId", String.valueOf(tenantId)) + ","
                + jsonPair("operationId", command.operationId()) + ","
                + jsonPair("domain", subject.domain()) + ","
                + jsonPair("subjectType", subject.subjectType()) + ","
                + jsonPair("subjectId", subject.subjectId()) + ","
                + jsonPair("subjectVersion", subject.subjectVersion()) + ","
                + jsonPair("action", command.action().name()) + ","
                + jsonPair("reason", command.reason()) + ","
                + jsonPair("actorId", String.valueOf(actor.actorId())) + ","
                + jsonPair("actorUsername", actor.username()) + ","
                + jsonPair("serverOccurredAt", occurredAt.toString()) + ","
                + jsonPair("beforeState", before.state()) + ","
                + jsonPair("beforeObjectVersion", before.objectVersion()) + ","
                + jsonPair("beforeStateJson", before.canonicalJson()) + ","
                + jsonPair("afterState", after.state()) + ","
                + jsonPair("afterObjectVersion", after.objectVersion()) + ","
                + jsonPair("afterStateJson", after.canonicalJson()) + ","
                + jsonPair("policyVersion", command.policyVersion()) + ","
                + jsonPair("idempotencyKey", command.idempotencyKey()) + ","
                + jsonPair("requestId", command.requestId()) + ","
                + jsonPair("signatureRecordId", command.signatureRecordId()) + ","
                + jsonPair("signatureContentHash", command.signatureContentHash())
                + "}";
    }

    private String jsonPair(String key, String value) {
        return "\"" + escape(key) + "\":\"" + escape(StrUtil.nullToEmpty(value)) + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

}
