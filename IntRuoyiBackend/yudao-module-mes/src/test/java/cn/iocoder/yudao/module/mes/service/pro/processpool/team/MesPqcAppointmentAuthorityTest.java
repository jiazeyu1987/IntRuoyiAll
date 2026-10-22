package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.system.api.permission.*;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesPqcAppointmentAuthorityTest {
    @Test void statusChangeLocksIdentityBeforeAuthorityAndAdvancesRevisionAfterCheckedWrite() {
        var scopes=mock(MesProcessPoolTeamLeaderScopeMapper.class);var users=mock(AdminUserApi.class);
        var authority=mock(ApprovalAuthorityApi.class);
        var service=new MesPqcLeaderPersonnelServiceImpl(scopes,users,mock(PermissionApi.class),mock(RoleApi.class));
        ReflectionTestUtils.setField(service,"qualificationAuthority",authority);
        var scope=MesProcessPoolTeamLeaderScopeDO.builder().id(2L).leaderUserId(3L).employeeUserId(4L)
            .leaderType("PQC").scopeType("EMPLOYEE").enabled(true).build();
        when(scopes.selectById(2L)).thenReturn(scope);when(scopes.updateById(any(MesProcessPoolTeamLeaderScopeDO.class))).thenReturn(1);
        when(authority.lockTenantApprovalAuthority(1L)).thenReturn(new ApprovalAuthorityRevisionDTO(1L,7L));
        TenantContextHolder.setTenantId(1L);
        try {
            service.updatePersonnelStatus(MesPqcLeaderPersonnelStatusUpdateReqBO.builder().leaderUserId(3L).scopeId(2L).enabled(false).build());
            var order=inOrder(users,authority,scopes);
            order.verify(scopes).selectById(2L);order.verify(users).lockSignatureIdentity(4L);
            order.verify(authority).lockTenantApprovalAuthority(1L);order.verify(scopes).selectById(2L);
            order.verify(scopes).updateById(any(MesProcessPoolTeamLeaderScopeDO.class));
            order.verify(authority).incrementLockedAuthorityRevision(1L,7L);
        } finally {TenantContextHolder.clear();}
    }
}