package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.EdhrEngineOwnershipReader;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.EdhrDomainAssignmentProof;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.EngineApprovalIdentity;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.NativeApprovalOwnership;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrEngineOwnershipMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reads only the submit/effect/work-task writer's existing formal relationship. */
@Component
public class MesEdhrEngineOwnershipReader implements EdhrEngineOwnershipReader {
    private final MesEdhrEngineOwnershipMapper mapper;
    private final BpmTaskService engineTasks;
    private final PermissionApi permissions;
    @Resource
    private GxpAuditService gxpAuditService;

    public MesEdhrEngineOwnershipReader(MesEdhrEngineOwnershipMapper mapper, BpmTaskService engineTasks,
            PermissionApi permissions) {
        this.mapper = mapper;
        this.engineTasks = engineTasks;
        this.permissions = permissions;
    }

    @Override
    public NativeApprovalOwnership read(BusinessApprovalRequestDO request, EngineApprovalIdentity identity, Long userId) {
        FormalFacts facts = readFormalFacts(request, identity, false);
        Long executionId = facts.execution().getId();
        var tasks = facts.tasks();
        List<MesEdhrEngineOwnershipMapper.WorkTaskFact> eligible = tasks.stream()
                .filter(MesEdhrEngineOwnershipReader::isActive)
                .filter(task -> MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(task, userId)).toList();
        if (eligible.size() != 1) {
            return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.EDHR_EXECUTION, null, Map.of(),
                    eligible.isEmpty() ? "EDHR_ENGINE_WORK_TASK_CONTEXT_MISSING" : "EDHR_ENGINE_WORK_TASK_CONTEXT_AMBIGUOUS");
        }
        validatePhase(facts.phaseTasks(), eligible.get(0));
        List<String> missing = requiredPagePermissions(userId);
        if (!missing.isEmpty()) {
            return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.EDHR_EXECUTION, null,
                    Map.of("requiredPermissionsMissing", String.join(",", missing)), "EDHR_ENGINE_DOMAIN_AUTHORITY_MISSING");
        }
        return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.EDHR_EXECUTION,
                "/mes/pro/feedback/edhr-approval/detail", Map.of("id", executionId.toString(),
                "workTaskId", eligible.get(0).getId().toString(), "bpmTaskId", identity.taskId()), null);
    }

    @Override
    public EdhrDomainAssignmentProof acquireDomainAssignment(BusinessApprovalRequestDO request,
            EngineApprovalIdentity identity, Long workTaskId, Long authenticatedActorId) {
        require(TransactionSynchronizationManager.isActualTransactionActive(), "ACTION_TRANSACTION_REQUIRED");
        gxpAuditService.acquireLedgerLock();
        return assignmentProof(request, identity, workTaskId, authenticatedActorId, true);
    }

    @Override
    public EdhrDomainAssignmentProof readDomainAssignment(BusinessApprovalRequestDO request,
            EngineApprovalIdentity identity, Long workTaskId, Long authenticatedActorId) {
        return assignmentProof(request, identity, workTaskId, authenticatedActorId, false);
    }

    private EdhrDomainAssignmentProof assignmentProof(BusinessApprovalRequestDO request, EngineApprovalIdentity identity,
            Long workTaskId, Long authenticatedActorId, boolean locked) {
        FormalFacts facts = readFormalFacts(request, identity, locked);
        var selected = facts.tasks().stream().filter(task -> Objects.equals(task.getId(), workTaskId)).toList();
        require(selected.size() == 1 && isActive(selected.get(0)), "SELECTED_WORK_TASK_NOT_WRITABLE");
        var task = selected.get(0);
        validatePhase(facts.phaseTasks(), task);
        Set<Long> eligible = new java.util.LinkedHashSet<>(
                MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(task.getCandidateUserSnapshot()));
        if (task.getAssigneeUserId() != null) eligible.add(task.getAssigneeUserId());
        require(!eligible.isEmpty() && eligible.stream().allMatch(id -> id != null && id > 0)
                && authenticatedActorId != null && eligible.contains(authenticatedActorId), "FROZEN_ACTOR_NOT_ELIGIBLE");
        require(requiredPagePermissions(authenticatedActorId).isEmpty(), "DOMAIN_PAGE_PERMISSION_MISSING");
        return new EdhrDomainAssignmentProof(request.getId(), facts.execution().getId(),
                facts.execution().getRevisionNo(), facts.snapshot().getId(), task.getId(), task.getTaskType(),
                authenticatedActorId, eligible, identity);
    }

    private List<String> requiredPagePermissions(Long userId) {
        return List.of("mes:pro-batch-record-execution:approve", "mes:pro-batch-record-execution:track",
                "mes:pro-batch-record-execution:signature-query").stream()
                .filter(permission -> !permissions.hasAnyPermissions(userId, permission)).toList();
    }

    private static boolean isActive(MesEdhrEngineOwnershipMapper.WorkTaskFact task) {
        return Set.of("TODO", "OVERDUE").contains(task.getStatus());
    }

    private static void validatePhase(List<MesEdhrEngineOwnershipMapper.WorkTaskFact> tasks,
            MesEdhrEngineOwnershipMapper.WorkTaskFact selected) {
        var approvals = tasks.stream().filter(task -> "APPROVE".equals(task.getTaskType())).toList();
        if ("REVIEW".equals(selected.getTaskType())) {
            require(approvals.isEmpty(), "REVIEW_PHASE_HAS_APPROVE");
        } else {
            require(approvals.size() == 1 && Objects.equals(approvals.get(0).getId(), selected.getId())
                    && tasks.stream().filter(task -> "REVIEW".equals(task.getTaskType()))
                    .allMatch(task -> Set.of("DONE", "CANCELED").contains(task.getStatus())), "APPROVE_PHASE_INVALID");
        }
    }

    private FormalFacts readFormalFacts(BusinessApprovalRequestDO request, EngineApprovalIdentity identity, boolean locked) {
        require(Objects.equals(identity.tenantId(), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(request.getTenantId(), identity.tenantId())
                && Objects.equals(request.getProcessInstanceId(), identity.processInstanceId())
                && MesProBatchRecordExecutionBusinessApprovalEffectExecutor.EXECUTOR_CODE.equals(request.getEffectExecutorCode()),
                "REQUEST_TENANT_PROCESS_EXECUTOR_MISMATCH");
        Long executionId;
        try { executionId = Long.valueOf(request.getObjectId()); }
        catch (NumberFormatException ex) { throw new IllegalStateException("EDHR_ENGINE_EXECUTION_ID_INVALID", ex); }
        require(executionId > 0, "EXECUTION_ID_INVALID");
        var executions = Objects.requireNonNull(locked ? mapper.selectExecutionsForUpdate(identity.tenantId(), executionId)
                : mapper.selectExecutions(identity.tenantId(), executionId));
        require(executions.size() == 1, "EXECUTION_RELATION_MISSING_OR_DUPLICATE");
        var execution = executions.get(0);
        require(Objects.equals(execution.getId(), executionId) && Objects.equals(execution.getTenantId(), identity.tenantId())
                && Objects.equals(execution.getProcessInstanceId(), identity.processInstanceId())
                && Objects.equals(execution.getProcessDefinitionKey(), identity.processDefinitionKey())
                && execution.getRevisionNo() != null && Objects.equals(String.valueOf(execution.getRevisionNo()), request.getObjectVersion())
                && Objects.equals(execution.getStatus(), MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED),
                "EXECUTION_IDENTITY_VERSION_STATUS_MISMATCH");
        var snapshots = Objects.requireNonNull(locked ? mapper.selectSnapshotsForUpdate(identity.tenantId(), executionId)
                : mapper.selectSnapshots(identity.tenantId(), executionId));
        require(snapshots.size() == 1, "SNAPSHOT_RELATION_MISSING_OR_DUPLICATE");
        var snapshot = snapshots.get(0);
        require(snapshot.getId() != null && snapshot.getId() > 0
                && Objects.equals(snapshot.getTenantId(), identity.tenantId())
                && Objects.equals(snapshot.getExecutionId(), executionId)
                && Objects.equals(snapshot.getProcessInstanceId(), identity.processInstanceId())
                && Objects.equals(snapshot.getProcessDefinitionKey(), identity.processDefinitionKey())
                // The submit writer stores the definition key, not the definition id.
                && (snapshot.getProcessDefinitionId() == null
                    || Objects.equals(snapshot.getProcessDefinitionId(), identity.processDefinitionId()))
                && Objects.equals(snapshot.getCurrentBpmTaskId(), identity.taskId())
                && Objects.equals(snapshot.getCurrentTaskDefinitionKey(), identity.taskDefinitionKey())
                && "SUBMITTED".equals(snapshot.getApprovalStatus()), "SNAPSHOT_ENGINE_MISMATCH");
        var lockedPhaseTasks = locked ? Objects.requireNonNull(mapper.selectPhaseWorkTasksForUpdate(identity.tenantId(), executionId)) : null;
        var tasks = locked ? lockedPhaseTasks.stream().filter(task -> Objects.equals(task.getBpmTaskId(), identity.taskId())).toList()
                : Objects.requireNonNull(mapper.selectWorkTasks(identity.tenantId(), executionId, identity.taskId()));
        Set<Long> taskIds = new java.util.HashSet<>();
        tasks.forEach(task -> require(task.getId() != null && task.getId() > 0 && taskIds.add(task.getId())
                && Objects.equals(task.getTenantId(), identity.tenantId())
                && Objects.equals(task.getExecutionId(), executionId) && Objects.equals(task.getBpmTaskId(), identity.taskId())
                && !Boolean.TRUE.equals(task.getDeleted()) && task.getTaskType() != null
                && Set.of("REVIEW", "APPROVE").contains(task.getTaskType()) && task.getStatus() != null,
                "WORK_TASK_IDENTITY_MISMATCH"));
        var engineTask = engineTasks.getTask(identity.taskId());
        require(engineTask != null && Objects.equals(engineTask.getId(), identity.taskId())
                && Objects.equals(engineTask.getTenantId(), identity.tenantId().toString())
                && Objects.equals(engineTask.getProcessInstanceId(), identity.processInstanceId())
                && Objects.equals(engineTask.getProcessDefinitionId(), identity.processDefinitionId())
                && Objects.equals(engineTask.getTaskDefinitionKey(), identity.taskDefinitionKey()), "ENGINE_REFETCH_IDENTITY_MISMATCH");
        var phaseTasks = locked ? lockedPhaseTasks : Objects.requireNonNull(mapper.selectPhaseWorkTasks(identity.tenantId(), executionId));
        Set<Long> phaseIds = new java.util.HashSet<>();
        phaseTasks.forEach(task -> require(task.getId() != null && task.getId() > 0 && phaseIds.add(task.getId())
                && Objects.equals(task.getTenantId(), identity.tenantId())
                && Objects.equals(task.getExecutionId(), executionId) && !Boolean.TRUE.equals(task.getDeleted())
                && task.getBpmTaskId() != null && !task.getBpmTaskId().isBlank()
                && task.getTaskType() != null && Set.of("REVIEW", "APPROVE").contains(task.getTaskType())
                && task.getStatus() != null, "PHASE_WORK_TASK_IDENTITY_MISMATCH"));
        require(phaseIds.containsAll(taskIds), "PHASE_WORK_TASK_RELATION_MISSING");
        return new FormalFacts(execution, snapshot, tasks, phaseTasks);
    }

    private record FormalFacts(MesEdhrEngineOwnershipMapper.ExecutionFact execution,
            MesEdhrEngineOwnershipMapper.SnapshotFact snapshot,
            List<MesEdhrEngineOwnershipMapper.WorkTaskFact> tasks,
            List<MesEdhrEngineOwnershipMapper.WorkTaskFact> phaseTasks) { }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("EDHR_ENGINE_" + code);
    }
}
