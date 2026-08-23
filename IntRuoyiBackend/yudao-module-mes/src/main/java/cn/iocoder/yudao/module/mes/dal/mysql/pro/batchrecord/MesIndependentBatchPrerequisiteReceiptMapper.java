package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesIndependentBatchPrerequisiteReceiptMapper
        extends BaseMapperX<MesIndependentBatchPrerequisiteReceiptDO> {

    default MesIndependentBatchPrerequisiteReceiptDO selectByReceiptId(Long tenantId, String receiptId) {
        return selectOne(new LambdaQueryWrapperX<MesIndependentBatchPrerequisiteReceiptDO>()
                .eq(MesIndependentBatchPrerequisiteReceiptDO::getTenantId, tenantId)
                .eq(MesIndependentBatchPrerequisiteReceiptDO::getReceiptId, receiptId));
    }

    default MesIndependentBatchPrerequisiteReceiptDO selectByIdempotencyKey(Long tenantId,
                                                                              String entryType,
                                                                              String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesIndependentBatchPrerequisiteReceiptDO>()
                .eq(MesIndependentBatchPrerequisiteReceiptDO::getTenantId, tenantId)
                .eq(MesIndependentBatchPrerequisiteReceiptDO::getEntryType, entryType)
                .eq(MesIndependentBatchPrerequisiteReceiptDO::getIdempotencyKey, idempotencyKey));
    }
}
