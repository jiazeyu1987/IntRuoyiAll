package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskResultSupport;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DccApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String EXTERNAL_REVIEW_PROCESS_DEFINITION_KEY = "dcc-external-file-review";
    private static final List<String> DCC_PROCESS_DEFINITION_KEYS = buildDccProcessDefinitionKeys();
    private static final String SOURCE_TASK_TYPE = "DCC_CONTROLLED_FILE_TASK";
    private static final String APPROVAL_CENTER_VIEWER_FROM = "approval-center";
    private static final String APPROVAL_CENTER_HANDLING_MODE = "approval";
    private static final int SOURCE_PAGE_SIZE = 200;
    private static final Set<String> PROCESS_IN_MODULE_ACTIONS = Set.of("PROCESS_IN_MODULE");
    private static final Set<String> QUICK_REVIEW_ACTIONS = Set.of("APPROVE", "REJECT", "PROCESS_IN_MODULE");
    private static final Set<String> FORM_CENTER_OBSOLETE_ACTIONS = PROCESS_IN_MODULE_ACTIONS;
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
    private final DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    private final DccProjectProductTaskDelegate projectApplications;
    private final DccOfflineTrainingTaskDelegate offlineTraining;

    @jakarta.annotation.Resource
    private DccControlledFileSignatureMapper signatureMapper;

    private static List<String> buildDccProcessDefinitionKeys() {
        List<String> keys = new ArrayList<>(DccControlledFileProcessDefinitionKeys.APPROVAL_CENTER_KEYS);
        keys.add(EXTERNAL_REVIEW_PROCESS_DEFINITION_KEY);
        return List.copyOf(keys);
    }

    public DccApprovalTaskAdapter(BpmTaskService bpmTaskService,
                                  BpmProcessInstanceService processInstanceService,
                                  DccControlledFileWorkflowService workflowService,
                                  DccControlledFileMapper controlledFileMapper,
                                  DccFileCategoryMapper fileCategoryMapper,
                                  DccControlledFileRouteSnapshotMapper routeSnapshotMapper,
                                  DccProjectProductTaskDelegate projectApplications,
                                  DccOfflineTrainingTaskDelegate offlineTraining) {
        this.bpmTaskService = bpmTaskService;
        this.processInstanceService = processInstanceService;
        this.workflowService = workflowService;
        this.controlledFileMapper = controlledFileMapper;
        this.fileCategoryMapper = fileCategoryMapper;
        this.routeSnapshotMapper = routeSnapshotMapper;
        this.projectApplications = Objects.requireNonNull(projectApplications);
        this.offlineTraining = Objects.requireNonNull(offlineTraining);
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
        List<ApprovalTaskSummary> nativeTasks = new ArrayList<>(projectApplications.list(context));
        nativeTasks.addAll(offlineTraining.list(context));
        if (context.getViewType() == ApprovalTaskViewType.TODO
                && cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow.from(context).limit() == 0) {
            var files = centerQuery.todo(context, "DCC_TASK");
            return new PageResult<>(List.of(), Math.addExact(files.getTotal(), nativeTasks.size()));
        }
        if (nativeTasks.isEmpty()) {
            return switch (context.getViewType()) {
                case TODO -> pageTodo(context);
                case DONE -> pageDone(context);
                default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: " + context.getViewType());
            };
        }
        ApprovalTaskQueryContext fullContext = ApprovalTaskQueryContext.of(context.getLoginUserId(), context.getViewType(),
                context.getModuleCode(), context.getKeyword(), 1, Integer.MAX_VALUE, context.isGlobalView());
        PageResult<ApprovalTaskSummary> files = switch (context.getViewType()) {
            case TODO -> pageTodo(fullContext);
            case DONE -> pageDone(fullContext);
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: DCC does not support "
                    + context.getViewType());
        };
        List<ApprovalTaskSummary> combined = new ArrayList<>(files.getList());
        if (files.getTotal() != files.getList().size()) {
            throw new IllegalStateException("APPROVAL_ADAPTER_SOURCE_PAGE_INCONSISTENT: incomplete DCC merge source");
        }
        combined.addAll(nativeTasks);
        combined = new ArrayList<>(cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl
                .sortRows(combined, context.getViewType()));
        VisibleWindow window = visibleWindow(context);
        int from = (int) Math.min(window.fromIndex(), combined.size());
        int to = (int) Math.min(window.toIndex(), combined.size());
        return new PageResult<>(combined.subList(from, to), (long) combined.size());
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        if (DccProjectProductTaskDelegate.supports(context.getSourceTaskType())) return projectApplications.timeline(context);
        requireSourceTaskType(context.getSourceTaskType());
        String processInstanceId = requireText(context.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC timeline requires process instance id");
        HistoricProcessInstance processInstance = requireHistoricProcessInstance(
                processInstanceService.getHistoricProcessInstanceMap(Set.of(processInstanceId)), processInstanceId);
        String businessKey = resolveTimelineBusinessKey(context.getBusinessKey(), processInstance);
        requireControlledFileSnapshotForTimeline(businessKey);
        List<HistoricTaskInstance> tasks = requireTimelineTasks(processInstanceId);
        boolean separateSignoff = isSignoffProcess(processInstance.getProcessDefinitionKey())
                && tasks.stream().anyMatch(task -> "MATRIX_REVIEW".equals(task.getTaskDefinitionKey()));
        List<DccControlledFileSignatureDO> signatures = separateSignoff
                ? signatureMapper.selectListByControlledFileId(parseBusinessKey(businessKey)) : List.of();
        assertTimelineAccess(context, processInstance, tasks, signatures, businessKey);
        return tasks.stream()
                .flatMap(task -> {
                    var entry = toTimelineEntry(task, businessKey);
                    return (separateSignoff && "MATRIX_REVIEW".equals(task.getTaskDefinitionKey())
                            ? signoffTimelineEntries(task, entry, signatures) : List.of(entry)).stream();
                })
                .toList();
    }

    private static boolean isSignoffProcess(String processKey) {
        return DccControlledFileProcessDefinitionKeys.UPLOAD.equals(processKey)
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(processKey)
                || DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(processKey);
    }

    // A shared engine task contains two business stages; only a persisted signature completes assignment.
    static List<ApprovalTaskTimelineEntry> signoffTimelineEntries(HistoricTaskInstance task,
            ApprovalTaskTimelineEntry review, List<DccControlledFileSignatureDO> signatures) {
        var assignments = signatures.stream()
                .filter(signature -> Objects.equals(review.getBusinessKey(), String.valueOf(signature.getControlledFileId()))
                        && Objects.equals(task.getProcessInstanceId(), signature.getProcessInstanceId())
                        && Objects.equals(task.getId(), signature.getTaskId())
                        && "ASSIGN".equals(signature.getActionType()))
                .toList();
        if (assignments.size() > 1) throw new IllegalStateException("DCC_TIMELINE_ASSIGNMENT_SIGNATURE_DUPLICATED");
        if (assignments.isEmpty()) {
            if ("APPROVED".equals(review.getAction())
                    || task.getTaskLocalVariables().get("dccAssignmentSignatureId") != null)
                throw new IllegalStateException("DCC_TIMELINE_ASSIGNMENT_SIGNATURE_MISSING");
            return List.of(review.setNodeCode("MATRIX_REVIEW_ASSIGN").setNodeName("部门负责人签名指派"));
        }
        var signature = assignments.get(0);
        if (signature.getId() == null || signature.getActorId() == null || signature.getSignedAt() == null
                || task.getCreateTime() == null
                || !"MATRIX_REVIEW_ASSIGN".equals(signature.getMeaningCode())
                || !"VALID".equals(signature.getEvidenceStatus())
                || signature.getSignedAt().isBefore(toLocalDateTime(task.getCreateTime()))
                || (task.getEndTime() != null && signature.getSignedAt().isAfter(toLocalDateTime(task.getEndTime()))))
            throw new IllegalStateException("DCC_TIMELINE_ASSIGNMENT_SIGNATURE_INVALID");
        var assignment = ApprovalTaskTimelineEntry.builder()
                .id(task.getId() + ":ASSIGN").moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE).sourceTaskId(task.getId()).businessKey(review.getBusinessKey())
                .nodeCode("MATRIX_REVIEW_ASSIGN").nodeName("部门负责人签名指派")
                .action("APPROVED").actionLabel("指派完成").status("DONE")
                .actorUserId(signature.getActorId()).actedAt(signature.getSignedAt()).comment(signature.getComment())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime())).taskCompletedAt(signature.getSignedAt())
                .taskCompletedByUserId(signature.getActorId())
                .evidenceType("DCC_SIGNATURE").domainReferenceId(signature.getId().toString())
                .evidenceId(signature.getId().toString()).build();
        review.setNodeName("实际会签人会签").setTaskCreatedAt(signature.getSignedAt());
        if ("CURRENT".equals(review.getAction())) review.setActedAt(signature.getSignedAt());
        return List.of(assignment, review);
    }

    static String businessTaskName(org.flowable.task.api.TaskInfo task, String processKey) {
        if (!isSignoffProcess(processKey) || !"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) return task.getName();
        return task.getTaskLocalVariables().get("dccAssignmentSignatureId") == null
                ? "部门负责人签名指派" : "实际会签人会签";
    }

    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService centerQuery;

    @Override
    public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,
            cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow window) {
        return page(context.withWindow(window));
    }

    private PageResult<ApprovalTaskSummary> pageTodo(ApprovalTaskQueryContext context) {
        var page=centerQuery.todo(context,"DCC_TASK");
        if (page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        var processes=processInstanceService.getProcessInstanceMap(toProcessInstanceIds(page.getList(),Task::getProcessInstanceId));
        var rows=page.getList().stream().map(task->Objects.requireNonNull(toSummary(task,
                requireProcessInstance(processes,task.getProcessInstanceId())),"APPROVAL_DCC_FORMAL_PROJECTION_MISMATCH")).toList();
        return new PageResult<>(rows,page.getTotal());
    }
    private PageResult<ApprovalTaskSummary> pageDone(ApprovalTaskQueryContext context) {
        var page=centerQuery.done(context,"DCC_TASK");
        if (page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        var processes=processInstanceService.getHistoricProcessInstanceMap(toProcessInstanceIds(page.getList(),HistoricTaskInstance::getProcessInstanceId));
        var rows=page.getList().stream().map(task->Objects.requireNonNull(toSummary(task,
                requireHistoricProcessInstance(processes,task.getProcessInstanceId())),"APPROVAL_DCC_FORMAL_PROJECTION_MISMATCH")).toList();
        return new PageResult<>(rows,page.getTotal());
    }

    private BpmTaskPageReqVO toBpmTaskPageReqVO(ApprovalTaskQueryContext context, int pageNo, int pageSize,
                                                String processDefinitionKey) {
        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        reqVO.setProcessDefinitionKey(processDefinitionKey);
        return reqVO;
    }

    private static boolean matchesKeyword(ApprovalTaskSummary summary, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        return containsIgnoreCase(summary.getBusinessTitle(), normalizedKeyword)
                || containsIgnoreCase(summary.getBusinessCode(), normalizedKeyword)
                || containsIgnoreCase(summary.getBusinessKey(), normalizedKeyword);
    }

    private static boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase().contains(normalizedKeyword);
    }

    private ApprovalTaskSummary toSummary(Task task, ProcessInstance processInstance) {
        String businessKey = resolveControlledFileBusinessKey(processInstance);
        if (!isDccControlledFileBusinessKey(businessKey)) {
            return null;
        }
        DccControlledFileDO file = requireControlledFileSnapshotForTodo(businessKey);
        return ApprovalTaskSummary.builder()
                .id("DCC:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .businessTitle(file.getTitle())
                .businessCode(file.getFileNumber())
                .businessStatus(file.getStatus())
                .businessContextTags(buildDccBusinessContextTags(file, businessTaskName(task, processInstance.getProcessDefinitionKey())))
                .businessDeleted(Boolean.FALSE)
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(businessTaskName(task, processInstance.getProcessDefinitionKey()))
                .initiatorUserId(parseLong(processInstance.getStartUserId()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/dcc/controlled-file/detail/" + businessKey)
                .detailQuery(approvalCenterHandlingDetailQuery(task))
                .availableActions(resolveTodoAvailableActions(processInstance.getProcessDefinitionKey(),
                        task.getTaskDefinitionKey(), file.getStatus()))
                .capabilities(CAPABILITIES)
                .build();
    }

    private ApprovalTaskSummary toSummary(HistoricTaskInstance task, HistoricProcessInstance processInstance) {
        String businessKey = resolveControlledFileBusinessKey(processInstance);
        if (!isDccControlledFileBusinessKey(businessKey)) {
            return null;
        }
        DccControlledFileDO file = requireControlledFileSnapshot(businessKey);
        if (file == null) {
            return toDeletedHistoricalSummary(task, processInstance, businessKey);
        }
        ApprovalTaskReviewResult approvalResult = resolveApprovalResult(task, "DCC done ");
        Map<String, String> detailQuery = Boolean.TRUE.equals(file.getDeleted())
                ? approvalCenterViewerDetailQuery() : approvalCenterHistoryDetailQuery(file, task, processInstance);
        return ApprovalTaskSummary.builder()
                .id("DCC:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .businessTitle(file.getTitle())
                .businessCode(file.getFileNumber())
                .businessStatus(file.getStatus())
                .businessContextTags(buildDccBusinessContextTags(file, businessTaskName(task, processInstance.getProcessDefinitionKey())))
                .businessDeleted(Boolean.TRUE.equals(file.getDeleted()))
                .currentNodeCode(task.getTaskDefinitionKey())
                .currentNodeName(businessTaskName(task, processInstance.getProcessDefinitionKey()))
                .initiatorUserId(parseLong(processInstance.getStartUserId()))
                .processInstanceId(task.getProcessInstanceId())
                .taskCreatedAt(toLocalDateTime(task.getCreateTime()))
                .taskCompletedAt(toLocalDateTime(task.getEndTime()))
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        FlowableUtils.getTaskReason(task)))
                .requiresSignature(Boolean.TRUE)
                .detailRoute(detailQuery == null ? null : "/dcc/controlled-file/detail/" + businessKey)
                .detailQuery(detailQuery == null ? Map.of() : detailQuery)
                .availableActions(PROCESS_IN_MODULE_ACTIONS)
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
                .availableActions(PROCESS_IN_MODULE_ACTIONS)
                .capabilities(CAPABILITIES)
                .build();
    }

    @Override
    public void review(ApprovalTaskReviewContext context) {
        if (DccProjectProductTaskDelegate.supports(context.getSourceTaskType())) {
            projectApplications.review(context);
            return;
        }
        requireSourceTaskType(context.getSourceTaskType());
        Long fileId = parseBusinessKey(requireText(context.getBusinessKey(),
                "APPROVAL_BUSINESS_KEY_REQUIRED: DCC review requires controlled file business key"));
        String taskId = requireText(context.getSourceTaskId(),
                "APPROVAL_TASK_ID_REQUIRED: DCC review requires source task id");
        String password = requireText(context.getSignaturePassword(),
                "APPROVAL_SIGNATURE_PASSWORD_REQUIRED: DCC review requires signature password");
        if (context.getResult() == ApprovalTaskReviewResult.APPROVE) {
            DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
            reqVO.setTaskId(taskId);
            reqVO.setPassword(password);
            reqVO.setReason(context.getReason());
            workflowService.approveTask(context.getLoginUserId(), fileId, reqVO);
            return;
        }
        if (context.getResult() == ApprovalTaskReviewResult.REJECT) {
            DccControlledFileRejectTaskReqVO reqVO = new DccControlledFileRejectTaskReqVO();
            reqVO.setTaskId(taskId);
            reqVO.setPassword(password);
            reqVO.setReason(requireText(cn.iocoder.yudao.module.bpm.approval.core.ApprovalReviewReason.normalize(context.getReason()),
                    "APPROVAL_REJECT_REASON_REQUIRED: DCC reject requires reason"));
            workflowService.rejectTask(context.getLoginUserId(), fileId, reqVO);
            return;
        }
        throw new IllegalArgumentException("APPROVAL_RESULT_UNSUPPORTED: DCC does not support "
                + context.getResult());
    }

    private Map<String, List<Long>> buildFrozenStageAssigneeMap(Long controlledFileId) {
        List<DccControlledFileRouteSnapshotDO> snapshots = routeSnapshotMapper
                .selectListByControlledFileId(controlledFileId);
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("DCC route snapshot is required for obsolete approval "
                    + controlledFileId);
        }
        return snapshots.stream()
                .filter(snapshot -> snapshot.getStageCode() != null && !snapshot.getStageCode().isBlank())
                .collect(Collectors.toMap(
                        DccControlledFileRouteSnapshotDO::getStageCode,
                        this::parseResolvedUserIds,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private List<Long> parseResolvedUserIds(DccControlledFileRouteSnapshotDO snapshot) {
        if (snapshot == null || snapshot.getResolvedUserIds() == null
                || snapshot.getResolvedUserIds().isBlank()) {
            return List.of();
        }
        return Arrays.stream(snapshot.getResolvedUserIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private boolean isObsoleteApprovalTask(ApprovalTaskReviewContext context) {
        String processInstanceId = requireText(context.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_REQUIRED: DCC review requires process instance id");
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        return processInstance != null
                && DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS
                .contains(processInstance.getProcessDefinitionKey());
    }

    private static Map<String, String> approvalCenterHistoryDetailQuery(DccControlledFileDO file,
            HistoricTaskInstance task, HistoricProcessInstance process) {
        String definition = process.getProcessDefinitionKey();
        String applicationType;
        if (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(definition)) applicationType = "UPLOAD";
        else if (DccControlledFileProcessDefinitionKeys.REVISION.equals(definition)) applicationType = "REVISION";
        else if (DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL.equals(definition)
                && task.getProcessInstanceId() != null
                && task.getProcessInstanceId().equals(file.getProcessInstanceId())) {
            applicationType = "NEW".equals(file.getChangeType()) ? "UPLOAD"
                    : "REVISION".equals(file.getChangeType()) ? "REVISION" : null;
            if (applicationType == null) return null;
        }
        else if (definition != null && DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS.contains(definition)) applicationType = "OBSOLETE";
        else return null;
        if (file.getId() == null || file.getId() <= 0 || task.getEndTime() == null
                || !isExactHistoryLinkText(file.getVersionNo(), 128)
                || !isExactHistoryLinkText(task.getProcessInstanceId(), 64)
                || !isExactHistoryLinkText(task.getId(), 64)) return null;
        var context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                file.getId().toString(), file.getVersionNo(), applicationType, task.getProcessInstanceId(), task.getId());
        Map<String, String> query = approvalCenterViewerDetailQuery();
        query.put("bodyReadContext", context.wire());
        return query;
    }

    private static boolean isExactHistoryLinkText(String value, int limit) {
        return value != null && !value.isBlank() && value.equals(value.strip()) && value.length() <= limit
                && value.chars().noneMatch(c -> c < 32 || c == 127);
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

    private static Set<String> resolveTodoAvailableActions(String processDefinitionKey,
                                                           String taskDefinitionKey,
                                                           String fileStatus) {
        // Native approval requires the exact task/BPM owner-selection dialog, absent from quick-review context.
        if ("MATRIX_APPROVAL".equals(taskDefinitionKey)
                && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(processDefinitionKey)
                    || DccControlledFileProcessDefinitionKeys.REVISION.equals(processDefinitionKey)))
            return PROCESS_IN_MODULE_ACTIONS;
        if ("MATRIX_REVIEW".equals(taskDefinitionKey)
                && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(processDefinitionKey)
                    || DccControlledFileProcessDefinitionKeys.REVISION.equals(processDefinitionKey)
                    || DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(processDefinitionKey)))
            return PROCESS_IN_MODULE_ACTIONS;
        if (processDefinitionKey != null
                && DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS.contains(processDefinitionKey)) {
            return FORM_CENTER_OBSOLETE_ACTIONS;
        }
        if (EXTERNAL_REVIEW_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return PROCESS_IN_MODULE_ACTIONS;
        }
        if (isDocControlFinalApprovalTask(taskDefinitionKey, fileStatus)) {
            return PROCESS_IN_MODULE_ACTIONS;
        }
        if (isQuickReviewTask(taskDefinitionKey, fileStatus)) {
            return QUICK_REVIEW_ACTIONS;
        }
        return PROCESS_IN_MODULE_ACTIONS;
    }

    private static boolean isDocControlFinalApprovalTask(String taskDefinitionKey, String fileStatus) {
        return DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode().equals(taskDefinitionKey)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(fileStatus)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(fileStatus);
    }

    private static boolean isQuickReviewTask(String taskDefinitionKey, String fileStatus) {
        return DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(taskDefinitionKey)
                || DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode().equals(taskDefinitionKey)
                || DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(fileStatus)
                || DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(fileStatus);
    }

    private List<String> buildDccBusinessContextTags(DccControlledFileRespVO file, String currentNodeName) {
        DccFileCategoryDO category = resolveCategory(file.getCategoryId());
        return buildDccBusinessContextTags(file.getFileNumber(), file.getVersionNo(),
                resolveCategoryLabel(category, file.getCategoryId()), currentNodeName,
                file.getStampedArtifactAvailable(),
                category == null ? null : category.getDistributionRequired());
    }

    List<String> buildDccBusinessContextTags(DccControlledFileDO file, String currentNodeName) {
        DccFileCategoryDO category = resolveCategory(file.getCategoryId());
        return buildDccBusinessContextTags(file.getFileNumber(), historicalVersionLabel(file.getVersionNo()),
                resolveHistoricalCategoryLabel(category, file.getCategoryId()), currentNodeName,
                file.getStampedFileId() != null,
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
                                                     Boolean stampedArtifactAvailable,
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
                Boolean.TRUE.equals(stampedArtifactAvailable) ? "盖章：已生成" : "盖章：需要",
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

    private String resolveHistoricalCategoryLabel(DccFileCategoryDO category, Long categoryId) {
        String categoryLabel = resolveCategoryLabel(category, categoryId);
        return categoryLabel == null ? "-" : categoryLabel;
    }

    private ApprovalTaskTimelineEntry toTimelineEntry(HistoricTaskInstance task, String businessKey) {
        Integer taskStatus = FlowableUtils.getTaskStatus(task);
        var decision = cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.valueOf(taskStatus);
        if (decision == null) throw new IllegalStateException("DCC_TIMELINE_TASK_STATUS_MISSING_OR_UNKNOWN: " + task.getId());
        String action = switch (decision) {
            case APPROVE -> "APPROVED";
            case REJECT -> "REJECTED";
            case CANCEL -> "CANCELLED";
            case RETURN -> "RETURNED";
            case SKIP -> "SKIPPED";
            case NOT_START, RUNNING, APPROVING, WAIT -> "CURRENT";
        };
        String label = switch (decision) {
            case APPROVE -> approvedNodeLabel(task.getTaskDefinitionKey());
            case REJECT -> "驳回";
            case CANCEL -> "取消";
            case RETURN -> "退回";
            case SKIP -> "跳过";
            case NOT_START, RUNNING, APPROVING, WAIT -> "处理中";
        };
        boolean completed = !"CURRENT".equals(action);
        return ApprovalTaskTimelineEntry.builder()
                .id(task.getId())
                .moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(task.getId())
                .businessKey(businessKey)
                .nodeCode(task.getTaskDefinitionKey())
                .nodeName(task.getName())
                .action(action)
                .actionLabel(label)
                .actorUserId(parseLong(task.getAssignee()))
                .actedAt(toLocalDateTime(completed ? task.getEndTime() : task.getCreateTime()))
                .status(completed ? "DONE" : "RUNNING")
                .evidenceType("FLOWABLE_HISTORY")
                .domainReferenceId(task.getId())
                .build();
    }

    private static String approvedNodeLabel(String nodeCode) {
        if (nodeCode == null) throw new IllegalStateException("DCC_TIMELINE_NODE_REQUIRED");
        return switch (nodeCode) {
            case "SUPERVISOR_REVIEW", "DOC_CONTROL_REVIEW", "MATRIX_REVIEW" -> "审核通过";
            case "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL" -> "批准通过";
            case "DELAYED_EFFECTIVE_CONFIRM" -> "延迟生效已确认";
            case "APPLICANT_REWORK" -> "已重新提交";
            default -> "任务通过";
        };
    }

    private void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: DCC does not support "
                    + sourceTaskType);
        }
    }

    private String resolveTimelineBusinessKey(String requestedBusinessKey, HistoricProcessInstance processInstance) {
        String actualBusinessKey = resolveControlledFileBusinessKey(processInstance);
        if (requestedBusinessKey == null || requestedBusinessKey.isBlank()) {
            return actualBusinessKey;
        }
        if (!requestedBusinessKey.equals(actualBusinessKey)) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_MISMATCH: DCC timeline business key "
                    + requestedBusinessKey + " does not match process " + actualBusinessKey);
        }
        return requestedBusinessKey;
    }

    private static String resolveControlledFileBusinessKey(ProcessInstance processInstance) {
        return resolveControlledFileBusinessKey(processInstance.getProcessDefinitionKey(),
                processInstance.getBusinessKey(), processInstance.getProcessVariables());
    }

    private static String resolveControlledFileBusinessKey(HistoricProcessInstance processInstance) {
        return resolveControlledFileBusinessKey(processInstance.getProcessDefinitionKey(),
                processInstance.getBusinessKey(), processInstance.getProcessVariables());
    }

    private static String resolveControlledFileBusinessKey(String processDefinitionKey,
                                                           String businessKey,
                                                           Map<String, Object> processVariables) {
        if (processDefinitionKey != null
                && DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS.contains(processDefinitionKey)) {
            Object objectId = processVariables == null ? null : processVariables.get("objectId");
            return requireBusinessKey(objectId == null ? null : String.valueOf(objectId));
        }
        return requireBusinessKey(businessKey);
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
                                      List<HistoricTaskInstance> tasks,
                                      List<DccControlledFileSignatureDO> signatures, String businessKey) {
        if (context.isGlobalView()) {
            return;
        }
        boolean handledByLoginUser = tasks.stream()
                .anyMatch(task -> Objects.equals(context.getLoginUserId(), parseLong(task.getAssignee())));
        boolean initiatedByLoginUser = Objects.equals(context.getLoginUserId(),
                parseLong(processInstance.getStartUserId()));
        boolean assignedByLoginUser = signatures.stream().anyMatch(signature ->
                Objects.equals(context.getLoginUserId(), signature.getActorId())
                        && Objects.equals(businessKey, String.valueOf(signature.getControlledFileId()))
                        && Objects.equals(context.getProcessInstanceId(), signature.getProcessInstanceId())
                        && "ASSIGN".equals(signature.getActionType())
                        && "MATRIX_REVIEW_ASSIGN".equals(signature.getMeaningCode())
                        && "VALID".equals(signature.getEvidenceStatus())
                        && tasks.stream().anyMatch(task -> "MATRIX_REVIEW".equals(task.getTaskDefinitionKey())
                                && Objects.equals(task.getId(), signature.getTaskId())));
        BpmTaskRespVO todoTask = context.getSourceTaskId() == null || context.getSourceTaskId().isBlank()
                ? null : bpmTaskService.getTodoTask(context.getLoginUserId(), context.getSourceTaskId(),
                context.getProcessInstanceId());
        if (!handledByLoginUser && !initiatedByLoginUser && !assignedByLoginUser && todoTask == null) {
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

    private DccControlledFileDO requireControlledFileSnapshot(String businessKey) {
        Long fileId = parseBusinessKey(businessKey);
        return controlledFileMapper.selectByIdIncludingDeleted(fileId);
    }

    private DccControlledFileDO requireControlledFileSnapshotForTodo(String businessKey) {
        DccControlledFileDO file = requireControlledFileSnapshot(businessKey);
        if (file == null || Boolean.TRUE.equals(file.getDeleted())) {
            throw new IllegalStateException("APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file summary snapshot not found "
                    + businessKey);
        }
        return file;
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

    private static <T> PageResult<T> requireSourcePage(PageResult<T> page, String viewType, int pageNo) {
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: DCC " + viewType);
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: DCC " + viewType);
        Objects.requireNonNull(page.getTotal(), "APPROVAL_ADAPTER_PAGE_TOTAL_REQUIRED: DCC " + viewType);
        if (page.getTotal() < 0) {
            throw new IllegalStateException("APPROVAL_ADAPTER_PAGE_TOTAL_INVALID: DCC " + viewType
                    + " page " + pageNo + " reported " + page.getTotal());
        }
        return page;
    }

    private static IllegalStateException sourcePageInconsistent(String viewType, int pageNo,
                                                                 long sourceOffset, long sourceTotal) {
        return new IllegalStateException("APPROVAL_ADAPTER_SOURCE_PAGE_INCONSISTENT: DCC " + viewType
                + " page " + pageNo + " is empty at offset " + sourceOffset
                + " but source total is " + sourceTotal);
    }

    private static VisibleWindow visibleWindow(ApprovalTaskQueryContext context) {
        var window = cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow.from(context);
        return new VisibleWindow(window.offset(), Math.addExact(window.offset(), window.limit()));
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

    private static String historicalVersionLabel(String versionNo) {
        return versionNo == null || versionNo.isBlank() ? "-" : versionNo;
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

    private record VisibleWindow(long fromIndex, long toIndex) {

        private boolean contains(long index) {
            return index >= fromIndex && index < toIndex;
        }
    }
}
