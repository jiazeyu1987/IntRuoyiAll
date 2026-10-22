package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Internal domain signature boundary. There is deliberately no HTTP or target-user input. */
@Service
public class EdhrDomainTaskAssignmentService {
    private final NativeApprovalOwnershipPolicy ownership;
    private final TaskService tasks;
    private final BpmTaskService bpmTasks;

    public EdhrDomainTaskAssignmentService(NativeApprovalOwnershipPolicy ownership, TaskService tasks,
            BpmTaskService bpmTasks) {
        this.ownership = ownership;
        this.tasks = tasks;
        this.bpmTasks = bpmTasks;
    }

    public Task read(String taskId, Long workTaskId) {
        requireTaskId(taskId);
        Task task = tasks.createTaskQuery().taskId(taskId).singleResult();
        ownership.readDomainAssignment(task, workTaskId, requireAuthenticatedActor());
        return task;
    }

    // The MES action must already hold ledger -> execution locks in this same transaction.
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Task assign(String taskId, Long workTaskId) {
        requireTaskId(taskId);
        Long actor = requireAuthenticatedActor();
        Task task = tasks.createTaskQuery().taskId(taskId).singleResult();
        ownership.acquireDomainAssignment(task, workTaskId, actor);
        // Re-read engine identity, then consume locked current-read request and domain facts again.
        task = tasks.createTaskQuery().taskId(taskId).singleResult();
        EdhrDomainAssignmentProof proof = ownership.acquireDomainAssignment(task, workTaskId, actor);
        if (!Objects.equals(task.getAssignee(), actor.toString())) {
            if (task.getOwner() == null || task.getOwner().isBlank()) {
                if (task.getAssignee() != null && !task.getAssignee().isBlank()) tasks.setOwner(taskId, task.getAssignee());
            }
            tasks.addComment(taskId, task.getProcessInstanceId(), "EDHR_DOMAIN_ASSIGNMENT",
                    "request=" + proof.requestId() + ";execution=" + proof.executionId()
                            + ";revision=" + proof.executionRevision() + ";snapshot=" + proof.snapshotId()
                            + ";workTask=" + proof.workTaskId() + ";phase=" + proof.phase()
                            + ";actor=" + actor + ";candidateDigest=" + proof.candidateDigest() + ";frozenActors="
                            + proof.eligibleActorIds().stream().sorted().toList());
            tasks.setAssignee(taskId, actor.toString());
        }
        Task assigned = tasks.createTaskQuery().taskId(taskId).singleResult();
        EngineApprovalIdentity identity = proof.engineIdentity();
        if (assigned == null || !Objects.equals(actor.toString(), assigned.getAssignee())
                || !Objects.equals(identity.taskId(), assigned.getId())
                || !Objects.equals(identity.tenantId().toString(), assigned.getTenantId())
                || !Objects.equals(identity.processInstanceId(), assigned.getProcessInstanceId())
                || !Objects.equals(identity.processDefinitionId(), assigned.getProcessDefinitionId())
                || !Objects.equals(identity.taskDefinitionKey(), assigned.getTaskDefinitionKey())) {
            throw new IllegalStateException("EDHR_DOMAIN_ASSIGNMENT_ENGINE_IDENTITY_MISMATCH");
        }
        return bpmTasks.validateTask(actor, taskId);
    }

    private static Long requireAuthenticatedActor() {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        if (actor == null || actor <= 0) throw new IllegalStateException("EDHR_DOMAIN_AUTHENTICATED_ACTOR_REQUIRED");
        return actor;
    }

    private static void requireTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) throw new IllegalStateException("EDHR_DOMAIN_TASK_ID_REQUIRED");
    }
}
