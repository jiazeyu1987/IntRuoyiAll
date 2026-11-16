package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesTeamRosterImportServiceTest {
    @Mock AdminUserService users;
    @Mock RoleService roles;
    @Mock PermissionService permissions;
    @Mock MesTeamLeaderRuntimeConfigService runtime;
    MesTeamRosterImportService service;

    @BeforeEach void setup() {
        service = new MesTeamRosterImportService();
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        ReflectionTestUtils.setField(service, "runtimeConfigService", runtime);
    }
    static MesTeamRosterRow row(String name, String type, String role) {
        var row = new MesTeamRosterRow();
        row.setWorkshop("压力泵"); row.setName(name); row.setEmploymentType(type); row.setRole(role);
        return row;
    }
    static MesTeamRosterRow leader() { return row("张飞红", "", "组长"); }
    static AdminUserDO user(long id, String name, String username) {
        return new AdminUserDO().setId(id).setNickname(name).setUsername(username).setStatus(0);
    }
    @Test void requiresOneLeaderAndOneWorkshop() {
        assertThrows(ServiceException.class, () -> MesTeamRosterImportService.validateRoster(List.of(row("张三", "正式工", "组员"))));
        var another = row("李四", "正式工", "组员"); another.setWorkshop("其他车间");
        assertThrows(ServiceException.class, () -> MesTeamRosterImportService.validateRoster(List.of(leader(), another)));
    }
    @Test void rejectsDuplicateNamesAndUnknownTypes() {
        assertThrows(ServiceException.class, () -> MesTeamRosterImportService.validateRoster(List.of(leader(), row("张三", "正式工", "组员"), row("张三", "临时工", "组员"))));
        assertThrows(ServiceException.class, () -> MesTeamRosterImportService.validateRoster(List.of(leader(), row("张三", "未知", "组员"))));
    }
    @Test void trimsNamesAndGeneratesPinyinWithExplicitAccountOverride() {
        var leader = leader(); leader.setName(" 张飞红 ");
        MesTeamRosterImportService.validateRoster(List.of(leader));
        assertEquals("zhangfeihong", MesTeamRosterImportService.usernameFor(leader));
        leader.setUsername("leader01");
        assertEquals("leader01", MesTeamRosterImportService.usernameFor(leader));
    }
    @Test void refusesPinyinCollision() {
        assertThrows(ServiceException.class, () -> MesTeamRosterImportService.validateRoster(List.of(leader(), row("张珊", "正式工", "组员"), row("张山", "正式工", "组员"))));
    }
    @Test void missingRoleStopsBeforeCreatingAccounts() {
        when(roles.getRoleList()).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.importLeader(List.of(leader())));
        verifyNoInteractions(users, runtime);
    }
    @Test void leaderImportKeepsExistingRolesAndDoesNotResetPassword() {
        when(roles.getRoleList()).thenReturn(List.of(new RoleDO().setId(7L).setName("生产组长权限角色").setStatus(0)));
        when(permissions.hasAnyPermissionsInRoles(anyCollection(), any(String[].class))).thenReturn(true);
        when(users.getUserByUsername("zhangfeihong")).thenReturn(user(10, "张飞红", "zhangfeihong"));
        when(permissions.getUserRoleIdListByUserId(10L)).thenReturn(Set.of(2L));
        var result = service.importLeader(List.of(leader()));
        assertEquals("zhangfeihong", result.get(0).username());
        verify(permissions).assignUserRole(eq(10L), eq(Set.of(2L, 7L)), contains("压力泵"), startsWith("team-roster:"));
        verify(users, never()).createUser(any());
        verifyNoInteractions(runtime);
    }
    @Test void membersCannotBeImportedIntoAnotherLeadersTeam() {
        when(users.getUserByUsername("zhangfeihong")).thenReturn(user(10, "张飞红", "zhangfeihong"));
        assertThrows(ServiceException.class, () -> service.importMembers(11L, List.of(leader(), row("张三", "正式工", "组员"))));
        verifyNoInteractions(runtime, permissions);
        verify(users, never()).createUser(any());
    }
    @Test void createsFormalAccountAndTemporarySignatureThenLinksOnlyToCurrentLeader() {
        when(users.getUserByUsername("zhangfeihong")).thenReturn(user(10, "张飞红", "zhangfeihong"));
        when(users.getUserListByNickname("张三")).thenReturn(List.of());
        when(users.createUser(any())).thenReturn(20L);
        when(users.getUser(20L)).thenReturn(user(20, "张三", "zhangsan"));
        when(runtime.listEmployeeProfiles(10L, null)).thenReturn(List.of());
        var result = service.importMembers(10L, List.of(leader(), row("张三", "正式工", "组员"), row("李四", "临时工", "组员")));
        var created = ArgumentCaptor.forClass(UserSaveReqVO.class);
        verify(users).createUser(created.capture());
        assertEquals("Int@2025", created.getValue().getPassword());
        verify(runtime).linkFormalEmployee(argThat(r -> r.getLeaderUserId().equals(10L) && r.getSystemUserId().equals(20L)));
        verify(runtime).createTemporaryEmployee(argThat(r -> r.getLeaderUserId().equals(10L) && r.getSignaturePassword().equals("Int@2025")));
        assertEquals(2, result.size());
        verifyNoInteractions(permissions);
    }
    @Test void repeatedTemporaryImportKeepsExistingCredential() {
        when(users.getUserByUsername("zhangfeihong")).thenReturn(user(10, "张飞红", "zhangfeihong"));
        when(runtime.listEmployeeProfiles(10L, null)).thenReturn(List.of(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(30L).displayName("李四").employeeType("TEMPORARY").enabled(true).build()));
        var result = service.importMembers(10L, List.of(leader(), row("李四", "临时工", "组员")));
        assertEquals("已存在，保留原密码", result.get(0).result());
        verify(runtime, never()).createTemporaryEmployee(any());
        verify(users, never()).createUser(any());
    }
    @Test void refusesToBindAccountBelongingToDifferentName() {
        when(users.getUserByUsername("zhangfeihong")).thenReturn(user(10, "其他人", "zhangfeihong"));
        assertThrows(ServiceException.class, () -> service.importMembers(10L, List.of(leader())));
        verifyNoInteractions(runtime);
    }
}
