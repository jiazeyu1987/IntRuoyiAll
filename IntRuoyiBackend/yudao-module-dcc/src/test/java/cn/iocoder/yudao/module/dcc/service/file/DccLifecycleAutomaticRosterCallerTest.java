package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccDistributionDispatchAttemptMapper;
import cn.iocoder.yudao.module.system.api.notify.*;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.service.notify.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real lifecycle->accept->afterCommit->formal batch->outbox->platform message Mapper.
 * Server roster and template lookup are explicit task-only external ports, never production policy/providers.
 * Inherits all original 9 lifecycle/outbox cases unchanged in business assertions. */
@Import({NotifyMessageSendApiImpl.class,NotifySendServiceImpl.class,NotifyMessageServiceImpl.class})
class DccLifecycleAutomaticRosterCallerTest extends DccLifecycleDistributionOutboxTransactionTest {
    @Resource private NotifyMessageSendApiImpl platformApi;
    @Resource private NotifyMessageServiceImpl platformMessages;
    @Resource private NotifyMessageMapper notifyRows;
    @Resource private DccDistributionDispatchAttemptMapper attempts;
    @MockitoBean private NotifyTemplateService templates;
    private ObjectProvider<DccDistributionRosterSource> providers;
    private DccDistributionRosterSource source;
    private DccControlledFileMessageDeliveryService delivery;

    @BeforeEach void formalPortFixture()throws Exception {
        for(String ddl:new ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(StandardCharsets.UTF_8).split(";"))
            if(!ddl.isBlank())jdbc.execute(ddl);
        jdbc.update("DELETE FROM system_notify_message");
        for(String op:List.of("dcc.distribution.batch.prepare","dcc.distribution.attempt.start","dcc.distribution.attempt.finish"))policy(op);
        providers=mock(ObjectProvider.class);source=mock(DccDistributionRosterSource.class);
        when(providers.orderedStream()).thenAnswer(ignored->Stream.of(source));
        when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.NotReady("ROSTER_POLICY_UNDECIDED","任务隔离政策未决：ROSTER_POLICY_UNDECIDED"));
        wire(batches,"rosterSources",providers);
        var template=NotifyTemplateDO.builder().id(310L).code("dcc_distribution").type(2).nickname("DCC系统").status(0)
                .content("{title} {version}").params(List.of("title","version")).build();
        when(templates.getNotifyTemplateByCodeFromCache("dcc_distribution")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(c->new NotifyTemplateServiceImpl().formatNotifyTemplateContent(c.getArgument(0),c.getArgument(1)));
        var attemptService=new DccDistributionDispatchAttemptService();wire(attemptService,"batchMapper",batchRows,"attemptMapper",attempts,"messageJobMapper",jobs,"gxpAuditService",ledger);
        delivery=new DccControlledFileMessageDeliveryService();wire(delivery,"messageJobMapper",jobs,"notifyMessageSendApi",platformApi,"notifyMessageService",platformMessages,
                "controlledFileMapper",files,"distributionMapper",scopes,"distributionRecipientMapper",recipients,"transactionManager",transactionManager,"dispatchAttemptService",attemptService);
        var distribution=new DccWorkflowDistributionService();wire(distribution,"controlledFileMapper",files,"distributionMapper",scopes,"recipientMapper",recipients,
                "messageJobMapper",jobs,"messageDeliveryService",delivery);
        wire(batches,"distributionService",distribution,"messageDeliveryService",delivery);
    }
    @AfterEach void cleanupPlatform(){if(jdbc!=null)jdbc.update("DELETE FROM system_notify_message");}

