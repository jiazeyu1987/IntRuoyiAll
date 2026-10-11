package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Original owner algorithms plus strict input syntax; does not normalize history or current eligibility. */
@Component
public class MesEdhrSourceOwnerIntegrity {
    private static final Set<String> EVIDENCE_FIELDS = Set.of("sourceType", "sourceId", "sourceVersion",
            "sourceSnapshotHash", "payloadHash", "signature", "sourceObjectType", "sourceObjectId",
            "snapshotJson", "sourceIdentityKey", "relationStatus", "relationReason");
    static final Set<String> MATERIAL_TYPES = Set.of("FINISHED_PRODUCT_INSPECTION_RECORD",
            "FINISHED_PRODUCT_INSPECTION_REPORT", "INCOMING_INSPECTION_REPORT", "STERILIZATION_REPORT");
    private final String issuerSystem;
    private final String signingSecret;

    public MesEdhrSourceOwnerIntegrity(
            @Value("${mes.pro.edhr.independent-receipt.issuer-system:}") String issuerSystem,
            @Value("${mes.pro.edhr.independent-receipt.signing-secret:}") String signingSecret) {
        this.issuerSystem = issuerSystem;
        this.signingSecret = signingSecret;
    }

    public ObjectNode independentRequest(MesIndependentBatchPrerequisiteReceiptDO row) {
        if (issuerSystem == null || issuerSystem.isBlank() || signingSecret == null || signingSecret.isBlank()) {
            throw fail("CONFIGURATION_MISSING");
        }
        require(Objects.equals(row.getIssuerSystem(), issuerSystem), "SOURCE_OWNER_INTEGRITY_FAILED");
        JsonNode evidence = strict(row.getSourceEvidenceJson());
        require(evidence.isArray() && !evidence.isEmpty(), "SOURCE_OWNER_INTEGRITY_FAILED");
        List<MesBatchExecutionSourceEvidence> items = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (JsonNode item : evidence) {
            keys(item, EVIDENCE_FIELDS);
            for (String field : List.of("sourceType", "sourceId", "sourceVersion", "sourceSnapshotHash",
                    "payloadHash", "signature", "relationStatus")) text(item.get(field));
            for (String field : List.of("sourceObjectType", "sourceObjectId", "snapshotJson", "sourceIdentityKey", "relationReason")) {
                require(item.get(field).isNull() || item.get(field).isTextual(), "SOURCE_OWNER_INTEGRITY_FAILED");
            }
            require(identities.add(item.get("sourceType").textValue() + "\u0000" + item.get("sourceId").textValue()),
                    "SOURCE_OWNER_INTEGRITY_FAILED");
            items.add(new MesBatchExecutionSourceEvidence().setSourceType(item.get("sourceType").textValue())
                    .setSourceId(item.get("sourceId").textValue()).setSourceVersion(item.get("sourceVersion").textValue())
                    .setSourceSnapshotHash(item.get("sourceSnapshotHash").textValue())
                    .setPayloadHash(item.get("payloadHash").textValue()).setSignature(item.get("signature").textValue()));
        }
        MesIndependentBatchPrerequisiteReceipt receipt = new MesIndependentBatchPrerequisiteReceipt()
                .setTenantId(row.getTenantId()).setEntryType(row.getEntryType()).setWorkOrderId(row.getWorkOrderId())
                .setWorkOrderCode(row.getWorkOrderCode()).setRouteId(row.getRouteId()).setRouteVersionId(row.getRouteVersionId())
                .setRouteVersion(row.getRouteVersion()).setBatchCode(row.getBatchCode()).setSourceRelationId(row.getSourceRelationId())
                .setSourceRelationVersion(row.getSourceRelationVersion()).setSourceRelationSnapshotHash(row.getSourceRelationSnapshotHash())
                .setSourceObjectType(row.getSourceObjectType()).setSourceObjectId(row.getSourceObjectId())
                .setMaterialSourceType(row.getMaterialSourceType()).setMaterialSourceId(row.getMaterialSourceId())
                .setSourceContextHash(row.getSourceContextHash()).setSourceSnapshotHash(row.getSourceSnapshotHash())
                .setBusinessReason(row.getBusinessReason()).setIssuerSystem(row.getIssuerSystem())
                .setIssuerUserId(row.getIssuerUserId()).setIssuerUserRole(row.getIssuerUserRole())
                .setIssuedAt(row.getIssuedAt()).setExpiresAt(row.getExpiresAt()).setCredentialVersion(row.getCredentialVersion())
                .setIdempotencyKey(row.getIdempotencyKey()).setSourceEvidence(items);
        String payload = MesIndependentBatchPrerequisiteReceiptCanonicalizer.canonicalPayload(receipt);
        String payloadHash = sha(payload);
        require(payload.equals(row.getCanonicalPayload()) && payloadHash.equals(row.getPayloadHash())
                && equalSecretDigest(hmac(payload, signingSecret), row.getSignature())
                && sha(payloadHash + "|" + row.getSignature()).equals(row.getReceiptHash()), "SOURCE_OWNER_INTEGRITY_FAILED");
        require("BACKEND_CONTROLLED".equals(row.getIssuerUserRole()), "SOURCE_OWNER_INTEGRITY_FAILED");
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.put("codecVersion", "MES-OC-JSON-V1").put("requestKind", "INDEPENDENT_ISSUE_REQUEST_V1");
        request.put("tenantId", row.getTenantId()).put("authenticatedActor", row.getIssuerUserId())
                .put("issuerSystem", row.getIssuerSystem()).put("entryType", row.getEntryType())
                .put("workOrderId", row.getWorkOrderId()).put("workOrderCode", row.getWorkOrderCode())
                .put("routeId", row.getRouteId()).put("routeVersionId", row.getRouteVersionId()).put("routeVersion", row.getRouteVersion())
                .put("batchCode", row.getBatchCode()).put("sourceRelationId", row.getSourceRelationId())
                .put("sourceRelationVersion", row.getSourceRelationVersion()).put("sourceRelationSnapshotHash", row.getSourceRelationSnapshotHash())
                .put("sourceObjectType", row.getSourceObjectType()).put("sourceObjectId", row.getSourceObjectId())
                .put("materialSourceType", row.getMaterialSourceType()).put("materialSourceId", row.getMaterialSourceId())
                .put("sourceContextHash", row.getSourceContextHash()).put("sourceSnapshotHash", row.getSourceSnapshotHash())
                .put("businessReason", row.getBusinessReason()).put("idempotencyKey", row.getIdempotencyKey());
        request.set("sourceEvidence", evidence);
        return MesEdhrSourceCreationCodec.decodeIssueRequestV1(MesEdhrSourceCreationCodec.encodeIssueRequestV1(request).utf8Bytes());
    }

