package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeInventoryListDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.ErpKingdeeInventoryListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpKingdeeInventoryListServiceImplTest {
    @Test void refreshUpdatesExistingQuantityAndKeepsItsIdentity() {
        var mapper = mock(ErpKingdeeInventoryListMapper.class);
        var client = mock(ErpKingdeeInventoryClient.class);
        var config = mock(ErpKingdeeConfigService.class);
        var service = new ErpKingdeeInventoryListServiceImpl();
        ReflectionTestUtils.setField(service, "inventoryListMapper", mapper);
        ReflectionTestUtils.setField(service, "inventoryClient", client);
        ReflectionTestUtils.setField(service, "kingdeeConfigService", config);
        var properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://erp.example.com"); properties.setAcctId("acct");
        properties.setUsername("user"); properties.setPassword("fixture"); properties.setLcid(2052);
        when(config.getEffectiveProperties()).thenReturn(properties);
        var source = new ErpKingdeeInventoryRow();
        source.setStockOrgNumber("O"); source.setWarehouseNumber("W");
        source.setMaterialNumber("P"); source.setLotNumber("L");
        source.setQuantity(new BigDecimal("7")); source.setMaterialName("corrected");
        when(client.fetchInventoryRows(properties)).thenReturn(List.of(source));
        when(mapper.selectBySourceLine("O|W|P|L")).thenReturn(new ErpKingdeeInventoryListDO().setId(77L));
        var result = service.syncAllWithUpdates();
        assertEquals(1, result.getUpdatedCount()); assertEquals(0, result.getCreatedCount());
        var saved = ArgumentCaptor.forClass(ErpKingdeeInventoryListDO.class);
        verify(mapper).updateById(saved.capture());
        assertEquals(77L, saved.getValue().getId());
        assertEquals(new BigDecimal("7"), saved.getValue().getQuantity());
        assertEquals("corrected", saved.getValue().getMaterialName());
        verify(mapper, never()).insert(any(ErpKingdeeInventoryListDO.class));
    }
}
