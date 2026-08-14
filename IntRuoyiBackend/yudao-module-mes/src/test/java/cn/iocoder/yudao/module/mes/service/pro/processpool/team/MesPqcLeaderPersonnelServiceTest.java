package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private RoleApi roleApi;

    private MesPqcLeaderPersonnelService service;

    @BeforeEach
    void setUp() {
        service = new MesPqcLeaderPersonnelServiceImpl(scopeMapper, adminUserApi, permissionApi, roleApi);
    }

    @Test
    void shouldSearchFormalInspectorCandidatesFromPqcPermissionRoleOnly() {
        Set<Long> roleUserIds = new LinkedHashSet<>(List.of(2001L, 2002L, 2003L));
        when(roleApi.getRoleByCode("pqc_permission")).thenReturn(role(910438L, "pqc_permission"));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(910438L))).thenReturn(roleUserIds);
        when(adminUserApi.getUserList(roleUserIds)).thenReturn(List.of(
                user(2002L, "pqc02", "张检验"),
                user(2001L, "pqc01", "王检验"),
                user(2003L, "pqc-disabled", "王停用", CommonStatusEnum.DISABLE.getStatus())));

        List<MesTeamFormalUserCandidateBO> rows = service.searchFormalInspectorCandidates(3001L, " 王 ");

        assertEquals(1, rows.size());
        assertEquals(2001L, rows.get(0).getSystemUserId());
        assertEquals("王检验", rows.get(0).getDisplayName());
        verify(adminUserApi).getUserList(roleUserIds);
        verify(adminUserApi, never()).getUserListByNickname(any());
    }

    @Test
    void shouldReturnAllEnabledPqcPermissionUsersWithoutCandidateLimitForBlankKeyword() {
        List<AdminUserRespDTO> users = users(2001L, 30);
        Set<Long> roleUserIds = userIds(users);
        when(roleApi.getRoleByCode("pqc_permission")).thenReturn(role(910438L, "pqc_permission"));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(910438L))).thenReturn(roleUserIds);
        when(adminUserApi.getUserList(roleUserIds)).thenReturn(users);

        List<MesTeamFormalUserCandidateBO> rows = service.searchFormalInspectorCandidates(3001L, " ");

        assertEquals(30, rows.size());
        verify(adminUserApi).getUserList(roleUserIds);
        verify(adminUserApi, never()).getUserListByNickname(any());
    }

    @Test
    void shouldMarkPqcPermissionCandidateOccupiedByOtherLeaderAsDisabled() {
        Set<Long> roleUserIds = new LinkedHashSet<>(List.of(2001L, 2002L));
        when(roleApi.getRoleByCode("pqc_permission")).thenReturn(role(910438L, "pqc_permission"));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(910438L))).thenReturn(roleUserIds);
        when(adminUserApi.getUserList(roleUserIds)).thenReturn(List.of(
                user(2001L, "pqc01", "王检验"),
                user(2002L, "pqc02", "张检验")));
        when(scopeMapper.selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC))
                .thenReturn(List.of(
                        MesProcessPoolTeamLeaderScopeDO.builder()
                                .id(9101L)
                                .leaderUserId(3002L)
                                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                                .employeeUserId(2001L)
                                .enabled(Boolean.TRUE)
                                .build(),
                        MesProcessPoolTeamLeaderScopeDO.builder()
                                .id(9102L)
                                .leaderUserId(3001L)
                                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                                .employeeUserId(2002L)
                                .enabled(Boolean.TRUE)
                                .build()));

        List<MesTeamFormalUserCandidateBO> rows = service.searchFormalInspectorCandidates(3001L, "");

        assertEquals(2, rows.size());
        MesTeamFormalUserCandidateBO occupied = rows.stream()
                .filter(row -> row.getSystemUserId().equals(2001L))
                .findFirst()
                .orElseThrow();
        MesTeamFormalUserCandidateBO currentLeaderCandidate = rows.stream()
                .filter(row -> row.getSystemUserId().equals(2002L))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, occupied.getDisabled());
        assertEquals(Boolean.TRUE, occupied.getOccupiedByOtherPqcLeader());
        assertEquals(3002L, occupied.getOccupiedLeaderUserId());
        assertEquals("已被其他PQC组长选择", occupied.getDisabledReason());
        assertEquals(Boolean.FALSE, currentLeaderCandidate.getDisabled());
        assertEquals(Boolean.FALSE, currentLeaderCandidate.getOccupiedByOtherPqcLeader());
    }

    @Test
    void shouldLinkFormalInspectorAsPqcEmployeeScope() {
        when(permissionApi.hasAnyRoles(2001L, "pqc_permission")).thenReturn(true);
        when(scopeMapper.selectPqcEmployeeScope(3001L, 2001L)).thenReturn(null);
        when(scopeMapper.selectActivePqcEmployeeScopesByEmployeeUserId(2001L)).thenReturn(List.of());
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
    void shouldRejectPqcInspectorOccupiedByOtherLeaderBeforeInsert() {
        when(permissionApi.hasAnyRoles(2001L, "pqc_permission")).thenReturn(true);
        when(scopeMapper.selectPqcEmployeeScope(3001L, 2001L)).thenReturn(null);
        when(scopeMapper.selectActivePqcEmployeeScopesByEmployeeUserId(2001L)).thenReturn(List.of(
                MesProcessPoolTeamLeaderScopeDO.builder()
                        .id(9002L)
                        .leaderUserId(3002L)
                        .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                        .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                        .employeeUserId(2001L)
                        .enabled(Boolean.TRUE)
                        .build()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.linkFormalInspector(
                MesPqcLeaderPersonnelLinkReqBO.builder()
                        .leaderUserId(3001L)
                        .systemUserId(2001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_OCCUPIED_BY_OTHER_LEADER.getCode(),
                ex.getCode());
        verify(adminUserApi).validateUser(2001L);
        verify(scopeMapper, never()).insert(any(MesProcessPoolTeamLeaderScopeDO.class));
    }

    @Test
    void shouldRejectDuplicateActivePqcInspectorBeforeInsert() {
        when(permissionApi.hasAnyRoles(2001L, "pqc_permission")).thenReturn(true);
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
    void shouldRejectFormalInspectorWithoutPqcPermissionRoleBeforeInsert() {
        when(permissionApi.hasAnyRoles(2001L, "pqc_permission")).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.linkFormalInspector(
                MesPqcLeaderPersonnelLinkReqBO.builder()
                        .leaderUserId(3001L)
                        .systemUserId(2001L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_PERMISSION_REQUIRED.getCode(), ex.getCode());
        verify(adminUserApi).validateUser(2001L);
        verify(scopeMapper, never()).selectPqcEmployeeScope(3001L, 2001L);
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
        return user(id, username, nickname, CommonStatusEnum.ENABLE.getStatus());
    }

    private static AdminUserRespDTO user(Long id, String username, String nickname, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(status);
        return user;
    }

    private static RoleRespDTO role(Long id, String code) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setCode(code);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }

    private static List<AdminUserRespDTO> users(long startId, int count) {
        List<AdminUserRespDTO> users = new ArrayList<>();
        for (long id = startId; id < startId + count; id++) {
            users.add(user(id, "pqc" + id, "PQC检验员" + id));
        }
        return users;
    }

    private static Set<Long> userIds(List<AdminUserRespDTO> users) {
        Set<Long> ids = new LinkedHashSet<>();
        users.forEach(user -> ids.add(user.getId()));
        return ids;
    }
}
