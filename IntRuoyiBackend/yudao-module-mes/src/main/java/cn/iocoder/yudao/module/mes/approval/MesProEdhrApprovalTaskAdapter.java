package cn.iocoder.yudao.module.mes.approval;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MesProEdhrApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String SOURCE_TASK_TYPE = "EDHR_WORK_TASK";
    private static final String EDHR_WORK_TASK_ROUTE = "/mes/pro/feedback/edhr-work-task";
    private static final String EDHR_APPROVAL_DETAIL_ROUTE = "/mes/pro/feedback/edhr-approval/detail";
    private static final String EDHR_BATCH_EXECUTION_DETAIL_ROUTE = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.AUDIT,
            ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
            ApprovalTaskCapability.EVIDENCE_LEDGER
    );

    private final MesProEdhrWorkTaskService workTaskService;
    private final MesProEdhrReleaseService releaseService;

    public MesProEdhrApprovalTaskAdapter(MesProEdhrWorkTaskService workTaskService,
                                         MesProEdhrReleaseService releaseService) {
        this.workTaskService = workTaskService;
        this.releaseService = releaseService;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.EDHR;
    }

    @Override
    public String getModuleName() {
        return "eDHR 审批";
    }

    @Override
    public String getProviderCode() {
        return "edhr-work-task-approval";
    }

    @Override
    public String getProviderVersion() {
        return "phase1";
    }

    @Override
    public Set<ApprovalTaskViewType> getSupportedViewTypes() {
        return SUPPORTED_VIEWS;
    }

    @Override
    public Set<ApprovalTaskCapability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        if (context.getViewType() == ApprovalTaskViewType.TODO) {
            return pageTodo(context);
        }
        MesProEdhrWorkTaskPageReqVO reqVO = toReqVO(context);
        PageResult<MesProEdhrWorkTaskRespVO> page = switch (context.getViewType()) {
            case DONE -> workTaskService.getApprovalCenterDonePage(reqVO, context.isGlobalView());
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: EDHR does not support "
                    + context.getViewType());
        };
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: EDHR");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: EDHR");
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(this::toSummary)
                .toList();
        return new PageResult<>(summaries, page.getTotal());
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long workTaskId = parseRequiredLong(context.getSourceTaskId(),
                "APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        Long executionId = parseOptionalLong(context.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_INVALID: eDHR execution id must be numeric ");
        List<MesProEdhrWorkTaskDO> tasks = workTaskService.getApprovalCenterTimelineTasks(workTaskId, executionId,
                context.isGlobalView());
        Objects.requireNonNull(tasks, "APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline list is required");
        if (tasks.isEmpty()) {
            throw new IllegalStateException("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty");
        }
        return tasks.stream()
                .map(this::toTimelineEntry)
                .toList();
    }

    @Override
    public void review(ApprovalTaskReviewContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long workTaskId = parseRequiredLong(context.getSourceTaskId(),
                "APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        MesProEdhrWorkTaskDO task = workTaskService.validateReleaseApprovalTask(workTaskId, null);
        Long releaseTransactionId = task.getBusinessScopeId();
        if (ApprovalTaskReviewResult.APPROVE.equals(context.getResult())) {
            releaseService.approve(new MesProEdhrReleaseApproveReqVO()
                    .setReleaseTransactionId(releaseTransactionId)
                    .setIdempotencyKey(buildReviewIdempotencyKey(context.getResult(), workTaskId))
                    .setSignoffEvidenceHash(buildSignoffEvidenceHash(context))
                    .setApprovalOpinion(context.getReason()));
            return;
        }
        if (ApprovalTaskReviewResult.REJECT.equals(context.getResult())) {
            releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                    .setReleaseTransactionId(releaseTransactionId)
                    .setIdempotencyKey(buildReviewIdempotencyKey(context.getResult(), workTaskId))
                    .setRejectReason(context.getReason()));
            return;
        }
        throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: EDHR release "
                + context.getResult());
    }

    private PageResult<ApprovalTaskSummary> pageTodo(ApprovalTaskQueryContext context) {
        MesProEdhrWorkTaskPageReqVO reqVO = toMergedTodoReqVO(context);
        PageResult<MesProEdhrWorkTaskRespVO> workTaskPage =
                workTaskService.getApprovalCenterTodoPage(reqVO, context.isGlobalView());
        Objects.requireNonNull(workTaskPage, "APPROVAL_ADAPTER_PAGE_REQUIRED: EDHR");
        Objects.requireNonNull(workTaskPage.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: EDHR");
        PageResult<MesProEdhrWorkTaskRespVO> candidateSignaturePage =
                workTaskService.getApprovalCenterCandidateSignatureTodoPage(reqVO, context.isGlobalView());
        Objects.requireNonNull(candidateSignaturePage, "APPROVAL_ADAPTER_PAGE_REQUIRED: EDHR candidate signature");
        Objects.requireNonNull(candidateSignaturePage.getList(),
                "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: EDHR candidate signature");
        Map<String, ApprovalTaskSummary> summaryMap = new LinkedHashMap<>();
        workTaskPage.getList().stream().map(this::toSummary)
                .forEach(summary -> summaryMap.putIfAbsent(summary.getId(), summary));
        candidateSignaturePage.getList().stream().map(this::toSummary)
                .forEach(summary -> summaryMap.putIfAbsent(summary.getId(), summary));
        List<ApprovalTaskSummary> summaries = new ArrayList<>(summaryMap.values());
        summaries.sort(Comparator.comparing(MesProEdhrApprovalTaskAdapter::sortTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        long duplicateTotal = workTaskService.countApprovalCenterTodoDuplicateTasks(reqVO, context.isGlobalView());
        return new PageResult<>(slicePageRows(summaries, context.getPageNo(), context.getPageSize()),
                safeTotal(workTaskPage) + safeTotal(candidateSignaturePage)
                        - Math.max(duplicateTotal, 0L));
    }

    private static MesProEdhrWorkTaskPageReqVO toReqVO(ApprovalTaskQueryContext context) {
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setPageNo(context.getPageNo() == null ? 1 : context.getPageNo());
        reqVO.setPageSize(context.getPageSize() == null ? 10 : context.getPageSize());
        reqVO.setBatchCode(context.getKeyword());
        return reqVO;
    }

    private static MesProEdhrWorkTaskPageReqVO toMergedTodoReqVO(ApprovalTaskQueryContext context) {
        MesProEdhrWorkTaskPageReqVO reqVO = toReqVO(context);
        int safePageNo = context.getPageNo() == null || context.getPageNo() < 1 ? 1 : context.getPageNo();
        int safePageSize = context.getPageSize() == null || context.getPageSize() < 1 ? 10 : context.getPageSize();
        reqVO.setPageNo(1);
        reqVO.setPageSize(safePageNo * safePageSize);
        return reqVO;
    }

    private ApprovalTaskSummary toSummary(MesProEdhrWorkTaskRespVO task) {
        if (task.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        }
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("workTaskId", String.valueOf(task.getId()));
        if (isReleaseApprovalTask(task)) {
            if (task.getBatchExecutionId() != null) {
                detailQuery.put("id", String.valueOf(task.getBatchExecutionId()));
            }
            detailQuery.put("focus", "approval");
            if (task.getBusinessScopeId() != null) {
                detailQuery.put("releaseTransactionId", String.valueOf(task.getBusinessScopeId()));
            }
        } else if (task.getExecutionId() != null) {
            detailQuery.put("executionId", String.valueOf(task.getExecutionId()));
        }
        ActionUrlParts decisionActionUrl = resolveDecisionActionUrl(task);
        Map<String, String> decisionDetailQuery = resolveDecisionDetailQuery(task, detailQuery, decisionActionUrl);
        return ApprovalTaskSummary.builder()
                .id("EDHR:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(task.getId()))
                .businessKey(String.valueOf(task.getId()))
                .businessTitle(resolveTitle(task))
                .businessCode(task.getTaskCode())
                .businessStatus(task.getStatus())
                .currentNodeCode(hasText(task.getSignatureCellKey()) ? task.getSignatureCellKey() : task.getTaskType())
                .currentNodeName(task.getProcessName())
                .initiatorUserId(task.getSourceUserId())
                .initiatorUserName(task.getSourceUserName())
                .assigneeUserId(task.getAssigneeUserId())
                .processInstanceId(resolveProcessInstanceId(task))
                .taskCreatedAt(task.getCreateTime())
                .taskCompletedAt(task.getCompletedAt())
                .approvalResult(resolveApprovalResult(task))
                .requiresSignature(isReleaseApprovalTask(task) || hasText(task.getSignatureCellKey()))
                .detailRoute(resolveDetailRoute(task))
                .detailQuery(detailQuery)
                .decisionDetailRoute(resolveDecisionDetailRoute(task, decisionActionUrl))
                .decisionDetailQuery(decisionDetailQuery)
                .availableActions(resolveAvailableActions(task))
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskTimelineEntry toTimelineEntry(MesProEdhrWorkTaskDO task) {
        if (task.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        }
        return ApprovalTaskTimelineEntry.builder()
                .id("EDHR:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(task.getId()))
                .businessKey(String.valueOf(task.getId()))
                .nodeCode(hasText(task.getSignatureCellKey()) ? task.getSignatureCellKey() : task.getTaskType())
                .nodeName(hasText(task.getProcessName()) ? task.getProcessName() : task.getTaskType())
                .action(resolveAction(task))
                .actionLabel(resolveActionLabel(task))
                .actorUserId(task.getAssigneeUserId())
                .actedAt(resolveActedAt(task))
                .comment(resolveComment(task))
                .status(task.getStatus())
                .evidenceType("REAL_WORK_TASK")
                .domainReferenceId(String.valueOf(task.getId()))
                .build();
    }

    private static String resolveTitle(MesProEdhrWorkTaskRespVO task) {
        if (hasText(task.getBatchCode()) && hasText(task.getProcessName())) {
            return task.getBatchCode() + " / " + task.getProcessName();
        }
        if (hasText(task.getTaskCode())) {
            return task.getTaskCode();
        }
        return "eDHR 工作任务 #" + task.getId();
    }

    private static String resolveProcessInstanceId(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task)) {
            return null;
        }
        return task.getExecutionId() == null ? null : String.valueOf(task.getExecutionId());
    }

    private static String resolveDetailRoute(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task)) {
            return EDHR_BATCH_EXECUTION_DETAIL_ROUTE;
        }
        return EDHR_WORK_TASK_ROUTE;
    }

    private static String resolveDecisionDetailRoute(MesProEdhrWorkTaskRespVO task,
                                                     ActionUrlParts decisionActionUrl) {
        if (isReleaseApprovalTask(task)) {
            return EDHR_BATCH_EXECUTION_DETAIL_ROUTE;
        }
        if (isApprovalDecisionTask(task)) {
            return task.getExecutionId() == null ? null : EDHR_APPROVAL_DETAIL_ROUTE;
        }
        return decisionActionUrl == null ? null : decisionActionUrl.route();
    }

    private static Map<String, String> resolveDecisionDetailQuery(MesProEdhrWorkTaskRespVO task,
                                                                  Map<String, String> detailQuery,
                                                                  ActionUrlParts decisionActionUrl) {
        if (isReleaseApprovalTask(task)) {
            return new LinkedHashMap<>(detailQuery);
        }
        if (!isApprovalDecisionTask(task)) {
            return decisionActionUrl == null ? new LinkedHashMap<>() : new LinkedHashMap<>(decisionActionUrl.query());
        }
        Map<String, String> query = new LinkedHashMap<>();
        if (task.getExecutionId() != null) {
            query.put("id", String.valueOf(task.getExecutionId()));
        }
        query.put("workTaskId", String.valueOf(task.getId()));
        if (hasText(task.getBpmTaskId())) {
            query.put("bpmTaskId", task.getBpmTaskId());
        }
        return query;
    }

    private static ActionUrlParts resolveDecisionActionUrl(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task) || isApprovalDecisionTask(task) || !hasText(task.getActionUrl())) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(task.getActionUrl().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_INVALID: eDHR work task " + task.getId(), ex);
        }
        if (!hasText(uri.getPath())) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_ROUTE_REQUIRED: eDHR work task " + task.getId());
        }
        return new ActionUrlParts(uri.getPath(), parseActionUrlQuery(uri.getRawQuery(), task.getId()));
    }

    private static Map<String, String> parseActionUrlQuery(String rawQuery, Long workTaskId) {
        Map<String, String> query = new LinkedHashMap<>();
        if (!hasText(rawQuery)) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int separatorIndex = pair.indexOf('=');
            String rawKey = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
            String rawValue = separatorIndex < 0 ? "" : pair.substring(separatorIndex + 1);
            String key = decodeActionUrlPart(rawKey, workTaskId);
            if (!hasText(key)) {
                throw new IllegalStateException("APPROVAL_ACTION_URL_QUERY_KEY_REQUIRED: eDHR work task "
                        + workTaskId);
            }
            query.put(key, decodeActionUrlPart(rawValue, workTaskId));
        }
        return query;
    }

    private static String decodeActionUrlPart(String value, Long workTaskId) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_QUERY_INVALID: eDHR work task " + workTaskId, ex);
        }
    }

    private static Set<String> resolveAvailableActions(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task) && isProcessableStatus(task.getStatus())) {
            return Set.of("APPROVE", "REJECT", "PROCESS_IN_MODULE");
        }
        if (isApprovalDecisionTask(task) && isProcessableStatus(task.getStatus())) {
            String reviewAction = MesProEdhrWorkTaskService.TASK_TYPE_APPROVE.equals(task.getTaskType())
                    ? "APPROVE_IN_MODULE" : "REVIEW_IN_MODULE";
            return Set.of(reviewAction, "PROCESS_IN_MODULE");
        }
        return Set.of("PROCESS_IN_MODULE");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static List<ApprovalTaskSummary> slicePageRows(List<ApprovalTaskSummary> rows,
                                                           Integer pageNo,
                                                           Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private static LocalDateTime sortTime(ApprovalTaskSummary summary) {
        if (summary.getTaskCreatedAt() != null) {
            return summary.getTaskCreatedAt();
        }
        return summary.getInitiatedAt();
    }

    private static long safeTotal(PageResult<?> page) {
        return page.getTotal() == null ? 0L : page.getTotal();
    }

    private static void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: EDHR does not support "
                    + sourceTaskType);
        }
    }

    private static Long parseRequiredLong(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return parseOptionalLong(value, message);
    }

    private static Long parseOptionalLong(String value, String message) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(message + value, ex);
        }
    }

    private static String resolveAction(MesProEdhrWorkTaskDO task) {
        if (MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())) {
            return isReleaseRejected(task) ? "REJECTED" : "APPROVED";
        }
        if (MesProEdhrWorkTaskStatus.OVERDUE.equals(task.getStatus())) {
            return "OVERDUE";
        }
        if (MesProEdhrWorkTaskStatus.CANCELED.equals(task.getStatus())) {
            return "CANCELLED";
        }
        if (MesProEdhrWorkTaskStatus.DOING.equals(task.getStatus())) {
            return "STARTED";
        }
        if (MesProEdhrWorkTaskStatus.TODO.equals(task.getStatus())) {
            return "CURRENT";
        }
        return task.getStatus();
    }

    private static ApprovalTaskReviewResult resolveApprovalResult(MesProEdhrWorkTaskRespVO task) {
        if (!MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())) {
            return null;
        }
        return isReleaseRejected(task) ? ApprovalTaskReviewResult.REJECT : ApprovalTaskReviewResult.APPROVE;
    }

    private static String resolveActionLabel(MesProEdhrWorkTaskDO task) {
        if (MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())) {
            return isReleaseRejected(task) ? "审批驳回" : "审批通过";
        }
        if (MesProEdhrWorkTaskStatus.OVERDUE.equals(task.getStatus())) {
            return "已逾期";
        }
        if (MesProEdhrWorkTaskStatus.CANCELED.equals(task.getStatus())) {
            return "已取消";
        }
        if (MesProEdhrWorkTaskStatus.DOING.equals(task.getStatus())) {
            return "处理中";
        }
        if (MesProEdhrWorkTaskStatus.TODO.equals(task.getStatus())) {
            return "待处理";
        }
        return task.getStatus();
    }

    private static LocalDateTime resolveActedAt(MesProEdhrWorkTaskDO task) {
        if (task.getCompletedAt() != null) {
            return task.getCompletedAt();
        }
        if (task.getOverdueAt() != null) {
            return task.getOverdueAt();
        }
        if (task.getStartedAt() != null) {
            return task.getStartedAt();
        }
        return task.getCreateTime();
    }

    private static String resolveComment(MesProEdhrWorkTaskDO task) {
        if (hasText(task.getReason())) {
            return stripReviewResultPrefix(task.getReason());
        }
        if (hasText(task.getRemark())) {
            return task.getRemark();
        }
        return task.getOverdueReason();
    }

    private static String buildReviewIdempotencyKey(ApprovalTaskReviewResult result, Long workTaskId) {
        return "APPROVAL-CENTER-" + result.name() + "-" + workTaskId;
    }

    private static String buildSignoffEvidenceHash(ApprovalTaskReviewContext context) {
        if (!hasText(context.getSignatureImageFileUrl())) {
            throw new IllegalStateException("APPROVAL_SIGNATURE_EVIDENCE_REQUIRED: EDHR release approval");
        }
        return DigestUtil.sha256Hex(context.getSignatureImageFileUrl());
    }

    private static boolean isProcessableStatus(String status) {
        return MesProEdhrWorkTaskStatus.TODO.equals(status) || MesProEdhrWorkTaskStatus.OVERDUE.equals(status);
    }

    private static boolean isReleaseApprovalTask(MesProEdhrWorkTaskRespVO task) {
        return task != null && MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType());
    }

    private static boolean isApprovalDecisionTask(MesProEdhrWorkTaskRespVO task) {
        return task != null && (MesProEdhrWorkTaskService.TASK_TYPE_REVIEW.equals(task.getTaskType())
                || MesProEdhrWorkTaskService.TASK_TYPE_APPROVE.equals(task.getTaskType()));
    }

    private static boolean isReleaseRejected(MesProEdhrWorkTaskRespVO task) {
        return isReleaseApprovalTask(task) && hasText(task.getReason()) && task.getReason().startsWith("REJECT:");
    }

    private static boolean isReleaseRejected(MesProEdhrWorkTaskDO task) {
        return task != null
                && MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())
                && hasText(task.getReason())
                && task.getReason().startsWith("REJECT:");
    }

    private static String stripReviewResultPrefix(String reason) {
        if (reason.startsWith("APPROVE:") || reason.startsWith("REJECT:")) {
            return reason.substring(reason.indexOf(':') + 1);
        }
        return reason;
    }

    private record ActionUrlParts(String route, Map<String, String> query) {
    }
}
