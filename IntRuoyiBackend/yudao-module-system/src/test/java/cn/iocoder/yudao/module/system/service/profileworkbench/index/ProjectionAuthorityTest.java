package cn.iocoder.yudao.module.system.service.profileworkbench.index;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionAuthorityTest {
    final AdminUserMapper users=mock(AdminUserMapper.class);final RoleMapper roles=mock(RoleMapper.class);
    final UserRoleMapper userRoles=mock(UserRoleMapper.class);final RoleMenuMapper roleMenus=mock(RoleMenuMapper.class);
    final MenuMapper menus=mock(MenuMapper.class);final TemporaryRoleGrantMapper temporary=mock(TemporaryRoleGrantMapper.class);
    final SystemEntitlementGrantMapper entitlements=mock(SystemEntitlementGrantMapper.class);final TenantMapper tenants=mock(TenantMapper.class);
    final SystemWorkbenchAuthorityExport export=new SystemWorkbenchAuthorityExport(users,roles,userRoles,roleMenus,menus,temporary,entitlements,tenants);
    final LocalDateTime userEnd=LocalDateTime.of(2030,1,1,0,0),tenantEnd=LocalDateTime.of(2031,1,1,0,0);
    final TenantDO tenant=new TenantDO();
    final WorkbenchAuthorityExport.Requirement requirement=new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_PERMISSION,Set.of("p"));
    @BeforeEach void setup(){
        TransactionSynchronizationManager.setActualTransactionActive(true);
        tenant.setId(2L);tenant.setStatus(0);tenant.setExpireTime(tenantEnd);when(tenants.selectById(2L)).thenReturn(tenant);
        AdminUserDO user=new AdminUserDO();user.setId(5L);user.setTenantId(2L);user.setStatus(0);user.setLifecycleEffectiveTime(userEnd);
        when(users.selectList(any(Wrapper.class))).thenAnswer(i->{assertNotNull(DataPermissionContextHolder.get());assertFalse(DataPermissionContextHolder.get().enable());return List.of(user);});
        RoleDO role=new RoleDO();role.setId(10L);role.setTenantId(2L);role.setStatus(0);role.setCode("editor");
        when(roles.selectList(any(Wrapper.class))).thenReturn(List.of(role));
        MenuDO menu=new MenuDO();menu.setId(8L);menu.setPermission("p");when(menus.selectList(any(Wrapper.class))).thenReturn(List.of(menu));
        RoleMenuDO relation=new RoleMenuDO();relation.setId(12L);relation.setRoleId(10L);relation.setMenuId(8L);relation.setTenantId(2L);
        when(roleMenus.selectList(any(Wrapper.class))).thenReturn(List.of(relation));
        when(userRoles.selectList(any(Wrapper.class))).thenReturn(List.of());when(temporary.selectList(any(Wrapper.class))).thenReturn(List.of());when(entitlements.selectList(any(Wrapper.class))).thenReturn(List.of());
    }
    @AfterEach void clear(){TransactionSynchronizationManager.clear();DataPermissionContextHolder.clear();}
    @Test void completeAuthorityExportIgnoresWriterScopeAndRestoresItAfterwards(){
        UserRoleDO relation=new UserRoleDO();relation.setId(20L);relation.setUserId(5L);relation.setRoleId(10L);relation.setTenantId(2L);when(userRoles.selectList(any(Wrapper.class))).thenReturn(List.of(relation));
        var grants=export.exportGrants(2,requirement);assertEquals(1,grants.size());assertEquals("role:20",grants.get(0).grantKey());assertEquals(userEnd.atZone(ZoneId.systemDefault()).toInstant(),grants.get(0).validUntil());assertNull(DataPermissionContextHolder.get());
    }
    @Test void futureTemporaryGrantRetainsIndependentIdentityAndHalfOpenBoundary(){
        TemporaryRoleGrantDO a=new TemporaryRoleGrantDO();a.setId(21L);a.setUserId(5L);a.setRoleId(10L);a.setEffectiveTime(userEnd.minusDays(1));a.setExpireTime(userEnd.plusDays(1));
        TemporaryRoleGrantDO b=new TemporaryRoleGrantDO();b.setId(22L);b.setUserId(5L);b.setRoleId(10L);b.setEffectiveTime(userEnd.minusDays(2));b.setExpireTime(userEnd.minusHours(1));
        when(temporary.selectList(any(Wrapper.class))).thenReturn(List.of(a,b));
        var grants=export.exportGrants(2,requirement);assertEquals(2,grants.size());assertEquals(Set.of("temporary:21","temporary:22"),grants.stream().map(WorkbenchAuthorityExport.Grant::grantKey).collect(java.util.stream.Collectors.toSet()));
        assertEquals(userEnd.atZone(ZoneId.systemDefault()).toInstant(),grants.get(0).validUntil());assertEquals(userEnd.minusDays(1).atZone(ZoneId.systemDefault()).toInstant(),grants.get(0).validFrom());
    }
    @Test void disabledTenantProducesNoAuthorityAndNeedsNoUserScan(){tenant.setStatus(1);assertTrue(export.exportGrants(2,requirement).isEmpty());verifyNoInteractions(users,roles);}
    @Test void exportFailureDoesNotLeakUnrestrictedDataPermissionContext(){when(users.selectList(any(Wrapper.class))).thenThrow(new IllegalStateException("formal scan failed"));assertThrows(IllegalStateException.class,()->export.exportGrants(2,requirement));assertNull(DataPermissionContextHolder.get());}
    @Test void missingFrozenTransactionFailsBeforeFormalReads(){TransactionSynchronizationManager.setActualTransactionActive(false);assertThrows(IllegalStateException.class,()->export.exportGrants(2,requirement));verifyNoInteractions(users,roles,tenants);}
}
