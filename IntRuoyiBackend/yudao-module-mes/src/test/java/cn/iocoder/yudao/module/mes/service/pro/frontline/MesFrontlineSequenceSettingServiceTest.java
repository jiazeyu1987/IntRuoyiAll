package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesFrontlineSequenceSettingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFrontlineSequenceSettingServiceTest {
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void resolvesOnlyCurrentTenantAndRejectsMissingSetting() {
        var mapper = mock(MesFrontlineSequenceSettingMapper.class);
        var service = new MesFrontlineSequenceSettingService(mapper);
        when(mapper.getEnabled(11L)).thenReturn(true);
        when(mapper.getEnabled(12L)).thenReturn(false);
        when(mapper.getEnabled(13L)).thenReturn(null);
        TenantContextHolder.setTenantId(11L);
        assertTrue(service.isEnabled());
        TenantContextHolder.setTenantId(12L);
        assertFalse(service.isEnabled());
        TenantContextHolder.setTenantId(13L);
        assertThrows(IllegalStateException.class, service::isEnabled);
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, service::isEnabled);
    }
}
