package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Read-only integrity facts. Domain creation certification belongs to the consuming domain. */
@Service
public class GxpCreationAuditVerificationService {
    private static final ObjectMapper FACT_READER = new ObjectMapper();
    private static final ObjectMapper STRICT_READER = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
    @Resource
    private GxpAuditEventMapper auditEventMapper;
    @Resource
    private GxpAuditEventRelationMapper eventRelationMapper;

    // Every field emitted by GxpAuditServiceImpl.canonicalEvent, including nullable fields.
    private static final List<String> PROTECTED_FIELDS = List.of("tenantId", "ledgerSequence", "operationId",
            "domain", "subjectType", "subjectId", "subjectVersion", "action", "reason", "actorId", "actorUsername",
            "actorDisplayName", "serverOccurredAt", "beforeState", "beforeObjectVersion", "beforeStateJson",
            "afterState", "afterObjectVersion", "afterStateJson", "policyVersion", "idempotencyKey",
            "idempotencyPayloadHash", "requestId", "signatureRecordId", "signatureContentHash", "previousEventHash",
            "algorithm", "eventSchemaVersion", "canonicalizationVersion", "resultStatus", "reasonCode", "reasonSource",
            "transactionId", "authenticatedActorJson", "performedByJson", "sourceType", "sourceLocator", "traceId",
            "errorCode", "attemptedOperationId", "relationManifestJson", "evidenceManifestJson", "statePayloadHash");

