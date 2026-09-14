package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED;

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
        Set<Long> employeeIds = activeScopes(leaderUserId, leaderType).stream()
                .filter(scope -> MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()))
                .map(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(leaderType)) {
            employeeIds.add(leaderUserId);
        }
        return Set.copyOf(employeeIds);
    }

    @Override
    public void assertCanAccessEmployee(Long leaderUserId, String leaderType, Long employeeUserId) {
        if (employeeUserId == null || !listResponsibleEmployeeIds(leaderUserId, leaderType).contains(employeeUserId)) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "员工");
        }
    }

    @Override
    public void assertCanMaintainProcess(Long leaderUserId, Long processId) {
        assertCanMaintainScope(leaderUserId, processId, MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS,
                MesProcessPoolTeamLeaderScopeDO::getProcessId, "leaderUserId/processId", "工序");
    }

    @Override
    public void assertCanMaintainProductionLine(Long leaderUserId, Long productionLineId) {
        assertCanMaintainScope(leaderUserId, productionLineId,
                MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PRODUCTION_LINE,
                MesProcessPoolTeamLeaderScopeDO::getProductionLineId, "leaderUserId/productionLineId", "产线");
    }

    @Override
    public void assertCanMaintainEquipment(Long leaderUserId, Long equipmentId) {
        assertCanMaintainScope(leaderUserId, equipmentId, MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EQUIPMENT,
                MesProcessPoolTeamLeaderScopeDO::getEquipmentId, "leaderUserId/equipmentId", "设备");
    }

    @Override
    public void assertCanMaintainOrder(Long leaderUserId, Long workOrderId) {
        assertCanMaintainScope(leaderUserId, workOrderId, MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_ORDER,
                MesProcessPoolTeamLeaderScopeDO::getWorkOrderId, "leaderUserId/workOrderId", "订单");
    }

    private void assertCanMaintainScope(Long leaderUserId, Long targetId, String scopeType,
                                        Function<MesProcessPoolTeamLeaderScopeDO, Long> scopeTargetGetter,
                                        String requiredLabel, String targetName) {
        if (leaderUserId == null || targetId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, requiredLabel);
        }
        boolean canMaintain = activeScopes(leaderUserId, null).stream()
                .filter(scope -> scopeType.equals(scope.getScopeType()))
                .anyMatch(scope -> Objects.equals(scopeTargetGetter.apply(scope), targetId));
        if (!canMaintain) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, targetName);
        }
    }

    private List<MesProcessPoolTeamLeaderScopeDO> activeScopes(Long leaderUserId, String leaderType) {
        return scopeMapper.selectActiveScopesByLeader(leaderUserId, leaderType).stream()
                .filter(scope -> Boolean.TRUE.equals(scope.getEnabled()))
                .toList();
    }
}
