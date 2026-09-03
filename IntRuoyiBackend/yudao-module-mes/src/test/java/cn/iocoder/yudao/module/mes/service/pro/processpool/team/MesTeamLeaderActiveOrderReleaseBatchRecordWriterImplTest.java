package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProductionPickListSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest {

    private static final Long ACTIVE_ORDER_ID = 10L;
    private static final Long WORK_ORDER_ID = 30L;
    private static final Long ROUTE_ID = 40L;
    private static final Long ROUTE_VERSION_ID = 41L;
    private static final Long ROUTE_PROCESS_ID = 101L;
    private static final Long PROCESS_ID = 1L;
    private static final Long BATCH_EXECUTION_ID = 8701L;
    private static final Long BATCH_TASK_ID = 9701L;

    @Mock private MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    @Mock private MesProBatchRecordReportMapper reportMapper;
    @Mock private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesTeamLeaderBatchRecordBackfillService backfillService;
    @Mock private MesProductionPickListSourceService productionPickListSourceService;

    private MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl writer;

    @BeforeEach
    void setUp() {
        writer = new MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl(bindingMapper, reportMapper, ruleMapper,
                batchTaskMapper, backfillService, productionPickListSourceService);
    }

    @Test
    void writeRejectsDuplicateAllocationsForTheSameProductionSubmitEvent() {
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(batchTask()));

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = new MesTeamLeaderActiveOrderReleaseBatchRecordPlan()
                .setCommand(command())
                .setPreparedProcesses(List.of(new MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess()
                        .setSource(source())
                        .setBinding(binding())
                        .setRules(List.of())))
                .setBlockers(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> writer.write(plan, BATCH_EXECUTION_ID));

        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED.getCode(), ex.getCode());
        verify(batchTaskMapper).selectListByBatchExecutionId(BATCH_EXECUTION_ID);
        verifyNoInteractions(backfillService);
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand()
                .setTenantId(1L)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setPickListBindingIds(List.of(8801L))
                .setWorkOrderId(WORK_ORDER_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setDccProjectCodeId(8001L)
                .setProductId(3101L)
                .setBatchCode("BATCH-9001")
                .setApplicantUserId(4101L);
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source() {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                .setSnapshot(snapshot())
                .setCompletion(completion())
                .setSourceEvents(List.of(event()))
                .setAllocations(List.of(allocation(7101L), allocation(7102L)))
                .setReviews(List.of(review()));
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(4101L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(7301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(BATCH_EXECUTION_ID)
                .lastEventId(1001L)
                .lastReviewId(7201L)
                .sourceEventIdsJson("[1001]")
                .sourceAllocationIdsJson("[7101,7102]")
                .aggregateHash("agg-production-5001")
                .backfillIdempotencyKey("PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-production-5001")
                .build();
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .rawPayload("{\"outputQuantity\":80,\"pressure\":15}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .signatureId(1101L)
                .signatureUserId(2101L)
                .signatureSnapshot("{\"signedAt\":\"2026-08-01T08:30:00\"}")
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(id)
                .eventId(1001L)
                .reviewId(7201L)
                .leaderUserId(3001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("40"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7201L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType("PRODUCTION")
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .reviewSignatureId(1201L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signedAt\":\"2026-08-01T09:00:00\"}")
                .build();
    }

    private static MesProEdhrBatchExecutionTaskDO batchTask() {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(BATCH_TASK_ID)
                .batchExecutionId(BATCH_EXECUTION_ID)
                .nodeType("ROUTE_FORM")
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .formSlotType(null)
                .routeBindingId(3001L)
                .build();
    }

    private static MesProRouteFlowProcessBatchRecordDO binding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(3001L)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .useType("BATCH")
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .formSlotType("MAIN")
                .permissionScopeId(9901L)
                .build();
    }
}
