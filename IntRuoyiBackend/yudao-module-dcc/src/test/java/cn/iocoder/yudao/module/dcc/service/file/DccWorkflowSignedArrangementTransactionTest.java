package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual A assignment, D authorization/store and Spring Flowable share one task-owned H2 transaction. */
class DccWorkflowSignedArrangementTransactionTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Resource DccControlledFileSignatureMapper signatures;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper trainingTickets;
    JdbcTemplate jdbc;ProcessEngine engine;DccWorkflowSignoffAssignmentService assignment;
    GxpAuditService audit;String processId,taskId;DccControlledFileTaskAssigneeSnapshotDO obligation;
    private static final List<String> TABLES=List.of("dcc_relation_notification_outbox","dcc_relation_remediation_task",
            "dcc_relation_controlled_event","dcc_relation_arrangement","dcc_current_file_relation_set","dcc_current_file_relation");
    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))
            if(!sql.isBlank())jdbc.execute(sql);
        for(String table:TABLES)jdbc.update("DELETE FROM "+table);
        jdbc.update("DELETE FROM dcc_controlled_file_related_file");
        jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=42 AND tenant_id=1");
        var config=new SpringProcessEngineConfiguration();config.setDataSource(dataSource);config.setTransactionManager(manager);
        // Explicit test configuration: existing shared H2 is MODE=MYSQL, so use the corresponding Flowable schema dialect.
        config.setDatabaseType("mysql");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        engine=config.buildProcessEngine();
        String model="""
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="DCC-assignment-txn">
                  <process id="dcc-controlled-file-revision" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="toSignoff" sourceRef="start" targetRef="MATRIX_REVIEW"/>
                    <userTask id="MATRIX_REVIEW" name="会签" flowable:assignee="99"/>
                    <sequenceFlow id="toTraining" sourceRef="MATRIX_REVIEW" targetRef="TRAINING"/>
                    <receiveTask id="TRAINING" name="线下培训"/><sequenceFlow id="toReview" sourceRef="TRAINING" targetRef="DOC_CONTROL_REVIEW"/>
                    <userTask id="DOC_CONTROL_REVIEW" name="文控审核" flowable:assignee="101"/>
                    <sequenceFlow id="toEnd" sourceRef="DOC_CONTROL_REVIEW" targetRef="end"/><endEvent id="end"/>
                  </process>
                </definitions>
                """;
        engine.getRepositoryService().createDeployment().tenantId("1").addString("assignment.bpmn20.xml",model).deploy();
        processId=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId("dcc-controlled-file-revision","42",Map.of("controlledFileId",42L),"1").getId();
        var task=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();taskId=task.getId();
        engine.getTaskService().setVariableLocal(taskId,BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,"42:MATRIX_REVIEW:51");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,deleted) VALUES(10,10,'SOP.pdf','N-1','ACTIVE_CHAIN',1,0),(20,10,'Related.pdf','N-2','ACTIVE_CHAIN',1,0)");
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,
                  file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,process_instance_id,process_definition_key)
                VALUES(42,10,10,3,100,100,'SOP.pdf','SOP','N-1','A/1','PENDING_MATRIX_REVIEW',99,99,1,0,?,'dcc-controlled-file-revision')
                """,processId);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,relation_source,tenant_id,deleted) VALUES(42,20,9,20,'SUBMIT',1,0)");
        obligation=DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(42L).tenantId(1L).stageCode("MATRIX_REVIEW")
                .stageNo(1).departmentId(51L).leaderUserId(99L).assigneeUserId(99L).processInstanceId(processId)
                .obligationId("42:MATRIX_REVIEW:51").build();obligations.insert(obligation);
        var bpm=new BpmTaskServiceImpl();wire(bpm,"taskService",engine.getTaskService());
        var users=mock(AdminUserApi.class);when(users.getUser(anyLong())).thenAnswer(call->new AdminUserRespDTO().setId(call.getArgument(0))
                .setStatus(0).setDeptId(51L).setNickname("事务测试账号").setPostIds(Set.of(10L)));
        var resolver=new DccLatestControlledFileResolverImpl();wire(resolver,"fileMapper",files,"masterMapper",masters);
        var access=new DccRelationAccessPolicyImpl();wire(access,"query",mock(DccControlledFileQueryService.class),
                "obligations",obligations,"tasks",engine.getTaskService(),"users",users,"masters",masters,"projects",mock(DccProjectAccessService.class));
        audit=mock(GxpAuditService.class);
        var remediation=new DccRelationRemediationService(new DccRelationStore(jdbc,audit),resolver,access,mock(DccRelationNotificationPostCommitScheduler.class));
        // Authentication/HMAC is a substituted kernel port; its projection write joins this real transaction.
        var signing=mock(DccSignatureVerificationService.class);
        when(signing.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any())).thenAnswer(call->{
            var signed=DccControlledFileSignatureDO.builder().controlledFileId(42L).revisionId(42L).versionNo("A/1")
                    .processInstanceId(processId).taskId(taskId).actorId(99L).actionType("ASSIGN").meaningCode("MATRIX_REVIEW_ASSIGN")
                    .signatureMode("PASSWORD").passwordVerified(true).signedAt(LocalDateTime.now().withNano(0)).comment(call.getArgument(7))
                    .evidencePayloadVersion("v4-workflow").evidenceStatus("VALID").build();
            assertEquals(1,signatures.insert(signed));
            return DccUnifiedSignatureResult.builder().signatureId(signed.getId()).evidenceStatus("VALID").build();
        });
        var definitions=new cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionServiceImpl();
        wire(definitions,"repositoryService",engine.getRepositoryService());
        assignment=new DccWorkflowSignoffAssignmentService();
        wire(assignment,"definitions",definitions);
        wire(assignment,"fileMapper",files,"masterMapper",masters,"snapshotMapper",obligations,"bpmTaskService",bpm,
                "taskService",engine.getTaskService(),"runtimeService",engine.getRuntimeService(),"adminUserApi",users,
                "readinessService",mock(DccControlledFileRouteReadinessService.class),"signatureService",signing,"remediationService",remediation);
    }
    @AfterEach void clear() {
        try {if(engine!=null){if(processId!=null && engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processId).singleResult()!=null)
            engine.getRuntimeService().deleteProcessInstance(processId,"task-owned fixture cleanup");engine.close();}}
        finally {for(String table:TABLES)jdbc.update("DELETE FROM "+table);jdbc.update("DELETE FROM dcc_controlled_file_related_file");
            jdbc.update("DELETE FROM dcc_controlled_file_task_assignee_snapshot WHERE controlled_file_id=42 AND tenant_id=1");TenantContextHolder.clear();}
    }
    @Test void selectedArrangementsAndRealTaskAssigneeCommitWithOneSignature() {
        assign();assertEquals("100",actualTask().getAssignee());
        var row=obligations.selectById(obligation.getId());assertEquals(100L,row.getAssigneeUserId());assertNotNull(row.getAssignmentSignatureId());
        assertEquals(processId,row.getProcessInstanceId());assertEquals(1,count("dcc_controlled_file_signature"));
        assertEquals(1,count("dcc_relation_arrangement"));assertEquals(0,count("dcc_relation_notification_outbox"));
        assertEquals(row.getAssignmentSignatureId(),engine.getTaskService().getVariableLocal(taskId,"dccAssignmentSignatureId"));
    }
    @Test void aLateFailureRollsBackActualFlowableAssignmentAndBothDomainEvidenceStores() {
        assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{assignment.assign(99L,42L,request());throw new IllegalStateException("late failure");}));
        unchanged();
    }
    @Test void dAuditFailureRollsBackSignatureProjectionObligationAndArrangement() {
        doThrow(new IllegalStateException("D audit persistence failed")).when(audit).append(any());
        assertThrows(IllegalStateException.class,this::assign);unchanged();
    }
    @Test void concurrentlyReplayedPayloadCommitsOneSignatureOneArrangementAndOneTaskChange() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Void> call=()->{TenantContextHolder.setTenantId(1L);try{start.await();assign();return null;}finally{TenantContextHolder.clear();}};
        try{var a=pool.submit(call);var b=pool.submit(call);start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
        assertEquals(1,count("dcc_controlled_file_signature"));assertEquals(1,count("dcc_relation_arrangement"));
        assertEquals("100",actualTask().getAssignee());
    }
    private void unchanged(){assertEquals("99",actualTask().getAssignee());assertNull(engine.getTaskService().getVariableLocal(taskId,"dccAssignmentSignatureId"));
        var row=obligations.selectById(obligation.getId());assertNull(row.getAssignmentSignatureId());assertEquals(99L,row.getAssigneeUserId());
        assertEquals(0,count("dcc_controlled_file_signature"));assertEquals(0,count("dcc_relation_arrangement"));}
    @Test void withdrawingTheProcessDoesNotLeaveItCanceledIfTheDomainTransactionFailsLater() {
        var platform=mock(DccControlledContentAdapter.class);
        doThrow(new IllegalStateException("withdrawal audit failed")).when(platform).recordWithdrawn(any(),any(),any());
        var workflow=withdrawalWorkflow(platform);
        assertThrows(IllegalStateException.class,()->withdraw(workflow));
        assertEquals("PENDING_MATRIX_REVIEW",files.selectById(42L).getStatus());assertNotNull(actualTask());
        assertEquals(processId,actualTask().getProcessInstanceId());assertEquals("99",actualTask().getAssignee());
        assertEquals(0,count("dcc_relation_notification_outbox"));
    }
    @Test void aCommittedWithdrawalRejectsLaterAssignmentAndDoesNotCreateAnySuccessEvidence() {
        withdraw(withdrawalWorkflow(mock(DccControlledContentAdapter.class)));
        assertEquals("WITHDRAWN",files.selectById(42L).getStatus());assertNull(actualTask());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::assign);
        assertEquals(0,count("dcc_controlled_file_signature"));assertEquals(0,count("dcc_relation_arrangement"));
        assertEquals(0,count("dcc_relation_notification_outbox"));
    }
    @Test void withdrawalAndSignedAssignmentSerializeOnTheSameMasterAndPreserveAnyEarlierEvidence() throws Exception {
        var workflow=withdrawalWorkflow(mock(DccControlledContentAdapter.class));
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> sign=()->{TenantContextHolder.setTenantId(1L);try{start.await();assign();return true;}
            catch(cn.iocoder.yudao.framework.common.exception.ServiceException rejected){
                assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED.getCode(),rejected.getCode());return false;
            }finally{TenantContextHolder.clear();}};
        Callable<Void> cancel=()->{TenantContextHolder.setTenantId(1L);try{start.await();withdraw(workflow);return null;}finally{TenantContextHolder.clear();}};
        try{var signed=pool.submit(sign);var canceled=pool.submit(cancel);start.countDown();boolean committed=signed.get(20,TimeUnit.SECONDS);canceled.get(20,TimeUnit.SECONDS);
            assertEquals(committed?1:0,count("dcc_controlled_file_signature"));assertEquals(committed?1:0,count("dcc_relation_arrangement"));}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
        assertEquals("WITHDRAWN",files.selectById(42L).getStatus());assertNull(actualTask());
        assertEquals(0,count("dcc_relation_notification_outbox"));
    }
    private DccControlledFileWorkflowServiceImpl withdrawalWorkflow(DccControlledContentAdapter platform) {
        var workflow=new DccControlledFileWorkflowServiceImpl();
        var bpm=mock(cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService.class);
        // The cancellation facade is a test port; real Flowable runtime deletion joins the same Spring transaction.
        doAnswer(call->{var request=(cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO)call.getArgument(1);
            engine.getRuntimeService().deleteProcessInstance(request.getId(),request.getReason());return null;})
                .when(bpm).cancelProcessInstanceByStartUser(eq(99L),any());
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"bpmProcessInstanceService",bpm,"platformAdapter",platform);
        return workflow;
    }
    private void withdraw(DccControlledFileWorkflowServiceImpl workflow) {
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO();request.setReason("正式撤回测试");
        tx().executeWithoutResult(s->workflow.withdrawControlledFile(99L,42L,request));
    }
    @Test void actualTrainingTicketBindingAndReceiveTaskAdvanceCommitTogether() {
        var workflow=trainingWorkflow();var request=trainingRequest();
        tx().executeWithoutResult(s->workflow.uploadTrainingRecord(99L,42L,request));
        assertEquals("BOUND",ticketState());assertEquals(42L,jdbc.queryForObject("SELECT bound_controlled_file_id FROM dcc_controlled_file_temporary_file WHERE upload_ticket='TRAINING-TXN'",Long.class));
        assertEquals(810L,files.selectById(42L).getTrainingRecordFileId());assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(42L).getStatus());
        assertEquals(0,engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").count());
        assertEquals("DOC_CONTROL_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult().getTaskDefinitionKey());
    }
    @Test void failedOuterTrainingTransactionRestoresTheAvailableTicketAndWaitingExecution() {
        var workflow=trainingWorkflow();var request=trainingRequest();
        assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{workflow.uploadTrainingRecord(99L,42L,request);throw new IllegalStateException("training commit failed");}));
        assertEquals("AVAILABLE",ticketState());assertNull(files.selectById(42L).getTrainingRecordFileId());
        assertEquals("PENDING_APPLICANT_TRAINING_RECORD",files.selectById(42L).getStatus());
        assertEquals(1,engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").count());
        assertNull(engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult());
    }
    @Test void anAvailableTicketFromAnotherTrainingRoundNeverBindsToTheCurrentWaitingVersion() {
        var workflow=trainingWorkflow();var request=trainingRequest();
        jdbc.update("UPDATE dcc_controlled_file_temporary_file SET session_id='dcc-training:42:3:old:session' WHERE upload_ticket='TRAINING-TXN'");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx().executeWithoutResult(s->workflow.uploadTrainingRecord(99L,42L,request)));
        assertEquals("AVAILABLE",ticketState());assertNull(files.selectById(42L).getTrainingRecordFileId());
        assertEquals(1,engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").count());
    }
    private DccControlledFileWorkflowServiceImpl trainingWorkflow() {
        engine.getTaskService().complete(taskId);
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD',need_training=1 WHERE id=42");
        var session=trainingRequest().getSessionId();
        jdbc.update("""
                INSERT INTO dcc_controlled_file_temporary_file(upload_ticket,session_id,purpose,category_id,uploader_id,original_file_name,
                  content_type,file_size,file_sha256,storage_file_id,status,expire_time,cleanup_status,tenant_id,deleted)
                VALUES('TRAINING-TXN',?,'TRAINING_RECORD',10,99,'offline.pdf','application/pdf',128,'test-only-body-hash',810,'AVAILABLE',?,'ACTIVE',1,0)
                """,session,LocalDateTime.now().plusDays(1));
        var fileStorage=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);
        when(fileStorage.selectById(810L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(810L).name("offline.pdf").type("application/pdf").build());
        var ticketService=new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl();
        wire(ticketService,"temporaryFileMapper",trainingTickets,"fileMapper",fileStorage);
        var processService=new cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl();wire(processService,"runtimeService",engine.getRuntimeService());
        var taskService=new BpmTaskServiceImpl();wire(taskService,"runtimeService",engine.getRuntimeService());
        var permission=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permission.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);when(categories.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        var workflow=new DccControlledFileWorkflowServiceImpl();
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "bpmProcessInstanceService",processService,"bpmTaskService",taskService,"uploadTicketService",ticketService,"permissionApi",permission,"categoryPermissionSupport",categories);
        return workflow;
    }
    private String ticketState(){return jdbc.queryForObject("SELECT status FROM dcc_controlled_file_temporary_file WHERE upload_ticket='TRAINING-TXN'",String.class);}
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO trainingRequest(){var req=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO();
        req.setSessionId("dcc-training:42:"+processId.length()+":"+processId+":session");req.setTrainingRecordUploadTicket("TRAINING-TXN");return req;}
    private org.flowable.task.api.Task actualTask(){return engine.getTaskService().createTaskQuery().taskId(taskId).singleResult();}
    private DccSignoffAssignmentReqVO request(){var req=new DccSignoffAssignmentReqVO();req.setTaskId(taskId);req.setAssigneeUserId(100L);
        req.setPassword("test-kernel-credential");req.setReason("正式指派测试");req.setRelationArrangements(List.of(new Arrangement(20L,200L,LocalDateTime.of(2026,11,1,12,0))));return req;}
    private void assign(){tx().executeWithoutResult(s->assignment.assign(99L,42L,request()));}
    private TransactionTemplate tx(){return new TransactionTemplate(manager);}
    private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
}
