package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityRevisionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Formal create flow with repository boundaries; no deployment schema or concurrency claim. */
class TenantApprovalInitializationTest {
    private final TenantMapper tenants = mock(TenantMapper.class);
    private final ApprovalAuthorityRevisionMapper revisions = mock(ApprovalAuthorityRevisionMapper.class);
    private final GxpAuditService audit = mock(GxpAuditService.class);
    private final TenantPackageService packages = mock(TenantPackageService.class);
    private final RoleService roles = mock(RoleService.class);
    private final AdminUserService users = mock(AdminUserService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final TenantServiceImpl service = new TenantServiceImpl();
    private final TenantSaveReqVO request = new TenantSaveReqVO();
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "tenantMapper", tenants);
        ReflectionTestUtils.setField(service, "approvalAuthorityRevisionMapper", revisions);
        ReflectionTestUtils.setField(service, "approvalAuditService", audit);
        ReflectionTestUtils.setField(service, "tenantPackageService", packages);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        request.setName("task tenant"); request.setPackageId(4L); request.setUsername("taskadmin");
        TenantPackageDO bundle = new TenantPackageDO(); bundle.setId(4L); bundle.setMenuIds(Set.of(2L));
        when(packages.validTenantPackage(4L)).thenReturn(bundle);
        when(tenants.insert(any(TenantDO.class))).thenAnswer(call -> {
            call.<TenantDO>getArgument(0).setId(24L); return 1;
        });
        when(revisions.initializeNewTenant(24L)).thenAnswer(call -> {
            assertEquals(24L, TenantContextHolder.getRequiredTenantId()); return 1;
        });
        when(roles.createRole(any(), anyInt())).thenReturn(31L);
        when(users.createUser(any())).thenReturn(42L);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void newTenantInitializesAuthorityBeforeAdministratorProvisioning() {
        assertEquals(24L, service.createTenant(request));
        var order = inOrder(tenants, audit, revisions, roles, users, permissions);
        order.verify(tenants).insert(any(TenantDO.class));
        order.verify(audit).acquireLedgerLock();
        order.verify(revisions).initializeNewTenant(24L);
        order.verify(roles).createRole(any(), anyInt());
        order.verify(users).createUser(any());
        order.verify(permissions).initializeTenantAdministrator(eq(24L), eq(42L), eq(31L), anyString(), anyString());
        assertEquals(7L, TenantContextHolder.getRequiredTenantId());
    }
    @Test void failedInitializationCannotCreateAdministratorOrRoleAndRestoresTenantContext() {
        doReturn(0).when(revisions).initializeNewTenant(24L);
        assertThrows(IllegalStateException.class, () -> service.createTenant(request));
        verifyNoInteractions(roles, users, permissions);
        assertEquals(7L, TenantContextHolder.getRequiredTenantId());
    }
    @Test void incompleteTenantInsertCannotCreateAuthorityRow() {
        when(tenants.insert(any(TenantDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.createTenant(request));
        verifyNoInteractions(revisions, audit, roles, users, permissions);
    }
}
