package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH;

@Component
@Transactional(rollbackFor = Exception.class)
public class MesProBatchRecordExecutionBusinessApprovalEffectExecutor implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "EDHR_BATCH_EXECUTION_SUBMIT_REVIEW";
    public static final String PROCESS_DEFINITION_KEY = "mes-edhr-approval-v1";

    private static final String OBJECT_TYPE = "EDHR_BATCH_EXECUTION";
    private static final String ACTION_CODE = "SUBMIT_REVIEW";
    private static final String OBJECT_STATE_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_SUBMITTED;
    private static final String STATUS_APPROVED = MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_APPROVED;
    private static final String APPROVAL_TASK_ASSIGNEE_VARIABLE = "approveNode_assignee";

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureService executionSignatureService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private BpmTaskService bpmTaskService;

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    public String getBpmProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        requireSubmitReviewContext(context);
        requireExecutionWithStatus(context, MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context,
                                                      BusinessApprovalRequest request) {
        requireSubmitReviewContext(context);
        MesProBatchRecordExecutionDO execution = requireExecutionWithStatus(context,
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT);
        Map<String, Object> variables = requireVariables(context);
        Long executionId = execution.getId();
        Long workTaskId = requireLong(variables, "workTaskId");
        Long submitSignatureId = requireLong(variables, "submitSignatureId");
        Long actorUserId = requireActorUserId(context.getApplicantUserId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = parseSubmittedAt(variables, now);

        executionSignatureService.bindSignatureFieldAuditEvidence(submitSignatureId, executionId,
                requireLong(variables, "fieldAuditRevision"),
                requireString(variables, "fieldAuditHeadHash"),
                requireString(variables, "cellValuesHash"));
        approvalSnapshotMapper.insert(MesProBatchRecordApprovalSnapshotDO.builder()
                .executionId(executionId)
                .processDefinitionKey(requireString(variables, "processDefinitionKey"))
                .processInstanceId(null)
                .approvalStatus(STATUS_APPROVED)
                .snapshotJson(requireString(variables, "approvalSnapshotJson"))
                .snapshotHash(requireString(variables, "approvalSnapshotHash"))
                .currentBpmTaskId(null)
                .currentTaskDefinitionKey(null)
                .submitSignatureId(submitSignatureId)
                .submittedBy(actorUserId)
                .submittedAt(submittedAt)
                .approvedBy(actorUserId)
                .approvedAt(now)
                .closedAt(now)
                .build());
        requireUpdated(executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED)
                .setProcessDefinitionKey(requireString(variables, "processDefinitionKey"))
                .setProcessInstanceId(null)
                .setSubmittedBy(actorUserId)
                .setSubmittedAt(submittedAt)
                .setApprovedBy(actorUserId)
                .setApprovedAt(now)
                .setClosedAt(now)),
                "direct approve batch record execution");
        executionMapper.clearActiveContextKey(executionId);
        workTaskService.completeFillAndCreateNextFillAfterOrdinarySubmit(workTaskId, executionId);
        return BusinessApprovalEffectResult.completed(STATUS_APPROVED);
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context,
                                                    BusinessApprovalRequest request) {
        requireSubmitReviewContext(context);
        MesProBatchRecordExecutionDO execution = requireExecutionWithStatus(context,
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT);
        Map<String, Object> variables = requireVariables(context);
        String processInstanceId = requireProcessInstanceId(request);
        Long executionId = execution.getId();
        Long submitSignatureId = requireLong(variables, "submitSignatureId");
        Long submittedBy = requireLong(variables, "submittedBy");
        LocalDateTime submittedAt = parseSubmittedAt(variables, LocalDateTime.now());
        String taskDefinitionKey = requireString(variables, "approvalTaskDefinitionKey");

        List<MesProEdhrReviewTaskCreateCommand> reviewTaskCommands = buildReviewTaskCreateCommands(
                processInstanceId, taskDefinitionKey, requireString(variables, "edhrReviewSignatureCells"));
        String currentBpmTaskId = requireSingleBpmTaskId(reviewTaskCommands, processInstanceId, taskDefinitionKey);
        executionSignatureService.attachSubmitSignatureProcessInstance(submitSignatureId, executionId, processInstanceId);
        executionSignatureService.bindSignatureFieldAuditEvidence(submitSignatureId, executionId,
                requireLong(variables, "fieldAuditRevision"),
                requireString(variables, "fieldAuditHeadHash"),
                requireString(variables, "cellValuesHash"));
        approvalSnapshotMapper.insert(MesProBatchRecordApprovalSnapshotDO.builder()
                .executionId(executionId)
                .processDefinitionKey(requireString(variables, "processDefinitionKey"))
                .processInstanceId(processInstanceId)
                .approvalStatus(STATUS_SUBMITTED)
                .snapshotJson(requireString(variables, "approvalSnapshotJson"))
                .snapshotHash(requireString(variables, "approvalSnapshotHash"))
                .currentBpmTaskId(currentBpmTaskId)
                .currentTaskDefinitionKey(taskDefinitionKey)
                .submitSignatureId(submitSignatureId)
                .submittedBy(submittedBy)
                .submittedAt(submittedAt)
                .build());
        requireUpdated(executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED)
                .setProcessDefinitionKey(requireString(variables, "processDefinitionKey"))
                .setProcessInstanceId(processInstanceId)
                .setSubmittedBy(submittedBy)
                .setSubmittedAt(submittedAt)),
                "mark pending batch record execution");
        workTaskService.createReviewTasks(requireLong(variables, "workTaskId"), executionId, reviewTaskCommands);
        return BusinessApprovalEffectResult.pending(STATUS_SUBMITTED);
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        requireSubmitReviewContext(context);
        MesProBatchRecordExecutionDO execution = requireExecutionWithStatus(context,
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED);
        Long executionId = execution.getId();
        String processInstanceId = requireProcessInstanceId(request);
        if (!StrUtil.equals(processInstanceId, execution.getProcessInstanceId())) {
            throw contextInvalid("MES batch record execution process instance mismatched: executionId="
                    + executionId + ", requestProcessInstanceId=" + processInstanceId
                    + ", executionProcessInstanceId=" + execution.getProcessInstanceId());
        }
        MesProBatchRecordApprovalSnapshotDO snapshot = requireSubmittedSnapshot(executionId, processInstanceId);
        String currentBpmTaskId = requireSnapshotBpmTaskId(snapshot, executionId);
        MesProEdhrWorkTaskDO reviewTask = workTaskService.getActiveReviewTaskByBpmTaskId(executionId,
                currentBpmTaskId);
        if (reviewTask == null) {
            throw reviewTaskContextInvalid("executionId=" + executionId
                    + ", bpmTaskId=" + currentBpmTaskId + ", reviewTask=missing");
        }
        Long approverUserId = requireActorUserId(actorUserId);
        MesProEdhrWorkTaskDO completedReviewTask = workTaskService.completeOneReviewTask(reviewTask.getId(),
                executionId);
        LocalDateTime now = LocalDateTime.now();
        requireUpdated(approvalSnapshotMapper.approveAndClearCurrentBpmTask(new MesProBatchRecordApprovalSnapshotDO()
                        .setId(snapshot.getId())
                        .setApprovalStatus(STATUS_APPROVED)
                        .setApprovedBy(approverUserId)
                        .setApprovedAt(now)
                        .setClosedAt(now)),
                "approve batch record execution snapshot");
        requireUpdated(executionMapper.updateById(new MesProBatchRecordExecutionDO()
                        .setId(executionId)
                        .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED)
                        .setApprovedBy(approverUserId)
                        .setApprovedAt(now)
                        .setClosedAt(now)),
                "approve batch record execution");
        executionMapper.clearActiveContextKey(executionId);
        workTaskService.createNextFillAfterReview(completedReviewTask);
        return BusinessApprovalEffectResult.completed(STATUS_APPROVED);
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requireSubmitReviewContext(context);
        return BusinessApprovalEffectResult.rejected(MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_REJECTED);
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requireSubmitReviewContext(context);
        return BusinessApprovalEffectResult.cancelled(MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_REJECTED);
    }

    private List<MesProEdhrReviewTaskCreateCommand> buildReviewTaskCreateCommands(String processInstanceId,
                                                                                  String taskDefinitionKey,
                                                                                  String assignmentsJson) {
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId, null,
                taskDefinitionKey);
        if (tasks == null || tasks.isEmpty()) {
            throw reviewTaskContextInvalid("processInstanceId=" + processInstanceId
                    + ", taskDefinitionKey=" + taskDefinitionKey
                    + ", tasks=empty");
        }
        JSONArray assignments = JSON.parseArray(assignmentsJson);
        if (assignments == null || assignments.isEmpty()) {
            throw reviewTaskContextInvalid("processInstanceId=" + processInstanceId
                    + ", taskDefinitionKey=" + taskDefinitionKey
                    + ", assignments=empty");
        }
        List<MesProEdhrReviewTaskCreateCommand> commands = new ArrayList<>();
        for (int i = 0; i < assignments.size(); i++) {
            JSONObject assignment = assignments.getJSONObject(i);
            List<Long> taskUserIds = reviewTaskUserIds(assignment);
            List<Task> matchedTasks = tasks.stream()
                    .filter(task -> {
                        Long assigneeUserId = resolveBpmTaskAssigneeUserId(task);
                        return assigneeUserId != null && taskUserIds.contains(assigneeUserId);
                    })
                    .toList();
            if (matchedTasks.isEmpty()) {
                throw reviewTaskContextInvalid("processInstanceId=" + processInstanceId
                        + ", taskDefinitionKey=" + taskDefinitionKey
                        + ", signatureCellKey=" + assignment.getString("signatureCellKey")
                        + ", assignee=mismatched");
            }
            for (Task task : matchedTasks) {
                Long assigneeUserId = resolveBpmTaskAssigneeUserId(task);
                commands.add(new MesProEdhrReviewTaskCreateCommand()
                        .setSignatureCellKey(assignment.getString("signatureCellKey"))
                        .setSignatureRowIndex(assignment.getInteger("signatureRowIndex"))
                        .setSignatureColumnIndex(assignment.getInteger("signatureColumnIndex"))
                        .setReviewSourceType(assignment.getString("reviewSourceType"))
                        .setReviewSourceId(assignment.getLong("reviewSourceId"))
                        .setReviewSourceName(assignment.getString("reviewSourceName"))
                        .setCandidateSourceType(resolveCandidateSourceType(assignment.getString("reviewSourceType")))
                        .setCandidateSourceId(assignment.getLong("reviewSourceId"))
                        .setCandidateUserSnapshot(candidateUserIds(assignment).stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")))
                        .setAssigneeUserId(assigneeUserId)
                        .setBpmTaskId(task.getId()));
            }
        }
        return commands;
    }

    private String requireSingleBpmTaskId(List<MesProEdhrReviewTaskCreateCommand> commands,
                                          String processInstanceId,
                                          String taskDefinitionKey) {
        List<String> taskIds = commands.stream()
                .map(MesProEdhrReviewTaskCreateCommand::getBpmTaskId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (taskIds.size() != 1) {
            throw reviewTaskContextInvalid("processInstanceId=" + processInstanceId
                    + ", taskDefinitionKey=" + taskDefinitionKey
                    + ", currentBpmTaskId=" + taskIds);
        }
        return taskIds.get(0);
    }

    private List<Long> reviewTaskUserIds(JSONObject assignment) {
        Long assigneeUserId = assignment.getLong("assigneeUserId");
        if (assigneeUserId != null) {
            return List.of(assigneeUserId);
        }
        return candidateUserIds(assignment);
    }

    private List<Long> candidateUserIds(JSONObject assignment) {
        JSONArray array = assignment.getJSONArray("candidateUserIds");
        if (array == null || array.isEmpty()) {
            throw contextInvalid("MES batch record execution candidate users are missing: "
                    + assignment.getString("signatureCellKey"));
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Long userId = array.getLong(i);
            if (userId == null || !userIds.add(userId)) {
                throw contextInvalid("MES batch record execution candidate user is invalid: "
                        + assignment.getString("signatureCellKey"));
            }
        }
        return new ArrayList<>(userIds);
    }

    private Long resolveBpmTaskAssigneeUserId(Task task) {
        Long assigneeUserId = parseNullableLong(task.getAssignee());
        if (assigneeUserId != null) {
            return assigneeUserId;
        }
        Object localAssignee = task.getTaskLocalVariables() == null
                ? null : task.getTaskLocalVariables().get(APPROVAL_TASK_ASSIGNEE_VARIABLE);
        if (localAssignee instanceof Number number) {
            return number.longValue();
        }
        return parseNullableLong(localAssignee == null ? null : String.valueOf(localAssignee));
    }

    private String resolveCandidateSourceType(String reviewSourceType) {
        return switch (reviewSourceType) {
            case "USER", "USERS" -> "USER";
            case "ROLE", "ROLES" -> "ROLE_GROUP";
            case "DEPT", "DEPTS" -> "DEPT_GROUP";
            case "DEPT_LEADER" -> "DEPT_LEADER";
            default -> reviewSourceType;
        };
    }

    private MesProBatchRecordExecutionDO requireExecutionWithStatus(BusinessApprovalContext context, Integer expectedStatus) {
        Long executionId = parseExecutionId(context);
        MesProBatchRecordExecutionDO execution = executionMapper.selectByIdForUpdate(executionId);
        if (execution == null || !Objects.equals(expectedStatus, execution.getStatus())) {
            throw contextInvalid("MES batch record execution status is invalid: executionId=" + executionId
                    + ", expected=" + expectedStatus
                    + ", actual=" + (execution == null ? null : execution.getStatus()));
        }
        return execution;
    }

    private void requireSubmitReviewContext(BusinessApprovalContext context) {
        if (context == null
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())
                || !OBJECT_STATE_DRAFT.equals(context.getObjectState())) {
            throw contextInvalid("MES batch record execution submit review approval context is invalid");
        }
        parseExecutionId(context);
    }

    private Long parseExecutionId(BusinessApprovalContext context) {
        try {
            return Long.valueOf(context.getObjectId());
        } catch (RuntimeException ex) {
            throw contextInvalid("MES batch record execution id is invalid: "
                    + (context == null ? null : context.getObjectId()));
        }
    }

    private Map<String, Object> requireVariables(BusinessApprovalContext context) {
        if (context.getVariables() == null || context.getVariables().isEmpty()) {
            throw contextInvalid("MES batch record execution business approval variables are required");
        }
        return context.getVariables();
    }

    private String requireProcessInstanceId(BusinessApprovalRequest request) {
        String processInstanceId = request == null ? null : request.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "MES batch record execution approval process instance is required");
        }
        return StrUtil.trim(processInstanceId);
    }

    private MesProBatchRecordApprovalSnapshotDO requireSubmittedSnapshot(Long executionId,
                                                                         String processInstanceId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(executionId);
        if (snapshot == null
                || !STATUS_SUBMITTED.equals(snapshot.getApprovalStatus())
                || !StrUtil.equals(processInstanceId, snapshot.getProcessInstanceId())) {
            throw contextInvalid("MES batch record execution approval snapshot is invalid: executionId="
                    + executionId + ", processInstanceId=" + processInstanceId);
        }
        return snapshot;
    }

    private String requireSnapshotBpmTaskId(MesProBatchRecordApprovalSnapshotDO snapshot, Long executionId) {
        String bpmTaskId = snapshot == null ? null : snapshot.getCurrentBpmTaskId();
        if (StrUtil.isBlank(bpmTaskId)) {
            throw reviewTaskContextInvalid("executionId=" + executionId + ", currentBpmTaskId=missing");
        }
        return StrUtil.trim(bpmTaskId);
    }

    private Long requireActorUserId(Long actorUserId) {
        if (actorUserId == null) {
            throw contextInvalid("MES batch record execution actor user is required");
        }
        return actorUserId;
    }

    private String requireString(Map<String, Object> variables, String key) {
        Object value = variables.get(key);
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            throw contextInvalid("MES batch record execution variable is required: " + key);
        }
        return String.valueOf(value);
    }

    private Long requireLong(Map<String, Object> variables, String key) {
        Long value = parseNullableLong(variables.get(key) == null ? null : String.valueOf(variables.get(key)));
        if (value == null) {
            throw contextInvalid("MES batch record execution variable is required: " + key);
        }
        return value;
    }

    private Long parseNullableLong(String rawValue) {
        if (StrUtil.isBlank(rawValue)) {
            return null;
        }
        try {
            return Long.valueOf(StrUtil.trim(rawValue));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseSubmittedAt(Map<String, Object> variables, LocalDateTime defaultValue) {
        String rawValue = String.valueOf(variables.get("submittedAt"));
        if (StrUtil.isBlank(rawValue)) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(rawValue);
        } catch (RuntimeException ex) {
            throw contextInvalid("MES batch record execution submittedAt is invalid: " + rawValue);
        }
    }

    private void requireUpdated(int updated, String action) {
        if (updated <= 0) {
            throw contextInvalid("MES batch record execution update failed: " + action);
        }
    }

    private BusinessApprovalException contextInvalid(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, message);
    }

    private RuntimeException reviewTaskContextInvalid(String message) {
        return exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH, message);
    }
}
