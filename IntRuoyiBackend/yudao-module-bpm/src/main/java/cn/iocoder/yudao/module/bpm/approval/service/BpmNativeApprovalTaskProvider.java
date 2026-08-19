package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCopyPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstancePageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BpmNativeApprovalTaskProvider implements ApprovalTaskProvider {

    private static final String PROCESS_INSTANCE_SOURCE = "BPM_PROCESS_INSTANCE";
    private static final String COPY_SOURCE = "BPM_PROCESS_INSTANCE_COPY";
    private static final String TODO_SOURCE = "BPM_TASK_TODO";
    private static final String DONE_SOURCE = "BPM_TASK_DONE";
    private static final String BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE =
            "BATCH_RECORD_VERSION_APPROVAL";
    private static final String EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE =
            "EDHR_BATCH_EXECUTION_VOID";
    private static final String MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE =
            "MES_ROUTE_VERSION_PUBLISH";
    private static final String BATCH_RECORD_VERSION_DETAIL_ROUTE =
            "/mes/pro/batch-record-form-list";
    private static final String EDHR_RECORD_CHANGE_DETAIL_ROUTE =
            "/mes/pro/feedback/edhr-change";
    private static final String ROUTE_VERSION_DETAIL_ROUTE_PREFIX =
            "/mes/pro/route/edit/";
    private static final Pattern TEMPLATE_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE,
            ApprovalTaskViewType.MY_INITIATED,
            ApprovalTaskViewType.CC
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.NOTIFICATION,
            ApprovalTaskCapability.AUDIT
    );
    private static final Set<String> TODO_ACTIONS = Set.of("OPEN_DETAIL", "APPROVE", "REJECT");
    private static final Set<String> DETAIL_ACTIONS = Set.of("OPEN_DETAIL");

    private final BpmProcessInstanceService processInstanceService;
    private final BpmProcessInstanceCopyService copyService;
    private final BpmTaskService taskService;

    public BpmNativeApprovalTaskProvider(BpmProcessInstanceService processInstanceService,
                                         BpmProcessInstanceCopyService copyService,
                                         BpmTaskService taskService) {
        this.processInstanceService = processInstanceService;
        this.copyService = copyService;
        this.taskService = taskService;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.BPM;
    }

    @Override
    public String getModuleName() {
        return "BPM 原生审批";
    }

    @Override
    public String getProviderCode() {
        return "bpm-native-approval";
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
        return switch (context.getViewType()) {
            case TODO -> pageTodo(context);
            case DONE -> pageDone(context);
            case MY_INITIATED -> pageMyInitiated(context);
            case CC -> pageCopied(context);
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: BPM does not support "
                    + context.getViewType());
        };
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        throw new IllegalArgumentException("APPROVAL_TIMELINE_UNSUPPORTED: BPM does not support timeline in phase2");
    }

    @Override
    public void review(ApprovalTaskReviewContext context) {
        Objects.requireNonNull(context, "APPROVAL_REVIEW_CONTEXT_REQUIRED");
        if (!TODO_SOURCE.equals(context.getSourceTaskType())) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: BPM only supports "
                    + TODO_SOURCE);
        }
        String taskId = requireText(context.getSourceTaskId(), "APPROVAL_TASK_ID_REQUIRED: BPM review");
        if (context.getResult() == ApprovalTaskReviewResult.APPROVE) {
            taskService.approveTask(context.getLoginUserId(), new BpmTaskApproveReqVO()
                    .setId(taskId)
                    .setReason(trimToNull(context.getReason()))
                    .setSignPicUrl(requireText(context.getSignatureImageFileUrl(),
                            "APPROVAL_SIGNATURE_IMAGE_URL_REQUIRED: BPM review")));
            return;
        }
        if (context.getResult() == ApprovalTaskReviewResult.REJECT) {
            taskService.rejectTask(context.getLoginUserId(), new BpmTaskRejectReqVO()
                    .setId(taskId)
                    .setReason(requireText(context.getReason(), "APPROVAL_REJECT_REASON_REQUIRED")));
            return;
        }
        throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: " + context.getResult());
    }

    private PageResult<ApprovalTaskSummary> pageTodo(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = buildTaskPageReqVO(context);
        PageResult<Task> page = taskService.getTaskTodoPage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM todo");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM todo");
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(this::toTodoSummary)
                .toList();
        if (page.getTotal() == 0 && hasText(context.getKeyword())) {
            return pageTodoByProcessInstanceId(context);
        }
        return new PageResult<>(summaries, page.getTotal());
    }

    private PageResult<ApprovalTaskSummary> pageTodoByProcessInstanceId(ApprovalTaskQueryContext context) {
        List<ApprovalTaskSummary> summaries = taskService
                .getRunningTaskListByProcessInstanceId(context.getKeyword().trim(), true, null)
                .stream()
                .filter(task -> context.isGlobalView()
                        || Objects.equals(String.valueOf(context.getLoginUserId()), task.getAssignee()))
                .filter(BpmNativeApprovalTaskProvider::isCurrentTenantTask)
                .map(this::toTodoSummary)
                .toList();
        return pageSummaries(summaries, context.getPageNo(), context.getPageSize());
    }

    private PageResult<ApprovalTaskSummary> pageDone(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = buildTaskPageReqVO(context);
        PageResult<HistoricTaskInstance> page = taskService.getTaskDonePage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM done");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM done");
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(this::toDoneSummary)
                .toList();
        return new PageResult<>(summaries, page.getTotal());
    }

    private PageResult<ApprovalTaskSummary> pageMyInitiated(ApprovalTaskQueryContext context) {
        BpmProcessInstancePageReqVO reqVO = new BpmProcessInstancePageReqVO();
        reqVO.setPageNo(context.getPageNo() == null ? 1 : context.getPageNo());
        reqVO.setPageSize(context.getPageSize() == null ? 10 : context.getPageSize());
        reqVO.setName(context.getKeyword());
        PageResult<HistoricProcessInstance> page =
                processInstanceService.getProcessInstancePage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM");
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(this::toProcessInstanceSummary)
                .toList();
        return new PageResult<>(summaries, page.getTotal());
    }

    private PageResult<ApprovalTaskSummary> pageCopied(ApprovalTaskQueryContext context) {
        BpmProcessInstanceCopyPageReqVO reqVO = new BpmProcessInstanceCopyPageReqVO();
        reqVO.setPageNo(context.getPageNo() == null ? 1 : context.getPageNo());
        reqVO.setPageSize(context.getPageSize() == null ? 10 : context.getPageSize());
        reqVO.setProcessInstanceName(context.getKeyword());
        PageResult<BpmProcessInstanceCopyDO> page =
                copyService.getProcessInstanceCopyPage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM copy");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM copy");
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(this::toCopySummary)
                .toList();
        return new PageResult<>(summaries, page.getTotal());
    }

    private ApprovalTaskSummary toTodoSummary(Task task) {
        requireTaskIdentity(task.getId(), task.getProcessInstanceId(), "BPM todo");
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", task.getProcessInstanceId());
        detailQuery.put("taskId", task.getId());
        Map<String, Object> variables = task.getProcessVariables();
        Map<String, String> decisionDetailQuery = buildDecisionDetailQuery(variables, task.getProcessInstanceId());
        return ApprovalTaskSummary.builder()
                .id("BPM:" + TODO_SOURCE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(TODO_SOURCE)
                .sourceTaskId(task.getId())
                .businessKey(task.getProcessInstanceId())
                .businessTitle(resolveBusinessTitle(task.getName(), variables))
                .businessStatus("TODO")
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(task.getName())
                .assigneeUserId(parseLong(task.getAssignee()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/bpm/process-instance/detail")
                .detailQuery(detailQuery)
                .decisionDetailRoute(resolveDecisionDetailRoute(variables))
                .decisionDetailQuery(decisionDetailQuery)
                .availableActions(TODO_ACTIONS)
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toDoneSummary(HistoricTaskInstance task) {
        requireTaskIdentity(task.getId(), task.getProcessInstanceId(), "BPM done");
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", task.getProcessInstanceId());
        detailQuery.put("taskId", task.getId());
        Map<String, Object> variables = task.getProcessVariables();
        Map<String, String> decisionDetailQuery = buildDecisionDetailQuery(variables, task.getProcessInstanceId());
        ApprovalTaskReviewResult approvalResult = resolveDoneApprovalResult(task);
        return ApprovalTaskSummary.builder()
                .id("BPM:" + DONE_SOURCE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(DONE_SOURCE)
                .sourceTaskId(task.getId())
                .businessKey(task.getProcessInstanceId())
                .businessTitle(resolveBusinessTitle(task.getName(), variables))
                .businessStatus("DONE")
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(task.getName())
                .assigneeUserId(parseLong(task.getAssignee()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .taskCompletedAt(toLocalDateTime(task.getEndTime()))
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        FlowableUtils.getTaskReason(task)))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/bpm/process-instance/detail")
                .detailQuery(detailQuery)
                .decisionDetailRoute(resolveDecisionDetailRoute(variables))
                .decisionDetailQuery(decisionDetailQuery)
                .availableActions(DETAIL_ACTIONS)
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toProcessInstanceSummary(HistoricProcessInstance instance) {
        if (instance.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: BPM process instance id is required");
        }
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", instance.getId());
        return ApprovalTaskSummary.builder()
                .id("BPM:" + PROCESS_INSTANCE_SOURCE + ":" + instance.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(PROCESS_INSTANCE_SOURCE)
                .sourceTaskId(instance.getId())
                .businessKey(instance.getBusinessKey())
                .businessTitle(instance.getName())
                .businessStatus("MY_INITIATED")
                .currentNodeName("我发起的")
                .initiatorUserId(parseLong(instance.getStartUserId()))
                .processInstanceId(instance.getId())
                .initiatedAt(toLocalDateTime(instance.getStartTime()))
                .taskCreatedAt(toLocalDateTime(instance.getStartTime()))
                .taskCompletedAt(toLocalDateTime(instance.getEndTime()))
                .requiresSignature(Boolean.FALSE)
                .detailRoute("/bpm/process-instance/detail")
                .detailQuery(detailQuery)
                .availableActions(DETAIL_ACTIONS)
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toCopySummary(BpmProcessInstanceCopyDO copy) {
        if (copy.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: BPM process copy id is required");
        }
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", copy.getProcessInstanceId());
        if (copy.getTaskId() != null) {
            detailQuery.put("taskId", copy.getTaskId());
        }
        if (copy.getActivityId() != null) {
            detailQuery.put("activityId", copy.getActivityId());
        }
        return ApprovalTaskSummary.builder()
                .id("BPM:" + COPY_SOURCE + ":" + copy.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(COPY_SOURCE)
                .sourceTaskId(String.valueOf(copy.getId()))
                .businessKey(copy.getProcessInstanceId())
                .businessTitle(copy.getProcessInstanceName())
                .businessStatus("CC")
                .currentNodeCode(copy.getActivityId())
                .currentNodeName(copy.getActivityName())
                .initiatorUserId(copy.getStartUserId())
                .processInstanceId(copy.getProcessInstanceId())
                .taskCreatedAt(copy.getCreateTime())
                .requiresSignature(Boolean.FALSE)
                .detailRoute("/bpm/process-instance/detail")
                .detailQuery(detailQuery)
                .availableActions(DETAIL_ACTIONS)
                .capabilities(CAPABILITIES)
                .build();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private static ApprovalTaskReviewResult resolveDoneApprovalResult(HistoricTaskInstance task) {
        Integer taskStatus = FlowableUtils.getTaskStatus(task);
        return taskStatus == null ? null : ApprovalTaskResultSupport.fromBpmTaskStatus(
                taskStatus, "BPM done " + task.getId());
    }

    private static BpmTaskPageReqVO buildTaskPageReqVO(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(context.getPageNo() == null ? 1 : context.getPageNo());
        reqVO.setPageSize(context.getPageSize() == null ? 10 : context.getPageSize());
        reqVO.setName(context.getKeyword());
        return reqVO;
    }

    private static PageResult<ApprovalTaskSummary> pageSummaries(List<ApprovalTaskSummary> rows,
                                                                 Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private static boolean isCurrentTenantTask(Task task) {
        String currentTenantId = FlowableUtils.getTenantId();
        String taskTenantId = task.getTenantId();
        if (!hasText(currentTenantId)) {
            return !hasText(taskTenantId);
        }
        return Objects.equals(currentTenantId, taskTenantId);
    }

    private static String resolveBusinessTitle(String fallback, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return sanitizeBusinessTitle(fallback, null);
        }
        String businessType = asText(variables.get("businessType"));
        if (Objects.equals(BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE, businessType)) {
            return resolveBatchRecordVersionTitle(variables);
        }
        if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            return resolveRouteVersionPublishTitle(variables);
        }
        return sanitizeBusinessTitle(fallback, variables);
    }

    private static String resolveBatchRecordVersionTitle(Map<String, Object> variables) {
        String batchRecordName = asText(variables.get("batchRecordName"));
        String versionNo = asText(variables.get("versionNo"));
        StringBuilder title = new StringBuilder("批记录升版");
        if (hasText(batchRecordName)) {
            title.append(' ').append(batchRecordName.trim());
        }
        if (hasText(versionNo)) {
            title.append(' ').append(versionNo.trim());
        }
        return title.toString();
    }

    private static String resolveRouteVersionPublishTitle(Map<String, Object> variables) {
        String routeDisplay = firstText(variables.get("routeName"), variables.get("routeCode"),
                variables.get("routeId"), variables.get("objectId"));
        String versionNo = firstText(variables.get("routeVersionNo"), variables.get("objectVersion"));
        StringBuilder title = new StringBuilder("工艺路线发布");
        if (hasText(routeDisplay)) {
            title.append(' ').append(routeDisplay.trim());
        }
        if (hasText(versionNo)) {
            title.append(' ').append(versionNo.trim());
        }
        return title.toString();
    }

    private static String sanitizeBusinessTitle(String fallback, Map<String, Object> variables) {
        String title = asText(fallback);
        if (!hasText(title)) {
            return "BPM 审批";
        }
        if (!title.contains("${")) {
            return title;
        }
        String resolved = replaceTemplatePlaceholders(title, variables);
        if (hasText(resolved) && !resolved.contains("${")) {
            return resolved.trim();
        }
        String businessType = variables == null ? null : asText(variables.get("businessType"));
        String businessKey = variables == null ? null : firstText(variables.get("businessKey"),
                variables.get("objectId"));
        StringBuilder sanitized = new StringBuilder("BPM 审批");
        if (hasText(businessType)) {
            sanitized.append(' ').append(businessType.trim());
        }
        if (hasText(businessKey)) {
            sanitized.append(' ').append(businessKey.trim());
        }
        return sanitized.toString();
    }

    private static String replaceTemplatePlaceholders(String template, Map<String, Object> variables) {
        Matcher matcher = TEMPLATE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables == null ? null : asText(variables.get(variableName));
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(hasText(value) ? value.trim() : ""));
        }
        matcher.appendTail(resolved);
        return resolved.toString().replaceAll("\\s+", " ").trim();
    }

    private static String resolveDecisionDetailRoute(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        String businessType = asText(variables.get("businessType"));
        if (Objects.equals(BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE, businessType)) {
            return BATCH_RECORD_VERSION_DETAIL_ROUTE;
        }
        if (Objects.equals(EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE, businessType)) {
            return EDHR_RECORD_CHANGE_DETAIL_ROUTE;
        }
        if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            String routeId = asText(variables.get("routeId"));
            return hasText(routeId) ? ROUTE_VERSION_DETAIL_ROUTE_PREFIX + routeId.trim() : null;
        }
        return null;
    }

    private static Map<String, String> buildDecisionDetailQuery(Map<String, Object> variables,
                                                                String processInstanceId) {
        Map<String, String> query = new LinkedHashMap<>();
        if (variables == null || variables.isEmpty()) {
            return query;
        }
        String businessType = asText(variables.get("businessType"));
        if (Objects.equals(BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE, businessType)) {
            putIfPresent(query, "businessType", businessType);
            putIfPresent(query, "batchRecordDefinitionId", variables.get("batchRecordDefinitionId"));
            putIfPresent(query, "batchRecordVersionId", variables.get("batchRecordVersionId"));
            putIfPresent(query, "versionNo", variables.get("versionNo"));
            putIfPresent(query, "sourceVersionId", variables.get("sourceVersionId"));
            putIfPresent(query, "sourceVersionNo", variables.get("sourceVersionNo"));
            putIfPresent(query, "routeId", variables.get("routeId"));
            putIfPresent(query, "sourceRouteId", variables.get("sourceRouteId"));
            putIfPresent(query, "processInstanceId", processInstanceId);
            return query;
        }
        if (Objects.equals(EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE, businessType)) {
            putIfPresent(query, "businessType", businessType);
            putIfPresent(query, "changeType", "VOID");
            putIfPresent(query, "targetScope", "BATCH");
            putIfPresent(query, "batchExecutionId", variables.get("batchExecutionId"));
            putIfPresent(query, "batchExecutionCode", variables.get("batchExecutionCode"));
            putIfPresent(query, "workOrderId", variables.get("workOrderId"));
            putIfPresent(query, "workOrderCode", variables.get("workOrderCode"));
            putIfPresent(query, "batchCode", variables.get("batchCode"));
            putIfPresent(query, "reasonCategory", variables.get("reasonCategory"));
            putIfPresent(query, "reasonText", variables.get("reasonText"));
            putIfPresent(query, "processInstanceId", processInstanceId);
            return query;
        }
        if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            putIfPresent(query, "businessType", businessType);
            putIfPresent(query, "routeId", variables.get("routeId"));
            putIfPresent(query, "routeVersionId", firstText(variables.get("routeVersionId"),
                    variables.get("objectId")));
            putIfPresent(query, "routeVersionNo", firstText(variables.get("routeVersionNo"),
                    variables.get("objectVersion")));
            putIfPresent(query, "routeVersionStatus", "PENDING_APPROVAL");
            putIfPresent(query, "tab", "flow");
            putIfPresent(query, "processInstanceId", processInstanceId);
        }
        return query;
    }

    private static void putIfPresent(Map<String, String> query, String key, Object value) {
        String text = asText(value);
        if (text == null || text.isBlank()) {
            return;
        }
        query.put(key, text.trim());
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstText(Object... values) {
        for (Object value : values) {
            String text = asText(value);
            if (hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Long resolveQueryUserId(ApprovalTaskQueryContext context) {
        return context.isGlobalView() ? null : context.getLoginUserId();
    }

    private static void requireTaskIdentity(String taskId, String processInstanceId, String source) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalStateException("APPROVAL_TASK_ID_REQUIRED: " + source);
        }
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new IllegalStateException("APPROVAL_PROCESS_INSTANCE_ID_REQUIRED: " + source);
        }
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
