package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionFieldAuditBatchMapper
        extends BaseMapperX<MesProBatchRecordExecutionFieldAuditBatchDO> {

    default MesProBatchRecordExecutionFieldAuditBatchDO selectByIdempotencyKey(Long tenantId, Long executionId,
                                                                               String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getTenantId, tenantId)
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getIdempotencyKey, idempotencyKey));
    }

    default List<MesProBatchRecordExecutionFieldAuditBatchDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditBatchDO::getAfterFieldAuditRevision)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditBatchDO::getId));
    }
}
