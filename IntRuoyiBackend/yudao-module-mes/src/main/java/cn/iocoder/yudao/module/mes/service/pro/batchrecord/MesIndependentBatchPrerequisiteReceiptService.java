package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesIndependentBatchPrerequisiteReceiptService extends MesIndependentBatchPrerequisiteReceiptPort {
    MesEdhrVerifiedIndependentReceiptFact getCreationVerifiedByReceiptId(Long tenantId, String receiptId,
            String entryType, String sourceSnapshotHash);
    MesIndependentBatchPrerequisiteReceipt issue(MesIndependentBatchPrerequisiteReceiptIssueCommand command,
                                                 Long tenantId, Long actorUserId);

    MesIndependentBatchPrerequisiteReceipt verify(MesIndependentBatchPrerequisiteReceiptVerifyCommand command,
                                                  Long tenantId);

    MesIndependentBatchPrerequisiteReceipt revoke(MesIndependentBatchPrerequisiteReceiptRevokeCommand command,
                                                  Long tenantId, Long actorUserId);
}
