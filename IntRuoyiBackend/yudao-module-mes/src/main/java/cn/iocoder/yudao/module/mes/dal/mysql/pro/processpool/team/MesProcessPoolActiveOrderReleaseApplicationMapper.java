package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderReleaseApplicationMapper
        extends BaseMapperX<MesProcessPoolActiveOrderReleaseApplicationDO> {

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByRequestIdempotencyKey(
            Long activeOrderId, String requestIdempotencyKey) {
        if (activeOrderId == null || requestIdempotencyKey == null || requestIdempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getRequestIdempotencyKey, requestIdempotencyKey)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByBusinessIdempotencyKey(
            Long activeOrderId, String businessIdempotencyKey) {
        if (activeOrderId == null || businessIdempotencyKey == null || businessIdempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getBusinessIdempotencyKey, businessIdempotencyKey)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectLatestByActiveOrderIds(
            Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderIds)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }
}
