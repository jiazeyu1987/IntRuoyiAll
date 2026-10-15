package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccDistributionOutboxTransactionTest extends BaseDbUnitTest {
    @DynamicPropertySource static void schema(DynamicPropertyRegistry registry){
        var root=DccDistributionSignatureTransactionTest.existingBackendRoot(java.nio.file.Path.of(System.getProperty("user.dir")));
        registry.add("spring.sql.init.schema-locations",()->String.join(",","classpath:/sql/create_tables.sql",
                root.resolve("yudao-module-system/src/test/resources/sql/create_tables.sql").toUri().toString(),"classpath:/sql/dcc_distribution_outbox.sql"));
        registry.add("mybatis-plus.global-config.db-config.id-type",()->"AUTO");
    }
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager tx;
    @Resource DccControlledFileMapper files;
    @Resource DccDistributionBatchMapper batches;
    @Resource DccDistributionDispatchAttemptMapper attempts;
    @Resource DccControlledFileDistributionMapper distributions;
    @Resource DccControlledFileDistributionRecipientMapper recipients;
    @Resource DccControlledFileMessageJobMapper jobs;
    @Resource GxpAuditEventMapper audits;
    @Resource GxpAuditLedgerSequenceMapper sequences;
    @Resource GxpAuditPolicyOperationMapper policies;
    JdbcTemplate jdbc;DccDistributionBatchService batchService;DccDistributionDispatchAttemptService attemptService;
    DccWorkflowDistributionService distributionService;DccControlledFileLifecycleEvent event;
    final LocalDateTime time=LocalDateTime.of(2026,10,11,9,0);
    @BeforeEach void fixture()throws Exception {
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,
                 status,submitter_id,requester_id,tenant_id,deleted,published_file_id,stamped_file_id,effective_date,process_instance_id,process_definition_key,
                 controlled_time,activated_time,distribution_department_ids_json)
                VALUES(42,10,10,20,100,100,'SOP.pdf','SOP','SOP-42','B/1','ACTIVE',99,99,1,0,100,100,'2026-10-11','round-42','dcc-controlled-file-upload',?,?,'["61"]')
                """,time,time);
        event=new DccControlledFileLifecycleEvent("root-event42","ACTIVATED",1L,10L,42L,null,"B/1","round-42",time);
        jdbc.update("INSERT INTO dcc_workflow_lifecycle_event(tenant_id,event_key,event_type,master_id,controlled_file_id,version_no,approval_process_instance_id,occurred_at,delivery_status) VALUES(1,'root-event42','ACTIVATED',10,42,'B/1','round-42',?,'PENDING')",time);
        var audit=new GxpAuditServiceImpl();wire(audit,"auditEventMapper",audits,"ledgerSequenceMapper",sequences,"policyOperationMapper",policies);
        for(String op:List.of("dcc.distribution.batch.accept","dcc.distribution.batch.prepare","dcc.distribution.batch.fail","dcc.distribution.attempt.start","dcc.distribution.attempt.finish")){
            var policy=new GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setPolicyVersion("ic10-memory-fixture");policy.setOperationId(op);policy.setSourceType("SERVICE_METHOD");
            policy.setSourceLocator("W07-memory-test#"+op);policy.setDomain("DCC");policy.setSubjectType("DCC_DISTRIBUTION");policy.setActionType("UPDATE");
            policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");policy.setSignaturePolicy("NOT_REQUIRED");policy.setStatePolicy("PRESENT_TO_PRESENT");policy.setRetentionClass("GXP_CONTROLLED_DOCUMENT");
            policy.setTestIds("IC10");policy.setOwner("w07-memory-fixture");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
        }
        distributionService=new DccWorkflowDistributionService();wire(distributionService,"controlledFileMapper",files,"distributionMapper",distributions,"recipientMapper",recipients,"messageJobMapper",jobs,"messageDeliveryService",mock(DccControlledFileMessageDeliveryService.class));
        batchService=new DccDistributionBatchService();wire(batchService,"controlledFileMapper",files,"batchMapper",batches,"distributionMapper",distributions,"recipientMapper",recipients,"messageJobMapper",jobs,"jdbcTemplate",jdbc,"gxpAuditService",audit,"transactionManager",tx,"distributionService",distributionService);
        attemptService=new DccDistributionDispatchAttemptService();wire(attemptService,"batchMapper",batches,"attemptMapper",attempts,"messageJobMapper",jobs,"gxpAuditService",audit);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();if(jdbc!=null)for(String table:List.of("dcc_distribution_dispatch_attempt","dcc_distribution_batch","gxp_audit_event","gxp_audit_ledger_sequence","gxp_audit_policy_operation","dcc_workflow_lifecycle_event"))jdbc.update("DELETE FROM "+table);TenantContextHolder.clear();}
    static void wire(Object bean,Object...fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
    Long accept(){return new TransactionTemplate(tx).execute(ignored->batchService.acceptActivatedEvent(event));}
    DccDistributionRosterSnapshot roster(){return new DccDistributionRosterSnapshot(1L,42L,"B/1","round-42",time,"formal-fixture-policy",List.of(new DccDistributionRosterSnapshot.Department(61L,List.of(99L,100L))));}
    @Test void acceptedVersionAndUndecidedPolicyAreDurableFailureWithoutAnySend(){
        var id=accept();assertEquals(id,accept());assertEquals(1L,batches.selectCount());
        var result=batchService.prepareAndDispatchBatch(id,null,false);assertEquals("FAILED",result.status());assertEquals("ROSTER_POLICY_UNDECIDED",result.errorCode());
        assertEquals(0L,recipients.selectCount());assertEquals(0L,jobs.selectCount());assertEquals(time,files.selectById(42L).getActivatedTime());assertEquals("ACTIVE",files.selectById(42L).getStatus());
        assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM dcc_workflow_lifecycle_event WHERE event_key='root-event42'",String.class));
        assertEquals(2L,audits.selectCount());assertTrue(audits.selectList().stream().allMatch(row->row.getActorId()==-1L));
    }
    @Test void acceptFailureRollsBackOuterActivationAndSourceEvent(){
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.accept'");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(tx).execute(ignored->{jdbc.update("UPDATE dcc_controlled_file SET activated_time=? WHERE id=42",time.plusHours(1));
            jdbc.update("UPDATE dcc_workflow_lifecycle_event SET occurred_at=? WHERE event_key='root-event42'",time.plusHours(1));
            var next=new DccControlledFileLifecycleEvent(event.eventKey(),event.eventType(),1L,10L,42L,null,"B/1","round-42",time.plusHours(1));return batchService.acceptActivatedEvent(next);}));
        assertEquals(time,files.selectById(42L).getActivatedTime());assertEquals(0L,batches.selectCount());assertEquals(0L,audits.selectCount());
    }
    @Test void longFailurePersistsFullTextWithoutBreakingCompactAudit(){
        var id=accept();String reason="中文失败".repeat(1000)+" password=private";
        batchService.recordFailure(id,"PERSISTENCE_FAILED",new IllegalStateException(reason));var stored=batches.selectById(id);
        assertTrue(stored.getErrorMessage().length()>500);assertEquals(reason.replace("password=private","password=[REDACTED]"),stored.getErrorMessage());
        assertEquals(2L,audits.selectCount());assertEquals(time,files.selectById(42L).getActivatedTime());
    }
    @Test void formalPrepareIsAtomicAndReplayCreatesNoDuplicateObligations(){
        var id=accept();assertEquals("DISPATCH_READY",batchService.prepareAndDispatchBatch(id,roster(),true).status());
        assertEquals("DISPATCH_READY",batchService.prepareAndDispatchBatch(id,roster(),true).status());
        assertEquals(2L,jobs.selectCount());assertEquals(2L,recipients.selectCount());assertTrue(jobs.selectList().stream().allMatch(job->id.equals(job.getDistributionBatchId())));
        assertNotNull(files.selectById(42L).getDistributedTime());assertEquals(2L,audits.selectCount());
    }
    @Test void preparationChildFailureRollsBackOnlyDispatchAndRecordsRealFailedBatch(){
        var id=accept();var fail=mock(DccControlledFileMessageJobMapper.class);when(fail.insert(any(DccControlledFileMessageJobDO.class))).thenThrow(new IllegalStateException("actual job failure"));wire(distributionService,"messageJobMapper",fail);
        assertThrows(RuntimeException.class,()->batchService.prepareAndDispatchBatch(id,roster(),true));
        assertEquals(0L,recipients.selectCount());assertEquals(0L,jobs.selectCount());assertNull(files.selectById(42L).getDistributedTime());assertEquals(time,files.selectById(42L).getActivatedTime());
        assertEquals("FAILED",batches.selectById(id).getStatus());assertEquals("actual job failure",batches.selectById(id).getErrorMessage());
    }
    @Test void attemptsKeepStableMessageKeyAndSentAggregationDoesNotChangeReceipt(){
        var id=accept();batchService.prepareAndDispatchBatch(id,roster(),true);var all=jobs.selectList();
        for(var job:all){var one=new TransactionTemplate(tx).execute(ignored->attemptService.startAttempt(id,job.getId()));
            new TransactionTemplate(tx).executeWithoutResult(ignored->{jobs.updateById(DccControlledFileMessageJobDO.builder().id(job.getId()).status("FAILED").errorMessage("failure").build());attemptService.finishAttempt(id,one.getId(),false,null,new IllegalStateException("失败".repeat(700)));});
            var retry=new TransactionTemplate(tx).execute(ignored->attemptService.startAttempt(id,job.getId()));assertEquals(one.getBusinessKey(),retry.getBusinessKey());
            new TransactionTemplate(tx).executeWithoutResult(ignored->{jobs.updateById(DccControlledFileMessageJobDO.builder().id(job.getId()).status("SENT").sentAt(time).build());attemptService.finishAttempt(id,retry.getId(),true,901L,null);});}
        assertEquals(4L,attempts.selectCount());assertEquals("SENT",batches.selectById(id).getStatus());assertEquals(time,batches.selectById(id).getSentAt());
        assertTrue(recipients.selectList().stream().allMatch(row->row.getReadAt()==null && row.getAcknowledgedAt()==null));
    }
    @Test void actualPlatformCommitThenLostDomainAcknowledgementRecoversOneMessageWithNewPersistedAttempt(){
        var id=accept();batchService.prepareAndDispatchBatch(id,roster(),true);var target=jobs.selectList().get(0);
        jdbc.execute("CREATE TABLE IF NOT EXISTS w07_platform_message(id BIGINT AUTO_INCREMENT PRIMARY KEY,business_key VARCHAR(255) UNIQUE NOT NULL,created_at TIMESTAMP NOT NULL)");
        var platform=mock(NotifyMessageSendApi.class);var platformRead=mock(cn.iocoder.yudao.module.system.service.notify.NotifyMessageService.class);
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(call->{
            var request=call.getArgument(0,cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO.class);
            var platformTx=new TransactionTemplate(tx);platformTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return platformTx.execute(ignored->{var found=jdbc.queryForList("SELECT id FROM w07_platform_message WHERE business_key=?",request.getBusinessKey());
                if(found.isEmpty())jdbc.update("INSERT INTO w07_platform_message(business_key,created_at) VALUES(?,?)",request.getBusinessKey(),time);
                return jdbc.queryForObject("SELECT id FROM w07_platform_message WHERE business_key=?",Long.class,request.getBusinessKey());});
        });
        when(platformRead.getNotifyMessage(anyLong())).thenAnswer(call->{Long messageId=call.getArgument(0,Long.class);var row=jdbc.queryForMap("SELECT * FROM w07_platform_message WHERE id=?",messageId);
            var message=cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO.builder().id(((Number)row.get("id")).longValue()).tenantId(1L).userType(2)
                    .userId(target.getRecipientUserId()).templateCode(target.getTemplateCode()).businessKey((String)row.get("business_key")).build();message.setCreateTime(((java.sql.Timestamp)row.get("created_at")).toLocalDateTime());return message;});
        var failureOnce=new java.util.concurrent.atomic.AtomicBoolean();
        var failingJobs=mock(DccControlledFileMessageJobMapper.class,call->{
            if(call.getMethod().getName().equals("updateById") && call.getArgument(0) instanceof DccControlledFileMessageJobDO update
                    && "SENT".equals(update.getStatus()) && failureOnce.compareAndSet(false,true))throw new IllegalStateException("actual ACK persistence lost");
            try{return call.getMethod().invoke(jobs,call.getArguments());}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
        });
        var delivery=new DccControlledFileMessageDeliveryService();wire(delivery,"messageJobMapper",failingJobs,"notifyMessageSendApi",platform,"notifyMessageService",platformRead,
                "controlledFileMapper",files,"distributionMapper",distributions,"distributionRecipientMapper",recipients,"transactionManager",tx,"dispatchAttemptService",attemptService);
        assertThrows(RuntimeException.class,()->delivery.replayMessageJob(target));
        assertEquals("FAILED",jobs.selectById(target.getId()).getStatus());assertEquals(1L,attempts.selectCount());assertEquals("FAILED",attempts.selectList().get(0).getStatus());
        delivery.replayMessageJob(target);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM w07_platform_message",Integer.class));
        assertEquals(2L,attempts.selectCount());assertEquals("SENT",jobs.selectById(target.getId()).getStatus());assertEquals(time,jobs.selectById(target.getId()).getSentAt());
        assertEquals(2L,recipients.selectCount());assertTrue(recipients.selectList().stream().allMatch(row->row.getReadAt()==null && row.getAcknowledgedAt()==null));
        jdbc.update("DELETE FROM w07_platform_message");
    }
    @Test void lateAttemptAuditFailureRollsBackAcknowledgementThenRecoveryPreservesPlatformIdentity(){
        var id=accept();batchService.prepareAndDispatchBatch(id,roster(),true);var target=jobs.selectList().get(0);
        var attempt=new TransactionTemplate(tx).execute(ignored->attemptService.startAttempt(id,target.getId()));
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.attempt.finish'");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(tx).execute(ignored->{jobs.updateById(DccControlledFileMessageJobDO.builder().id(target.getId()).status("SENT").sentAt(time).build());attemptService.finishAttempt(id,attempt.getId(),true,901L,null);return null;}));
        assertEquals("PENDING",jobs.selectById(target.getId()).getStatus());assertEquals("STARTED",attempts.selectById(attempt.getId()).getStatus());
        assertEquals("DISPATCH_READY",batches.selectById(id).getStatus());assertTrue(recipients.selectList().stream().allMatch(row->row.getAcknowledgedAt()==null));
    }
    @Test void batchSourceDepartmentStringsCannotBeChangedOrCoercedDuringPrepare(){
        var id=accept();jdbc.update("UPDATE dcc_controlled_file SET distribution_department_ids_json='[61]' WHERE id=42");
        assertThrows(RuntimeException.class,()->batchService.prepareAndDispatchBatch(id,roster(),true));assertEquals(0L,jobs.selectCount());
        assertEquals("[\"61\"]",batches.selectById(id).getDepartmentIdsJson());assertEquals("FAILED",batches.selectById(id).getStatus());
    }
    @Test void concurrentRealReplaysCommitOnlyOneBatchAndNoDuplicateEventAudit()throws Exception{
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);
        try {
            var action=(java.util.concurrent.Callable<Long>)()->{TenantContextHolder.setTenantId(1L);try{start.await();return accept();}finally{TenantContextHolder.clear();}};
            var first=pool.submit(action);var second=pool.submit(action);start.countDown();assertEquals(first.get(15,java.util.concurrent.TimeUnit.SECONDS),second.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1L,batches.selectCount());assertEquals(1L,audits.selectCount());
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void originalNormalizedDepartmentStringsArePreservedAtLongBoundary(){
        String json="[\"61\",\"9223372036854775807\"]";jdbc.update("UPDATE dcc_controlled_file SET distribution_department_ids_json=? WHERE id=42",json);
        var id=accept();assertEquals(json,batches.selectById(id).getDepartmentIdsJson());
        jdbc.update("UPDATE dcc_controlled_file SET distribution_department_ids_json=NULL WHERE id=42");
        assertThrows(RuntimeException.class,()->batchService.prepareAndDispatchBatch(id,roster(),true));assertEquals(json,batches.selectById(id).getDepartmentIdsJson());
    }
    @Test void existingLegacyDistributionRowsCannotBeAdoptedByNewBatch(){
        var id=accept();jdbc.update("INSERT INTO dcc_controlled_file_distribution(id,controlled_file_id,department_id,distribution_medium,status,tenant_id,deleted) VALUES(501,42,61,'PUBLIC_FOLDER','PENDING',1,0)");
        assertThrows(RuntimeException.class,()->batchService.prepareAndDispatchBatch(id,roster(),true));assertEquals(0L,jobs.selectCount());
        assertEquals(1L,distributions.selectCount());assertEquals("FAILED",batches.selectById(id).getStatus());
    }
    @Test void privateAdditiveSchemaRepeatsWithoutChangingOriginalMessageBytes()throws Exception{
        String original="原始失败：字节🙂";jdbc.update("INSERT INTO dcc_controlled_file_message_job(id,business_type,business_id,template_code,recipient_user_id,status,error_message,tenant_id,deleted) VALUES(990,'TRAINING',1,'dcc_training',99,'FAILED',?,1,0)",original);
        var script=new org.springframework.core.io.ClassPathResource("sql/dcc_distribution_outbox.sql");
        try(var connection=dataSource.getConnection()){org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);}
        assertEquals(original,jdbc.queryForObject("SELECT error_message FROM dcc_controlled_file_message_job WHERE id=990",String.class));
        assertNull(jobs.selectById(990L).getDistributionBatchId());assertEquals(1L,jobs.selectCount());
    }
}
