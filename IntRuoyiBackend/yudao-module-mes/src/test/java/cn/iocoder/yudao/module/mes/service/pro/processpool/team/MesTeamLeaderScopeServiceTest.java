package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderScopeServiceTest {

    @Mock
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;

    private MesTeamLeaderScopeService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderScopeServiceImpl(scopeMapper);
    }

    @Test
    void shouldListResponsibleEmployeesFromExplicitScope() {
        when(scopeMapper.selectActiveScopesByLeader(100L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION))
                .thenReturn(List.of(employeeScope(2001L), employeeScope(2002L)));

        Set<Long> employeeIds = service.listResponsibleEmployeeIds(100L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);

        assertEquals(Set.of(2001L, 2002L), employeeIds);
    }

    @Test
    void shouldRejectOutOfScopeEmployeeAccess() {
        when(scopeMapper.selectActiveScopesByLeader(100L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION))
                .thenReturn(List.of(employeeScope(2001L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.assertCanAccessEmployee(100L,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 9001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED.getCode(), ex.getCode());
    }

    @Test
    void shouldAllowProcessMaintenanceOnlyInsideScope() {
        when(scopeMapper.selectActiveScopesByLeader(100L, null))
                .thenReturn(List.of(processScope(6001L)));

        service.assertCanMaintainProcess(100L, 6001L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.assertCanMaintainProcess(100L, 9001L));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED.getCode(), ex.getCode());
    }

    private static MesProcessPoolTeamLeaderScopeDO employeeScope(Long employeeUserId) {
        return MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(100L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(employeeUserId)
                .enabled(Boolean.TRUE)
                .build();
    }

    private static MesProcessPoolTeamLeaderScopeDO processScope(Long processId) {
        return MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(100L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_PROCESS)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .build();
    }
}
