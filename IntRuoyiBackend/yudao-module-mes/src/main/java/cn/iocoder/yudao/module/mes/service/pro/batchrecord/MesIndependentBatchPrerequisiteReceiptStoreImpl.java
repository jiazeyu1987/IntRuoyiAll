package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MesIndependentBatchPrerequisiteReceiptStoreImpl implements MesIndependentBatchPrerequisiteReceiptStore {
    @Resource
    private MesIndependentBatchPrerequisiteReceiptMapper mapper;

    @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptId(Long tenantId, String receiptId) {
        return mapper.selectByReceiptId(tenantId, receiptId);
    }
    @Override public MesIndependentBatchPrerequisiteReceiptDO selectByReceiptIdUnscoped(String receiptId) {
        return mapper.selectByReceiptIdUnscoped(receiptId);
    }
    @Override public MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId, String entryType, String idempotencyKey) {
        return mapper.selectByIdempotencyKey(tenantId, entryType, idempotencyKey);
    }
    @Override public void insert(MesIndependentBatchPrerequisiteReceiptDO data) {
        if (mapper.insert(data) != 1 || data.getId() == null || data.getId() <= 0)
            throw new IllegalStateException("INDEPENDENT_RECEIPT_INSERT_FAILED");
    }
    @Override public void update(MesIndependentBatchPrerequisiteReceiptDO data) {
        if (mapper.updateById(data) != 1) throw new IllegalStateException("INDEPENDENT_RECEIPT_UPDATE_FAILED");
    }
}
