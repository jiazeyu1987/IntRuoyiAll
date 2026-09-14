package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrMaterialGateReceiptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrMaterialGateReceiptMapper extends BaseMapperX<MesProEdhrMaterialGateReceiptDO> {

    default MesProEdhrMaterialGateReceiptDO selectByReceiptId(
            Long tenantId, Long batchExecutionId, String receiptId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrMaterialGateReceiptDO>()
                .eq(MesProEdhrMaterialGateReceiptDO::getTenantId, tenantId)
                .eq(MesProEdhrMaterialGateReceiptDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrMaterialGateReceiptDO::getReceiptId, receiptId));
    }

    default MesProEdhrMaterialGateReceiptDO selectLatestByBatchExecutionId(
            Long tenantId, Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrMaterialGateReceiptDO>()
                .eq(MesProEdhrMaterialGateReceiptDO::getTenantId, tenantId)
                .eq(MesProEdhrMaterialGateReceiptDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrMaterialGateReceiptDO::getVersion)
                .last("LIMIT 1"));
    }
}
