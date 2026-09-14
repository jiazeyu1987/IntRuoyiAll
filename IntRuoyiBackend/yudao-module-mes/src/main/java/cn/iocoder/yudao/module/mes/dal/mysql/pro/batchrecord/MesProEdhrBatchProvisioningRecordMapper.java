package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchProvisioningRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrBatchProvisioningRecordMapper
        extends BaseMapperX<MesProEdhrBatchProvisioningRecordDO> {

    default MesProEdhrBatchProvisioningRecordDO selectByBatchExecutionId(Long tenantId, Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchProvisioningRecordDO>()
                .eq(MesProEdhrBatchProvisioningRecordDO::getTenantId, tenantId)
                .eq(MesProEdhrBatchProvisioningRecordDO::getBatchExecutionId, batchExecutionId));
    }

    default MesProEdhrBatchProvisioningRecordDO selectByIdempotencyKey(Long tenantId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchProvisioningRecordDO>()
                .eq(MesProEdhrBatchProvisioningRecordDO::getTenantId, tenantId)
                .eq(MesProEdhrBatchProvisioningRecordDO::getIdempotencyKey, idempotencyKey));
    }

    default MesProEdhrBatchProvisioningRecordDO selectByIdAndTenantId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchProvisioningRecordDO>()
                .eq(MesProEdhrBatchProvisioningRecordDO::getTenantId, tenantId)
                .eq(MesProEdhrBatchProvisioningRecordDO::getId, id));
    }
}
