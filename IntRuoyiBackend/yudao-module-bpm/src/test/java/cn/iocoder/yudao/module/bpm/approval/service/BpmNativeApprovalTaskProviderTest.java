package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.NativeApprovalOwnershipPolicy;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCopyPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstancePageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BpmNativeApprovalTaskProviderTest {

    @Mock
    private BpmProcessInstanceService processInstanceService;
    @Mock
    private BpmProcessInstanceCopyService copyService;
    @Mock
    private BpmTaskService taskService;
    @Mock
    private org.flowable.engine.TaskService flowableTaskService;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private ApprovalFlowableTaskQueryService centerQuery;
    @InjectMocks
    private BpmNativeApprovalTaskProvider provider;

    @BeforeEach
    void stubRuntimeProcessInstancesForExistingScenarios() {
        TenantContextHolder.setTenantId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "centerQuery", centerQuery);
        var requests = mock(cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper.class);
        var forms = mock(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class);
        var definitions = mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        var definition = mock(org.flowable.engine.repository.ProcessDefinition.class);
        lenient().when(definition.getKey()).thenReturn("ordinary-native-test-process");
        lenient().when(definitions.getProcessDefinition("native-test-definition")).thenReturn(definition);
        lenient().when(requests.selectOwnershipByTenantAndProcessId(eq(1L), any())).thenReturn(List.of());
        lenient().when(forms.selectOwnershipByTenantAndProcessId(eq(1L), any())).thenReturn(List.of());
        var ownership = new NativeApprovalOwnershipPolicy(requests, forms, definitions, processInstanceService,
                mock(cn.iocoder.yudao.module.bpm.approval.service.ownership.EdhrEngineOwnershipReader.class));
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "ownershipPolicy", ownership);
        lenient().when(taskService.getTask(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            Task task = nativeTask();
            lenient().when(task.getId()).thenReturn(id);
            lenient().when(task.getProcessInstanceId()).thenReturn(id.replaceFirst("task-", "pi-"));
            return task;
        });
        lenient().when(processInstanceService.getProcessInstanceMap(any())).thenAnswer(invocation -> {
            Set<String> ids = invocation.getArgument(0);
            Map<String, ProcessInstance> processInstances = new java.util.LinkedHashMap<>();
            ids.forEach(id -> processInstances.put(id, mock(ProcessInstance.class)));
            return processInstances;
        });
    }

    private Task nativeTask() {
        Task task = mock(Task.class);
        lenient().when(task.getProcessDefinitionId()).thenReturn("native-test-definition");
        lenient().when(task.getTenantId()).thenReturn("1");
        return task;
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void pageTodoMapsNativeBpmTodoTasksToUnifiedSummary() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-100");
        when(task.getName()).thenReturn("通用审批");
        when(task.getTaskDefinitionKey()).thenReturn("userTask");
        when(task.getProcessInstanceId()).thenReturn("pi-100");
        when(task.getAssignee()).thenReturn("910272");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "通用", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_TASK_TODO:task-100", summary.getId());
        assertEquals(ApprovalModuleCode.BPM, summary.getModuleCode());
        assertEquals("BPM_TASK_TODO", summary.getSourceTaskType());
        assertEquals("通用审批", summary.getBusinessTitle());
        assertEquals("TODO", summary.getBusinessStatus());
        assertNull(summary.getBusinessContextTags(), "ordinary tasks keep the existing absent-tag contract");
        assertEquals(910272L, summary.getAssigneeUserId());
        assertNull(summary.getAssigneeRoleCode());
        assertNull(summary.getAssigneeRoleName());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/bpm/process-instance/detail", summary.getDetailRoute());
        assertEquals("pi-100", summary.getDetailQuery().get("id"));
        assertEquals("task-100", summary.getDetailQuery().get("taskId"));
        assertTrue(summary.getAvailableActions().contains("OPEN_DETAIL"));
        assertTrue(summary.getAvailableActions().contains("APPROVE"));
        assertTrue(summary.getAvailableActions().contains("REJECT"));

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).todo(captor.capture(),eq("BPM"));
        assertEquals("通用",captor.getValue().getKeyword());
    }

    @Test
    void pageTodoFindsAssignedTaskWhenKeywordIsProcessInstanceId() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-access");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-access");
        when(task.getAssignee()).thenReturn("149");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(149L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "pi-regcert-access", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_TASK_TODO:task-regcert-access", summary.getId());
        assertEquals("task-regcert-access", summary.getSourceTaskId());
        assertEquals("pi-regcert-access", summary.getBusinessKey());
        assertEquals("pi-regcert-access", summary.getProcessInstanceId());
        assertEquals(149L, summary.getAssigneeUserId());
        assertTrue(summary.getAvailableActions().contains("APPROVE"));
        ArgumentCaptor<ApprovalTaskQueryContext> query = ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).todo(query.capture(), eq("BPM"));
        assertEquals("pi-regcert-access", query.getValue().getKeyword());
        assertEquals(149L, query.getValue().getLoginUserId());
        assertEquals(ApprovalTaskViewType.TODO, query.getValue().getViewType());
    }

    @Test
    void pageTodoUsesBatchRecordVersionVariablesForBusinessTitle() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-batch-version");
        when(task.getName()).thenReturn("批记录升版审核");
        when(task.getTaskDefinitionKey()).thenReturn("batchRecordVersionApprove");
        when(task.getProcessInstanceId()).thenReturn("pi-batch-version");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of(
                "businessType", "BATCH_RECORD_VERSION_APPROVAL",
                "batchRecordName", "球囊扩张压力泵",
                "versionNo", "V4.0"));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "球囊", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("批记录升版 球囊扩张压力泵 V4.0", summary.getBusinessTitle());
        assertEquals("批记录升版审核", summary.getCurrentNodeName());
    }

    @Test
    void pageTodoUsesEdhrExecutionVariablesForReadableBusinessSummary() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-edhr-execution");
        when(task.getName()).thenReturn("eDHR Approval V1");
        when(task.getTaskDefinitionKey()).thenReturn("approveNode");
        when(task.getProcessInstanceId()).thenReturn("pi-edhr-execution");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of(
                "edhrExecutionId", 7001L,
                "edhrExecutionCode", "EDHR-20260830-001",
                "workOrderId", 6001L,
                "workOrderCode", "WO-20260830-001",
                "batchCode", "BATCH-20260830-001",
                "processName", "组装",
                "workstationName", "组装工位A"));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "EDHR", 1, 10)).getList().get(0);

        assertEquals("电子批记录审核 EDHR-20260830-001 工单 WO-20260830-001 批次 BATCH-20260830-001 工序 组装",
                summary.getBusinessTitle());
        assertEquals("EDHR-20260830-001", summary.getBusinessCode());
        assertEquals(List.of("工单：WO-20260830-001", "批次：BATCH-20260830-001", "工序：组装", "工作站：组装工位A"),
                summary.getBusinessContextTags());
        assertEquals("电子批记录审核", summary.getCurrentNodeName());
    }

    @Test
    void pageTodoUsesRegistrationCertificateAccessVariablesForReadableBusinessSummary() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-access-summary");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-access-summary");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8801L),
                Map.entry("requestId", 8801L),
                Map.entry("certificateId", 7701L),
                Map.entry("ownerCompanyId", 6601L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("requestOperation", "UPLOAD_CERTIFICATE"),
                Map.entry("requestKey", "REG-UPLOAD-20260830-001"),
                Map.entry("certificateNo", "国械注准20263000001"),
                Map.entry("classification", "III类"),
                Map.entry("productName", "一次性使用无菌导管"),
                Map.entry("ownerCompanyName", "示例医疗器械有限公司")));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "REG-UPLOAD", 1, 10)).getList().get(0);

        assertEquals("注册证上传审批 国械注准20263000001", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("注册证编号：国械注准20263000001", "分类：III类", "产品：一次性使用无菌导管",
                        "所属公司名称：示例医疗器械有限公司"),
                summary.getBusinessContextTags());
        assertEquals("注册证访问审批", summary.getCurrentNodeName());
        assertEquals("dcc_registration_certificate_approver", summary.getAssigneeRoleCode());
        assertEquals("注册部经理", summary.getAssigneeRoleName());
        assertEquals("/mdm/registration-certificate/detail/7701", summary.getDecisionDetailRoute());
        assertEquals("8801", summary.getDecisionDetailQuery().get("requestId"));
        assertEquals("pi-regcert-access-summary", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoUsesRenewalOperationForRegistrationCertificateApprovalTitle() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-renewal-summary");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-renewal-summary");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8802L),
                Map.entry("requestId", 8802L),
                Map.entry("certificateId", 7702L),
                Map.entry("ownerCompanyId", 6601L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("requestOperation", "RENEWAL_CERTIFICATE"),
                Map.entry("requestKey", "DCC-REG-CERT-RENEWAL-20260831-001"),
                Map.entry("certificateNo", "国械注准20263000002"),
                Map.entry("classification", "II类"),
                Map.entry("productName", "球囊扩张导管"),
                Map.entry("ownerCompanyName", "示例医疗器械有限公司")));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "RENEWAL", 1, 10)).getList().get(0);

        assertEquals("注册证延续审批 国械注准20263000002", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("注册证编号：国械注准20263000002", "分类：II类", "产品：球囊扩张导管",
                        "所属公司名称：示例医疗器械有限公司"),
                summary.getBusinessContextTags());
        assertEquals("dcc_registration_certificate_approver", summary.getAssigneeRoleCode());
        assertEquals("注册部经理", summary.getAssigneeRoleName());
        assertEquals("/mdm/registration-certificate/detail/7702", summary.getDecisionDetailRoute());
        assertEquals("8802", summary.getDecisionDetailQuery().get("requestId"));
        assertEquals("pi-regcert-renewal-summary", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoUsesChangeOperationForRegistrationCertificateApprovalTitle() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-change-summary");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-change-summary");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8805L),
                Map.entry("requestId", 8805L),
                Map.entry("certificateId", 7705L),
                Map.entry("ownerCompanyId", 6601L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("requestOperation", "CHANGE_CERTIFICATE"),
                Map.entry("requestKey", "DCC-REG-CERT-CHANGE-20260902-001"),
                Map.entry("productName", "球囊扩张导管变更后")));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "CHANGE", 1, 10)).getList().get(0);

        assertEquals("注册证变更审批", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("产品：球囊扩张导管变更后"), summary.getBusinessContextTags());
        assertEquals("dcc_registration_certificate_approver", summary.getAssigneeRoleCode());
        assertEquals("注册部经理", summary.getAssigneeRoleName());
        assertEquals("/mdm/registration-certificate/detail/7705", summary.getDecisionDetailRoute());
        assertEquals("8805", summary.getDecisionDetailQuery().get("requestId"));
        assertEquals("pi-regcert-change-summary", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoUsesRegistrationCertificateDetailOperationForUploadAndRenewalSummary() {
        Task uploadTask = nativeTask();
        when(uploadTask.getId()).thenReturn("task-regcert-upload-detail-operation");
        when(uploadTask.getName()).thenReturn("Registration certificate access approval");
        when(uploadTask.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(uploadTask.getProcessInstanceId()).thenReturn("pi-regcert-upload-detail-operation");
        when(uploadTask.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(uploadTask.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8810L),
                Map.entry("requestId", 8810L),
                Map.entry("certificateId", 7710L),
                Map.entry("ownerCompanyId", 6610L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("operation", "UPLOAD_CERTIFICATE"),
                Map.entry("requestKey", "DCC-REG-CERT-UPLOAD-REAL-001"),
                Map.entry("certificateNo", "国械注准20263000010"),
                Map.entry("classification", "二类"),
                Map.entry("productName", "按压式球囊扩充压力泵"),
                Map.entry("ownerCompanyName", "上海七木医疗器械有限公司")));

        Task renewalTask = nativeTask();
        when(renewalTask.getId()).thenReturn("task-regcert-renewal-detail-operation");
        when(renewalTask.getName()).thenReturn("Registration certificate access approval");
        when(renewalTask.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(renewalTask.getProcessInstanceId()).thenReturn("pi-regcert-renewal-detail-operation");
        when(renewalTask.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(renewalTask.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8811L),
                Map.entry("requestId", 8811L),
                Map.entry("certificateId", 7711L),
                Map.entry("ownerCompanyId", 6610L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("operation", "RENEWAL_CERTIFICATE"),
                Map.entry("requestKey", "DCC-REG-CERT-RENEWAL-REAL-001"),
                Map.entry("certificateNo", "国械注准20263000011"),
                Map.entry("classification", "三类"),
                Map.entry("productName", "按压式球囊扩充压力泵"),
                Map.entry("ownerCompanyName", "上海七木医疗器械有限公司")));

        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(uploadTask, renewalTask), 2L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "REAL", 1, 10));

        assertEquals(2L, page.getTotal());
        ApprovalTaskSummary uploadSummary = page.getList().get(0);
        assertEquals("注册证上传审批 国械注准20263000010", uploadSummary.getBusinessTitle());
        assertNull(uploadSummary.getBusinessCode());
        assertEquals(Boolean.TRUE, uploadSummary.getBusinessIdentifierHidden());
        assertEquals(List.of("注册证编号：国械注准20263000010", "分类：二类", "产品：按压式球囊扩充压力泵",
                        "所属公司名称：上海七木医疗器械有限公司"),
                uploadSummary.getBusinessContextTags());

        ApprovalTaskSummary renewalSummary = page.getList().get(1);
        assertEquals("注册证延续审批 国械注准20263000011", renewalSummary.getBusinessTitle());
        assertNull(renewalSummary.getBusinessCode());
        assertEquals(Boolean.TRUE, renewalSummary.getBusinessIdentifierHidden());
        assertEquals(List.of("注册证编号：国械注准20263000011", "分类：三类", "产品：按压式球囊扩充压力泵",
                        "所属公司名称：上海七木医疗器械有限公司"),
                renewalSummary.getBusinessContextTags());
    }

    @Test
    void pageTodoUsesProcessVariablesWhenRegistrationCertificateTaskHasNoLocalVariables() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-runtime-vars");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-runtime-vars");
        when(task.getAssignee()).thenReturn("100");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));

        Map<String, Object> processVariables = Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8820L),
                Map.entry("requestId", 8820L),
                Map.entry("certificateId", 7720L),
                Map.entry("ownerCompanyId", 6620L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("requestOperation", "UPLOAD_CERTIFICATE"),
                Map.entry("requestKey", "DCC-REG-CERT-UPLOAD-RUNTIME-001"),
                Map.entry("certificateNo", "国械注准20263000020"),
                Map.entry("classification", "二类"),
                Map.entry("productName", "按压式球囊扩充压力泵"),
                Map.entry("ownerCompanyName", "上海七木医疗器械有限公司"));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("100");
        when(processInstance.getProcessVariables()).thenReturn(processVariables);

        org.mockito.Mockito.doReturn(Map.of("pi-regcert-runtime-vars", processInstance))
                .when(processInstanceService)
                .getProcessInstanceMap(anySet());
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "RUNTIME", 1, 10)).getList().get(0);

        assertEquals("注册证上传审批 国械注准20263000020", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("注册证编号：国械注准20263000020", "分类：二类", "产品：按压式球囊扩充压力泵",
                        "所属公司名称：上海七木医疗器械有限公司"),
                summary.getBusinessContextTags());
        assertEquals("dcc_registration_certificate_approver", summary.getAssigneeRoleCode());
        assertEquals("注册部经理", summary.getAssigneeRoleName());
        assertEquals("/mdm/registration-certificate/detail/7720", summary.getDecisionDetailRoute());
        assertEquals("8820", summary.getDecisionDetailQuery().get("requestId"));
        assertEquals("pi-regcert-runtime-vars", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoHidesMissingRegistrationCertificateSummaryTags() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-summary-missing");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-summary-missing");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8803L),
                Map.entry("requestId", 8803L),
                Map.entry("certificateId", 7703L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("requestOperation", "RENEWAL_CERTIFICATE"),
                Map.entry("productName", "球囊扩张导管")));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals("注册证延续审批", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("产品：球囊扩张导管"), summary.getBusinessContextTags());
    }

    @Test
    void pageTodoDoesNotFailWhenRegistrationCertificateOperationIsMissing() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-regcert-operation-missing");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-regcert-operation-missing");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.ofEntries(
                Map.entry("registrationCertificateAccessRequestId", 8804L),
                Map.entry("requestId", 8804L),
                Map.entry("certificateId", 7704L),
                Map.entry("requestType", "UPLOAD_CERTIFICATE"),
                Map.entry("productName", "球囊扩张导管")));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals("注册证审批", summary.getBusinessTitle());
        assertNull(summary.getBusinessCode());
        assertEquals(Boolean.TRUE, summary.getBusinessIdentifierHidden());
        assertEquals(List.of("产品：球囊扩张导管"), summary.getBusinessContextTags());
    }

    @Test
    void pageTodoMapsBatchRecordVersionVariablesToDecisionDetailRoute() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-batch-version-detail");
        when(task.getName()).thenReturn("批记录升版审核");
        when(task.getTaskDefinitionKey()).thenReturn("batchRecordVersionApprove");
        when(task.getProcessInstanceId()).thenReturn("pi-batch-version-detail");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of(
                "businessType", "BATCH_RECORD_VERSION_APPROVAL",
                "batchRecordDefinitionId", 501L,
                "batchRecordName", "PTCA球囊扩张导管",
                "batchRecordVersionId", 9001L,
                "versionNo", "V5.0",
                "sourceVersionId", 8001L,
                "sourceVersionNo", "V4.0"));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "PTCA", 1, 10)).getList().get(0);

        assertEquals("/bpm/process-instance/detail", summary.getDetailRoute());
        assertEquals("pi-batch-version-detail", summary.getDetailQuery().get("id"));
        assertEquals("/mes/pro/batch-record-form-list", summary.getDecisionDetailRoute());
        assertEquals("BATCH_RECORD_VERSION_APPROVAL", summary.getDecisionDetailQuery().get("businessType"));
        assertEquals("9001", summary.getDecisionDetailQuery().get("batchRecordVersionId"));
        assertEquals("V5.0", summary.getDecisionDetailQuery().get("versionNo"));
        assertEquals("8001", summary.getDecisionDetailQuery().get("sourceVersionId"));
        assertEquals("V4.0", summary.getDecisionDetailQuery().get("sourceVersionNo"));
        assertEquals("pi-batch-version-detail", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoMapsRouteVersionPublishVariablesToReadableTitleAndRouteDetail() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-route-version-detail");
        when(task.getName()).thenReturn("${routeName}发布审批");
        when(task.getTaskDefinitionKey()).thenReturn("routeVersionPublishApprove");
        when(task.getProcessInstanceId()).thenReturn("pi-route-version-detail");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of(
                "businessType", "MES_ROUTE_VERSION_PUBLISH",
                "objectId", "1002",
                "objectVersion", "V2",
                "routeId", 9001L,
                "routeVersionId", 1002L,
                "routeVersionNo", "V2",
                "routeCode", "RT-001",
                "routeName", "球囊扩张压力泵工艺路线"));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "工艺路线", 1, 10)).getList().get(0);

        assertEquals("工艺路线发布 球囊扩张压力泵工艺路线 V2", summary.getBusinessTitle());
        assertFalse(summary.getBusinessTitle().contains("${"));
        assertEquals("/bpm/process-instance/detail", summary.getDetailRoute());
        assertEquals("/mes/pro/route/edit/9001", summary.getDecisionDetailRoute());
        assertEquals("MES_ROUTE_VERSION_PUBLISH", summary.getDecisionDetailQuery().get("businessType"));
        assertEquals("9001", summary.getDecisionDetailQuery().get("routeId"));
        assertEquals("1002", summary.getDecisionDetailQuery().get("routeVersionId"));
        assertEquals("V2", summary.getDecisionDetailQuery().get("routeVersionNo"));
        assertEquals("PENDING_APPROVAL", summary.getDecisionDetailQuery().get("routeVersionStatus"));
        assertEquals("flow", summary.getDecisionDetailQuery().get("tab"));
        assertEquals("pi-route-version-detail", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void pageTodoMapsEdhrBatchVoidVariablesToDecisionDetailRoute() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-void-detail");
        when(task.getName()).thenReturn("作废审核");
        when(task.getTaskDefinitionKey()).thenReturn("batchExecutionVoidApprove");
        when(task.getProcessInstanceId()).thenReturn("pi-void-detail");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of(
                "businessType", "EDHR_BATCH_EXECUTION_VOID",
                "batchExecutionId", 7001L,
                "batchExecutionCode", "EDHRB-1783609501380",
                "workOrderId", 6001L,
                "workOrderCode", "WO-20260717",
                "batchCode", "BATCH-VOID-001",
                "reasonCategory", "QUALITY_REVIEW",
                "reasonText", "质量复核要求作废"));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "作废", 1, 10)).getList().get(0);

        assertEquals("电子批记录批次作废 EDHRB-1783609501380 批次 BATCH-VOID-001 工单 WO-20260717",
                summary.getBusinessTitle());
        assertEquals("EDHRB-1783609501380", summary.getBusinessCode());
        assertEquals(List.of("工单：WO-20260717", "批次：BATCH-VOID-001", "原因：质量复核要求作废"),
                summary.getBusinessContextTags());
        assertEquals("电子批记录批次作废审核", summary.getCurrentNodeName());
        assertEquals("/mes/pro/feedback/edhr-change", summary.getDecisionDetailRoute());
        assertEquals("EDHR_BATCH_EXECUTION_VOID", summary.getDecisionDetailQuery().get("businessType"));
        assertEquals("VOID", summary.getDecisionDetailQuery().get("changeType"));
        assertEquals("7001", summary.getDecisionDetailQuery().get("batchExecutionId"));
        assertEquals("EDHRB-1783609501380", summary.getDecisionDetailQuery().get("batchExecutionCode"));
        assertEquals("WO-20260717", summary.getDecisionDetailQuery().get("workOrderCode"));
        assertEquals("BATCH-VOID-001", summary.getDecisionDetailQuery().get("batchCode"));
        assertEquals("pi-void-detail", summary.getDecisionDetailQuery().get("processInstanceId"));
    }

    @Test
    void reviewApprovesNativeBpmTodoTaskThroughTaskService() {
        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-approve-100", "pi-approve-100", "pi-approve-100",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png"));

        ArgumentCaptor<BpmTaskApproveReqVO> captor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(taskService).approveTask(eq(100L), captor.capture());
        assertEquals("task-approve-100", captor.getValue().getId());
    }

    @Test
    void reviewApprovesNativeBpmTodoTaskWithApprovalCenterSignatureImageUrl() {
        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-approve-101", "pi-approve-101", "pi-approve-101",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png");

        provider.review(context);

        ArgumentCaptor<BpmTaskApproveReqVO> captor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(taskService).approveTask(eq(100L), captor.capture());
        assertEquals("task-approve-101", captor.getValue().getId());
        assertEquals("http://127.0.0.1:9000/yudao/signature/user-100.png", captor.getValue().getSignPicUrl());
    }

    @Test
    void reviewClaimsRegistrationUploadTaskWhenLoginUserIsSelectedCandidateAndHasApproverRoleAndPermission() {
        Task task = nativeTask();
        when(task.getAssignee()).thenReturn("200");
        when(task.getProcessInstanceId()).thenReturn("pi-upload-approve");
        lenient().when(task.getId()).thenReturn("task-upload-approve");
        when(taskService.getTask("task-upload-approve")).thenReturn(task);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                "registrationCertificateAccessRequestId", 150L,
                "requestId", 150L,
                "certificateId", 990819196L,
                "requestType", "UPLOAD_CERTIFICATE",
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                Map.of("REG_CERT_ACCESS_APPROVAL", List.of(100L, 200L))));
        when(processInstanceService.getProcessInstance("pi-upload-approve")).thenReturn(processInstance);
        when(centerQuery.isFormalRegistrationCandidate(org.mockito.ArgumentMatchers.anyString(),eq(100L))).thenReturn(true);

        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-upload-approve", "pi-upload-approve", "pi-upload-approve",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png"));

        verify(flowableTaskService).setAssignee("task-upload-approve", "100");
        ArgumentCaptor<BpmTaskApproveReqVO> captor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(taskService).approveTask(eq(100L), captor.capture());
        assertEquals("task-upload-approve", captor.getValue().getId());
    }

    @Test
    void reviewClaimsRegistrationDownloadTaskWhenLoginUserIsSelectedCandidateAndHasAccessApprovalPermission() {
        Task task = nativeTask();
        when(task.getAssignee()).thenReturn("200");
        when(task.getProcessInstanceId()).thenReturn("pi-download-approve");
        lenient().when(task.getId()).thenReturn("task-download-approve");
        when(taskService.getTask("task-download-approve")).thenReturn(task);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                "registrationCertificateAccessRequestId", 300L,
                "requestId", 300L,
                "certificateId", 990819129L,
                "requestType", "DOWNLOAD_FILE",
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                Map.of("REG_CERT_ACCESS_APPROVAL", List.of(100L, 200L))));
        when(processInstanceService.getProcessInstance("pi-download-approve")).thenReturn(processInstance);
        when(centerQuery.isFormalRegistrationCandidate(org.mockito.ArgumentMatchers.anyString(),eq(100L))).thenReturn(true);

        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-download-approve", "pi-download-approve", "pi-download-approve",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png"));

        verify(flowableTaskService).setAssignee("task-download-approve", "100");
        ArgumentCaptor<BpmTaskApproveReqVO> captor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(taskService).approveTask(eq(100L), captor.capture());
        assertEquals("task-download-approve", captor.getValue().getId());
    }

    @Test
    void reviewDoesNotClaimRegistrationUploadTaskWhenLoginUserIsNotSelectedCandidate() {
        Task task = nativeTask();
        when(task.getAssignee()).thenReturn("200");
        when(task.getProcessInstanceId()).thenReturn("pi-upload-approve");
        lenient().when(task.getId()).thenReturn("task-upload-approve");
        when(taskService.getTask("task-upload-approve")).thenReturn(task);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                "registrationCertificateAccessRequestId", 150L,
                "requestId", 150L,
                "certificateId", 990819196L,
                "requestType", "UPLOAD_CERTIFICATE",
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                Map.of("REG_CERT_ACCESS_APPROVAL", List.of(200L))));
        when(processInstanceService.getProcessInstance("pi-upload-approve")).thenReturn(processInstance);

        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-upload-approve", "pi-upload-approve", "pi-upload-approve",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png"));

        verify(flowableTaskService, never()).setAssignee("task-upload-approve", "100");
        verify(taskService).approveTask(eq(100L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void reviewDoesNotClaimRegistrationUploadTaskWithoutApproverRole() {
        Task task = nativeTask();
        when(task.getAssignee()).thenReturn("200");
        when(task.getProcessInstanceId()).thenReturn("pi-upload-approve");
        lenient().when(task.getId()).thenReturn("task-upload-approve");
        when(taskService.getTask("task-upload-approve")).thenReturn(task);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                "registrationCertificateAccessRequestId", 150L,
                "certificateId", 990819196L,
                "requestType", "UPLOAD_CERTIFICATE",
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                Map.of("REG_CERT_ACCESS_APPROVAL", List.of(100L))));
        when(processInstanceService.getProcessInstance("pi-upload-approve")).thenReturn(processInstance);
        when(centerQuery.isFormalRegistrationCandidate("task-upload-approve",100L)).thenReturn(false);

        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-upload-approve", "pi-upload-approve", "pi-upload-approve",
                ApprovalTaskReviewResult.APPROVE, null, "secret", false)
                .setSignatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png"));

        verify(flowableTaskService, never()).setAssignee("task-upload-approve", "100");
        verify(taskService).approveTask(eq(100L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void reviewRejectsNativeBpmTodoTaskThroughTaskService() {
        provider.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-reject-100", "pi-reject-100", "pi-reject-100",
                ApprovalTaskReviewResult.REJECT, "资料不完整", "secret", false));

        ArgumentCaptor<BpmTaskRejectReqVO> captor = ArgumentCaptor.forClass(BpmTaskRejectReqVO.class);
        verify(taskService).rejectTask(eq(100L), captor.capture());
        assertEquals("task-reject-100", captor.getValue().getId());
        assertEquals("资料不完整", captor.getValue().getReason());
    }

    @Test
    void pageDoneMapsNativeBpmDoneTasksToUnifiedSummary() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("task-done-100");
        when(task.getName()).thenReturn("已办审批");
        when(task.getTaskDefinitionKey()).thenReturn("archiveTask");
        when(task.getProcessInstanceId()).thenReturn("pi-done-100");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 3, "TASK_REASON", "资料不完整"));
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getId()).thenReturn("pi-done-100");
        when(instance.getName()).thenReturn("已办审批");
        when(instance.getProcessVariables()).thenReturn(Map.of());
        when(centerQuery.done(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(processInstanceService.getHistoricProcessInstances(Set.of("pi-done-100")))
                .thenReturn(List.of(instance));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, "已办", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_TASK_DONE:task-done-100", summary.getId());
        assertEquals("BPM_TASK_DONE", summary.getSourceTaskType());
        assertEquals("已办审批", summary.getBusinessTitle());
        assertEquals("DONE", summary.getBusinessStatus());
        assertEquals(ApprovalTaskReviewResult.REJECT, summary.getApprovalResult());
        assertEquals("资料不完整", summary.getApprovalRemark());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("pi-done-100", summary.getDetailQuery().get("id"));
        assertEquals("task-done-100", summary.getDetailQuery().get("taskId"));

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).done(captor.capture(),eq("BPM"));
        assertEquals("已办",captor.getValue().getKeyword());
    }

    @Test
    void pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("task-done-legacy");
        when(task.getName()).thenReturn("历史已办审批");
        when(task.getTaskDefinitionKey()).thenReturn("legacyApprovalTask");
        when(task.getProcessInstanceId()).thenReturn("pi-done-legacy");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of());
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getId()).thenReturn("pi-done-legacy");
        when(instance.getName()).thenReturn("历史已办审批");
        when(instance.getProcessVariables()).thenReturn(Map.of());
        when(centerQuery.done(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(processInstanceService.getHistoricProcessInstances(Set.of("pi-done-legacy")))
                .thenReturn(List.of(instance));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_TASK_DONE:task-done-legacy", summary.getId());
        assertEquals("DONE", summary.getBusinessStatus());
        assertNull(summary.getApprovalResult());
        assertNull(summary.getApprovalRemark());
        assertEquals("pi-done-legacy", summary.getDetailQuery().get("id"));
    }

    private static RoleRespDTO registrationManagerRole() {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(990819191L);
        role.setCode("dcc_registration_certificate_approver");
        role.setName("注册部经理");
        role.setStatus(0);
        return role;
    }

    @Test
    void pageDoneUsesHistoricProcessVariablesForReadableBusinessSummary() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("task-done-regcert");
        when(task.getName()).thenReturn("Registration certificate access approval");
        when(task.getTaskDefinitionKey()).thenReturn("regcertAccessApproval");
        when(task.getProcessInstanceId()).thenReturn("pi-done-regcert");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));

        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getId()).thenReturn("pi-done-regcert");
        when(instance.getName()).thenReturn("Registration certificate access workflow");
        when(instance.getProcessVariables()).thenReturn(Map.of(
                "registrationCertificateAccessRequestId", 8802L,
                "requestType", "DOWNLOAD_FILE",
                "requestKey", "REG-DOWNLOAD-20260831-001",
                "certificateId", 7702L,
                "ownerCompanyId", 6602L));
        when(centerQuery.done(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(processInstanceService.getHistoricProcessInstances(Set.of("pi-done-regcert")))
                .thenReturn(List.of(instance));
        when(roleApi.getRoleByCode("dcc_registration_certificate_approver"))
                .thenReturn(registrationManagerRole());

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals("注册证下载审批 REG-DOWNLOAD-20260831-001", summary.getBusinessTitle());
        assertEquals("REG-DOWNLOAD-20260831-001", summary.getBusinessCode());
        assertEquals(List.of("申请类型：注册证下载", "申请编号：8802", "注册证：7702", "所属公司：6602"),
                summary.getBusinessContextTags());
        assertEquals("注册证访问审批", summary.getCurrentNodeName());
    }

    @Test
    void pageDoneReturnsEmptyWithoutHistoricProcessLookup() {
        when(centerQuery.done(any(),eq("BPM")))
                .thenReturn(PageResult.empty());

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10));

        assertTrue(page.getList().isEmpty());
        assertEquals(0L, page.getTotal());
        verify(processInstanceService, never()).getHistoricProcessInstances(any());
    }

    @Test
    void pageDoneFailsWhenHistoricProcessInstanceIsMissing() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getProcessInstanceId()).thenReturn("pi-done-missing");
        when(centerQuery.done(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(processInstanceService.getHistoricProcessInstances(Set.of("pi-done-missing")))
                .thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.page(ApprovalTaskQueryContext.of(100L,
                        ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10)));

        assertEquals("APPROVAL_PROCESS_INSTANCE_REQUIRED: BPM done pi-done-missing", exception.getMessage());
    }

    @Test
    void pageTodoUsesNullUserFilterWhenGlobalViewEnabled() {
        Task task = nativeTask();
        when(task.getId()).thenReturn("task-global-100");
        when(task.getName()).thenReturn("全量待办审批");
        when(task.getTaskDefinitionKey()).thenReturn("userTask");
        when(task.getProcessInstanceId()).thenReturn("pi-global-100");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("BPM")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, "全量", 1, 10, true));

        assertEquals(1L, page.getTotal());
        verify(centerQuery).todo(org.mockito.ArgumentMatchers.argThat(ApprovalTaskQueryContext::isGlobalView),eq("BPM"));
    }

    @Test
    void pageMyInitiatedMapsProcessInstancesToUnifiedSummary() {
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getId()).thenReturn("pi-100");
        when(instance.getName()).thenReturn("流程申请");
        when(instance.getBusinessKey()).thenReturn("biz-100");
        when(instance.getStartUserId()).thenReturn("100");
        when(instance.getStartTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.initiated(any()))
                .thenReturn(new PageResult<>(List.of(instance), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.MY_INITIATED, ApprovalModuleCode.BPM, "流程", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_PROCESS_INSTANCE:pi-100", summary.getId());
        assertEquals(ApprovalModuleCode.BPM, summary.getModuleCode());
        assertEquals("BPM_PROCESS_INSTANCE", summary.getSourceTaskType());
        assertEquals("流程申请", summary.getBusinessTitle());
        assertEquals("/bpm/process-instance/detail", summary.getDetailRoute());
        assertEquals("pi-100", summary.getDetailQuery().get("id"));

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).initiated(captor.capture());
        assertEquals("流程",captor.getValue().getKeyword());
    }

    @Test
    void pageMyInitiatedUsesProcessVariablesForReadableBusinessSummary() {
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getId()).thenReturn("pi-initiated-route");
        when(instance.getName()).thenReturn("MES route version publish workflow");
        when(instance.getBusinessKey()).thenReturn("route-version-501");
        when(instance.getStartUserId()).thenReturn("100");
        when(instance.getStartTime()).thenReturn(new Date(1782180000000L));
        when(instance.getProcessVariables()).thenReturn(Map.of(
                "businessType", "MES_ROUTE_VERSION_PUBLISH",
                "routeId", 901L,
                "routeCode", "ROUTE-20260831-001",
                "routeName", "球囊扩张压力泵生产路线",
                "routeVersionNo", "V31"));
        when(centerQuery.initiated(any()))
                .thenReturn(new PageResult<>(List.of(instance), 1L));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.MY_INITIATED, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals("工艺路线发布 球囊扩张压力泵生产路线 V31", summary.getBusinessTitle());
        assertEquals("ROUTE-20260831-001", summary.getBusinessCode());
        assertEquals(List.of("路线编号：ROUTE-20260831-001", "路线名称：球囊扩张压力泵生产路线", "版本：V31"),
                summary.getBusinessContextTags());
    }

    @Test
    void pageCcMapsCopiedProcessInstancesToUnifiedSummary() {
        BpmProcessInstanceCopyDO copy = new BpmProcessInstanceCopyDO();
        copy.setId(10L);
        copy.setStartUserId(501L);
        copy.setProcessInstanceId("pi-copy");
        copy.setProcessInstanceName("抄送流程");
        copy.setActivityId("activity-a");
        copy.setActivityName("知会");
        copy.setTaskId("task-copy");
        copy.setReason("请关注");
        copy.setCreateTime(LocalDateTime.parse("2026-06-23T12:30:00"));
        when(centerQuery.copied(any()))
                .thenReturn(new PageResult<>(List.of(copy), 1L));

        PageResult<ApprovalTaskSummary> page = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.CC, ApprovalModuleCode.BPM, "抄送", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("BPM:BPM_PROCESS_INSTANCE_COPY:10", summary.getId());
        assertEquals("BPM_PROCESS_INSTANCE_COPY", summary.getSourceTaskType());
        assertEquals("抄送流程", summary.getBusinessTitle());
        assertEquals("知会", summary.getCurrentNodeName());
        assertEquals("pi-copy", summary.getDetailQuery().get("id"));
        assertEquals("task-copy", summary.getDetailQuery().get("taskId"));
        assertEquals("activity-a", summary.getDetailQuery().get("activityId"));

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).copied(captor.capture());
        assertEquals("抄送",captor.getValue().getKeyword());
    }
}
