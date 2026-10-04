package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.*;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccWorkflowObsoleteCompletionReadTest {
    @Test
    void exactFlowableCompletionCallbackReadsApprovedHistoryButRejectsUnsignedDomainEffect() throws Exception {
        var bpm=new BpmProcessInstanceServiceImpl();
        var listener=new BpmProcessInstanceEventListener();
        ReflectionTestUtils.setField(listener,"processInstanceService",bpm);
        ReflectionTestUtils.setField(bpm,"processDefinitionService",mock(BpmProcessDefinitionService.class));
        ReflectionTestUtils.setField(bpm,"messageService",mock(BpmMessageService.class));
        var candidates=mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUserListByTask(any())).thenReturn(List.of(99L,99L));
        when(candidates.calculateUsersByTask(any())).thenReturn(Set.of(99L));
        var factory=new BpmActivityBehaviorFactory();factory.setTaskCandidateInvoker(candidates);
        var config=new StandaloneInMemProcessEngineConfiguration();config.setJdbcUrl("jdbc:h2:mem:dcc_a_completion_read;DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        config.setActivityBehaviorFactory(factory);config.setEventListeners(List.of(listener));
        var engine=config.buildProcessEngine();
        try {
            ReflectionTestUtils.setField(bpm,"runtimeService",engine.getRuntimeService());
            ReflectionTestUtils.setField(bpm,"historyService",engine.getHistoryService());
            var domain=new DccControlledFileObsoleteServiceImpl();
            var evidence=new DccWorkflowObsoleteEvidenceGuard();
            wire(evidence,"taskSnapshotMapper",mock(DccControlledFileTaskAssigneeSnapshotMapper.class),
                    "signatureMapper",mock(DccControlledFileSignatureMapper.class),
                    "bpmTaskService",mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class),
                    "signatureManagementService",mock(DccElectronicSignatureManagementService.class));
            var files=mock(DccControlledFileMapper.class);var masters=mock(DccControlledFileMasterMapper.class);
            var audits=mock(DccControlledFileObsoleteAuditMapper.class);
            var category=mock(DccControlledFileCategoryPermissionSupport.class);var messages=mock(DccControlledFileMessageJobMapper.class);
            var file=DccControlledFileDO.builder().id(42L).masterId(20L).tenantId(1L).categoryId(10L)
                    .versionNo("A/1").status("ACTIVE").requesterId(99L).build();
            when(files.selectById(42L)).thenReturn(file);when(files.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
            when(masters.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).currentActiveControlledFileId(42L).build());
            when(category.hasCategoryPermission(any(),any(),any())).thenReturn(true);
            when(files.updateById(any(DccControlledFileDO.class))).thenReturn(1);
            when(audits.insert(any(DccControlledFileObsoleteAuditDO.class))).thenReturn(1);
            when(masters.clearCurrentActive(1L,20L,42L)).thenReturn(1);
            when(messages.insert(any(DccControlledFileMessageJobDO.class))).thenReturn(1);
            wire(domain,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"obsoleteAuditMapper",audits,
                    "permissionSupport",category,"obsoleteProcessService",bpm,"obsoleteArchiveRequestService",mock(DccWorkflowObsoleteArchiveRequestService.class),
                    "obsoleteEvidenceGuard",evidence,
                    "distributionMapper",mock(DccControlledFileDistributionMapper.class),"trainingMapper",mock(DccControlledFileTrainingMapper.class),
                    "messageJobMapper",messages,"messageDeliveryService",mock(DccControlledFileMessageDeliveryService.class),
                    "platformAdapter",mock(DccControlledContentAdapter.class));
            AtomicInteger effects=new AtomicInteger();
            ReflectionTestUtils.setField(bpm,"processInstanceEventPublisher",new BpmProcessInstanceEventPublisher(event->{
                var approved=(BpmProcessInstanceStatusEvent)event;
                assertEquals(2,approved.getStatus());
                var request=new DccControlledFileObsoleteReqVO();request.setReason("正式批准作废");
                request.setApprovalProcessInstanceId(approved.getId());request.setApprovedVersionNo("A/1");
                var history=bpm.getHistoricProcessInstance(approved.getId());
                assertNotNull(history);assertEquals(2,history.getProcessVariables().get("PROCESS_STATUS"));
                assertEquals(99L,history.getProcessVariables().get("PROCESS_LAST_APPROVER_USER_ID"));
                var completedTasks=engine.getHistoryService().createHistoricTaskInstanceQuery().processInstanceId(approved.getId())
                        .includeTaskLocalVariables().list();
                assertEquals(3,completedTasks.size());
                assertTrue(completedTasks.stream().allMatch(task->task.getEndTime()!=null
                        && Integer.valueOf(2).equals(task.getTaskLocalVariables().get("TASK_STATUS"))));
                assertThrows(RuntimeException.class,()->domain.applyApprovedObsoleteControlledFile(99L,42L,request));
                effects.incrementAndGet();
            }));
            String sql=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
            String marker="SET @dcc_obsolete_bpmn = '";int start=sql.indexOf(marker)+marker.length();
            String xml=sql.substring(start,sql.indexOf("';",start));
            engine.getRepositoryService().createDeployment().tenantId("1").addString("obsolete.bpmn20.xml",xml).deploy();
            TenantContextHolder.setTenantId(1L);engine.getIdentityService().setAuthenticatedUserId("99");
            var instance=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-controlled-file-obsolete",Map.of(
                    "PROCESS_STATUS",1,"systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE",
                    "objectId","42","objectVersion","A/1","PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("dept-1","dept-2")),
                    "PROCESS_START_USER_SELECT_ASSIGNEES",Map.of("MATRIX_REVIEW",List.of(99L,99L)),
                    "PROCESS_APPROVE_USER_SELECT_ASSIGNEES",Map.of("MATRIX_APPROVAL",List.of(99L))),"1");
            var matrix=engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list();
            assertEquals(2,matrix.size());
            for(var task:matrix) {
                engine.getTaskService().setVariableLocal(task.getId(),"TASK_STATUS",2);
                engine.getTaskService().complete(task.getId());
            }
            var approval=engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).singleResult();
            assertEquals("MATRIX_APPROVAL",approval.getTaskDefinitionKey());
            engine.getTaskService().setVariableLocal(approval.getId(),"TASK_STATUS",2);
            engine.getRuntimeService().setVariable(instance.getId(),"PROCESS_LAST_APPROVER_USER_ID",99L);
            engine.getTaskService().complete(approval.getId());
            assertEquals(1,effects.get());verify(files,never()).updateById(any(DccControlledFileDO.class));
        } finally { engine.close();TenantContextHolder.clear(); }
    }
    private void wire(Object target,Object... fields) {
        for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);
    }
}
