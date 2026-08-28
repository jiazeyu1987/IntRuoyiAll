package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProEdhrBatchTraceFormalSourceResolverTest {

    @Test
    void activeReceiptWitnessMustMatchPersistedFlow6Context() {
        JSONObject metadata = metadata("ACTIVE_ORDER_COMPLETION", "900", "receipt-hash", "snapshot-a");
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(900L).setTenantId(7L).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setReceiptHash("receipt-hash").setSourceSnapshotHash("snapshot-a")
                .setActiveOrderId(10L).setWorkOrderId(20L);
        metadata.put("activeOrderId", 10L);
        metadata.put("workOrderId", 20L);
        JSONArray evidence = evidence();

        assertEquals(1, MesProEdhrBatchTraceFormalSourceResolver
                .resolveActive(7L, receipt, metadata, evidence).size());

        receipt.setReceiptHash("changed-after-precheck");
        MesProEdhrBatchTraceFormalSourceResolver.MesProEdhrBatchTraceFormalSourceBlocked error =
                assertThrows(MesProEdhrBatchTraceFormalSourceResolver.MesProEdhrBatchTraceFormalSourceBlocked.class,
                        () -> MesProEdhrBatchTraceFormalSourceResolver.resolveActive(7L, receipt, metadata, evidence));
        assertEquals("SOURCE_SNAPSHOT_HASH_MISMATCH", error.getReasonCode());
    }

    @Test
    void activeOrderEntryAliasesUseTheFlow4ReceiptContract() {
        for (String entryType : java.util.List.of("ACTIVE_ORDER_COMPLETION", "ACTIVE_ORDER_SCHEDULED",
                "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY")) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(entryType));
        }
        org.junit.jupiter.api.Assertions.assertFalse(
                MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType("PQC_INDEPENDENT"));
    }

    @Test
    void independentReceiptMustBeServerVerifiedAndCannotUseActiveFields() {
        JSONObject metadata = metadata("MANUAL", "independent-1", "credential-hash", "snapshot-i");
        MesIndependentBatchPrerequisiteReceipt receipt = new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1").setTenantId(7L).setEntryType("MANUAL")
                .setSourceSnapshotHash("snapshot-i").setReceiptHash("credential-hash");
        assertEquals(1, MesProEdhrBatchTraceFormalSourceResolver
                .resolveIndependent(7L, receipt, metadata, evidence()).size());
    }

    @Test
    void missingPersistedEvidenceBlocksWithStableCode() {
        JSONObject metadata = metadata("MANUAL", "independent-1", "credential-hash", "snapshot-i");
        MesIndependentBatchPrerequisiteReceipt receipt = new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1").setTenantId(7L).setEntryType("MANUAL")
                .setSourceSnapshotHash("snapshot-i").setReceiptHash("credential-hash");
        MesProEdhrBatchTraceFormalSourceResolver.MesProEdhrBatchTraceFormalSourceBlocked error =
                assertThrows(MesProEdhrBatchTraceFormalSourceResolver.MesProEdhrBatchTraceFormalSourceBlocked.class,
                        () -> MesProEdhrBatchTraceFormalSourceResolver.resolveIndependent(7L, receipt, metadata, null));
        assertEquals("FORMAL_SOURCE_EVIDENCE_REQUIRED", error.getReasonCode());
    }

    private static JSONObject metadata(String entryType, String credentialId, String credentialHash, String snapshotHash) {
        return new JSONObject().fluentPut("entryType", entryType).fluentPut("sourceCredentialId", credentialId)
                .fluentPut("completionBackfillReceiptHash", credentialHash).fluentPut("sourceCredentialHash", credentialHash)
                .fluentPut("sourceSnapshotHash", snapshotHash);
    }

    private static JSONArray evidence() {
        return new JSONArray().fluentAdd(new JSONObject().fluentPut("sourceType", "WORK_ORDER")
                .fluentPut("sourceObjectType", "WORK_ORDER").fluentPut("sourceObjectId", "20")
                .fluentPut("snapshotJson", "{\"id\":20}").fluentPut("sourceSnapshotHash", "hash")
                .fluentPut("sourceIdentityKey", "WORK_ORDER:20").fluentPut("relationStatus", "LINKED"));
    }
}
