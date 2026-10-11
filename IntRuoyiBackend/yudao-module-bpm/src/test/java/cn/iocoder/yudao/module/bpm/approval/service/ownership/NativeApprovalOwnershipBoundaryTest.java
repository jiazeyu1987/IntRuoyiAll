package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.bpm.approval.service.signature.*;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.system.api.permission.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Executes the production coordinator, native provider, center and generic guard together. */
class NativeApprovalOwnershipBoundaryTest {
    private final BusinessApprovalRequestMapper requests = mock(BusinessApprovalRequestMapper.class);
    private final FormActionInstanceMapper forms = mock(FormActionInstanceMapper.class);
    private final BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
    private final BpmProcessInstanceService processes = mock(BpmProcessInstanceService.class);
    private final BpmTaskService tasks = mock(BpmTaskService.class);
    private final org.flowable.engine.TaskService engineTasks = mock(org.flowable.engine.TaskService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ApprovalSignatureRecordService signatures = mock(ApprovalSignatureRecordService.class);
    private final EdhrEngineOwnershipReader domain = mock(EdhrEngineOwnershipReader.class);
    private final NativeApprovalOwnershipPolicy policy = new NativeApprovalOwnershipPolicy(requests, forms, definitions, processes, domain);
    private final BpmNativeApprovalTaskProvider provider = new BpmNativeApprovalTaskProvider(processes,
            mock(BpmProcessInstanceCopyService.class), tasks, engineTasks, permissions, mock(RoleApi.class));
    private final ApprovalFlowableTaskQueryService centerQuery=mock(ApprovalFlowableTaskQueryService.class);
    private Task task;
    private BusinessApprovalRequestDO request;
    private ApprovalCenterService center;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(122L);
        org.springframework.test.util.ReflectionTestUtils.setField(policy,"formalOwnership",mock(ApprovalProcessOwnershipService.class));
        ReflectionTestUtils.setField(provider, "ownershipPolicy", policy);
        ReflectionTestUtils.setField(provider,"centerQuery",centerQuery);
        task = mock(Task.class);
        when(task.getId()).thenReturn("engine-1");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        when(task.getProcessDefinitionId()).thenReturn("definition-1");
        when(task.getTaskDefinitionKey()).thenReturn("reviewNode");
        when(task.getTenantId()).thenReturn("122");
        when(task.getAssignee()).thenReturn("7");
        when(tasks.getTask("engine-1")).thenReturn(task);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition-1");
        when(definition.getKey()).thenReturn("mes-edhr-approval-v1");
        when(definitions.getProcessDefinition("definition-1")).thenReturn(definition);
        ProcessInstance process = mock(ProcessInstance.class);
        when(process.getId()).thenReturn("process-1");
        when(process.getTenantId()).thenReturn("122");
        when(process.getProcessDefinitionId()).thenReturn("definition-1");
        when(process.getProcessDefinitionKey()).thenReturn("mes-edhr-approval-v1");
        when(process.getBusinessKey()).thenReturn("BUSINESS_APPROVAL:19");
        when(processes.getProcessInstance("process-1")).thenReturn(process);
        when(processes.getProcessInstanceMap(any())).thenReturn(Map.of("process-1", process));
        when(centerQuery.todo(any(),eq("BPM"))).thenReturn(new PageResult<>(List.of(task), 9L));
        var context = BusinessApprovalContext.builder().tenantId(122L).dataDomain("MES").systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION").objectId("301").objectVersion("2").actionCode("SUBMIT_REVIEW")
                .objectState("DRAFT").applicantUserId(7L).build();
        request = BusinessApprovalRequestDO.builder().id(19L).tenantId(122L).policyMode("BPM_REQUIRED")
                .dataDomain("MES").systemCode("MES").objectType("EDHR_BATCH_EXECUTION").objectId("301")
                .objectVersion("2").actionCode("SUBMIT_REVIEW").objectState("DRAFT").applicantUserId(7L)
                .effectExecutorCode("EDHR_BATCH_EXECUTION_SUBMIT_REVIEW")
                .requestStatus("PENDING_BPM").processDefinitionKey("mes-edhr-approval-v1").processInstanceId("process-1")
                .businessContextJson(JsonUtils.toJsonString(context)).build();
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(request));
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        when(domain.read(any(), any(), any())).thenReturn(owner(null));
        center = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(provider)), permissions,
                mock(AdminUserApi.class), signatures);
    }

    @AfterEach void clean() { TenantContextHolder.clear(); }

    @Test void nativeTodoRetainsEngineIdentityAndTotalWithOnlyDomainProcessingAction() {
        var page = provider.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10));
        assertEquals(9L, page.getTotal());
        var summary = page.getList().get(0);
        assertEquals("engine-1", summary.getSourceTaskId());
        assertEquals(Set.of("OPEN_DETAIL", "PROCESS_IN_MODULE"), summary.getAvailableActions());
        assertEquals("/mes/pro/feedback/edhr-approval/detail", summary.getDecisionDetailRoute());
        assertEquals(Map.of("id", "301", "workTaskId", "401", "bpmTaskId", "engine-1"), summary.getDecisionDetailQuery());
        assertNull(summary.getBusinessContextTags(), "domain navigation without a blocker needs no display tags");
    }

    @Test void multipleDomainCellsRemainReadableWithoutInventedTaskOrGenericActions() {
        when(domain.read(any(), any(), any())).thenReturn(owner("EDHR_ENGINE_WORK_TASK_CONTEXT_AMBIGUOUS"));
        var summary = provider.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);
        assertEquals(Set.of("OPEN_DETAIL"), summary.getAvailableActions());
        assertNull(summary.getDecisionDetailRoute());
        assertEquals(Map.of(), summary.getDecisionDetailQuery());
        assertTrue(summary.getBusinessContextTags().contains("存在多个可办理签字格，缺少按执行记录选择工作任务的入口"));
    }

    @Test void doneHistoryKeepsBpmReadEntryWithoutCurrentOwnerExclusionOrActions() {
        var historicTask = mock(org.flowable.task.api.history.HistoricTaskInstance.class);
        when(historicTask.getId()).thenReturn("historic-engine");
        when(historicTask.getProcessInstanceId()).thenReturn("process-1");
        var historicProcess = mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(historicProcess.getId()).thenReturn("process-1");
        when(centerQuery.done(any(),eq("BPM"))).thenReturn(new PageResult<>(List.of(historicTask), 1L));
        when(processes.getHistoricProcessInstances(Set.of("process-1"))).thenReturn(List.of(historicProcess));
        var page = provider.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10));
        assertEquals(1L, page.getTotal());
        assertEquals(Set.of("OPEN_DETAIL"), page.getList().get(0).getAvailableActions());
        assertEquals("/bpm/process-instance/detail", page.getList().get(0).getDetailRoute());
        verifyNoInteractions(requests, forms, domain);
    }

    @Test void myInitiatedAndCcAreNotExcludedByCurrentDomainOwner() {
        var historicProcess = mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(historicProcess.getId()).thenReturn("process-1");
        when(centerQuery.initiated(any())).thenReturn(new PageResult<>(List.of(historicProcess), 1L));
        var initiated = provider.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.MY_INITIATED,
                ApprovalModuleCode.BPM, null, 1, 10));
        assertEquals(1L, initiated.getTotal());
        assertEquals(1, initiated.getList().size());
        var copies = mock(BpmProcessInstanceCopyService.class);
        var copy = new cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO();
        copy.setId(88L); copy.setProcessInstanceId("process-1");
        when(centerQuery.copied(any())).thenReturn(new PageResult<>(List.of(copy), 1L));
        var copyProvider = new BpmNativeApprovalTaskProvider(processes, copies, tasks, engineTasks, permissions, mock(RoleApi.class));
        ReflectionTestUtils.setField(copyProvider, "ownershipPolicy", policy);
        ReflectionTestUtils.setField(copyProvider,"centerQuery",centerQuery);
        var copied = copyProvider.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.CC, ApprovalModuleCode.BPM, null, 1, 10));
        assertEquals(1L, copied.getTotal()); assertEquals(1, copied.getList().size());
        verifyNoInteractions(requests, forms, domain);
    }

    @ParameterizedTest @EnumSource(ApprovalTaskReviewResult.class)
    void centerRejectsFormalEdhrBeforeSignatureImageClaimAndMutation(ApprovalTaskReviewResult result) {
        var kernel = mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureService.class);
        var images = mock(ApprovalSignatureImageSnapshotProvider.class);
        var actualCenter = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(provider)), permissions,
                mock(AdminUserApi.class), new ApprovalSignatureRecordServiceImpl(kernel, images));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> actualCenter.reviewTask(7L, command(result)));
        verifyNoInteractions(signatures, kernel, images, engineTasks);
        verify(tasks, never()).approveTask(any(), any());
        verify(tasks, never()).rejectTask(any(), any());
    }

    @ParameterizedTest @EnumSource(ApprovalTaskReviewResult.class)
    void nativeReviewRechecksBeforeClaimAndMutation(ApprovalTaskReviewResult result) {
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> provider.review(review(result)));
        verifyNoInteractions(engineTasks);
        verify(tasks, never()).approveTask(any(), any());
        verify(tasks, never()).rejectTask(any(), any());
    }

    @ParameterizedTest @EnumSource(ApprovalTaskReviewResult.class)
    void genuineFormSharedKeyKeepsCenterSignatureAndNativeMutation(ApprovalTaskReviewResult result) {
        useForm();
        when(signatures.recordReviewSignature(any())).thenReturn(ApprovalSignatureRecordResult.builder()
                .subjectId("subject").evidenceHash("evidence").signatureImageFileUrl("/signed.png").build());
        center.reviewTask(7L, command(result));
        verify(signatures).recordReviewSignature(any());
        if (result == ApprovalTaskReviewResult.APPROVE) verify(tasks).approveTask(eq(7L), any());
        else verify(tasks).rejectTask(eq(7L), any());
    }

    @Test void signatureToReviewRaceCannotUseEarlierFormOwnershipToClaim() {
        useForm();
        when(signatures.recordReviewSignature(any())).thenAnswer(invocation -> {
            when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(request));
            when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
            return ApprovalSignatureRecordResult.builder().subjectId("subject").evidenceHash("evidence")
                    .signatureImageFileUrl("/signed.png").build();
        });
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> center.reviewTask(7L, command(ApprovalTaskReviewResult.APPROVE)));
        verify(signatures).recordReviewSignature(any());
        verifyNoInteractions(engineTasks);
        verify(tasks, never()).approveTask(any(), any());
    }

    @Test void forgedProcessFailsBeforePolicyOrSignature() {
        assertThrows(IllegalArgumentException.class, () -> center.reviewTask(7L,
                command(ApprovalTaskReviewResult.APPROVE).setProcessInstanceId("forged")));
        verifyNoInteractions(requests, forms, domain, signatures, engineTasks);
    }

    @Test void actualGenericGuardUsesSameConflictAndFormRules() {
        BpmTaskExternalSignatureGuard guard = new BpmTaskExternalSignatureGuard();
        ReflectionTestUtils.setField(guard, "processDefinitionService", definitions);
        ReflectionTestUtils.setField(guard, "formActionInstanceMapper", forms);
        ReflectionTestUtils.setField(guard, "ownershipPolicy", policy);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> guard.assertGenericTaskMutationAllowed(task));
        useForm();
        guard.assertGenericTaskMutationAllowed(task);
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(request));
        assertTrue(assertThrows(IllegalStateException.class, () -> guard.assertGenericTaskMutationAllowed(task))
                .getMessage().contains("FORM_EDHR_CONFLICT"));
    }

    private void useForm() {
        when(requests.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of());
        when(forms.selectOwnershipByTenantAndProcessId(122L, "process-1")).thenReturn(List.of(
                FormActionInstanceDO.builder().id(91L).tenantId(122L).bpmProcessInstanceId("process-1").build()));
    }
    private NativeApprovalOwnership owner(String blocker) {
        return new NativeApprovalOwnership(NativeApprovalOwnership.Owner.EDHR_EXECUTION,
                blocker == null ? "/mes/pro/feedback/edhr-approval/detail" : null,
                blocker == null ? Map.of("id", "301", "workTaskId", "401", "bpmTaskId", "engine-1") : Map.of(), blocker);
    }
    private ApprovalTaskReviewCommand command(ApprovalTaskReviewResult result) {
        return new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.BPM).setSourceTaskType("BPM_TASK_TODO")
                .setSourceTaskId("engine-1").setProcessInstanceId("process-1").setBusinessKey("process-1")
                .setResult(result).setReason("reviewed evidence").setSignaturePassword("test-only");
    }
    private ApprovalTaskReviewContext review(ApprovalTaskReviewResult result) {
        return ApprovalTaskReviewContext.of(7L, ApprovalModuleCode.BPM, "BPM_TASK_TODO", "engine-1", "process-1",
                "process-1", result, "reviewed evidence", "test-only", false).setSignatureImageFileUrl("/signed.png");
    }
}
