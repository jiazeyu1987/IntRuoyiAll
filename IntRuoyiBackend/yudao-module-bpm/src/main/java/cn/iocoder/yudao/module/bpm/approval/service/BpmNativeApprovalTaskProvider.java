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
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE =
            "MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART";
    private static final String REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_TYPE =
            "UPLOAD_CERTIFICATE";
    private static final String REGISTRATION_CERTIFICATE_UPLOAD_OPERATION =
            "UPLOAD_CERTIFICATE";
    private static final String REGISTRATION_CERTIFICATE_RENEWAL_OPERATION =
            "RENEWAL_CERTIFICATE";
    private static final String REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE =
            "dcc_registration_certificate_approver";
    private static final String REGISTRATION_CERTIFICATE_UPLOAD_APPROVAL_PERMISSION =
            "dcc:registration-certificate:upload:approve";
    private static final String BATCH_RECORD_VERSION_DETAIL_ROUTE =
            "/mes/pro/batch-record-form-list";
    private static final String EDHR_RECORD_CHANGE_DETAIL_ROUTE =
            "/mes/pro/feedback/edhr-change";
    private static final String ROUTE_VERSION_DETAIL_ROUTE_PREFIX =
            "/mes/pro/route/edit/";
    private static final String ACTIVE_ORDER_VERSION_UPGRADE_DETAIL_ROUTE =
            "/mes/pro/processpool/team-leader";
    private static final String REGISTRATION_CERTIFICATE_DETAIL_ROUTE_PREFIX =
            "/mdm/registration-certificate/detail/";
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
    private final org.flowable.engine.TaskService flowableTaskService;
    private final PermissionApi permissionApi;
    private final RoleApi roleApi;

    public BpmNativeApprovalTaskProvider(BpmProcessInstanceService processInstanceService,
                                         BpmProcessInstanceCopyService copyService,
                                         BpmTaskService taskService,
                                         org.flowable.engine.TaskService flowableTaskService,
                                         PermissionApi permissionApi,
                                         RoleApi roleApi) {
        this.processInstanceService = processInstanceService;
        this.copyService = copyService;
        this.taskService = taskService;
        this.flowableTaskService = flowableTaskService;
        this.permissionApi = permissionApi;
        this.roleApi = roleApi;
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
    @Transactional(rollbackFor = Exception.class)
    public void review(ApprovalTaskReviewContext context) {
        Objects.requireNonNull(context, "APPROVAL_REVIEW_CONTEXT_REQUIRED");
        if (!TODO_SOURCE.equals(context.getSourceTaskType())) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: BPM only supports "
                    + TODO_SOURCE);
        }
        String taskId = requireText(context.getSourceTaskId(), "APPROVAL_TASK_ID_REQUIRED: BPM review");
        if (context.getResult() == ApprovalTaskReviewResult.APPROVE) {
            claimRegistrationUploadTaskIfPermitted(context, taskId);
            taskService.approveTask(context.getLoginUserId(), new BpmTaskApproveReqVO()
                    .setId(taskId)
                    .setReason(trimToNull(context.getReason()))
                    .setSignPicUrl(requireText(context.getSignatureImageFileUrl(),
                            "APPROVAL_SIGNATURE_IMAGE_URL_REQUIRED: BPM review")));
            return;
        }
        if (context.getResult() == ApprovalTaskReviewResult.REJECT) {
            claimRegistrationUploadTaskIfPermitted(context, taskId);
            taskService.rejectTask(context.getLoginUserId(), new BpmTaskRejectReqVO()
                    .setId(taskId)
                    .setReason(requireText(context.getReason(), "APPROVAL_REJECT_REASON_REQUIRED")));
            return;
        }
        throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: " + context.getResult());
    }

    private void claimRegistrationUploadTaskIfPermitted(ApprovalTaskReviewContext context, String taskId) {
        Long loginUserId = context.getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        Task task = taskService.getTask(taskId);
        if (task == null || !hasText(task.getAssignee())
                || Objects.equals(String.valueOf(loginUserId), task.getAssignee())) {
            return;
        }
        String processInstanceId = task.getProcessInstanceId();
        if (!hasText(processInstanceId)) {
            return;
        }
        if (hasText(context.getProcessInstanceId())
                && !Objects.equals(context.getProcessInstanceId(), processInstanceId)) {
            throw new IllegalArgumentException("APPROVAL_TASK_PROCESS_INSTANCE_MISMATCH: BPM review "
                    + taskId);
        }
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        Map<String, Object> variables = processInstance == null ? null : processInstance.getProcessVariables();
        if (!isRegistrationCertificateUploadApproval(variables)
                || !hasRegistrationCertificateUploadApprovalAuthority(loginUserId)) {
            return;
        }
        flowableTaskService.setAssignee(taskId, String.valueOf(loginUserId));
    }

    private PageResult<ApprovalTaskSummary> pageTodo(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = buildTaskPageReqVO(context);
        PageResult<Task> page = taskService.getTaskTodoPage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM todo");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM todo");
        Map<String, ProcessInstance> processInstancesById = requireRuntimeProcessInstances(page.getList());
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(task -> toTodoSummary(task, requireRuntimeProcessInstance(
                        processInstancesById, task.getProcessInstanceId())))
                .toList();
        if (page.getTotal() == 0 && hasText(context.getKeyword())) {
            return pageTodoByProcessInstanceId(context);
        }
        return new PageResult<>(summaries, page.getTotal());
    }

    private PageResult<ApprovalTaskSummary> pageTodoByProcessInstanceId(ApprovalTaskQueryContext context) {
        List<Task> tasks = taskService
                .getRunningTaskListByProcessInstanceId(context.getKeyword().trim(), true, null)
                .stream()
                .filter(task -> context.isGlobalView()
                        || Objects.equals(String.valueOf(context.getLoginUserId()), task.getAssignee()))
                .filter(BpmNativeApprovalTaskProvider::isCurrentTenantTask)
                .toList();
        Map<String, ProcessInstance> processInstancesById = requireRuntimeProcessInstances(tasks);
        return pageSummaries(tasks.stream()
                .map(task -> toTodoSummary(task, requireRuntimeProcessInstance(
                        processInstancesById, task.getProcessInstanceId())))
                .toList(), context.getPageNo(), context.getPageSize());
    }

    private PageResult<ApprovalTaskSummary> pageDone(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = buildTaskPageReqVO(context);
        PageResult<HistoricTaskInstance> page = taskService.getTaskDonePage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: BPM done");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: BPM done");
        if (page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        Set<String> processInstanceIds = page.getList().stream()
                .map(HistoricTaskInstance::getProcessInstanceId)
                .map(id -> requireText(id, "APPROVAL_PROCESS_INSTANCE_ID_REQUIRED: BPM done"))
                .collect(Collectors.toSet());
        Map<String, HistoricProcessInstance> processInstancesById = processInstanceService
                .getHistoricProcessInstances(processInstanceIds)
                .stream()
                .collect(Collectors.toMap(
                        instance -> requireText(instance.getId(),
                                "APPROVAL_PROCESS_INSTANCE_ID_REQUIRED: BPM done history"),
                        Function.identity()));
        List<ApprovalTaskSummary> summaries = page.getList().stream()
                .map(task -> toDoneSummary(task, requireHistoricProcessInstance(
                        processInstancesById, task.getProcessInstanceId())))
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

    private ApprovalTaskSummary toTodoSummary(Task task, ProcessInstance processInstance) {
        requireTaskIdentity(task.getId(), task.getProcessInstanceId(), "BPM todo");
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", task.getProcessInstanceId());
        detailQuery.put("taskId", task.getId());
        Map<String, Object> variables = task.getProcessVariables();
        Map<String, String> decisionDetailQuery = buildDecisionDetailQuery(variables, task.getProcessInstanceId());
        RoleRespDTO assigneeRole = resolveRegistrationCertificateAssigneeRole(variables);
        return ApprovalTaskSummary.builder()
                .id("BPM:" + TODO_SOURCE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(TODO_SOURCE)
                .sourceTaskId(task.getId())
                .businessKey(task.getProcessInstanceId())
                .businessTitle(resolveBusinessTitle(task.getName(), variables))
                .businessCode(resolveBusinessCode(variables))
                .businessIdentifierHidden(isRegistrationCertificateUploadOrRenewalApproval(variables))
                .businessContextTags(resolveBusinessContextTags(variables))
                .businessStatus("TODO")
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(resolveCurrentNodeName(task.getName(), task.getTaskDefinitionKey(), variables))
                .initiatorUserId(resolveProcessInstanceStartUserId(processInstance.getStartUserId(),
                        processInstance.getProcessVariables()))
                .assigneeUserId(parseLong(task.getAssignee()))
                .assigneeRoleCode(assigneeRole == null ? null : assigneeRole.getCode())
                .assigneeRoleName(assigneeRole == null ? null : assigneeRole.getName())
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

    private ApprovalTaskSummary toDoneSummary(HistoricTaskInstance task, HistoricProcessInstance instance) {
        requireTaskIdentity(task.getId(), task.getProcessInstanceId(), "BPM done");
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("id", task.getProcessInstanceId());
        detailQuery.put("taskId", task.getId());
        Map<String, Object> variables = instance.getProcessVariables();
        Map<String, String> decisionDetailQuery = buildDecisionDetailQuery(variables, task.getProcessInstanceId());
        ApprovalTaskReviewResult approvalResult = resolveDoneApprovalResult(task);
        RoleRespDTO assigneeRole = resolveRegistrationCertificateAssigneeRole(variables);
        return ApprovalTaskSummary.builder()
                .id("BPM:" + DONE_SOURCE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(DONE_SOURCE)
                .sourceTaskId(task.getId())
                .businessKey(instance.getBusinessKey())
                .businessTitle(resolveBusinessTitle(instance.getName(), variables))
                .businessCode(resolveBusinessCode(variables))
                .businessIdentifierHidden(isRegistrationCertificateUploadOrRenewalApproval(variables))
                .businessContextTags(resolveBusinessContextTags(variables))
                .businessStatus("DONE")
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(resolveCurrentNodeName(task.getName(), task.getTaskDefinitionKey(), variables))
                .initiatorUserId(resolveProcessInstanceStartUserId(instance.getStartUserId(),
                        instance.getProcessVariables()))
                .assigneeUserId(parseLong(task.getAssignee()))
                .assigneeRoleCode(assigneeRole == null ? null : assigneeRole.getCode())
                .assigneeRoleName(assigneeRole == null ? null : assigneeRole.getName())
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
        Map<String, Object> variables = instance.getProcessVariables();
        return ApprovalTaskSummary.builder()
                .id("BPM:" + PROCESS_INSTANCE_SOURCE + ":" + instance.getId())
                .moduleCode(ApprovalModuleCode.BPM)
                .sourceTaskType(PROCESS_INSTANCE_SOURCE)
                .sourceTaskId(instance.getId())
                .businessKey(instance.getBusinessKey())
                .businessTitle(resolveBusinessTitle(instance.getName(), variables))
                .businessCode(resolveBusinessCode(variables))
                .businessIdentifierHidden(isRegistrationCertificateUploadOrRenewalApproval(variables))
                .businessContextTags(resolveBusinessContextTags(variables))
                .businessStatus("MY_INITIATED")
                .currentNodeName("我发起的")
                .initiatorUserId(resolveProcessInstanceStartUserId(instance.getStartUserId(),
                        instance.getProcessVariables()))
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

    private Map<String, ProcessInstance> requireRuntimeProcessInstances(List<? extends Task> tasks) {
        Set<String> processInstanceIds = tasks.stream()
                .map(Task::getProcessInstanceId)
                .map(id -> requireText(id, "APPROVAL_PROCESS_INSTANCE_ID_REQUIRED: BPM todo"))
                .collect(Collectors.toSet());
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ProcessInstance> processInstancesById = processInstanceService.getProcessInstanceMap(processInstanceIds);
        Objects.requireNonNull(processInstancesById, "APPROVAL_PROCESS_INSTANCE_MAP_REQUIRED: BPM todo");
        return processInstancesById;
    }

    private static ProcessInstance requireRuntimeProcessInstance(
            Map<String, ProcessInstance> processInstancesById, String processInstanceId) {
        ProcessInstance processInstance = processInstancesById.get(processInstanceId);
        if (processInstance == null) {
            throw new IllegalStateException("APPROVAL_PROCESS_INSTANCE_REQUIRED: BPM todo " + processInstanceId);
        }
        return processInstance;
    }

    private RoleRespDTO resolveRegistrationCertificateAssigneeRole(Map<String, Object> variables) {
        if (!isRegistrationCertificateUploadApproval(variables)) {
            return null;
        }
        RoleRespDTO role = Objects.requireNonNull(roleApi.getRoleByCode(REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE),
                "APPROVAL_ASSIGNEE_ROLE_REQUIRED: " + REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE);
        if (!REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE.equals(role.getCode()) || !hasText(role.getName())) {
            throw new IllegalStateException("APPROVAL_ASSIGNEE_ROLE_INVALID: "
                    + REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE);
        }
        return role;
    }

    private static HistoricProcessInstance requireHistoricProcessInstance(
            Map<String, HistoricProcessInstance> processInstancesById, String processInstanceId) {
        HistoricProcessInstance instance = processInstancesById.get(processInstanceId);
        if (instance == null) {
            throw new IllegalStateException("APPROVAL_PROCESS_INSTANCE_REQUIRED: BPM done " + processInstanceId);
        }
        return instance;
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

    private static Long resolveProcessInstanceStartUserId(String startUserId, Map<String, Object> processVariables) {
        Long value = parseLong(startUserId);
        if (value != null || processVariables == null) {
            return value;
        }
        return parseLong(processVariables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID));
    }

    private static Long parseLong(Object value) {
        String text = asText(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        return Long.valueOf(text.trim());
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
        if (Objects.equals(EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE, businessType)) {
            return resolveEdhrBatchExecutionVoidTitle(variables);
        }
        if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            return resolveRouteVersionPublishTitle(variables);
        }
        if (Objects.equals(MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE, businessType)) {
            return resolveActiveOrderVersionUpgradeTitle(variables);
        }
        if (isEdhrExecutionApproval(variables)) {
            return resolveEdhrExecutionApprovalTitle(variables);
        }
        if (isRegistrationCertificateAccessApproval(variables)) {
            return resolveRegistrationCertificateAccessTitle(variables);
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

    private static String resolveEdhrExecutionApprovalTitle(Map<String, Object> variables) {
        StringBuilder title = new StringBuilder("电子批记录审核");
        appendTitlePart(title, firstText(variables.get("edhrExecutionCode"), variables.get("edhrExecutionId")));
        appendLabeledTitlePart(title, "工单", variables.get("workOrderCode"));
        appendLabeledTitlePart(title, "批次", variables.get("batchCode"));
        appendLabeledTitlePart(title, "工序", variables.get("processName"));
        return title.toString();
    }

    private static String resolveEdhrBatchExecutionVoidTitle(Map<String, Object> variables) {
        StringBuilder title = new StringBuilder("电子批记录批次作废");
        appendTitlePart(title, firstText(variables.get("batchExecutionCode"), variables.get("batchExecutionId")));
        appendLabeledTitlePart(title, "批次", variables.get("batchCode"));
        appendLabeledTitlePart(title, "工单", variables.get("workOrderCode"));
        return title.toString();
    }

    private static String resolveRegistrationCertificateAccessTitle(Map<String, Object> variables) {
        StringBuilder title = new StringBuilder(resolveRegistrationCertificateRequestTypeLabel(variables));
        if (isRegistrationCertificateUploadOrRenewalApproval(variables)) {
            return title.toString();
        }
        appendTitlePart(title, firstText(variables.get("requestKey"), variables.get("registrationCertificateAccessRequestId"),
                variables.get("requestId")));
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

    private static String resolveActiveOrderVersionUpgradeTitle(Map<String, Object> variables) {
        StringBuilder title = new StringBuilder("活跃订单升级重启");
        appendTitlePart(title, firstText(variables.get("requestCode"), variables.get("requestId")));
        appendLabeledTitlePart(title, "工单", variables.get("workOrderCode"));
        return title.toString();
    }

    private static String resolveBusinessCode(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        if (isRegistrationCertificateUploadOrRenewalApproval(variables)) {
            return null;
        }
        return firstText(variables.get("businessCode"),
                variables.get("edhrExecutionCode"),
                variables.get("batchExecutionCode"),
                variables.get("requestKey"),
                variables.get("routeCode"),
                variables.get("batchRecordCode"),
                variables.get("batchRecordVersionId"),
                variables.get("batchCode"),
                variables.get("workOrderCode"),
                variables.get("objectId"),
                variables.get("businessKey"));
    }

    private static List<String> resolveBusinessContextTags(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        List<String> tags = new ArrayList<>();
        String businessType = asText(variables.get("businessType"));
        if (Objects.equals(BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE, businessType)) {
            addTag(tags, "批记录", variables.get("batchRecordName"));
            addTag(tags, "版本", variables.get("versionNo"));
            addTag(tags, "源版本", variables.get("sourceVersionNo"));
            addTag(tags, "工艺路线", firstText(variables.get("routeName"), variables.get("routeCode"),
                    variables.get("routeId")));
        } else if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            addTag(tags, "路线编号", variables.get("routeCode"));
            addTag(tags, "路线名称", variables.get("routeName"));
            addTag(tags, "版本", firstText(variables.get("routeVersionNo"), variables.get("objectVersion")));
        } else if (Objects.equals(MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE, businessType)) {
            addTag(tags, "申请单", variables.get("requestCode"));
            addTag(tags, "来源活跃订单", variables.get("sourceActiveOrderId"));
            addTag(tags, "工单", variables.get("workOrderCode"));
            addTag(tags, "目标版本", variables.get("targetVersionsSummary"));
        } else if (Objects.equals(EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE, businessType)) {
            addTag(tags, "工单", variables.get("workOrderCode"));
            addTag(tags, "批次", variables.get("batchCode"));
            addTag(tags, "原因", firstText(variables.get("reasonText"), variables.get("reasonCategory")));
        } else if (isEdhrExecutionApproval(variables)) {
            addTag(tags, "工单", variables.get("workOrderCode"));
            addTag(tags, "批次", variables.get("batchCode"));
            addTag(tags, "工序", variables.get("processName"));
            addTag(tags, "工作站", variables.get("workstationName"));
        } else if (isRegistrationCertificateUploadOrRenewalApproval(variables)) {
            addTag(tags, "注册证编号", variables.get("certificateNo"));
            addTag(tags, "分类", variables.get("classification"));
            addTag(tags, "产品", variables.get("productName"));
            addTag(tags, "所属公司名称", variables.get("ownerCompanyName"));
        } else if (isRegistrationCertificateAccessApproval(variables)) {
            addTag(tags, "申请类型", resolveRegistrationCertificateRequestTypeLabel(variables)
                    .replace("审批", ""));
            addTag(tags, "申请编号", firstText(variables.get("registrationCertificateAccessRequestId"),
                    variables.get("requestId")));
            addTag(tags, "注册证", variables.get("certificateId"));
            addTag(tags, "所属公司", variables.get("ownerCompanyId"));
        }
        return tags.isEmpty() ? null : tags;
    }

    private static String resolveCurrentNodeName(String taskName, String taskDefinitionKey,
                                                 Map<String, Object> variables) {
        String businessType = variables == null ? null : asText(variables.get("businessType"));
        if (Objects.equals(BATCH_RECORD_VERSION_APPROVAL_BUSINESS_TYPE, businessType)) {
            return "批记录升版审核";
        }
        if (Objects.equals(MES_ROUTE_VERSION_PUBLISH_BUSINESS_TYPE, businessType)) {
            return "工艺路线发布审核";
        }
        if (Objects.equals(MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE, businessType)) {
            return "活跃订单升级重启审核";
        }
        if (Objects.equals(EDHR_BATCH_EXECUTION_VOID_BUSINESS_TYPE, businessType)) {
            return "电子批记录批次作废审核";
        }
        if (isEdhrExecutionApproval(variables)) {
            return "电子批记录审核";
        }
        if (isRegistrationCertificateAccessApproval(variables)) {
            return "注册证访问审批";
        }
        if ("approveNode".equals(taskDefinitionKey)) {
            return "审核节点";
        }
        return taskName;
    }

    private static boolean isEdhrExecutionApproval(Map<String, Object> variables) {
        return variables != null
                && hasText(firstText(variables.get("edhrExecutionCode"), variables.get("edhrExecutionId")));
    }

    private static boolean isRegistrationCertificateAccessApproval(Map<String, Object> variables) {
        return variables != null
                && (hasText(firstText(variables.get("registrationCertificateAccessRequestId"),
                variables.get("certificateId")))
                || isRegistrationCertificateRequestType(variables.get("requestType")));
    }

    private static boolean isRegistrationCertificateUploadOrRenewalApproval(Map<String, Object> variables) {
        if (variables == null
                || !REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_TYPE.equals(firstText(variables.get("requestType")))) {
            return false;
        }
        String operation = firstText(variables.get("requestOperation"));
        if (!hasText(operation)) {
            return true;
        }
        if (REGISTRATION_CERTIFICATE_UPLOAD_OPERATION.equals(operation)
                || REGISTRATION_CERTIFICATE_RENEWAL_OPERATION.equals(operation)) {
            return true;
        }
        throw new IllegalArgumentException(
                "APPROVAL_BUSINESS_SUMMARY_VARIABLE_INVALID: registration certificate requestOperation");
    }

    private static boolean hasKnownRegistrationCertificateOperation(Map<String, Object> variables) {
        String operation = variables == null ? null : firstText(variables.get("requestOperation"));
        if (!hasText(operation)) {
            return false;
        }
        if (!REGISTRATION_CERTIFICATE_UPLOAD_OPERATION.equals(operation)
                && !REGISTRATION_CERTIFICATE_RENEWAL_OPERATION.equals(operation)) {
            throw new IllegalArgumentException(
                    "APPROVAL_BUSINESS_SUMMARY_VARIABLE_INVALID: registration certificate requestOperation");
        }
        return true;
    }

    private static boolean isRegistrationCertificateUploadApproval(Map<String, Object> variables) {
        return variables != null
                && REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_TYPE.equals(firstText(variables.get("requestType")))
                && hasText(firstText(variables.get("registrationCertificateAccessRequestId"),
                variables.get("requestId")))
                && hasText(firstText(variables.get("certificateId")));
    }

    private boolean hasRegistrationCertificateUploadApprovalAuthority(Long userId) {
        return permissionApi.hasAnyRoles(userId, REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE)
                && permissionApi.hasAnyPermissions(userId, REGISTRATION_CERTIFICATE_UPLOAD_APPROVAL_PERMISSION);
    }

    private static boolean isRegistrationCertificateRequestType(Object requestType) {
        String type = firstText(requestType);
        if (!hasText(type)) {
            return false;
        }
        return switch (type) {
            case "UPLOAD_CERTIFICATE", "VIEW_OLD_CERTIFICATE", "DOWNLOAD_FILE" -> true;
            default -> false;
        };
    }

    private static String resolveRegistrationCertificateRequestTypeLabel(Map<String, Object> variables) {
        String type = firstText(variables.get("requestType"));
        if (!hasText(type)) {
            return "注册证访问审批";
        }
        return switch (type) {
            case "UPLOAD_CERTIFICATE" -> {
                if (!hasKnownRegistrationCertificateOperation(variables)) {
                    yield "注册证审批";
                }
                yield REGISTRATION_CERTIFICATE_RENEWAL_OPERATION.equals(firstText(variables.get("requestOperation")))
                        ? "注册证延续审批" : "注册证上传审批";
            }
            case "VIEW_OLD_CERTIFICATE" -> "旧注册证查看审批";
            case "DOWNLOAD_FILE" -> "注册证下载审批";
            default -> "注册证访问审批";
        };
    }

    private static void appendTitlePart(StringBuilder title, Object value) {
        String text = asText(value);
        if (hasText(text)) {
            title.append(' ').append(text.trim());
        }
    }

    private static void appendLabeledTitlePart(StringBuilder title, String label, Object value) {
        String text = asText(value);
        if (hasText(text)) {
            title.append(' ').append(label).append(' ').append(text.trim());
        }
    }

    private static void addTag(List<String> tags, String label, Object value) {
        String text = asText(value);
        if (hasText(text)) {
            tags.add(label + "：" + text.trim());
        }
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
        if (Objects.equals(MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE, businessType)) {
            return ACTIVE_ORDER_VERSION_UPGRADE_DETAIL_ROUTE;
        }
        if (isRegistrationCertificateAccessApproval(variables)) {
            String certificateId = firstText(variables.get("certificateId"));
            if (!hasText(certificateId)) {
                throw new IllegalArgumentException(
                        "APPROVAL_BUSINESS_DETAIL_VARIABLE_REQUIRED: registration certificate certificateId");
            }
            return REGISTRATION_CERTIFICATE_DETAIL_ROUTE_PREFIX + certificateId.trim();
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
            return query;
        }
        if (Objects.equals(MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART_BUSINESS_TYPE, businessType)) {
            putIfPresent(query, "businessType", businessType);
            putIfPresent(query, "requestId", variables.get("requestId"));
            putIfPresent(query, "requestCode", variables.get("requestCode"));
            putIfPresent(query, "sourceActiveOrderId", variables.get("sourceActiveOrderId"));
            putIfPresent(query, "workOrderCode", variables.get("workOrderCode"));
            putIfPresent(query, "processInstanceId", processInstanceId);
            return query;
        }
        if (isRegistrationCertificateAccessApproval(variables)) {
            putIfPresent(query, "requestId", firstText(variables.get("registrationCertificateAccessRequestId"),
                    variables.get("requestId")));
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
