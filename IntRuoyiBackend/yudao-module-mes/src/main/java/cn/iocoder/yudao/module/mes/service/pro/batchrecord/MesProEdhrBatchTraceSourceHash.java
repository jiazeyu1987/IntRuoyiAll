package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Set;

public final class MesProEdhrBatchTraceSourceHash {

    private static final Set<String> EXTERNAL_WITNESS_TYPES = Set.of(
            MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE,
            MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE,
            MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
            MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT,
            MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT,
            MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT,
            MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT);

    private MesProEdhrBatchTraceSourceHash() {
    }

    public static String calculate(String linkType, String snapshotJson) {
        if (MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(linkType)) {
            return DigestUtil.sha256Hex(snapshotJson);
        }
        return DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(snapshotJson));
    }

    public static boolean isValid(String linkType, String snapshotJson, String snapshotHash) {
        if (linkType == null || snapshotJson == null || snapshotHash == null
                || snapshotJson.isBlank() || snapshotHash.isBlank()) {
            return false;
        }
        if (calculate(linkType, snapshotJson).equalsIgnoreCase(snapshotHash)) {
            return true;
        }
        if (!EXTERNAL_WITNESS_TYPES.contains(linkType)) {
            return false;
        }
        try {
            JSONObject snapshot = JSON.parseObject(snapshotJson);
            return snapshot != null && snapshotHash.equals(snapshot.getString("witnessHash"));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
