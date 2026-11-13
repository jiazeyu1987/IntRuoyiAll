package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(TenantConfigService.class)
class TenantConfigServiceTest extends BaseDbUnitTest {
    @Resource
    private TenantConfigService service;

    @Test
    void switchingOneTenantsConnectionLeavesTheOtherTenantsSelectionUnchanged() {
        TenantContextHolder.setTenantId(11L);
        Long firstId = service.createConfig(request(null, "PRODUCTION"));
        TenantContextHolder.setTenantId(22L);
        Long secondId = service.createConfig(request(null, "PRODUCTION"));
        assertNotEquals(firstId, secondId);
        service.updateConfig(request(secondId, "TEST"));
        assertEquals("TEST", service.getConfigByKey(key()).getValue());
        TenantContextHolder.setTenantId(11L);
        assertEquals("PRODUCTION", service.getConfigByKey(key()).getValue());
    }

    @Test
    void foreignIdCannotBeUpdatedEvenWhenTenantInterceptionIsIgnored() {
        TenantContextHolder.setTenantId(11L);
        Long id = service.createConfig(request(null, "PRODUCTION"));
        TenantContextHolder.setTenantId(22L);
        TenantContextHolder.setIgnore(true);
        assertNull(service.getConfigByKey(key()));
        assertThrows(RuntimeException.class, () -> service.updateConfig(request(id, "TEST")));
        TenantContextHolder.setTenantId(11L);
        assertEquals("PRODUCTION", service.getConfigByKey(key()).getValue());
    }

    @Test
    void missingTenantFailsBeforeAnyConfigurationIsReadOrWritten() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> service.getConfigByKey(key()));
        assertThrows(RuntimeException.class, () -> service.createConfig(request(null, "TEST")));
        assertThrows(RuntimeException.class, () -> service.updateConfig(request(1L, "TEST")));
    }

    @Test
    void duplicateKeysAreRejectedWithinTheSameTenant() {
        service.createConfig(request(null, "PRODUCTION"));
        assertThrows(RuntimeException.class, () -> service.createConfig(request(null, "TEST")));
        ConfigDO row = service.getConfigByKey(key());
        assertEquals("PRODUCTION", row.getValue());
    }

    private String key() { return "yudao.erp.kingdee.connection.active"; }

    private ConfigSaveReqVO request(Long id, String value) {
        ConfigSaveReqVO request = new ConfigSaveReqVO();
        request.setId(id);
        request.setKey(key());
        request.setValue(value);
        request.setCategory("erp");
        request.setName("ERP connection");
        request.setVisible(false);
        return request;
    }
}
