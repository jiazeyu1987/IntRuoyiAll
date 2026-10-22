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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeApprovalOwnershipPolicyTest {
    private final BusinessApprovalRequestMapper requests = mock(BusinessApprovalRequestMapper.class);
    private final FormActionInstanceMapper forms = mock(FormActionInstanceMapper.class);
    private final BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
    private final BpmProcessInstanceService processes = mock(BpmProcessInstanceService.class);
    private final EdhrEngineOwnershipReader reader = mock(EdhrEngineOwnershipReader.class);
    private final NativeApprovalOwnershipPolicy policy = new NativeApprovalOwnershipPolicy(requests, forms, definitions, processes, reader);
    private Task task;
    private ProcessInstance process;
    private ProcessDefinition definition;
    private BusinessApprovalRequestDO request;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(122L);
        org.springframework.test.util.ReflectionTestUtils.setField(policy,"formalOwnership",mock(ApprovalProcessOwnershipService.class));
        task = mock(Task.class);
        when(task.getId()).thenReturn("engine-1");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        when(task.getTaskDefinitionKey()).thenReturn("reviewNode");
        when(task.getTenantId()).thenReturn("122");
        definition = mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition-1");
        when(definition.getKey()).thenReturn("mes-edhr-approval-v1");
        when(definitions.getProcessDefinition("definition-1")).thenReturn(definition);
        process = mock(ProcessInstance.class);
        when(process.getId()).thenReturn("process-1");
        when(process.getTenantId()).thenReturn("122");
        when(process.getProcessDefinitionId()).thenReturn("definition-1");
        when(process.getProcessDefinitionKey()).thenReturn("mes-edhr-approval-v1");
        when(process.getBusinessKey()).thenReturn("BUSINESS_APPROVAL:19");
        when(processes.getProcessInstance("process-1")).thenReturn(process);
        var context = BusinessApprovalContext.builder().tenantId(122L).dataDomain("MES").systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION").objectId("301").objectVersion("2").actionCode("SUBMIT_REVIEW")
                .objectState("DRAFT").applicantUserId(7L).reason("submitted").build();
        request = BusinessApprovalRequestDO.builder().id(19L).tenantId(122L).policyMode("BPM_REQUIRED")
                .dataDomain("MES").systemCode("MES").objectType("EDHR_BATCH_EXECUTION").objectId("301")
                .objectVersion("2").actionCode("SUBMIT_REVIEW").objectState("DRAFT").applicantUserId(7L)
                .effectExecutorCode("EDHR_BATCH_EXECUTION_SUBMIT_REVIEW")
                .requestStatus("PENDING_BPM").processDefinitionKey("mes-edhr-approval-v1").processInstanceId("process-1")
                .businessContextJson(JsonUtils.toJsonString(context)).build();
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(request));
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        when(reader.read(any(), any(), any())).thenReturn(new NativeApprovalOwnership(
                NativeApprovalOwnership.Owner.EDHR_EXECUTION, "/mes/pro/feedback/edhr-approval/detail",
                Map.of("id", "301", "workTaskId", "401", "bpmTaskId", "engine-1"), null));
    }

    @AfterEach void clean() { TenantContextHolder.clear(); }

    @Test void explicitReadProofUsesExactFormalIdentityAndNeverAcquiresAssignmentLocks() {
        when(reader.readDomainAssignment(any(), any(), any(), any())).thenAnswer(call ->
                new EdhrDomainAssignmentProof(19L, 301L, 2, 20L, call.getArgument(2), "REVIEW",
                        call.getArgument(3), java.util.Set.of(7L, 8L), call.getArgument(1)));
        assertEquals(401L, policy.readDomainAssignment(task, 401L, 7L).workTaskId());
        var proof = policy.readDomainAssignment(task, 401L, 7L);
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex("EDHR_FROZEN_ACTORS_V1:7,8"), proof.candidateDigest());
        assertThrows(UnsupportedOperationException.class, () -> proof.eligibleActorIds().add(9L));
        verify(reader, never()).acquireDomainAssignment(any(), any(), any(), any());
        verify(reader, never()).read(any(), any(), any());
    }

    @Test void actionProofRequiresLockedSpiAndRejectsForeignReturnedActor() {
        when(reader.acquireDomainAssignment(any(), any(), any(), any())).thenAnswer(call ->
                new EdhrDomainAssignmentProof(19L, 301L, 2, 20L, 401L, "APPROVE",
                        8L, java.util.Set.of(7L, 8L), call.getArgument(1)));
        assertThrows(IllegalStateException.class, () -> policy.acquireDomainAssignment(task, 401L, 7L));
        verify(reader).acquireDomainAssignment(eq(request), any(), eq(401L), eq(7L));
    }

    @Test void terminalCurrentRequestRejectsDespiteOrdinaryPendingReadView() {
        when(reader.acquireDomainAssignment(any(), any(), any(), any())).thenAnswer(call ->
                new EdhrDomainAssignmentProof(19L, 301L, 2, 20L, 401L, "APPROVE",
                        7L, java.util.Set.of(7L, 8L), call.getArgument(1)));
        var terminal = BusinessApprovalRequestDO.builder().id(19L).tenantId(122L).policyMode("BPM_REQUIRED")
                .dataDomain("MES").systemCode("MES").objectType("EDHR_BATCH_EXECUTION").objectId("301")
                .objectVersion("2").actionCode("SUBMIT_REVIEW").objectState("DRAFT").applicantUserId(7L)
                .effectExecutorCode("EDHR_BATCH_EXECUTION_SUBMIT_REVIEW").requestStatus("APPROVED")
                .processDefinitionKey("mes-edhr-approval-v1").processInstanceId("process-1")
                .businessContextJson(request.getBusinessContextJson()).build();
        when(requests.selectOwnershipByTenantAndProcessIdForUpdate(122L, "process-1")).thenReturn(List.of(terminal));
        assertThrows(IllegalStateException.class, () -> policy.acquireDomainAssignment(task, 401L, 7L));
        var order = inOrder(reader, requests);
        order.verify(reader).acquireDomainAssignment(eq(request), any(), eq(401L), eq(7L));
        order.verify(requests).selectOwnershipByTenantAndProcessIdForUpdate(122L, "process-1");
        verify(reader, times(1)).acquireDomainAssignment(any(), any(), any(), any());
    }

    @Test void genericOrUnclaimedProcessCannotSupplyDomainAssignmentProof() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> policy.readDomainAssignment(task, 401L, 7L));
        verifyNoInteractions(reader);
    }

    @Test void formalEdhrUsesExistingDomainRelationshipAndBlocksGenericMutation() {
        assertEquals("401", policy.inspect(task, 7L).decisionQuery().get("workTaskId"));
        verify(reader).read(eq(request), eq(new EngineApprovalIdentity(122L, "engine-1", "process-1",
                "definition-1", "mes-edhr-approval-v1", "BUSINESS_APPROVAL:19", "reviewNode")), eq(7L));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> policy.assertGenericMutationAllowed(task, 7L));
    }

    @Test void genuineFormWithSharedDefinitionKeepsGenericCapabilities() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(form()));
        assertEquals(NativeApprovalOwnership.Owner.FORM_ACTION, policy.inspect(task, 7L).owner());
        policy.assertGenericMutationAllowed(task, 7L);
        verifyNoInteractions(reader);
    }

    @Test void sharedKeyWithoutFormalRowsRemainsReadableButRequiresDomainReview() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        assertEquals("EDHR_ENGINE_FORMAL_REQUEST_MISSING", policy.inspect(task, 7L).navigationBlocker());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> policy.assertGenericMutationAllowed(task, 7L));
    }

    @Test void formalEdhrAndFormConflictFailsBeforeDomainRead() {
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(form()));
        assertFailure("FORM_EDHR_CONFLICT");
    }

    @Test void duplicateRequestDoesNotTakeFirst() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(request, request));
        assertFailure("FORMAL_RELATION_DUPLICATE");
    }

    @Test void duplicateFormDoesNotTakeFirst() {
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(form(), form()));
        assertFailure("FORMAL_RELATION_DUPLICATE");
    }

    @Test void sameNumericObjectWithoutFormalAssociationDoesNotClaimOtherDefinition() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        when(definition.getKey()).thenReturn("ordinary-process");
        assertEquals(NativeApprovalOwnership.Owner.GENERIC, policy.inspect(task, 7L).owner());
        verifyNoInteractions(reader);
    }

    @Test void currentTenantIsRequiredAndNeverInferredFromTask() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> policy.inspect(task, 7L));
        verifyNoInteractions(requests, forms, reader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"requestTenant", "requestProcess", "taskTenant", "processTenant", "processDefinition",
            "definitionKey", "businessKey", "system", "domain", "action", "requestDefinition", "requestStatus",
            "jsonObject", "jsonApplicant", "objectType", "deletedRequest", "formTenant", "formProcess"})
    void inconsistentFormalFactsFailBeforeDomainRead(String mutation) {
        switch (mutation) {
            case "requestTenant" -> request.setTenantId(123L);
            case "requestProcess" -> request.setProcessInstanceId("another");
            case "taskTenant" -> when(task.getTenantId()).thenReturn("123");
            case "processTenant" -> when(process.getTenantId()).thenReturn("123");
            case "processDefinition" -> when(process.getProcessDefinitionId()).thenReturn("another");
            case "definitionKey" -> when(definition.getKey()).thenReturn("another");
            case "businessKey" -> when(process.getBusinessKey()).thenReturn("BUSINESS_APPROVAL:20");
            case "system" -> request.setSystemCode("OTHER");
            case "domain" -> request.setDataDomain("OTHER");
            case "action" -> request.setActionCode("OTHER");
            case "requestDefinition" -> request.setProcessDefinitionKey("another");
            case "requestStatus" -> request.setRequestStatus("APPROVED");
            case "jsonObject" -> request.setObjectId("302");
            case "jsonApplicant" -> request.setApplicantUserId(8L);
            case "objectType" -> request.setObjectType("OTHER");
            case "deletedRequest" -> request.setDeleted(true);
            case "formTenant" -> { var form = form(); form.setTenantId(123L);
                when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(form)); }
            case "formProcess" -> { var form = form(); form.setBpmProcessInstanceId("another");
                when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(form)); }
            default -> throw new AssertionError(mutation);
        }
        assertThrows(IllegalStateException.class, () -> policy.inspect(task, 7L));
        verifyNoInteractions(reader);
    }

    private FormActionInstanceDO form() {
        return FormActionInstanceDO.builder().id(90L).tenantId(122L).bpmProcessInstanceId("process-1").build();
    }

    private void assertFailure(String code) {
        assertTrue(assertThrows(IllegalStateException.class, () -> policy.inspect(task, 7L)).getMessage().contains(code));
        verifyNoInteractions(reader);
    }
}
