package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant.InvoiceVoucherPrintKingdeeConfigProvider.KingdeeConfigSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpInvoiceVoucherPrintKingdeeConfigProviderTest {

    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;

    @InjectMocks
    private ErpInvoiceVoucherPrintKingdeeConfigProvider provider;

    @Test
    void getCurrentConfigSnapshotReturnsCurrentEffectiveKingdeeConfig() {
        ErpKingdeeProperties properties = buildProperties();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);

        KingdeeConfigSnapshot snapshot = provider.getCurrentConfigSnapshot();

        assertEquals("http://kingdee/K3Cloud", snapshot.getBaseUrl());
        assertEquals("acct-001", snapshot.getAcctId());
        assertEquals("kingdee-user", snapshot.getUsername());
        assertEquals("kingdee-password", snapshot.getPassword());
        assertEquals("invoice-print-app", snapshot.getAppId());
        assertEquals("invoice-print-secret", snapshot.getAppSecret());
        assertEquals(2052, snapshot.getLcid());
        verify(kingdeeConfigService).getEffectiveProperties();
    }

    @Test
    void getCurrentConfigSnapshotFailsFastWhenAppIdIsMissing() {
        ErpKingdeeProperties properties = buildProperties();
        properties.setAppId(" ");
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> provider.getCurrentConfigSnapshot());

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("appId"));
    }

    @Test
    void getCurrentConfigSnapshotFailsFastWhenAppSecretIsMissing() {
        ErpKingdeeProperties properties = buildProperties();
        properties.setAppSecret(null);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> provider.getCurrentConfigSnapshot());

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("appSecret"));
    }

    private static ErpKingdeeProperties buildProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(" http://kingdee/K3Cloud ");
        properties.setAcctId(" acct-001 ");
        properties.setUsername(" kingdee-user ");
        properties.setPassword(" kingdee-password ");
        properties.setAppId(" invoice-print-app ");
        properties.setAppSecret(" invoice-print-secret ");
        properties.setLcid(2052);
        return properties;
    }

}
