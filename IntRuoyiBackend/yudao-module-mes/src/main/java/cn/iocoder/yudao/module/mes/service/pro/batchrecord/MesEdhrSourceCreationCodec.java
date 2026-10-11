package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static java.util.Map.entry;

/** Dedicated MES-OC-JSON-V1 and OC-ID-V1 byte protocols, independent of owner and web serializers. */
public final class MesEdhrSourceCreationCodec {

    private static final String CODEC_VERSION = "MES-OC-JSON-V1";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final DateTimeFormatter SECOND = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);
    private static final CharacterEscapes ESCAPES = new CanonicalEscapes();
    private static final Set<String> ENTRY_TYPES = Set.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT");
    private static final List<String> MATERIAL_TYPES = List.of("FINISHED_PRODUCT_INSPECTION_RECORD",
            "FINISHED_PRODUCT_INSPECTION_REPORT", "INCOMING_INSPECTION_REPORT", "STERILIZATION_REPORT");

    private enum Shape { ISSUE_REQUEST, ISSUE_STATE, REVOKE_REQUEST, REVOKE_STATE, MATERIAL_REQUEST, MATERIAL_STATE, EVIDENCE, SUBMISSION_ASSOCIATION }

    @FunctionalInterface
    private interface Rule {
        JsonNode validate(JsonNode value, String path);
    }

    private static final Rule P = (value, path) -> positiveInteger(value, path, Long.MAX_VALUE, false);
    private static final Rule I = (value, path) -> positiveInteger(value, path, Integer.MAX_VALUE, true);
    private static final Rule S = (value, path) -> string(value, path, true);
    private static final Rule OPTIONAL_STRING = (value, path) -> value != null && value.isNull()
            ? NullNode.instance : string(value, path, false);
    private static final Rule EXPLICIT_NULL = (value, path) -> {
        if (value == null || !value.isNull()) {
            throw invalid(path, "EXPLICIT_NULL_REQUIRED");
        }
        return NullNode.instance;
    };
    private static final Rule H = (value, path) -> {
        JsonNode text = string(value, path, true);
        requireHash(text.textValue(), path);
        return text;
    };
    private static final Rule T = (value, path) -> {
        JsonNode text = string(value, path, true);
        try {
            LocalDateTime parsed = LocalDateTime.parse(text.textValue(), SECOND);
            if (!formatSecond(parsed).equals(text.textValue())) {
                throw invalid(path, "NON_CANONICAL_SECOND");
            }
        } catch (DateTimeParseException e) {
            throw invalid(path, "INVALID_SECOND", e);
        }
        return text;
    };

    private static final Map<String, Rule> ISSUE_REQUEST = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("requestKind", fixed("INDEPENDENT_ISSUE_REQUEST_V1")),
            entry("tenantId", P), entry("authenticatedActor", P), entry("issuerSystem", S),
            entry("entryType", (value, path) -> {
                JsonNode text = string(value, path, true);
                if (!ENTRY_TYPES.contains(text.textValue())) {
                    throw invalid(path, "INVALID_ENTRY_TYPE");
                }
                return text;
            }), entry("workOrderId", P), entry("workOrderCode", S), entry("routeId", P),
            entry("routeVersionId", P), entry("routeVersion", S), entry("batchCode", S),
            entry("sourceRelationId", S), entry("sourceRelationVersion", S), entry("sourceRelationSnapshotHash", H),
            entry("sourceObjectType", S), entry("sourceObjectId", S), entry("materialSourceType", S),
            entry("materialSourceId", S), entry("sourceContextHash", H), entry("sourceSnapshotHash", H),
            entry("businessReason", S), entry("idempotencyKey", S), entry("sourceEvidence", MesEdhrSourceCreationCodec::evidence));
    private static final Map<String, Rule> EVIDENCE = Map.ofEntries(
            entry("sourceType", S), entry("sourceId", S), entry("sourceVersion", S), entry("sourceSnapshotHash", H),
            entry("payloadHash", H), entry("signature", S), entry("relationStatus", S),
            entry("sourceObjectType", OPTIONAL_STRING), entry("sourceObjectId", OPTIONAL_STRING),
            entry("snapshotJson", OPTIONAL_STRING), entry("sourceIdentityKey", OPTIONAL_STRING), entry("relationReason", OPTIONAL_STRING));
    private static final Map<String, Rule> ISSUE_STATE = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("stateKind", fixed("ISSUE_STATE_V1")),
            entry("request", nested(Shape.ISSUE_REQUEST)), entry("sourceRowId", P), entry("receiptId", S),
            entry("credentialVersion", P), entry("issuerUserRole", fixed("BACKEND_CONTROLLED")),
            entry("issuedAt", T), entry("expiresAt", T), entry("status", fixed("ISSUED")),
            entry("revokedAt", EXPLICIT_NULL), entry("revocationReason", EXPLICIT_NULL),
            entry("legacyOwnerAuditToken", S), entry("canonicalPayloadRaw", S), entry("sourceEvidenceJsonRaw", S),
            entry("payloadHash", H), entry("signature", S), entry("receiptHash", H),
            entry("requestHashVersion", fixed("INDEPENDENT_ISSUE_REQUEST_V1")), entry("requestHash", H));
    private static final Map<String, Rule> REVOKE_REQUEST = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("requestKind", fixed("INDEPENDENT_REVOKE_REQUEST_V1")),
            entry("tenantId", P), entry("authenticatedActor", P), entry("receiptId", S), entry("credentialVersion", P), entry("reason", S));
    private static final Map<String, Rule> REVOKE_STATE = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("stateKind", fixed("REVOKE_STATE_V1")),
            entry("creationState", nested(Shape.ISSUE_STATE)), entry("creationEventId", P), entry("creationEventHash", H),
            entry("revocationRequest", nested(Shape.REVOKE_REQUEST)), entry("status", fixed("REVOKED")),
            entry("revokedAt", T), entry("revocationReason", S),
            entry("requestHashVersion", fixed("INDEPENDENT_REVOKE_REQUEST_V1")), entry("requestHash", H));
    private static final Map<String, Rule> MATERIAL_REQUEST = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("requestKind", fixed("MATERIAL_READY_REQUEST_V1")),
            entry("tenantId", P), entry("batchExecutionId", P), entry("version", I), entry("issuedBy", P),
            entry("sourceSnapshotHash", H), entry("manifestHash", H), entry("materialVersionSetHash", H),
            entry("materialTypeKeys", MesEdhrSourceCreationCodec::materials));
    private static final Map<String, Rule> MATERIAL_STATE = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("stateKind", fixed("MATERIAL_READY_STATE_V1")),
            entry("request", nested(Shape.MATERIAL_REQUEST)), entry("sourceRowId", P), entry("receiptId", S),
            entry("gateStatus", fixed("MATERIALS_READY")), entry("materialTypeKeysJsonRaw", S), entry("legacyOwnerAuditToken", S),
            entry("receiptHash", H), entry("requestHashVersion", fixed("MATERIAL_READY_REQUEST_V1")), entry("requestHash", H));

    private MesEdhrSourceCreationCodec() {
    }

    private static final Map<String, Rule> SUBMISSION_ASSOCIATION = Map.ofEntries(
            entry("codecVersion", fixed(CODEC_VERSION)), entry("associationProtocol", fixed("SUBMISSION_MATERIAL_ASSOCIATION_V1")),
            entry("associationId", S), entry("associationVersion", fixedIntegerOne()), entry("tenantId", P),
            entry("releaseTransactionId", P), entry("batchExecutionId", P), entry("submissionEventId", P),
            entry("submissionTransactionVersion", P), entry("approvalWorkTaskId", P), entry("sourceMethod", S),
            entry("submitIdempotencyKey", S), entry("submittedBy", P), entry("submissionEvidenceHash", H),
            entry("precheckSnapshotHash", H), entry("materialSourceRowId", P), entry("materialReceiptId", S),
            entry("materialVersion", P), entry("materialReceiptHash", H), entry("materialManifestHash", H),
            entry("sourceSnapshotHash", H), entry("materialCreationEventId", P), entry("materialCreationEventHash", H));

    private static Rule fixedIntegerOne() {
        return (value, path) -> {
            if (value == null || !value.isIntegralNumber() || value.longValue() != 1) throw invalid(path, "VERSION_ONE_REQUIRED");
            return IntNode.valueOf(1);
        };
    }

    public static Encoded encodeSubmissionMaterialAssociationV1(JsonNode input) {
        Encoded encoded = encode(input, Shape.SUBMISSION_ASSOCIATION);
        String id = input.get("associationId").textValue();
        if (!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || !Set.of("submit", "submitForApproval").contains(input.get("sourceMethod").textValue()))
            throw invalid("$", "SUBMISSION_ASSOCIATION_IDENTITY_INVALID");
        return encoded;
    }

    public static String submissionMaterialKeyDigest(long tenant, long release, String key) {
        positive(tenant, "tenantId"); positive(release, "releaseTransactionId"); requiredString(key, "submitIdempotencyKey");
        return identity("", "SUBMISSION_MATERIAL", Long.toString(tenant), Long.toString(release), key);
    }

    public static String managedHandoffAuditKey(long tenant, long application, String method, long task, int expectedVersion, String key) {
        positive(tenant, "tenantId"); positive(application, "applicationId"); positive(task, "sourceWorkTaskId");
        if (expectedVersion < 0 || !Set.of("MANAGED_PQC_APPROVE", "MANAGED_REPORT_COMPLETE").contains(method)) throw invalid("$", "HANDOFF_IDENTITY_INVALID");
        requiredString(key, "sourceIdempotencyKey");
        return identity("MH1:", "MANAGED_HANDOFF", Long.toString(tenant), Long.toString(application), method,
                Long.toString(task), Integer.toString(expectedVersion), key);
    }

    public static Encoded encodeManagedHandoffMaterialAssociationV1(JsonNode input) {
        Set<String> longs = Set.of("tenantId", "applicationId", "batchExecutionId", "releaseTransactionId", "managerWorkTaskId",
                "sourceActorId", "sourceWorkTaskId", "materialSourceRowId", "materialCreationEventId");
        Set<String> integers = Set.of("releaseTransactionVersion", "materialVersion", "sourceExpectedApplicationVersion", "initializedApplicationVersion");
        Set<String> hashes = Set.of("sourcePayloadHash", "handoffRequestHash", "businessReadinessSnapshotHash", "precheckSnapshotHash",
                "materialReceiptHash", "materialManifestHash", "sourceSnapshotHash", "materialCreationEventHash");
        Set<String> strings = Set.of("sourceIdempotencyKey", "reportSnapshotHash", "businessReadinessSnapshotJsonRaw", "managerCandidateSnapshotHash",
                "materialReceiptId", "sourceMethod", "associationId", "codecVersion", "associationProtocol");
        Set<String> keys = new HashSet<>(longs); keys.addAll(integers); keys.addAll(hashes); keys.addAll(strings); keys.add("associationVersion");
        if (input == null || !input.isObject() || input.size() != keys.size()) throw invalid("$", "EXACT_FIELD_SET_REQUIRED");
        ObjectNode result = MAPPER.createObjectNode();
        for (String key : new TreeSet<>(keys)) {
            JsonNode value = input.get(key);
            if (longs.contains(key)) value = P.validate(value, key);
            else if (integers.contains(key)) value = positiveInteger(value, key, Integer.MAX_VALUE, key.startsWith("sourceExpected") || key.startsWith("initialized"));
            else if (hashes.contains(key)) value = H.validate(value, key);
            else if ("associationVersion".equals(key)) value = fixedIntegerOne().validate(value, key);
            else value = S.validate(value, key);
            result.set(key, value);
        }
        if (!CODEC_VERSION.equals(result.get("codecVersion").textValue())
                || !"MANAGED_HANDOFF_MATERIAL_ASSOCIATION_V1".equals(result.get("associationProtocol").textValue())
                || !result.get("associationId").textValue().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || !Set.of("MANAGED_PQC_APPROVE", "MANAGED_REPORT_COMPLETE").contains(result.get("sourceMethod").textValue()))
            throw invalid("$", "HANDOFF_PROTOCOL_INVALID");
        return encodeDedicatedObject(result);
    }

    public static Encoded encodeManagedHandoffRequestV1(JsonNode input) {
        Set<String> keys = Set.of("codecVersion", "sourceMethod", "tenantId", "applicationId", "batchExecutionId",
                "sourceWorkTaskId", "sourceActorId", "sourceExpectedApplicationVersion", "sourceIdempotencyKey", "sourcePayloadHash",
                "initializedApplicationVersion", "reportSnapshotHash", "reportEvidenceOwnerHashes");
        Set<String> actual = new HashSet<>(); if (input != null && input.isObject()) input.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(keys)) throw invalid("$", "EXACT_FIELD_SET_REQUIRED");
        for (String key : Set.of("tenantId", "applicationId", "batchExecutionId", "sourceWorkTaskId", "sourceActorId")) P.validate(input.get(key), key);
        for (String key : Set.of("sourceExpectedApplicationVersion", "initializedApplicationVersion")) positiveInteger(input.get(key), key, Integer.MAX_VALUE, true);
        H.validate(input.get("sourcePayloadHash"), "sourcePayloadHash");
        S.validate(input.get("sourceIdempotencyKey"), "sourceIdempotencyKey"); S.validate(input.get("reportSnapshotHash"), "reportSnapshotHash");
        if (!CODEC_VERSION.equals(input.path("codecVersion").asText())
                || !Set.of("MANAGED_PQC_APPROVE", "MANAGED_REPORT_COMPLETE").contains(input.path("sourceMethod").asText())
                || !input.path("reportEvidenceOwnerHashes").isArray()) throw invalid("$", "HANDOFF_PROTOCOL_INVALID");
        String previous = null;
        for (JsonNode item : input.get("reportEvidenceOwnerHashes")) {
            if (!item.isObject() || item.size() != 2 || !item.has("type") || !item.has("ownerHash")) throw invalid("$", "REPORT_OWNER_FIELDS_INVALID");
            String type = S.validate(item.get("type"), "type").textValue(); H.validate(item.get("ownerHash"), "ownerHash");
            if (previous != null && previous.compareTo(type) >= 0) throw invalid("$", "REPORT_OWNER_ORDER_INVALID"); previous = type;
        }
        int expected = "MANAGED_PQC_APPROVE".equals(input.path("sourceMethod").asText()) ? 0 : 4;
        if (input.get("reportEvidenceOwnerHashes").size() != expected) throw invalid("$", "REPORT_OWNER_COUNT_INVALID");
        return encodeDedicatedObject(input);
    }
    /** Dedicated protocol trees only; preserves native integer/null types and sorts every object. */
    public static Encoded encodeDedicatedObject(JsonNode input) {
        if (input == null || !input.isObject()) throw invalid("$", "OBJECT_REQUIRED");
        String json = canonicalJson(sortDedicated(input));
        return new Encoded(json, digest(strictUtf8(json, "$")));
    }
    private static JsonNode sortDedicated(JsonNode input) {
        if (input.isObject()) {
            ObjectNode output = MAPPER.createObjectNode(); java.util.List<String> keys = new java.util.ArrayList<>();
            input.fieldNames().forEachRemaining(keys::add); java.util.Collections.sort(keys);
            for (String key : keys) output.set(key, sortDedicated(input.get(key))); return output;
        }
        if (input.isArray()) { ArrayNode output = MAPPER.createArrayNode(); for (JsonNode value : input) output.add(sortDedicated(value)); return output; }
        if (input.isFloatingPointNumber()) throw invalid("$", "DEDICATED_INTEGER_REQUIRED");
        return input.deepCopy();
    }

    public static String issueAuditKey(long tenantId, String entryType, String rawIdempotencyKey) {
        positive(tenantId, "tenantId");
        requiredString(entryType, "entryType");
        if (!ENTRY_TYPES.contains(entryType)) {
            throw invalid("entryType", "INVALID_ENTRY_TYPE");
        }
        requiredString(rawIdempotencyKey, "idempotencyKey");
        return identity("OI1:", "ISSUE", Long.toString(tenantId), entryType, rawIdempotencyKey);
    }

    public static String revokeAuditKey(long tenantId, String receiptId, long credentialVersion) {
        positive(tenantId, "tenantId");
        positive(credentialVersion, "credentialVersion");
        requiredString(receiptId, "receiptId");
        return identity("OR1:", "REVOKE", Long.toString(tenantId), receiptId, Long.toString(credentialVersion));
    }

    public static String materialCreateAuditKey(long tenantId, long batchExecutionId, int version) {
        positive(tenantId, "tenantId");
        positive(batchExecutionId, "batchExecutionId");
        positive(version, "version");
        return identity("OM1:", "MATERIAL_CREATE", Long.toString(tenantId), Long.toString(batchExecutionId), Integer.toString(version));
    }

    public static Encoded encodeIssueRequestV1(JsonNode input) { return encode(input, Shape.ISSUE_REQUEST); }
    public static Encoded encodeIssueStateV1(JsonNode input) { return encode(input, Shape.ISSUE_STATE); }
    public static Encoded encodeRevokeRequestV1(JsonNode input) { return encode(input, Shape.REVOKE_REQUEST); }
    public static Encoded encodeRevokeStateV1(JsonNode input) { return encode(input, Shape.REVOKE_STATE); }
    public static Encoded encodeMaterialReadyRequestV1(JsonNode input) { return encode(input, Shape.MATERIAL_REQUEST); }
    public static Encoded encodeMaterialReadyStateV1(JsonNode input) { return encode(input, Shape.MATERIAL_STATE); }
    public static ObjectNode decodeIssueRequestV1(byte[] input) { return decode(input, Shape.ISSUE_REQUEST); }
    public static ObjectNode decodeIssueStateV1(byte[] input) { return decode(input, Shape.ISSUE_STATE); }
    public static ObjectNode decodeRevokeRequestV1(byte[] input) { return decode(input, Shape.REVOKE_REQUEST); }
    public static ObjectNode decodeRevokeStateV1(byte[] input) { return decode(input, Shape.REVOKE_STATE); }
    public static ObjectNode decodeMaterialReadyRequestV1(byte[] input) { return decode(input, Shape.MATERIAL_REQUEST); }
    public static ObjectNode decodeMaterialReadyStateV1(byte[] input) { return decode(input, Shape.MATERIAL_STATE); }

    public static String formatSecond(LocalDateTime value) {
        if (value == null || value.getNano() != 0) {
            throw invalid("time", "SECOND_PRECISION_REQUIRED");
        }
        return SECOND.format(value);
    }

    /** Immutable text and digest; callers receive a new UTF-8 byte array on every access. */
    public record Encoded(String canonicalJson, String sha256) {
        public Encoded {
            requiredString(canonicalJson, "canonicalJson");
            requireHash(sha256, "sha256");
            if (!digest(strictUtf8(canonicalJson, "canonicalJson")).equals(sha256)) {
                throw invalid("sha256", "DIGEST_MISMATCH");
            }
        }

        public byte[] utf8Bytes() {
            return strictUtf8(canonicalJson, "canonicalJson");
        }
    }

    private static Encoded encode(JsonNode input, Shape shape) {
        String json = canonicalJson(canonicalObject(input, shape, "$"));
        return new Encoded(json, digest(strictUtf8(json, "$")));
    }

    private static ObjectNode decode(byte[] input, Shape shape) {
        if (input == null) {
            throw invalid("$", "UTF8_BYTES_REQUIRED");
        }
        byte[] original = input.clone();
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(original)).toString();
        } catch (CharacterCodingException e) {
            throw invalid("$", "INVALID_UTF8", e);
        }
        ObjectNode result;
        try (JsonParser parser = FACTORY.createParser(text)) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parser.nextToken() != null) {
                throw invalid("$", "MULTIPLE_ROOTS");
            }
            result = canonicalObject(parsed, shape, "$");
        } catch (IOException e) {
            throw invalid("$", "INVALID_JSON", e);
        }
        if (!Arrays.equals(original, strictUtf8(canonicalJson(result), "$"))) {
            throw invalid("$", "NON_CANONICAL_BYTES");
        }
        return result;
    }

    private static ObjectNode canonicalObject(JsonNode input, Shape shape, String path) {
        if (input == null || !input.isObject()) {
            throw invalid(path, "OBJECT_REQUIRED");
        }
        Map<String, Rule> schema = switch (shape) {
            case ISSUE_REQUEST -> ISSUE_REQUEST;
            case ISSUE_STATE -> ISSUE_STATE;
            case REVOKE_REQUEST -> REVOKE_REQUEST;
            case REVOKE_STATE -> REVOKE_STATE;
            case MATERIAL_REQUEST -> MATERIAL_REQUEST;
            case MATERIAL_STATE -> MATERIAL_STATE;
            case EVIDENCE -> EVIDENCE;
            case SUBMISSION_ASSOCIATION -> SUBMISSION_ASSOCIATION;
        };
        Set<String> actual = new HashSet<>();
        input.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(schema.keySet())) {
            throw invalid(path, "EXACT_FIELD_SET_REQUIRED");
        }
        ObjectNode result = MAPPER.createObjectNode();
        for (String key : new TreeSet<>(schema.keySet())) {
            result.set(key, schema.get(key).validate(input.get(key), path + "." + key));
        }
        switch (shape) {
            case ISSUE_STATE, MATERIAL_STATE -> checkRequestHash(result, "request", path);
            case REVOKE_STATE -> {
                checkRequestHash(result, "revocationRequest", path);
                JsonNode creation = result.get("creationState");
                JsonNode request = result.get("revocationRequest");
                same(request.get("tenantId"), creation.get("request").get("tenantId"), path + ".revocationRequest.tenantId");
                same(request.get("receiptId"), creation.get("receiptId"), path + ".revocationRequest.receiptId");
                same(request.get("credentialVersion"), creation.get("credentialVersion"), path + ".revocationRequest.credentialVersion");
                same(result.get("revocationReason"), request.get("reason"), path + ".revocationReason");
            }
            default -> { }
        }
        return result;
    }

    private static void checkRequestHash(ObjectNode value, String requestKey, String path) {
        String expected = digest(strictUtf8(canonicalJson(value.get(requestKey)), path + "." + requestKey));
        if (!expected.equals(value.get("requestHash").textValue())) {
            throw invalid(path + ".requestHash", "REQUEST_DIGEST_MISMATCH");
        }
    }

    private static void same(JsonNode first, JsonNode second, String path) {
        if (!first.equals(second)) {
            throw invalid(path, "IDENTITY_MISMATCH");
        }
    }

    private static Rule nested(Shape shape) {
        return (value, path) -> canonicalObject(value, shape, path);
    }

    private static Rule fixed(String expected) {
        return (value, path) -> {
            JsonNode text = string(value, path, true);
            if (!expected.equals(text.textValue())) {
                throw invalid(path, "FIXED_VALUE_REQUIRED");
            }
            return text;
        };
    }

    private static JsonNode positiveInteger(JsonNode value, String path, long max, boolean intValue) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()
                || value.longValue() <= 0 || value.longValue() > max) {
            throw invalid(path, "POSITIVE_INTEGER_REQUIRED");
        }
        return intValue ? IntNode.valueOf(value.intValue()) : LongNode.valueOf(value.longValue());
    }

    private static JsonNode string(JsonNode value, String path, boolean nonblank) {
        if (value == null || !value.isTextual()) {
            throw invalid(path, "STRING_REQUIRED");
        }
        String text = value.textValue();
        if (text == null) {
            throw invalid(path, "STRING_REQUIRED");
        }
        if (nonblank && text.isBlank()) {
            throw invalid(path, "NONBLANK_REQUIRED");
        }
        strictUtf8(text, path);
        return TextNode.valueOf(text);
    }

    private static ArrayNode evidence(JsonNode value, String path) {
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw invalid(path, "NONEMPTY_EVIDENCE_REQUIRED");
        }
        List<ObjectNode> items = new ArrayList<>();
        for (JsonNode item : value) {
            items.add(canonicalObject(item, Shape.EVIDENCE, path + "[]"));
        }
        items.sort(MesEdhrSourceCreationCodec::compareEvidence);
        ArrayNode result = MAPPER.createArrayNode();
        ObjectNode previous = null;
        for (ObjectNode item : items) {
            if (previous != null && compareEvidence(previous, item) == 0) {
                throw invalid(path, "DUPLICATE_EVIDENCE_IDENTITY");
            }
            result.add(item);
            previous = item;
        }
        return result;
    }

    private static int compareEvidence(ObjectNode first, ObjectNode second) {
        int sourceType = Arrays.compareUnsigned(strictUtf8(first.get("sourceType").textValue(), "sourceEvidence.sourceType"),
                strictUtf8(second.get("sourceType").textValue(), "sourceEvidence.sourceType"));
        return sourceType != 0 ? sourceType : Arrays.compareUnsigned(
                strictUtf8(first.get("sourceId").textValue(), "sourceEvidence.sourceId"),
                strictUtf8(second.get("sourceId").textValue(), "sourceEvidence.sourceId"));
    }

    private static ArrayNode materials(JsonNode value, String path) {
        if (value == null || !value.isArray() || value.size() != MATERIAL_TYPES.size()) {
            throw invalid(path, "FOUR_MATERIAL_TYPES_REQUIRED");
        }
        Set<String> actual = new HashSet<>();
        for (JsonNode item : value) {
            String type = string(item, path + "[]", true).textValue();
            if (!actual.add(type)) {
                throw invalid(path, "DUPLICATE_MATERIAL_TYPE");
            }
        }
        if (!actual.equals(Set.copyOf(MATERIAL_TYPES))) {
            throw invalid(path, "EXACT_MATERIAL_TYPES_REQUIRED");
        }
        ArrayNode result = MAPPER.createArrayNode();
        MATERIAL_TYPES.forEach(result::add);
        return result;
    }

    private static String canonicalJson(JsonNode value) {
        StringWriter writer = new StringWriter();
        // A character generator keeps supplementary Unicode as pairs until strict UTF-8 encoding.
        try (JsonGenerator generator = FACTORY.createGenerator(writer)) {
            generator.setCharacterEscapes(ESCAPES);
            generator.setHighestNonEscapedChar(0);
            write(generator, value);
        } catch (IOException e) {
            throw invalid("$", "JSON_ENCODING_FAILED", e);
        }
        return writer.toString();
    }

    private static void write(JsonGenerator generator, JsonNode value) throws IOException {
        if (value.isObject()) {
            generator.writeStartObject();
            for (var fields = value.fields(); fields.hasNext();) {
                var field = fields.next();
                generator.writeFieldName(field.getKey());
                write(generator, field.getValue());
            }
            generator.writeEndObject();
        } else if (value.isArray()) {
            generator.writeStartArray();
            for (JsonNode item : value) {
                write(generator, item);
            }
            generator.writeEndArray();
        } else if (value.isTextual()) {
            generator.writeString(value.textValue());
        } else if (value.isIntegralNumber()) {
            generator.writeNumber(value.longValue());
        } else if (value.isNull()) {
            generator.writeNull();
        } else {
            throw invalid("$", "UNSUPPORTED_CANONICAL_NODE");
        }
    }

    private static String identity(String prefix, String tag, String... tuple) {
        StringBuilder bytes = new StringBuilder("MES-OC-ID-V1\0").append(tag).append('\0')
                .append(tuple.length).append(':');
        for (String part : tuple) {
            bytes.append(strictUtf8(part, "identity").length).append(':').append(part);
        }
        return prefix + digest(strictUtf8(bytes.toString(), "identity"));
    }

    private static void positive(long value, String path) {
        if (value <= 0) {
            throw invalid(path, "POSITIVE_INTEGER_REQUIRED");
        }
    }

    private static void requiredString(String value, String path) {
        if (value == null || value.isBlank()) {
            throw invalid(path, "NONBLANK_REQUIRED");
        }
        strictUtf8(value, path);
    }

    private static void requireHash(String value, String path) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw invalid(path, "LOWERCASE_SHA256_REQUIRED");
        }
    }

    private static byte[] strictUtf8(String value, String path) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException e) {
            throw invalid(path, "INVALID_UTF8_STRING", e);
        }
    }

    private static String digest(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    private static IllegalArgumentException invalid(String path, String category) {
        return new IllegalArgumentException(path + ": " + category);
    }

    private static IllegalArgumentException invalid(String path, String category, Exception cause) {
        return new IllegalArgumentException(path + ": " + category, cause);
    }

    private static final class CanonicalEscapes extends CharacterEscapes {
        private final int[] ascii = CharacterEscapes.standardAsciiEscapesForJSON();

        private CanonicalEscapes() {
            for (int c = 0; c < 32; c++) {
                if (c != '\b' && c != '\t' && c != '\n' && c != '\f' && c != '\r') {
                    ascii[c] = ESCAPE_CUSTOM;
                }
            }
        }

        @Override
        public int[] getEscapeCodesForAscii() { return ascii.clone(); }

        @Override
        public SerializableString getEscapeSequence(int c) {
            if (c >= 0 && c < 32) {
                return new SerializedString(String.format(Locale.ROOT, "\\u00%02X", c));
            }
            return null; // Non-control characters have no custom escape in this byte protocol.
        }
    }
}
