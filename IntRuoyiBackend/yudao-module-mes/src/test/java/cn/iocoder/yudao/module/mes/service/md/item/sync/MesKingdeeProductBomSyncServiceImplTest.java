package cn.iocoder.yudao.module.mes.service.md.item.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomLine;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdProductBomMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderBomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeProductBomSyncServiceImplTest {

    @Mock
    private ErpKingdeeBomClient bomClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdProductBomMapper productBomMapper;
    @Mock
    private MesProWorkOrderBomService workOrderBomService;

    private ErpKingdeeProperties kingdeeProperties;
    private MesKingdeeProductBomSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        kingdeeProperties.getBom().setQueryLimit(200);
        syncService = new MesKingdeeProductBomSyncServiceImpl(
                bomClient, kingdeeConfigService, itemMapper, productBomMapper, workOrderBomService);
    }

    @Test
    void syncErpBom_replacesExistingBomForItem() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "2", "1")
        ));
        when(itemMapper.selectByCode("SUB-001")).thenReturn(new MesMdItemDO().setId(200L).setCode("SUB-001"));
        when(productBomMapper.selectByItemId(200L)).thenReturn(List.of());

        MesKingdeeProductBomSyncResult result = syncService.syncErpBom(10L);

        assertEquals(10L, result.getItemId());
        assertEquals("V1", result.getErpBomVersion());
        assertEquals(1, result.getSyncedBomCount());
        verify(productBomMapper).deleteByItemId(10L);
        ArgumentCaptor<List<MesMdProductBomDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(productBomMapper).insertBatch(rows.capture());
        assertEquals(new BigDecimal("2"), rows.getValue().get(0).getQuantity());
        assertEquals("ERP BOM\u7248\u672c: V1", rows.getValue().get(0).getRemark());
    }

    @Test
    void syncErpBom_blocksWhenMultipleApprovedVersionsExist() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "1", "1"),
                buildBomLine("V2", "SUB-001", "1", "1")
        ));

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(10L));
        verify(productBomMapper, never()).deleteByItemId(any());
    }

    @Test
    void syncErpBom_blocksWhenApprovedBomMissing() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(10L));
        verify(productBomMapper, never()).deleteByItemId(any());
    }

    @Test
    void syncErpBom_blocksWhenLocalChildItemIsMissing() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-MISSING", "1", "1")
        ));
        when(itemMapper.selectByCode("SUB-MISSING")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> syncService.syncErpBom(10L));
        assertTrue(exception.getMessage().contains(
                "ERP BOM \u5b50\u9879\u7269\u6599\u672a\u6620\u5c04\u5230\u672c\u5730 MES \u7269\u6599"));
        verify(productBomMapper, never()).deleteByItemId(any());
    }

    @Test
    void syncErpBom_blocksWhenChildOwnsDownstreamBom() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "1", "1")
        ));
        when(itemMapper.selectByCode("SUB-001")).thenReturn(new MesMdItemDO().setId(200L).setCode("SUB-001"));
        when(productBomMapper.selectByItemId(200L)).thenReturn(List.of(MesMdProductBomDO.builder()
                .id(1L)
                .itemId(200L)
                .bomItemId(201L)
                .quantity(BigDecimal.ONE)
                .build()));

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(10L));
        verify(productBomMapper, never()).deleteByItemId(any());
    }

    @Test
    void syncErpBom_preservesRatioPrecision() {
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(itemMapper.selectById(10L)).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "1.5", "4")
        ));
        when(itemMapper.selectByCode("SUB-001")).thenReturn(new MesMdItemDO().setId(200L).setCode("SUB-001"));
        when(productBomMapper.selectByItemId(200L)).thenReturn(List.of());

        syncService.syncErpBom(10L);

        ArgumentCaptor<List<MesMdProductBomDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(productBomMapper).insertBatch(rows.capture());
        assertEquals(new BigDecimal("0.375"), rows.getValue().get(0).getQuantity());
    }

    @Test
    void syncBomLinesModifiedBetween_replacesChangedParentBom() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(bomClient.fetchBomLinesModifiedBetween(kingdeeProperties, windowStart, windowEnd)).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "2", "1")
        ));
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(10L).setCode("MAT-001"));
        when(itemMapper.selectByCode("SUB-001")).thenReturn(new MesMdItemDO().setId(200L).setCode("SUB-001"));
        when(productBomMapper.selectByItemId(200L)).thenReturn(List.of());
        when(workOrderBomService.regenerateOpenWorkOrderBomByProductIds(anyCollection())).thenReturn(2);

        MesKingdeeProductBomSyncResult result = syncService.syncBomLinesModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getSyncedParentCount());
        assertEquals(1, result.getSyncedBomCount());
        assertEquals(2, result.getRecalculatedWorkOrderCount());
        verify(productBomMapper).deleteByItemId(10L);
        ArgumentCaptor<List<MesMdProductBomDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(productBomMapper).insertBatch(rows.capture());
        assertEquals(10L, rows.getValue().get(0).getItemId());
        assertEquals(200L, rows.getValue().get(0).getBomItemId());
        verify(workOrderBomService).regenerateOpenWorkOrderBomByProductIds(argThat(ids ->
                ids.size() == 1 && ids.contains(10L)));
    }

    @Test
    void syncBomLinesModifiedBetween_blocksWhenParentItemIsMissing() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(bomClient.fetchBomLinesModifiedBetween(kingdeeProperties, windowStart, windowEnd)).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "2", "1")
        ));
        when(itemMapper.selectByCode("MAT-001")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> syncService.syncBomLinesModifiedBetween(windowStart, windowEnd));

        assertTrue(exception.getMessage().contains("ERP BOM \u7236\u9879\u7269\u6599\u672a\u6620\u5c04\u5230\u672c\u5730 MES \u7269\u6599"));
        verify(productBomMapper, never()).deleteByItemId(any());
    }

    private static ErpKingdeeBomLine buildBomLine(String version, String childCode,
                                                  String numerator, String denominator) {
        ErpKingdeeBomLine line = new ErpKingdeeBomLine();
        line.setFid("310119");
        line.setBomVersion(version);
        line.setParentMaterialNumber("MAT-001");
        line.setParentMaterialName("Parent Product");
        line.setParentMaterialSpecification("Parent Spec");
        line.setChildMaterialNumber(childCode);
        line.setChildMaterialName("Child");
        line.setChildMaterialSpecification("Child Spec");
        line.setChildUnitName("PCS");
        line.setNumerator(new BigDecimal(numerator));
        line.setDenominator(new BigDecimal(denominator));
        return line;
    }

}
