package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileIdentityTest {
    @Test
    void derivesTenantFromAuthenticatedUserIncludingSystemTenantZero() {
        for (long tenantId : new long[]{0L, 1L}) {
            var controller = new UserProfileController();
            var users = mock(AdminUserService.class);
            var tenants = mock(TenantService.class);
            var permissions = mock(PermissionService.class);
            var roles = mock(RoleService.class);
            ReflectionTestUtils.setField(controller, "userService", users);
            ReflectionTestUtils.setField(controller, "tenantService", tenants);
            ReflectionTestUtils.setField(controller, "permissionService", permissions);
            ReflectionTestUtils.setField(controller, "roleService", roles);
            var user = AdminUserDO.builder().id(341L).username("dedicated").nickname("专用测试").build();
            user.setTenantId(tenantId);
            when(users.getUser(341L)).thenReturn(user);
            when(permissions.getUserRoleIdListByUserId(341L)).thenReturn(Set.of());
            when(roles.getRoleListFromCache(Set.of())).thenReturn(List.of());
            when(tenants.getTenant(tenantId)).thenReturn(new TenantDO().setId(tenantId).setName("正式租户"));
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(341L);
                var response = controller.getUserProfile().getData();
                assertEquals(341L, response.getId());
                assertEquals("dedicated", response.getUsername());
                assertEquals(tenantId, response.getTenantId());
                assertEquals("正式租户", response.getTenantName());
                when(tenants.getTenant(tenantId)).thenReturn(null);
                assertThrows(IllegalStateException.class, controller::getUserProfile);
                verify(users, times(2)).getUser(341L);
                verify(tenants, times(2)).getTenant(tenantId);
                verifyNoMoreInteractions(tenants);
            }
        }
    }
}
