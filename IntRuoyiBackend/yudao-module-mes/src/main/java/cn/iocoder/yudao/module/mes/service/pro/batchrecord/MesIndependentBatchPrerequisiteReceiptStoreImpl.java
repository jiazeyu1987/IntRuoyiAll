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
    @Override public MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId, String entryType, String idempotencyKey) {
        return mapper.selectByIdempotencyKey(tenantId, entryType, idempotencyKey);
    }
    @Override public void insert(MesIndependentBatchPrerequisiteReceiptDO data) { mapper.insert(data); }
    @Override public void update(MesIndependentBatchPrerequisiteReceiptDO data) { mapper.updateById(data); }
}
