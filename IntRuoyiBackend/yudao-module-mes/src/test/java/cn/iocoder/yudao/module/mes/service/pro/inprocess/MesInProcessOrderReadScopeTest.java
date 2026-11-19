package cn.iocoder.yudao.module.mes.service.pro.inprocess;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesInProcessOrderReadScopeTest {
    private final PermissionApi permissions = mock(PermissionApi.class);
    private MesProcessPoolActiveOrderDO order;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(10L).setTenantId(1L), null));
        order = MesProcessPoolActiveOrderDO.builder().id(100L).leaderUserId(20L)
                .activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        order.setTenantId(1L);
        when(permissions.hasAnyPermissions(10L, "mes:pro-in-process-order:query")).thenReturn(true);
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }
    @Test void permitsCurrentTenantAcrossLeadersInEachListedStage() {
        for (String status : new String[]{"ACTIVE", "COMPLETED", "VERSION_UPGRADE_PENDING"}) {
            order.setBusinessStatus(status);
            assertTrue(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
        }
    }
    @Test void rejectsMissingPermissionAndDifferentActor() {
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 20L, order));
        when(permissions.hasAnyPermissions(10L, "mes:pro-in-process-order:query")).thenReturn(false);
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
    }
    @Test void rejectsOtherTenantAndIgnoredTenantScope() {
        order.setTenantId(2L);
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
        order.setTenantId(1L);
        TenantContextHolder.setIgnore(true);
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
    }
    @Test void rejectsClosedOrdersAndMissingLogin() {
        order.setActiveStatus("CLOSED");
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
        order.setActiveStatus("ACTIVE");
        order.setBusinessStatus("VOIDED");
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
        order.setBusinessStatus("ACTIVE");
        SecurityContextHolder.clearContext();
        assertFalse(MesInProcessOrderReadScope.canRead(permissions, 10L, order));
    }
}
