package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE;

/** Coordinates exact formal rows, independently of display titles, variables and numeric coincidences. */
@Component
public class NativeApprovalOwnershipPolicy {
    public static final String EDHR_DEFINITION_KEY = "mes-edhr-approval-v1";
    private final BusinessApprovalRequestMapper requests;
    private final FormActionInstanceMapper forms;
    private final BpmProcessDefinitionService definitions;
    private final BpmProcessInstanceService processes;
    private final EdhrEngineOwnershipReader edhr;

    @jakarta.annotation.Resource
    private ApprovalProcessOwnershipService formalOwnership;

    public NativeApprovalOwnershipPolicy(BusinessApprovalRequestMapper requests, FormActionInstanceMapper forms,
            BpmProcessDefinitionService definitions, BpmProcessInstanceService processes, EdhrEngineOwnershipReader edhr) {
        this.requests = requests;
        this.forms = forms;
        this.definitions = definitions;
        this.processes = processes;
        this.edhr = edhr;
    }

    public NativeApprovalOwnership inspect(Task task, Long userId) {
        return inspect(task, userId, (request, identity) -> edhr.read(request, identity, userId));
    }

    public EdhrDomainAssignmentProof readDomainAssignment(Task task, Long workTaskId, Long authenticatedActorId) {
        return domainAssignment(task, workTaskId, authenticatedActorId, false);
    }

    public EdhrDomainAssignmentProof acquireDomainAssignment(Task task, Long workTaskId, Long authenticatedActorId) {
        // First acquisition always takes ledger -> execution -> snapshot -> work-task locks.
        // Only afterwards may the formal request be locked and the complete proof reread.
        EdhrDomainAssignmentProof initial = domainAssignment(task, workTaskId, authenticatedActorId, true, false);
        return domainAssignment(task, workTaskId, authenticatedActorId, true, true, initial);
    }

    private EdhrDomainAssignmentProof domainAssignment(Task task, Long workTaskId, Long authenticatedActorId,
            boolean lockDomain) {
        return domainAssignment(task, workTaskId, authenticatedActorId, lockDomain, false);
    }

    private EdhrDomainAssignmentProof domainAssignment(Task task, Long workTaskId, Long authenticatedActorId,
            boolean lockDomain, boolean lockedRequest) {
        return domainAssignment(task, workTaskId, authenticatedActorId, lockDomain, lockedRequest, null);
    }

