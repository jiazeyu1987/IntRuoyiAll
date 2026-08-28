package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MesOrderReleaseCompletenessServiceTest {

    @InjectMocks
    private MesOrderReleaseCompletenessServiceImpl service;

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    @Mock
    private MesProcessPoolWorkOrderAbnormalMapper workOrderAbnormalMapper;
    @Mock
    private MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;
    @Mock
    private MesWmMaterialStockMapper materialStockMapper;

    @BeforeEach
    void setUp() {
        lenient().when(regulationVersionMapper.selectById(9902L))
                .thenReturn(regulationVersion(true, null));
    }

    @Test
    void evaluateInspectionResultSummarizesLargePendingPqcTaskSetWithinReleaseCheckColumnBudget() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .workOrderId(980008L)
                .workOrderCode("RRM-20260801-PP-MO-001")
                .routeId(922119L)
                .routeVersionId(922120L)
                .build();
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(12L)
                .workOrderId(batch.getWorkOrderId())
                .routeId(batch.getRouteId())
                .routeVersionId(batch.getRouteVersionId())
                .activeStatus("ACTIVE")
                .build();
        List<MesPqcInspectionTaskDO> pendingTasks = LongStream.rangeClosed(1, 120)
                .mapToObj(id -> MesPqcInspectionTaskDO.builder()
                        .id(id)
                        .activeOrderId(activeOrder.getId())
                        .taskStatus("PENDING")
                        .build())
                .toList();
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId()))
                .thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(pendingTasks);

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("共 120 个"));
        assertTrue(result.failureReason().contains("示例"));
        assertTrue(result.failureReason().length() <= 500);
    }

    @Test
    void evaluateInspectionResultBlocksWhenConfirmedPqcTasksMissExpectedPatrolPmIdentity() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "FINAL", "FINAL")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("缺少预期 PQC 检验任务身份"));
        assertTrue(result.failureReason().contains("inspectionType=PATROL"));
        assertTrue(result.failureReason().contains("shiftCode=PM"));
    }

    @Test
    void evaluateInspectionResultPassesWhenConfirmedPqcTasksCoverExpectedIdentities() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("身份完整"));
    }

    @Test
    void evaluateInspectionResultBlocksWhenConfirmedPqcTasksDuplicateAnExpectedIdentity() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "FIRST", "FIRST"),
                confirmedPqcTask(3L, "PATROL", "AM"),
                confirmedPqcTask(4L, "PATROL", "PM"),
                confirmedPqcTask(5L, "FINAL", "FINAL")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("重复任务"));
        assertTrue(result.failureReason().contains("inspectionType=FIRST"));
    }

    @Test
    void evaluateInspectionResultPassesWithoutFinalWhenRegulationMarksFinalNotApplicable() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(regulationVersionMapper.selectById(9902L))
                .thenReturn(regulationVersion(false, "该工序后续 OQC 覆盖最终包装确认"));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("身份完整"));
    }

    @Test
    void evaluateInspectionResultPassesWithoutFinalWhenRegulationMissingExplicitApplicability() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(regulationVersionMapper.selectById(9902L))
                .thenReturn(regulationVersion(null, null));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("身份完整"));
    }

    @Test
    void evaluateInspectionResultStillBlocksWithoutFinalWhenRegulationExplicitlyDisablesFinalWithoutReason() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(regulationVersionMapper.selectById(9902L))
                .thenReturn(regulationVersion(false, null));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM")));
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("末检不适用但缺少明确依据"));
    }

    @Test
    void evaluateInventoryConsistencyUsesRouteVersionScopedActiveOrder() {
        MesProEdhrBatchExecutionDO batch = batch();

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("activeOrder"));
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId());
        verify(activeOrderMapper, never()).selectActiveByWorkOrderAndRoute(any(), any());
    }

    @Test
    void evaluateInventoryConsistencyBlocksWhenMandatoryTraceTypesMissing() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(activeOrder.getId()), any()))
                .thenReturn(List.of(trace(1L, MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER)));

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("缺少必备库存追溯来源"));
        assertTrue(result.failureReason().contains(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT));
        assertTrue(result.failureReason().contains(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_BATCH_TRACE));
        verify(materialStockMapper, never()).selectListByIds(any());
    }

    @Test
    void evaluateInventoryConsistencyBlocksWhenTraceQuantityInvalid() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = completeInventoryTraces();
        traces.get(1).setQuantity(BigDecimal.ZERO);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(activeOrder.getId()), any()))
                .thenReturn(traces);

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("无效库存追溯来源"));
        assertTrue(result.failureReason().contains("数量"));
        verify(materialStockMapper, never()).selectListByIds(any());
    }

    @Test
    void evaluateInventoryConsistencyBlocksWhenMovementSourceStatusNotFinished() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = completeInventoryTraces();
        traces.get(0).setSourceStatus("3");
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(activeOrder.getId()), any()))
                .thenReturn(traces);

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("无效库存追溯来源"));
        assertTrue(result.failureReason().contains("来源状态未闭环"));
        verify(materialStockMapper, never()).selectListByIds(any());
    }

    @Test
    void evaluateInventoryConsistencyBlocksWhenTraceSourceTypesDuplicate() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = new ArrayList<>(completeInventoryTraces());
        traces.add(trace(4L, MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(activeOrder.getId()), any()))
                .thenReturn(traces);

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("重复库存追溯来源"));
        verify(materialStockMapper, never()).selectListByIds(any());
    }

    @Test
    void evaluateInventoryConsistencyPassesWhenFormalSourcesAreCompleteAndStockIsHealthy() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = completeInventoryTraces();
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(eq(activeOrder.getId()), any()))
                .thenReturn(traces);
        when(materialStockMapper.selectListByIds(List.of(501L, 502L, 503L)))
                .thenReturn(List.of(stock(501L), stock(502L), stock(503L)));

        MesOrderReleaseCompletenessCheck result = service.evaluateInventoryConsistency(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("追溯来源已接入"));
    }

    private static MesProEdhrBatchExecutionDO batch() {
        return MesProEdhrBatchExecutionDO.builder()
                .workOrderId(980008L)
                .workOrderCode("RRM-20260801-PP-MO-001")
                .routeId(922119L)
                .routeVersionId(922120L)
                .build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder(MesProEdhrBatchExecutionDO batch) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(12L)
                .workOrderId(batch.getWorkOrderId())
                .routeId(batch.getRouteId())
                .routeVersionId(batch.getRouteVersionId())
                .activeStatus("ACTIVE")
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(1001L)
                .activeOrderId(12L)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeVersionId(922120L)
                .routeProcessId(928609L)
                .processId(6001L)
                .build();
    }

    private static MesPqcInspectionTaskDO confirmedPqcTask(Long id, String inspectionType, String shiftCode) {
        return MesPqcInspectionTaskDO.builder()
                .id(id)
                .activeOrderId(12L)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeVersionId(922120L)
                .routeProcessId(928609L)
                .processId(6001L)
                .regulationVersionId(9902L)
                .inspectionType(inspectionType)
                .shiftCode(shiftCode)
                .roundNo(1)
                .taskStatus("CONFIRMED")
                .build();
    }

    private static MesQaInspectionRegulationVersionDO regulationVersion(Boolean finalInspectionApplicable,
                                                                        String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(9902L)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .snapshotJson("{}")
                .build();
    }

    private static List<MesProcessPoolActiveOrderTransferTraceDO> completeInventoryTraces() {
        return List.of(
                trace(1L, MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER),
                trace(2L, MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT),
                trace(3L, MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_BATCH_TRACE));
    }

    private static MesProcessPoolActiveOrderTransferTraceDO trace(Long id, String sourceType) {
        return MesProcessPoolActiveOrderTransferTraceDO.builder()
                .id(id)
                .activeOrderId(12L)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeVersionId(922120L)
                .sourceType(sourceType)
                .materialStockId(500L + id)
                .batchId(700L + id)
                .itemId(800L + id)
                .quantity(new BigDecimal("10.000000"))
                .sourceObjectType(sourceType + "_SOURCE")
                .sourceObjectId(String.valueOf(900L + id))
                .sourceObjectCode(sourceType + "-001")
                .sourceStatus("4")
                .build();
    }

    private static MesWmMaterialStockDO stock(Long id) {
        return MesWmMaterialStockDO.builder()
                .id(id)
                .quantity(new BigDecimal("10.000000"))
                .frozen(false)
                .build();
    }
}