    public ArrayNode materialTypes(MesProEdhrMaterialGateReceiptDO row) {
        JsonNode types = strict(row.getMaterialTypeKeysJson());
        require(types.isArray() && types.size() == 4, "SOURCE_OWNER_INTEGRITY_FAILED");
        Set<String> actual = new HashSet<>();
        for (JsonNode type : types) require(type.isTextual() && actual.add(type.textValue()), "SOURCE_OWNER_INTEGRITY_FAILED");
        require(actual.equals(MATERIAL_TYPES), "SOURCE_OWNER_INTEGRITY_FAILED");
        String hash = sha(String.join("|", String.valueOf(row.getTenantId()), String.valueOf(row.getBatchExecutionId()),
                String.valueOf(row.getReceiptId()), String.valueOf(row.getGateStatus()), row.getMaterialTypeKeysJson(),
                String.valueOf(row.getManifestHash()), String.valueOf(row.getSourceSnapshotHash()),
                String.valueOf(row.getMaterialVersionSetHash()), String.valueOf(row.getIssuedBy()),
                String.valueOf(row.getAuditEventId()), String.valueOf(row.getVersion())));
        require(hash.equals(row.getReceiptHash()) && "MATERIALS_READY".equals(row.getGateStatus()), "SOURCE_OWNER_INTEGRITY_FAILED");
        return (ArrayNode) types;
    }

