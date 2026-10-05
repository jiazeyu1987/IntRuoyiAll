package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.permission.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesHandoffUnifiedPermissionTest {
    @Test void realUserOptionsUsesUnifiedDynamicAndTemporaryGrantAndExcludesRevokedExpiredOrDisabled() {
        TenantContextHolder.setTenantId(1L);
        try(var spring=mockStatic(SpringUtil.class)) {
            var realPermissions=new PermissionServiceImpl();var api=new PermissionApiImpl();
            var dynamic=mock(SystemEntitlementService.class);var temporary=mock(TemporaryRoleGrantService.class);
            var roleService=mock(RoleService.class);var menuService=mock(MenuService.class);
            var userRoles=mock(UserRoleMapper.class);var menuRoles=mock(RoleMenuMapper.class);
            for(var e:Map.of("systemEntitlementService",dynamic,"temporaryRoleGrantService",temporary,"roleService",roleService,
                    "menuService",menuService,"userRoleMapper",userRoles,"roleMenuMapper",menuRoles).entrySet())
                ReflectionTestUtils.setField(realPermissions,e.getKey(),e.getValue());
            spring.when(()->SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(realPermissions);
            ReflectionTestUtils.setField(api,"permissionService",realPermissions);
            var service=new MesPqcHandoffAssignmentService();
            var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
            ReflectionTestUtils.setField(service,"permissions",api);ReflectionTestUtils.setField(service,"users",users);
            var user=new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO();
            user.setId(344L);user.setNickname("正式PQC");user.setUsername("pqc");user.setStatus(0);
            when(users.getUserListByNickname("正式PQC")).thenReturn(List.of(user));
            String permission=MesPqcHandoffAssignmentService.CREATE_PERMISSION;
            var enabled=new AtomicBoolean(true);
            when(dynamic.hasAnyPermission(344L,permission)).thenAnswer(i->enabled.get());
            assertEquals(List.of(new MesPqcHandoffAssignmentService.Option(344L,"正式PQC（pqc）")),service.userOptions("正式PQC"));
            verifyNoInteractions(userRoles,temporary);
            enabled.set(false);when(userRoles.selectListByUserId(344L)).thenReturn(List.of());
            when(roleService.getRoleListFromCache(Set.of())).thenReturn(List.of());
            when(temporary.getActiveRoleIdsByUserId(eq(344L),any())).thenReturn(Set.of());
            when(menuService.getMenuIdListByPermissionFromCache(permission)).thenReturn(List.of(7L));
            var menuRole=new RoleMenuDO();menuRole.setRoleId(501L);menuRole.setMenuId(7L);
            when(menuRoles.selectListByMenuId(7L)).thenReturn(List.of(menuRole));
            assertTrue(service.userOptions("正式PQC").isEmpty());
            var role=new RoleDO();role.setId(501L);role.setStatus(0);
            when(roleService.getRoleListFromCache(Set.of(501L))).thenReturn(List.of(role));
            when(temporary.getActiveRoleIdsByUserId(eq(344L),any())).thenReturn(Set.of(501L));
            assertEquals(344L,service.userOptions("正式PQC").get(0).id());verify(temporary).recordPermissionUse(344L,permission);
            role.setStatus(1);assertTrue(service.userOptions("正式PQC").isEmpty());
            role.setStatus(0);when(temporary.getActiveRoleIdsByUserId(eq(344L),any())).thenReturn(Set.of());
            assertTrue(service.userOptions("正式PQC").isEmpty());
            enabled.set(true);user.setStatus(1);assertTrue(service.userOptions("正式PQC").isEmpty());
        } finally {TenantContextHolder.clear();}
    }
    @Test void noPermanentRoleUsesUnifiedDynamicAndTemporaryPermissionsWithoutLosingTenantChecks() {
        TenantContextHolder.setTenantId(1L);
        try(var spring=mockStatic(SpringUtil.class)) {
            var realPermissions=new PermissionServiceImpl();var api=new PermissionApiImpl();
            var dynamic=mock(SystemEntitlementService.class);var temporary=mock(TemporaryRoleGrantService.class);
            var roleService=mock(RoleService.class);var menuService=mock(MenuService.class);
            var userRoles=mock(UserRoleMapper.class);var menuRoles=mock(RoleMenuMapper.class);
            for(var e:Map.of("systemEntitlementService",dynamic,"temporaryRoleGrantService",temporary,"roleService",roleService,
                    "menuService",menuService,"userRoleMapper",userRoles,"roleMenuMapper",menuRoles).entrySet())
                ReflectionTestUtils.setField(realPermissions,e.getKey(),e.getValue());
            spring.when(()->SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(realPermissions);
            ReflectionTestUtils.setField(api,"permissionService",realPermissions);
            var owners=new MesActiveOrderHandoffOwnerResolver();var users=mock(AdminUserMapper.class);
            ReflectionTestUtils.setField(owners,"permissions",api);ReflectionTestUtils.setField(owners,"users",users);
            var user=AdminUserDO.builder().id(341L).nickname("原责任组长").status(0).build();user.setTenantId(1L);
            when(users.selectById(341L)).thenReturn(user);
            String permission="mes:pro-process-pool-team-leader:review";
            var enabled=new AtomicBoolean(true);
            when(dynamic.hasAnyPermission(341L,permission)).thenAnswer(i->enabled.get());
            assertDoesNotThrow(()->owners.user(341L,permission));
            verifyNoInteractions(userRoles,temporary);
            enabled.set(false);
            when(userRoles.selectListByUserId(341L)).thenReturn(List.of());
            when(roleService.getRoleListFromCache(Set.of())).thenReturn(List.of());
            when(temporary.getActiveRoleIdsByUserId(eq(341L),any())).thenReturn(Set.of());
            when(menuService.getMenuIdListByPermissionFromCache(permission)).thenReturn(List.of(7L));
            var menuRole=new RoleMenuDO();menuRole.setRoleId(501L);menuRole.setMenuId(7L);
            when(menuRoles.selectListByMenuId(7L)).thenReturn(List.of(menuRole));
            assertThrows(RuntimeException.class,()->owners.user(341L,permission));
            var role=new RoleDO();role.setId(501L);role.setStatus(0);
            when(roleService.getRoleListFromCache(Set.of(501L))).thenReturn(List.of(role));
            when(temporary.getActiveRoleIdsByUserId(eq(341L),any())).thenReturn(Set.of(501L));
            assertDoesNotThrow(()->owners.user(341L,permission));verify(temporary).recordPermissionUse(341L,permission);
            role.setStatus(1);assertThrows(RuntimeException.class,()->owners.user(341L,permission));
            role.setStatus(0);when(temporary.getActiveRoleIdsByUserId(eq(341L),any())).thenReturn(Set.of());
            assertThrows(RuntimeException.class,()->owners.user(341L,permission));
            enabled.set(true);user.setTenantId(2L);assertThrows(RuntimeException.class,()->owners.user(341L,permission));
            user.setTenantId(1L);user.setStatus(1);assertThrows(RuntimeException.class,()->owners.user(341L,permission));
        } finally { TenantContextHolder.clear(); }
    }
}