    @Test void readyFormalSourceCreatesActualPlatformNotificationsOnlyAfterActivationCommit() {
        readySource();
        tx.executeWithoutResult(s->{controlLocked();assertEquals(1,batchRows.selectList().size());
            assertEquals("PENDING",batchRows.selectList().get(0).getStatus());assertEquals(0,notifyRows.selectList().size());verifyNoInteractions(source);});
        var batch=batchRows.selectList().get(0);assertEquals("SENT",batch.getStatus());assertEquals(2,jobs.selectList().size());
        assertEquals(2,recipients.selectList().size());assertEquals(1,scopes.selectList().size());assertEquals(2,notifyRows.selectList().size());assertEquals(2,attempts.selectList().size());
        assertTrue(jobs.selectList().stream().allMatch(job->"SENT".equals(job.getStatus())&&job.getSentAt()!=null&&Objects.equals(batch.getId(),job.getDistributionBatchId())));
        assertTrue(recipients.selectList().stream().allMatch(recipient->recipient.getReadAt()==null&&recipient.getAcknowledgedAt()==null));
        var captor=org.mockito.ArgumentCaptor.forClass(DccDistributionRosterSource.Request.class);verify(source).resolve(captor.capture());
        assertEquals(1L,captor.getValue().tenantId());assertEquals(20L,captor.getValue().controlledFileId());assertEquals("round-1",captor.getValue().approvalProcessInstanceId());
        assertEquals(List.of("31"),captor.getValue().savedDepartmentIds());assertEquals(batch.getId(),captor.getValue().batchId());assertEquals(now,captor.getValue().activatedAt());
        verify(batches).processAutomaticBatch(batch.getId());verify(batches,never()).prepareAndDispatchBatch(anyLong(),any(),anyBoolean());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'",String.class));
    }
    @Test void missingSourceReturnsAccurateIndependentFailedBatchAfterCommittedActivation(){
        when(providers.orderedStream()).thenAnswer(ignored->Stream.empty());control();
        assertEquals("ROSTER_SOURCE_MISSING",batchRows.selectList().get(0).getErrorCode());assertEquals("FAILED",batchRows.selectList().get(0).getStatus());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertTrue(recipients.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());verifyNoInteractions(source);
    }
    @Test void explicitUnreadyPolicyDoesNotInventARecipientRoster(){
        when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.NotReady("ROSTER_POLICY_NOT_READY","仍缺政策决定"));control();
        assertEquals("ROSTER_POLICY_NOT_READY",batchRows.selectList().get(0).getErrorCode());assertEquals("ACTIVE",files.selectById(20L).getStatus());
        assertTrue(scopes.selectList().isEmpty());assertTrue(jobs.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());
    }
    @Test void technicalDirectoryFailurePersistsTruthAndCannotBreakActiveOrTheNextCallback(){
        when(source.resolve(any())).thenThrow(new IllegalStateException("task directory unavailable"));var later=new AtomicBoolean();
        assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();registerLater(later);}));
        assertTrue(later.get());assertEquals("ROSTER_SOURCE_FAILED",batchRows.selectList().get(0).getErrorCode());assertEquals("ACTIVE",files.selectById(20L).getStatus());
        assertTrue(notifyRows.selectList().isEmpty());assertEquals(1,count("gxp_audit_event","operation_id='dcc.distribution.batch.fail'"));
    }
    @Test void latePrepareAuditFailureRollsDispatchBackAndLeavesCommittedActiveWithFailedBatch(){
        readySource();jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.prepare'");var later=new AtomicBoolean();
        assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();registerLater(later);}));assertTrue(later.get());
        assertEquals("DISPATCH_PREPARE_FAILED",batchRows.selectList().get(0).getErrorCode());assertEquals("ACTIVE",files.selectById(20L).getStatus());
        assertTrue(scopes.selectList().isEmpty());assertTrue(recipients.selectList().isEmpty());assertTrue(jobs.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"AUDIT","STORAGE"})
    void lateFailurePersistenceRefusalKeepsTheAcceptedBatchRecoverableAndOtherCallbacksRun(String failure){
        when(source.resolve(any())).thenThrow(new IllegalStateException("task directory unavailable"));
        if("AUDIT".equals(failure))jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.fail'");
        else jdbc.execute("ALTER TABLE dcc_distribution_batch ADD CONSTRAINT ic31_fail_persist CHECK(status<>'FAILED')");
        try{var later=new AtomicBoolean();assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();registerLater(later);}));assertTrue(later.get());
            assertEquals("PENDING",batchRows.selectList().get(0).getStatus());assertEquals("ACTIVE",files.selectById(20L).getStatus());assertTrue(notifyRows.selectList().isEmpty());
            assertEquals(1,batchRows.recoverable(1L).size());
        }finally{if("STORAGE".equals(failure))jdbc.execute("ALTER TABLE dcc_distribution_batch DROP CONSTRAINT ic31_fail_persist");}
    }
    @Test void callbackRestoresCapturedTenantAndPreservesTheAmbientTenant(){
        readySource();var tenantSeenByLater=new java.util.concurrent.atomic.AtomicLong();
        tx.executeWithoutResult(s->{controlLocked();TenantContextHolder.setTenantId(2L);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){tenantSeenByLater.set(TenantContextHolder.getRequiredTenantId());}});});
        assertEquals(2L,tenantSeenByLater.get());assertEquals(2L,TenantContextHolder.getRequiredTenantId());TenantContextHolder.setTenantId(1L);
        assertEquals("SENT",batchRows.selectList().get(0).getStatus());assertEquals(2,notifyRows.selectList().size());
    }
    @Test void frozenRecoveryNeverCallsTodaysProviderOrCreatesDuplicatePlatformMessages(){
        readySource();control();var batch=batchRows.selectList().get(0);var before=jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id");
        doThrow(new AssertionError("must not reload a frozen roster")).when(source).resolve(any());
        assertEquals("SENT",batches.processAutomaticBatch(batch.getId()).status());assertEquals(before,jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id"));
        verify(source,times(1)).resolve(any());assertEquals(2,recipients.selectList().size());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void mismatchedOrMissingCallbackResultIsNotSuccessfulAndCannotBreakLaterCallback(boolean missing){
        doReturn(missing?null:new DccDistributionBatchResult(9999L,"SENT",null,null)).when(batches).processAutomaticBatch(anyLong());var later=new AtomicBoolean();
        assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();registerLater(later);}));assertTrue(later.get());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals("PENDING",batchRows.selectList().get(0).getStatus());assertTrue(notifyRows.selectList().isEmpty());
    }
    @Test void futureControlledVersionDoesNotResolveRosterOrCreateDispatchScope(){
        readySource();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate().plusDays(2));control();
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(20L).getStatus());assertNull(files.selectById(20L).getActivatedTime());
        assertTrue(batchRows.selectList().isEmpty());assertTrue(scopes.selectList().isEmpty());assertTrue(recipients.selectList().isEmpty());
        assertTrue(jobs.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());verifyNoInteractions(source);verify(batches,never()).processAutomaticBatch(anyLong());
    }
    @Test void missingControlledArtifactRollsBackBeforeAnyRosterOrCallback(){
        readySource();jdbc.update("UPDATE dcc_controlled_file SET published_file_id=NULL,stamped_file_id=NULL WHERE id=20");
        assertThrows(RuntimeException.class,this::control);assertNull(files.selectById(20L).getControlledTime());assertNull(files.selectById(20L).getActivatedTime());
        assertTrue(batchRows.selectList().isEmpty());assertTrue(scopes.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());
        verifyNoInteractions(source);verify(batches,never()).processAutomaticBatch(anyLong());
    }
    @Test void realRecipientPersistenceFailureRollsDispatchBackWithoutUndoingActivation(){
        readySource();jdbc.execute("ALTER TABLE dcc_controlled_file_distribution_recipient ADD CONSTRAINT ic31_recipient_persist CHECK(user_id<>100)");
        try{var later=new AtomicBoolean();assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();registerLater(later);}));assertTrue(later.get());
            assertEquals("FAILED",batchRows.selectList().get(0).getStatus());assertEquals("DISPATCH_PREPARE_FAILED",batchRows.selectList().get(0).getErrorCode());
            assertEquals("ACTIVE",files.selectById(20L).getStatus());assertTrue(scopes.selectList().isEmpty());assertTrue(recipients.selectList().isEmpty());
            assertTrue(jobs.selectList().isEmpty());assertTrue(notifyRows.selectList().isEmpty());
        }finally{jdbc.execute("ALTER TABLE dcc_controlled_file_distribution_recipient DROP CONSTRAINT ic31_recipient_persist");}
    }
    private void readySource(){when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.Ready(
            new DccDistributionRosterSnapshot(1L,20L,"B/1","round-1",now,"IC31-TASK-ONLY-POLICY",
                    List.of(new DccDistributionRosterSnapshot.Department(31L,List.of(99L,100L)))),"IC31_TASK_ONLY_SOURCE","IC31_TASK_ONLY_POLICY_EVIDENCE"));}
    private void registerLater(AtomicBoolean later){TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){later.set(true);}});}
}
