package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskResultSupport;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DccApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval";
    private static final String SOURCE_TASK_TYPE = "DCC_CONTROLLED_FILE_TASK";
    private static final String APPROVAL_CENTER_VIEWER_FROM = "approval-center";
    private static final String APPROVAL_CENTER_HANDLING_MODE = "approval";
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.NOTIFICATION,
            ApprovalTaskCapability.AUDIT,
            ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
            ApprovalTaskCapability.EVIDENCE_LEDGER
    );

    private final BpmTaskService bpmTaskService;
    private final BpmProcessInstanceService processInstanceService;
    private final DccControlledFileWorkflowService workflowService;
    private final DccControlledFileMapper controlledFileMapper;
    private final DccFileCategoryMapper fileCategoryMapper;

    public DccApprovalTaskAdapter(BpmTaskService bpmTaskService,
                                  BpmProcessInstanceService processInstanceService,
                                  DccControlledFileWorkflowService workflowService,
                                  DccControlledFileMapper controlledFileMapper,
                                  DccFileCategoryMapper fileCategoryMapper) {
        this.bpmTaskService = bpmTaskService;
        this.processInstanceService = processInstanceService;
        this.workflowService = workflowService;
        this.controlledFileMapper = controlledFileMapper;
        this.fileCategoryMapper = fileCategoryMapper;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.DCC;
    }

    @Override
    public String getModuleName() {
        return "DCC 文控审批";
    }

    @Override
    public String getProviderCode() {
        return "dcc-controlled-file-approval";
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
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: DCC does not support "
                    + context.getViewType());
        };
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        String processInstanceId = requireText(context.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC timeline requires process instance id");
        HistoricProcessInstance processInstance = requireHistoricProcessInstance(
                processInstanceService.getHistoricProcessInstanceMap(Set.of(processInstanceId)), processInstanceId);
        String businessKey = resolveTimelineBusinessKey(context.getBusinessKey(), processInstance);
        requireControlledFileSnapshotForTimeline(businessKey);
        List<HistoricTaskInstance> tasks = requireTimelineTasks(processInstanceId);
        assertTimelineAccess(context, processInstance, tasks);
        return tasks.stream()
                .map(task -> toTimelineEntry(task, businessKey))
                .toList();
    }

    private PageResult<ApprovalTaskSummary> pageTodo(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = toBpmTaskPageReqVO(context);
        PageResult<Task> page = bpmTaskService.getTaskTodoPage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: DCC");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: DCC");
        if (page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        Map<String, ProcessInstance> processInstanceMap = processInstanceService.getProcessInstanceMap(
                toProcessInstanceIds(page.getList(), Task::getProcessInstanceId));
        List<ApprovalTaskSummary> summaries = new ArrayList<>();
        long skipped = 0L;
        for (Task task : page.getList()) {
            ApprovalTaskSummary summary = toSummary(task,
                    requireProcessInstance(processInstanceMap, task.getProcessInstanceId()));
            if (summary == null) {
                skipped++;
                continue;
            }
            summaries.add(summary);
        }
        return new PageResult<>(summaries, adjustedTotal(page.getTotal(), skipped, summaries.size()));
    }

    private PageResult<ApprovalTaskSummary> pageDone(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = toBpmTaskPageReqVO(context);
        PageResult<HistoricTaskInstance> page = bpmTaskService.getTaskDonePage(resolveQueryUserId(context), reqVO);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: DCC");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: DCC");
        if (page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        Map<String, HistoricProcessInstance> processInstanceMap = processInstanceService.getHistoricProcessInstanceMap(
                toProcessInstanceIds(page.getList(), HistoricTaskInstance::getProcessInstanceId));
        List<ApprovalTaskSummary> summaries = new ArrayList<>();
        long skipped = 0L;
        for (HistoricTaskInstance task : page.getList()) {
            ApprovalTaskSummary summary = toSummary(task, requireHistoricProcessInstance(processInstanceMap,
                    task.getProcessInstanceId()));
            if (summary == null) {
                skipped++;
                continue;
            }
            summaries.add(summary);
        }
        return new PageResult<>(summaries, adjustedTotal(page.getTotal(), skipped, summaries.size()));
    }

    private BpmTaskPageReqVO toBpmTaskPageReqVO(ApprovalTaskQueryContext context) {
        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(context.getPageNo() == null ? 1 : context.getPageNo());
        reqVO.setPageSize(context.getPageSize() == null ? 10 : context.getPageSize());
        reqVO.setProcessDefinitionKey(PROCESS_DEFINITION_KEY);
        reqVO.setName(context.getKeyword());
        return reqVO;
    }

    private ApprovalTaskSummary toSummary(Task task, ProcessInstance processInstance) {
        String businessKey = requireBusinessKey(processInstance.getBusinessKey());
        if (!isDccControlledFileBusinessKey(businessKey)) {
            return null;
        }
        DccControlledFileRespVO file = requireControlledFile(businessKey);
        return ApprovalTaskSummary.builder()
                .id("DCC:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .businessTitle(file.getTitle())
                .businessCode(file.getFileNumber())
                .businessStatus(file.getStatus())
                .businessContextTags(buildDccBusinessContextTags(file, task.getName()))
                .businessDeleted(Boolean.FALSE)
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(task.getName())
                .initiatorUserId(parseLong(processInstance.getStartUserId()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/dcc/controlled-file/detail/" + businessKey)
                .detailQuery(approvalCenterHandlingDetailQuery(task))
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toSummary(HistoricTaskInstance task, HistoricProcessInstance processInstance) {
        String businessKey = requireBusinessKey(processInstance.getBusinessKey());
        if (!isDccControlledFileBusinessKey(businessKey)) {
            return null;
        }
        DccControlledFileDO file = requireControlledFileSnapshot(businessKey);
        if (file == null) {
            return toDeletedHistoricalSummary(task, processInstance, businessKey);
        }
        ApprovalTaskReviewResult approvalResult = resolveApprovalResult(task, "DCC done ");
        return ApprovalTaskSummary.builder()
                .id("DCC:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .businessTitle(file.getTitle())
                .businessCode(file.getFileNumber())
                .businessStatus(file.getStatus())
                .businessContextTags(buildDccBusinessContextTags(file, task.getName()))
                .businessDeleted(Boolean.TRUE.equals(file.getDeleted()))
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(task.getName())
                .initiatorUserId(parseLong(processInstance.getStartUserId()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .taskCompletedAt(toLocalDateTime(task.getEndTime()))
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        FlowableUtils.getTaskReason(task)))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/dcc/controlled-file/detail/" + businessKey)
                .detailQuery(approvalCenterViewerDetailQuery())
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toDeletedHistoricalSummary(HistoricTaskInstance task,
                                                           HistoricProcessInstance processInstance,
                                                           String businessKey) {
        ApprovalTaskReviewResult approvalResult = resolveApprovalResult(task, "DCC deleted done ");
        return ApprovalTaskSummary.builder()
                .id("DCC:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .businessTitle("已删除文控文件")
                .businessCode(businessKey)
                .businessStatus("DELETED")
                .businessContextTags(buildDeletedDccBusinessContextTags(businessKey, task.getName()))
                .businessDeleted(Boolean.TRUE)
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(task.getName())
                .initiatorUserId(parseLong(processInstance.getStartUserId()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .taskCompletedAt(toLocalDateTime(task.getEndTime()))
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        FlowableUtils.getTaskReason(task)))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/dcc/controlled-file/detail/" + businessKey)
                .detailQuery(approvalCenterViewerDetailQuery())
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(CAPABILITIES)
                .build();
    }

    private static Map<String, String> approvalCenterViewerDetailQuery() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("viewer", "1");
        query.put("from", APPROVAL_CENTER_VIEWER_FROM);
        return query;
    }

    private static Map<String, String> approvalCenterHandlingDetailQuery(Task task) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("handling", APPROVAL_CENTER_HANDLING_MODE);
        query.put("from", APPROVAL_CENTER_VIEWER_FROM);
        query.put("processInstanceId", requireText(task.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC handling route requires process instance id"));
        query.put("taskId", requireText(task.getId(),
                "APPROVAL_TASK_ID_REQUIRED: DCC handling route requires task id"));
        return query;
    }

    private List<String> buildDccBusinessContextTags(DccControlledFileRespVO file, String currentNodeName) {
        DccFileCategoryDO category = resolveCategory(file.getCategoryId());
        return buildDccBusinessContextTags(file.getFileNumber(), file.getVersionNo(),
                resolveCategoryLabel(category, file.getCategoryId()), currentNodeName, file.getStampedFileId(),
                category == null ? null : category.getDistributionRequired());
    }

    private List<String> buildDccBusinessContextTags(DccControlledFileDO file, String currentNodeName) {
        DccFileCategoryDO category = resolveCategory(file.getCategoryId());
        return buildDccBusinessContextTags(file.getFileNumber(), file.getVersionNo(),
                resolveCategoryLabel(category, file.getCategoryId()), currentNodeName, file.getStampedFileId(),
                category == null ? null : category.getDistributionRequired());
    }

    private List<String> buildDeletedDccBusinessContextTags(String businessKey, String currentNodeName) {
        return List.of(
                "文件编号：" + requireText(businessKey, "APPROVAL_BUSINESS_KEY_REQUIRED: DCC deleted business key"),
                "版本：-",
                "分类：已删除记录",
                "当前节点：" + requireText(currentNodeName, "APPROVAL_TASK_NAME_REQUIRED: DCC deleted task name"),
                "盖章：记录已删除",
                "分发：记录已删除");
    }

    private List<String> buildDccBusinessContextTags(String fileNumber,
                                                     String versionNo,
                                                     String categoryLabel,
                                                     String currentNodeName,
                                                     Long stampedFileId,
                                                     Boolean distributionRequired) {
        return List.of(
                "文件编号：" + requireText(fileNumber,
                        "APPROVAL_BUSINESS_CODE_REQUIRED: DCC controlled file number is required"),
                "版本：" + requireText(versionNo,
                        "APPROVAL_BUSINESS_VERSION_REQUIRED: DCC controlled file version is required"),
                "分类：" + requireText(categoryLabel,
                        "APPROVAL_BUSINESS_CATEGORY_REQUIRED: DCC controlled file category is required"),
                "当前节点：" + requireText(currentNodeName,
                        "APPROVAL_TASK_NAME_REQUIRED: DCC task name is required"),
                stampedFileId == null ? "盖章：需要" : "盖章：已生成",
                Boolean.TRUE.equals(distributionRequired) ? "分发：需要" : "分发：不需要");
    }

    private DccFileCategoryDO resolveCategory(Long categoryId) {
        return categoryId == null ? null : fileCategoryMapper.selectById(categoryId);
    }

    private String resolveCategoryLabel(DccFileCategoryDO category, Long categoryId) {
        if (category != null && category.getName() != null && !category.getName().isBlank()) {
            return category.getName();
        }
        return categoryId == null ? null : "缺失类别#" + categoryId;
    }

    private ApprovalTaskTimelineEntry toTimelineEntry(HistoricTaskInstance task, String businessKey) {
        boolean completed = task.getEndTime() != null;
        return ApprovalTaskTimelineEntry.builder()
                .id(task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .nodeCode(task.getTaskDefinitionKey())
                .nodeName(task.getName())
                .action(completed ? "APPROVED" : "CURRENT")
                .actionLabel(completed ? "审批通过" : "处理中")
                .actorUserId(parseLong(task.getAssignee()))
                .actedAt(toLocalDateTime(completed ? task.getEndTime() : task.getCreateTime()))
                .status(completed ? "DONE" : "RUNNING")
                .evidenceType("FLOWABLE_HISTORY")
                .domainReferenceId(task.getId())
                .build();
    }

    private void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: DCC does not support "
                    + sourceTaskType);
        }
    }

    private String resolveTimelineBusinessKey(String requestedBusinessKey, HistoricProcessInstance processInstance) {
        String actualBusinessKey = requireBusinessKey(processInstance.getBusinessKey());
        if (requestedBusinessKey == null || requestedBusinessKey.isBlank()) {
            return actualBusinessKey;
        }
        if (!requestedBusinessKey.equals(actualBusinessKey)) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_MISMATCH: DCC timeline business key "
                    + requestedBusinessKey + " does not match process " + actualBusinessKey);
        }
        return requestedBusinessKey;
    }

    private List<HistoricTaskInstance> requireTimelineTasks(String processInstanceId) {
        List<HistoricTaskInstance> tasks = bpmTaskService.getTaskListByProcessInstanceId(processInstanceId, true);
        Objects.requireNonNull(tasks, "APPROVAL_TIMELINE_SOURCE_REQUIRED: DCC BPM historic task list is required");
        if (tasks.isEmpty()) {
            throw new IllegalStateException("APPROVAL_TIMELINE_SOURCE_REQUIRED: DCC BPM process has no historic tasks "
                    + processInstanceId);
        }
        return tasks;
    }

    private void assertTimelineAccess(ApprovalTaskTimelineQueryContext context,
                                      HistoricProcessInstance processInstance,
                                      List<HistoricTaskInstance> tasks) {
        if (context.isGlobalView()) {
            return;
        }
        boolean handledByLoginUser = tasks.stream()
                .anyMatch(task -> Objects.equals(context.getLoginUserId(), parseLong(task.getAssignee())));
        boolean initiatedByLoginUser = Objects.equals(context.getLoginUserId(),
                parseLong(processInstance.getStartUserId()));
        BpmTaskRespVO todoTask = context.getSourceTaskId() == null || context.getSourceTaskId().isBlank()
                ? null : bpmTaskService.getTodoTask(context.getLoginUserId(), context.getSourceTaskId(),
                context.getProcessInstanceId());
        if (!handledByLoginUser && !initiatedByLoginUser && todoTask == null) {
            throw new IllegalStateException("APPROVAL_TIMELINE_ACCESS_DENIED: DCC timeline is not visible to login user");
        }
    }

    private static ProcessInstance requireProcessInstance(Map<String, ProcessInstance> processInstanceMap,
                                                          String processInstanceId) {
        ProcessInstance processInstance = processInstanceMap.get(processInstanceId);
        if (processInstance == null) {
            throw new IllegalStateException("APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC BPM task missing process instance "
                    + processInstanceId);
        }
        return processInstance;
    }

    private static HistoricProcessInstance requireHistoricProcessInstance(
            Map<String, HistoricProcessInstance> processInstanceMap, String processInstanceId) {
        HistoricProcessInstance processInstance = processInstanceMap.get(processInstanceId);
        if (processInstance == null) {
            throw new IllegalStateException("APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC BPM task missing historic process instance "
                    + processInstanceId);
        }
        return processInstance;
    }

    private static String requireBusinessKey(String businessKey) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: DCC BPM task missing controlled file business key");
        }
        return businessKey;
    }

    private DccControlledFileRespVO requireControlledFile(String businessKey) {
        Long fileId = parseBusinessKey(businessKey);
        DccControlledFileRespVO file = workflowService.getControlledFile(fileId);
        if (file == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file not found "
                    + businessKey);
        }
        return file;
    }

    private DccControlledFileDO requireControlledFileSnapshot(String businessKey) {
        Long fileId = parseBusinessKey(businessKey);
        return controlledFileMapper.selectByIdIncludingDeleted(fileId);
    }

    private DccControlledFileDO requireControlledFileSnapshotForTimeline(String businessKey) {
        DccControlledFileDO file = requireControlledFileSnapshot(businessKey);
        if (file == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file summary snapshot not found "
                    + businessKey);
        }
        return file;
    }

    private static Long parseBusinessKey(String businessKey) {
        try {
            return Long.valueOf(businessKey);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_INVALID: DCC controlled file business key must be numeric "
                    + businessKey, ex);
        }
    }

    private static boolean isDccControlledFileBusinessKey(String businessKey) {
        return businessKey != null && businessKey.chars().allMatch(Character::isDigit);
    }

    private static long adjustedTotal(Long originalTotal, long skipped, int visibleSize) {
        if (originalTotal == null) {
            return visibleSize;
        }
        return Math.max(visibleSize, originalTotal - skipped);
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static ApprovalTaskReviewResult resolveApprovalResult(HistoricTaskInstance task, String sourcePrefix) {
        return ApprovalTaskResultSupport.fromBpmTaskStatus(FlowableUtils.getTaskStatus(task),
                sourcePrefix + task.getId());
    }

    private static Long resolveQueryUserId(ApprovalTaskQueryContext context) {
        return context.isGlobalView() ? null : context.getLoginUserId();
    }

    private static <T> Set<String> toProcessInstanceIds(Collection<T> rows,
                                                        java.util.function.Function<T, String> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
