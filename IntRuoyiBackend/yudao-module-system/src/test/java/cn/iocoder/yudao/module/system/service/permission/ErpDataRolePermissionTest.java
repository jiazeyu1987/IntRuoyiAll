package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.enums.ErpDataPermission;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpDataRolePermissionTest extends BaseMockitoUnitTest {
    @InjectMocks private PermissionServiceImpl service;
    @Mock private RoleService roles;
    @Mock private MenuService menus;
    @Mock private RoleMenuMapper grants;
    @Mock private cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper userRoles;
    @Mock private SystemEntitlementService systemEntitlementService;
    @Mock private TemporaryRoleGrantService temporaryRoleGrantService;

    @ParameterizedTest
    @ValueSource(strings={"tenant_admin","super_admin","mes_workshop_director","mes_team_leader","mes_scheduler"})
    void oldRolesCannotRegainDataMenusOrApisFromStaleGrants(String code) {
        var ids=Set.of(1L);
        var product=new MenuDO().setId(2565L).setParentId(991511L).setStatus(0);
        var query=new MenuDO().setId(2566L).setParentId(2565L).setStatus(0).setPermission("erp:product:query");
        var unrelated=new MenuDO().setId(10L).setParentId(0L).setStatus(0).setPermission("mes:pro-feedback:query");
        when(roles.getRoleListFromCache(ids)).thenAnswer(i -> new ArrayList<>(List.of(new RoleDO().setId(1L).setCode(code).setStatus(0))));
        when(roles.hasAnySuperAdmin(ids)).thenReturn(code.equals("super_admin"));
        when(menus.getMenuList()).thenReturn(List.of(product,query,unrelated));
        when(grants.selectListByRoleId(ids)).thenReturn(List.of(new RoleMenuDO().setRoleId(1L).setMenuId(2565L),new RoleMenuDO().setRoleId(1L).setMenuId(2566L)));
        when(menus.getMenuIdListByPermissionFromCache("erp:product:query")).thenReturn(List.of(2566L));
        Set<Long> result=service.getRoleMenuListByRoleId(ids);
        assertFalse(result.contains(2565L));
        assertFalse(result.contains(2566L));
        if (code.equals("tenant_admin") || code.equals("super_admin")) assertTrue(result.contains(10L));
        assertFalse(service.hasAnyPermissionsInRoles(ids,"erp:product:query"));
    }

    @Test void addedProductRoleWorksAlongsideAdministratorWithoutGrantingOrderAccess() {
        var ids=Set.of(1L,2L);
        var roleList=List.of(new RoleDO().setId(1L).setCode("super_admin").setStatus(0),
                new RoleDO().setId(2L).setCode("erp_data_product_operator").setStatus(0));
        when(roles.getRoleListFromCache(ids)).thenAnswer(i -> new ArrayList<>(roleList));
        when(menus.getMenuIdListByPermissionFromCache("erp:product:query")).thenReturn(List.of(2566L));
        when(menus.getMenuIdListByPermissionFromCache("mes:pro-work-order:query")).thenReturn(List.of(5531L));
        when(grants.selectListByMenuId(2566L)).thenReturn(List.of(new RoleMenuDO().setRoleId(2L).setMenuId(2566L)));
        when(grants.selectListByMenuId(5531L)).thenReturn(List.of(new RoleMenuDO().setRoleId(1L).setMenuId(5531L)));
        try (var spring=mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(service);
            assertTrue(service.hasAnyPermissionsInRoles(ids,"erp:product:query"));
            assertFalse(service.hasAnyPermissionsInRoles(ids,"mes:pro-work-order:query"));
        }
    }

    @Test void permanentSuperAdminCannotUseTheApiBypassForErpData() {
        when(userRoles.selectListByUserId(7L)).thenReturn(List.of(new UserRoleDO().setUserId(7L).setRoleId(1L)));
        when(roles.getRoleListFromCache(Set.of(1L))).thenReturn(List.of(new RoleDO().setId(1L).setCode("super_admin").setStatus(0)));
        when(roles.hasAnySuperAdmin(Set.of(1L))).thenReturn(true);
        when(menus.getMenuIdListByPermissionFromCache("erp:product:query")).thenReturn(List.of(2566L));
        try (var spring=mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(service);
            assertFalse(service.hasAnyPermissions(7L,"erp:product:query"));
            assertTrue(service.hasAnyPermissions(7L,"mes:pro-feedback:query"));
        }
    }

    @Test void syncTypeCannotBorrowAnotherTabsOperationPermission() {
        assertEquals("erp:data-product:operate",ErpDataPermission.forSyncType("PRODUCT"));
        assertEquals("erp:data-order:operate",ErpDataPermission.forSyncType("PRODUCTION_ORDER"));
        assertEquals("erp:data-inventory:operate",ErpDataPermission.forSyncType("INVENTORY_DETAIL"));
        assertThrows(IllegalArgumentException.class,()->ErpDataPermission.forSyncType("UNKNOWN"));
        assertThrows(IllegalArgumentException.class,()->ErpDataPermission.forSyncType(null));
    }
}
