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

    default List<MesProcessPoolTeamLeaderScopeDO> selectPqcEmployeeScopes(Long leaderUserId, Boolean enabled) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamLeaderScopeDO>()
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderType,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .eq(MesProcessPoolTeamLeaderScopeDO::getScopeType,
                        MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .eqIfPresent(MesProcessPoolTeamLeaderScopeDO::getEnabled, enabled)
                .orderByAsc(MesProcessPoolTeamLeaderScopeDO::getId));
    }

    default MesProcessPoolTeamLeaderScopeDO selectPqcEmployeeScope(Long leaderUserId, Long employeeUserId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolTeamLeaderScopeDO>()
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderType,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .eq(MesProcessPoolTeamLeaderScopeDO::getScopeType,
                        MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .eq(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId, employeeUserId));
    }

    default List<MesProcessPoolTeamLeaderScopeDO> selectActivePqcEmployeeScopesByEmployeeUserId(Long employeeUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolTeamLeaderScopeDO>()
                .eq(MesProcessPoolTeamLeaderScopeDO::getLeaderType,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .eq(MesProcessPoolTeamLeaderScopeDO::getScopeType,
                        MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .eq(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId, employeeUserId)
                .eq(MesProcessPoolTeamLeaderScopeDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId));
    }
}
