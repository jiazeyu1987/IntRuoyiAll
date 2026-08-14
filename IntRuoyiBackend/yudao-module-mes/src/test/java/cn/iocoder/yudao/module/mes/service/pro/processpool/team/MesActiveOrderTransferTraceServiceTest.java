package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private MesActiveOrderTransferTraceService service;

    @BeforeEach
    void setUp() {
        service = new MesActiveOrderTransferTraceServiceImpl(transferTraceMapper, transferMapper,
                transferLineMapper, transferDetailMapper);
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
}
