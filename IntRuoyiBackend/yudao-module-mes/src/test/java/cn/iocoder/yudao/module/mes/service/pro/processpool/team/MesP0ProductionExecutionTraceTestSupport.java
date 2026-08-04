package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class MesP0ProductionExecutionTraceTestSupport {

    static final Long EVENT_ID = 1001L;
    static final Long WORK_ORDER_ID = 9001L;
    static final Long ROUTE_PROCESS_ID = 5001L;
    static final Long PROCESS_ID = 6001L;

    private MesP0ProductionExecutionTraceTestSupport() {
    }

    static Map<String, MesProductionExecutionTraceRespVO.Section> sections(
            MesProductionExecutionTraceRespVO trace) {
        return trace.getSections().stream()
                .collect(Collectors.toMap(MesProductionExecutionTraceRespVO.Section::getSectionKey,
                        Function.identity()));
    }

    static MesProProcessPoolEventDO submitEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(EVENT_ID)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(4001L)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2001L)
                .deviceAccountId(3001L)
                .deviceId(4001L)
                .workstationId(5001L)
                .feedbackSourceId(101L)
                .recordbookEntryId(201L)
                .recordbookSourceId(202L)
                .signatureId(901L)
                .signatureUserId(2001L)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 0))
                .rawPayload("{\"outputQuantity\":80}")
                .build();
    }

    static MesProProcessPoolEventDO pqcEvent(Long id, String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .deviceAccountId(3001L)
                .deviceId(4001L)
                .workstationId(5001L)
                .feedbackSourceId(6101L)
                .signatureId(902L)
                .signatureUserId(2101L)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 5))
                .rawPayload(rawPayload)
                .build();
    }

    static MesProcessPoolSubmissionReviewDO review(Long id, Long signatureId, Long signatureUserId) {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(id)
                .eventId(EVENT_ID)
                .leaderUserId(3001L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewSignatureId(signatureId)
                .reviewSignatureUserId(signatureUserId)
                .reviewSignatureSnapshotJson(signatureId == null ? null : "{\"signature\":\"review\"}")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 9, 10))
                .build();
    }

    static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(EVENT_ID)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 3, 9, 15))
                .build();
    }

    static MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(8301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(8801L)
                .lastEventId(EVENT_ID)
                .lastReviewId(7001L)
                .completedAt(LocalDateTime.of(2026, 8, 3, 9, 20))
                .build();
    }

    static MesProBatchRecordExecutionDO execution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .fieldAuditLastBatchId(9901L)
                .build();
    }

    static MesProBatchRecordExecutionFieldAuditItemDO auditItem() {
        return MesProBatchRecordExecutionFieldAuditItemDO.builder()
                .id(99011L)
                .auditBatchId(9901L)
                .executionId(8801L)
                .fieldPath("report.pressure")
                .fieldKey("pressure")
                .rowIndex(6)
                .columnIndex(2)
                .newValueDisplay("15")
                .build();
    }
}
