package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class MesProBatchRecordExecutionFieldAuditHasher {

    public static final String HASH_VERSION_PREFIX = "EDHR_FIELD_AUDIT_V1";
    public static final String GENESIS_HEAD_HASH = sha256(HASH_VERSION_PREFIX + ":GENESIS");

    private static final ObjectMapper CANONICAL_MAPPER = JsonUtils.getObjectMapper().copy()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private MesProBatchRecordExecutionFieldAuditHasher() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for eDHR field audit", e);
        }
    }

    public static String hashTypedValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        return sha256(HASH_VERSION_PREFIX + ":VALUE\n" + canonicalizeTypedValue(valueType, value));
    }

    public static String hashCanonicalTypedValue(String canonicalValueJson) {
        requireNonBlank(canonicalValueJson, "canonicalValueJson");
        return sha256(HASH_VERSION_PREFIX + ":VALUE\n" + canonicalValueJson);
    }

    public static String hashCellValues(String cellValuesJson) {
        requireNonBlank(cellValuesJson, "cellValuesJson");
        return sha256(HASH_VERSION_PREFIX + ":CELL_VALUES\n" + canonicalizeJsonString(cellValuesJson));
    }

    public static String hashExecutionSnapshot(String executionSnapshotJson) {
        requireNonBlank(executionSnapshotJson, "executionSnapshotJson");
        return sha256(HASH_VERSION_PREFIX + ":EXECUTION_SNAPSHOT\n" + canonicalizeJsonString(executionSnapshotJson));
    }

    public static String hashItem(MesProBatchRecordExecutionFieldAuditItemHashInput input) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        putRequired(root, "fieldPath", input.getFieldPath());
        putRequired(root, "fieldKey", input.getFieldKey());
        root.put("rowIndex", requireNonNull(input.getRowIndex(), "rowIndex"));
        root.put("columnIndex", requireNonNull(input.getColumnIndex(), "columnIndex"));
        putRequired(root, "valueType", requireNonNull(input.getValueType(), "valueType").name());
        putRequired(root, "oldValueJson", input.getOldValueJson());
        putRequiredAllowEmpty(root, "oldValueDisplay", input.getOldValueDisplay());
        putRequired(root, "oldValueHash", input.getOldValueHash());
        putRequired(root, "newValueJson", input.getNewValueJson());
        putRequiredAllowEmpty(root, "newValueDisplay", input.getNewValueDisplay());
        putRequired(root, "newValueHash", input.getNewValueHash());
        putRequired(root, "reasonCategory", input.getReasonCategory());
        putRequired(root, "reasonText", input.getReasonText());
        root.put("actorId", requireNonNull(input.getActorId(), "actorId"));
        putRequired(root, "actorName", input.getActorName());
        putRequired(root, "signatureProjectionHash", input.getSignatureProjectionHash());
        putRequired(root, "previousHash", input.getPreviousHash());
        putRequired(root, "changedAt", formatChangedAt(input.getChangedAt()));
        return sha256(HASH_VERSION_PREFIX + ":ITEM\n" + canonicalize(root));
    }

    public static String hashRequest(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("executionId", requireNonNull(command.getExecutionId(), "executionId"));
        putRequired(root, "idempotencyKey", command.getIdempotencyKey());
        putRequired(root, "baseCellValuesHash", command.getBaseCellValuesHash());
        root.put("baseFieldAuditRevision", requireNonNull(command.getBaseFieldAuditRevision(), "baseFieldAuditRevision"));
        putRequired(root, "baseFieldAuditHeadHash", command.getBaseFieldAuditHeadHash());
        putRequired(root, "reasonCategory", command.getReasonCategory());
        putRequired(root, "reasonText", StrUtil.trim(command.getReasonText()));
        root.set("changes", CANONICAL_MAPPER.valueToTree(command.getChanges()));
        root.set("attachmentChanges", CANONICAL_MAPPER.valueToTree(command.getAttachmentChanges()));
        ObjectNode signature = JsonNodeFactory.instance.objectNode();
        signature.put("passwordPresent", command.getSignature() != null && StrUtil.isNotBlank(command.getSignature().getPassword()));
        putSignatureTime(signature, command.getSignature() == null ? null : command.getSignature().getSignatureTimeCommand());
        root.set("signature", signature);
        return sha256(HASH_VERSION_PREFIX + ":REQUEST\n" + canonicalize(root));
    }

    public static String hashSignatureChallenge(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                                Long actorId) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("executionId", requireNonNull(command.getExecutionId(), "executionId"));
        putRequired(root, "idempotencyKey", command.getIdempotencyKey());
        putRequired(root, "baseCellValuesHash", command.getBaseCellValuesHash());
        root.put("baseFieldAuditRevision", requireNonNull(command.getBaseFieldAuditRevision(), "baseFieldAuditRevision"));
        putRequired(root, "baseFieldAuditHeadHash", command.getBaseFieldAuditHeadHash());
        root.set("changes", CANONICAL_MAPPER.valueToTree(command.getChanges()));
        root.set("attachmentChanges", CANONICAL_MAPPER.valueToTree(command.getAttachmentChanges()));
        putRequired(root, "reasonCategory", command.getReasonCategory());
        putRequired(root, "reasonText", StrUtil.trim(command.getReasonText()));
        if (actorId != null) {
            root.put("actorId", actorId);
        }
        putSignatureTime(root, command.getSignature() == null ? null : command.getSignature().getSignatureTimeCommand());
        return sha256(HASH_VERSION_PREFIX + ":SIGNATURE_CHALLENGE\n" + canonicalize(root));
    }

    public static String hashSignatureProjection(MesProBatchRecordExecutionFieldAuditSignatureProjection projection) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("id", requireNonNull(projection.getId(), "id"));
        root.put("executionId", requireNonNull(projection.getExecutionId(), "executionId"));
        putRequired(root, "actionType", projection.getActionType());
        root.put("actorId", requireNonNull(projection.getActorId(), "actorId"));
        putRequired(root, "actorName", projection.getActorName());
        putRequired(root, "signatureMode", projection.getSignatureMode());
        root.put("passwordVerified", Boolean.TRUE.equals(projection.getPasswordVerified()));
        putRequired(root, "signedAt", formatChangedAt(projection.getSignedAt()));
        putRequiredAllowEmpty(root, "selectedSignedAt", formatOptionalChangedAt(projection.getSelectedSignedAt()));
        putRequiredAllowEmpty(root, "signatureDisplayAt", formatOptionalChangedAt(projection.getSignatureDisplayAt()));
        putRequiredAllowEmpty(root, "signatureTimeMode", projection.getSignatureTimeMode());
        putRequiredAllowEmpty(root, "selectedTimeZone", projection.getSelectedTimeZone());
        putRequiredAllowEmpty(root, "selectedTimeReason", projection.getSelectedTimeReason());
        putRequiredAllowEmpty(root, "selectedTimePolicyVersion", projection.getSelectedTimePolicyVersion());
        putRequiredAllowEmpty(root, "selectedTimeAuditHash", projection.getSelectedTimeAuditHash());
        putRequired(root, "reasonCategory", projection.getReasonCategory());
        putRequired(root, "reasonText", projection.getReasonText());
        root.put("auditBatchId", requireNonNull(projection.getAuditBatchId(), "auditBatchId"));
        putRequired(root, "signatureChallengeHash", projection.getSignatureChallengeHash());
        root.put("fieldAuditRevision", requireNonNull(projection.getFieldAuditRevision(), "fieldAuditRevision"));
        putRequired(root, "cellValuesHash", projection.getCellValuesHash());
        return sha256(HASH_VERSION_PREFIX + ":SIGNATURE_PROJECTION\n" + canonicalize(root));
    }

    private static void putSignatureTime(ObjectNode root, MesProBatchRecordExecutionSignatureTimeCommand command) {
        ObjectNode signatureTime = JsonNodeFactory.instance.objectNode();
        if (command != null) {
            putRequiredAllowEmpty(signatureTime, "selectedSignedAt", formatOptionalChangedAt(command.getSelectedSignedAt()));
            putRequiredAllowEmpty(signatureTime, "selectedTimeZone", command.getSelectedTimeZone());
            putRequiredAllowEmpty(signatureTime, "selectedTimeReason", command.getSelectedTimeReason());
        }
        root.set("signatureTime", signatureTime);
    }

    public static String canonicalizeTypedValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        JsonNode node = toJsonNode(value);
        return canonicalizeTypedValue(valueType, node);
    }

    public static String canonicalizeTypedValue(MesProBatchRecordExecutionFieldAuditValueType valueType, JsonNode node) {
        requireNonNull(valueType, "valueType");
        JsonNode normalized = switch (valueType) {
            case STRING -> {
                if (node == null || !node.isTextual()) {
                    throw new IllegalArgumentException("STRING field audit value must be a JSON string");
                }
                yield TextNode.valueOf(node.textValue());
            }
            case NUMBER -> {
                if (node == null || !node.isNumber()) {
                    throw new IllegalArgumentException("NUMBER field audit value must be a JSON number");
                }
                yield DecimalNode.valueOf(normalizeNumber(node.decimalValue()));
            }
            case BOOLEAN -> {
                if (node == null || !node.isBoolean()) {
                    throw new IllegalArgumentException("BOOLEAN field audit value must be a JSON boolean");
                }
                yield BooleanNode.valueOf(node.booleanValue());
            }
            case DATE, DATETIME -> {
                if (node == null || !node.isTextual() || StrUtil.isBlank(node.textValue())) {
                    throw new IllegalArgumentException(valueType.name() + " field audit value must be a nonblank JSON string");
                }
                yield TextNode.valueOf(node.textValue());
            }
            case JSON -> {
                if (node == null || node.isMissingNode() || node.isNull()) {
                    throw new IllegalArgumentException("JSON field audit value must be an explicit JSON value");
                }
                yield node;
            }
            case SIGNATURE -> {
                if (node == null || node.isMissingNode() || node.isNull()) {
                    throw new IllegalArgumentException("SIGNATURE field audit value must be an explicit JSON value");
                }
                yield node;
            }
            case NULL -> {
                if (node != null && !node.isNull()) {
                    throw new IllegalArgumentException("NULL field audit value must be JSON null");
                }
                yield NullNode.instance;
            }
        };
        return canonicalize(normalized);
    }

    public static String canonicalizeJsonString(String json) {
        try {
            return canonicalize(CANONICAL_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON for eDHR field audit canonicalization", e);
        }
    }

    public static String canonicalize(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "null";
        }
        if (node.isObject()) {
            TreeMap<String, String> fields = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                fields.put(entry.getKey(), canonicalize(entry.getValue()));
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(quote(entry.getKey())).append(':').append(entry.getValue());
            }
            return builder.append('}').toString();
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(canonicalize(node.get(i)));
            }
            return builder.append(']').toString();
        }
        if (node.isTextual()) {
            return quote(node.textValue());
        }
        if (node.isNumber()) {
            return normalizeNumber(node.decimalValue()).toPlainString();
        }
        if (node.isBoolean()) {
            return node.booleanValue() ? "true" : "false";
        }
        return node.toString();
    }

    public static JsonNode toJsonNode(Object value) {
        if (value == null) {
            return NullNode.instance;
        }
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        return CANONICAL_MAPPER.valueToTree(value);
    }

    public static BigDecimal normalizeNumber(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return BigDecimal.ZERO.compareTo(normalized) == 0 ? BigDecimal.ZERO : normalized;
    }

    private static String quote(String value) {
        try {
            return CANONICAL_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON string value for eDHR field audit", e);
        }
    }

    private static void putRequired(ObjectNode root, String fieldName, String value) {
        root.put(fieldName, requireNonBlank(value, fieldName));
    }

    private static void putRequiredAllowEmpty(ObjectNode root, String fieldName, String value) {
        root.put(fieldName, requireNonNull(value, fieldName));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required for eDHR field audit");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required for eDHR field audit");
        }
        return value;
    }

    private static String formatChangedAt(LocalDateTime changedAt) {
        return requireNonNull(changedAt, "changedAt").format(DATE_TIME_FORMATTER);
    }

    private static String formatOptionalChangedAt(LocalDateTime changedAt) {
        return changedAt == null ? "" : changedAt.format(DATE_TIME_FORMATTER);
    }
}
