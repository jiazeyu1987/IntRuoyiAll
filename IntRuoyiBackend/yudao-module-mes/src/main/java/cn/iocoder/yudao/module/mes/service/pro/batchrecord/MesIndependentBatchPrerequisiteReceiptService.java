package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesIndependentBatchPrerequisiteReceiptService {
    MesIndependentBatchPrerequisiteReceipt issue(MesIndependentBatchPrerequisiteReceiptIssueCommand command,
                                                 Long tenantId, Long actorUserId);

    MesIndependentBatchPrerequisiteReceipt verify(MesIndependentBatchPrerequisiteReceiptVerifyCommand command,
                                                  Long tenantId);

    MesIndependentBatchPrerequisiteReceipt revoke(MesIndependentBatchPrerequisiteReceiptRevokeCommand command,
                                                  Long tenantId, Long actorUserId);
}
