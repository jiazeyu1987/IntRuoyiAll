package cn.iocoder.yudao.module.ai.tool;

import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntruoyiMcpReadOnlyToolsTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private AdminUserService adminUserService;

    @Test
    void getSystemSummaryShouldReturnRealReadOnlyCounts() {
        when(tenantService.getTenantIdList()).thenReturn(List.of(1L, 2L, 3L));
        when(tenantService.getTenantListByStatus(anyInt())).thenReturn(List.of(
                new TenantDO().setId(1L),
                new TenantDO().setId(2L)
        ));
        when(adminUserService.getUserListByStatus(anyInt())).thenReturn(List.of(
                new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(11L),
                new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(12L),
                new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(13L)
        ));

        IntruoyiMcpReadOnlyTools tools = new IntruoyiMcpReadOnlyTools(tenantService, adminUserService);

        IntruoyiMcpReadOnlyTools.SystemSummary summary = tools.getSystemSummary();

        assertEquals(3, summary.tenantCount());
        assertEquals(2, summary.enabledTenantCount());
        assertEquals(3, summary.enabledUserCount());
    }

    @Test
    void getTenantSummaryShouldExposeOnlySafeFields() {
        LocalDateTime expireTime = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(tenantService.getTenant(7L)).thenReturn(new TenantDO()
                .setId(7L)
                .setName("芋道源码")
                .setStatus(0)
                .setExpireTime(expireTime)
                .setContactMobile("should-not-be-exposed"));

        IntruoyiMcpReadOnlyTools tools = new IntruoyiMcpReadOnlyTools(tenantService, adminUserService);

        IntruoyiMcpReadOnlyTools.TenantSummary summary = tools.getTenantSummary(7L);

        assertEquals(7L, summary.id());
        assertEquals("芋道源码", summary.name());
        assertEquals(0, summary.status());
        assertEquals(expireTime, summary.expireTime());
    }

    @Test
    void getTenantSummaryShouldFailWhenTenantDoesNotExist() {
        when(tenantService.getTenant(404L)).thenReturn(null);
        IntruoyiMcpReadOnlyTools tools = new IntruoyiMcpReadOnlyTools(tenantService, adminUserService);

        assertThrows(IllegalArgumentException.class, () -> tools.getTenantSummary(404L));
    }

}