    public void completion(MesProcessPoolActiveOrderCompletionReceiptDO row) {
        for (String raw : new String[]{row.getFormalSourceSnapshotJson(), row.getSignatureSnapshotJson()}) {
            require(strict(raw).isObject(), "SOURCE_OWNER_INTEGRITY_FAILED");
        }
        // The real loss reader produces a List, including the empty zero-loss list.
        // Keep that array, its order and the original raw bytes for the owner digests.
        JsonNode lossFacts = strict(row.getLossConditionFactsJson());
        require(lossFacts.isArray(), "SOURCE_OWNER_INTEGRITY_FAILED");
        for (JsonNode condition : lossFacts) require(condition.isObject(), "SOURCE_OWNER_INTEGRITY_FAILED");
        require(strict(row.getBatchRecordSourceIdsJson()).isArray()
                && strict(row.getProcessInspectionSourceIdsJson()).isArray(), "SOURCE_OWNER_INTEGRITY_FAILED");
        require(row.getHasActualLoss() != null && row.getLossQuantity() != null
                && row.getExpectedVersion() != null && row.getExpectedVersion() >= 0 && row.getExpectedVersion() < Integer.MAX_VALUE
                && row.getCompletedVersion() != null && row.getCompletedVersion() == row.getExpectedVersion() + 1
                && "BACKFILL_SUCCEEDED".equals(row.getReceiptStatus()) && "SUCCESS".equals(row.getCompletionStatus())
                && "SUCCESS".equals(row.getBatchRecordStatus()) && "SUCCESS".equals(row.getProcessInspectionStatus())
                && "PENDING_FLOW6".equals(row.getProvisionHandoff()), "SOURCE_OWNER_INTEGRITY_FAILED");
        if (row.getHasActualLoss()) {
            require(row.getLossQuantity().signum() > 0 && row.getLossRecordId() != null && row.getLossRecordId() > 0
                    && "SUCCESS".equals(row.getLossReportStatus()) && row.getZeroLossConfirmationSnapshot() == null
                    && row.getLossSourceHash() != null && row.getLossSourceHash().matches("[0-9a-f]{64}"),
                    "SOURCE_OWNER_INTEGRITY_FAILED");
        } else {
            require(row.getLossQuantity().signum() == 0 && row.getLossRecordId() == null
                    && "NOT_REQUIRED".equals(row.getLossReportStatus()), "SOURCE_OWNER_INTEGRITY_FAILED");
            JsonNode zero = strict(row.getZeroLossConfirmationSnapshot());
            require(zero.isObject() && !zero.isEmpty(), "SOURCE_OWNER_INTEGRITY_FAILED");
        }
        require(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(row).equals(row.getReceiptHash())
                && MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                        row.getFormalSourceSnapshotJson(), row.getLossConditionFactsJson()).equals(row.getSourceSnapshotHash()),
                "SOURCE_OWNER_INTEGRITY_FAILED");
        if (row.getLossSourceHash() != null) {
            require(sha(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(
                    row.getLossConditionFactsJson())).equals(row.getLossSourceHash()), "SOURCE_OWNER_INTEGRITY_FAILED");
        }
    }

    static JsonNode strict(String raw) {
        if (raw == null || raw.isBlank()) throw fail("SOURCE_OWNER_INTEGRITY_FAILED");
        ObjectMapper mapper = JsonUtils.getObjectMapper().copy();
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        try (JsonParser parser = mapper.getFactory().createParser(raw)) {
            JsonNode node = mapper.readTree(parser);
            require(node != null && parser.nextToken() == null, "SOURCE_OWNER_INTEGRITY_FAILED");
            return node;
        } catch (IOException e) { throw new IllegalStateException("SOURCE_OWNER_INTEGRITY_FAILED", e); }
    }
    static void keys(JsonNode node, Set<String> expected) {
        require(node != null && node.isObject(), "SOURCE_OWNER_INTEGRITY_FAILED");
        Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(expected), "SOURCE_OWNER_INTEGRITY_FAILED");
    }
    static String text(JsonNode node) {
        require(node != null && node.isTextual() && !node.textValue().isBlank(), "SOURCE_OWNER_INTEGRITY_FAILED");
        return node.textValue();
    }
    static String sha(String raw) { return MesIndependentBatchPrerequisiteReceiptCanonicalizer.sha256(raw); }
    static void require(boolean condition, String code) { if (!condition) throw fail(code); }
    static IllegalStateException fail(String code) { return new IllegalStateException(code); }
    private static String hmac(String raw, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("SOURCE_OWNER_INTEGRITY_FAILED", e); }
    }
    private static boolean equalSecretDigest(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
