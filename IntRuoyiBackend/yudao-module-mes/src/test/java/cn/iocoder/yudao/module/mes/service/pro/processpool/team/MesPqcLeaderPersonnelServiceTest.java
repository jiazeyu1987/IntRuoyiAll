package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcLeaderPersonnelServiceTest {

    @Mock
    private MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    @Mock
    private AdminUserApi adminUserApi;

    private MesPqcLeaderPersonnelService service;

    @BeforeEach
    void setUp() {
        service = new MesPqcLeaderPersonnelServiceImpl(scopeMapper, adminUserApi);
    }

    @Test
    void shouldLinkFormalInspectorAsPqcEmployeeScope() {
        when(scopeMapper.selectPqcEmployeeScope(3001L, 2001L)).thenReturn(null);
        when(scopeMapper.insert(any(MesProcessPoolTeamLeaderScopeDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamLeaderScopeDO.class).setId(9001L);
            return 1;
        });

        Long scopeId = service.linkFormalInspector(MesPqcLeaderPersonnelLinkReqBO.builder()
                .leaderUserId(3001L)
                .systemUserId(2001L)
                .build());

        assertEquals(9001L, scopeId);
        verify(adminUserApi).validateUser(2001L);
        verify(adminUserApi, never()).getUserListBySubordinate(3001L);
        ArgumentCaptor<MesProcessPoolTeamLeaderScopeDO> scopeCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamLeaderScopeDO.class);
        verify(scopeMapper).insert(scopeCaptor.capture());
        assertEquals(3001L, scopeCaptor.getValue().getLeaderUserId());
        assertEquals(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, scopeCaptor.getValue().getLeaderType());
        assertEquals(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE, scopeCaptor.getValue().getScopeType());
        assertEquals(2001L, scopeCaptor.getValue().getEmployeeUserId());
        assertEquals(Boolean.TRUE, scopeCaptor.getValue().getEnabled());
    }

    @Test
    void shouldRejectDuplicateActivePqcInspectorBeforeInsert() {
        when(scopeMapper.selectPqcEmployeeScope(3001L, 2001L)).thenReturn(MesProcessPoolTeamLeaderScopeDO.builder()
                .id(9001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(2001L)
                .enabled(Boolean.TRUE)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.linkFormalInspector(
                MesPqcLeaderPersonnelLinkReqBO.builder()
                        .leaderUserId(3001L)
                        .systemUserId(2001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_DUPLICATE.getCode(), ex.getCode());
        verify(adminUserApi).validateUser(2001L);
        verify(adminUserApi, never()).getUserListBySubordinate(3001L);
        verify(scopeMapper, never()).insert(any(MesProcessPoolTeamLeaderScopeDO.class));
    }

    @Test
    void shouldListOnlyCurrentLeaderPqcEmployeeScopes() {
        when(scopeMapper.selectPqcEmployeeScopes(3001L, true)).thenReturn(List.of(
                MesProcessPoolTeamLeaderScopeDO.builder()
                        .id(9001L)
                        .leaderUserId(3001L)
                        .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                        .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                        .employeeUserId(2001L)
                        .enabled(Boolean.TRUE)
                        .build()));
        when(adminUserApi.getUserList(List.of(2001L))).thenReturn(List.of(user(2001L, "pqc01", "王检验")));

        List<MesPqcLeaderPersonnelBO> rows = service.listPersonnel(3001L, true);

        assertEquals(1, rows.size());
        assertEquals(9001L, rows.get(0).getScopeId());
        assertEquals(2001L, rows.get(0).getSystemUserId());
        assertEquals("王检验", rows.get(0).getDisplayName());
        assertEquals("pqc01", rows.get(0).getUsername());
        assertEquals(Boolean.TRUE, rows.get(0).getEnabled());
    }

    @Test
    void shouldUpdateOnlyCurrentLeaderPqcEmployeeScopeStatus() {
        MesProcessPoolTeamLeaderScopeDO scope = MesProcessPoolTeamLeaderScopeDO.builder()
                .id(9001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(2001L)
                .enabled(Boolean.TRUE)
                .build();
        when(scopeMapper.selectById(9001L)).thenReturn(scope);

        service.updatePersonnelStatus(MesPqcLeaderPersonnelStatusUpdateReqBO.builder()
                .leaderUserId(3001L)
                .scopeId(9001L)
                .enabled(false)
                .build());

        ArgumentCaptor<MesProcessPoolTeamLeaderScopeDO> updateCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamLeaderScopeDO.class);
        verify(scopeMapper).updateById(updateCaptor.capture());
        assertEquals(9001L, updateCaptor.getValue().getId());
        assertEquals(Boolean.FALSE, updateCaptor.getValue().getEnabled());
    }

    @Test
    void shouldRejectStatusUpdateForDifferentLeaderOrScopeType() {
        when(scopeMapper.selectById(9001L)).thenReturn(MesProcessPoolTeamLeaderScopeDO.builder()
                .id(9001L)
                .leaderUserId(3002L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(2001L)
                .enabled(Boolean.TRUE)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updatePersonnelStatus(
                MesPqcLeaderPersonnelStatusUpdateReqBO.builder()
                        .leaderUserId(3001L)
                        .scopeId(9001L)
                        .enabled(false)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_NOT_EXISTS.getCode(), ex.getCode());
        verify(scopeMapper, never()).updateById(any(MesProcessPoolTeamLeaderScopeDO.class));
    }

    private static AdminUserRespDTO user(Long id, String username, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
