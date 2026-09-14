package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

/**
 * Internal receipt boundary for Flow 6. Implementations must load by tenant and
 * receipt id and return only a server-verified canonical receipt.
 */
public interface MesIndependentBatchPrerequisiteReceiptPort {

    MesIndependentBatchPrerequisiteReceipt getVerifiedByReceiptId(Long tenantId, String receiptId,
                                                                  String entryType, String sourceSnapshotHash);
}
