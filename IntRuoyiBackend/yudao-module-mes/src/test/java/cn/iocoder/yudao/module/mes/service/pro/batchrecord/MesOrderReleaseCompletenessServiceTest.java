package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Mock
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;

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
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL"));
        mockFormalSuccessEvidence(tasks);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("身份完整"));
    }

    @Test
    void evaluateInspectionResultPassesWithMultipleQaItemsAndDedicatedCommonRegulations() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(11L, "FIRST", "FIRST", 9902L, 7101L, "DEDICATED-PRESSURE", "FIRST"),
                confirmedPqcTask(12L, "FIRST", "FIRST", 9903L, 7201L, "COMMON-APPEARANCE", "FIRST"),
                confirmedPqcTask(13L, "PATROL", "AM", 9902L, 7101L, "DEDICATED-PRESSURE", "PATROL_AM"),
                confirmedPqcTask(14L, "PATROL", "AM", 9903L, 7201L, "COMMON-APPEARANCE", "PATROL_AM"),
                confirmedPqcTask(15L, "PATROL", "PM", 9902L, 7101L, "DEDICATED-PRESSURE", "PATROL_PM"),
                confirmedPqcTask(16L, "PATROL", "PM", 9903L, 7201L, "COMMON-APPEARANCE", "PATROL_PM"),
                confirmedPqcTask(17L, "FINAL", "FINAL", 9902L, 7101L, "DEDICATED-PRESSURE", "FINAL"));
        mockFormalSuccessEvidence(tasks);
        when(regulationVersionMapper.selectById(9902L))
                .thenReturn(regulationVersion(9902L, true, null));
        when(regulationVersionMapper.selectById(9903L))
                .thenReturn(regulationVersion(9903L, false, "通用外观规程由专用末检覆盖"));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("正式检验结论"));
    }

    @Test
    void evaluateInspectionResultPassesWhenScrapOnlyFailureHasClosedQaDisposition() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL"));
        mockFormalSuccessEvidence(tasks);
        MesPqcInspectionTaskDO scrapOnlyFailure = tasks.get(0);
        mockFormalPqcEvidence(scrapOnlyFailure,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE,
                "{\"scrapQuantity\":1,\"inspectionResult\":\"FAILURE\"}",
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
        lenient().when(nonconformanceReviewMapper.selectLatestBySource(
                        MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_SUBMISSION,
                        scrapOnlyFailure.getSubmittedEventId()))
                .thenReturn(closedPqcSubmissionReview(scrapOnlyFailure.getSubmittedEventId()));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, result.checkResult());
        assertTrue(result.failureReason().contains("必要处置依据完整"));
    }

    @Test
    void evaluateInspectionResultBlocksScrapOnlyFailureWithoutClosedQaDisposition() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL"));
        mockFormalSuccessEvidence(tasks);
        MesPqcInspectionTaskDO scrapOnlyFailure = tasks.get(0);
        mockFormalPqcEvidence(scrapOnlyFailure,
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE,
                "{\"scrapQuantity\":1,\"inspectionResult\":\"FAILURE\"}",
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("失败检验缺少不合格处置记录"));
    }

    @Test
    void evaluateInspectionResultBlocksWhenScrapQuantityEvidenceIsMissingFromFormalPayload() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL"));
        mockFormalSuccessEvidence(tasks);
        mockFormalPqcEvidence(tasks.get(0),
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS,
                "{\"inspectionResult\":\"SUCCESS\"}",
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("scrapQuantity"));
    }

    @Test
    void evaluateInspectionResultBlocksWhenConfirmedTaskLacksFormalInspectionResultEvidence() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(batch);
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"),
                confirmedPqcTask(4L, "FINAL", "FINAL"));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(batch.getWorkOrderId(), batch.getRouteId(),
                batch.getRouteVersionId())).thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
        when(processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(List.of(processSnapshot()));

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("PQC 检验结果证据不完整"));
        assertTrue(result.failureReason().contains("正式提交事件"));
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
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"));
        mockFormalSuccessEvidence(tasks);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
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
        List<MesPqcInspectionTaskDO> tasks = List.of(
                confirmedPqcTask(1L, "FIRST", "FIRST"),
                confirmedPqcTask(2L, "PATROL", "AM"),
                confirmedPqcTask(3L, "PATROL", "PM"));
        mockFormalSuccessEvidence(tasks);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId())).thenReturn(tasks);
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
        return confirmedPqcTask(id, inspectionType, shiftCode, 9902L, 7001L, "QA-ITEM-A",
                inspectionRuleKey(inspectionType, shiftCode));
    }

    private static MesPqcInspectionTaskDO confirmedPqcTask(Long id, String inspectionType, String shiftCode,
                                                          Long regulationVersionId, Long qaProcessId,
                                                          String qaItemCode, String inspectionRuleKey) {
        return MesPqcInspectionTaskDO.builder()
                .id(id)
                .activeOrderId(12L)
                .workOrderId(980008L)
                .routeId(922119L)
                .routeVersionId(922120L)
                .routeProcessId(928609L)
                .processId(6001L)
                .qaProcessId(qaProcessId)
                .qaItemCode(qaItemCode)
                .regulationVersionId(regulationVersionId)
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(LocalDate.of(2026, 9, 13))
                .shiftCode(shiftCode)
                .roundNo(1)
                .plannedInspectionQuantity(1)
                .actualInspectionQuantity(1)
                .taskStatus("CONFIRMED")
                .submittedEventId(10000L + id)
                .build();
    }

    private static String inspectionRuleKey(String inspectionType, String shiftCode) {
        if ("PATROL".equals(inspectionType) && "AM".equals(shiftCode)) {
            return "PATROL_AM";
        }
        if ("PATROL".equals(inspectionType) && "PM".equals(shiftCode)) {
            return "PATROL_PM";
        }
        return inspectionType;
    }

    private static MesQaInspectionRegulationVersionDO regulationVersion(Boolean finalInspectionApplicable,
                                                                        String reason) {
        return regulationVersion(9902L, finalInspectionApplicable, reason);
    }

    private static MesQaInspectionRegulationVersionDO regulationVersion(Long id, Boolean finalInspectionApplicable,
                                                                        String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(id)
                .regulationId(id - 1)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .snapshotJson("{}")
                .build();
    }

    private void mockFormalSuccessEvidence(List<MesPqcInspectionTaskDO> tasks) {
        for (MesPqcInspectionTaskDO task : tasks) {
            mockFormalPqcEvidence(task,
                    MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS,
                    "{\"scrapQuantity\":0,\"inspectionResult\":\"SUCCESS\"}",
                    MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
        }
    }

    private void mockFormalPqcEvidence(MesPqcInspectionTaskDO task, String inspectionResult,
                                       String rawPayload, String pieceJudgement) {
        LocalDateTime aggregatedAt = LocalDateTime.of(2026, 9, 13, 9, 30);
        MesProProcessPoolPqcRecordDO record = MesProProcessPoolPqcRecordDO.builder()
                .id(20000L + task.getId())
                .eventId(task.getSubmittedEventId())
                .workOrderId(task.getWorkOrderId())
                .routeId(task.getRouteId())
                .routeProcessId(task.getRouteProcessId())
                .processId(task.getProcessId())
                .qaProcessId(task.getQaProcessId())
                .actualEmployeeId(501L)
                .signatureId(30000L + task.getId())
                .signatureUserId(501L)
                .inspectionResult(inspectionResult)
                .serverSubmitTime(aggregatedAt.minusMinutes(5))
                .rawPayload(rawPayload)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .processInspectionReviewId(40000L + task.getId())
                .processInspectionAggregatedAt(aggregatedAt)
                .build();
        MesPqcInspectionPieceDetailDO pieceDetail = MesPqcInspectionPieceDetailDO.builder()
                .id(50000L + task.getId())
                .taskId(task.getId())
                .sampleNo(1)
                .itemCode(task.getQaItemCode())
                .itemName(task.getQaItemCode())
                .inspectionMethod("目视")
                .standardText("合格")
                .resultType("TEXT")
                .itemResult("OK")
                .measuredValue("OK")
                .judgement(pieceJudgement)
                .build();
        MesPqcProcessInspectionAggregateDetailDO aggregateDetail = MesPqcProcessInspectionAggregateDetailDO.builder()
                .id(60000L + task.getId())
                .sourcePqcRecordId(record.getId())
                .sourcePieceDetailId(pieceDetail.getId())
                .eventId(task.getSubmittedEventId())
                .reviewId(record.getProcessInspectionReviewId())
                .pqcTaskId(task.getId())
                .activeOrderId(task.getActiveOrderId())
                .workOrderId(task.getWorkOrderId())
                .routeId(task.getRouteId())
                .routeVersionId(task.getRouteVersionId())
                .routeProcessId(task.getRouteProcessId())
                .processId(task.getProcessId())
                .regulationVersionId(task.getRegulationVersionId())
                .inspectionType(task.getInspectionType())
                .businessDate(task.getBusinessDate())
                .shiftCode(task.getShiftCode())
                .roundNo(task.getRoundNo())
                .actualInspectionQuantity(task.getActualInspectionQuantity())
                .sampleNo(pieceDetail.getSampleNo())
                .itemCode(pieceDetail.getItemCode())
                .itemName(pieceDetail.getItemName())
                .inspectionMethod(pieceDetail.getInspectionMethod())
                .standardText(pieceDetail.getStandardText())
                .resultType(pieceDetail.getResultType())
                .itemResult(pieceDetail.getItemResult())
                .measuredValue(pieceDetail.getMeasuredValue())
                .judgement(pieceDetail.getJudgement())
                .aggregatedAt(aggregatedAt)
                .build();
        lenient().when(pqcRecordMapper.selectByEventId(task.getSubmittedEventId())).thenReturn(record);
        lenient().when(pqcPieceDetailMapper.selectListByTaskId(task.getId())).thenReturn(List.of(pieceDetail));
        lenient().when(aggregateDetailMapper.selectListByEventId(task.getSubmittedEventId()))
                .thenReturn(List.of(aggregateDetail));
    }

    private static MesProEdhrNonconformanceReviewDO closedPqcSubmissionReview(Long submittedEventId) {
        return MesProEdhrNonconformanceReviewDO.builder()
                .id(70000L + submittedEventId)
                .sourceType(MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_SUBMISSION)
                .sourceId(submittedEventId)
                .reviewStatus(MesProEdhrNonconformanceReviewService.STATUS_CLOSED)
                .disposition(MesProEdhrNonconformanceReviewService.DISPOSITION_CONCESSION_RELEASE)
                .reviewMaterialUrl("https://qa.example/review-material")
                .reviewOpinion("QA 已完成让步放行处置")
                .qaSignature("QA电子签名#9001")
                .qaUserId(9001L)
                .closedAt(LocalDateTime.of(2026, 9, 13, 10, 0))
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
