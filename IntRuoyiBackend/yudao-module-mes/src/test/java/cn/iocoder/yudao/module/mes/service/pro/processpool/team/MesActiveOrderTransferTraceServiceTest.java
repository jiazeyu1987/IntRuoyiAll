package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesActiveOrderTransferTraceServiceTest {

    @Mock
    private MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;
    @Mock
    private MesWmTransferMapper transferMapper;
    @Mock
    private MesWmTransferLineMapper transferLineMapper;
    @Mock
    private MesWmTransferDetailMapper transferDetailMapper;
    @Mock
    private MesFormalProductionPickListSourceResolver pickListSourceResolver;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;

    private MesActiveOrderTransferTraceService service;

    @BeforeEach
    void setUp() {
        service = new MesActiveOrderTransferTraceServiceImpl(transferTraceMapper, transferMapper,
                transferLineMapper, transferDetailMapper, pickListSourceResolver, pickListBindingMapper);
    }

    @Test
    void shouldRecordTransferTraceWithFormalActiveOrderIdentity() {
        when(transferTraceMapper.selectByIdempotencyKey("transfer-9001-line-2-batch-3")).thenReturn(null);
        when(transferTraceMapper.insert(any(MesProcessPoolActiveOrderTransferTraceDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderTransferTraceDO.class).setId(7101L);
            return 1;
        });

        MesProcessPoolActiveOrderTransferTraceDO trace = service.recordTransferTrace(trace());

        assertEquals(7101L, trace.getId());
        ArgumentCaptor<MesProcessPoolActiveOrderTransferTraceDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderTransferTraceDO.class);
        verify(transferTraceMapper).insert(captor.capture());
        MesProcessPoolActiveOrderTransferTraceDO inserted = captor.getValue();
        assertEquals(8101L, inserted.getActiveOrderId());
        assertEquals(9001L, inserted.getWorkOrderId());
        assertEquals(922119L, inserted.getRouteId());
        assertEquals(448L, inserted.getRouteVersionId());
        assertEquals(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER, inserted.getSourceType());
        assertEquals("OUT", inserted.getDirection());
        assertEquals(5001L, inserted.getTransferId());
        assertEquals(5002L, inserted.getTransferLineId());
        assertEquals(5003L, inserted.getTransferDetailId());
        assertEquals(6001L, inserted.getMaterialStockId());
        assertEquals(7001L, inserted.getBatchId());
        assertEquals(8001L, inserted.getItemId());
        assertEquals(0, new BigDecimal("15.000000").compareTo(inserted.getQuantity()));
        assertEquals("WM_TRANSFER_DETAIL", inserted.getSourceObjectType());
        assertEquals("5003", inserted.getSourceObjectId());
        assertEquals("TR-9001", inserted.getSourceObjectCode());
        assertEquals("SHIPPED", inserted.getSourceStatus());
        assertEquals(LocalDateTime.of(2026, 8, 3, 10, 15), inserted.getSourceOccurredAt());
        assertEquals("transfer-9001-line-2-batch-3", inserted.getIdempotencyKey());
        assertEquals("{\"transferNo\":\"TR-9001\"}", inserted.getSourceSnapshotJson());
    }

    @Test
    void shouldReturnExistingTransferTraceWhenSameIdempotencyKeyAlreadyRecorded() {
        MesProcessPoolActiveOrderTransferTraceDO existing = trace().setId(7102L);
        when(transferTraceMapper.selectByIdempotencyKey("transfer-9001-line-2-batch-3")).thenReturn(existing);

        MesProcessPoolActiveOrderTransferTraceDO trace = service.recordTransferTrace(trace());

        assertEquals(7102L, trace.getId());
        verify(transferTraceMapper, never()).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
    }

    @Test
    void shouldReturnExistingTransferTraceWhenConcurrentInsertHitsUniqueKey() {
        MesProcessPoolActiveOrderTransferTraceDO existing = trace().setId(7103L);
        when(transferTraceMapper.selectByIdempotencyKey("transfer-9001-line-2-batch-3"))
                .thenReturn(null, existing);
        when(transferTraceMapper.insert(any(MesProcessPoolActiveOrderTransferTraceDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order_transfer_trace"));

        MesProcessPoolActiveOrderTransferTraceDO trace = service.recordTransferTrace(trace());

        assertEquals(7103L, trace.getId());
        verify(transferTraceMapper, times(2)).selectByIdempotencyKey("transfer-9001-line-2-batch-3");
    }

    @Test
    void shouldProjectFormalTransferDetailsForActiveOrderTransferIds() {
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build();
        when(transferMapper.selectById(5001L)).thenReturn(MesWmTransferDO.builder()
                .id(5001L)
                .code("TR-9001")
                .status(1)
                .transferDate(LocalDateTime.of(2026, 8, 3, 10, 15))
                .build());
        when(transferLineMapper.selectListByTransferId(5001L)).thenReturn(List.of(MesWmTransferLineDO.builder()
                .id(5002L)
                .transferId(5001L)
                .materialStockId(6001L)
                .itemId(8001L)
                .batchId(7001L)
                .quantity(new BigDecimal("15.000000"))
                .build()));
        when(transferDetailMapper.selectListByTransferId(5001L)).thenReturn(List.of(MesWmTransferDetailDO.builder()
                .id(5003L)
                .lineId(5002L)
                .transferId(5001L)
                .itemId(8001L)
                .batchId(7001L)
                .quantity(new BigDecimal("15.000000"))
                .build()));
        when(transferTraceMapper.selectByIdempotencyKey("active-order-8101-transfer-5001-line-5002-detail-5003"))
                .thenReturn(null);

        List<MesProcessPoolActiveOrderTransferTraceDO> traces =
                service.recordTransferTracesForActiveOrder(activeOrder, List.of(5001L));

        assertEquals(1, traces.size());
        ArgumentCaptor<MesProcessPoolActiveOrderTransferTraceDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderTransferTraceDO.class);
        verify(transferTraceMapper).insert(captor.capture());
        MesProcessPoolActiveOrderTransferTraceDO inserted = captor.getValue();
        assertEquals(8101L, inserted.getActiveOrderId());
        assertEquals(9001L, inserted.getWorkOrderId());
        assertEquals(922119L, inserted.getRouteId());
        assertEquals(448L, inserted.getRouteVersionId());
        assertEquals(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER, inserted.getSourceType());
        assertEquals(5001L, inserted.getTransferId());
        assertEquals(5002L, inserted.getTransferLineId());
        assertEquals(5003L, inserted.getTransferDetailId());
        assertEquals(6001L, inserted.getMaterialStockId());
        assertEquals(7001L, inserted.getBatchId());
        assertEquals(8001L, inserted.getItemId());
        assertEquals("TR-9001", inserted.getSourceObjectCode());
        assertEquals("1", inserted.getSourceStatus());
        assertEquals("active-order-8101-transfer-5001-line-5002-detail-5003", inserted.getIdempotencyKey());
    }

    @Test
    void formalErpPickListsGenerateCompleteEvidenceWithoutLocalInventoryIds() {
        givenPickList();
        var traces = service.recordFormalPickListTracesForActiveOrder(activeOrder());
        assertEquals(3, traces.size());
        assertEquals(List.of("TRANSFER", "SHIPMENT", "BATCH_TRACE"), traces.stream().map(
                MesProcessPoolActiveOrderTransferTraceDO::getSourceType).toList());
        verify(transferTraceMapper, times(3)).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
        for (var trace : traces) {
            assertEquals("ERP_PRODUCTION_PICK_LIST_ITEM", trace.getSourceObjectType());
            assertEquals("6201", trace.getSourceObjectId());
            assertEquals("PICK-9001", trace.getSourceObjectCode());
            assertEquals("C", trace.getSourceStatus());
            assertEquals(new BigDecimal("15"), trace.getQuantity());
            assertNull(trace.getMaterialStockId());
            assertNull(trace.getBatchId());
            assertNull(trace.getItemId());
            var snapshot = JsonUtils.parseTree(trace.getSourceSnapshotJson());
            assertEquals("LOT-A", snapshot.path("pickListItem").path("lotNumber").asText());
            assertEquals("WH-A", snapshot.path("pickListItem").path("warehouseNumber").asText());
            assertTrue(trace.getIdempotencyKey().contains("-pick-list-6101-item-6201-"));
        }
        var completeness = new MesOrderReleaseCompletenessServiceImpl();
        var activeOrders = mock(MesProcessPoolActiveOrderMapper.class);
        ReflectionTestUtils.setField(completeness, "activeOrderMapper", activeOrders);
        ReflectionTestUtils.setField(completeness, "transferTraceMapper", transferTraceMapper);
        when(activeOrders.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L)).thenReturn(activeOrder());
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(8101L), any())).thenReturn(traces);
        var batch = MesProEdhrBatchExecutionDO.builder().workOrderId(9001L).routeId(922119L).routeVersionId(448L).build();
        assertEquals("PASS", completeness.evaluateInventoryConsistency(batch).checkResult());
        traces.get(0).setQuantity(BigDecimal.ONE);
        assertTrue(completeness.evaluateInventoryConsistency(batch).failureReason().contains("无效库存追溯"));
    }

    @Test
    void missingFrozenBindingBlocksBeforeTraceWrites() {
        var source = source(item());
        when(pickListSourceResolver.resolve(9001L)).thenReturn(resolution(source));
        when(pickListBindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> service.recordFormalPickListTracesForActiveOrder(activeOrder()));
        verify(transferTraceMapper, never()).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
    }

    @Test
    void changedSourceHashIsRecordedWithoutBlocking() {
        var source = source(item());
        var binding = binding(source); binding.setSourceSnapshotHash("changed");
        when(pickListSourceResolver.resolve(9001L)).thenReturn(resolution(source));
        when(pickListBindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(binding));
        var traces = service.recordFormalPickListTracesForActiveOrder(activeOrder());
        assertEquals(3, traces.size());
        assertTrue(traces.get(0).getSourceSnapshotJson().contains("领料单已变更"));
        verify(transferTraceMapper, times(3)).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
    }

    @Test
    void foreignOrderBindingIsRejected() {
        var source = source(item());
        var binding = binding(source); binding.setWorkOrderId(999L);
        when(pickListSourceResolver.resolve(9001L)).thenReturn(resolution(source));
        when(pickListBindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(binding));
        assertThrows(RuntimeException.class, () -> service.recordFormalPickListTracesForActiveOrder(activeOrder()));
        verify(transferTraceMapper, never()).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"lot", "warehouse", "quantity", "zero", "negative"})
    void incompleteSourceIsRecordedWithoutBlocking(String missing) {
        var item = item();
        if (missing.equals("lot")) item.setLotNumber(null);
        if (missing.equals("warehouse")) item.setWarehouseNumber(null);
        if (missing.equals("quantity")) item.setActualQuantity(null);
        if (missing.equals("zero")) item.setActualQuantity(BigDecimal.ZERO);
        if (missing.equals("negative")) item.setActualQuantity(new BigDecimal("-2"));
        var source = source(item);
        when(pickListSourceResolver.resolve(9001L)).thenReturn(resolution(source));
        when(pickListBindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(binding(source)));
        var traces = service.recordFormalPickListTracesForActiveOrder(activeOrder());
        assertEquals(3, traces.size());
        assertEquals(item.getActualQuantity(), traces.get(0).getQuantity());
        var snapshot = JsonUtils.parseTree(traces.get(0).getSourceSnapshotJson());
        assertEquals(1, snapshot.path("sourceWarnings").size());
        var completeness = new MesOrderReleaseCompletenessServiceImpl();
        assertNull(ReflectionTestUtils.invokeMethod(completeness, "invalidInventoryTraceReason", traces.get(0)));
    }

    @Test
    void repeatedProjectionUsesSameIdempotencyKeysWithoutDuplicateWrites() {
        givenPickList();
        var first = service.recordFormalPickListTracesForActiveOrder(activeOrder());
        for (var trace : first) when(transferTraceMapper.selectByIdempotencyKey(trace.getIdempotencyKey())).thenReturn(trace);
        var second = service.recordFormalPickListTracesForActiveOrder(activeOrder());
        assertEquals(first, second);
        verify(transferTraceMapper, times(3)).insert(any(MesProcessPoolActiveOrderTransferTraceDO.class));
    }

    private void givenPickList() {
        var source = source(item());
        when(pickListSourceResolver.resolve(9001L)).thenReturn(resolution(source));
        when(pickListBindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(binding(source)));
    }
    private static MesFormalProductionPickListSourceResolver.Resolution resolution(MesFormalProductionPickListSourceResolver.Source source) {
        return new MesFormalProductionPickListSourceResolver.Resolution(null, "MO-9001", List.of(source), source.hash());
    }
    private static MesFormalProductionPickListSourceResolver.Source source(ErpKingdeeProductionPickListItemDO item) {
        var header = ErpKingdeeProductionPickListDO.builder().id(6101L).sourceFid("F-6101")
                .sourceBillNo("PICK-9001").documentStatus("C").billDate(LocalDateTime.of(2026, 8, 3, 11, 20)).build();
        return new MesFormalProductionPickListSourceResolver.Source(header, List.of(item),
                MesFormalProductionPickListSourceResolver.snapshotHash(header, List.of(item)));
    }
    private static MesProcessPoolActiveOrderPickListBindingDO binding(MesFormalProductionPickListSourceResolver.Source source) {
        return MesProcessPoolActiveOrderPickListBindingDO.builder().id(6501L).activeOrderId(8101L)
                .workOrderId(9001L).pickListId(6101L).sourceFid("F-6101").sourceBillNo("PICK-9001")
                .sourceDocumentStatus("C").bindingStatus("BOUND").sourceSnapshotHash(source.hash()).build();
    }
    private static ErpKingdeeProductionPickListItemDO item() {
        return ErpKingdeeProductionPickListItemDO.builder().id(6201L).productionPickListId(6101L)
                .sourceFid("F-6101").sourceEntryId("ENTRY-6201").sourceLineKey("LINE-6201")
                .productionOrderNo("MO-9001").materialNumber("MAT-A").lotNumber("LOT-A")
                .warehouseNumber("WH-A").sourceModifyTime(LocalDateTime.of(2026, 8, 3, 11, 20, 5, 123000000))
                .actualQuantity(new BigDecimal("15")).build();
    }

    private static MesProcessPoolActiveOrderTransferTraceDO trace() {
        return MesProcessPoolActiveOrderTransferTraceDO.builder()
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .sourceType(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER)
                .direction("OUT")
                .transferId(5001L)
                .transferLineId(5002L)
                .transferDetailId(5003L)
                .materialStockId(6001L)
                .batchId(7001L)
                .itemId(8001L)
                .quantity(new BigDecimal("15.000000"))
                .sourceObjectType("WM_TRANSFER_DETAIL")
                .sourceObjectId("5003")
                .sourceObjectCode("TR-9001")
                .sourceStatus("SHIPPED")
                .sourceOccurredAt(LocalDateTime.of(2026, 8, 3, 10, 15))
                .idempotencyKey("transfer-9001-line-2-batch-3")
                .sourceSnapshotJson("{\"transferNo\":\"TR-9001\"}")
                .build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build();
    }

}
