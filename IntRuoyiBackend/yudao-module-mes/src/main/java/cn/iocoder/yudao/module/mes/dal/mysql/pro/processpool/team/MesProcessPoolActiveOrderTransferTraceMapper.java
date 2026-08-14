package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderTransferTraceMapper
        extends BaseMapperX<MesProcessPoolActiveOrderTransferTraceDO> {

    default MesProcessPoolActiveOrderTransferTraceDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderTransferTraceDO>()
                .eq(MesProcessPoolActiveOrderTransferTraceDO::getIdempotencyKey, idempotencyKey));
    }

    default List<MesProcessPoolActiveOrderTransferTraceDO> selectListByActiveOrderId(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderTransferTraceDO>()
                .eq(MesProcessPoolActiveOrderTransferTraceDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderTransferTraceDO::getSourceOccurredAt)
                .orderByAsc(MesProcessPoolActiveOrderTransferTraceDO::getId));
    }

    default List<MesProcessPoolActiveOrderTransferTraceDO> selectListByActiveOrderIdAndSourceTypes(
            Long activeOrderId, Collection<String> sourceTypes) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderTransferTraceDO>()
                .eq(MesProcessPoolActiveOrderTransferTraceDO::getActiveOrderId, activeOrderId)
                .in(MesProcessPoolActiveOrderTransferTraceDO::getSourceType, sourceTypes)
                .orderByAsc(MesProcessPoolActiveOrderTransferTraceDO::getSourceOccurredAt)
                .orderByAsc(MesProcessPoolActiveOrderTransferTraceDO::getId));
    }
}
