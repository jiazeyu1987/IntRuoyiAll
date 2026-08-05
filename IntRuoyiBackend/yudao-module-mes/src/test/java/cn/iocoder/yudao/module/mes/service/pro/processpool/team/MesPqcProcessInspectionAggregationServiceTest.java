package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcProcessInspectionAggregationServiceTest {

    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pieceDetailMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;

    private MesPqcProcessInspectionAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MesPqcProcessInspectionAggregationServiceImpl(pqcRecordMapper, eventMapper, pqcTaskMapper,
                pieceDetailMapper, aggregateDetailMapper);
    }

    @Test
    void shouldPersistStructuredAggregateDetailsAndMarkRecordAggregatedByApprovedReview() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(eventMapper.selectById(1001L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectById(8001L)).thenReturn(submittedTask());
        when(pieceDetailMapper.selectListByTaskId(8001L)).thenReturn(pieceDetails());
        when(aggregateDetailMapper.insertBatch(any())).thenReturn(true);
        when(pqcRecordMapper.updateProcessInspectionAggregatedIfPending(eq(100L), eq(1001L), eq(7001L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(1);

        service.aggregateApprovedPqcSubmission(1001L, 7001L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesPqcProcessInspectionAggregateDetailDO>> aggregateCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(aggregateDetailMapper).insertBatch(aggregateCaptor.capture());
        List<MesPqcProcessInspectionAggregateDetailDO> aggregateRows =
                List.copyOf(aggregateCaptor.getValue());
        assertEquals(2, aggregateRows.size());
        MesPqcProcessInspectionAggregateDetailDO pressure = aggregateRows.get(0);
        assertEquals(100L, pressure.getTenantId());
        assertEquals(9001L, pressure.getSourcePqcRecordId());
        assertEquals(1001L, pressure.getEventId());
        assertEquals(7001L, pressure.getReviewId());
        assertEquals(5001L, pressure.getProductionSubmitEventId());
        assertEquals(8001L, pressure.getPqcTaskId());
        assertEquals(6001L, pressure.getRegulationVersionId());
        assertEquals("PATROL", pressure.getInspectionType());
        assertEquals(1, pressure.getRoundNo());
        assertEquals(2L, pressure.getSourcePieceDetailId());
        assertEquals(1, pressure.getSampleNo());
        assertEquals("pressure", pressure.getItemCode());
        assertEquals("压力", pressure.getItemName());
        assertEquals("测压", pressure.getInspectionMethod());
        assertEquals("0.60-0.80MPa", pressure.getStandardText());
        assertEquals(new BigDecimal("0.600000"), pressure.getStandardLowerLimit());
        assertEquals(new BigDecimal("0.800000"), pressure.getStandardUpperLimit());
        assertEquals("MPa", pressure.getStandardUnit());
        assertEquals(3, pressure.getStandardPrecision());
        assertEquals(9101L, pressure.getSelectedEquipmentId());
        assertEquals("EQ-PRESS", pressure.getSelectedEquipmentCode());
        assertEquals("压力表", pressure.getSelectedEquipmentName());
        assertEquals("SN-P-001", pressure.getSelectedEquipmentNumber());
        assertEquals("0.72", pressure.getMeasuredValue());
        assertEquals(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS, pressure.getJudgement());
        ArgumentCaptor<LocalDateTime> aggregatedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pqcRecordMapper).updateProcessInspectionAggregatedIfPending(eq(100L), eq(1001L), eq(7001L),
                aggregatedAtCaptor.capture());
        assertNotNull(aggregatedAtCaptor.getValue());
        assertTrue(aggregateRows.stream().allMatch(row -> aggregatedAtCaptor.getValue().equals(row.getAggregatedAt())));
    }

    @Test
    void shouldFailFastWhenApprovedSubmissionHasNoPqcRecord() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RECORD_REQUIRED.getCode(), ex.getCode());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectAlreadyAggregatedPqcRecord() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(aggregatedRecord());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED.getCode(),
                ex.getCode());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectConcurrentDuplicateAggregationWhenPendingWasConsumed() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(eventMapper.selectById(1001L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectById(8001L)).thenReturn(submittedTask());
        when(pieceDetailMapper.selectListByTaskId(8001L)).thenReturn(pieceDetails());
        when(pqcRecordMapper.updateProcessInspectionAggregatedIfPending(eq(100L), eq(1001L), eq(7001L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED.getCode(),
                ex.getCode());
        verify(aggregateDetailMapper, never()).insertBatch(any());
    }

    @Test
    void shouldRejectCrossTenantPqcEventBeforeAggregation() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(eventMapper.selectById(1001L)).thenReturn(pqcEvent().setTenantId(200L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(pieceDetailMapper, never()).selectListByTaskId(any());
        verify(aggregateDetailMapper, never()).insertBatch(any());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectAggregationWhenFormalPqcPieceDetailsAreMissing() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(eventMapper.selectById(1001L)).thenReturn(pqcEvent());
        when(pqcTaskMapper.selectById(8001L)).thenReturn(submittedTask());
        when(pieceDetailMapper.selectListByTaskId(8001L)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RECORD_REQUIRED.getCode(), ex.getCode());
        verify(aggregateDetailMapper, never()).insertBatch(any());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static MesProProcessPoolPqcRecordDO pendingRecord() {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(9001L)
                .eventId(1001L)
                .productionSubmitEventId(5001L)
                .workOrderId(2001L)
                .routeId(3001L)
                .routeProcessId(4001L)
                .processId(4002L)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_PENDING)
                .build()
                .setTenantId(100L);
    }

    private static MesProProcessPoolPqcRecordDO aggregatedRecord() {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(9001L)
                .eventId(1001L)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .processInspectionReviewId(7000L)
                .processInspectionAggregatedAt(LocalDateTime.of(2026, 8, 3, 18, 0))
                .build()
                .setTenantId(100L);
    }

    private static MesProProcessPoolEventDO pqcEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(2001L)
                .routeId(3001L)
                .routeProcessId(4001L)
                .processId(4002L)
                .feedbackSourceType("MES_PQC_INSPECTION_TASK")
                .feedbackSourceId(8001L)
                .recordbookSourceType("MES_PQC_INSPECTION_TASK")
                .recordbookSourceId(8001L)
                .build()
                .setTenantId(100L);
    }

    private static MesPqcInspectionTaskDO submittedTask() {
        return MesPqcInspectionTaskDO.builder()
                .id(8001L)
                .activeOrderId(8101L)
                .workOrderId(2001L)
                .routeId(3001L)
                .routeVersionId(3101L)
                .routeProcessId(4001L)
                .processId(4002L)
                .regulationVersionId(6001L)
                .inspectionType("PATROL")
                .businessDate(LocalDate.of(2026, 8, 5))
                .shiftCode("DAY")
                .roundNo(1)
                .actualInspectionQuantity(2)
                .taskStatus("SUBMITTED")
                .build()
                .setTenantId(100L);
    }

    private static List<MesPqcInspectionPieceDetailDO> pieceDetails() {
        return List.of(
                MesPqcInspectionPieceDetailDO.builder()
                        .id(2L)
                        .taskId(8001L)
                        .sampleNo(1)
                        .itemCode("pressure")
                        .itemName("压力")
                        .inspectionMethod("测压")
                        .standardText("0.60-0.80MPa")
                        .selectedEquipmentId(9101L)
                        .selectedEquipmentCode("EQ-PRESS")
                        .selectedEquipmentName("压力表")
                        .selectedEquipmentNumber("SN-P-001")
                        .standardLowerLimit(new BigDecimal("0.600000"))
                        .standardUpperLimit(new BigDecimal("0.800000"))
                        .standardUnit("MPa")
                        .standardPrecision(3)
                        .resultType("NUMBER")
                        .itemResult("0.72")
                        .measuredValue("0.72")
                        .judgement(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                        .build()
                        .setTenantId(100L),
                MesPqcInspectionPieceDetailDO.builder()
                        .id(3L)
                        .taskId(8001L)
                        .sampleNo(2)
                        .itemCode("pressure")
                        .itemName("压力")
                        .inspectionMethod("测压")
                        .standardText("0.60-0.80MPa")
                        .selectedEquipmentId(9101L)
                        .selectedEquipmentCode("EQ-PRESS")
                        .selectedEquipmentName("压力表")
                        .selectedEquipmentNumber("SN-P-001")
                        .standardLowerLimit(new BigDecimal("0.600000"))
                        .standardUpperLimit(new BigDecimal("0.800000"))
                        .standardUnit("MPa")
                        .standardPrecision(3)
                        .resultType("NUMBER")
                        .itemResult("0.73")
                        .measuredValue("0.73")
                        .judgement(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                        .build()
                        .setTenantId(100L));
    }
}
