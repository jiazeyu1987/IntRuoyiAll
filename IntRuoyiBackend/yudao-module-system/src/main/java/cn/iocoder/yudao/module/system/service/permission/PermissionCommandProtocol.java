package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.PermissionCommandReceiptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.PermissionCommandReceiptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Three closed permission commands. No issuance endpoint or historical repair path. */
@Component
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class PermissionCommandProtocol {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final String RESULT = canonical(Map.of("schemaVersion", "system.permission.result.v1", "kind", "VOID"));
    private static final String EVIDENCE_TYPE = "SYSTEM_PERMISSION_COMMAND_RECEIPT";
    @Resource private PermissionCommandReceiptMapper receipts;
    @Resource private GxpAuditEventMapper events;
    @Resource private GxpAuditPolicyActivationMapper activations;
    @Resource private GxpAuditPolicyOperationMapper operations;
    @Resource private GxpAuditService audit;

    public record Command(PermissionCommandReceiptDO receipt, String key, String reason, boolean replayed,
                          Long tenantId, boolean tenantAdministration) {
    }

    public Command begin(String operation, Long subject, String reason, String sourceKey, Map<String, Object> data) {
        return beginInternal(operation, subject, reason, sourceKey, data, TenantContextHolder.getRequiredTenantId(), false);
    }

    // Only PermissionService's authorized tenant-management entry calls this protocol path.
    Command beginTenantAdministration(String operation, Long subject, String reason, String sourceKey,
                                       Map<String, Object> data, Long targetTenant, String managementPermission) {
        var actor = SecurityFrameworkUtils.getLoginUser();
        require(actor != null && actor.getTenantId() != null, "authenticated-actor-required");
        require(Objects.equals(targetTenant, TenantContextHolder.getRequiredTenantId()), "target-tenant-context-required");
        Map<String, Object> managementData = new TreeMap<>(data);
        managementData.put("targetTenantId", targetTenant.toString());
        managementData.put("managementPermission", managementPermission);
        return TenantUtils.execute(actor.getTenantId(), () ->
                beginInternal(operation, subject, reason, sourceKey, managementData, targetTenant, true));
    }

    private Command beginInternal(String operation, Long subject, String reason, String sourceKey,
                                  Map<String, Object> data, Long targetTenant, boolean management) {
        byte[] originalKey = validateKey(sourceKey);
        require(!StrUtil.isBlank(reason), "reason-required");
        utf8(reason);
        require(subject != null && subject > 0, "subject-required");
        boolean user = "system.permission.user-role.assign".equals(operation);
        String subjectType = user ? "SYSTEM_USER" : "SYSTEM_ROLE";
        Map<String, Object> domainData = new TreeMap<>(data);
        if (management) {
            domainData.remove("targetTenantId");
            domainData.remove("managementPermission");
        }
        validateData(operation, subject, domainData);
        var actor = SecurityFrameworkUtils.getLoginUser();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        require(actor != null && actor.getId() != null && actor.getId() > 0
                && Objects.equals(tenant, actor.getTenantId()), "authenticated-tenant-actor-required");
        String identity = canonical(List.of(subjectType, subject.toString(), sourceKey));
        String key = "GXP2:" + hash(canonical(Map.of("identity", List.of(subjectType, subject.toString(), sourceKey),
                "operationId", operation, "tenantId", tenant.toString())));
        Map<String, Object> input = new TreeMap<>(data);
        input.put("reason", reason);
        String inputHash = hash(canonical(Map.of("actorId", actor.getId().toString(), "input", input)));
        String source = canonical(Map.of("schemaVersion", "system.permission.command.v1",
                "sourceKey", sourceKey, "sourceKeySha256", hex(digest(originalKey)),
                "actorId", actor.getId().toString(), "input", input, "inputSha256", inputHash));

        // Same ordering as the writer; all lookups below are current reads, not RR snapshots.
        audit.acquireLedgerLock();
        var activation = activations.selectLatestForUpdate(tenant);
        require(activation != null, "activation-required");
        var policy = operations.selectByPolicyVersionForUpdate(tenant, activation.getPolicyVersion(), operation);
        require(policy != null && Boolean.TRUE.equals(policy.getActive()) && "GXP".equals(policy.getApplicability()),
                "active-operation-required");
        var saved = receipts.selectForUpdate(tenant, operation, digest(originalKey));
        var event = events.selectByIdempotencyKeyForUpdate(tenant, key);
        PermissionCommandReceiptDO draft = new PermissionCommandReceiptDO();
        draft.setTenantId(tenant); draft.setOperationId(operation); draft.setSubjectId(subjectType + ":" + subject);
        draft.setSourceKey(originalKey); draft.setSourceKeySha256(digest(originalKey));
        draft.setSourcePayloadHash(unhex(inputHash)); draft.setIdentityJson(identity); draft.setSourceJson(source);
        draft.setResultJson(RESULT);
        if (saved != null) {
            validateReplay(saved, draft, event, key, actor.getId(), reason);
            return new Command(saved, key, reason, true, targetTenant, management);
        }
        require(event == null, "event-without-receipt");
        // A legacy event cannot be silently upgraded or supplied a missing success receipt.
        require(events.selectByIdempotencyKeyForUpdate(tenant, sourceKey) == null, "legacy-event-without-receipt");
        draft.setId(IdWorker.getId());
        draft.setCreatedAtUtc(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS));
        return new Command(draft, key, reason, false, targetTenant, management);
    }

    public void finish(Command command, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> beforeState = new TreeMap<>(before), afterState = new TreeMap<>(after);
        if (command.tenantAdministration()) {
            beforeState.put("targetTenantId", command.tenantId().toString());
            afterState.put("targetTenantId", command.tenantId().toString());
        }
        TenantUtils.execute(command.receipt().getTenantId(), () -> finishInternal(command, beforeState, afterState));
    }

    private void finishInternal(Command command, Map<String, Object> before, Map<String, Object> after) {
        require(!command.replayed(), "replay-cannot-write");
        var receipt = command.receipt();
        String beforeJson = canonical(before), afterJson = canonical(after);
        String beforeVersion = "sha256:" + hash(beforeJson), afterVersion = "sha256:" + hash(afterJson);
        var result = audit.append(GxpAuditCommand.builder()
                .operationId(receipt.getOperationId()).subjectId(receipt.getSubjectId())
                .subjectVersion(afterVersion)
                .reason(command.reason()).idempotencyKey(command.key()).requestId(command.key())
                .source("PermissionServiceImpl")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT")
                        .objectVersion(beforeVersion).canonicalJson(beforeJson).build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT")
                        .objectVersion(afterVersion).canonicalJson(afterJson).build())
                .evidences(List.of(evidence(receipt))).build());
        require(result != null && result.eventId() != null && result.eventId() > 0
                && result.ledgerSequence() != null && result.ledgerSequence() > 0 && !result.replayed(),
                "new-event-receipt-required");
        receipt.setAuditEventId(result.eventId());
        receipt.setAuditEventHash(unhex(result.eventHash()));
        require(receipts.insert(receipt) == 1, "receipt-insert-failed");
    }

    private static void validateReplay(PermissionCommandReceiptDO saved, PermissionCommandReceiptDO expected,
                                       GxpAuditEventDO event, String key, Long actor, String reason) {
        require(saved.getId() != null && saved.getId() > 0 && saved.getCreatedAtUtc() != null, "receipt-metadata");
        require(Objects.equals(saved.getTenantId(), expected.getTenantId())
                && Objects.equals(saved.getOperationId(), expected.getOperationId())
                && Objects.equals(saved.getSubjectId(), expected.getSubjectId())
                && Arrays.equals(saved.getSourceKey(), expected.getSourceKey())
                && Arrays.equals(saved.getSourceKeySha256(), expected.getSourceKeySha256())
                && Arrays.equals(saved.getSourcePayloadHash(), expected.getSourcePayloadHash())
                && tree(saved.getIdentityJson()).equals(tree(expected.getIdentityJson()))
                && tree(saved.getSourceJson()).equals(tree(expected.getSourceJson()))
                && tree(saved.getResultJson()).equals(tree(RESULT)), "receipt-binding-conflict");
        require(event != null && Objects.equals(saved.getAuditEventId(), event.getId())
                && Objects.equals(event.getTenantId(), expected.getTenantId())
                && Objects.equals(event.getOperationId(), expected.getOperationId())
                && Objects.equals(event.getSubjectId(), expected.getSubjectId())
                && Objects.equals(event.getIdempotencyKey(), key)
                && Objects.equals(event.getActorId(), actor) && Objects.equals(event.getReason(), reason)
                && "SUCCESS".equals(event.getResultStatus()) && Integer.valueOf(2).equals(event.getEventSchemaVersion())
                && "GXP_CANONICAL_V2".equals(event.getCanonicalizationVersion())
                && "SHA-256".equals(event.getAlgorithm())
                && Arrays.equals(saved.getAuditEventHash(), unhex(event.getEventHash())), "event-binding-conflict");
        require(event.getCanonicalEventJson() != null
                && hash(event.getCanonicalEventJson()).equals(event.getEventHash()), "event-hash-conflict");
        // Validate protocol bindings against the authoritative hash-bearing event, not a
        // reconstruction of timestamps rounded by the database's timestamp precision.
        JsonNode canonicalEvent = tree(event.getCanonicalEventJson());
        var bean = new BeanWrapperImpl(event);
        for (String field : List.of("tenantId", "operationId", "subjectId", "subjectVersion", "actorId",
                "reason", "idempotencyKey", "resultStatus", "eventSchemaVersion", "canonicalizationVersion",
                "algorithm", "evidenceManifestJson", "beforeStateJson", "afterStateJson", "statePayloadHash")) {
            // Parse both sides identically: Java Long width is not a JSON type difference.
            // Strict tree equality still rejects strings, fractional numbers and missing fields.
            require(Objects.equals(canonicalEvent.get(field), tree(canonical(bean.getPropertyValue(field)))),
                    "event-columns-conflict");
        }
        require(tree(event.getEvidenceManifestJson()).equals(tree(canonical(List.of(evidence(expectedWithId(expected, saved.getId())))))),
                "receipt-evidence-conflict");
    }

    private static PermissionCommandReceiptDO expectedWithId(PermissionCommandReceiptDO expected, Long id) {
        expected.setId(id);
        return expected;
    }

    private static GxpAuditEvidence evidence(PermissionCommandReceiptDO receipt) {
        return new GxpAuditEvidence(EVIDENCE_TYPE, receipt.getId().toString(), "1",
                hash(canonical(tree(receipt.getSourceJson()))), "COMMAND_SOURCE");
    }

    /** Closed domain data. Collection IDs are numeric-sorted decimal strings, not JSON numbers. */
    static void validateData(String operation, Long id, Map<String, Object> data) {
        Set<String> fields = switch (operation) {
            case "system.permission.role-menu.assign" -> Set.of("roleId", "menuIds");
            case "system.permission.user-role.assign" -> Set.of("userId", "roleIds");
            case "system.permission.role-data-scope.assign" -> Set.of("roleId", "dataScope", "dataScopeDeptIds");
            default -> throw new IllegalArgumentException("Unsupported permission operation");
        };
        require(data.keySet().equals(fields), "domain-fields");
        require(id.toString().equals(data.get(fields.contains("userId") ? "userId" : "roleId")), "domain-subject");
    }

    static List<String> ids(Collection<Long> values) {
        if (values == null) return List.of(); // Existing assign semantics: null requests clear the set.
        TreeSet<Long> sorted = new TreeSet<>();
        for (Long value : values) {
            require(value != null && value > 0, "invalid-domain-id");
            sorted.add(value);
        }
        return sorted.stream().map(Object::toString).toList();
    }

    public static byte[] validateKey(String key) {
        require(!StrUtil.isBlank(key), "source-key-required");
        byte[] bytes = utf8(key);
        require(bytes.length >= 1 && bytes.length <= 1024, "source-key-utf8-length");
        return bytes;
    }

    private static byte[] utf8(String text) {
        try {
            var buffer = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            byte[] result = new byte[buffer.remaining()]; buffer.get(result); return result;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid UTF-8 permission input", e);
        }
    }
    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    private static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }
    private static String hash(String text) { return hex(digest(utf8(text))); }
    private static byte[] unhex(String hash) {
        require(hash != null && hash.matches("[0-9a-f]{64}"), "invalid-sha256");
        return HexFormat.of().parseHex(hash);
    }
    private static JsonNode tree(String text) {
        require(text != null, "missing-json");
        try { JsonNode node = JSON.readTree(text); require(node != null, "missing-json"); return node; }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid permission receipt JSON", e); }
    }
    private static String canonical(Object value) {
        try {
            // Conversion through maps also sorts fields read from MySQL's reordered JSON representation.
            return JSON.writeValueAsString(value instanceof JsonNode node ? JSON.convertValue(node, Object.class) : value);
        } catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid permission canonical data", e); }
    }
    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("Permission command rejected: " + reason);
    }
}
