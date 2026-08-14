package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_COMPLETION_TRACE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TRACE_REQUIRED;

@Service
public class MesTeamLeaderTraceServiceImpl implements MesTeamLeaderTraceService {

    private static final String PRODUCTION_TRACE_PATH =
            "/admin-api/mes/pro/process-pool/team-leader/production-execution/trace";
    private static final String ALLOCATION_TRACE_PATH =
            "/admin-api/mes/pro/process-pool/team-leader/submission/allocation/trace";
    private static final String ORDER_PROCESS_TRACE_PATH =
            "/admin-api/mes/pro/process-pool/team-leader/order-process/trace";
    private static final String BATCH_RECORD_TRACE_PATH =
            "/admin-api/mes/pro/process-pool/team-leader/batch-record/trace";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    public MesTeamLeaderTraceServiceImpl(MesProProcessPoolEventMapper eventMapper,
                                         MesProcessPoolSubmissionReviewMapper reviewMapper,
                                         MesProcessPoolReportAllocationMapper allocationMapper,
                                         MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                         MesProBatchRecordExecutionMapper executionMapper,
                                         MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper,
                                         MesProProcessPoolPqcRecordMapper pqcRecordMapper) {
        this.eventMapper = eventMapper;
        this.reviewMapper = reviewMapper;
        this.allocationMapper = allocationMapper;
        this.completionMapper = completionMapper;
        this.executionMapper = executionMapper;
        this.auditItemMapper = auditItemMapper;
        this.pqcRecordMapper = pqcRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public MesProductionExecutionTraceRespVO getProductionExecutionTrace(Long eventId) {
        requireContext(eventId, "productionExecutionTrace");
        MesProProcessPoolEventDO submitEvent = eventMapper.selectById(eventId);
        if (submitEvent == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionExecutionTrace.event");
        }

        List<MesProductionExecutionTraceRespVO.Section> sections = new ArrayList<>();
        sections.add(buildSubmitSection(submitEvent));
        List<MesProProcessPoolEventDO> pqcEvents = eventMapper.selectPqcEventsForSubmit(submitEvent);
        sections.add(buildQualitySection(pqcEvents, eventId));
        List<MesProcessPoolSubmissionReviewDO> reviews = reviewMapper.selectListByEventId(eventId);
        sections.add(buildReviewSection(reviews, eventId));
        List<MesProcessPoolReportAllocationDO> allocations = allocationMapper.selectListByEventId(eventId);
        sections.add(buildAllocationSection(allocations, submitEvent));
        MesProcessPoolOrderProcessCompletionDO completion = completionMapper.selectByWorkOrderAndProcess(
                submitEvent.getWorkOrderId(), submitEvent.getRouteProcessId(), submitEvent.getProcessId());
        sections.add(buildCompletionSection(completion, submitEvent));
        sections.add(buildBatchRecordSection(completion, allocations, submitEvent));

        MesProductionExecutionTraceRespVO.ClosureEvidence closureEvidence =
                buildClosureEvidence(eventId, sections);
        List<MesProductionExecutionTraceRespVO.Blocker> blockers = new ArrayList<>(sections.stream()
                .flatMap(section -> section.getBlockers().stream())
                .toList());
        boolean sectionsComplete = sections.stream().allMatch(section -> "COMPLETE".equals(section.getStatus()))
                && blockers.isEmpty();
        if (sectionsComplete) {
            blockers.addAll(closureEvidence.getBlockers());
        }
        boolean complete = sectionsComplete && Boolean.TRUE.equals(closureEvidence.getComplete()) && blockers.isEmpty();
        return new MesProductionExecutionTraceRespVO()
                .setProcessPoolEventId(eventId)
                .setComplete(complete)
                .setSections(sections)
                .setBlockers(blockers)
                .setCandidateEvents(toCandidateEvents(pqcEvents))
                .setClosureEvidence(closureEvidence)
                .setLastUpdatedAt(sections.stream()
                        .map(MesProductionExecutionTraceRespVO.Section::getLastUpdatedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderAllocationTraceRespVO getAllocationTrace(Long eventId, Long workOrderId,
                                                                 Long routeProcessId, Long processId) {
        requireContext(eventId, workOrderId, routeProcessId, processId, "allocationTrace");
        List<MesProcessPoolReportAllocationDO> allocations =
                allocationMapper.selectListByTrace(eventId, workOrderId, routeProcessId, processId);
        if (allocations.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TRACE_REQUIRED,
                    eventId, workOrderId, routeProcessId, processId);
        }
        BigDecimal total = allocations.stream()
                .map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MesTeamLeaderAllocationTraceRespVO()
                .setEventId(eventId)
                .setWorkOrderId(workOrderId)
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setTotalAllocatedQuantity(total)
                .setLines(allocations.stream()
                        .map(MesTeamLeaderTraceServiceImpl::toAllocationLine)
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderOrderProcessTraceRespVO getOrderProcessTrace(Long workOrderId,
                                                                     Long routeProcessId, Long processId) {
        MesProcessPoolOrderProcessCompletionDO completion =
                requireCompletion(workOrderId, routeProcessId, processId);
        return toOrderProcessTrace(completion);
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderBatchRecordTraceRespVO getBatchRecordTrace(Long workOrderId,
                                                                   Long routeProcessId, Long processId) {
        MesProcessPoolOrderProcessCompletionDO completion =
                requireCompletion(workOrderId, routeProcessId, processId);
        if (completion.getBackfillExecutionId() == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(completion.getBackfillExecutionId());
        if (execution == null || execution.getFieldAuditLastBatchId() == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        List<MesProBatchRecordExecutionFieldAuditItemDO> items =
                auditItemMapper.selectListByBatchId(execution.getFieldAuditLastBatchId());
        if (items.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        return new MesTeamLeaderBatchRecordTraceRespVO()
                .setWorkOrderId(workOrderId)
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setBatchRecordDefinitionId(execution.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(execution.getBatchRecordVersionId())
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setFieldAuditLastBatchId(execution.getFieldAuditLastBatchId())
                .setCellValuesJson(execution.getCellValuesJson())
                .setCells(items.stream()
                        .map(MesTeamLeaderTraceServiceImpl::toCell)
                        .toList());
    }

    private MesProcessPoolOrderProcessCompletionDO requireCompletion(Long workOrderId,
                                                                     Long routeProcessId, Long processId) {
        requireContext(workOrderId, routeProcessId, processId, "orderProcessTrace");
        MesProcessPoolOrderProcessCompletionDO completion =
                completionMapper.selectByWorkOrderAndProcess(workOrderId, routeProcessId, processId);
        if (completion == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_COMPLETION_TRACE_REQUIRED,
                    workOrderId, routeProcessId, processId);
        }
        return completion;
    }

    private static void requireContext(Object first, Object second, Object third, String context) {
        if (first == null || second == null || third == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, context);
        }
    }

    private static void requireContext(Object first, Object second, Object third, Object fourth, String context) {
        if (first == null || second == null || third == null || fourth == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, context);
        }
    }

    private static void requireContext(Object first, String context) {
        if (first == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, context);
        }
    }

    private static MesProductionExecutionTraceRespVO.Section buildSubmitSection(
            MesProProcessPoolEventDO event) {
        MesProductionExecutionTraceRespVO.Section section = section("submitEvent", event.getServerSubmitTime());
        put(section, "processPoolEventId", event.getId());
        put(section, "eventType", event.getEventType());
        put(section, "feedbackId", event.getFeedbackSourceId());
        put(section, "recordbookEntryId", event.getRecordbookEntryId());
        put(section, "recordbookEventId", event.getRecordbookSourceId());
        put(section, "actualEmployeeId", event.getActualEmployeeId());
        put(section, "deviceAccountId", event.getDeviceAccountId());
        put(section, "deviceId", event.getDeviceId());
        put(section, "workstationId", event.getWorkstationId());
        put(section, "submitSignatureId", event.getSignatureId());
        put(section, "submitSignatureUserId", event.getSignatureUserId());
        put(section, "workOrderId", event.getWorkOrderId());
        put(section, "routeProcessId", event.getRouteProcessId());
        put(section, "processId", event.getProcessId());
        if (event.getFeedbackSourceId() == null || event.getRecordbookEntryId() == null
                || event.getRecordbookSourceId() == null
                || event.getActualEmployeeId() == null || event.getDeviceAccountId() == null
                || event.getDeviceId() == null || event.getWorkstationId() == null
                || event.getSignatureId() == null) {
            block(section, "SUBMIT_EVENT_MISSING", "提交事件缺少正式报工、记录本、员工、设备或签名来源",
                    "submitEvent", "补齐正式生产提交事件来源 ID");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private MesProductionExecutionTraceRespVO.Section buildQualitySection(
            List<MesProProcessPoolEventDO> pqcEvents, Long submitEventId) {
        MesProductionExecutionTraceRespVO.Section section = section("quality", null);
        if (pqcEvents == null || pqcEvents.isEmpty()) {
            block(section, "PQC_EVENT_MISSING", "缺少正式 PQC 工序池事件", "pqcEvent",
                    "完成 PQC 提交并绑定 PQC_INSPECTION 事件");
            return section;
        }
        if (pqcEvents.size() > 1) {
            block(section, "PQC_BINDING_AMBIGUOUS", "存在多条 PQC 候选事件，无法唯一绑定当前提交", "pqcEvent",
                    "使用正式结构化 ID 绑定目标提交事件");
            return section;
        }
        MesProProcessPoolEventDO pqcEvent = pqcEvents.get(0);
        put(section, "pqcEventId", pqcEvent.getId());
        put(section, "pqcTaskId", pqcEvent.getFeedbackSourceId());
        put(section, "pqcSignatureId", pqcEvent.getSignatureId());
        section.setLastUpdatedAt(pqcEvent.getServerSubmitTime());
        MesProProcessPoolPqcRecordDO pqcRecord = pqcRecordMapper.selectByEventId(pqcEvent.getId());
        if (pqcRecord == null) {
            block(section, "PQC_BINDING_MISSING", "PQC 事件缺少正式结构化过程检验记录",
                    "pqcRecord", "重新提交带正式生产提交事件 ID 的 PQC 结果");
            return section;
        }
        put(section, "pqcRecordId", pqcRecord.getId());
        Long productionSubmitEventId = pqcRecord.getProductionSubmitEventId();
        put(section, "productionSubmitEventId", productionSubmitEventId);
        put(section, "inspectionResult", pqcRecord.getInspectionResult());
        if (!Objects.equals(productionSubmitEventId, submitEventId)) {
            block(section, "PQC_BINDING_MISSING", "PQC 事件缺少指向当前生产提交的正式绑定 ID",
                    "productionSubmitEventId", "使用正式结构化 ID 绑定目标提交事件");
        }
        if (!MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(pqcRecord.getInspectionResult())) {
            block(section, "QUALITY_NOT_ALLOCATABLE", "PQC 结果不是可分配成功状态", "inspectionResult",
                    "完成正式合格 PQC 检验后再进入 FIFO");
        }
        if (pqcEvent.getFeedbackSourceId() == null || pqcEvent.getSignatureId() == null) {
            block(section, "PQC_EVENT_MISSING", "PQC 事件缺少任务或签名来源", "pqcEvent",
                    "补齐 PQC 任务、逐件明细和签名来源");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private static MesProductionExecutionTraceRespVO.Section buildReviewSection(
            List<MesProcessPoolSubmissionReviewDO> reviews, Long eventId) {
        MesProductionExecutionTraceRespVO.Section section = section("review", null);
        if (reviews == null || reviews.isEmpty()) {
            block(section, "REVIEW_MISSING", "缺少班组长复核记录", "review",
                    "由负责班组长完成带电子签名的复核");
            return section;
        }
        MesProcessPoolSubmissionReviewDO review = reviews.get(0);
        put(section, "reviewIds", reviews.stream().map(MesProcessPoolSubmissionReviewDO::getId).toList());
        put(section, "reviewId", review.getId());
        put(section, "reviewStatus", review.getReviewStatus());
        put(section, "reviewerUserId", review.getLeaderUserId());
        put(section, "reviewSignatureId", review.getReviewSignatureId());
        put(section, "reviewSignatureUserId", review.getReviewSignatureUserId());
        put(section, "reviewSourceProcessPoolEventId", review.getEventId());
        section.setLastUpdatedAt(reviews.stream()
                .map(MesProcessPoolSubmissionReviewDO::getReviewedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        boolean hasUnsignedReview = reviews.stream()
                .anyMatch(item -> item.getReviewSignatureId() == null || item.getReviewSignatureUserId() == null
                        || item.getReviewSignatureSnapshotJson() == null || item.getReviewSignatureSnapshotJson().isBlank());
        if (hasUnsignedReview) {
            block(section, "REVIEW_SIGNATURE_MISSING", "复核记录缺少正式电子签名", "reviewSignature",
                    "重新提交带签名的复核记录");
        }
        boolean hasDetachedReview = reviews.stream()
                .anyMatch(item -> item.getEventId() == null || !Objects.equals(item.getEventId(), eventId));
        if (hasDetachedReview) {
            block(section, "REVIEW_SOURCE_MISSING", "复核记录缺少当前提交事件来源", "review",
                    "补齐复核来源事件 ID");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private static MesProductionExecutionTraceRespVO.Section buildAllocationSection(
            List<MesProcessPoolReportAllocationDO> allocations, MesProProcessPoolEventDO submitEvent) {
        MesProductionExecutionTraceRespVO.Section section = section("allocation", null);
        if (allocations == null || allocations.isEmpty()) {
            block(section, "ALLOCATION_MISSING", "缺少 FIFO 或手工分配记录", "allocation",
                    "班组长复核后确认活跃生产工单分配");
            return section;
        }
        MesProcessPoolReportAllocationDO allocation = allocations.get(0);
        put(section, "allocationId", allocation.getId());
        put(section, "activeOrderId", allocation.getActiveOrderId());
        put(section, "targetWorkOrderId", allocation.getWorkOrderId());
        put(section, "routeProcessId", allocation.getRouteProcessId());
        put(section, "processId", allocation.getProcessId());
        put(section, "allocatedQuantity", allocation.getAllocatedQuantity());
        put(section, "sourceReviewId", allocation.getReviewId());
        put(section, "sourceProcessPoolEventId", allocation.getEventId());
        section.setLastUpdatedAt(allocation.getConfirmedAt());
        if (allocation.getReviewId() == null || allocation.getActiveOrderId() == null
                || allocation.getWorkOrderId() == null) {
            block(section, "ALLOCATION_SOURCE_MISSING", "分配记录缺少复核、活跃订单或目标工单来源",
                    "allocation", "补齐正式分配来源 ID");
        }
        boolean hasDetachedAllocation = allocations.stream()
                .anyMatch(item -> item.getEventId() == null || !Objects.equals(item.getEventId(), submitEvent.getId()));
        if (hasDetachedAllocation) {
            block(section, "ALLOCATION_SOURCE_MISSING", "分配记录来源事件不是当前生产提交根事件",
                    "sourceProcessPoolEventId", "只允许当前生产提交事件的正式分配进入 trace");
        }
        boolean hasScopeMismatch = allocations.stream()
                .anyMatch(item -> !sameWorkOrderProcess(item.getWorkOrderId(), item.getRouteProcessId(),
                        item.getProcessId(), submitEvent));
        if (hasScopeMismatch) {
            block(section, "ALLOCATION_SCOPE_MISMATCH", "分配记录不属于当前生产工单、路线工序或 MES 工序",
                    "allocationScope", "只允许同工单、同路线工序、同 MES 工序的分配进入 trace");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private static MesProductionExecutionTraceRespVO.Section buildCompletionSection(
            MesProcessPoolOrderProcessCompletionDO completion, MesProProcessPoolEventDO submitEvent) {
        Long eventId = submitEvent.getId();
        MesProductionExecutionTraceRespVO.Section section = section("completion", null);
        if (completion == null
                || !MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())) {
            block(section, "ORDER_PROCESS_NOT_COMPLETE", "订单工序尚未完成", "orderProcessCompletion",
                    "确认分配数量达到目标后生成订单工序完成记录");
            return section;
        }
        put(section, "orderProcessCompletionId", completion.getId());
        put(section, "targetWorkOrderId", completion.getWorkOrderId());
        put(section, "routeProcessId", completion.getRouteProcessId());
        put(section, "processId", completion.getProcessId());
        put(section, "targetQuantity", completion.getTargetQuantity());
        put(section, "confirmedQuantity", completion.getConfirmedQuantity());
        put(section, "lastSourceProcessPoolEventId", completion.getLastEventId());
        put(section, "lastReviewId", completion.getLastReviewId());
        section.setLastUpdatedAt(completion.getCompletedAt());
        if (completion.getId() == null || completion.getLastEventId() == null || completion.getLastReviewId() == null) {
            block(section, "COMPLETION_SOURCE_MISSING", "订单工序完成记录缺少来源事件或复核", "completion",
                    "补齐完成记录来源 ID");
        }
        if (completion.getLastEventId() != null && !Objects.equals(completion.getLastEventId(), eventId)) {
            block(section, "COMPLETION_SOURCE_MISSING", "订单工序完成来源事件不是当前生产提交根事件",
                    "lastSourceProcessPoolEventId", "只允许当前生产提交事件驱动的完工事实进入 trace");
        }
        if (!sameWorkOrderProcess(completion.getWorkOrderId(), completion.getRouteProcessId(),
                completion.getProcessId(), submitEvent)) {
            block(section, "COMPLETION_SCOPE_MISMATCH", "订单工序完成记录不属于当前生产工单、路线工序或 MES 工序",
                    "completionScope", "只允许同工单、同路线工序、同 MES 工序的完工事实进入 trace");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private MesProductionExecutionTraceRespVO.Section buildBatchRecordSection(
            MesProcessPoolOrderProcessCompletionDO completion, List<MesProcessPoolReportAllocationDO> allocations,
            MesProProcessPoolEventDO submitEvent) {
        Long eventId = submitEvent.getId();
        MesProductionExecutionTraceRespVO.Section section = section("batchRecord", null);
        if (completion == null || completion.getBackfillExecutionId() == null) {
            block(section, "FIELD_AUDIT_MISSING", "缺少批记录执行或字段审计投影", "fieldAudit",
                    "完成正式批记录回填并保存字段审计");
            return section;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(completion.getBackfillExecutionId());
        if (execution == null || execution.getFieldAuditLastBatchId() == null) {
            block(section, "FIELD_AUDIT_MISSING", "批记录执行缺少字段审计 batch", "fieldAudit",
                    "按 PROCESS_POOL_REPORT 映射写入字段审计");
            return section;
        }
        List<MesProBatchRecordExecutionFieldAuditItemDO> items =
                auditItemMapper.selectListByBatchId(execution.getFieldAuditLastBatchId());
        if (items == null || items.isEmpty()) {
            block(section, "FIELD_AUDIT_MISSING", "批记录字段审计明细为空", "fieldAuditItem",
                    "写入正式字段审计明细");
            return section;
        }
        MesProBatchRecordExecutionFieldAuditItemDO firstItem = items.get(0);
        Long sourceAllocationId = firstAllocationId(allocations, submitEvent);
        put(section, "batchRecordExecutionId", execution.getId());
        put(section, "batchRecordReportId", execution.getBatchRecordReportId());
        put(section, "batchRecordDefinitionId", execution.getBatchRecordDefinitionId());
        put(section, "batchRecordVersionId", execution.getBatchRecordVersionId());
        put(section, "fieldAuditBatchId", execution.getFieldAuditLastBatchId());
        put(section, "fieldAuditItemId", firstItem.getId());
        put(section, "fieldAuditFieldKey", firstItem.getFieldKey());
        put(section, "fieldAuditValueDisplay", firstItem.getNewValueDisplay());
        put(section, "sourceProcessPoolEventId", completion.getLastEventId());
        put(section, "sourceAllocationId", sourceAllocationId);
        section.setLastUpdatedAt(completion.getCompletedAt());
        if (completion.getLastEventId() == null || !Objects.equals(completion.getLastEventId(), eventId)) {
            block(section, "BATCH_RECORD_SOURCE_MISSING", "批记录追溯缺少来源生产提交事件 ID",
                    "sourceProcessPoolEventId", "补齐订单工序完成和批记录回填来源事件 ID");
        }
        if (!sameWorkOrderProcess(completion.getWorkOrderId(), completion.getRouteProcessId(),
                completion.getProcessId(), submitEvent)) {
            block(section, "BATCH_RECORD_SOURCE_MISSING", "批记录追溯来源不属于当前生产工单、路线工序或 MES 工序",
                    "batchRecordScope", "只允许同工单、同路线工序、同 MES 工序的批记录回填进入 trace");
        }
        if (sourceAllocationId == null) {
            block(section, "BATCH_RECORD_SOURCE_MISSING", "批记录追溯缺少来源分配 ID", "sourceAllocationId",
                    "补齐批记录回填来源分配记录");
        }
        if (!hasFormalFieldAuditItem(firstItem)) {
            block(section, "FIELD_AUDIT_MISSING", "批记录字段审计缺少字段路径或单元格位置", "fieldAuditItem",
                    "补齐字段路径、行列坐标和字段审计明细");
        }
        completeIfNoBlocker(section);
        return section;
    }

    private static MesProductionExecutionTraceRespVO.ClosureEvidence buildClosureEvidence(
            Long eventId, List<MesProductionExecutionTraceRespVO.Section> sections) {
        Map<String, MesProductionExecutionTraceRespVO.Section> byKey = sectionMap(sections);
        List<MesProductionExecutionTraceRespVO.SameSourceCheck> sameSourceChecks =
                buildSameSourceChecks(eventId, byKey);
        boolean sameSourcePassed = sameSourceChecks.stream()
                .allMatch(check -> Boolean.TRUE.equals(check.getPassed()));
        MesProductionExecutionTraceRespVO.ClosureEvidence evidence =
                new MesProductionExecutionTraceRespVO.ClosureEvidence()
                        .setProcessPoolEventId(eventId)
                        .setSameSourceChecks(sameSourceChecks);
        sameSourceChecks.stream()
                .filter(check -> !Boolean.TRUE.equals(check.getPassed()))
                .forEach(check -> evidence.getBlockers().add(blocker("CLOSURE_EVIDENCE_SAME_SOURCE_FAILED",
                        "闭环证据同源校验失败：" + check.getCheckKey(), check.getCheckKey(),
                        "只允许同一生产提交、生产工单、路线工序和 MES 工序的正式事实进入证据包")));

        addAnswer(evidence, answer("who", "submitEvent",
                values("actualEmployeeId", source(byKey, "submitEvent", "actualEmployeeId"),
                        "deviceAccountId", source(byKey, "submitEvent", "deviceAccountId"),
                        "submitSignatureUserId", source(byKey, "submitEvent", "submitSignatureUserId")),
                values("processPoolEventId", eventId,
                        "actualEmployeeId", source(byKey, "submitEvent", "actualEmployeeId"),
                        "deviceAccountId", source(byKey, "submitEvent", "deviceAccountId"),
                        "submitSignatureUserId", source(byKey, "submitEvent", "submitSignatureUserId")),
                List.of("processPoolEventId", "actualEmployeeId", "deviceAccountId", "submitSignatureUserId"),
                sameSourcePassed, productionTraceVerification(eventId)));
        addAnswer(evidence, answer("device", "submitEvent,quality",
                values("deviceAccountId", source(byKey, "submitEvent", "deviceAccountId"),
                        "deviceId", source(byKey, "submitEvent", "deviceId"),
                        "workstationId", source(byKey, "submitEvent", "workstationId")),
                values("processPoolEventId", eventId,
                        "deviceAccountId", source(byKey, "submitEvent", "deviceAccountId"),
                        "deviceId", source(byKey, "submitEvent", "deviceId"),
                        "workstationId", source(byKey, "submitEvent", "workstationId"),
                        "pqcEventId", source(byKey, "quality", "pqcEventId")),
                List.of("processPoolEventId", "deviceAccountId", "deviceId", "workstationId", "pqcEventId"),
                sameSourcePassed, productionTraceVerification(eventId)));
        addAnswer(evidence, answer("process", "submitEvent,completion",
                values("workOrderId", source(byKey, "submitEvent", "workOrderId"),
                        "routeProcessId", source(byKey, "submitEvent", "routeProcessId"),
                        "processId", source(byKey, "submitEvent", "processId")),
                values("processPoolEventId", eventId,
                        "workOrderId", source(byKey, "submitEvent", "workOrderId"),
                        "routeProcessId", source(byKey, "submitEvent", "routeProcessId"),
                        "processId", source(byKey, "submitEvent", "processId"),
                        "orderProcessCompletionId", source(byKey, "completion", "orderProcessCompletionId")),
                List.of("processPoolEventId", "workOrderId", "routeProcessId", "processId",
                        "orderProcessCompletionId"),
                sameSourcePassed, orderProcessVerification(byKey)));
        addAnswer(evidence, answer("quantity", "submitEvent,quality,allocation,completion,batchRecord",
                values("allocatedQuantity", source(byKey, "allocation", "allocatedQuantity"),
                        "confirmedQuantity", source(byKey, "completion", "confirmedQuantity"),
                        "fieldAuditValueDisplay", source(byKey, "batchRecord", "fieldAuditValueDisplay")),
                values("processPoolEventId", eventId,
                        "allocationId", source(byKey, "allocation", "allocationId"),
                        "allocatedQuantity", source(byKey, "allocation", "allocatedQuantity"),
                        "orderProcessCompletionId", source(byKey, "completion", "orderProcessCompletionId"),
                        "confirmedQuantity", source(byKey, "completion", "confirmedQuantity"),
                        "fieldAuditItemId", source(byKey, "batchRecord", "fieldAuditItemId"),
                        "fieldAuditValueDisplay", source(byKey, "batchRecord", "fieldAuditValueDisplay")),
                List.of("processPoolEventId", "allocationId", "allocatedQuantity", "orderProcessCompletionId",
                        "confirmedQuantity", "fieldAuditItemId"),
                sameSourcePassed, allocationAndBatchRecordVerification(eventId, byKey)));
        addAnswer(evidence, answer("quality", "quality",
                source(byKey, "quality", "inspectionResult"),
                values("processPoolEventId", eventId,
                        "pqcEventId", source(byKey, "quality", "pqcEventId"),
                        "pqcTaskId", source(byKey, "quality", "pqcTaskId"),
                        "pqcRecordId", source(byKey, "quality", "pqcRecordId"),
                        "productionSubmitEventId", source(byKey, "quality", "productionSubmitEventId"),
                        "inspectionResult", source(byKey, "quality", "inspectionResult")),
                List.of("processPoolEventId", "pqcEventId", "pqcTaskId", "pqcRecordId",
                        "productionSubmitEventId", "inspectionResult"),
                sameSourcePassed, productionTraceVerification(eventId)));
        addAnswer(evidence, answer("signature", "submitEvent,quality,review",
                values("submitSignatureId", source(byKey, "submitEvent", "submitSignatureId"),
                        "pqcSignatureId", source(byKey, "quality", "pqcSignatureId"),
                        "reviewSignatureId", source(byKey, "review", "reviewSignatureId")),
                values("processPoolEventId", eventId,
                        "submitSignatureId", source(byKey, "submitEvent", "submitSignatureId"),
                        "submitSignatureUserId", source(byKey, "submitEvent", "submitSignatureUserId"),
                        "pqcSignatureId", source(byKey, "quality", "pqcSignatureId"),
                        "reviewSignatureId", source(byKey, "review", "reviewSignatureId"),
                        "reviewSignatureUserId", source(byKey, "review", "reviewSignatureUserId")),
                List.of("processPoolEventId", "submitSignatureId", "submitSignatureUserId", "pqcSignatureId",
                        "reviewSignatureId", "reviewSignatureUserId"),
                sameSourcePassed, productionTraceVerification(eventId)));
        addAnswer(evidence, answer("workOrder", "allocation,completion",
                values("activeOrderId", source(byKey, "allocation", "activeOrderId"),
                        "targetWorkOrderId", source(byKey, "allocation", "targetWorkOrderId")),
                values("processPoolEventId", eventId,
                        "activeOrderId", source(byKey, "allocation", "activeOrderId"),
                        "targetWorkOrderId", source(byKey, "allocation", "targetWorkOrderId"),
                        "sourceReviewId", source(byKey, "allocation", "sourceReviewId"),
                        "sourceProcessPoolEventId", source(byKey, "allocation", "sourceProcessPoolEventId"),
                        "orderProcessCompletionId", source(byKey, "completion", "orderProcessCompletionId")),
                List.of("processPoolEventId", "activeOrderId", "targetWorkOrderId", "sourceReviewId",
                        "sourceProcessPoolEventId", "orderProcessCompletionId"),
                sameSourcePassed, allocationAndOrderVerification(eventId, byKey)));
        addAnswer(evidence, answer("review", "review",
                values("reviewStatus", source(byKey, "review", "reviewStatus"),
                        "reviewerUserId", source(byKey, "review", "reviewerUserId")),
                values("processPoolEventId", eventId,
                        "reviewId", source(byKey, "review", "reviewId"),
                        "reviewerUserId", source(byKey, "review", "reviewerUserId"),
                        "reviewSignatureId", source(byKey, "review", "reviewSignatureId"),
                        "reviewSourceProcessPoolEventId", source(byKey, "review",
                                "reviewSourceProcessPoolEventId")),
                List.of("processPoolEventId", "reviewId", "reviewerUserId", "reviewSignatureId",
                        "reviewSourceProcessPoolEventId"),
                sameSourcePassed, productionTraceVerification(eventId)));
        addAnswer(evidence, answer("batchRecord", "batchRecord",
                values("batchRecordExecutionId", source(byKey, "batchRecord", "batchRecordExecutionId"),
                        "batchRecordReportId", source(byKey, "batchRecord", "batchRecordReportId"),
                        "fieldAuditItemId", source(byKey, "batchRecord", "fieldAuditItemId")),
                values("processPoolEventId", eventId,
                        "batchRecordExecutionId", source(byKey, "batchRecord", "batchRecordExecutionId"),
                        "batchRecordReportId", source(byKey, "batchRecord", "batchRecordReportId"),
                        "batchRecordDefinitionId", source(byKey, "batchRecord", "batchRecordDefinitionId"),
                        "batchRecordVersionId", source(byKey, "batchRecord", "batchRecordVersionId"),
                        "fieldAuditBatchId", source(byKey, "batchRecord", "fieldAuditBatchId"),
                        "fieldAuditItemId", source(byKey, "batchRecord", "fieldAuditItemId"),
                        "sourceProcessPoolEventId", source(byKey, "batchRecord", "sourceProcessPoolEventId"),
                        "sourceAllocationId", source(byKey, "batchRecord", "sourceAllocationId")),
                List.of("processPoolEventId", "batchRecordExecutionId", "batchRecordReportId",
                        "batchRecordDefinitionId", "batchRecordVersionId", "fieldAuditBatchId",
                        "fieldAuditItemId", "sourceProcessPoolEventId", "sourceAllocationId"),
                sameSourcePassed, batchRecordVerification(byKey)));
        evidence.setComplete(evidence.getAnswers().size() == 9 && evidence.getBlockers().isEmpty());
        return evidence;
    }

    private static List<MesProductionExecutionTraceRespVO.SameSourceCheck> buildSameSourceChecks(
            Long eventId, Map<String, MesProductionExecutionTraceRespVO.Section> byKey) {
        return List.of(
                sameSourceCheck("processPoolEventChain", values(
                                "processPoolEventId", eventId,
                                "qualityProductionSubmitEventId", source(byKey, "quality",
                                        "productionSubmitEventId"),
                                "reviewSourceProcessPoolEventId", source(byKey, "review",
                                        "reviewSourceProcessPoolEventId"),
                                "allocationSourceProcessPoolEventId", source(byKey, "allocation",
                                        "sourceProcessPoolEventId"),
                                "completionLastSourceProcessPoolEventId", source(byKey, "completion",
                                        "lastSourceProcessPoolEventId"),
                                "batchRecordSourceProcessPoolEventId", source(byKey, "batchRecord",
                                        "sourceProcessPoolEventId")),
                        allEqual(eventId,
                                source(byKey, "quality", "productionSubmitEventId"),
                                source(byKey, "review", "reviewSourceProcessPoolEventId"),
                                source(byKey, "allocation", "sourceProcessPoolEventId"),
                                source(byKey, "completion", "lastSourceProcessPoolEventId"),
                                source(byKey, "batchRecord", "sourceProcessPoolEventId"))),
                sameSourceCheck("workOrder", values(
                                "submitWorkOrderId", source(byKey, "submitEvent", "workOrderId"),
                                "allocationTargetWorkOrderId", source(byKey, "allocation",
                                        "targetWorkOrderId"),
                                "completionTargetWorkOrderId", source(byKey, "completion",
                                        "targetWorkOrderId")),
                        allEqual(source(byKey, "submitEvent", "workOrderId"),
                                source(byKey, "allocation", "targetWorkOrderId"),
                                source(byKey, "completion", "targetWorkOrderId"))),
                sameSourceCheck("routeProcessAndProcess", values(
                                "submitRouteProcessId", source(byKey, "submitEvent", "routeProcessId"),
                                "allocationRouteProcessId", source(byKey, "allocation", "routeProcessId"),
                                "completionRouteProcessId", source(byKey, "completion", "routeProcessId"),
                                "submitProcessId", source(byKey, "submitEvent", "processId"),
                                "allocationProcessId", source(byKey, "allocation", "processId"),
                                "completionProcessId", source(byKey, "completion", "processId")),
                        allEqual(source(byKey, "submitEvent", "routeProcessId"),
                                source(byKey, "allocation", "routeProcessId"),
                                source(byKey, "completion", "routeProcessId"))
                                && allEqual(source(byKey, "submitEvent", "processId"),
                                source(byKey, "allocation", "processId"),
                                source(byKey, "completion", "processId"))));
    }

    private static MesProductionExecutionTraceRespVO.EvidenceAnswer answer(
            String answerKey, String section, Object value, Map<String, Object> sourceIds,
            List<String> requiredSourceKeys, boolean sameSourcePassed,
            List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> verificationEntries) {
        MesProductionExecutionTraceRespVO.EvidenceAnswer answer =
                new MesProductionExecutionTraceRespVO.EvidenceAnswer()
                        .setAnswerKey(answerKey)
                        .setValue(value)
                        .setSection(section)
                        .setSourceIds(sourceIds)
                        .setReadOnlyVerificationEntries(verificationEntries);
        List<String> missingKeys = requiredSourceKeys.stream()
                .filter(key -> !sourceIds.containsKey(key))
                .toList();
        if (isEmptyValue(value) || !missingKeys.isEmpty()) {
            block(answer, "CLOSURE_EVIDENCE_MISSING_SOURCE",
                    "闭环证据缺少正式来源：" + missingKeys, answerKey,
                    "补齐该审计问题的业务值和正式 sourceIds");
        }
        if (!sameSourcePassed) {
            block(answer, "CLOSURE_EVIDENCE_SAME_SOURCE_FAILED",
                    "闭环证据未通过同源校验", answerKey,
                    "只允许同一生产提交链路的事实进入该答案");
        }
        if (verificationEntries == null || verificationEntries.isEmpty()) {
            block(answer, "CLOSURE_EVIDENCE_READONLY_VERIFY_MISSING",
                    "闭环证据缺少只读复验入口", answerKey,
                    "补齐对应 trace 只读核验 API 和参数");
        }
        answer.setSameSource(answer.getBlockers().isEmpty());
        return answer;
    }

    private static void addAnswer(MesProductionExecutionTraceRespVO.ClosureEvidence evidence,
                                  MesProductionExecutionTraceRespVO.EvidenceAnswer answer) {
        evidence.getAnswers().put(answer.getAnswerKey(), answer);
        evidence.getBlockers().addAll(answer.getBlockers());
    }

    private static Map<String, MesProductionExecutionTraceRespVO.Section> sectionMap(
            List<MesProductionExecutionTraceRespVO.Section> sections) {
        Map<String, MesProductionExecutionTraceRespVO.Section> byKey = new LinkedHashMap<>();
        for (MesProductionExecutionTraceRespVO.Section section : sections) {
            byKey.put(section.getSectionKey(), section);
        }
        return byKey;
    }

    private static Object source(Map<String, MesProductionExecutionTraceRespVO.Section> byKey,
                                 String sectionKey, String sourceKey) {
        MesProductionExecutionTraceRespVO.Section section = byKey.get(sectionKey);
        return section == null ? null : section.getSourceIds().get(sourceKey);
    }

    private static Map<String, Object> values(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                values.put(String.valueOf(keyValues[i]), value);
            }
        }
        return values;
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof Map<?, ?> map && map.isEmpty();
    }

    private static boolean allEqual(Object expected, Object... values) {
        if (expected == null) {
            return false;
        }
        for (Object value : values) {
            if (value == null || !Objects.equals(expected, value)) {
                return false;
            }
        }
        return true;
    }

    private static MesProductionExecutionTraceRespVO.SameSourceCheck sameSourceCheck(
            String checkKey, Map<String, Object> sourceIds, boolean passed) {
        return new MesProductionExecutionTraceRespVO.SameSourceCheck()
                .setCheckKey(checkKey)
                .setPassed(passed)
                .setSourceIds(sourceIds)
                .setMessage(passed ? "same source verified" : "same source mismatch or missing formal source");
    }

    private static List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> productionTraceVerification(
            Long eventId) {
        return List.of(verification("productionExecutionTrace", PRODUCTION_TRACE_PATH,
                values("processPoolEventId", eventId)));
    }

    private static List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> allocationAndBatchRecordVerification(
            Long eventId, Map<String, MesProductionExecutionTraceRespVO.Section> byKey) {
        List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> entries = new ArrayList<>();
        entries.add(verification("productionExecutionTrace", PRODUCTION_TRACE_PATH,
                values("processPoolEventId", eventId)));
        entries.addAll(allocationAndOrderVerification(eventId, byKey));
        entries.addAll(batchRecordVerification(byKey));
        return entries;
    }

    private static List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> allocationAndOrderVerification(
            Long eventId, Map<String, MesProductionExecutionTraceRespVO.Section> byKey) {
        List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> entries = new ArrayList<>();
        entries.add(verification("allocationTrace", ALLOCATION_TRACE_PATH,
                values("eventId", eventId,
                        "workOrderId", source(byKey, "allocation", "targetWorkOrderId"),
                        "routeProcessId", source(byKey, "allocation", "routeProcessId"),
                        "processId", source(byKey, "allocation", "processId"))));
        entries.addAll(orderProcessVerification(byKey));
        return entries;
    }

    private static List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> orderProcessVerification(
            Map<String, MesProductionExecutionTraceRespVO.Section> byKey) {
        return List.of(verification("orderProcessTrace", ORDER_PROCESS_TRACE_PATH,
                values("workOrderId", source(byKey, "completion", "targetWorkOrderId"),
                        "routeProcessId", source(byKey, "completion", "routeProcessId"),
                        "processId", source(byKey, "completion", "processId"))));
    }

    private static List<MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry> batchRecordVerification(
            Map<String, MesProductionExecutionTraceRespVO.Section> byKey) {
        return List.of(verification("batchRecordTrace", BATCH_RECORD_TRACE_PATH,
                values("workOrderId", source(byKey, "completion", "targetWorkOrderId"),
                        "routeProcessId", source(byKey, "completion", "routeProcessId"),
                        "processId", source(byKey, "completion", "processId"))));
    }

    private static MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry verification(
            String key, String path, Map<String, Object> params) {
        return new MesProductionExecutionTraceRespVO.ReadOnlyVerificationEntry()
                .setVerificationKey(key)
                .setMethod("GET")
                .setPath(path)
                .setParams(params);
    }

    private static MesProductionExecutionTraceRespVO.Blocker blocker(String code, String message,
                                                                     String missingObjectType, String resolution) {
        return new MesProductionExecutionTraceRespVO.Blocker()
                .setCode(code)
                .setMessage(message)
                .setMissingObjectType(missingObjectType)
                .setResolution(resolution);
    }

    private static void block(MesProductionExecutionTraceRespVO.EvidenceAnswer answer, String code, String message,
                              String missingObjectType, String resolution) {
        answer.getBlockers().add(blocker(code, message, missingObjectType, resolution));
    }

    private static List<MesProductionExecutionTraceRespVO.CandidateEvent> toCandidateEvents(
            List<MesProProcessPoolEventDO> pqcEvents) {
        if (pqcEvents == null || pqcEvents.size() <= 1) {
            return List.of();
        }
        return pqcEvents.stream()
                .map(event -> new MesProductionExecutionTraceRespVO.CandidateEvent()
                        .setProcessPoolEventId(event.getId())
                        .setEventType(event.getEventType())
                        .setActualEmployeeId(event.getActualEmployeeId())
                        .setServerSubmitTime(event.getServerSubmitTime())
                        .setStatus("AMBIGUOUS"))
                .toList();
    }

    private static Long firstAllocationId(List<MesProcessPoolReportAllocationDO> allocations,
                                          MesProProcessPoolEventDO submitEvent) {
        return Optional.ofNullable(allocations).orElse(List.of()).stream()
                .filter(allocation -> Objects.equals(allocation.getEventId(), submitEvent.getId()))
                .filter(allocation -> sameWorkOrderProcess(allocation.getWorkOrderId(), allocation.getRouteProcessId(),
                        allocation.getProcessId(), submitEvent))
                .map(MesProcessPoolReportAllocationDO::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static boolean sameWorkOrderProcess(Long workOrderId, Long routeProcessId, Long processId,
                                                MesProProcessPoolEventDO submitEvent) {
        return Objects.equals(workOrderId, submitEvent.getWorkOrderId())
                && Objects.equals(routeProcessId, submitEvent.getRouteProcessId())
                && Objects.equals(processId, submitEvent.getProcessId());
    }

    private static boolean hasFormalFieldAuditItem(MesProBatchRecordExecutionFieldAuditItemDO item) {
        return item.getId() != null
                && item.getAuditBatchId() != null
                && item.getExecutionId() != null
                && item.getFieldPath() != null && !item.getFieldPath().isBlank()
                && item.getRowIndex() != null
                && item.getColumnIndex() != null;
    }

    private static MesProductionExecutionTraceRespVO.Section section(String key, LocalDateTime lastUpdatedAt) {
        return new MesProductionExecutionTraceRespVO.Section()
                .setSectionKey(key)
                .setStatus("BLOCKED")
                .setSourceIds(new LinkedHashMap<>())
                .setBlockers(new ArrayList<>())
                .setLastUpdatedAt(lastUpdatedAt);
    }

    private static void completeIfNoBlocker(MesProductionExecutionTraceRespVO.Section section) {
        if (section.getBlockers().isEmpty()) {
            section.setStatus("COMPLETE");
        }
    }

    private static void put(MesProductionExecutionTraceRespVO.Section section, String key, Object value) {
        if (value != null) {
            section.getSourceIds().put(key, value);
        }
    }

    private static void block(MesProductionExecutionTraceRespVO.Section section, String code, String message,
                              String missingObjectType, String resolution) {
        section.getBlockers().add(new MesProductionExecutionTraceRespVO.Blocker()
                .setCode(code)
                .setMessage(message)
                .setMissingObjectType(missingObjectType)
                .setResolution(resolution));
        section.setStatus("BLOCKED");
    }

    private static MesTeamLeaderAllocationTraceRespVO.Line toAllocationLine(
            MesProcessPoolReportAllocationDO allocation) {
        return new MesTeamLeaderAllocationTraceRespVO.Line()
                .setAllocationId(allocation.getId())
                .setReviewId(allocation.getReviewId())
                .setLeaderUserId(allocation.getLeaderUserId())
                .setActiveOrderId(allocation.getActiveOrderId())
                .setWorkOrderId(allocation.getWorkOrderId())
                .setRouteProcessId(allocation.getRouteProcessId())
                .setProcessId(allocation.getProcessId())
                .setAllocatedQuantity(allocation.getAllocatedQuantity())
                .setAllocationMode(allocation.getAllocationMode())
                .setConfirmedAt(allocation.getConfirmedAt());
    }

    private static MesTeamLeaderOrderProcessTraceRespVO toOrderProcessTrace(
            MesProcessPoolOrderProcessCompletionDO completion) {
        return new MesTeamLeaderOrderProcessTraceRespVO()
                .setWorkOrderId(completion.getWorkOrderId())
                .setRouteProcessId(completion.getRouteProcessId())
                .setProcessId(completion.getProcessId())
                .setTargetQuantity(completion.getTargetQuantity())
                .setConfirmedQuantity(completion.getConfirmedQuantity())
                .setCompletionStatus(completion.getCompletionStatus())
                .setCompletedAt(completion.getCompletedAt())
                .setBackfillStatus(completion.getBackfillStatus())
                .setBackfillExecutionId(completion.getBackfillExecutionId())
                .setBackfillError(completion.getBackfillError())
                .setLastEventId(completion.getLastEventId())
                .setLastReviewId(completion.getLastReviewId());
    }

    private static MesTeamLeaderBatchRecordTraceRespVO.Cell toCell(
            MesProBatchRecordExecutionFieldAuditItemDO item) {
        return new MesTeamLeaderBatchRecordTraceRespVO.Cell()
                .setAuditItemId(item.getId())
                .setAuditBatchId(item.getAuditBatchId())
                .setExecutionId(item.getExecutionId())
                .setFieldAuditRevision(item.getFieldAuditRevision())
                .setFieldPath(item.getFieldPath())
                .setFieldKey(item.getFieldKey())
                .setRowIndex(item.getRowIndex())
                .setColumnIndex(item.getColumnIndex())
                .setValueType(item.getValueType())
                .setValueJson(item.getNewValueJson())
                .setValueDisplay(item.getNewValueDisplay());
    }
}