    private EdhrDomainAssignmentProof domainAssignment(Task task, Long workTaskId, Long authenticatedActorId,
            boolean lockDomain, boolean lockedRequest, EdhrDomainAssignmentProof initial) {
        require(authenticatedActorId != null && authenticatedActorId > 0 && workTaskId != null && workTaskId > 0,
                "DOMAIN_ACTOR_WORK_TASK_REQUIRED");
        EdhrDomainAssignmentProof[] proof = new EdhrDomainAssignmentProof[1];
        NativeApprovalOwnership ownership = inspect(task, authenticatedActorId, (request, identity) -> {
            if (initial != null) {
                require(Objects.equals(request.getId(), initial.requestId())
                        && Objects.equals(request.getObjectId(), initial.executionId().toString())
                        && Objects.equals(request.getObjectVersion(), initial.executionRevision().toString())
                        && Objects.equals(identity, initial.engineIdentity()), "LOCKED_REQUEST_DOMAIN_CHANGED");
            }
            proof[0] = Objects.requireNonNull(lockDomain
                    ? edhr.acquireDomainAssignment(request, identity, workTaskId, authenticatedActorId)
                    : edhr.readDomainAssignment(request, identity, workTaskId, authenticatedActorId),
                    "EDHR_DOMAIN_ASSIGNMENT_PROOF_REQUIRED");
            require(Objects.equals(proof[0].requestId(), request.getId())
                    && Objects.equals(proof[0].workTaskId(), workTaskId)
                    && Objects.equals(proof[0].actorId(), authenticatedActorId)
                    && Objects.equals(proof[0].engineIdentity(), identity)
                    && proof[0].eligibleActorIds().contains(authenticatedActorId), "DOMAIN_PROOF_MISMATCH");
            return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.EDHR_EXECUTION, null,
                    java.util.Map.of(), null);
        }, lockedRequest);
        require(ownership.owner() == NativeApprovalOwnership.Owner.EDHR_EXECUTION && proof[0] != null,
                "DOMAIN_FORMAL_REQUEST_REQUIRED");
        return proof[0];
    }

    private NativeApprovalOwnership inspect(Task task, Long userId,
            java.util.function.BiFunction<BusinessApprovalRequestDO, EngineApprovalIdentity, NativeApprovalOwnership> reader) {
        return inspect(task, userId, reader, false);
    }

    private NativeApprovalOwnership inspect(Task task, Long userId,
            java.util.function.BiFunction<BusinessApprovalRequestDO, EngineApprovalIdentity, NativeApprovalOwnership> reader,
            boolean lockedRequest) {
        require(task != null && hasText(task.getId()) && hasText(task.getProcessInstanceId())
                && hasText(task.getProcessDefinitionId()), "ENGINE_IDENTITY_REQUIRED");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        String processId = task.getProcessInstanceId();
        List<BusinessApprovalRequestDO> requestRows = Objects.requireNonNull(
                lockedRequest ? requests.selectOwnershipByTenantAndProcessIdForUpdate(tenant, processId)
                        : requests.selectOwnershipByTenantAndProcessId(tenant, processId), "OWNERSHIP_REQUEST_ROWS_REQUIRED");
        ProcessDefinition definition = definitions.getProcessDefinition(task.getProcessDefinitionId());
        require(definition != null && hasText(definition.getKey()), "DEFINITION_REQUIRED");
        // Do not change genuine non-eDHR domain policies or deserialize their business contexts.
        if (!EDHR_DEFINITION_KEY.equals(definition.getKey()) && requestRows.stream().noneMatch(row ->
                "EDHR_BATCH_EXECUTION".equals(row.getObjectType())
                || "MES".equals(row.getDataDomain()) && "SUBMIT_REVIEW".equals(row.getActionCode()))) {
            return NativeApprovalOwnership.generic();
        }
        ProcessInstance exactProcess=processes.getProcessInstance(processId);
        require(exactProcess!=null && Objects.equals(String.valueOf(tenant),task.getTenantId())
                && Objects.equals(String.valueOf(tenant),exactProcess.getTenantId()),"FORMAL_PROCESS_TENANT_MISMATCH");
        var formal=formalOwnership.requireExact(tenant,processId,task.getProcessDefinitionId(),definition.getKey(),exactProcess.getBusinessKey());
        List<FormActionInstanceDO> formRows = Objects.requireNonNull(
                forms.selectOwnershipByTenantAndProcessId(tenant, processId), "OWNERSHIP_FORM_ROWS_REQUIRED");
        require(requestRows.size() <= 1 && formRows.size() <= 1, "FORMAL_RELATION_DUPLICATE");
        requestRows.forEach(row -> require(Objects.equals(row.getTenantId(), tenant)
                && Objects.equals(row.getProcessInstanceId(), processId) && !Boolean.TRUE.equals(row.getDeleted()),
                "REQUEST_TENANT_PROCESS_MISMATCH"));
        formRows.forEach(row -> require(row.getId() != null && row.getId() > 0 && Objects.equals(row.getTenantId(), tenant)
                && Objects.equals(row.getBpmProcessInstanceId(), processId) && !Boolean.TRUE.equals(row.getDeleted()),
                "FORM_TENANT_PROCESS_MISMATCH"));
        BusinessApprovalRequestDO request = requestRows.isEmpty() ? null : requestRows.get(0);
        BusinessApprovalContext context = request == null ? null
                : JsonUtils.parseObject(request.getBusinessContextJson(), BusinessApprovalContext.class);
        boolean edhrClaim = request != null && ("EDHR_BATCH_EXECUTION".equals(request.getObjectType())
                || context != null && "EDHR_BATCH_EXECUTION".equals(context.getObjectType())
                || "MES".equals(request.getDataDomain()) && "SUBMIT_REVIEW".equals(request.getActionCode()));
        if (!edhrClaim && !EDHR_DEFINITION_KEY.equals(definition.getKey())) {
            return NativeApprovalOwnership.generic();
        }
        require(Objects.equals(String.valueOf(tenant), task.getTenantId()), "TASK_TENANT_MISMATCH");
        require(hasText(task.getTaskDefinitionKey()), "TASK_DEFINITION_KEY_REQUIRED");
        ProcessInstance process = processes.getProcessInstance(processId);
        require(process != null && Objects.equals(processId, process.getId())
                && Objects.equals(String.valueOf(tenant), process.getTenantId())
                && Objects.equals(task.getProcessDefinitionId(), process.getProcessDefinitionId())
                && Objects.equals(definition.getId(), task.getProcessDefinitionId())
                && Objects.equals(definition.getKey(), process.getProcessDefinitionKey()), "PROCESS_DEFINITION_TENANT_MISMATCH");
        require(!(edhrClaim && !formRows.isEmpty()), "FORM_EDHR_CONFLICT");
        if (!formRows.isEmpty()) {
            return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.FORM_ACTION, null, java.util.Map.of(), null);
        }
        if (!edhrClaim) {
            require(request == null, "REQUEST_DOMAIN_MISMATCH");
            return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.UNCLAIMED_EDHR, null,
                    java.util.Map.of(), "EDHR_ENGINE_FORMAL_REQUEST_MISSING");
        }
        require(request.getId() != null && request.getId() > 0
                && "BPM_REQUIRED".equals(request.getPolicyMode()) && "PENDING_BPM".equals(request.getRequestStatus())
                && "MES".equals(request.getDataDomain()) && "MES".equals(request.getSystemCode())
                && "EDHR_BATCH_EXECUTION".equals(request.getObjectType())
                && "SUBMIT_REVIEW".equals(request.getActionCode())
                && "DRAFT".equals(request.getObjectState()) && request.getApplicantUserId() != null
                && request.getApplicantUserId() > 0
                && EDHR_DEFINITION_KEY.equals(request.getProcessDefinitionKey())
                && Objects.equals(request.getProcessDefinitionKey(), definition.getKey())
                && Objects.equals("BUSINESS_APPROVAL:" + request.getId(), process.getBusinessKey()), "REQUEST_ENGINE_MISMATCH");
        require(hasText(request.getBusinessContextJson()), "REQUEST_CONTEXT_REQUIRED");
        require(context != null && Objects.equals(context.getTenantId(), request.getTenantId())
                && Objects.equals(context.getDataDomain(), request.getDataDomain())
                && Objects.equals(context.getSystemCode(), request.getSystemCode())
                && Objects.equals(context.getObjectType(), request.getObjectType())
                && Objects.equals(context.getObjectId(), request.getObjectId())
                && Objects.equals(context.getObjectVersion(), request.getObjectVersion())
                && Objects.equals(context.getActionCode(), request.getActionCode())
                && Objects.equals(context.getObjectState(), request.getObjectState())
                && Objects.equals(context.getApplicantUserId(), request.getApplicantUserId()), "REQUEST_JSON_SCALAR_MISMATCH");
        NativeApprovalOwnership ownership = Objects.requireNonNull(reader.apply(request, new EngineApprovalIdentity(tenant, task.getId(), processId,
                task.getProcessDefinitionId(), definition.getKey(), process.getBusinessKey(), task.getTaskDefinitionKey())),
                "EDHR_OWNERSHIP_RESULT_REQUIRED");
        require(ownership.owner() == NativeApprovalOwnership.Owner.EDHR_EXECUTION, "EDHR_READER_OWNER_MISMATCH");
        return ownership;
    }

    public void assertGenericMutationAllowed(Task task, Long userId) {
        if (inspect(task, userId).requiresDomainReview()) throw exception(TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE);
    }

    private static boolean hasText(String text) { return text != null && !text.isBlank(); }
    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("APPROVAL_OWNERSHIP_" + code);
    }
}
