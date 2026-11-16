package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.LocalDateTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformConfigurationAccessTest extends BaseMockitoUnitTest {
    @InjectMocks private PermissionServiceImpl service;
    @Mock private TenantMapper tenants;
    @Mock private SystemEntitlementService entitlements;
    @Mock private RoleService roles;
    @Mock private MenuService menus;
    @Mock private TemporaryRoleGrantService temporary;

    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    private LoginUser login(long tenant) {
        LoginUser user = new LoginUser().setId(100L).setTenantId(tenant).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null));
        TenantContextHolder.setTenantId(tenant);
        return user;
    }
    private TenantDO tenant(long id, long packageId) {
        return new TenantDO().setId(id).setPackageId(packageId).setStatus(0).setExpireTime(LocalDateTime.now().plusYears(1));
    }

    @ParameterizedTest
    @ValueSource(strings={"system:menu:create","system:menu:update","system:menu:delete","system:tenant:update",
            "system:tenant-package:update","infra:codegen:update","infra:data-source-config:query"})
    void businessTenantCannotReachDynamicSuperadminOrTemporaryGrants(String permission) {
        login(22L);
        when(tenants.selectById(22L)).thenReturn(tenant(22L,1L));
        assertFalse(service.hasAnyPermissions(100L,permission));
        assertFalse(service.hasAnyPermissionsInRoles(Set.of(1L),permission));
        verifyNoInteractions(entitlements,roles,temporary);
    }

    @Test void platformIdentityMustReturnToItsOwnTenantBeforePlatformChanges() {
        LoginUser user=login(1L);
        user.setVisitTenantId(22L);
        TenantContextHolder.setTenantId(22L);
        when(tenants.selectById(22L)).thenReturn(tenant(22L,1L));
        assertFalse(service.hasAnyPermissions(100L,"system:menu:update"));
        TenantContextHolder.setTenantId(1L);
        user.setVisitTenantId(null);
        when(tenants.selectById(1L)).thenReturn(tenant(1L,0L));
        when(entitlements.hasAnyPermission(100L,"system:menu:update")).thenReturn(true);
        assertTrue(service.hasAnyPermissions(100L,"system:menu:update"));
    }

    @Test void platformContextDoesNotGrantPermissionByItself() {
        login(1L);
        when(tenants.selectById(1L)).thenReturn(tenant(1L,0L));
        PermissionServiceImpl spy = spy(service);
        doReturn(Set.of()).when(spy).getUserRoleIdListByUserIdFromCache(100L);
        try (var spring = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            spring.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(spy);
            assertFalse(spy.hasAnyPermissions(100L,"system:menu:update"));
        }
    }

    @Test void mixedChecksKeepAuthorizedBusinessPermissionsWithoutConsultingBlockedGrant() {
        login(22L);
        when(tenants.selectById(22L)).thenReturn(tenant(22L,1L));
        when(entitlements.hasAnyPermission(100L,"system:dict:update")).thenReturn(true);
        assertTrue(service.hasAnyPermissions(100L,"system:menu:update","system:dict:update"));
        verify(entitlements,never()).hasAnyPermission(100L,"system:menu:update");
    }

    @Test void missingDisabledExpiredOrMismatchedContextIsDenied() {
        LoginUser user=login(1L);
        TenantDO platform=tenant(1L,0L);
        assertTrue(PlatformConfigurationAccess.isPlatformContext(user,100L,1L,platform));
        assertFalse(PlatformConfigurationAccess.isPlatformContext(user,101L,1L,platform));
        assertFalse(PlatformConfigurationAccess.isPlatformContext(user,100L,null,platform));
        assertFalse(PlatformConfigurationAccess.isPlatformContext(null,100L,1L,platform));
        assertFalse(PlatformConfigurationAccess.isPlatformContext(user,100L,1L,null));
        platform.setStatus(1);
        assertFalse(PlatformConfigurationAccess.isPlatformContext(user,100L,1L,platform));
        platform.setStatus(0).setExpireTime(LocalDateTime.now().minusDays(1));
        assertFalse(PlatformConfigurationAccess.isPlatformContext(user,100L,1L,platform));
    }
}
