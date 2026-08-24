package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessOverageLimitDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolTeamProcessOverageLimitMapper extends BaseMapperX<MesProcessPoolTeamProcessOverageLimitDO> {
    default MesProcessPoolTeamProcessOverageLimitDO selectByLeaderAndRouteProcess(Long leaderUserId,
                                                                                     Long routeProcessId,
                                                                                     Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolTeamProcessOverageLimitDO>()
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getProcessId, processId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getEnabled, Boolean.TRUE));
    }

    default MesProcessPoolTeamProcessOverageLimitDO selectByLeaderAndRouteProcessForUpdate(Long leaderUserId,
                                                                                              Long routeProcessId,
                                                                                              Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolTeamProcessOverageLimitDO>()
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getRouteProcessId, routeProcessId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getProcessId, processId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getEnabled, Boolean.TRUE)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolTeamProcessOverageLimitDO> selectByLeader(Long leaderUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamProcessOverageLimitDO>()
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamProcessOverageLimitDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolTeamProcessOverageLimitDO::getRouteProcessId));
    }
}
