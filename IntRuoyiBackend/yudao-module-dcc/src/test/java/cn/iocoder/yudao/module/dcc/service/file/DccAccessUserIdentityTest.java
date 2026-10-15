package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DccAccessUserIdentityTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void enabledSameTenantAndExactActorIsRequired() {
        TenantContextHolder.setTenantId(1L);
        var user=new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0);
        assertTrue(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setStatus(1);assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setStatus(null);assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setStatus(0).setTenantId(2L);assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setTenantId(null);assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setTenantId(1L).setId(98L);assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
        user.setId(99L);TenantContextHolder.setIgnore(true);
        assertFalse(DccAccessUserIdentity.isEnabledTenantUser(99L,user));
    }
}
