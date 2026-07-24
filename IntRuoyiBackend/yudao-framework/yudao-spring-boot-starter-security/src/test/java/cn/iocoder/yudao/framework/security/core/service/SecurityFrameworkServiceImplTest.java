package cn.iocoder.yudao.framework.security.core.service;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFrameworkServiceImplTest {

    @Mock
    private PermissionCommonApi permissionApi;

    @InjectMocks
    private SecurityFrameworkServiceImpl securityFrameworkService;

    @Test
    void hasAnyPermissions_crossTenantVisitStillChecksRealPermission() {
        LoginUser loginUser = createLoginUser(100L, 1L, 2L, List.of("scope-a"));
        when(permissionApi.hasAnyPermissions(100L, "dcc:controlled-file:category:manage")).thenReturn(false);

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);

            assertFalse(securityFrameworkService.hasAnyPermissions("dcc:controlled-file:category:manage"));
        }
    }

    @Test
    void hasAnyRoles_crossTenantVisitStillChecksRealRole() {
        LoginUser loginUser = createLoginUser(100L, 1L, 2L, List.of("scope-a"));
        when(permissionApi.hasAnyRoles(100L, "doc_control")).thenReturn(false);

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);

            assertFalse(securityFrameworkService.hasAnyRoles("doc_control"));
        }
    }

    @Test
    void hasAnyScopes_crossTenantVisitStillChecksRealScope() {
        LoginUser loginUser = createLoginUser(100L, 1L, 2L, List.of("dcc:controlled-file:query"));

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);

            assertTrue(securityFrameworkService.hasAnyScopes("dcc:controlled-file:query"));
            assertFalse(securityFrameworkService.hasAnyScopes("dcc:controlled-file:category:manage"));
            verifyNoInteractions(permissionApi);
        }
    }

    private LoginUser createLoginUser(Long userId, Long tenantId, Long visitTenantId, List<String> scopes) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setTenantId(tenantId);
        loginUser.setVisitTenantId(visitTenantId);
        loginUser.setScopes(scopes);
        return loginUser;
    }
}
