package cn.iocoder.yudao.module.mes.service.pro.processpool;

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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MesProcessPoolPqcInspectionCorrectionServiceTest {

    @Test
    void approvalCommittedAfterIdentityReadStillRefreshesCorrectionAggregation() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectByIdForUpdate(Fixture.TASK_ID);
        task.setQaProcessId(6001L).setRouteVersionId(2101L).setRegulationVersionId(6101L)
                .setInspectionType("PROCESS").setBusinessDate(java.time.LocalDate.of(2026, 9, 28))
                .setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO snapshotRecord = Fixture.record()
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("PENDING");
        MesProProcessPoolPqcRecordDO committedRecord = Fixture.record()
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("PENDING");
        AtomicBoolean identityRead = new AtomicBoolean();
        AtomicBoolean approvalCommitted = new AtomicBoolean();
        AtomicBoolean ownRecordWrite = new AtomicBoolean();
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenAnswer(invocation -> {
            identityRead.set(true);
            return event;
        });
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            assertTrue(identityRead.get(), "The identity read establishes the older transaction snapshot");
            // Another transaction commits its first approval before correction obtains its write locks.
            task.setTaskStatus("CONFIRMED");
            committedRecord.setProcessInspectionAggregationStatus("AGGREGATED")
                    .setProcessInspectionReviewId(7000L);
            approvalCommitted.set(true);
            return fixture.activeOrder;
        });
        initializeMapperMetadata(MesProProcessPoolPqcRecordDO.class);
        MesProProcessPoolPqcRecordMapper recordMapper = mock(MesProProcessPoolPqcRecordMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertTrue(approvalCommitted.get());
            Wrapper<MesProProcessPoolPqcRecordDO> query = invocation.getArgument(0);
            return query.getSqlSegment().contains("FOR UPDATE") || ownRecordWrite.get()
                    ? committedRecord : snapshotRecord;
        }).when(recordMapper).selectOne(org.mockito.ArgumentMatchers.<Wrapper<MesProProcessPoolPqcRecordDO>>any());
        org.mockito.Mockito.doAnswer(invocation -> {
            ownRecordWrite.set(true);
            return 1;
        }).when(recordMapper).updateById(any(MesProProcessPoolPqcRecordDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(committedRecord.getProcessInspectionReviewId(), invocation.getArgument(2));
            committedRecord.setProcessInspectionReviewId(invocation.getArgument(3));
            return 1;
        }).when(recordMapper).replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any());
        ReflectionTestUtils.setField(fixture.service, "pqcRecordMapper", recordMapper);
        List<MesProcessPoolSubmissionReviewDO> reviews = fixture.setupConfirmedReview();
        AtomicReference<List<MesPqcInspectionPieceDetailDO>> currentDetails = new AtomicReference<>(
                fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> currentDetails.get());
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> rows = invocation.getArgument(0);
            rows.get(0).setId(7200L);
            currentDetails.set(rows);
            return true;
        });
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return 701L;
        });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        ReflectionTestUtils.setField(fixture.service, "aggregationService",
                new MesPqcProcessInspectionAggregationServiceImpl(recordMapper, fixture.eventMapper,
                        fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper, fixture.reviewMapper));

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        assertEquals(2, reviews.size(), "A committed approval must gain a signed correction review");
        assertEquals(7000L, reviews.get(0).getId(), "The previous review remains as evidence");
        assertEquals(7001L, committedRecord.getProcessInspectionReviewId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(rows.capture());
        assertEquals("合格", rows.getValue().get(0).getMeasuredValue());
        assertEquals(7200L, rows.getValue().get(0).getSourcePieceDetailId());
        assertEquals(7001L, rows.getValue().get(0).getReviewId());
        assertEquals("PENDING", snapshotRecord.getProcessInspectionAggregationStatus());
    }

    @Test
    void priorCorrectionCommittedAfterIdentityReadKeepsCurrentPieceEvidenceAndEquipment() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesPqcInspectionPieceDetailDO snapshotPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setSelectedEquipmentId(8100L).setSelectedEquipmentNumber("OLD-EQUIPMENT");
        MesPqcInspectionPieceDetailDO committedPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setId(7150L).setMeasuredValue("不合格").setItemResult("不合格").setJudgement("FAILURE")
                .setSelectedEquipmentId(8200L).setSelectedEquipmentNumber("CURRENT-EQUIPMENT");
        AtomicBoolean identityRead = new AtomicBoolean();
        AtomicBoolean previousCorrectionCommitted = new AtomicBoolean();
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenAnswer(invocation -> {
            identityRead.set(true);
            return event;
        });
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            assertTrue(identityRead.get());
            event.setRawPayload("{\"inspectionResult\":\"FAILURE\",\"scrapQuantity\":1}");
            previousCorrectionCommitted.set(true);
            return fixture.activeOrder;
        });
        initializeMapperMetadata(MesPqcInspectionPieceDetailDO.class);
        MesPqcInspectionPieceDetailMapper pieceMapper = mock(MesPqcInspectionPieceDetailMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertTrue(previousCorrectionCommitted.get());
            Wrapper<MesPqcInspectionPieceDetailDO> query = invocation.getArgument(0);
            return List.of(query.getSqlSegment().contains("FOR UPDATE") ? committedPiece : snapshotPiece);
        }).when(pieceMapper).selectList(org.mockito.ArgumentMatchers.<Wrapper<MesPqcInspectionPieceDetailDO>>any());
        org.mockito.Mockito.doReturn(1).when(pieceMapper).deleteByTaskId(Fixture.TASK_ID);
        org.mockito.Mockito.doReturn(true).when(pieceMapper).insertBatch(any());
        ReflectionTestUtils.setField(fixture.service, "pieceDetailMapper", pieceMapper);

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revision =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revision.capture());
        MesProcessPoolEventRevisionFieldChangeBO sampleChange = revision.getValue().getChangedFields().stream()
                .filter(change -> "PQC.ITEM.QA-001".equals(change.getFieldCode())).findFirst().orElseThrow();
        assertEquals("不合格", sampleChange.getBeforeValue(), "Audit evidence must describe the immediately preceding revision");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(pieceMapper).insertBatch(rows.capture());
        assertEquals(8200L, rows.getValue().get(0).getSelectedEquipmentId());
        assertEquals("CURRENT-EQUIPMENT", rows.getValue().get(0).getSelectedEquipmentNumber());
        assertEquals("原值", snapshotPiece.getMeasuredValue());
        assertEquals("不合格", committedPiece.getMeasuredValue());
    }

    private static void initializeMapperMetadata(Class<?> entityClass) {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "pqc-correction-snapshot-test"),
                entityClass);
    }

    @Test
    void correctionRefreshExcludesHistoryVisibleOnlyToOldSnapshot() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectById(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectById(Fixture.TASK_ID);
        task.setTaskStatus("CONFIRMED").setQaProcessId(6001L).setRouteVersionId(2101L)
                .setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID)
                .setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        String payloadHash = cn.iocoder.yudao.module.mes.service.pro.batchrecord
                .MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload());
        MesProcessPoolSubmissionReviewDO correctionReview = MesProcessPoolSubmissionReviewDO.builder()
                .id(7001L).eventId(Fixture.EVENT_ID).leaderType("PQC").leaderUserId(Fixture.ACTOR_ID)
                .reviewStatus("APPROVED").reviewSignatureId(9102L).reviewSignatureUserId(Fixture.ACTOR_ID)
                .reviewSignatureSnapshotJson("{\"actionType\":\"PQC_INSPECTION_CORRECTION\","
                        + "\"supersededReviewId\":7000,\"revisionId\":701,\"signatureId\":9102,"
                        + "\"payloadHash\":\"" + payloadHash + "\"}").build();
        correctionReview.setTenantId(1L);
        when(fixture.reviewMapper.selectLatestByEventIdForUpdate(Fixture.EVENT_ID)).thenReturn(correctionReview);
        MesPqcInspectionPieceDetailDO snapshotHistory = Fixture.existingDetail("BOOLEAN", null, null, null);
        MesPqcInspectionPieceDetailDO ownCurrentPiece = Fixture.existingDetail("BOOLEAN", null, null, null)
                .setId(7200L).setMeasuredValue("合格").setItemResult("合格");
        initializeMapperMetadata(MesPqcInspectionPieceDetailDO.class);
        MesPqcInspectionPieceDetailMapper pieceMapper = mock(MesPqcInspectionPieceDetailMapper.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doAnswer(invocation -> {
            Wrapper<MesPqcInspectionPieceDetailDO> query = invocation.getArgument(0);
            // A prior transaction deleted history; this transaction deleted its successor and inserted current.
            // A consistent snapshot can still see history together with our own newly inserted row.
            return query.getSqlSegment().contains("FOR UPDATE")
                    ? List.of(ownCurrentPiece) : List.of(snapshotHistory, ownCurrentPiece);
        }).when(pieceMapper).selectList(org.mockito.ArgumentMatchers.<Wrapper<MesPqcInspectionPieceDetailDO>>any());
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenReturn(1);
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        MesPqcProcessInspectionAggregationService realAggregation = new MesPqcProcessInspectionAggregationServiceImpl(
                fixture.pqcRecordMapper, fixture.eventMapper, fixture.taskMapper, pieceMapper, aggregateMapper,
                fixture.reviewMapper);

        realAggregation.refreshCorrectedPqcSubmission(Fixture.EVENT_ID, 7000L, 7001L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(rows.capture());
        assertEquals(1, rows.getValue().size(), "Current aggregation must exclude history visible to an older snapshot");
        assertEquals(7200L, rows.getValue().get(0).getSourcePieceDetailId());
        assertEquals("合格", rows.getValue().get(0).getMeasuredValue());
    }

    @Test
    void concurrentCompletionCanFinishBeforeCorrectionWithoutReverseLockWait() throws Exception {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        var orderLock = new java.util.concurrent.locks.ReentrantLock();
        var workLock = new java.util.concurrent.locks.ReentrantLock();
        var firstLockAttempt = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        when(fixture.activeOrderMapper.selectByIdForUpdate(5001L)).thenAnswer(invocation -> {
            firstLockAttempt.countDown();
            orderLock.lock();
            return fixture.activeOrder;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            workLock.lock();
            firstLockAttempt.countDown();
            return null;
        }).when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        orderLock.lock();
        try {
            var correction = executor.submit(() -> {
                try {
                    return assertThrows(ServiceException.class,
                            () -> fixture.service.correct(fixture.command("合格")));
                } finally {
                    if (workLock.isHeldByCurrentThread()) workLock.unlock();
                    if (orderLock.isHeldByCurrentThread()) orderLock.unlock();
                }
            });
            assertTrue(firstLockAttempt.await(5, java.util.concurrent.TimeUnit.SECONDS));
            boolean completionCanLockWorkOrder = workLock.tryLock(300, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (completionCanLockWorkOrder) workLock.unlock();
            fixture.activeOrder.setBusinessStatus("COMPLETED");
            orderLock.unlock();
            correction.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(completionCanLockWorkOrder, "Correction must not hold the work order while waiting for completion's active order");
            verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
        } finally {
            if (orderLock.isHeldByCurrentThread()) orderLock.unlock();
            executor.shutdownNow();
        }
    }

    @Test
    void correctConfirmedPqcUsingRealAggregationReplacesItsFormalDetails() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectByIdForUpdate(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectByIdForUpdate(Fixture.TASK_ID);
        task.setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).setQaProcessId(6001L)
                .setRouteVersionId(2101L).setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO record = fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID);
        record.setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        when(fixture.eventMapper.selectById(Fixture.EVENT_ID)).thenReturn(event);
        when(fixture.taskMapper.selectById(Fixture.TASK_ID)).thenReturn(task);
        AtomicReference<List<MesPqcInspectionPieceDetailDO>> currentDetails = new AtomicReference<>(
                fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> currentDetails.get());
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> rows = invocation.getArgument(0);
            for (int i = 0; i < rows.size(); i++) rows.get(i).setId(7200L + i);
            currentDetails.set(rows);
            return true;
        });
        when(fixture.taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(invocation -> {
            task.setActualInspectionQuantity(invocation.getArgument(0, MesPqcInspectionTaskDO.class)
                    .getActualInspectionQuantity());
            return 1;
        });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper =
                mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        fixture.setupConfirmedReview();
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return 701L;
        });
        MesPqcProcessInspectionAggregationService realAggregation =
                new MesPqcProcessInspectionAggregationServiceImpl(fixture.pqcRecordMapper,
                        fixture.eventMapper, fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper,
                        fixture.reviewMapper);
        ReflectionTestUtils.setField(fixture.service, "aggregationService", realAggregation);

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper).insertBatch(captor.capture());
        assertEquals("合格", captor.getValue().get(0).getMeasuredValue());
        assertEquals(7200L, captor.getValue().get(0).getSourcePieceDetailId());
        assertEquals(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED, task.getTaskStatus());
        assertEquals(7001L, captor.getValue().get(0).getReviewId());
        verify(aggregateMapper).deleteByEventId(Fixture.EVENT_ID);
    }

    @Test
    void blocksCompletedRemovedAndReworkedCycleBeforeSignatureOrFormalWrites() {
        for (String status : List.of("COMPLETED", "REWORKED", "RELEASED", "REMOVED")) {
            Fixture fixture = new Fixture("BOOLEAN", null, null, null);
            fixture.activeOrder.setBusinessStatus(status);
            assertThrows(ServiceException.class, () -> fixture.service.correct(fixture.command("合格")));
            verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
            verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        }
    }

    @Test
    void twoSignedCorrectionsRefreshCurrentAggregationAndKeepReviewAndPieceHistory() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        MesProProcessPoolEventDO event = fixture.eventMapper.selectById(Fixture.EVENT_ID);
        event.setQaProcessId(6001L);
        MesPqcInspectionTaskDO task = fixture.taskMapper.selectById(Fixture.TASK_ID);
        task.setTaskStatus("CONFIRMED").setQaProcessId(6001L).setRouteVersionId(2101L)
                .setRegulationVersionId(6101L).setInspectionType("PROCESS")
                .setBusinessDate(java.time.LocalDate.of(2026, 9, 28)).setShiftCode("DAY").setRoundNo(1);
        MesProProcessPoolPqcRecordDO record = fixture.pqcRecordMapper.selectByEventId(Fixture.EVENT_ID);
        record.setWorkOrderId(1001L).setRouteId(2001L).setQaProcessId(6001L)
                .setProcessInspectionAggregationStatus("AGGREGATED").setProcessInspectionReviewId(7000L);
        List<MesProcessPoolSubmissionReviewDO> reviews = fixture.setupConfirmedReview();
        List<List<MesPqcInspectionPieceDetailDO>> pieceHistory = new ArrayList<>();
        pieceHistory.add(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID));
        when(fixture.pieceDetailMapper.selectListByTaskId(Fixture.TASK_ID))
                .thenAnswer(invocation -> pieceHistory.get(pieceHistory.size() - 1));
        when(fixture.pieceDetailMapper.insertBatch(any())).thenAnswer(invocation -> {
            List<MesPqcInspectionPieceDetailDO> next = invocation.getArgument(0);
            next.get(0).setId(7200L + pieceHistory.size());
            pieceHistory.add(next);
            return true;
        });
        AtomicLong revisionId = new AtomicLong(700L);
        when(fixture.revisionService.updatePqcInspectionRecord(any())).thenAnswer(invocation -> {
            event.setRawPayload(invocation.getArgument(0, MesProcessPoolEventRevisionUpdateReqBO.class).getAfterPayload());
            return revisionId.incrementAndGet();
        });
        AtomicLong signatureId = new AtomicLong(9200L);
        when(fixture.signatureService.recordFieldChangeSignature(any())).thenAnswer(invocation ->
                Fixture.signature().setSignatureId(signatureId.incrementAndGet()));
        when(fixture.pqcRecordMapper.replaceProcessInspectionReviewIfAggregated(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    assertEquals(record.getProcessInspectionReviewId(), invocation.getArgument(2));
                    record.setProcessInspectionReviewId(invocation.getArgument(3));
                    return 1;
                });
        MesPqcProcessInspectionAggregateDetailMapper aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        when(aggregateMapper.deleteByEventId(Fixture.EVENT_ID)).thenReturn(1);
        when(aggregateMapper.insertBatch(any())).thenReturn(true);
        ReflectionTestUtils.setField(fixture.service, "aggregationService",
                new MesPqcProcessInspectionAggregationServiceImpl(fixture.pqcRecordMapper, fixture.eventMapper,
                        fixture.taskMapper, fixture.pieceDetailMapper, aggregateMapper, fixture.reviewMapper));

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));
        assertEquals(702L, fixture.service.correct(fixture.command(List.of("不合格"), 1)));

        assertEquals(3, pieceHistory.size());
        assertEquals("原值", pieceHistory.get(0).get(0).getMeasuredValue());
        assertEquals("FAILURE", pieceHistory.get(2).get(0).getJudgement());
        assertEquals(3, reviews.size());
        assertEquals(9201L, reviews.get(1).getReviewSignatureId());
        assertEquals(9202L, reviews.get(2).getReviewSignatureId());
        assertEquals(7002L, record.getProcessInspectionReviewId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcProcessInspectionAggregateDetailDO>> rows = ArgumentCaptor.forClass(List.class);
        verify(aggregateMapper, org.mockito.Mockito.times(2)).insertBatch(rows.capture());
        assertEquals("FAILURE", rows.getAllValues().get(1).get(0).getJudgement());
        assertEquals(7002L, rows.getAllValues().get(1).get(0).getReviewId());
    }

    @Test
    void blocksPendingReleaseApplicationBeforeSignatureOrFormalWrites() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        when(fixture.releaseStateService.isReleaseApplicationLockedForUpdate(5001L)).thenReturn(true);
        assertThrows(ServiceException.class, () -> fixture.service.correct(fixture.command("合格")));
        verifyNoInteractions(fixture.signatureService, fixture.revisionService, fixture.reviewMapper);
    }

    @Test
    void rejectedCorrectionSignsCurrentReviewLinkInsteadOfInheritingPreviousPayloadLink() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        fixture.eventMapper.selectById(Fixture.EVENT_ID).setRawPayload(
                "{\"inspectionResult\":\"SUCCESS\",\"scrapQuantity\":0,\"supersededReviewId\":6999}");
        when(fixture.reviewMapper.selectLatestByEventIdForUpdate(Fixture.EVENT_ID))
                .thenReturn(MesProcessPoolSubmissionReviewDO.builder().id(7000L).eventId(Fixture.EVENT_ID)
                        .leaderType("PQC").reviewStatus("REJECTED").build());

        assertEquals(701L, fixture.service.correct(fixture.command("合格")));

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revision =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revision.capture());
        assertTrue(revision.getValue().getAfterPayload().contains("\"supersededReviewId\":7000"));
        assertEquals(9102L, revision.getValue().getRevisionSignatureId());
        verify(fixture.reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verifyNoInteractions(fixture.aggregationService);
    }

    @Test
    void correctPqcInspection_shouldRejectWhenPqcSubmissionIsUnderNonconformanceReview() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/"
                        + "MesProcessPoolPqcInspectionCorrectionService.java"));

        int guard = source.indexOf("nonconformanceReviewService.ensureWorkOrderNotFrozen");
        int signature = source.indexOf("recordCorrectionSignature");
        int formalUpdate = source.indexOf("updateFormalPqcTables");
        assertTrue(guard > 0, "PQC correction must check nonconformance freeze before controlled correction writes");
        assertTrue(guard < signature, "PQC correction freeze gate must run before electronic signature is recorded");
        assertTrue(guard < formalUpdate, "PQC correction freeze gate must run before formal PQC facts are overwritten");
    }

    @Test
    void correctStopsBeforeSignatureAndFormalWritesWhenNonconformanceReviewFreezesWorkOrder() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command("不合格")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), error.getCode());
        verify(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        verifyNoInteractions(fixture.signatureService, fixture.releaseStateService,
                fixture.pqcRecordMapper, fixture.pieceDetailMapper, fixture.revisionService);
    }

    @Test
    void appliesCanonicalBooleanSemantics() {
        assertCorrection("BOOLEAN", "合格", null, null, null, "SUCCESS", "合格");
        assertCorrection("BOOLEAN", "不合格", null, null, null, "FAILURE", "不合格");
    }

    @Test
    void appliesCanonicalNumericInclusiveBoundsAndPrecision() {
        assertCorrection("NUMERIC", "1.00", decimal("1.00"), decimal("2.00"), 2, "SUCCESS", "1.00");
        assertCorrection("NUMERIC", "2.00", decimal("1.00"), decimal("2.00"), 2, "SUCCESS", "2.00");
        assertCorrection("NUMERIC", "2.01", decimal("1.00"), decimal("2.00"), 2, "FAILURE", "2.01");

        assertInvalidCorrection("NUMERIC", "1.001", decimal("1.00"), decimal("2.00"), 2);
        assertInvalidCorrection("NUMERIC", "1e0", decimal("1.00"), decimal("2.00"), 2);
    }

    @Test
    void appliesCanonicalTextSemanticsAndRejectsBlankText() {
        assertCorrection("TEXT", "  修正说明  ", null, null, null, "SUCCESS", "修正说明");
        assertInvalidCorrection("TEXT", "   ", null, null, null);
    }

    @Test
    void rejectsLegacyNumberAndChoiceAliases() {
        assertInvalidCorrection("NUMBER", "1.00", decimal("1.00"), decimal("2.00"), 2);
        assertInvalidCorrection("CHOICE", "合格", null, null, null);
    }

    @Test
    void rejectsScrapQuantityAboveActualInspectionQuantityBeforeLoadingEvent() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(
                        List.of("合格", "合格", "合格", "合格", "合格"), 10)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(fixture.eventMapper, never()).selectByIdForUpdate(any());
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
    }

    @Test
    void rejectsScrapQuantityBelowFailedPieceDetailFloorBeforeWrite() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(
                        List.of("不合格", "不合格", "合格", "合格", "合格"), 1)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
        verify(fixture.taskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        verify(fixture.pieceDetailMapper, never()).insertBatch(any());
        verify(fixture.pqcRecordMapper, never()).updateById(any(MesProProcessPoolPqcRecordDO.class));
    }

    @Test
    void permitsScrapQuantityEqualFailedPieceDetailFloor() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);

        assertEquals(701L, fixture.service.correct(fixture.command(
                List.of("不合格", "不合格", "合格", "合格", "合格"), 2)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> detailsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.pieceDetailMapper).insertBatch(detailsCaptor.capture());
        List<MesPqcInspectionPieceDetailDO> details = detailsCaptor.getValue();
        assertEquals(5, details.size());
        assertEquals(2L, details.stream()
                .filter(detail -> MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(
                        detail.getJudgement()))
                .map(MesPqcInspectionPieceDetailDO::getSampleNo)
                .distinct()
                .count());

        ArgumentCaptor<MesProProcessPoolPqcRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolPqcRecordDO.class);
        verify(fixture.pqcRecordMapper).updateById(recordCaptor.capture());
        assertEquals(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE,
                recordCaptor.getValue().getInspectionResult());
    }

    @Test
    void rejectsFrozenWorkOrderBeforeCorrectionWrites() {
        Fixture fixture = new Fixture("BOOLEAN", null, null, null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command("不合格")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), error.getCode());
        verify(fixture.nonconformanceReviewService).ensureWorkOrderNotFrozen(1001L, "PQC检验更正");
        verify(fixture.signatureService, never()).recordFieldChangeSignature(any());
        verify(fixture.revisionService, never()).updatePqcInspectionRecord(any());
        verify(fixture.pqcRecordMapper, never()).selectByEventId(any());
        verify(fixture.pqcRecordMapper, never()).selectByEventIdForUpdate(any());
        verify(fixture.pieceDetailMapper, never()).deleteByTaskId(any());
        verify(fixture.pieceDetailMapper, never()).insertBatch(any());
        verify(fixture.taskMapper, never()).updateById(any(MesPqcInspectionTaskDO.class));
        verify(fixture.pqcRecordMapper, never()).updateById(any(MesProProcessPoolPqcRecordDO.class));
        verify(fixture.aggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    private static void assertCorrection(String resultType, String requestedValue,
                                         BigDecimal lower, BigDecimal upper, Integer precision,
                                         String expectedJudgement, String expectedStoredValue) {
        Fixture fixture = new Fixture(resultType, lower, upper, precision);

        int scrapQuantity = MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(expectedJudgement) ? 1 : 0;
        assertEquals(701L, fixture.service.correct(fixture.command(List.of(requestedValue), scrapQuantity)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesPqcInspectionPieceDetailDO>> detailsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.pieceDetailMapper).insertBatch(detailsCaptor.capture());
        MesPqcInspectionPieceDetailDO detail = detailsCaptor.getValue().get(0);
        assertEquals(resultType, detail.getResultType());
        assertEquals(expectedStoredValue, detail.getMeasuredValue());
        assertEquals(expectedJudgement, detail.getJudgement());

        ArgumentCaptor<MesProProcessPoolPqcRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolPqcRecordDO.class);
        verify(fixture.pqcRecordMapper).updateById(recordCaptor.capture());
        assertEquals(expectedJudgement, recordCaptor.getValue().getInspectionResult());

        ArgumentCaptor<MesProcessPoolEventRevisionUpdateReqBO> revisionCaptor =
                ArgumentCaptor.forClass(MesProcessPoolEventRevisionUpdateReqBO.class);
        verify(fixture.revisionService).updatePqcInspectionRecord(revisionCaptor.capture());
        String afterPayload = revisionCaptor.getValue().getAfterPayload();
        org.junit.jupiter.api.Assertions.assertFalse(
                afterPayload.contains("nonconformanceDescription"),
                "PQC correction payload must not write the removed nonconformance description");
        org.junit.jupiter.api.Assertions.assertFalse(
                afterPayload.contains("defectDescription"),
                "PQC correction payload must not write the removed defect description");
    }

    private static void assertInvalidCorrection(String resultType, String requestedValue,
                                                BigDecimal lower, BigDecimal upper, Integer precision) {
        Fixture fixture = new Fixture(resultType, lower, upper, precision);

        ServiceException error = assertThrows(ServiceException.class,
                () -> fixture.service.correct(fixture.command(requestedValue)));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static final class Fixture {

        private static final long EVENT_ID = 176L;
        private static final long TASK_ID = 5101L;
        private static final long ACTOR_ID = 3001L;

        private final MesProProcessPoolEventMapper eventMapper =
                mock(MesProProcessPoolEventMapper.class);
        private final MesPqcInspectionTaskMapper taskMapper =
                mock(MesPqcInspectionTaskMapper.class);
        private final MesProProcessPoolPqcRecordMapper pqcRecordMapper =
                mock(MesProProcessPoolPqcRecordMapper.class);
        private final MesPqcInspectionPieceDetailMapper pieceDetailMapper =
                mock(MesPqcInspectionPieceDetailMapper.class);
        private final MesProcessPoolEventRevisionService revisionService =
                mock(MesProcessPoolEventRevisionService.class);
        private final MesProBatchRecordExecutionSignatureService signatureService =
                mock(MesProBatchRecordExecutionSignatureService.class);
        private final MesReportAllocationReleaseStateService releaseStateService =
                mock(MesReportAllocationReleaseStateService.class);
        private final MesProEdhrNonconformanceReviewService nonconformanceReviewService =
                mock(MesProEdhrNonconformanceReviewService.class);
        private final MesPqcProcessInspectionAggregationService aggregationService =
                mock(MesPqcProcessInspectionAggregationService.class);
        private final MesProcessPoolActiveOrderMapper activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        private final MesProcessPoolSubmissionReviewMapper reviewMapper = mock(MesProcessPoolSubmissionReviewMapper.class);
        private final MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(5001L).workOrderId(1001L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        private final MesProcessPoolPqcInspectionCorrectionService service;

        private Fixture(String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesTeamLeaderScopeService scopeService = mock(MesTeamLeaderScopeService.class);

            MesProProcessPoolEventDO event = event();
            MesPqcInspectionTaskDO task = task();
            when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event);
            when(eventMapper.selectById(EVENT_ID)).thenReturn(event);
            when(taskMapper.selectByIdForUpdate(TASK_ID)).thenReturn(task);
            when(taskMapper.selectById(TASK_ID)).thenReturn(task);
            activeOrder.setTenantId(1L);
            when(activeOrderMapper.selectByIdForUpdate(5001L)).thenReturn(activeOrder);
            when(releaseStateService.findReleasedActiveOrderIdsForUpdate(List.of(5001L))).thenReturn(Set.of());
            when(pqcRecordMapper.selectByEventId(EVENT_ID)).thenReturn(record());
            // Nonconcurrent fixtures expose the same row to both read modes; snapshot tests use separate versions.
            when(pqcRecordMapper.selectByEventIdForUpdate(EVENT_ID))
                    .thenAnswer(invocation -> pqcRecordMapper.selectByEventId(EVENT_ID));
            when(pieceDetailMapper.selectListByTaskId(TASK_ID))
                    .thenReturn(List.of(existingDetail(resultType, lower, upper, precision)));
            when(pieceDetailMapper.selectListByTaskIdForUpdate(TASK_ID))
                    .thenAnswer(invocation -> pieceDetailMapper.selectListByTaskId(TASK_ID));
            when(signatureService.recordFieldChangeSignature(any())).thenReturn(signature());
            when(revisionService.updatePqcInspectionRecord(any())).thenReturn(701L);
            when(taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
            when(pieceDetailMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
            when(pqcRecordMapper.updateById(any(MesProProcessPoolPqcRecordDO.class))).thenReturn(1);

            service = new MesProcessPoolPqcInspectionCorrectionService(eventMapper, pqcRecordMapper,
                    taskMapper, pieceDetailMapper, revisionService, signatureService, scopeService,
                    releaseStateService, aggregationService, nonconformanceReviewService);
            ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrderMapper);
            ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        }

        private List<MesProcessPoolSubmissionReviewDO> setupConfirmedReview() {
            MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                    .id(7000L).eventId(EVENT_ID).leaderType("PQC").leaderUserId(ACTOR_ID)
                    .reviewStatus("APPROVED").reviewSignatureId(9000L).reviewSignatureUserId(ACTOR_ID).build();
            review.setTenantId(1L);
            List<MesProcessPoolSubmissionReviewDO> history = new ArrayList<>(List.of(review));
            when(reviewMapper.selectLatestByEventIdForUpdate(EVENT_ID))
                    .thenAnswer(invocation -> history.get(history.size() - 1));
            when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
                MesProcessPoolSubmissionReviewDO next = invocation.getArgument(0);
                next.setId(7000L + history.size());
                history.add(next);
                return 1;
            });
            return history;
        }

        private MesProcessPoolPqcInspectionCorrectionCommand command(String requestedValue) {
            return command(List.of(requestedValue), 0);
        }

        private MesProcessPoolPqcInspectionCorrectionCommand command(List<String> requestedValues,
                                                                     int scrapQuantity) {
            return new MesProcessPoolPqcInspectionCorrectionCommand()
                    .setEventId(EVENT_ID)
                    .setActorUserId(ACTOR_ID)
                    .setActualInspectionQuantity(requestedValues.size())
                    .setScrapQuantity(scrapQuantity)
                    .setItemResults(List.of(new MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand()
                            .setItemCode("QA-001")
                            .setSampleValues(requestedValues)))
                    .setChangeReason("纠正检验值")
                    .setSignaturePassword("valid-password");
        }

        private static MesProProcessPoolEventDO event() {
            MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                    .id(EVENT_ID).eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                    .feedbackSourceType("MES_PQC_INSPECTION_TASK").feedbackSourceId(TASK_ID)
                    .recordbookSourceType("MES_PQC_INSPECTION_TASK").recordbookSourceId(TASK_ID)
                    .workOrderId(1001L).routeId(2001L).routeProcessId(3001L).processId(4001L)
                    .actualEmployeeId(101L).rawPayload("{\"inspectionResult\":\"SUCCESS\"," +
                            "\"scrapQuantity\":0,\"nonconformanceDescription\":\"纠正前\"}")
                    .build();
            event.setTenantId(1L);
            return event;
        }

        private static MesPqcInspectionTaskDO task() {
            MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder().id(TASK_ID).activeOrderId(5001L)
                    .workOrderId(1001L).routeId(2001L).routeProcessId(3001L).processId(4001L)
                    .actualInspectionQuantity(1).taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED).build();
            task.setTenantId(1L);
            return task;
        }

        private static MesProProcessPoolPqcRecordDO record() {
            MesProProcessPoolPqcRecordDO record = MesProProcessPoolPqcRecordDO.builder()
                    .id(6101L).eventId(EVENT_ID).inspectionResult("SUCCESS").build();
            record.setTenantId(1L);
            return record;
        }

        private static MesPqcInspectionPieceDetailDO existingDetail(
                String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesPqcInspectionPieceDetailDO detail = MesPqcInspectionPieceDetailDO.builder()
                    .id(7101L).taskId(TASK_ID).sampleNo(1).itemCode("QA-001").itemName("检验项目")
                    .inspectionMethod("目测").standardText("正式标准")
                    .standardLowerLimit(lower).standardUpperLimit(upper).standardPrecision(precision)
                    .resultType(resultType).itemResult("原值").measuredValue("原值").judgement("SUCCESS")
                    .build();
            detail.setTenantId(1L);
            return detail;
        }

        private static MesProBatchRecordExecutionFieldAuditSignatureResult signature() {
            return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                    .setSignatureId(9102L).setActorId(ACTOR_ID).setActorName("PQC组长")
                    .setSignedAt(LocalDateTime.of(2026, 8, 14, 10, 0));
        }
    }
}
