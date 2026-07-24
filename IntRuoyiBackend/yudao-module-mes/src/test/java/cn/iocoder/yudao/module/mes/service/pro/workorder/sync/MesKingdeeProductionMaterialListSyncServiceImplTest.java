package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialList;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialListClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeProductionMaterialListSyncServiceImplTest {

    @Mock
    private ErpKingdeeProductionMaterialListClient client;
    @Mock
    private ErpKingdeeConfigService configService;
    @Mock
    private MesKingdeeProductionMaterialListMapper materialListMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Mock
    private MesMdItemMapper itemMapper;

    private ErpKingdeeProperties properties;
    private MesKingdeeProductionMaterialListSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("user");
        properties.setPassword("password");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(500);
        when(configService.getEffectiveProperties()).thenReturn(properties);
        syncService = new MesKingdeeProductionMaterialListSyncServiceImpl(
                client, configService, materialListMapper, workOrderMapper, workOrderBomMapper, itemMapper);
    }

    @Test
    void syncModifiedBetween_insertsMappedProductionMaterialListLine() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeProductionMaterialList row = buildRow();
        when(client.fetchProductionMaterialListsModifiedBetween(properties, start, end)).thenReturn(List.of(row));
        when(materialListMapper.selectBySourceLine("PPBOM0030888", "CODXMO20260", 1, "A001.02.014.300"))
                .thenReturn(null);
        when(workOrderMapper.selectListByCodes(argThat((Collection<String> codes) ->
                codes != null && codes.contains("CODXMO20260")))).thenReturn(List.of(
                        new MesProWorkOrderDO().setId(501L).setCode("CODXMO20260").setProductId(20L)
                ));
        when(itemMapper.selectByCode("AW.106.03.08.10")).thenReturn(new MesMdItemDO().setId(20L).setCode("AW.106.03.08.10"));
        when(itemMapper.selectByCode("A001.02.014.300")).thenReturn(new MesMdItemDO().setId(30L));
        when(workOrderBomMapper.selectListByWorkOrderId(501L)).thenReturn(List.of(
                new MesProWorkOrderBomDO().setId(601L).setItemId(30L)));

        MesKingdeeProductionMaterialListSyncResult result = syncService.syncModifiedBetween(start, end);

        assertEquals(1, result.getCreatedCount());
        ArgumentCaptor<MesKingdeeProductionMaterialListDO> captor =
                ArgumentCaptor.forClass(MesKingdeeProductionMaterialListDO.class);
        verify(materialListMapper).insert(captor.capture());
        MesKingdeeProductionMaterialListDO saved = captor.getValue();
        assertEquals("PRD_PPBOM", saved.getSourceFormId());
        assertEquals("PPBOM0030888", saved.getSourceBillNo());
        assertEquals("AW.106.03.08.10", saved.getProductCode());
        assertEquals("CODXMO20260", saved.getProductionOrderNo());
        assertEquals(501L, saved.getWorkOrderId());
        assertEquals(601L, saved.getWorkOrderBomId());
        assertEquals(20L, saved.getProductId());
        assertEquals(30L, saved.getChildMaterialId());
    }

    @Test
    void syncModifiedBetween_updatesExistingLine() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeProductionMaterialList row = buildRow();
        when(client.fetchProductionMaterialListsModifiedBetween(properties, start, end)).thenReturn(List.of(row));
        when(materialListMapper.selectBySourceLine("PPBOM0030888", "CODXMO20260", 1, "A001.02.014.300"))
                .thenReturn(new MesKingdeeProductionMaterialListDO().setId(77L));
        when(workOrderMapper.selectListByCodes(argThat((Collection<String> codes) ->
                codes != null && codes.contains("CODXMO20260")))).thenReturn(List.of(
                        new MesProWorkOrderDO().setId(501L).setCode("CODXMO20260").setProductId(20L)
                ));
        when(itemMapper.selectByCode("AW.106.03.08.10")).thenReturn(new MesMdItemDO().setId(20L).setCode("AW.106.03.08.10"));
        when(itemMapper.selectByCode("A001.02.014.300")).thenReturn(new MesMdItemDO().setId(30L));
        when(workOrderBomMapper.selectListByWorkOrderId(501L)).thenReturn(List.of(
                new MesProWorkOrderBomDO().setId(601L).setItemId(30L)));

        MesKingdeeProductionMaterialListSyncResult result = syncService.syncModifiedBetween(start, end);

        assertEquals(1, result.getUpdatedCount());
        ArgumentCaptor<MesKingdeeProductionMaterialListDO> captor =
                ArgumentCaptor.forClass(MesKingdeeProductionMaterialListDO.class);
        verify(materialListMapper).updateById(captor.capture());
        assertEquals(77L, captor.getValue().getId());
    }

    @Test
    void syncModifiedBetween_shouldInsertRowsForEachMatchedTenantWorkOrder() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeProductionMaterialList row = buildRow();
        MesProWorkOrderDO tenant1WorkOrder = new MesProWorkOrderDO();
        tenant1WorkOrder.setId(501L);
        tenant1WorkOrder.setTenantId(1L);
        tenant1WorkOrder.setCode("CODXMO20260");
        tenant1WorkOrder.setProductId(20L);
        MesProWorkOrderDO tenant122WorkOrder = new MesProWorkOrderDO();
        tenant122WorkOrder.setId(502L);
        tenant122WorkOrder.setTenantId(122L);
        tenant122WorkOrder.setCode("CODXMO20260");
        tenant122WorkOrder.setProductId(120L);
        when(client.fetchProductionMaterialListsModifiedBetween(properties, start, end)).thenReturn(List.of(row));
        when(workOrderMapper.selectListByCodes(argThat((Collection<String> codes) ->
                codes != null && codes.contains("CODXMO20260")))).thenReturn(List.of(
                        tenant1WorkOrder,
                        tenant122WorkOrder
                ));
        when(materialListMapper.selectBySourceLine("PPBOM0030888", "CODXMO20260", 1, "A001.02.014.300"))
                .thenReturn(null);
        when(itemMapper.selectByCode("AW.106.03.08.10"))
                .thenReturn(new MesMdItemDO().setId(20L).setCode("AW.106.03.08.10"));
        when(itemMapper.selectByCode("A001.02.014.300"))
                .thenReturn(new MesMdItemDO().setId(30L));
        when(workOrderBomMapper.selectListByWorkOrderId(501L))
                .thenReturn(List.of(new MesProWorkOrderBomDO().setId(601L).setItemId(30L)));
        when(workOrderBomMapper.selectListByWorkOrderId(502L))
                .thenReturn(List.of(new MesProWorkOrderBomDO().setId(602L).setItemId(30L)));

        List<MesKingdeeProductionMaterialListDO> insertedRows = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            MesKingdeeProductionMaterialListDO target = invocation.getArgument(0);
            target.setId(1000L + insertedRows.size());
            insertedRows.add(target);
            return null;
        }).when(materialListMapper).insert(any(MesKingdeeProductionMaterialListDO.class));

        MesKingdeeProductionMaterialListSyncResult result = syncService.syncModifiedBetween(start, end);

        assertEquals(2, result.getCreatedCount());
        assertEquals(2, insertedRows.size());
        assertEquals(List.of(501L, 502L), insertedRows.stream().map(MesKingdeeProductionMaterialListDO::getWorkOrderId).toList());
        assertEquals(List.of(601L, 602L), insertedRows.stream().map(MesKingdeeProductionMaterialListDO::getWorkOrderBomId).toList());
    }

    @Test
    void syncByProductionOrderNos_shouldFetchTargetOrderAndInsertMappedRow() {
        ErpKingdeeProductionMaterialList row = buildRow();
        when(client.fetchProductionMaterialListsByProductionOrderNos(properties, Set.of("CODXMO20260")))
                .thenReturn(List.of(row));
        when(materialListMapper.selectBySourceLine("PPBOM0030888", "CODXMO20260", 1, "A001.02.014.300"))
                .thenReturn(null);
        MesProWorkOrderDO targetWorkOrder = new MesProWorkOrderDO();
        targetWorkOrder.setId(501L);
        targetWorkOrder.setTenantId(1L);
        targetWorkOrder.setCode("CODXMO20260");
        targetWorkOrder.setProductId(20L);
        when(workOrderMapper.selectListByCodes(argThat((Collection<String> codes) ->
                codes != null && codes.contains("CODXMO20260")))).thenReturn(List.of(
                        targetWorkOrder
                ));
        when(itemMapper.selectByCode("AW.106.03.08.10")).thenReturn(new MesMdItemDO().setId(20L).setCode("AW.106.03.08.10"));
        when(itemMapper.selectByCode("A001.02.014.300")).thenReturn(new MesMdItemDO().setId(30L));
        when(workOrderBomMapper.selectListByWorkOrderId(501L)).thenReturn(List.of(
                new MesProWorkOrderBomDO().setId(601L).setItemId(30L)));

        MesKingdeeProductionMaterialListSyncResult result = syncService.syncByProductionOrderNos(Set.of("CODXMO20260"));

        assertEquals(1, result.getCreatedCount());
        verify(materialListMapper).insert(any(MesKingdeeProductionMaterialListDO.class));
    }

    private static ErpKingdeeProductionMaterialList buildRow() {
        return ErpKingdeeProductionMaterialList.builder()
                .formId("PRD_PPBOM")
                .entryId("1001")
                .billNo("PPBOM0030888")
                .productCode("AW.106.03.08.10")
                .productionOrderNo("CODXMO20260")
                .productionOrderLineNo(1)
                .productionOrderStatus("计划")
                .childMaterialCode("A001.02.014.300")
                .childMaterialName("造影导管软端")
                .childMaterialSpecification("4F")
                .childMaterialType("标准件")
                .numerator(new BigDecimal("3"))
                .denominator(new BigDecimal("1000"))
                .childUnitName("支")
                .requiredQuantity(BigDecimal.ONE)
                .issueMethod("直接领料")
                .demandTime(LocalDateTime.of(2026, 6, 12, 0, 0))
                .sourceModifyTime(LocalDateTime.of(2026, 6, 12, 8, 30))
                .rawPayload("[1001]")
                .build();
    }

}
