package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.stock.sync.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;

class ErpKingdeeStockMoveListServiceImplTest {
    @Test void refreshRemovesDeletedEntriesBeforeInsertingCurrentSnapshot() {
        var client = mock(ErpKingdeeStockMoveClient.class);
        var config = mock(ErpKingdeeConfigService.class);
        var headers = mock(ErpKingdeeStockMoveListMapper.class);
        var items = mock(ErpKingdeeStockMoveListItemMapper.class);
        var service = new ErpKingdeeStockMoveListServiceImpl();
        ReflectionTestUtils.setField(service,"stockMoveClient",client);
        ReflectionTestUtils.setField(service,"kingdeeConfigService",config);
        ReflectionTestUtils.setField(service,"stockMoveListMapper",headers);
        ReflectionTestUtils.setField(service,"stockMoveListItemMapper",items);
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        properties.setAcctId("acct");properties.setUsername("user");properties.setPassword("password");properties.setLcid(2052);
        var source = new ErpKingdeeStockMove();source.setFid("M1");source.setBillNo("MOVE1");
        var entry = new ErpKingdeeStockMove.Line();entry.setEntryId("E2");source.setLines(List.of(entry));
        when(config.getEffectiveProperties()).thenReturn(properties);
        when(client.fetchStockMoves(properties)).thenReturn(List.of(source));
        when(headers.selectBySource(ErpKingdeeStockMove.FORM_ID,"M1")).thenReturn(ErpKingdeeStockMoveListDO.builder().id(1L).build());
        service.syncAll();
        var ordered = inOrder(items); ordered.verify(items).deleteByStockMoveId(1L);
        ordered.verify(items).insert(any(ErpKingdeeStockMoveListItemDO.class));
        verify(items,never()).updateById(any(ErpKingdeeStockMoveListItemDO.class));
    }
}
