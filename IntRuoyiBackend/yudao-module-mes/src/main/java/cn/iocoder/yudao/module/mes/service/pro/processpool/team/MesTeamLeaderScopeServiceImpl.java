package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;

@Service
@Validated
public class MesTeamLeaderScopeServiceImpl implements MesTeamLeaderScopeService {

    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;

    public MesTeamLeaderScopeServiceImpl(MesProcessPoolTeamLeaderScopeMapper scopeMapper) {
        this.scopeMapper = scopeMapper;
    }

    @Override
    public Set<Long> listResponsibleEmployeeIds(Long leaderUserId, String leaderType) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "leaderUserId");
        }
        return activeScopes(leaderUserId, leaderType).stream()
                .filter(scope -> MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()))
                .map(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void assertCanAccessEmployee(Long leaderUserId, String leaderType, Long employeeUserId) {
        if (employeeUserId == null || !listResponsibleEmployeeIds(leaderUserId, leaderType).contains(employeeUserId)) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_DENIED);
        }
    }

    @Override
    public void assertCanMaintainProcess(Long leaderUserId, Long processId) {
        if (leaderUserId == null || processId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "leaderUserId/processId");
        }
        boolean canMaintain = activeScopes(leaderUserId, null).stream()
                .filter(scope -> MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS.equals(scope.getScopeType()))
                .anyMatch(scope -> Objects.equals(scope.getProcessId(), processId));
        if (!canMaintain) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_DENIED);
        }
    }

    private List<MesProcessPoolTeamLeaderScopeDO> activeScopes(Long leaderUserId, String leaderType) {
        return scopeMapper.selectActiveScopesByLeader(leaderUserId, leaderType).stream()
                .filter(scope -> Boolean.TRUE.equals(scope.getEnabled()))
                .toList();
    }
}
