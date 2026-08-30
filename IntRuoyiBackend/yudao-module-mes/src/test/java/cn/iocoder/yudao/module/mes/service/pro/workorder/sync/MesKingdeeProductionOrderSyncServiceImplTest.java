package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDiffDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderDiffStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeProductionOrderSyncServiceImplTest {

    @Mock
    private ErpKingdeeProductionOrderClient productionOrderClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderDiffMapper scheduleOrderDiffMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdItemTypeMapper itemTypeMapper;
    @Mock
    private MesMdUnitMeasureMapper unitMeasureMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private MesKingdeeProductionOrderSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        syncService = new MesKingdeeProductionOrderSyncServiceImpl(
                productionOrderClient, kingdeeConfigService, workOrderService, workOrderMapper,
                syncRecordMapper, scheduleOrderMapper, scheduleOrderDiffMapper,
                itemMapper, itemTypeMapper, unitMeasureMapper);
    }

    @Test
    void syncWorkOrders_autoCreatesItemUnitTypeAndWorkOrder() {
        ErpKingdeeProductionOrder order = buildOrder();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(null);
        when(itemTypeMapper.selectByParentIdAndCode(MesMdItemTypeDO.PARENT_ID_ROOT, "KINGDEE_PRODUCT")).thenReturn(null);
        when(unitMeasureMapper.selectByCode("kg")).thenReturn(null);
        when(unitMeasureMapper.selectByName("千克")).thenReturn(null);
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(501L);
        doAnswer(invocation -> {
            MesMdItemTypeDO itemType = invocation.getArgument(0);
            itemType.setId(30L);
            return 1;
        }).when(itemTypeMapper).insert(any(MesMdItemTypeDO.class));
        doAnswer(invocation -> {
            MesMdUnitMeasureDO unit = invocation.getArgument(0);
            unit.setId(40L);
            return 1;
        }).when(unitMeasureMapper).insert(any(MesMdUnitMeasureDO.class));
        doAnswer(invocation -> {
            MesMdItemDO item = invocation.getArgument(0);
            item.setId(20L);
            return 1;
        }).when(itemMapper).insert(any(MesMdItemDO.class));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCreatedCount());
        verify(productionOrderClient).fetchProductionOrdersByBillDateRange(
                org.mockito.ArgumentMatchers.eq(kingdeeProperties),
                org.mockito.ArgumentMatchers.eq(LocalDate.now().minusYears(1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()));
        ArgumentCaptor<MesProWorkOrderSaveReqVO> workOrderCaptor = ArgumentCaptor.forClass(MesProWorkOrderSaveReqVO.class);
        verify(workOrderService).createWorkOrder(workOrderCaptor.capture());
        MesProWorkOrderSaveReqVO reqVO = workOrderCaptor.getValue();
        assertEquals(20L, reqVO.getProductId());
        assertEquals("881MO091049", reqVO.getCode());
        assertNull(reqVO.getOrderSourceCode());
        assertEquals("BATCH-881MO091049", reqVO.getBatchCode());
        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        List<MesProWorkOrderDO> updates = updateCaptor.getAllValues();
        assertEquals(501L, updates.get(0).getId());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), updates.get(0).getPlannedStartTime());
        assertEquals(MesProWorkOrderStatusEnum.CONFIRMED.getStatus(), updates.get(1).getStatus());
        assertEquals(501L, updates.get(1).getId());
        verify(syncRecordMapper).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrdersFullSkipExisting_doesNotUpdateExistingWorkOrder() {
        ErpKingdeeProductionOrder order = buildOrder();
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .productId(20L)
                .build();
        when(productionOrderClient.fetchProductionOrders(kingdeeProperties)).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrdersFullSkipExisting();

        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper, never()).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_updatesErpSnapshotFieldsAndKeepsLocalExtensions() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setWorkshopName("组装车间");
        order.setBomVersion("BOM-2026-01");
        order.setPickMode("直接领料");
        order.setAuxiliaryCode("K20260113");
        order.setBusinessStatus("424");
        order.setDrawingNumber("255ACSXXXX");
        order.setScheduleStatus("未排产");
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .quantityScheduled(new BigDecimal("7"))
                .temporaryFrozen(Boolean.TRUE)
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        syncService.syncWorkOrders();

        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper).updateById(updateCaptor.capture());
        MesProWorkOrderDO update = updateCaptor.getValue();
        assertEquals("组装车间", update.getWorkshopName());
        assertEquals("BOM-2026-01", update.getBomVersion());
        assertEquals("直接领料", update.getPickMode());
        assertEquals("K20260113", update.getAuxiliaryCode());
        assertEquals("424", update.getBusinessStatus());
        assertEquals("255ACSXXXX", update.getDrawingNumber());
        assertEquals("未排产", update.getScheduleStatus());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), update.getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), update.getPlannedEndTime());
        assertNull(update.getQuantityScheduled());
        assertNull(update.getTemporaryFrozen());
    }

    @Test
    void syncWorkOrders_keepsErpSnapshotFieldsNullWhenErpReturnsBlank() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setWorkshopName(null);
        order.setBomVersion(null);
        order.setPickMode(null);
        order.setAuxiliaryCode(null);
        order.setBusinessStatus(null);
        order.setDrawingNumber(null);
        order.setScheduleStatus(null);
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        syncService.syncWorkOrders();

        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper).updateById(updateCaptor.capture());
        MesProWorkOrderDO update = updateCaptor.getValue();
        assertNull(update.getWorkshopName());
        assertNull(update.getBomVersion());
        assertNull(update.getPickMode());
        assertNull(update.getAuxiliaryCode());
        assertNull(update.getBusinessStatus());
        assertNull(update.getDrawingNumber());
        assertNull(update.getScheduleStatus());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), update.getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), update.getPlannedEndTime());
    }

    @Test
    void syncWorkOrders_initialContextUsesBusinessDateWindowUpToClickDay() {
        LocalDateTime windowStart = LocalDateTime.of(2025, 6, 24, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 24, 17, 30);
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of());

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders(ErpKingdeeSyncContext.builder()
                .initialSync(true)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build());

        assertEquals(0, result.getCreatedCount());
        verify(productionOrderClient).fetchProductionOrdersByBillDateRange(
                org.mockito.ArgumentMatchers.eq(kingdeeProperties),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2025, 6, 24)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 6, 24)));
        verify(productionOrderClient, never()).fetchProductionOrdersModifiedBetween(any(), any(), any());
    }

    @Test
    void syncWorkOrders_incrementalContextUsesModifyWindowAndCreatesFutureBusinessDateOrder() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 23, 18, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 24, 9, 0);
        ErpKingdeeProductionOrder order = buildOrder();
        order.setFid("310130");
        order.setBillNo("TESTERP-FUTURE-001");
        order.setMaterialNumber("MAT-FUTURE");
        order.setPlannedStartDate(LocalDateTime.of(2026, 7, 1, 0, 0));
        order.setPlannedEndDate(LocalDateTime.of(2026, 7, 1, 0, 0));
        order.setSourceModifyTime(LocalDateTime.of(2026, 6, 24, 8, 30));
        when(productionOrderClient.fetchProductionOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310130", "MAT-FUTURE")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-FUTURE")).thenReturn(new MesMdItemDO().setId(20L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(601L);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders(ErpKingdeeSyncContext.builder()
                .initialSync(false)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build());

        assertEquals(1, result.getCreatedCount());
        verify(productionOrderClient).fetchProductionOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        verify(productionOrderClient, never()).fetchProductionOrdersByBillDateRange(any(), any(), any());
        ArgumentCaptor<MesProWorkOrderSaveReqVO> workOrderCaptor = ArgumentCaptor.forClass(MesProWorkOrderSaveReqVO.class);
        verify(workOrderService).createWorkOrder(workOrderCaptor.capture());
        assertEquals("TESTERP-FUTURE-001", workOrderCaptor.getValue().getCode());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), workOrderCaptor.getValue().getRequestDate());
    }

    @Test
    void syncWorkOrders_createsKingdeeBillNoUsingBillDateWhenPlanDatesAreBlank() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setFid("310121");
        order.setBillNo("123123123");
        order.setMaterialNumber("A001.01.053.001");
        order.setMaterialName("ABS");
        order.setQuantity(new BigDecimal("12"));
        order.setPlannedStartDate(null);
        order.setPlannedEndDate(null);
        order.setBillDate(LocalDateTime.of(2026, 3, 19, 0, 0));
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310121", "A001.01.053.001")).thenReturn(null);
        when(itemMapper.selectByCode("A001.01.053.001")).thenReturn(new MesMdItemDO().setId(20L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(502L);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCreatedCount());
        ArgumentCaptor<MesProWorkOrderSaveReqVO> workOrderCaptor = ArgumentCaptor.forClass(MesProWorkOrderSaveReqVO.class);
        verify(workOrderService).createWorkOrder(workOrderCaptor.capture());
        MesProWorkOrderSaveReqVO reqVO = workOrderCaptor.getValue();
        assertEquals("123123123", reqVO.getCode());
        assertEquals(LocalDateTime.of(2026, 3, 19, 0, 0), reqVO.getRequestDate());
    }

    @Test
    void syncWorkOrders_skipsExistingSourceRecord() {
        ErpKingdeeProductionOrder order = buildOrder();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(77L)
                .setWorkOrderId(501L);
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(9L)
                .quantity(new BigDecimal("10"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(syncRecord);
        when(workOrderMapper.selectById(501L)).thenReturn(existing);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(workOrderMapper).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper).updateById(any(MesKingdeeProductionOrderSyncRecordDO.class));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
    }

    @Test
    void syncWorkOrders_updatesExistingWorkOrderCodeAndContinuesWithLaterOrders() {
        ErpKingdeeProductionOrder existingOrder = buildOrder();
        ErpKingdeeProductionOrder newOrder = buildOrder();
        newOrder.setFid("310120");
        newOrder.setBillNo("881MO091050");
        newOrder.setMaterialNumber("MAT-002");
        newOrder.setMaterialName("Material B");

        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any()))
                .thenReturn(List.of(existingOrder, newOrder));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(syncRecordMapper.selectBySourceKey("310120", "MAT-002")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049"))
                .thenReturn(new MesProWorkOrderDO().setId(900L).setCode("881MO091049").setName("Old A")
                        .setProductId(19L).setQuantity(BigDecimal.ONE)
                        .setRequestDate(LocalDateTime.of(2026, 3, 20, 0, 0)).setRemark("old"));
        when(workOrderService.getWorkOrder("881MO091050")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(19L));
        when(itemMapper.selectByCode("MAT-002")).thenReturn(new MesMdItemDO().setId(20L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(501L);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(workOrderService).getWorkOrder("881MO091049");
        verify(workOrderService).getWorkOrder("881MO091050");
        verify(workOrderService).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(itemMapper).selectByCode("MAT-001");
        verify(itemMapper).selectByCode("MAT-002");
        verify(workOrderMapper, org.mockito.Mockito.times(3)).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper, org.mockito.Mockito.times(2))
                .insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_defaultsNewWorkOrdersToConfirmed() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setStatus("6");
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(501L);

        syncService.syncWorkOrders();

        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        List<MesProWorkOrderDO> updates = updateCaptor.getAllValues();
        MesProWorkOrderDO statusUpdate = updates.get(1);
        assertEquals(MesProWorkOrderStatusEnum.CONFIRMED.getStatus(), statusUpdate.getStatus());
        assertEquals(501L, statusUpdate.getId());
        assertEquals(null, statusUpdate.getFinishDate());
        assertEquals(null, statusUpdate.getCancelDate());
    }

    @Test
    void syncWorkOrders_createsAndFinishesFinishedOrdersVisibleInErpList() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setStatus("5");
        order.setBusinessStatus("结案");
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(501L);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getFinishedCount());
        verify(workOrderService).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(workOrderService).finishWorkOrder(501L);
        verify(syncRecordMapper).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_skipsLaterRowsWithSameBillNoInSingleBatch() {
        ErpKingdeeProductionOrder firstOrder = buildOrder();
        firstOrder.setFid("310110");
        firstOrder.setBillNo("908MO000020");
        firstOrder.setMaterialNumber("WQ12F1017501");

        ErpKingdeeProductionOrder secondOrder = buildOrder();
        secondOrder.setFid("310110");
        secondOrder.setBillNo("908MO000020");
        secondOrder.setMaterialNumber("WQ12F1004301");

        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any()))
                .thenReturn(List.of(firstOrder, secondOrder));
        when(syncRecordMapper.selectBySourceKey("310110", "WQ12F1017501")).thenReturn(null);
        when(workOrderService.getWorkOrder("908MO000020")).thenReturn(null);
        when(itemMapper.selectByCode("WQ12F1017501")).thenReturn(new MesMdItemDO().setId(21L));
        when(workOrderService.createWorkOrder(any(MesProWorkOrderSaveReqVO.class))).thenReturn(601L);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());
        verify(workOrderService).getWorkOrder("908MO000020");
        verify(workOrderService).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(syncRecordMapper).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
        verify(itemMapper, never()).selectByCode("WQ12F1004301");
    }

    @Test
    void syncWorkOrders_createsScheduleOrderDiffWhenConfirmedWorkOrderAlreadyScheduled() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setQuantity(new BigDecimal("15"));
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Material A")
                .productId(20L)
                .quantity(new BigDecimal("12"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("Kingdee K3Cloud production order: OLD")
                .build();
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(601L)
                .workOrderId(501L)
                .diffStatus(MesProScheduleOrderDiffStatusEnum.NONE.getStatus())
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(501L)).thenReturn(scheduleOrder);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getUpdatedCount());
        ArgumentCaptor<MesProScheduleOrderDiffDO> diffCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderDiffDO.class);
        verify(scheduleOrderDiffMapper).insert(diffCaptor.capture());
        MesProScheduleOrderDiffDO diff = diffCaptor.getValue();
        assertEquals(601L, diff.getScheduleOrderId());
        assertEquals(501L, diff.getWorkOrderId());
        assertEquals("ERP_WORK_ORDER_SYNC", diff.getDiffType());
        assertEquals(MesProScheduleOrderDiffStatusEnum.PENDING.getStatus(), diff.getStatus());
        verify(scheduleOrderMapper).updateById(new MesProScheduleOrderDO()
                .setId(601L)
                .setDiffStatus(MesProScheduleOrderDiffStatusEnum.PENDING.getStatus()));
    }

    @Test
    void syncWorkOrders_preservesLocalSchedulingAttributesWhenUpdatingExistingWorkOrder() {
        ErpKingdeeProductionOrder order = buildOrder();
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .quantityScheduled(new BigDecimal("7"))
                .temporaryFrozen(Boolean.TRUE)
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getUpdatedCount());
        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper).updateById(updateCaptor.capture());
        MesProWorkOrderDO update = updateCaptor.getValue();
        assertNull(update.getQuantityScheduled());
        assertNull(update.getTemporaryFrozen());
    }

    @Test
    void syncWorkOrders_doesNotCreateScheduleDiffForLocalSchedulingAttributes() {
        ErpKingdeeProductionOrder order = buildOrder();
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Material A")
                .type(MesProWorkOrderTypeEnum.SELF.getType())
                .orderSourceType(MesProWorkOrderSourceTypeEnum.STORE.getType())
                .productId(20L)
                .quantity(new BigDecimal("12"))
                .quantityScheduled(new BigDecimal("7"))
                .temporaryFrozen(Boolean.TRUE)
                .batchCode("BATCH-881MO091049")
                .plannedStartTime(LocalDateTime.of(2026, 3, 25, 0, 0))
                .plannedEndTime(LocalDateTime.of(2026, 3, 25, 0, 0))
                .requestDate(LocalDateTime.of(2026, 3, 25, 0, 0))
                .remark("Kingdee K3Cloud production order: 881MO091049")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getUpdatedCount());
        assertTrue(result.getCreatedWorkOrderIds().isEmpty());
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(scheduleOrderDiffMapper, never()).insert(any(MesProScheduleOrderDiffDO.class));
    }

    @Test
    void syncWorkOrders_preservesExistingBatchCodeWhenErpBatchNumberBlank() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setBatchNumber(" ");
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Material A")
                .type(MesProWorkOrderTypeEnum.SELF.getType())
                .orderSourceType(MesProWorkOrderSourceTypeEnum.STORE.getType())
                .productId(20L)
                .quantity(new BigDecimal("12"))
                .batchCode("MES-PRODUCE-BATCH-001")
                .plannedStartTime(LocalDateTime.of(2026, 3, 25, 0, 0))
                .plannedEndTime(LocalDateTime.of(2026, 3, 25, 0, 0))
                .requestDate(LocalDateTime.of(2026, 3, 25, 0, 0))
                .remark("Kingdee K3Cloud production order: 881MO091049")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getUpdatedCount());
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(scheduleOrderDiffMapper, never()).insert(any(MesProScheduleOrderDiffDO.class));
    }

    @Test
    void syncWorkOrders_insertsSyncRecordWhenSameSourceExistsInAnotherTenant() {
        ErpKingdeeProductionOrder order = buildOrder();
        MesProWorkOrderDO existing = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(existing);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getUpdatedCount());
        ArgumentCaptor<MesKingdeeProductionOrderSyncRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesKingdeeProductionOrderSyncRecordDO.class);
        verify(syncRecordMapper).insert(recordCaptor.capture());
        assertEquals("310119", recordCaptor.getValue().getSourceFid());
        assertEquals("MAT-001", recordCaptor.getValue().getSourceMaterialNumber());
        assertEquals(501L, recordCaptor.getValue().getWorkOrderId());
        verify(syncRecordMapper, never()).updateById(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_usesSourceRecordWorkOrderWhenBillNoChanges() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setBillNo("881MO091049-REV");
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(77L)
                .setSourceFid("310119")
                .setSourceBillNo("881MO091049")
                .setSourceMaterialNumber("MAT-001")
                .setWorkOrderId(501L);
        MesProWorkOrderDO sourceLinkedWorkOrder = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(syncRecord);
        when(workOrderMapper.selectById(501L)).thenReturn(sourceLinkedWorkOrder);
        when(workOrderService.getWorkOrder("881MO091049-REV")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(new MesMdItemDO().setId(20L));

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        ArgumentCaptor<MesProWorkOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProWorkOrderDO.class);
        verify(workOrderMapper).updateById(updateCaptor.capture());
        assertEquals(501L, updateCaptor.getValue().getId());
        assertEquals("881MO091049-REV", updateCaptor.getValue().getCode());
        ArgumentCaptor<MesKingdeeProductionOrderSyncRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesKingdeeProductionOrderSyncRecordDO.class);
        verify(syncRecordMapper).updateById(recordCaptor.capture());
        assertEquals(77L, recordCaptor.getValue().getId());
        assertEquals("881MO091049-REV", recordCaptor.getValue().getSourceBillNo());
        assertEquals(501L, recordCaptor.getValue().getWorkOrderId());
    }

    @Test
    void syncWorkOrders_failsFastWhenSourceRecordBillNoConflictsWithAnotherWorkOrder() {
        ErpKingdeeProductionOrder order = buildOrder();
        order.setBillNo("881MO091049-REV");
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(77L)
                .setSourceFid("310119")
                .setSourceBillNo("881MO091049")
                .setSourceMaterialNumber("MAT-001")
                .setWorkOrderId(501L);
        MesProWorkOrderDO sourceLinkedWorkOrder = MesProWorkOrderDO.builder()
                .id(501L)
                .code("881MO091049")
                .name("Old material")
                .productId(20L)
                .quantity(new BigDecimal("10"))
                .requestDate(LocalDateTime.of(2026, 3, 24, 0, 0))
                .remark("old")
                .build();
        MesProWorkOrderDO conflictingWorkOrder = MesProWorkOrderDO.builder()
                .id(999L)
                .code("881MO091049-REV")
                .name("Other material")
                .productId(21L)
                .quantity(new BigDecimal("5"))
                .requestDate(LocalDateTime.of(2026, 3, 20, 0, 0))
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(syncRecord);
        when(workOrderMapper.selectById(501L)).thenReturn(sourceLinkedWorkOrder);
        when(workOrderService.getWorkOrder("881MO091049-REV")).thenReturn(conflictingWorkOrder);

        ServiceException exception = assertThrows(ServiceException.class, () -> syncService.syncWorkOrders());

        assertTrue(exception.getMessage().contains("production order source key conflicts"));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper, never()).updateById(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_failsFastWithBusinessMessageWhenSourceRecordKeyIsDuplicated() {
        ErpKingdeeProductionOrder order = buildOrder();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001"))
                .thenThrow(new TooManyResultsException(
                        "Expected one result (or null) to be returned by selectOne(), but found: 2"));

        ServiceException exception = assertThrows(ServiceException.class, () -> syncService.syncWorkOrders());

        assertTrue(exception.getMessage().contains("生产订单同步记录重复"));
        assertTrue(exception.getMessage().contains("sourceFid=310119"));
        assertTrue(exception.getMessage().contains("sourceMaterialNumber=MAT-001"));
        assertTrue(!exception.getMessage().contains("Expected one result"));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper, never()).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_failsFastWithBusinessMessageWhenWorkOrderCodeIsDuplicated() {
        ErpKingdeeProductionOrder order = buildOrder();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049"))
                .thenThrow(new TooManyResultsException(
                        "Expected one result (or null) to be returned by selectOne(), but found: 2"));

        ServiceException exception = assertThrows(ServiceException.class, () -> syncService.syncWorkOrders());

        assertTrue(exception.getMessage().contains("生产工单编码重复"));
        assertTrue(exception.getMessage().contains("workOrderCode=881MO091049"));
        assertTrue(exception.getMessage().contains("sourceKey=310119:MAT-001"));
        assertTrue(!exception.getMessage().contains("Expected one result"));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(workOrderMapper, never()).updateById(any(MesProWorkOrderDO.class));
        verify(syncRecordMapper, never()).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_failsFastWithBusinessMessageWhenUnitCodeIsDuplicated() {
        ErpKingdeeProductionOrder order = buildOrder();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of(order));
        when(syncRecordMapper.selectBySourceKey("310119", "MAT-001")).thenReturn(null);
        when(workOrderService.getWorkOrder("881MO091049")).thenReturn(null);
        when(itemMapper.selectByCode("MAT-001")).thenReturn(null);
        when(unitMeasureMapper.selectByCode("kg"))
                .thenThrow(new TooManyResultsException(
                        "Expected one result (or null) to be returned by selectOne(), but found: 2"));

        ServiceException exception = assertThrows(ServiceException.class, () -> syncService.syncWorkOrders());

        assertTrue(exception.getMessage().contains("计量单位编码重复"));
        assertTrue(exception.getMessage().contains("unitCode=kg"));
        assertTrue(exception.getMessage().contains("workOrderCode=881MO091049"));
        assertTrue(!exception.getMessage().contains("Expected one result"));
        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
        verify(workOrderService, never()).createWorkOrder(any(MesProWorkOrderSaveReqVO.class));
        verify(syncRecordMapper, never()).insert(any(MesKingdeeProductionOrderSyncRecordDO.class));
    }

    @Test
    void syncWorkOrders_cancelsSyncedWorkOrderWhenKingdeeBillMissing() {
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(77L)
                .setSourceBillNo("WO-VOID")
                .setSourceMaterialNumber("MAT-001")
                .setWorkOrderId(501L);
        MesProWorkOrderDO existingWorkOrder = MesProWorkOrderDO.builder()
                .id(501L)
                .code("WO-VOID")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .build();
        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of());
        when(syncRecordMapper.selectList()).thenReturn(List.of(syncRecord));
        when(productionOrderClient.fetchProductionOrdersByBillNos(
                org.mockito.ArgumentMatchers.eq(kingdeeProperties),
                org.mockito.ArgumentMatchers.eq(List.of("WO-VOID")))).thenReturn(List.of());
        when(workOrderMapper.selectById(501L)).thenReturn(existingWorkOrder);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCanceledCount());
        verify(workOrderService).cancelWorkOrder(501L);
    }

    @Test
    void syncWorkOrders_cancelsSyncedWorkOrderWhenKingdeeDocumentVoided() {
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(78L)
                .setSourceBillNo("WO-VOID")
                .setSourceMaterialNumber("MAT-001")
                .setWorkOrderId(502L);
        MesProWorkOrderDO existingWorkOrder = MesProWorkOrderDO.builder()
                .id(502L)
                .code("WO-VOID")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .build();
        ErpKingdeeProductionOrder voidedOrder = buildOrder();
        voidedOrder.setBillNo("WO-VOID");
        voidedOrder.setDocumentStatus("Z");
        voidedOrder.setStatus("1");

        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of());
        when(syncRecordMapper.selectList()).thenReturn(List.of(syncRecord));
        when(productionOrderClient.fetchProductionOrdersByBillNos(
                org.mockito.ArgumentMatchers.eq(kingdeeProperties),
                org.mockito.ArgumentMatchers.eq(List.of("WO-VOID")))).thenReturn(List.of(voidedOrder));
        when(workOrderMapper.selectById(502L)).thenReturn(existingWorkOrder);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getCanceledCount());
        verify(workOrderService).cancelWorkOrder(502L);
    }

    @Test
    void syncWorkOrders_finishesSyncedWorkOrderWhenKingdeeStatusFinished() {
        MesKingdeeProductionOrderSyncRecordDO syncRecord = new MesKingdeeProductionOrderSyncRecordDO()
                .setId(79L)
                .setSourceBillNo("WO-DONE")
                .setSourceMaterialNumber("MAT-001")
                .setWorkOrderId(503L);
        MesProWorkOrderDO existingWorkOrder = MesProWorkOrderDO.builder()
                .id(503L)
                .code("WO-DONE")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .build();
        ErpKingdeeProductionOrder finishedOrder = buildOrder();
        finishedOrder.setBillNo("WO-DONE");
        finishedOrder.setDocumentStatus("C");
        finishedOrder.setStatus("5");

        when(productionOrderClient.fetchProductionOrdersByBillDateRange(any(), any(), any())).thenReturn(List.of());
        when(syncRecordMapper.selectList()).thenReturn(List.of(syncRecord));
        when(productionOrderClient.fetchProductionOrdersByBillNos(
                org.mockito.ArgumentMatchers.eq(kingdeeProperties),
                org.mockito.ArgumentMatchers.eq(List.of("WO-DONE")))).thenReturn(List.of(finishedOrder));
        when(workOrderMapper.selectById(503L)).thenReturn(existingWorkOrder);

        MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders();

        assertEquals(1, result.getFinishedCount());
        verify(workOrderService).finishWorkOrder(503L);
    }

    private static ErpKingdeeProductionOrder buildOrder() {
        ErpKingdeeProductionOrder order = new ErpKingdeeProductionOrder();
        order.setFid("310119");
        order.setBillNo("881MO091049");
        order.setMaterialNumber("MAT-001");
        order.setMaterialName("Material A");
        order.setMaterialSpecification("Spec A");
        order.setUnitCode("kg");
        order.setUnitName("千克");
        order.setQuantity(new BigDecimal("12"));
        order.setPlannedStartDate(LocalDateTime.of(2026, 3, 25, 0, 0));
        order.setPlannedEndDate(LocalDateTime.of(2026, 3, 25, 0, 0));
        order.setDocumentStatus("C");
        order.setStatus("2");
        order.setBatchNumber("BATCH-881MO091049");
        return order;
    }

}
