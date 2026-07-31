package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolTeamLeaderScopeMapper extends BaseMapperX<MesProcessPoolTeamLeaderScopeDO> {

    default List<MesProcessPoolTeamLeaderScopeDO> selectActiveScopesByLeader(Long leaderUserId, String leaderType) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamLeaderScopeDO>()
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId, leaderUserId)
                .eqIfPresent(MesProcessPoolTeamLeaderScopeDO::getLeaderType, leaderType)
                .eq(MesProcessPoolTeamLeaderScopeDO::getEnabled, Boolean.TRUE));
    }

    default List<MesProcessPoolTeamLeaderScopeDO> selectActiveScopesByLeaderType(String leaderType) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamLeaderScopeDO>()
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderType, leaderType)
                .eq(MesProcessPoolTeamLeaderScopeDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId)
                .orderByAsc(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId));
    }
}
