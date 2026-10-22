package cn.iocoder.yudao.module.mes.service.md.item.kingdee;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesKingdeeBomListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesKingdeeBomListMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesKingdeeBomListServiceImplTest {
    private final ErpKingdeeBomClient client = mock(ErpKingdeeBomClient.class);
    private final MesKingdeeBomListMapper mapper = mock(MesKingdeeBomListMapper.class);
    private final ErpKingdeeConfigService config = mock(ErpKingdeeConfigService.class);
    private final MesKingdeeBomListServiceImpl service = new MesKingdeeBomListServiceImpl();
    private final ErpKingdeeProperties properties = new ErpKingdeeProperties();

    @BeforeEach void setup() {
        properties.setBaseUrl("https://erp.example.com"); properties.setAcctId("acct");
        properties.setUsername("user"); properties.setPassword("password"); properties.setLcid(2052);
        ReflectionTestUtils.setField(service, "bomClient", client);
        ReflectionTestUtils.setField(service, "bomListMapper", mapper);
        ReflectionTestUtils.setField(service, "kingdeeConfigService", config);
        when(config.getEffectiveProperties()).thenReturn(properties);
    }
    @Test void changedUsageReplacesCompleteBomAndKeepsRepeatedMaterialEntriesDistinct() {
        var a = line("101", "2", 1); var b = line("102", "3", 2);
        when(client.fetchBomLines(properties)).thenReturn(List.of(a,b));
        assertEquals(2, service.syncAll());
        var rows = org.mockito.ArgumentCaptor.forClass(MesKingdeeBomListDO.class);
        var ordered = inOrder(mapper);
        ordered.verify(mapper).deleteBySourceFid("B1");
        verify(mapper,times(2)).insert(rows.capture());
        assertEquals(List.of("101","102"), rows.getAllValues().stream().map(MesKingdeeBomListDO::getSourceLineKey).toList());
        assertEquals("配置BOM", rows.getValue().getBomType()); assertEquals("盒", rows.getValue().getChildUnitName());
        assertEquals(2, rows.getValue().getLineNo());
        verify(mapper,never()).deleteBySourceFid("UNRELATED");
    }
    @Test void fullSyncSkipsExistingBomWithoutAddingNewUsageKeys() {
        when(client.fetchBomLines(properties)).thenReturn(List.of(line("101","5",1)));
        when(mapper.countBySourceFid("B1")).thenReturn(2L);
        assertEquals(1,service.syncAllSkipExisting().getSkippedCount());
        verify(mapper,never()).deleteBySourceFid(any()); verify(mapper,never()).insert(any(MesKingdeeBomListDO.class));
    }
    @Test void duplicateEntryIdentityFailsBeforeDeletingSnapshot() {
        when(client.fetchBomLines(properties)).thenReturn(List.of(line("101","2",1),line("101","3",2)));
        assertThrows(IllegalStateException.class,service::syncAll);
        verify(mapper,never()).deleteBySourceFid(any());
    }
    private ErpKingdeeBomLine line(String id,String quantity,int sequence) {
        var line = new ErpKingdeeBomLine(); line.setFid("B1"); line.setEntryId(id);
        line.setBomVersion("BOM1"); line.setParentMaterialNumber("P1"); line.setChildMaterialNumber("C1");
        line.setNumerator(new BigDecimal(quantity)); line.setDenominator(BigDecimal.ONE);
        line.setBomType("配置BOM"); line.setChildUnitName("盒"); line.setLineNo(sequence);
        line.setParentQuantity(BigDecimal.ONE); return line;
    }
}
