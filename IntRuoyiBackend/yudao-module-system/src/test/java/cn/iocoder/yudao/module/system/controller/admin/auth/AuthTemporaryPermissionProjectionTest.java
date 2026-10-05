package cn.iocoder.yudao.module.system.controller.admin.auth;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthTemporaryPermissionProjectionTest {
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"active","expired","revoked","disabled","foreignRole","tempAdmin"})
 void actualAuthAndPermissionProjectionExposeOnlyExplicitCurrentTemporaryMenus(String state){
  TenantContextHolder.setTenantId(1L);
  try(var security=mockStatic(SecurityFrameworkUtils.class)){
   var controller=new AuthController();var permissions=new PermissionServiceImpl();
   var grants=mock(TemporaryRoleGrantService.class);var roles=mock(RoleService.class);var roleMenus=mock(RoleMenuMapper.class);
   var permanent=mock(UserRoleMapper.class);var entitlements=mock(SystemEntitlementService.class);
   var users=mock(AdminUserService.class);var menus=mock(MenuService.class);
   for(var e:Map.of("permissionService",permissions,"roleService",roles,"userService",users,"menuService",menus).entrySet())ReflectionTestUtils.setField(controller,e.getKey(),e.getValue());
   for(var e:Map.of("temporaryRoleGrantService",grants,"roleService",roles,"roleMenuMapper",roleMenus,"userRoleMapper",permanent,"systemEntitlementService",entitlements).entrySet())ReflectionTestUtils.setField(permissions,e.getKey(),e.getValue());
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(341L);
   var user=AdminUserDO.builder().id(341L).username("worker").nickname("正常用户").status(0).build();user.setTenantId(1L);when(users.getUser(341L)).thenReturn(user);
   when(permanent.selectListByUserId(341L)).thenReturn(List.of());when(roles.getRoleList(Set.of())).thenReturn(List.of());
   var role=new RoleDO().setId(81L).setStatus("disabled".equals(state)?1:0).setCode("tempAdmin".equals(state)?"super_admin":"temporary");role.setTenantId("foreignRole".equals(state)?2L:1L);
   when(grants.getActiveRoleIdsByUserId(eq(341L),any())).thenReturn(Set.of("expired".equals(state)||"revoked".equals(state)?new Long[]{}:new Long[]{81L}));
   when(roles.getRoleListFromCache(Set.of(81L))).thenReturn(List.of(role));
   when(roleMenus.selectListByRoleId(List.of(81L))).thenReturn(List.of(new RoleMenuDO().setRoleId(81L).setMenuId(901L)));
   var parent=new MenuDO().setId(900L).setParentId(0L).setType(MenuTypeEnum.MENU.getType()).setName("生产放行").setPath("/mes-release").setStatus(0);
   var button=new MenuDO().setId(901L).setParentId(900L).setType(MenuTypeEnum.BUTTON.getType()).setName("办理").setPermission("mes:pro-edhr-work-task:handle").setStatus(0);
   when(menus.getMenu(900L)).thenReturn(parent);
   when(menus.getMenuList(anyCollection())).thenAnswer(i->{Collection<Long> ids=i.getArgument(0);return List.of(parent,button).stream().filter(m->ids.contains(m.getId())).toList();});
   when(menus.filterDisableMenus(anyList())).thenAnswer(i->i.getArgument(0));
   var result=controller.getPermissionInfo().getData();boolean allowed=Set.of("active","tempAdmin").contains(state);
   assertEquals(allowed,result.getPermissions().contains("mes:pro-edhr-work-task:handle"));
   assertEquals(allowed?1:0,result.getMenus().size());assertTrue(result.getRoles().isEmpty());
   verify(roles,never()).hasAnySuperAdmin(anyCollection());
   verify(menus,never()).getMenuList();
  }finally{TenantContextHolder.clear();}
 }
}
