package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpFenbeitongKingdeeConfigProviderTest {
    @Mock private ErpKingdeeConfigService service;
    @InjectMocks private ErpFenbeitongKingdeeConfigProvider provider;

    private ErpKingdeeConfigRespVO passwordConfig() {
        ErpKingdeeConfigRespVO config = new ErpKingdeeConfigRespVO();
        config.setBaseUrl("http://formal/K3Cloud");
        config.setAcctId("formal-account");
        config.setUsername("formal-user");
        config.setPassword("fixture-password");
        config.setLcid(2052);
        return config;
    }

    @Test void passwordProtocolDoesNotRequireOrRequestSimPasCredentials() {
        when(service.getConfig()).thenReturn(passwordConfig());
        var snapshot = provider.getCurrentPasswordLoginConfig();
        assertEquals("formal-account", snapshot.getAcctId());
        assertEquals("formal-user", snapshot.getUsername());
        assertEquals("fixture-password", snapshot.getPassword());
        assertEquals(2052, snapshot.getLcid());
        verify(service).getConfig();
        verify(service, never()).getEffectiveProperties();
    }

    @Test void missingPasswordIsStillRejected() {
        var config = passwordConfig();config.setPassword(" ");
        when(service.getConfig()).thenReturn(config);
        assertTrue(assertThrows(ServiceException.class, provider::getCurrentPasswordLoginConfig)
                .getMessage().contains("password"));
    }

    @Test void failedSelectedAccountResolutionIsNotReplacedWithAnotherAccount() {
        when(service.getConfig()).thenThrow(new IllegalStateException("selected account missing"));
        assertThrows(IllegalStateException.class, provider::getCurrentPasswordLoginConfig);
        verify(service).getConfig();
        verifyNoMoreInteractions(service);
    }
}
