package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccWorkflowSignoffAssignmentTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMapper fileMapper;
    @Mock private DccControlledFileMasterMapper masterMapper;
    @Mock private DccControlledFileTaskAssigneeSnapshotMapper snapshotMapper;
    @Mock private BpmTaskService bpmTaskService;
    @Mock private TaskService taskService;
    @Mock private org.flowable.engine.RuntimeService runtimeService;
    @Mock private cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions;
    @Mock private DccControlledFileSignatureBindingService signatureBindingService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private DccControlledFileRouteReadinessService readinessService;
    @Mock private DccSignatureVerificationService signatureService;
    @Mock private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService remediationService;
    @InjectMocks private DccWorkflowSignoffAssignmentService service;
    private Task task;
    private DccControlledFileTaskAssigneeSnapshotDO row;
    private DccControlledFileDO file;
    private org.flowable.engine.repository.ProcessDefinition definition;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        file=DccControlledFileDO.builder().id(10L).masterId(20L).tenantId(1L)
                .processInstanceId("round-1").status("PENDING_MATRIX_REVIEW").build();
        when(fileMapper.selectById(10L)).thenReturn(file);
        when(fileMapper.selectByIdAndTenantForUpdate(1L,10L)).thenReturn(file);
        when(masterMapper.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        task=mock(Task.class);
        when(task.getId()).thenReturn("task-51"); when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("round-1");
        when(task.getTenantId()).thenReturn("1");
        definition=mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(definition.getId()).thenReturn("dcc-controlled-file-revision:4:real-definition");
        when(definition.getTenantId()).thenReturn("1");when(definition.getKey()).thenReturn(DccControlledFileProcessDefinitionKeys.REVISION);
        when(task.getProcessDefinitionId()).thenReturn("dcc-controlled-file-revision:4:real-definition");
        when(definitions.getProcessDefinition(anyString())).thenReturn(definition);
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,"10:MATRIX_REVIEW:51"));
        when(bpmTaskService.getTask("task-51")).thenReturn(task);
        when(bpmTaskService.validateTask(99L,"task-51")).thenReturn(task);
        row=DccControlledFileTaskAssigneeSnapshotDO.builder().id(1L).controlledFileId(10L)
                .stageCode("MATRIX_REVIEW").departmentId(51L).leaderUserId(99L).assigneeUserId(99L).build();
        when(snapshotMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(ignored -> row);
        when(snapshotMapper.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
        when(adminUserApi.getUser(anyLong())).thenAnswer(call -> new AdminUserRespDTO().setId(call.getArgument(0))
                .setStatus(0).setDeptId(51L).setNickname("会签人").setPostIds(java.util.Set.of(10L)));
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any()))
                .thenReturn(DccUnifiedSignatureResult.builder().signatureId(7L).evidenceStatus("VALID").build());
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void selfAssignmentHasIndependentSignatureAndDoesNotCompleteSignoff() {
        service.assign(99L,10L,request(99L));
        verify(signatureService).verifyPasswordAndCreateWorkflowSignature(eq(99L),eq(10L),eq("task-51"),eq("round-1"),eq("MATRIX_REVIEW"),eq("ASSIGN"),
                eq("password"),argThat(reason -> reason.contains("部门=51，会签人=99，指派载荷=")));
        verify(taskService).setAssignee("task-51","99");
        verify(bpmTaskService,never()).approveTask(any(),any());
        assertThrows(RuntimeException.class,()->service.requireAssigned(99L,10L,task));
    }
    @Test void otherAssigneeIsPersistedPerDepartmentAndMustSignSeparately() {
        service.assign(99L,10L,request(100L));
        var capture=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(capture.capture());
        assertEquals(100L,capture.getValue().getAssigneeUserId());
        assertEquals(7L,capture.getValue().getAssignmentSignatureId());
        verify(taskService).setAssignee("task-51","100");
    }
    @Test void frozenLeaderOfAnotherDepartmentCanStillAssignThemself() {
        row.setDepartmentId(52L);
        service.assign(99L,10L,request(99L));
        verify(taskService).setAssignee("task-51","99");
        verify(bpmTaskService,never()).approveTask(any(),any());
    }
    @Test void repeatedAssignmentReturnsSavedFactAndDifferentPayloadIsRejected() {
        service.assign(99L,10L,request(99L));
        var saved=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(saved.capture());
        row.setAssignmentSignatureId(7L); row.setAssignedTime(LocalDateTime.now()); row.setBpmTaskId("task-51");
        row.setAssignmentPayloadHash(saved.getValue().getAssignmentPayloadHash());
        when(bpmTaskService.getTask("task-51")).thenReturn(null);
        clearInvocations(signatureService,taskService,snapshotMapper);
        service.assign(99L,10L,request(99L));
        verifyNoInteractions(signatureService,taskService);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        var changed=request(99L);changed.setReason("不同指派意见");
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,changed));
    }
    @Test void wrongLeaderDepartmentTenantOrRoundIsRejectedWithoutSignature() {
        assertThrows(RuntimeException.class,()->service.assign(100L,10L,request(99L)));
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setStatus(0).setDeptId(52L));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        row.setProcessInstanceId("old-round");
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        TenantContextHolder.setTenantId(122L);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        verifyNoInteractions(signatureService,taskService);
    }
    @Test void signatureFailureAndLostWriteNeverChangeTaskAssignee() {
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any()))
                .thenThrow(new IllegalStateException("signature failed"));
        assertThrows(IllegalStateException.class,()->service.assign(99L,10L,request(99L)));
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        verifyNoInteractions(taskService);
    }
    @Test void taskFromAnotherTenantCannotAuthorizeThisTenantsFileAssignment() {
        when(task.getTenantId()).thenReturn("122");
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        verifyNoInteractions(signatureService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }
    @Test void selectedAssigneeWithoutConfiguredPostIsRejectedBeforeTheLeadersSignature() {
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setStatus(0)
                .setDeptId(51L).setPostIds(java.util.Set.of()));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        verifyNoInteractions(signatureService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }
    @Test void directoryResponseForAnotherAccountCannotReplaceTheSelectedAssignee() {
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(101L).setStatus(0)
                .setDeptId(51L).setPostIds(java.util.Set.of(10L)));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        verifyNoInteractions(signatureService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }
    @Test void unsignedOtherDepartmentObligationCannotReuseFirstDepartmentAssignment() {
        row.setAssignmentSignatureId(7L); row.setAssignedTime(LocalDateTime.now()); row.setBpmTaskId("task-52");
        assertThrows(RuntimeException.class,()->service.requireAssigned(99L,10L,task));
    }
    @Test void signedAssignmentPersistsExactlySelectedRemediationBeforeTheTaskIsReassigned() {
        var request=request(100L);
        var arrangement=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement(
                30L,200L,LocalDateTime.of(2026,10,20,12,0));
        request.setRelationArrangements(java.util.List.of(arrangement));
        service.assign(99L,10L,request);
        var order=inOrder(signatureService,snapshotMapper,remediationService,taskService);
        order.verify(signatureService).verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),eq("round-1"),any(),eq("ASSIGN"),any(),any());
        order.verify(snapshotMapper).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        order.verify(remediationService).saveArrangements(99L,10L,"round-1",java.util.List.of(arrangement),"指派本人");
        order.verify(taskService).setAssignee("task-51","100");
    }
    @Test void changingSelectedRemediationInAnAssignmentReplayIsAConflictingSignedPayload() {
        var first=request(99L);
        first.setRelationArrangements(java.util.List.of(new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement(
                30L,200L,LocalDateTime.of(2026,10,20,12,0))));
        service.assign(99L,10L,first);
        var capture=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(capture.capture());
        row.setAssignmentSignatureId(7L);row.setAssignmentPayloadHash(capture.getValue().getAssignmentPayloadHash());
        row.setBpmTaskId("task-51");
        var changed=request(99L);changed.setRelationArrangements(java.util.List.of(
                new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement(
                        30L,201L,LocalDateTime.of(2026,10,20,12,0))));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,changed));
    }
    @Test void arrangementFailureDoesNotChangeTheActualTaskAssignee() {
        doThrow(new IllegalStateException("arrangement unavailable")).when(remediationService).saveArrangements(any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->service.assign(99L,10L,request(99L)));
        verifyNoInteractions(taskService);
    }
    @Test void publicAssignmentReadUsesExactObligationAndAllowsItsCrossDepartmentLeader() {
        row.setDepartmentId(52L);row.setDepartmentName("正式部门52");row.setObligationId("10:MATRIX_REVIEW:52");
        when(adminUserApi.getUserListByDeptIds(java.util.List.of(52L))).thenReturn(java.util.List.of(
            new AdminUserRespDTO().setId(102L).setStatus(0).setDeptId(52L).setNickname("部门成员").setPostIds(java.util.Set.of(10L)),
            new AdminUserRespDTO().setId(103L).setStatus(1).setDeptId(52L).setNickname("停用")));
        Object context=org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"assignmentContext",99L,10L,"task-51");
        assertEquals(Long.valueOf(52L),(Object)org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"departmentId"));
        assertEquals("10:MATRIX_REVIEW:52",org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"obligationId"));
        assertEquals(true,org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"canAssign"));
        assertEquals(2,((java.util.List<?>)org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"assigneeOptions")).size());
        verifyNoInteractions(signatureService,taskService,remediationService);
    }
    @Test void publicAssignmentReadRejectsUnrelatedActorOrMissingRealObligation() {
        assertThrows(RuntimeException.class,()->org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"assignmentContext",100L,10L,"task-51"));
        when(task.getTaskLocalVariables()).thenReturn(Map.of());
        assertThrows(RuntimeException.class,()->org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"assignmentContext",99L,10L,"task-51"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"dcc-controlled-file-upload","dcc-controlled-file-obsolete"})
    void nonRevisionAssignmentSignsAndReassignsWithoutCallingRevisionRemediation(String key) {
        currentDefinition(key);
        service.assign(99L,10L,request(99L));
        verify(signatureService).verifyPasswordAndCreateWorkflowSignature(eq(99L),eq(10L),eq("task-51"),eq("round-1"),
                eq("MATRIX_REVIEW"),eq("ASSIGN"),eq("password"),anyString());
        verify(taskService).setAssignee("task-51","99");verifyNoInteractions(remediationService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"dcc-controlled-file-upload","dcc-controlled-file-obsolete"})
    void nonRevisionCallerCannotSendRemediationFactsBeforeTheActualSignature(String key) {
        currentDefinition(key);
        var request=request(99L);request.setRelationArrangements(java.util.List.of(
                new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement(
                        30L,200L,LocalDateTime.of(2026,10,20,12,0))));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request));
        verifyNoInteractions(signatureService,remediationService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }

    private void currentDefinition(String key) {
        String id=key+":4:real-definition";
        when(task.getProcessDefinitionId()).thenReturn(id);when(definition.getId()).thenReturn(id);when(definition.getKey()).thenReturn(key);
        if(DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(key)) {
            file.setProcessInstanceId("original-upload-round");file.setStatus("ACTIVE");file.setPublishedFileId(501L);
            when(runtimeService.getVariables("round-1")).thenReturn(Map.of("systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE","objectId","10"));
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"dcc-controlled-file-upload","dcc-controlled-file-revision","dcc-controlled-file-obsolete"})
    void contextUsesTheExactCurrentTaskDefinitionEvenWhenTheFileRetainsAnOlderUploadRound(String key) {
        currentDefinition(key);row.setObligationId("10:MATRIX_REVIEW:51");row.setDepartmentName("正式部门");
        when(adminUserApi.getUserListByDeptIds(java.util.List.of(51L))).thenReturn(java.util.List.of());
        var context=service.assignmentContext(99L,10L,"task-51");
        assertEquals(key,context.processDefinitionKey());assertEquals("round-1",context.processInstanceId());
        assertEquals("task-51",context.taskId());assertEquals("10:MATRIX_REVIEW:51",context.obligationId());
        verifyNoInteractions(signatureService,remediationService,taskService);
    }

    @Test void foreignMissingOrUnexpectedDefinitionCannotAuthorizeTheTaskBeforeItsSignature() {
        when(definition.getTenantId()).thenReturn("122");assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        when(definition.getTenantId()).thenReturn("1");when(definition.getKey()).thenReturn("unrelated-key");
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        when(definitions.getProcessDefinition(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        verifyNoInteractions(signatureService,remediationService,taskService);
    }

    private DccSignoffAssignmentReqVO request(long assignee) {
        var req=new DccSignoffAssignmentReqVO(); req.setTaskId("task-51");req.setAssigneeUserId(assignee);
        req.setPassword("password");req.setReason("指派本人");return req;
    }
}