    public List<Long> readCandidateEventIdsByTarget(Long tenantId, String targetType, String targetId) {
        requireTenant(tenantId);
        if (StrUtil.hasBlank(targetType, targetId)) throw new IllegalArgumentException("Audit target must be nonblank");
        List<Long> ids = eventRelationMapper.selectEventIdsByTargets(tenantId, List.of(targetType), targetId);
        if (ids == null) throw new IllegalStateException("Audit candidate query returned null");
        Set<Long> unique = new HashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0 || !unique.add(id)) {
                throw new IllegalStateException("Audit candidate query returned an invalid or duplicate ID");
            }
        }
        return List.copyOf(ids);
    }

    public VerifiedAuditEventFact requireVerifiedEvent(Long tenantId, Long eventId) {
        requireTenant(tenantId);
        if (eventId == null || eventId <= 0) throw new IllegalArgumentException("Audit event ID must be positive");
        GxpAuditEventDO event = auditEventMapper.selectByTenantIdAndId(tenantId, eventId);
        if (event == null) throw new IllegalStateException("AUDIT_EVENT_MISSING");
        if (!Objects.equals(eventId, event.getId()) || !originalIntegrity(event, tenantId)
                || StrUtil.isBlank(event.getIdempotencyKey())) throw invalid("AUDIT_INTEGRITY_FAILED");
        JsonNode canonical = strictTree(event.getCanonicalEventJson(), "AUDIT_INTEGRITY_FAILED");
        requireObjectKeys(canonical, Set.copyOf(PROTECTED_FIELDS), "AUDIT_INTEGRITY_FAILED");
        if (event.getActorId() == null || event.getActorId() <= 0
                || !strictActorMatches(event.getAuthenticatedActorJson(), event.getActorId(), tenantId)
                || !strictActorMatches(event.getPerformedByJson(), event.getActorId(), tenantId)) {
            throw invalid("AUDIT_ACTOR_MISMATCH");
        }
        List<GxpAuditRelation> relations = readRelations(event.getRelationManifestJson());
        List<GxpAuditEvidence> evidences = readEvidences(event.getEvidenceManifestJson());
        List<GxpAuditEventRelationDO> rows = eventRelationMapper.selectListByTenantIdAndEventId(tenantId, eventId);
        if (rows == null) throw invalid("AUDIT_RELATION_MISMATCH");
        Set<RelationKey> keys = new HashSet<>();
        Set<Long> rowIds = new HashSet<>();
        Set<GxpAuditRelation> physical = new HashSet<>();
        for (GxpAuditEventRelationDO row : rows) {
            if (row == null || row.getId() == null || row.getId() <= 0 || !rowIds.add(row.getId())
                    || !Objects.equals(tenantId, row.getTenantId()) || !Objects.equals(eventId, row.getEventId())
                    || StrUtil.hasBlank(row.getRelationType(), row.getTargetType(), row.getTargetId())) {
                throw invalid("AUDIT_RELATION_MISMATCH");
            }
            GxpAuditRelation relation = new GxpAuditRelation(row.getRelationType(), row.getTargetType(),
                    row.getTargetId(), row.getTargetVersion(), row.getTargetHash());
            if (!keys.add(key(relation))) throw invalid("AUDIT_RELATION_MISMATCH");
            physical.add(relation);
        }
        if (!physical.equals(new HashSet<>(relations))) throw invalid("AUDIT_RELATION_MISMATCH");
        return new VerifiedAuditEventFact(eventId, event.getEventHash(), event.getCanonicalEventJson(),
                columns(event), relations, evidences);
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("Audit tenant must be positive");
        if (!Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("Audit read belongs to another tenant");
        }
    }

    private static IllegalStateException invalid(String category) { return new IllegalStateException(category); }

    private static JsonNode strictTree(String json, String category) {
        if (StrUtil.isBlank(json)) throw invalid(category);
        try (JsonParser parser = STRICT_READER.createParser(json)) {
            JsonNode root = STRICT_READER.readTree(parser);
            if (root == null || parser.nextToken() != null) throw invalid(category);
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException(category, ex);
        }
    }

    private static void requireObjectKeys(JsonNode node, Set<String> allowed, String category) {
        if (!node.isObject()) throw invalid(category);
        node.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) throw invalid(category); });
    }

    private static boolean strictActorMatches(String json, Long actorId, Long tenantId) {
        JsonNode actor = strictTree(json, "AUDIT_ACTOR_MISMATCH");
        return actor.isObject()
                && Objects.equals(actor.get("actorId"), originalTree(JsonUtils.toJsonString(actorId)))
                && Objects.equals(actor.get("tenantId"), originalTree(JsonUtils.toJsonString(tenantId)));
    }

    static boolean originalActorMatches(String json, Long actorId, Long tenantId) {
        JsonNode actor = originalTree(json);
        return actor != null && actor.isObject()
                && Objects.equals(actor.get("actorId"), originalTree(JsonUtils.toJsonString(actorId)))
                && Objects.equals(actor.get("tenantId"), originalTree(JsonUtils.toJsonString(tenantId)));
    }

    private static String manifestString(JsonNode node, String field, boolean required) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            if (required) throw invalid("AUDIT_MANIFEST_INVALID");
            return null;
        }
        if (!value.isTextual() || required && StrUtil.isBlank(value.textValue())) throw invalid("AUDIT_MANIFEST_INVALID");
        return value.textValue();
    }

    private static List<GxpAuditRelation> readRelations(String json) {
        JsonNode array = strictTree(json, "AUDIT_MANIFEST_INVALID");
        if (!array.isArray()) throw invalid("AUDIT_MANIFEST_INVALID");
        List<GxpAuditRelation> result = new ArrayList<>();
        Set<RelationKey> keys = new HashSet<>();
        for (JsonNode node : array) {
            requireObjectKeys(node, Set.of("relationType", "objectType", "objectId", "objectVersion", "objectHash"), "AUDIT_MANIFEST_INVALID");
            GxpAuditRelation relation = new GxpAuditRelation(manifestString(node, "relationType", true),
                    manifestString(node, "objectType", true), manifestString(node, "objectId", true),
                    manifestString(node, "objectVersion", false), manifestString(node, "objectHash", false));
            if (!keys.add(key(relation))) throw invalid("AUDIT_MANIFEST_INVALID");
            result.add(relation);
        }
        return List.copyOf(result);
    }

    private static List<GxpAuditEvidence> readEvidences(String json) {
        JsonNode array = strictTree(json, "AUDIT_MANIFEST_INVALID");
        if (!array.isArray()) throw invalid("AUDIT_MANIFEST_INVALID");
        List<GxpAuditEvidence> result = new ArrayList<>();
        for (JsonNode node : array) {
            requireObjectKeys(node, Set.of("evidenceType", "sourceId", "sourceVersion", "sha256", "role"), "AUDIT_MANIFEST_INVALID");
            result.add(new GxpAuditEvidence(manifestString(node, "evidenceType", false), manifestString(node, "sourceId", false),
                    manifestString(node, "sourceVersion", false), manifestString(node, "sha256", false), manifestString(node, "role", false)));
        }
        return List.copyOf(result);
    }

    private static RelationKey key(GxpAuditRelation relation) {
        return new RelationKey(relation.relationType(), relation.objectType(), relation.objectId());
    }
    private record RelationKey(String role, String type, String id) {}

    public record VerifiedAuditEventFact(Long eventId, String eventHash, String canonicalEventJson,
            ProtectedColumns columns, List<GxpAuditRelation> relations, List<GxpAuditEvidence> evidences) {
        public VerifiedAuditEventFact {
            Objects.requireNonNull(columns, "columns");
            relations = List.copyOf(relations);
            evidences = List.copyOf(evidences);
        }
    }

    static boolean originalIntegrity(GxpAuditEventDO audit, Long tenantId) {
        if (audit == null || audit.getId() == null || audit.getId() <= 0
                || !Objects.equals(tenantId, audit.getTenantId()) || !"SUCCESS".equals(audit.getResultStatus())
                || !Integer.valueOf(2).equals(audit.getEventSchemaVersion()) || !"SHA-256".equals(audit.getAlgorithm())
                || !"GXP_CANONICAL_V2".equals(audit.getCanonicalizationVersion())
                || audit.getLedgerSequence() == null || audit.getLedgerSequence() <= 0
                || audit.getServerOccurredAt() == null
                || StrUtil.hasBlank(audit.getTransactionId(), audit.getCanonicalEventJson(), audit.getEventHash(),
                        audit.getBeforeStateJson(), audit.getAfterStateJson(), audit.getStatePayloadHash(), audit.getPolicyVersion(),
                        audit.getIdempotencyPayloadHash())
                || !Objects.equals(DigestUtil.sha256Hex(audit.getCanonicalEventJson()), audit.getEventHash())
                || !Objects.equals(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()),
                        audit.getStatePayloadHash())) return false;
        JsonNode canonical = originalTree(audit.getCanonicalEventJson());
        if (canonical == null || !canonical.isObject()) return false;
        var bean = new BeanWrapperImpl(audit);
        for (String field : PROTECTED_FIELDS) {
            Object column = bean.getPropertyValue(field);
            // The writer explicitly serializes this field using LocalDateTime.toString().
            if ("serverOccurredAt".equals(field)) column = audit.getServerOccurredAt().toString();
            JsonNode node = canonical.get(field);
            if (column == null ? node != null && !node.isNull()
                    : !Objects.equals(node, originalTree(JsonUtils.toJsonString(column)))) return false;
        }
        return true;
    }

    static JsonNode originalTree(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return FACT_READER.readTree(json); }
        catch (JsonProcessingException ex) { return null; } // Malformed stored fact, never an infrastructure read.
    }

    public record ProtectedColumns(
            Long tenantId,
            Long ledgerSequence,
            String operationId,
            String domain,
            String subjectType,
            String subjectId,
            String subjectVersion,
            String action,
            String reason,
            Long actorId,
            String actorUsername,
            String actorDisplayName,
            LocalDateTime serverOccurredAt,
            String beforeState,
            String beforeObjectVersion,
            String beforeStateJson,
            String afterState,
            String afterObjectVersion,
            String afterStateJson,
            String policyVersion,
            String idempotencyKey,
            String idempotencyPayloadHash,
            String requestId,
            String signatureRecordId,
            String signatureContentHash,
            String previousEventHash,
            String algorithm,
            Integer eventSchemaVersion,
            String canonicalizationVersion,
            String resultStatus,
            String reasonCode,
            String reasonSource,
            String transactionId,
            String authenticatedActorJson,
            String performedByJson,
            String sourceType,
            String sourceLocator,
            String traceId,
            String errorCode,
            String attemptedOperationId,
            String relationManifestJson,
            String evidenceManifestJson,
            String statePayloadHash) {}
    private static ProtectedColumns columns(GxpAuditEventDO event) {
        return new ProtectedColumns(event.getTenantId(), event.getLedgerSequence(), event.getOperationId(), event.getDomain(), event.getSubjectType(), event.getSubjectId(), event.getSubjectVersion(), event.getAction(), event.getReason(), event.getActorId(), event.getActorUsername(), event.getActorDisplayName(), event.getServerOccurredAt(), event.getBeforeState(), event.getBeforeObjectVersion(), event.getBeforeStateJson(), event.getAfterState(), event.getAfterObjectVersion(), event.getAfterStateJson(), event.getPolicyVersion(), event.getIdempotencyKey(), event.getIdempotencyPayloadHash(), event.getRequestId(), event.getSignatureRecordId(), event.getSignatureContentHash(), event.getPreviousEventHash(), event.getAlgorithm(), event.getEventSchemaVersion(), event.getCanonicalizationVersion(), event.getResultStatus(), event.getReasonCode(), event.getReasonSource(), event.getTransactionId(), event.getAuthenticatedActorJson(), event.getPerformedByJson(), event.getSourceType(), event.getSourceLocator(), event.getTraceId(), event.getErrorCode(), event.getAttemptedOperationId(), event.getRelationManifestJson(), event.getEvidenceManifestJson(), event.getStatePayloadHash());
    }
}
