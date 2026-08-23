package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchTraceOutboxEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchTraceOutboxEventMapper
        extends BaseMapperX<MesProEdhrBatchTraceOutboxEventDO> {

    default MesProEdhrBatchTraceOutboxEventDO selectByEventId(String eventId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchTraceOutboxEventDO>()
                .eq(MesProEdhrBatchTraceOutboxEventDO::getEventId, eventId));
    }

    default MesProEdhrBatchTraceOutboxEventDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchTraceOutboxEventDO>()
                .eq(MesProEdhrBatchTraceOutboxEventDO::getIdempotencyKey, idempotencyKey));
    }

    default List<MesProEdhrBatchTraceOutboxEventDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchTraceOutboxEventDO>()
                .eq(MesProEdhrBatchTraceOutboxEventDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchTraceOutboxEventDO::getId));
    }
}
