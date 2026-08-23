package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;

public interface MesIndependentBatchPrerequisiteReceiptStore {
    MesIndependentBatchPrerequisiteReceiptDO selectByReceiptId(Long tenantId, String receiptId);
    MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId, String entryType, String idempotencyKey);
    void insert(MesIndependentBatchPrerequisiteReceiptDO data);
    void update(MesIndependentBatchPrerequisiteReceiptDO data);
}
