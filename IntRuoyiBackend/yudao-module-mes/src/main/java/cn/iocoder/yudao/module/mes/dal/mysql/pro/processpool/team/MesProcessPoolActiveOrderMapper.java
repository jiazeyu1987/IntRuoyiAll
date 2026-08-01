package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderMapper extends BaseMapperX<MesProcessPoolActiveOrderDO> {

    default List<MesProcessPoolActiveOrderDO> selectActiveListByLeader(Long leaderUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByAsc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }
}
