package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesPackagingLeaderAccessTest {
    PermissionApi permissions; AdminUserMapper users; MesPackagingLeaderAccess access;
    @BeforeEach void setup() { TenantContextHolder.setTenantId(10L); permissions=mock(PermissionApi.class);
        users=mock(AdminUserMapper.class); access=new MesPackagingLeaderAccess(permissions,users); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void explicitRoleCanAccessOtherLeadersOrderInSameTenant() {
        var user=AdminUserDO.builder().id(20L).status(0).build(); user.setTenantId(10L);
        when(users.selectById(20L)).thenReturn(user);
        when(permissions.hasAnyRoles(20L,MesPackagingLeaderAccess.ROLE)).thenReturn(true);
        var order=MesProcessPoolActiveOrderDO.builder().leaderUserId(30L).build();order.setTenantId(10L);
        assertTrue(access.canAccess(20L,order));assertEquals(30L,order.getLeaderUserId());
        order.setTenantId(11L);assertFalse(access.canAccess(20L,order));
        order.setTenantId(10L);user.setStatus(1);assertFalse(access.canAccess(20L,order));
        user.setStatus(0);user.setTenantId(11L);assertFalse(access.canAccess(20L,order));
        user.setTenantId(10L);TenantContextHolder.setIgnore(true);assertFalse(access.canAccess(20L,order));
    }
    @Test void ordinaryLeaderOrGenericAdminPermissionCannotGrantCrossTeamAccess() {
        var user=AdminUserDO.builder().id(20L).status(0).build();user.setTenantId(10L);
        when(users.selectById(20L)).thenReturn(user);
        assertFalse(access.isPackagingLeader(20L));
        verify(permissions).hasAnyRoles(20L,MesPackagingLeaderAccess.ROLE);
        assertFalse(access.isPackagingLeader(null));TenantContextHolder.clear();assertFalse(access.isPackagingLeader(20L));
    }
}
