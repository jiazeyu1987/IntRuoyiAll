package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.StringJoiner;

/** Fixed field-order canonical payload for the persisted independent prerequisite receipt. */
public final class MesIndependentBatchPrerequisiteReceiptCanonicalizer {

    private MesIndependentBatchPrerequisiteReceiptCanonicalizer() {
    }

    public static String canonicalPayload(MesIndependentBatchPrerequisiteReceipt receipt) {
        StringJoiner joiner = new StringJoiner("|");
        field(joiner, "credentialVersion", receipt.getCredentialVersion());
        field(joiner, "tenantId", receipt.getTenantId()); field(joiner, "entryType", receipt.getEntryType());
        field(joiner, "workOrderId", receipt.getWorkOrderId()); field(joiner, "workOrderCode", receipt.getWorkOrderCode());
        field(joiner, "routeId", receipt.getRouteId()); field(joiner, "routeVersionId", receipt.getRouteVersionId());
        field(joiner, "routeVersion", receipt.getRouteVersion()); field(joiner, "batchCode", receipt.getBatchCode());
        field(joiner, "sourceRelationId", receipt.getSourceRelationId());
        field(joiner, "sourceRelationVersion", receipt.getSourceRelationVersion());
        field(joiner, "sourceRelationSnapshotHash", receipt.getSourceRelationSnapshotHash());
        field(joiner, "sourceObjectType", receipt.getSourceObjectType()); field(joiner, "sourceObjectId", receipt.getSourceObjectId());
        field(joiner, "materialSourceType", receipt.getMaterialSourceType()); field(joiner, "materialSourceId", receipt.getMaterialSourceId());
        field(joiner, "sourceContextHash", receipt.getSourceContextHash()); field(joiner, "sourceSnapshotHash", receipt.getSourceSnapshotHash());
        field(joiner, "businessReason", receipt.getBusinessReason()); field(joiner, "issuerSystem", receipt.getIssuerSystem());
        field(joiner, "issuerUserId", receipt.getIssuerUserId()); field(joiner, "issuerUserRole", receipt.getIssuerUserRole());
        field(joiner, "issuedAt", receipt.getIssuedAt()); field(joiner, "expiresAt", receipt.getExpiresAt());
        field(joiner, "idempotencyKey", receipt.getIdempotencyKey());
        List<MesBatchExecutionSourceEvidence> evidence = receipt.getSourceEvidence();
        if (evidence != null) {
            evidence.stream().sorted(Comparator.comparing(MesBatchExecutionSourceEvidence::getSourceType,
                            Comparator.nullsFirst(String::compareTo))
                    .thenComparing(MesBatchExecutionSourceEvidence::getSourceId,
                            Comparator.nullsFirst(String::compareTo)))
                    .forEach(item -> {
                        field(joiner, "evidence.sourceType", item.getSourceType());
                        field(joiner, "evidence.sourceId", item.getSourceId());
                        field(joiner, "evidence.sourceVersion", item.getSourceVersion());
                        field(joiner, "evidence.sourceSnapshotHash", item.getSourceSnapshotHash());
                        field(joiner, "evidence.payloadHash", item.getPayloadHash());
                        field(joiner, "evidence.signature", item.getSignature());
                    });
        }
        return joiner.toString();
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    private static void field(StringJoiner joiner, String name, Object value) {
        String text = value == null ? "<null>" : String.valueOf(value);
        joiner.add(name + "=" + text.length() + ":" + text);
    }
}
