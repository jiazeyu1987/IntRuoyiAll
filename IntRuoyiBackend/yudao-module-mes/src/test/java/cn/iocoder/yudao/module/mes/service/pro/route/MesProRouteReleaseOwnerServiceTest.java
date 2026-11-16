package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProRouteReleaseOwnerServiceTest {
    private static final String CODE = "MES_PQC_RELEASE_OWNER";
    @Mock private RoleApi roles;
    @Mock private PermissionApi permissions;
    @Mock private AdminUserApi users;
    @InjectMocks private MesProRouteReleaseOwnerService service;
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    private RoleRespDTO role() {
        return new RoleRespDTO().setId(23L).setName("PQC负责人").setCode(CODE).setStatus(0);
    }
    private AdminUserRespDTO user(long id, long tenantId) {
        return new AdminUserRespDTO().setId(id).setTenantId(tenantId)
                .setUsername("user" + id).setNickname("人员" + id).setStatus(0).setMobile("private");
    }

    @Test void readsAllMembersAndReturnsOnlyDisplayFields() {
        when(roles.getRoleByCode(CODE)).thenReturn(role());
        var members = IntStream.rangeClosed(1, 101).mapToObj(i -> user(i, 1)).toList();
        members.get(100).setStatus(1);
        Set<Long> ids = members.stream().map(AdminUserRespDTO::getId).collect(java.util.stream.Collectors.toSet());
        when(permissions.getUserRoleIdListByRoleIds(Set.of(23L))).thenReturn(ids);
        when(users.getUserList(ids)).thenReturn(members);
        var result = service.getOwner(CODE);
        assertEquals(101, result.users().size());
        assertEquals(1, result.users().get(100).status());
        assertEquals("PQC负责人", result.role().name());
        assertFalse(result.toString().contains("private"));
    }

    @Test void emptyRoleKeepsRoleIdentity() {
        when(roles.getRoleByCode(CODE)).thenReturn(role());
        when(permissions.getUserRoleIdListByRoleIds(Set.of(23L))).thenReturn(Set.of());
        var result = service.getOwner(CODE);
        assertEquals(23L, result.role().id());
        assertTrue(result.users().isEmpty());
        verifyNoInteractions(users);
    }

    @Test void arbitraryRolesCannotBeEnumerated() {
        assertThrows(ServiceException.class, () -> service.getOwner("super_admin"));
        verifyNoInteractions(roles, permissions, users);
    }

    @Test void missingDuplicateAndDisabledRolesFailExplicitly() {
        when(roles.getRoleByCode(CODE)).thenReturn(null);
        assertTrue(assertThrows(ServiceException.class, () -> service.getOwner(CODE)).getMessage().contains("未配置"));
        when(roles.getRoleByCode(CODE)).thenThrow(new TooManyResultsException());
        assertTrue(assertThrows(ServiceException.class, () -> service.getOwner(CODE)).getMessage().contains("唯一"));
        doReturn(role().setStatus(1)).when(roles).getRoleByCode(CODE);
        assertTrue(assertThrows(ServiceException.class, () -> service.getOwner(CODE)).getMessage().contains("停用"));
        verifyNoInteractions(permissions, users);
    }

    @Test void foreignAndMissingUsersFailInsteadOfAppearingUnassigned() {
        when(roles.getRoleByCode(CODE)).thenReturn(role());
        when(permissions.getUserRoleIdListByRoleIds(Set.of(23L))).thenReturn(Set.of(8L));
        when(users.getUserList(Set.of(8L))).thenReturn(List.of(user(8, 2)));
        assertThrows(ServiceException.class, () -> service.getOwner(CODE));
        when(users.getUserList(Set.of(8L))).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.getOwner(CODE));
    }

    @Test void missingTenantStopsBeforeAnyLookup() {
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, () -> service.getOwner(CODE));
        verifyNoInteractions(roles, permissions, users);
    }
}
