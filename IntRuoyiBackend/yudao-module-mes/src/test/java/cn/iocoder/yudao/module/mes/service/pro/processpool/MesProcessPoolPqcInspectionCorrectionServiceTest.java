package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
        private final MesProcessPoolPqcInspectionCorrectionService service;

        private Fixture(String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesTeamLeaderScopeService scopeService = mock(MesTeamLeaderScopeService.class);

            when(eventMapper.selectByIdForUpdate(EVENT_ID)).thenReturn(event());
            when(taskMapper.selectByIdForUpdate(TASK_ID)).thenReturn(task());
            when(releaseStateService.findReleasedActiveOrderIdsForUpdate(List.of(5001L))).thenReturn(Set.of());
            when(pqcRecordMapper.selectByEventId(EVENT_ID)).thenReturn(record());
            when(pieceDetailMapper.selectListByTaskId(TASK_ID))
                    .thenReturn(List.of(existingDetail(resultType, lower, upper, precision)));
            when(signatureService.recordFieldChangeSignature(any())).thenReturn(signature());
            when(revisionService.updatePqcInspectionRecord(any())).thenReturn(701L);
            when(taskMapper.updateById(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
            when(pieceDetailMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
            when(pqcRecordMapper.updateById(any(MesProProcessPoolPqcRecordDO.class))).thenReturn(1);

            service = new MesProcessPoolPqcInspectionCorrectionService(eventMapper, pqcRecordMapper,
                    taskMapper, pieceDetailMapper, revisionService, signatureService, scopeService,
                    releaseStateService, aggregationService, nonconformanceReviewService);
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
