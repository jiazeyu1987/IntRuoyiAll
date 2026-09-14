package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessApprovedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessCancelledReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessRejectedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCompletedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCreatedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormEffectExecutionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySwitchApprovalModeReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormEffectExecutionDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionSnapshotDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTaskPermissionDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionSnapshotMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormEffectExecutionMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTaskPermissionMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormEffectStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTaskPermissionCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormCenterRuntimeBpmCallbackTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private FormActionPolicyMapper actionPolicyMapper;
    @Mock
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock
    private FormActionInstanceMapper actionInstanceMapper;
    @Mock
    private FormActionSnapshotMapper actionSnapshotMapper;
    @Mock
    private FormTaskPermissionMapper taskPermissionMapper;
    @Mock
    private FormEffectExecutionMapper effectExecutionMapper;
    @Mock
    private FormTemplateRecognizer templateRecognizer;
    @Mock
    private BpmProcessInstanceApi processInstanceApi;
    @Mock
    private TaskService flowableTaskService;
    @Mock
    private TaskQuery taskQuery;
    @Mock
    private Task activeTask;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void findActiveBusinessActionReturnsActiveInstanceByObjectIdentityAcrossActionCode() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO active = instance();
        active.setActionCode("OBSOLETE");
        when(actionInstanceMapper.selectActiveByBusinessObject(122L, "DCC", "CONTROLLED_FILE", "FILE-1001"))
                .thenReturn(active);
        BusinessActionContextReqVO reqVO = new BusinessActionContextReqVO();
        reqVO.setSystemCode("DCC");
        reqVO.setObjectType("CONTROLLED_FILE");
        reqVO.setObjectId("FILE-1001");
        reqVO.setActionCode("PUBLISH");

        FormInstanceRespVO result = runtimeService.findActiveBusinessAction(reqVO);

        assertEquals(10L, result.getId());
        assertEquals("IN_APPROVAL", result.getStatus());
        assertEquals("PI-1001", result.getBpmProcessInstanceId());
        assertEquals("UPLOAD", result.getContext().getActionCode());
    }

    @Test
    void taskCreatedPersistsActivePermissionsForEachHandler() {
        TenantContextHolder.setTenantId(122L);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());

        FormBpmTaskCreatedReqVO reqVO = new FormBpmTaskCreatedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        reqVO.setTaskId("TASK-1");
        reqVO.setHandlerUserIds(List.of(101L, 102L));

        runtimeService.onBpmTaskCreated(reqVO);

        ArgumentCaptor<FormTaskPermissionDO> captor = ArgumentCaptor.forClass(FormTaskPermissionDO.class);
        verify(taskPermissionMapper).selectByTaskIdAndUserId(122L, 10L, "TASK-1", 101L);
        verify(taskPermissionMapper).selectByTaskIdAndUserId(122L, 10L, "TASK-1", 102L);
        verify(taskPermissionMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of(101L, 102L), captor.getAllValues().stream().map(FormTaskPermissionDO::getUserId).toList());
        assertEquals(EnumSet.allOf(FormTaskPermissionCode.class).size(),
                JsonUtils.parseArray(captor.getAllValues().get(0).getPermissionCodesJson(), FormTaskPermissionCode.class).size());
        assertEquals(FormTaskPermissionDO.STATUS_ACTIVE, captor.getAllValues().get(0).getStatus());
    }

    @Test
    void taskCompletedRevokesOnlyCurrentTaskPermissionsAndDoesNotApplyEffect() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FormTaskPermissionDO permission = FormTaskPermissionDO.builder()
                .tenantId(122L)
                .instanceId(10L)
                .bpmProcessInstanceId("PI-1001")
                .taskId("TASK-1")
                .userId(101L)
                .permissionCodesJson("[]")
                .status(FormTaskPermissionDO.STATUS_ACTIVE)
                .build();
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(taskPermissionMapper.selectActiveByTaskId(122L, 10L, "TASK-1")).thenReturn(List.of(permission));

        FormBpmTaskCompletedReqVO reqVO = new FormBpmTaskCompletedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        reqVO.setTaskId("TASK-1");

        runtimeService.onBpmTaskCompleted(reqVO);

        assertEquals(FormTaskPermissionDO.STATUS_REVOKED, permission.getStatus());
        verify(taskPermissionMapper).updateById(permission);
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
        verifyNoInteractions(effectExecutionMapper);
    }

    @Test
    void taskCompletedWithoutActiveTaskPermissionFailsFast() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(taskPermissionMapper.selectActiveByTaskId(122L, 10L, "TASK-OLD")).thenReturn(List.of());

        FormBpmTaskCompletedReqVO reqVO = new FormBpmTaskCompletedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        reqVO.setTaskId("TASK-OLD");

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.onBpmTaskCompleted(reqVO));

        assertEquals(FormCenterErrorCode.BPM_TASK_PERMISSION_MISSING, exception.getErrorCode());
        verify(taskPermissionMapper, never()).updateById(any(FormTaskPermissionDO.class));
    }

    @Test
    void processApprovedAppliesBusinessEffectAfterRevokingActivePermissions() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FormTaskPermissionDO permission = FormTaskPermissionDO.builder()
                .tenantId(122L)
                .instanceId(10L)
                .bpmProcessInstanceId("PI-1001")
                .taskId("TASK-1")
                .userId(101L)
                .permissionCodesJson("[]")
                .status(FormTaskPermissionDO.STATUS_ACTIVE)
                .build();
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors",
                List.of(new FixedEffectExecutor("DCC_UPLOAD", FormBusinessEffectResult.success("FILE-1001"))));
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(taskPermissionMapper.selectActiveByProcessInstanceId(122L, "PI-1001")).thenReturn(List.of(permission));
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormBpmProcessApprovedReqVO reqVO = new FormBpmProcessApprovedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        FormEffectExecutionRespVO response = runtimeService.onBpmProcessApproved(reqVO);

        assertEquals(FormTaskPermissionDO.STATUS_REVOKED, permission.getStatus());
        assertEquals(FormEffectStatus.APPLIED.name(), response.getStatus());
        assertEquals("FILE-1001", response.getResultRef());
        assertEquals(FormInstanceStatus.EFFECTIVE.name(), instance.getStatus());
        ArgumentCaptor<FormEffectExecutionDO> executionCaptor = ArgumentCaptor.forClass(FormEffectExecutionDO.class);
        verify(effectExecutionMapper).insert(executionCaptor.capture());
        assertEquals(FormEffectStatus.APPLIED.name(), executionCaptor.getValue().getStatus());
        assertEquals("FILE-1001", executionCaptor.getValue().getResultRef());
        assertEquals(FormControlledActionApprovalOutcome.EFFECTIVE, lifecycleAdapter.pendingClosedOutcome);
    }

    @Test
    void processApprovedPersistsPendingRecordWhenEffectExecutorFails() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors",
                List.of(new FixedEffectExecutor("DCC_UPLOAD", FormBusinessEffectResult.failure("downstream locked"))));
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormBpmProcessApprovedReqVO reqVO = new FormBpmProcessApprovedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        FormEffectExecutionRespVO response = runtimeService.onBpmProcessApproved(reqVO);

        assertEquals(FormEffectStatus.FAILED_PENDING.name(), response.getStatus());
        assertEquals("downstream locked", response.getFailureReason());
        assertEquals(FormInstanceStatus.EFFECT_FAILED_PENDING.name(), instance.getStatus());
        ArgumentCaptor<FormEffectExecutionDO> executionCaptor = ArgumentCaptor.forClass(FormEffectExecutionDO.class);
        verify(effectExecutionMapper).insert(executionCaptor.capture());
        assertEquals(FormEffectStatus.FAILED_PENDING.name(), executionCaptor.getValue().getStatus());
        assertEquals("downstream locked", executionCaptor.getValue().getFailureReason());
        assertEquals(FormControlledActionApprovalOutcome.EFFECT_FAILED_PENDING, lifecycleAdapter.pendingClosedOutcome);
    }

    @Test
    void submitInstancePersistsFirstActiveTaskPermissionsAfterBpmStartsSynchronously() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters",
                List.of(lifecycleAdapter));
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("PI-1001");
        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("PI-1001")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(activeTask));
        when(activeTask.getId()).thenReturn("TASK-1");
        when(activeTask.getAssignee()).thenReturn("101");
        when(actionSnapshotMapper.selectCountByInstanceId(122L, 10L)).thenReturn(0L);

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "upload"));

        runtimeService.submitInstance(10L, reqVO, 100L);

        ArgumentCaptor<FormTaskPermissionDO> captor = ArgumentCaptor.forClass(FormTaskPermissionDO.class);
        verify(taskPermissionMapper).selectByTaskIdAndUserId(122L, 10L, "TASK-1", 101L);
        verify(taskPermissionMapper).insert(captor.capture());
        assertEquals(FormTaskPermissionDO.STATUS_ACTIVE, captor.getValue().getStatus());
        assertEquals("PI-1001", captor.getValue().getBpmProcessInstanceId());
        assertEquals("TASK-1", captor.getValue().getTaskId());
        assertEquals(101L, captor.getValue().getUserId());
        assertEquals("PI-1001", lifecycleAdapter.pendingStartedInstance.getBpmBinding().getProcessInstanceId());
        assertEquals(FormInstanceStatus.IN_APPROVAL, lifecycleAdapter.pendingStartedInstance.getStatus());
        assertEquals(Map.of("reason", "upload"), lifecycleAdapter.pendingStartedInstance.getFormData());
    }

    @Test
    void submitInstancePendingLockFailureDoesNotReturnSuccess() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters",
                List.of(new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass(), true)));
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("PI-1001");
        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("PI-1001")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(activeTask));
        when(activeTask.getId()).thenReturn("TASK-1");
        when(activeTask.getAssignee()).thenReturn("101");
        when(actionSnapshotMapper.selectCountByInstanceId(122L, 10L)).thenReturn(0L);

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "upload"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> runtimeService.submitInstance(10L, reqVO, 100L));

        assertEquals("pending lock failed", exception.getMessage());
        verify(processInstanceApi).cancelProcessInstance(100L, "PI-1001",
                "form action submit compensation: instanceId=10");
    }

    @Test
    void submitInstanceActiveTaskPermissionFailureCancelsBpmAndDoesNotStartPendingLock() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        when(processInstanceApi.createProcessInstance(any(), any())).thenReturn("PI-NO-TASK");
        when(flowableTaskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("PI-NO-TASK")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of());

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "upload"));

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.submitInstance(10L, reqVO, 100L));

        assertEquals(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING, exception.getErrorCode());
        verify(processInstanceApi).cancelProcessInstance(100L, "PI-NO-TASK",
                "form action submit compensation: instanceId=10");
        assertNull(lifecycleAdapter.pendingStartedInstance);
    }

    @Test
    void processRejectedStaleProcessBindingFailsFast() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setBpmProcessInstanceId("PI-NEW");
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-OLD")).thenReturn(instance);

        FormBpmProcessRejectedReqVO reqVO = new FormBpmProcessRejectedReqVO();
        reqVO.setProcessInstanceId("PI-OLD");

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.onBpmProcessRejected(reqVO));

        assertEquals(FormCenterErrorCode.BPM_CALLBACK_STALE, exception.getErrorCode());
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
        verifyNoInteractions(taskPermissionMapper);
    }

    @Test
    void processRejectedReleasesPendingLockAfterRevokingActivePermissions() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FormTaskPermissionDO permission = FormTaskPermissionDO.builder()
                .tenantId(122L)
                .instanceId(10L)
                .bpmProcessInstanceId("PI-1001")
                .taskId("TASK-1")
                .userId(101L)
                .permissionCodesJson("[]")
                .status(FormTaskPermissionDO.STATUS_ACTIVE)
                .build();
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(taskPermissionMapper.selectActiveByProcessInstanceId(122L, "PI-1001")).thenReturn(List.of(permission));
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormBpmProcessRejectedReqVO reqVO = new FormBpmProcessRejectedReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        runtimeService.onBpmProcessRejected(reqVO);

        assertEquals(FormTaskPermissionDO.STATUS_REVOKED, permission.getStatus());
        assertEquals(FormInstanceStatus.REJECTED.name(), instance.getStatus());
        assertEquals(FormControlledActionApprovalOutcome.REJECTED, lifecycleAdapter.pendingClosedOutcome);
        assertEquals("PI-1001", lifecycleAdapter.pendingClosedInstance.getBpmBinding().getProcessInstanceId());
        verify(taskPermissionMapper).updateById(permission);
        verify(actionInstanceMapper).updateById(instance);
    }

    @Test
    void processCancelledReleasesPendingLockAsCancelled() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FormTaskPermissionDO permission = FormTaskPermissionDO.builder()
                .tenantId(122L)
                .instanceId(10L)
                .bpmProcessInstanceId("PI-1001")
                .taskId("TASK-1")
                .userId(101L)
                .permissionCodesJson("[]")
                .status(FormTaskPermissionDO.STATUS_ACTIVE)
                .build();
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(taskPermissionMapper.selectActiveByProcessInstanceId(122L, "PI-1001")).thenReturn(List.of(permission));
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormBpmProcessCancelledReqVO reqVO = new FormBpmProcessCancelledReqVO();
        reqVO.setProcessInstanceId("PI-1001");
        reqVO.setReason("applicant withdraw");
        runtimeService.onBpmProcessCancelled(reqVO);

        assertEquals(FormTaskPermissionDO.STATUS_REVOKED, permission.getStatus());
        assertEquals(FormInstanceStatus.ABANDONED.name(), instance.getStatus());
        assertEquals(FormControlledActionApprovalOutcome.CANCELLED, lifecycleAdapter.pendingClosedOutcome);
        assertEquals("applicant withdraw", lifecycleAdapter.pendingClosedReason);
        assertEquals("PI-1001", lifecycleAdapter.pendingClosedInstance.getBpmBinding().getProcessInstanceId());
        verify(taskPermissionMapper).updateById(permission);
        verify(actionInstanceMapper).updateById(instance);
    }

    @Test
    void processApprovedWithoutLifecycleAdapterFailsBeforeEffectExecution() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        FixedEffectExecutor executor = new FixedEffectExecutor("DCC_UPLOAD",
                FormBusinessEffectResult.success("FILE-1001"));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors", List.of(executor));
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormBpmProcessApprovedReqVO reqVO = new FormBpmProcessApprovedReqVO();
        reqVO.setProcessInstanceId("PI-1001");

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.onBpmProcessApproved(reqVO));

        assertEquals(FormCenterErrorCode.CONTROLLED_ACTION_ADAPTER_MISSING, exception.getErrorCode());
        assertEquals(0, executor.executionCount);
        verify(effectExecutionMapper, never()).insert(any(FormEffectExecutionDO.class));
        verify(effectExecutionMapper, never()).updateById(any(FormEffectExecutionDO.class));
    }

    @Test
    void retryEffectSuccessClosesPendingLockAsEffective() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.EFFECT_FAILED_PENDING.name());
        FormEffectExecutionDO execution = FormEffectExecutionDO.builder()
                .id(500L)
                .tenantId(122L)
                .instanceId(10L)
                .executionCode("EFFECT-IDEM-1001")
                .idempotencyKey("IDEM-1001")
                .status(FormEffectStatus.FAILED_PENDING.name())
                .failureReason("downstream locked")
                .build();
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors",
                List.of(new FixedEffectExecutor("DCC_UPLOAD", FormBusinessEffectResult.success("FILE-1001"))));
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());
        when(effectExecutionMapper.selectByInstanceIdAndIdempotencyKey(122L, 10L, "IDEM-1001"))
                .thenReturn(execution);

        FormEffectExecutionRespVO response = runtimeService.retryEffect(10L);

        assertEquals(FormEffectStatus.APPLIED.name(), response.getStatus());
        assertEquals(FormEffectStatus.APPLIED.name(), execution.getStatus());
        assertEquals(FormInstanceStatus.EFFECTIVE.name(), instance.getStatus());
        assertEquals(FormControlledActionApprovalOutcome.EFFECTIVE, lifecycleAdapter.pendingClosedOutcome);
        verify(effectExecutionMapper).updateById(execution);
        verify(actionInstanceMapper, org.mockito.Mockito.times(2)).updateById(instance);
    }

    @Test
    void retryEffectWithoutLifecycleAdapterFailsBeforeEffectExecution() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.EFFECT_FAILED_PENDING.name());
        FixedEffectExecutor executor = new FixedEffectExecutor("DCC_UPLOAD",
                FormBusinessEffectResult.success("FILE-1001"));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors", List.of(executor));
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.retryEffect(10L));

        assertEquals(FormCenterErrorCode.CONTROLLED_ACTION_ADAPTER_MISSING, exception.getErrorCode());
        assertEquals(0, executor.executionCount);
        verify(effectExecutionMapper, never()).insert(any(FormEffectExecutionDO.class));
        verify(effectExecutionMapper, never()).updateById(any(FormEffectExecutionDO.class));
    }

    @Test
    void submitInstanceDirectApprovalModeAppliesEffectWithoutStartingBpm() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        instance.setBpmProcessInstanceId(null);
        BusinessApprovalPolicyDO policy = businessPolicy();
        policy.setProcessDefinitionKey(null);
        policy.setPolicyMode(BusinessApprovalPolicyMode.DIRECT.name());
        FixedLifecycleAdapter lifecycleAdapter = new FixedLifecycleAdapter(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters", List.of(lifecycleAdapter));
        ReflectionTestUtils.setField(runtimeService, "effectExecutors",
                List.of(new FixedEffectExecutor("DCC_UPLOAD", FormBusinessEffectResult.success("FILE-1001"))));
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(policy);
        when(effectExecutionMapper.selectByInstanceIdAndIdempotencyKey(122L, 10L, "IDEM-1001"))
                .thenReturn(null);

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "direct obsolete"));

        FormInstanceRespVO response = runtimeService.submitInstance(10L, reqVO, 100L);

        assertEquals(FormInstanceStatus.EFFECTIVE.name(), response.getStatus());
        assertNull(response.getBpmProcessInstanceId());
        assertNull(instance.getBpmProcessInstanceId());
        assertNull(lifecycleAdapter.pendingStartedInstance);
        assertEquals(FormControlledActionApprovalOutcome.EFFECTIVE, lifecycleAdapter.pendingClosedOutcome);
        assertEquals(FormInstanceStatus.EFFECTIVE, lifecycleAdapter.pendingClosedInstance.getStatus());
        verifyNoInteractions(processInstanceApi);
        verify(actionSnapshotMapper).insert(any(FormActionSnapshotDO.class));
        ArgumentCaptor<FormEffectExecutionDO> executionCaptor = ArgumentCaptor.forClass(FormEffectExecutionDO.class);
        verify(effectExecutionMapper).insert(executionCaptor.capture());
        assertEquals(FormEffectStatus.APPLIED.name(), executionCaptor.getValue().getStatus());
        assertEquals("FILE-1001", executionCaptor.getValue().getResultRef());
    }

    @Test
    void switchPolicyApprovalModePublishesDirectPolicyAndDisablesSourcePolicy() {
        TenantContextHolder.setTenantId(122L);
        FormActionPolicyDO sourcePolicy = policy();
        sourcePolicy.setApprovalMode("BPM_REQUIRED");
        when(actionPolicyMapper.selectById(20L)).thenReturn(sourcePolicy);

        FormPolicySwitchApprovalModeReqVO reqVO = new FormPolicySwitchApprovalModeReqVO();
        reqVO.setApprovalMode("DIRECT");

        FormPolicyRespVO response = runtimeService.switchPolicyApprovalMode(20L, reqVO);

        assertEquals("DISABLED", sourcePolicy.getStatus());
        verify(actionPolicyMapper).updateById(sourcePolicy);
        ArgumentCaptor<FormActionPolicyDO> policyCaptor = ArgumentCaptor.forClass(FormActionPolicyDO.class);
        verify(actionPolicyMapper).insert(policyCaptor.capture());
        FormActionPolicyDO inserted = policyCaptor.getValue();
        assertEquals("DIRECT", inserted.getApprovalMode());
        assertEquals(FormActionPolicy.STATUS_PUBLISHED, inserted.getStatus());
        assertEquals("form-change-approval", inserted.getBpmProcessKey());
        assertEquals("DCC_UPLOAD", inserted.getEffectExecutorCode());
        assertEquals("DIRECT", response.getApprovalMode());
    }

    @Test
    void switchPolicyApprovalModeAllowsNonePolicyWithoutTemplateSlots() {
        TenantContextHolder.setTenantId(122L);
        FormActionPolicyDO sourcePolicy = policy();
        sourcePolicy.setPolicyType(FormPolicyType.NONE.name());
        sourcePolicy.setApprovalMode("BPM_REQUIRED");
        sourcePolicy.setSlotsJson("[]");
        sourcePolicy.setBpmProcessKey("mes-edhr-batch-execution-void-v1");
        sourcePolicy.setEffectExecutorCode("EDHR_BATCH_VOID");
        when(actionPolicyMapper.selectById(20L)).thenReturn(sourcePolicy);

        FormPolicySwitchApprovalModeReqVO reqVO = new FormPolicySwitchApprovalModeReqVO();
        reqVO.setApprovalMode("DIRECT");

        FormPolicyRespVO response = runtimeService.switchPolicyApprovalMode(20L, reqVO);

        assertEquals("DISABLED", sourcePolicy.getStatus());
        ArgumentCaptor<FormActionPolicyDO> policyCaptor = ArgumentCaptor.forClass(FormActionPolicyDO.class);
        verify(actionPolicyMapper).insert(policyCaptor.capture());
        FormActionPolicyDO inserted = policyCaptor.getValue();
        assertEquals(FormPolicyType.NONE.name(), inserted.getPolicyType());
        assertEquals("[]", inserted.getSlotsJson());
        assertEquals("DIRECT", inserted.getApprovalMode());
        assertEquals("mes-edhr-batch-execution-void-v1", inserted.getBpmProcessKey());
        assertEquals("EDHR_BATCH_VOID", inserted.getEffectExecutorCode());
        assertEquals("DIRECT", response.getApprovalMode());
    }

    @Test
    void publishPolicyAllowsDirectModeWithoutBpmProcessKey() {
        TenantContextHolder.setTenantId(122L);
        FormActionPolicyDO policy = policy();
        policy.setStatus("DRAFT");
        policy.setApprovalMode("DIRECT");
        policy.setBpmProcessKey(null);
        when(actionPolicyMapper.selectById(20L)).thenReturn(policy);
        mockLatestTemplate();

        runtimeService.publishPolicy(20L);

        assertEquals(FormActionPolicy.STATUS_PUBLISHED, policy.getStatus());
        verify(actionPolicyMapper).updateById(policy);
    }

    @Test
    void submitInstanceWithoutLifecycleAdapterFailsBeforeStartingBpm() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "obsolete"));

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.submitInstance(10L, reqVO, 100L));

        assertEquals(FormCenterErrorCode.CONTROLLED_ACTION_ADAPTER_MISSING, exception.getErrorCode());
        verifyNoInteractions(processInstanceApi);
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
        verify(actionSnapshotMapper, never()).insert(any(FormActionSnapshotDO.class));
    }

    @Test
    void submitInstanceLifecyclePreflightFailureStopsBeforeStartingBpm() {
        TenantContextHolder.setTenantId(122L);
        FormActionInstanceDO instance = instance();
        instance.setStatus(FormInstanceStatus.DRAFT.name());
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(businessPolicy());
        ReflectionTestUtils.setField(runtimeService, "lifecycleAdapters",
                List.of(new FixedLifecycleAdapter(FormBusinessEffectPrecheck.fail("OBJECT_VERSION_MISMATCH"))));

        FormInstanceSubmitReqVO reqVO = new FormInstanceSubmitReqVO();
        reqVO.setFormData(Map.of("reason", "obsolete"));

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.submitInstance(10L, reqVO, 100L));

        assertEquals(FormCenterErrorCode.CONTROLLED_ACTION_PREFLIGHT_FAILED, exception.getErrorCode());
        verifyNoInteractions(processInstanceApi);
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
        verify(actionSnapshotMapper, never()).insert(any(FormActionSnapshotDO.class));
    }

    private FormActionInstanceDO instance() {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setTenantId(122L);
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId("FILE-1001");
        context.setObjectVersion("V1");
        context.setActionCode("UPLOAD");
        context.setObjectState("DRAFT");
        return FormActionInstanceDO.builder()
                .id(10L)
                .tenantId(122L)
                .instanceCode("FCI-1001")
                .policyId(20L)
                .applicantUserId(100L)
                .status(FormInstanceStatus.IN_APPROVAL.name())
                .bpmProcessInstanceId("PI-1001")
                .idempotencyKey("IDEM-1001")
                .businessContextJson(JsonUtils.toJsonString(context))
                .formDataJson("{\"reason\":\"upload\"}")
                .build();
    }

    private BusinessApprovalPolicyDO businessPolicy() {
        return BusinessApprovalPolicyDO.builder()
                .id(20L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED.name())
                .processDefinitionKey("form-change-approval")
                .effectExecutorCode("DCC_UPLOAD")
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .build();
    }
    private FormActionPolicyDO policy() {
        return FormActionPolicyDO.builder()
                .id(20L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyType(FormPolicyType.REQUIRED.name())
                .bpmProcessKey("form-change-approval")
                .effectExecutorCode("DCC_UPLOAD")
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .slotsJson(JsonUtils.toJsonString(List.of(FormPolicySlot.required("change-request",
                        FormTemplateVersionRef.of(1001L, "200", "V1", "Change Form")))))
                .build();
    }

    private void mockLatestTemplate() {
        when(templateVersionMapper.selectLatestPublishedByTemplateId(122L, 200L))
                .thenReturn(FormTemplateVersionDO.builder()
                        .id(1001L)
                        .templateId(200L)
                        .tenantId(122L)
                        .templateName("Change Form")
                        .versionNo("V1")
                        .status(FormTemplateStatus.PUBLISHED.name())
                        .build());
    }

    private static final class FixedEffectExecutor implements FormBusinessEffectExecutor {

        private final String executorCode;
        private final FormBusinessEffectResult result;
        private int executionCount;

        private FixedEffectExecutor(String executorCode, FormBusinessEffectResult result) {
            this.executorCode = executorCode;
            this.result = result;
        }

        @Override
        public String getExecutorCode() {
            return executorCode;
        }

        @Override
        public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
            executionCount++;
            return result;
        }
    }

    private static final class FixedLifecycleAdapter implements FormControlledActionLifecycleAdapter {

        private final FormBusinessEffectPrecheck precheck;
        private final boolean failPendingStart;

        private FixedLifecycleAdapter(FormBusinessEffectPrecheck precheck) {
            this(precheck, false);
        }

        private FixedLifecycleAdapter(FormBusinessEffectPrecheck precheck, boolean failPendingStart) {
            this.precheck = precheck;
            this.failPendingStart = failPendingStart;
        }

        @Override
        public boolean supports(FormActionInstance instance) {
            return "DCC".equals(instance.getBusinessContext().getSystemCode())
                    && "CONTROLLED_FILE".equals(instance.getBusinessContext().getObjectType())
                    && "UPLOAD".equals(instance.getBusinessContext().getActionCode());
        }

        @Override
        public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
            return precheck;
        }

        private FormActionInstance pendingStartedInstance;
        private FormActionInstance pendingClosedInstance;
        private FormControlledActionApprovalOutcome pendingClosedOutcome;
        private String pendingClosedReason;

        @Override
        public void onPendingApprovalStarted(FormActionInstance instance) {
            if (failPendingStart) {
                throw new IllegalStateException("pending lock failed");
            }
            this.pendingStartedInstance = instance;
        }

        @Override
        public void onPendingApprovalClosed(FormActionInstance instance,
                FormControlledActionApprovalOutcome outcome, String reason) {
            this.pendingClosedInstance = instance;
            this.pendingClosedOutcome = outcome;
            this.pendingClosedReason = reason;
        }
    }
}
