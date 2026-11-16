package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesPackagingSubmissionOwnerTest {
    @AfterEach void clear(){TenantContextHolder.clear();}
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void reviewUsesEmployeeTeamAndRequiresExplicitCrossTeamRole(boolean packaging) {
        TenantContextHolder.setTenantId(10L);
        var users=mock(AdminUserMapper.class);var permissions=mock(PermissionApi.class);
        var profiles=mock(MesProcessPoolTeamEmployeeProfileMapper.class);
        var scope=mock(MesTeamLeaderScopeService.class);
        var access=new MesPackagingLeaderAccess(permissions,users);
        var owner=spy(new MesActiveOrderHandoffOwnerResolver());
        for(var entry:Map.of("users",users,"permissions",permissions,"profiles",profiles,"leaderScopes",scope,"packagingAccess",access).entrySet())
            ReflectionTestUtils.setField(owner,entry.getKey(),entry.getValue());
        var event=MesProProcessPoolEventDO.builder().id(1L).workOrderId(2L).actualEmployeeId(40L).eventType("PRODUCTION_SUBMIT").build();event.setTenantId(10L);
        doReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER",40L,40L)).when(owner).submissionIdentity(event);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).workOrderId(2L).leaderUserId(30L).build();order.setTenantId(10L);
        var profile=MesProcessPoolTeamEmployeeProfileDO.builder().id(5L).systemUserId(40L).leaderUserId(20L).employeeType("FORMAL").enabled(true).build();profile.setTenantId(10L);
        when(profiles.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(profile));
        var leader=AdminUserDO.builder().id(20L).status(0).build();leader.setTenantId(10L);when(users.selectById(20L)).thenReturn(leader);
        when(permissions.hasAnyRoles(20L,MesPackagingLeaderAccess.ROLE)).thenReturn(packaging);
        when(permissions.hasAnyPermissions(20L,"mes:pro-process-pool-team-leader:review")).thenReturn(true);
        if(packaging) {
            assertEquals(20L,owner.submissionProductionLeader(event,order));
            verify(scope).assertCanAccessEmployee(20L,"PRODUCTION",40L);
            assertEquals(30L,order.getLeaderUserId());
            profile.setEnabled(false);assertThrows(RuntimeException.class,()->owner.submissionProductionLeader(event,order));
            profile.setEnabled(true);profile.setTenantId(11L);assertThrows(RuntimeException.class,()->owner.submissionProductionLeader(event,order));
        } else assertThrows(RuntimeException.class,()->owner.submissionProductionLeader(event,order));
    }
}
