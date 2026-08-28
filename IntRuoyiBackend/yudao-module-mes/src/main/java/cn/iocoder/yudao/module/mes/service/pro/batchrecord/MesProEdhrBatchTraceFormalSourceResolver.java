package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Objects;
import java.util.Set;

/**
 * Validates the server-side source bundle used by Tx-C.  This class deliberately
 * accepts only persisted receipt objects and persisted audit evidence; it has no
 * client payload or work-order lookup fallback.
 */
public final class MesProEdhrBatchTraceFormalSourceResolver {

    public static final String ACTIVE_ORDER_COMPLETION = "ACTIVE_ORDER_COMPLETION";
    private static final Set<String> ACTIVE_ORDER_ENTRY_TYPES = Set.of(
            ACTIVE_ORDER_COMPLETION, "ACTIVE_ORDER_SCHEDULED", "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY");

    private MesProEdhrBatchTraceFormalSourceResolver() {
    }

    public static boolean isActiveOrderEntryType(String entryType) {
        return entryType != null && ACTIVE_ORDER_ENTRY_TYPES.contains(entryType);
    }

    public static JSONArray resolveActive(Long tenantId, MesFlow6CompletionBackfillReceipt receipt,
                                          JSONObject metadata, JSONArray persistedEvidence) {
        if (receipt == null) {
            throw blocked("FORMAL_SOURCE_RECEIPT_REQUIRED", "Flow 4 completion receipt is missing");
        }
        if (!Objects.equals(tenantId, receipt.getTenantId())
                || !MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED.equals(receipt.getStatus())) {
            throw blocked("FORMAL_SOURCE_RECEIPT_REQUIRED", "Flow 4 completion receipt is not tenant-visible and successful");
        }
        require(metadata, "sourceCredentialId");
        require(metadata, "completionBackfillReceiptHash");
        if (!Objects.equals(String.valueOf(receipt.getReceiptId()), metadata.getString("sourceCredentialId"))
                || !Objects.equals(receipt.getReceiptHash(), metadata.getString("completionBackfillReceiptHash"))) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "Flow 4 receipt witness does not match Flow 6 context");
        }
        if (!Objects.equals(receipt.getSourceSnapshotHash(), metadata.getString("sourceSnapshotHash"))
                || !Objects.equals(receipt.getWorkOrderId(), metadata.getLong("workOrderId"))
                || !Objects.equals(receipt.getActiveOrderId(), metadata.getLong("activeOrderId"))) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "Flow 4 receipt identity or snapshot changed");
        }
        if (metadata.getBoolean("hasActualLoss") != null
                && !Objects.equals(receipt.getHasActualLoss(), metadata.getBoolean("hasActualLoss"))) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "loss decision changed after Flow 4 completion");
        }
        return requireEvidence(persistedEvidence);
    }

    public static JSONArray resolveIndependent(Long tenantId, MesIndependentBatchPrerequisiteReceipt receipt,
                                               JSONObject metadata, JSONArray persistedEvidence) {
        if (receipt == null) {
            throw blocked("FORMAL_SOURCE_RECEIPT_REQUIRED", "Flow 9 prerequisite receipt is missing");
        }
        if (!Objects.equals(tenantId, receipt.getTenantId())
                || !Objects.equals(receipt.getEntryType(), metadata.getString("entryType"))
                || !Objects.equals(receipt.getSourceSnapshotHash(), metadata.getString("sourceSnapshotHash"))) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "Flow 9 receipt identity or snapshot changed");
        }
        require(metadata, "sourceCredentialId");
        require(metadata, "sourceCredentialHash");
        if (!Objects.equals(receipt.getReceiptId(), metadata.getString("sourceCredentialId"))
                || !Objects.equals(receipt.getReceiptHash(), metadata.getString("sourceCredentialHash"))) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "Flow 9 credential witness does not match Flow 6 context");
        }
        return requireEvidence(persistedEvidence);
    }

    private static JSONArray requireEvidence(JSONArray evidence) {
        if (evidence == null || evidence.isEmpty()) {
            throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED",
                    "Flow 6 persisted sourceEvidence is missing; client evidence is not accepted");
        }
        for (Object raw : evidence) {
            if (!(raw instanceof JSONObject item)) {
                throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "persisted sourceEvidence item is not an object");
            }
            require(item, "sourceType");
            require(item, "sourceObjectType");
            require(item, "sourceObjectId");
            require(item, "snapshotJson");
            require(item, "sourceSnapshotHash");
            require(item, "sourceIdentityKey");
            require(item, "relationStatus");
        }
        return evidence;
    }

    private static void require(JSONObject object, String key) {
        if (object == null || object.get(key) == null || object.getString(key).isBlank()) {
            throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "formal source field is missing: " + key);
        }
    }

    private static MesProEdhrBatchTraceFormalSourceBlocked blocked(String code, String message) {
        return new MesProEdhrBatchTraceFormalSourceBlocked(code, message);
    }

    public static final class MesProEdhrBatchTraceFormalSourceBlocked extends RuntimeException {
        private final String reasonCode;

        public MesProEdhrBatchTraceFormalSourceBlocked(String reasonCode, String message) {
            super(message);
            this.reasonCode = reasonCode;
        }

        public String getReasonCode() {
            return reasonCode;
        }
    }
}
