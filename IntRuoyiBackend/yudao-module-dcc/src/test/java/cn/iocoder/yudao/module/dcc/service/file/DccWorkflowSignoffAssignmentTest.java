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
    @Mock private DccControlledFileSignatureMapper signatureMapper;
    @Mock private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService relationStageService;
    @Mock private DccApplicationRoundService rounds;
    @Mock private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper formInstances;
    @Mock private AdminUserApi adminUserApi;
    @Mock private DccControlledFileRouteReadinessService readinessService;
    @Mock private DccSignatureVerificationService signatureService;
    @Mock private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService remediationService;
    @Mock private DccBoundWorkflowContractResolver obsoleteBoundContracts;
    @InjectMocks private DccWorkflowSignoffAssignmentService service;
    private Task task;
    private DccControlledFileTaskAssigneeSnapshotDO row;
    private DccControlledFileDO file;
    private org.flowable.engine.repository.ProcessDefinition definition;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        file=DccControlledFileDO.builder().id(10L).masterId(20L).tenantId(1L)
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION).processInstanceId("round-1").status("PENDING_MATRIX_REVIEW").versionNo("A/1").build();
        when(rounds.require(anyString(),eq(10L),anyString())).thenReturn(3);
        when(formInstances.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(java.util.List.of(
                cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO.builder().id(2L).tenantId(1L).systemCode("DCC")
                        .objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectId("10").objectVersion("A/1").status("IN_APPROVAL").build()));
        when(fileMapper.selectById(10L)).thenReturn(file);
        when(fileMapper.selectByIdAndTenantForUpdate(1L,10L)).thenReturn(file);
        when(masterMapper.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        task=mock(Task.class);
        when(task.getId()).thenReturn("task-51"); when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("round-1");
        when(task.getExecutionId()).thenReturn("execution-51");
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
                .stageCode("MATRIX_REVIEW").departmentId(51L).leaderUserId(99L).assigneeUserId(99L)
                .obligationId("10:MATRIX_REVIEW:51").nodeInstanceId("execution-51").processInstanceId("round-1").build();
        when(snapshotMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(ignored -> row);
        when(snapshotMapper.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
        when(adminUserApi.getUser(anyLong())).thenAnswer(call -> new AdminUserRespDTO().setId(call.getArgument(0))
                .setTenantId(1L).setStatus(0).setDeptId(51L).setNickname("会签人").setPostIds(java.util.Set.of(10L)));
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any()))
                .thenReturn(DccUnifiedSignatureResult.builder().signatureId(7L).evidenceStatus("VALID")
                        .evidenceHash("signature-domain-hmac").signedAt(LocalDateTime.of(2026,10,11,12,0)).build());
        when(signatureMapper.selectActionSignature(10L,"task-51",99L,"ASSIGN")).thenReturn(DccControlledFileSignatureDO.builder()
                .id(7L).controlledFileId(10L).taskId("task-51").actorId(99L).processInstanceId("round-1")
                .signedAt(LocalDateTime.of(2026,10,11,12,0)).evidenceStatus("VALID").evidenceHash("signature-domain-hmac").build());
        when(relationStageService.prepare(any(),any(),any(),any(),any(),anyString(),isNull(),anyString())).thenAnswer(call->{
            var identity=call.<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.ValidatedTaskIdentity>getArgument(2);
            java.util.List<Long> supplements=call.getArgument(3);
            var complete=supplements.stream().map(id->new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection(
                    id,id,1L,"related-number","related-name","A/1","SUPPLEMENT",true)).toList();
            String digest=cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                    java.util.Arrays.asList(identity,supplements,call.getArgument(4),call.getArgument(7))));
            return new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Prepared(call.getArgument(0),call.getArgument(1),
                    identity,call.getArgument(5),"a".repeat(64),digest,digest,call.getArgument(7),complete,call.getArgument(4));
        });
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
        assertEquals(LocalDateTime.of(2026,10,11,12,0),capture.getValue().getAssignedTime());
        assertNull(capture.getValue().getDepartmentId());
        assertNull(capture.getValue().getObligationId());
        assertEquals(51L,row.getDepartmentId());
        verify(readinessService).requireReadyParticipants("MATRIX_REVIEW",java.util.List.of(100L));
        verify(taskService).setAssignee("task-51","100");
        verify(bpmTaskService,never()).approveTask(any(),any());
    }
    @Test void frozenLeaderOfAnotherDepartmentCannotAssignThemself() {
        row.setDepartmentId(52L);
        assertAssignmentDenied(99L);
        verifyNoAssignmentWrites();
    }
    @Test void repeatedAssignmentReturnsSavedFactAndDifferentPayloadIsRejected() {
        service.assign(99L,10L,request(99L));
        var saved=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(saved.capture());
        row.setAssignmentSignatureId(7L); row.setAssignedTime(saved.getValue().getAssignedTime()); row.setBpmTaskId("task-51");
        row.setAssignmentPayloadHash(saved.getValue().getAssignmentPayloadHash());
        when(bpmTaskService.getTask("task-51")).thenReturn(null);
        clearInvocations(signatureService,taskService,snapshotMapper);
        service.assign(99L,10L,request(99L));
        verifyNoInteractions(signatureService,taskService);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        var changed=request(99L);changed.setReason("不同指派意见");
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,changed));
    }
    @Test void wrongLeaderTenantOrRoundIsRejectedWithoutSignature() {
        assertThrows(RuntimeException.class,()->service.assign(100L,10L,request(99L)));
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setTenantId(122L)
                .setStatus(0).setDeptId(52L).setPostIds(java.util.Set.of(10L)));
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
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setTenantId(1L).setStatus(0)
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
    @Test void publicAssignmentReadUsesExactObligationAndExcludesItsCrossDepartmentLeader() {
        row.setDepartmentId(52L);row.setDepartmentName("正式部门52");row.setObligationId("10:MATRIX_REVIEW:52");
        when(adminUserApi.getUserListByDeptIds(java.util.List.of(52L))).thenReturn(java.util.List.of(
            new AdminUserRespDTO().setId(102L).setTenantId(1L).setStatus(0).setDeptId(52L).setNickname("部门成员").setPostIds(java.util.Set.of(10L)),
            new AdminUserRespDTO().setId(103L).setTenantId(1L).setStatus(1).setDeptId(52L).setNickname("停用")));
        Object context=org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"assignmentContext",99L,10L,"task-51");
        assertEquals(Long.valueOf(52L),(Object)org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"departmentId"));
        assertEquals("10:MATRIX_REVIEW:52",org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"obligationId"));
        assertEquals(true,org.springframework.test.util.ReflectionTestUtils.invokeMethod(context,"canAssign"));
        assertEquals(java.util.List.of(102L),service.assignmentContext(99L,10L,"task-51").assigneeOptions().stream()
                .map(DccSignoffAssignmentContext.AssigneeOption::id).toList());
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
            when(formInstances.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(java.util.List.of(cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO.builder().id(2L).tenantId(1L).instanceCode("HISTORICAL-V1").systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectId("10").objectVersion("A/1").status("IN_APPROVAL").bpmProcessInstanceId("round-1").formDataJson("{\"dccExecutionPreparation\":{\"formatVersion\":\"DCC_OBSOLETE_FORM_PREPARATION_V1\"}}").build()));
            when(obsoleteBoundContracts.resolveRoundFlowContract(eq(file),any())).thenReturn(new DccBoundWorkflowContractResolver.RoundFlowContract(1L,10L,"OBSOLETE",3,"round-1",key,id,"historical-deployment","historical-full-digest","historical-contract",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_OBSOLETE_APPROVAL_END,null,2L,"HISTORICAL-V1",DccBoundWorkflowContractResolver.RoundContractStatus.READY,null));
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"dcc-controlled-file-upload","dcc-controlled-file-revision","dcc-controlled-file-obsolete"})
    void enabledCrossDepartmentAssigneeIsRejectedBeforeSigningInEveryAssignmentFlow(String key) {
        currentDefinition(key);
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setTenantId(1L)
                .setStatus(0).setDeptId(52L).setNickname("跨部门会签人").setPostIds(java.util.Set.of(10L)));
        assertAssignmentDenied(100L);
        assertEquals(51L,row.getDepartmentId());
        verifyNoAssignmentWrites();
        verify(bpmTaskService,never()).approveTask(any(),any());
    }

    @Test void memberMovedAfterCandidateLoadingIsRejectedUsingTheCurrentDirectory() {
        var candidate=new AdminUserRespDTO().setId(100L).setTenantId(1L).setStatus(0).setDeptId(51L)
                .setNickname("部门会签人").setPostIds(java.util.Set.of(10L));
        when(adminUserApi.getUserListByDeptIds(java.util.List.of(51L))).thenReturn(java.util.List.of(candidate));
        when(adminUserApi.getUser(100L)).thenReturn(candidate);
        assertTrue(service.assignmentContext(99L,10L,"task-51").assigneeOptions().stream().anyMatch(user->user.id().equals(100L)));
        candidate.setDeptId(52L);
        assertAssignmentDenied(100L);
        verify(adminUserApi).getUser(100L);
        verifyNoAssignmentWrites();
    }

    @Test void departmentDirectoryCannotInjectForeignDepartmentMembersAndSelfMustBelongToTheDepartment() {
        when(adminUserApi.getUserListByDeptIds(java.util.List.of(51L))).thenReturn(java.util.List.of(
                new AdminUserRespDTO().setId(100L).setTenantId(1L).setStatus(0).setDeptId(52L).setNickname("其他部门").setPostIds(java.util.Set.of(10L)),
                new AdminUserRespDTO().setId(101L).setTenantId(1L).setStatus(0).setDeptId(51L).setNickname("无岗位").setPostIds(java.util.Set.of()),
                new AdminUserRespDTO().setId(102L).setTenantId(1L).setStatus(1).setDeptId(51L).setNickname("停用").setPostIds(java.util.Set.of(10L))));
        assertEquals(java.util.List.of(99L),service.assignmentContext(99L,10L,"task-51").assigneeOptions().stream()
                .map(DccSignoffAssignmentContext.AssigneeOption::id).toList());
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0)
                .setDeptId(52L).setNickname("跨部门负责人").setPostIds(java.util.Set.of(10L)));
        assertTrue(service.assignmentContext(99L,10L,"task-51").assigneeOptions().isEmpty());
        verifyNoAssignmentWrites();
    }

    @Test void missingDepartmentCannotAuthorizeEvenAnAccountWhoseDepartmentIsAlsoMissing() {
        row.setDepartmentId(null);
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setTenantId(1L).setStatus(0)
                .setNickname("无部门会签人").setPostIds(java.util.Set.of(10L)));
        assertAssignmentDenied(100L);
        verifyNoAssignmentWrites();
    }

    @Test void disabledAndForeignCrossDepartmentUsersAreRejectedBeforeSigning() {
        for(var user:java.util.List.of(
                new AdminUserRespDTO().setId(100L).setTenantId(1L).setStatus(1).setDeptId(52L).setPostIds(java.util.Set.of(10L)),
                new AdminUserRespDTO().setId(100L).setTenantId(122L).setStatus(0).setDeptId(52L).setPostIds(java.util.Set.of(10L)))) {
            when(adminUserApi.getUser(100L)).thenReturn(user);
            assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(100L)));
        }
        verifyNoInteractions(signatureService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }

    @Test void departmentMemberSignatureReadinessFailureCannotCreateAssignmentFact() {
        when(adminUserApi.getUser(100L)).thenReturn(new AdminUserRespDTO().setId(100L).setTenantId(1L)
                .setStatus(0).setDeptId(51L).setNickname("本部门会签人").setPostIds(java.util.Set.of(10L)));
        doThrow(new IllegalStateException("目标会签人未获真实签名授权"))
                .when(readinessService).requireReadyParticipants("MATRIX_REVIEW",java.util.List.of(100L));
        assertThrows(IllegalStateException.class,()->service.assign(99L,10L,request(100L)));
        verifyNoInteractions(signatureService,taskService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }

    @Test void assignmentSignatureWithoutAuthoritativeTimeCannotPersistOrReassign() {
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any()))
                .thenReturn(DccUnifiedSignatureResult.builder().signatureId(7L).evidenceStatus("VALID").build());
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        verifyNoInteractions(taskService,remediationService);
    }

    @Test void signedSupplementFreezesCompletePreparedFactsBeforeAssignmentChangesTheTask() {
        var command=request(99L);command.setSupplementalRelatedControlledFileIds(java.util.List.of(9007199254740993L));
        service.assign(99L,10L,command);
        var order=inOrder(relationStageService,signatureService,snapshotMapper,taskService);
        order.verify(relationStageService).prepare(eq(99L),eq(10L),any(),eq(java.util.List.of(9007199254740993L)),any(),anyString(),isNull(),anyString());
        order.verify(signatureService).verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),
                argThat(reason->reason.contains("dccRelationPayloadHash=")));
        order.verify(relationStageService).freeze(any(),argThat(signature->Long.valueOf(7L).equals(signature.signatureId())
                && LocalDateTime.of(2026,10,11,12,0).equals(signature.signedAt()) && "signature-domain-hmac".equals(signature.evidenceHash())));
        order.verify(snapshotMapper).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        order.verify(taskService).setAssignee("task-51","99");
    }

    @Test void lateStageFreezeFailureCannotPersistAssignmentOrAdvanceBpm() {
        doThrow(new IllegalStateException("正式阶段快照写入失败")).when(relationStageService).freeze(any(),any());
        assertThrows(IllegalStateException.class,()->service.assign(99L,10L,request(99L)));
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        verifyNoInteractions(remediationService,taskService);
    }

    @Test void actualOuterTransactionRollsBackSignatureStageAssignmentAndTaskWhenLateBpmReassignmentFails() {
        var dataSource=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:w05_stage_"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        for(var table:java.util.List.of("signatures","stages","assignments","tasks")) jdbc.execute("CREATE TABLE "+table+"(id BIGINT)");
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any())).thenAnswer(call->{
            jdbc.update("INSERT INTO signatures VALUES(7)");
            return DccUnifiedSignatureResult.builder().signatureId(7L).evidenceStatus("VALID").evidenceHash("signature-domain-hmac")
                    .signedAt(LocalDateTime.of(2026,10,11,12,0)).build();
        });
        when(relationStageService.freeze(any(),any())).thenAnswer(call->{jdbc.update("INSERT INTO stages VALUES(8)");return null;});
        when(snapshotMapper.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenAnswer(call->{jdbc.update("INSERT INTO assignments VALUES(1)");return 1;});
        doAnswer(call->{jdbc.update("INSERT INTO tasks VALUES(1)");throw new IllegalStateException("真实晚阶段BPM转派失败");})
                .when(taskService).setAssignee("task-51","99");
        var manager=new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        var factory=new org.springframework.aop.framework.ProxyFactory(service);factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var transactional=(DccWorkflowSignoffAssignmentService)factory.getProxy();
        var command=request(99L);command.setSupplementalRelatedControlledFileIds(java.util.List.of(7L));
        assertThrows(IllegalStateException.class,()->transactional.assign(99L,10L,command));
        for(var table:java.util.List.of("signatures","stages","assignments","tasks"))
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class),table+" must roll back together");
    }

    @Test void identicalPreparedSupplementReplayReusesOriginalSignatureAndFreezeWithoutResigning() {
        var command=request(99L);command.setSupplementalRelatedControlledFileIds(java.util.List.of(7L));
        service.assign(99L,10L,command);
        var capture=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(capture.capture());
        row.setAssignmentSignatureId(7L);row.setAssignedTime(capture.getValue().getAssignedTime());row.setBpmTaskId("task-51");
        row.setAssignmentPayloadHash(capture.getValue().getAssignmentPayloadHash());
        clearInvocations(signatureService,relationStageService,taskService);
        service.assign(99L,10L,command);
        verifyNoInteractions(signatureService,taskService);
        verify(relationStageService).freeze(any(),argThat(signature->Long.valueOf(7L).equals(signature.signatureId())));
        var omitted=request(99L);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,omitted));
    }

    @Test void invalidSupplementIdentitiesCannotReachSignatureAndDuplicatesCannotBeSilentlyDropped() {
        for(var ids:java.util.List.of(java.util.List.of(0L),java.util.List.of(7L,7L))) {
            var command=request(99L);command.setSupplementalRelatedControlledFileIds(ids);
            assertThrows(RuntimeException.class,()->service.assign(99L,10L,command));
        }
        verifyNoInteractions(signatureService,taskService,remediationService);
    }

    @Test void assignmentReplayCannotSilentlyAddSupplementOrChangeTheFrozenDepartmentObligation() {
        service.assign(99L,10L,request(99L));
        var captured=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(captured.capture());
        row.setAssignmentSignatureId(7L);row.setAssignedTime(captured.getValue().getAssignedTime());
        row.setBpmTaskId("task-51");row.setAssignmentPayloadHash(captured.getValue().getAssignmentPayloadHash());
        clearInvocations(signatureService,taskService);
        var changed=request(99L);changed.setSupplementalRelatedControlledFileIds(java.util.List.of(7L));
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,changed));
        row.setDepartmentId(52L);
        assertThrows(RuntimeException.class,()->service.assign(99L,10L,request(99L)));
        verifyNoInteractions(signatureService,taskService);
    }

    private DccSignoffAssignmentReqVO request(long assignee) {
        var req=new DccSignoffAssignmentReqVO(); req.setTaskId("task-51");req.setAssigneeUserId(assignee);
        req.setPassword("password");req.setReason("指派本人");return req;
    }

    private void verifyNoAssignmentWrites() {
        verifyNoInteractions(signatureService,taskService,relationStageService,remediationService);
        verify(snapshotMapper,never()).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
    }

    private void assertAssignmentDenied(long assignee) {
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->service.assign(99L,10L,request(assignee)));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED.getCode(),failure.getCode());
    }
}
