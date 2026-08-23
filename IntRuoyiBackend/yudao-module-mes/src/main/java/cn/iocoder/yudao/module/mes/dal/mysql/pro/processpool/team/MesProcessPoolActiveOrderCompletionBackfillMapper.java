package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderCompletionBackfillMapper
        extends BaseMapperX<MesProcessPoolActiveOrderCompletionBackfillDO> {

    default MesProcessPoolActiveOrderCompletionBackfillDO selectByActiveOrderAndTypeForUpdate(
            Long activeOrderId, String backfillType) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderCompletionBackfillDO>()
                .eq(MesProcessPoolActiveOrderCompletionBackfillDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderCompletionBackfillDO::getBackfillType, backfillType)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderCompletionBackfillDO> selectListByActiveOrderIdForUpdate(
            Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderCompletionBackfillDO>()
                .eq(MesProcessPoolActiveOrderCompletionBackfillDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderCompletionBackfillDO::getBackfillType)
                .last("FOR UPDATE"));
    }
}
