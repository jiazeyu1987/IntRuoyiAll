package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.flowable.task.api.Task;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual command service, explicit mock persistence/BPM/crypto ports. Transaction rollback tested separately. */
class DccDelayedEffectiveConfirmationServiceTest {
    private Object service;
    private DccControlledFileMapper files;
    private DccControlledFileMasterMapper masters;
    private DccSignatureVerificationService signing;
    private BpmTaskService tasks;
    private DccControlledFileDO file;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"g01-command-mock"),DccControlledFileDO.class);
        service=Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationService").getConstructor().newInstance();
        files=mock(DccControlledFileMapper.class);masters=mock(DccControlledFileMasterMapper.class);signing=mock(DccSignatureVerificationService.class);tasks=mock(BpmTaskService.class);
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).requesterId(99L).versionNo("B/1").effectiveMode("DELAYED")
                .status("PENDING_DELAYED_EFFECTIVE_CONFIRM").processInstanceId("R").processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION).build();
        when(files.selectById(20L)).thenReturn(file);when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        when(masters.selectByIdForUpdate(3L)).thenReturn(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).build());
        var account=mock(AdminUserApi.class);when(account.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        var task=mock(Task.class);when(task.getId()).thenReturn("T");when(task.getTenantId()).thenReturn("1");when(task.getProcessInstanceId()).thenReturn("R");
        when(task.getProcessDefinitionId()).thenReturn("revision:6:exact");when(task.getTaskDefinitionKey()).thenReturn("DELAYED_EFFECTIVE_CONFIRM");when(task.getAssignee()).thenReturn("99");when(tasks.getTask("T")).thenReturn(task);
        var bound=mock(DccBoundWorkflowContractResolver.class);when(bound.resolve(file)).thenReturn(new DccBoundWorkflowContractResolver.BoundExecution(1L,20L,"R",2,DccControlledFileProcessDefinitionKeys.REVISION,"revision:6:exact","deployment","a".repeat(64),"PUBLIC_MATRIX_NATIVE_V2",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM,true));
        var clock=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return LocalDateTime.of(2026,10,15,12,0,0,123456789);}};clock.setZoneId("Asia/Shanghai");
        for(var pair:Map.of("fileMapper",files,"masterMapper",masters,"signatureService",signing,"tasks",tasks,"accounts",account,"boundContracts",bound,"datePolicy",clock).entrySet())ReflectionTestUtils.setField(service,pair.getKey(),pair.getValue());
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    private Object command(String time,String password,String reason) throws Exception {
        var cls=Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDelayedEffectiveConfirmReqVO");var request=cls.getConstructor().newInstance();
        for(var entry:Map.of("taskId","T","processInstanceId","R","attributeRound",2,"expectedProcessDefinitionId","revision:6:exact","expectedDefinitionDigest","a".repeat(64),"selectedEffectiveTime",LocalDateTime.parse(time),"password",password,"reason",reason,"idempotencyKey","K").entrySet())ReflectionTestUtils.setField(request,entry.getKey(),entry.getValue());
        return request;
    }
    @Test void wrongActorEarlierTimeMissingPasswordOrReasonRejectBeforeSigningOrBpm() throws Exception {
        var valid=command("2026-10-16T12:00:00","credential","真实意见");
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",88L,20L,valid));
        for(var request:List.of(command("2026-10-15T12:00:00","credential","真实意见"),command("2026-10-16T12:00:00","","真实意见"),command("2026-10-16T12:00:00","credential","  ")))
            assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request));
        verifyNoInteractions(signing);verify(tasks,never()).approveTask(any(),any());verify(files,never()).update(any(),any());
    }
    private void positivePorts(){
        file.setPreparedWorkflowContract("PUBLIC_MATRIX_NATIVE_V2");file.setPreparedProcessDefinitionId("revision:6:exact");file.setPreparedDefinitionDigest("a".repeat(64));file.setNeedTraining(false);
        var all=new ArrayList<DccControlledFileSignatureDO>();var roster=new ArrayList<DccControlledFileRouteSnapshotDO>();
        for(String stage:List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")){
            var row=DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).stageCode(stage).resolvedUserIds("99").candidateSourceType(stage.equals("MATRIX_REVIEW")?"DEPT":"USER")
                    .candidateSourceId(stage.equals("MATRIX_REVIEW")?7L:99L).candidateSourceIds(stage.equals("MATRIX_REVIEW")?"7":"99").approveMethod(stage.equals("MATRIX_REVIEW")?"ALL":"ANY").requireAllApprovals(stage.equals("MATRIX_REVIEW")).stageNo(stage.equals("SUPERVISOR_REVIEW")?0:1).stageOrder(stage.equals("SUPERVISOR_REVIEW")?0:10).build();roster.add(row);
            all.add(DccControlledFileSignatureDO.builder().id((long)all.size()+1).controlledFileId(20L).revisionId(20L).versionNo("B/1").taskId(stage).actorId(99L).processInstanceId("R").actionType("APPROVE")
                    .meaningCode(stage+"_APPROVE").passwordVerified(true).signedAt(LocalDateTime.of(2026,10,15,10,0)).evidenceHash("b".repeat(64)).evidenceStatus("VALID").evidencePayloadVersion("v4-workflow").build());
            var task=mock(org.flowable.task.api.history.HistoricTaskInstance.class);when(task.getEndTime()).thenReturn(new Date());when(task.getProcessInstanceId()).thenReturn("R");when(task.getProcessDefinitionId()).thenReturn("revision:6:exact");when(task.getTenantId()).thenReturn("1");when(task.getAssignee()).thenReturn("99");when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));when(tasks.getHistoricTask(stage)).thenReturn(task);
        }
        var assign=DccControlledFileSignatureDO.builder().id(70L).controlledFileId(20L).revisionId(20L).versionNo("B/1").taskId("MATRIX_REVIEW").actorId(88L).processInstanceId("R").actionType("ASSIGN")
                .meaningCode("MATRIX_REVIEW_ASSIGN").passwordVerified(true).signedAt(LocalDateTime.of(2026,10,15,10,0)).evidenceHash("b".repeat(64)).evidenceStatus("VALID").evidencePayloadVersion("v4-workflow").build();all.add(assign);
        var ownerApproval=all.stream().filter(row->"MATRIX_APPROVAL_APPROVE".equals(row.getMeaningCode())).findFirst().orElseThrow();
        file.setFileOwnerUserId(99L);file.setFileOwnerSignatureId(ownerApproval.getId());file.setFileOwnerSelectedTime(ownerApproval.getSignedAt());
        file.setFileOwnerProcessInstanceId("R");file.setFileOwnerApprovalTaskId("MATRIX_APPROVAL");
        var signed=DccControlledFileSignatureDO.builder().id(700L).controlledFileId(20L).comment("真实意见").build();
        var signatureMapper=mock(DccControlledFileSignatureMapper.class);when(signatureMapper.selectListByControlledFileId(20L)).thenReturn(all);when(signatureMapper.selectById(700L)).thenReturn(signed);
        var routeMapper=mock(DccControlledFileRouteSnapshotMapper.class);when(routeMapper.selectListByControlledFileId(20L)).thenReturn(roster);
        var assigned=mock(DccControlledFileTaskAssigneeSnapshotMapper.class);when(assigned.selectListByControlledFileId(20L)).thenReturn(List.of(DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(20L).departmentId(7L).stageCode("MATRIX_REVIEW")
                .processInstanceId("R").assignmentSignatureId(70L).assignedTime(LocalDateTime.of(2026,10,15,10,0)).bpmTaskId("MATRIX_REVIEW").leaderUserId(88L).assigneeUserId(99L).build()));
        var crypto=mock(DccElectronicSignatureManagementService.class);when(crypto.verifySignatureEvidence(anyLong())).thenAnswer(call->{var result=new cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO();result.setSignatureId(call.getArgument(0));result.setVerificationStatus("VALID");return result;});
        var proof=new DccDelayedEffectiveConfirmationEvidence.VerifiedConfirmation(LocalDateTime.of(2026,10,16,12,0),LocalDateTime.of(2026,10,15,12,0,0,123456000),700L,"R","T",2,99L,"a".repeat(64));
        var verifier=mock(DccDelayedEffectiveConfirmationEvidence.class);when(verifier.requireSignedForActiveTask(eq(file),any(),eq(700L))).thenReturn(proof);when(verifier.requireConfirmed(eq(file),any())).thenReturn(proof);
        when(signing.verifyPasswordAndCreateDelayedConfirmationSignature(any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(DccUnifiedSignatureResult.builder().signatureId(700L).build());
        when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        var audit=mock(DccWorkflowFileStateAudit.class);when(audit.capture(any(),any())).thenReturn(new DccWorkflowFileStateAudit.Snapshot(1L,20L,3L,"B/1","PENDING_DELAYED_EFFECTIVE_CONFIRM","{}"));
        var historical=mock(org.flowable.task.api.history.HistoricTaskInstance.class);when(historical.getTaskLocalVariables()).thenReturn(Map.of("dccDelayedConfirmationKey","K"));when(tasks.getHistoricTask("T")).thenReturn(historical);
        for(var entry:Map.of("signatures",signatureMapper,"routes",routeMapper,"assignments",assigned,"signatureManagement",crypto,"confirmationEvidence",verifier,"lifecycle",mock(DccControlledFileLifecycleService.class),"audit",audit,"electronicAuthorization",mock(DccElectronicSignatureAuthorizationService.class),"engineTasks",mock(org.flowable.engine.TaskService.class)).entrySet())ReflectionTestUtils.setField(service,entry.getKey(),entry.getValue());
    }
    @Test void actualCommandSignsThenCasAuditsAndApprovesOnceAndReplaysOriginalEvidence() throws Exception {
        positivePorts();var request=command("2026-10-16T12:00:00","credential","真实意见");var first=ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request);
        assertEquals("CONFIRMED",ReflectionTestUtils.invokeMethod(first,"getStatus"));assertEquals(LocalDateTime.of(2026,10,15,12,0,0,123456000),file.getDelayedEffectiveConfirmedAt());
        assertNotNull(ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request));verify(signing,times(1)).verifyPasswordAndCreateDelayedConfirmationSignature(any(),any(),any(),any(),any(),any(),any(),any());verify(tasks,times(1)).approveTask(any(),any());
        var changed=command("2026-10-16T12:00:01","credential","真实意见");assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,changed));verify(tasks,times(1)).approveTask(any(),any());
    }
    @Test void failedCasOrAuditNeverAdvancesBpm() throws Exception {
        positivePorts();when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(0);var request=command("2026-10-16T12:00:00","credential","真实意见");
        var cas=assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request));assertTrue(cas.getMessage().contains("保存冲突"));verify(tasks,never()).approveTask(any(),any());
        when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        var audit=(DccWorkflowFileStateAudit)ReflectionTestUtils.getField(service,"audit");doThrow(new IllegalStateException("actual port late audit failure")).when(audit).recordDelayedConfirmation(any(),any(),any());
        var append=assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request));assertTrue(append.getMessage().contains("late audit failure"));verify(tasks,never()).approveTask(any(),any());
    }
    @Test void bpmLateFailurePropagatesWithoutReturningSuccessfulConfirmation() throws Exception {
        positivePorts();var request=command("2026-10-16T12:00:00","credential","真实意见");
        doThrow(new IllegalStateException("real BPM port completion failed")).when(tasks).approveTask(any(),any());
        var failure=assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"confirm",99L,20L,request));
        assertTrue(failure.getMessage().contains("BPM port completion failed"));
        var audit=(DccWorkflowFileStateAudit)ReflectionTestUtils.getField(service,"audit");verify(audit).recordDelayedConfirmation(any(),eq("R"),any());
        verify(signing,times(1)).verifyPasswordAndCreateDelayedConfirmationSignature(any(),any(),any(),any(),any(),any(),any(),any());
    }
}
