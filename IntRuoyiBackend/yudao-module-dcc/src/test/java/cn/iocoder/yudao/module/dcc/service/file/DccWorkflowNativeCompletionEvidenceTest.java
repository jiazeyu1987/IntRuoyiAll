package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exact native models and real synchronous final callback; authentication/HMAC is an explicit external port. */
class DccWorkflowNativeCompletionEvidenceTest {
    @ParameterizedTest @CsvSource({"upload,false","upload,true","revision,false","revision,true"})
    void nativeCompletedTaskEvidenceIsReadableInsideTheActualFinalProcessCallback(String action,boolean training) throws Exception {
        var processService=new BpmProcessInstanceServiceImpl();var listener=new BpmProcessInstanceEventListener();
        ReflectionTestUtils.setField(listener,"processInstanceService",processService);
        ReflectionTestUtils.setField(processService,"processDefinitionService",mock(BpmProcessDefinitionService.class));
        ReflectionTestUtils.setField(processService,"messageService",mock(BpmMessageService.class));
        var candidates=mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUserListByTask(any())).thenReturn(List.of(99L,99L));
        when(candidates.calculateUsersByTask(any())).thenReturn(Set.of(99L));
        var behaviors=new BpmActivityBehaviorFactory();behaviors.setTaskCandidateInvoker(candidates);
        var config=new StandaloneInMemProcessEngineConfiguration();config.setJdbcUrl("jdbc:h2:mem:dcc_native_evidence_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        config.setActivityBehaviorFactory(behaviors);config.setEventListeners(List.of(listener));
        var engine=config.buildProcessEngine();
        TenantContextHolder.setTenantId(1L);
        try {
            ReflectionTestUtils.setField(processService,"runtimeService",engine.getRuntimeService());
            ReflectionTestUtils.setField(processService,"historyService",engine.getHistoryService());
            var tasks=new BpmTaskServiceImpl();ReflectionTestUtils.setField(tasks,"historyService",engine.getHistoryService());
            var finalization=new DccControlledFileFinalizationServiceImpl();ReflectionTestUtils.setField(finalization,"bpmTaskService",tasks);
            var signatures=mock(DccElectronicSignatureManagementService.class);
            when(signatures.verifySignatureEvidence(anyLong())).thenAnswer(call->{var result=new DccSignatureVerifyRespVO();
                result.setSignatureId(call.getArgument(0));result.setVerificationStatus("VALID");return result;});
            ReflectionTestUtils.setField(finalization,"signatureManagementService",signatures);
            var file=DccControlledFileDO.builder().id(42L).masterId(20L).tenantId(1L).versionNo("A/1")
                    .processDefinitionKey("dcc-controlled-file-"+action).build();
            var evidence=new ArrayList<DccControlledFileSignatureDO>();var callbacks=new AtomicInteger();
            ReflectionTestUtils.setField(processService,"processInstanceEventPublisher",new BpmProcessInstanceEventPublisher(event->{
                var approved=(BpmProcessInstanceStatusEvent)event;
                assertEquals(2,approved.getStatus());assertEquals(file.getProcessInstanceId(),approved.getId());
                assertEquals(4,evidence.size());
                ReflectionTestUtils.invokeMethod(finalization,"verifyApprovalSignatureEvidence",file,evidence);
                for(var signed:evidence) {
                    var ended=tasks.getHistoricTask(signed.getTaskId());assertNotNull(ended.getEndTime());
                    assertEquals(2,ended.getTaskLocalVariables().get("TASK_STATUS"));
                }
                // Aggregate SQL filters can still exclude the current cached last task before command flush.
                // The production guard uses its authoritative ID read; after command completion the SQL count is checked below.
                callbacks.incrementAndGet();
            }));
            String sql=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
            String marker="SET @dcc_"+action+"_bpmn = '";int from=sql.indexOf(marker)+marker.length();
            engine.getRepositoryService().createDeployment().tenantId("1").addString(action+".bpmn20.xml",sql.substring(from,sql.indexOf("';",from))).deploy();
            engine.getIdentityService().setAuthenticatedUserId("99");
            var process=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-controlled-file-"+action,"42",Map.of(
                    "PROCESS_STATUS",1,"needTraining",training,"controlledFileId",42L,
                    "PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("dept-1","dept-2")),
                    "PROCESS_START_USER_SELECT_ASSIGNEES",Map.of("MATRIX_REVIEW",List.of(99L,99L)),
                    "PROCESS_APPROVE_USER_SELECT_ASSIGNEES",Map.of("MATRIX_APPROVAL",List.of(99L),"DOC_CONTROL_REVIEW",List.of(99L))),"1");
            file.setProcessInstanceId(process.getId());
            var reviews=engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).list();assertEquals(2,reviews.size());
            for(var task:reviews) signAndComplete(engine,file,task,evidence);
            signAndComplete(engine,file,engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult(),evidence);
            if(training) {var receive=engine.getRuntimeService().createExecutionQuery().processInstanceId(process.getId()).activityId("TRAINING").singleResult();
                assertNotNull(receive);engine.getRuntimeService().trigger(receive.getId());}
            var control=engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();assertEquals("DOC_CONTROL_REVIEW",control.getTaskDefinitionKey());
            signAndComplete(engine,file,control,evidence);
            assertEquals(1,callbacks.get());verify(signatures,times(4)).verifySignatureEvidence(anyLong());
            assertEquals(4,engine.getHistoryService().createHistoricTaskInstanceQuery().processInstanceId(process.getId()).finished().count());
        } finally {engine.close();TenantContextHolder.clear();}
    }
    private void signAndComplete(ProcessEngine engine,DccControlledFileDO file,Task task,List<DccControlledFileSignatureDO> evidence) {
        var signature=DccControlledFileSignatureDO.builder().id((long)evidence.size()+1).controlledFileId(file.getId()).revisionId(file.getId())
                .versionNo(file.getVersionNo()).processInstanceId(file.getProcessInstanceId()).taskId(task.getId()).actorId(99L)
                .actionType("APPROVE").meaningCode(task.getTaskDefinitionKey()+"_APPROVE").passwordVerified(true)
                .evidencePayloadVersion("v4-workflow").evidenceStatus("VALID").evidenceHash("test-kernel-evidence")
                .signedAt(LocalDateTime.now().withNano(0)).build();
        evidence.add(signature);
        engine.getTaskService().setVariableLocal(task.getId(),"TASK_STATUS",2);
        engine.getRuntimeService().setVariable(file.getProcessInstanceId(),"PROCESS_LAST_APPROVER_USER_ID",99L);
        engine.getTaskService().complete(task.getId());
    }
}
