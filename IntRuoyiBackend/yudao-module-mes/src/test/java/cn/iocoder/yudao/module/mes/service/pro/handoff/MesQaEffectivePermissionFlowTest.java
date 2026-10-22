package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesQaEffectivePermissionFlowTest {
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"dynamic","temporary","noGrant","expired","revoked","disabledRole","disabledUser","foreignUser"})
 void userQuerySaveAndQaCreationUseTheSameRealEffectivePermissionChain(String state){
  TenantContextHolder.setTenantId(1L);
  try(var spring=mockStatic(SpringUtil.class)){
   var permission=new PermissionServiceImpl();var api=new PermissionApiImpl();
   var dynamic=mock(SystemEntitlementService.class);var temporary=mock(TemporaryRoleGrantService.class);
   var roleService=mock(RoleService.class);var menuService=mock(MenuService.class);var userRoles=mock(UserRoleMapper.class);var menuRoles=mock(RoleMenuMapper.class);
   for(var e:Map.of("systemEntitlementService",dynamic,"temporaryRoleGrantService",temporary,"roleService",roleService,"menuService",menuService,"userRoleMapper",userRoles,"roleMenuMapper",menuRoles).entrySet())ReflectionTestUtils.setField(permission,e.getKey(),e.getValue());
   spring.when(()->SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(permission);ReflectionTestUtils.setField(api,"permissionService",permission);
   String grant=MesQaHandoffAssignmentService.DISPOSE_PERMISSION;
   when(dynamic.hasAnyPermission(345L,grant)).thenReturn("dynamic".equals(state)||"disabledUser".equals(state)||"foreignUser".equals(state));
   when(userRoles.selectListByUserId(345L)).thenReturn(List.of());when(roleService.getRoleListFromCache(Set.of())).thenReturn(List.of());
   var role=new RoleDO().setId(501L).setStatus("disabledRole".equals(state)?1:0);role.setTenantId(1L);
   when(temporary.getActiveRoleIdsByUserId(eq(345L),any())).thenReturn(Set.of("temporary".equals(state)||"disabledRole".equals(state)?new Long[]{501L}:new Long[]{}));
   when(roleService.getRoleListFromCache(Set.of(501L))).thenReturn(List.of(role));when(menuService.getMenuIdListByPermissionFromCache(grant)).thenReturn(List.of(7L));
   when(menuRoles.selectListByMenuId(7L)).thenReturn(List.of(new RoleMenuDO().setMenuId(7L).setRoleId(501L)));
   var userApi=mock(AdminUserApi.class);var dto=new AdminUserRespDTO();dto.setId(345L);dto.setNickname("QA负责人");dto.setUsername("qa");dto.setStatus("disabledUser".equals(state)?1:0);
   when(userApi.getUserListByNickname("QA负责人")).thenReturn(List.of(dto));when(userApi.getUser(345L)).thenReturn(dto);
   var users=mock(AdminUserMapper.class);var account=AdminUserDO.builder().id(345L).nickname("QA负责人").status(dto.getStatus()).build();account.setTenantId("foreignUser".equals(state)?2L:1L);when(users.selectById(345L)).thenReturn(account);
   var owners=new MesActiveOrderHandoffOwnerResolver();ReflectionTestUtils.setField(owners,"users",users);ReflectionTestUtils.setField(owners,"permissions",api);
   var candidates=new MesProEdhrCandidateResolver();ReflectionTestUtils.setField(candidates,"adminUserApi",userApi);ReflectionTestUtils.setField(candidates,"permissionApi",api);
   var assignment=new MesQaHandoffAssignmentService();var rules=mock(MesProEdhrWorkTaskAssignmentRuleMapper.class);var routes=mock(MesProRouteMapper.class);
   var saved=new AtomicReference<MesProEdhrWorkTaskAssignmentRuleDO>();
   when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(MesProRouteDO.builder().id(98L).status(0).build());
   when(rules.selectListByScopeAndType("ROUTE",98L,"QA_REVIEW")).thenAnswer(i->saved.get()==null?List.of():List.of(saved.get()));
   when(rules.insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class))).thenAnswer(i->{var rule=i.getArgument(0,MesProEdhrWorkTaskAssignmentRuleDO.class);rule.setId(901L);saved.set(rule);return 1;});
   for(var e:Map.of("rules",rules,"routes",routes,"candidates",candidates,"owners",owners,"users",userApi,"permissions",api,"roles",roleService,"audit",mock(MesActiveOrderHandoffAudit.class),"operationAudit",mock(MesProEdhrOperationAuditService.class)).entrySet())ReflectionTestUtils.setField(assignment,e.getKey(),e.getValue());
   boolean allowed=Set.of("dynamic","temporary").contains(state);
   if(!"foreignUser".equals(state))assertEquals(allowed, !assignment.userOptions("QA负责人").isEmpty());
   if(!allowed){assertThrows(RuntimeException.class,()->assignment.save(98L,"USER",345L,true,"选择正式QA责任人",null));verify(rules,never()).insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class));return;}
   var rule=assignment.save(98L,"USER",345L,true,"选择正式QA责任人",null);assertEquals(345L,rule.getCandidateSourceId());assertEquals("345",assignment.resolve(98L));
   var handoff=new MesActiveOrderHandoffService();var taskMapper=mock(MesActiveOrderHandoffTaskMapper.class);var orders=mock(MesProcessPoolActiveOrderMapper.class);var workOrders=mock(MesProWorkOrderMapper.class);
   var order=MesProcessPoolActiveOrderDO.builder().id(413L).workOrderId(274L).routeId(98L).leaderUserId(341L).activeStatus("ACTIVE").build();order.setTenantId(1L);when(orders.selectByIdForUpdate(413L)).thenReturn(order);
   var work=MesProWorkOrderDO.builder().id(274L).code("EDHR-413").build();work.setTenantId(1L);when(workOrders.selectById(274L)).thenReturn(work);
   var initiator=AdminUserDO.builder().id(341L).nickname("原组长").status(0).build();initiator.setTenantId(1L);when(users.selectById(341L)).thenReturn(initiator);
   when(taskMapper.insert(any(MesActiveOrderHandoffTaskDO.class))).thenReturn(1);
   for(var e:Map.of("tasks",taskMapper,"orders",orders,"workOrders",workOrders,"owners",owners,"qaAssignment",assignment,"audit",mock(MesActiveOrderHandoffAudit.class),"delivery",mock(MesActiveOrderHandoffDeliveryService.class)).entrySet())ReflectionTestUtils.setField(handoff,e.getKey(),e.getValue());
   var review=new MesProEdhrNonconformanceReviewDO().setId(91L).setWorkOrderId(274L).setNonconformanceReason("真实QA原因");review.setTenantId(1L);handoff.qaCreated(review,413L,341L);
   var task=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(taskMapper).insert(task.capture());assertEquals("345",task.getValue().getCandidateUserSnapshot());assertEquals("QA_REVIEW",task.getValue().getTaskType());
   verify(userRoles,never()).selectListByRoleIds(anyCollection());
  }finally{TenantContextHolder.clear();}
 }
}
