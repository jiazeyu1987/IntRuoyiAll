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
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProcessPoolPqcInspectionCorrectionServiceTest {

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

    private static void assertCorrection(String resultType, String requestedValue,
                                         BigDecimal lower, BigDecimal upper, Integer precision,
                                         String expectedJudgement, String expectedStoredValue) {
        Fixture fixture = new Fixture(resultType, lower, upper, precision);

        assertEquals(701L, fixture.service.correct(fixture.command(requestedValue)));

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

        private final MesProProcessPoolPqcRecordMapper pqcRecordMapper =
                mock(MesProProcessPoolPqcRecordMapper.class);
        private final MesPqcInspectionPieceDetailMapper pieceDetailMapper =
                mock(MesPqcInspectionPieceDetailMapper.class);
        private final MesProcessPoolPqcInspectionCorrectionService service;

        private Fixture(String resultType, BigDecimal lower, BigDecimal upper, Integer precision) {
            MesProProcessPoolEventMapper eventMapper = mock(MesProProcessPoolEventMapper.class);
            MesPqcInspectionTaskMapper taskMapper = mock(MesPqcInspectionTaskMapper.class);
            MesProcessPoolEventRevisionService revisionService = mock(MesProcessPoolEventRevisionService.class);
            MesProBatchRecordExecutionSignatureService signatureService =
                    mock(MesProBatchRecordExecutionSignatureService.class);
            MesTeamLeaderScopeService scopeService = mock(MesTeamLeaderScopeService.class);
            MesReportAllocationReleaseStateService releaseStateService =
                    mock(MesReportAllocationReleaseStateService.class);

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
                    releaseStateService, mock(MesPqcProcessInspectionAggregationService.class));
        }

        private MesProcessPoolPqcInspectionCorrectionCommand command(String requestedValue) {
            return new MesProcessPoolPqcInspectionCorrectionCommand()
                    .setEventId(EVENT_ID)
                    .setActorUserId(ACTOR_ID)
                    .setActualInspectionQuantity(1)
                    .setScrapQuantity(0)
                    .setNonconformanceDescription("纠正后")
                    .setItemResults(List.of(new MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand()
                            .setItemCode("QA-001")
                            .setSampleValues(List.of(requestedValue))))
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
