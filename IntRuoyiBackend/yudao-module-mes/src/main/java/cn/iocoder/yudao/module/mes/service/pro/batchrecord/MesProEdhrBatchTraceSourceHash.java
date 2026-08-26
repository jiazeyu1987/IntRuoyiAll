package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;

public final class MesProEdhrBatchTraceSourceHash {

    private MesProEdhrBatchTraceSourceHash() {
    }

    public static String calculate(String linkType, String snapshotJson) {
        if (MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(linkType)) {
            return DigestUtil.sha256Hex(snapshotJson);
        }
        return DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(snapshotJson));
    }
}
