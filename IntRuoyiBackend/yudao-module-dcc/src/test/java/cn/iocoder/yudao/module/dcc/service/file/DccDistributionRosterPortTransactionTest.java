package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Inherits the existing real H2 source-event/Mapper/audit fixture; source and platform are explicit task-only ports. */
class DccDistributionRosterPortTransactionTest extends DccDistributionOutboxTransactionTest {
    ObjectProvider<DccDistributionRosterSource> providers;
    DccDistributionRosterSource source;
    DccControlledFileMessageDeliveryService realDelivery;
    Map<String,Long> platform=Collections.synchronizedMap(new LinkedHashMap<>());
    @BeforeEach void sourceFixture() {
        providers=mock(ObjectProvider.class);source=mock(DccDistributionRosterSource.class);
        var frozenAccounts=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        lenient().when(frozenAccounts.getUserList(any())).thenAnswer(call->{java.util.Collection<Long> ids=call.getArgument(0);
            return ids.stream().map(id->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0)).toList();});
        wire(batchService,"adminUserApi",frozenAccounts);
        lenient().when(providers.orderedStream()).thenAnswer(ignored->Stream.of(source));
        lenient().when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.Ready(roster(),"TASK_FIXTURE_FORMAL_SOURCE","TASK_FIXTURE_APPROVED_POLICY_REFERENCE"));
        // Wire only after the RED phase has proved that the actual production entry does not exist.
        if(Arrays.stream(batchService.getClass().getDeclaredFields()).anyMatch(field->field.getName().equals("rosterSources")))
            ReflectionTestUtils.setField(batchService,"rosterSources",providers);
        var send=mock(NotifyMessageSendApi.class);var read=mock(NotifyMessageService.class);
        when(send.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(call->{var input=call.getArgument(0,NotifySendSingleToUserIdempotentReqDTO.class);return platform.computeIfAbsent(input.getBusinessKey(),key->901L+platform.size());});
        lenient().when(read.getNotifyMessage(anyLong())).thenAnswer(call->{Long id=call.getArgument(0);var entry=platform.entrySet().stream().filter(item->item.getValue().equals(id)).findFirst().orElseThrow();
            Long jobId=Long.valueOf(entry.getKey().substring(entry.getKey().lastIndexOf(':')+1));var job=jobs.selectById(jobId);
            var message=NotifyMessageDO.builder().id(id).tenantId(1L).userType(2).userId(job.getRecipientUserId()).templateCode(job.getTemplateCode()).businessKey(entry.getKey()).build();message.setCreateTime(time);return message;});
        var delivery=new DccControlledFileMessageDeliveryService();wire(delivery,"messageJobMapper",jobs,"notifyMessageSendApi",send,"notifyMessageService",read,
                "controlledFileMapper",files,"distributionMapper",distributions,"distributionRecipientMapper",recipients,"transactionManager",tx,"dispatchAttemptService",attemptService);
        realDelivery=delivery;
        if(Arrays.stream(batchService.getClass().getDeclaredFields()).anyMatch(field->field.getName().equals("messageDeliveryService")))wire(batchService,"messageDeliveryService",delivery);
    }
    DccDistributionBatchResult automatic(Long id)throws Exception {
        // Parent fixture scenarios keep their original explicit pending-outbox behavior.
        wire(distributionService,"messageDeliveryService",realDelivery);
        var method=assertDoesNotThrow(()->batchService.getClass().getMethod("processAutomaticBatch",Long.class),"IC31 actual automatic roster entry must exist");
        try{return(DccDistributionBatchResult)method.invoke(batchService,id);}catch(java.lang.reflect.InvocationTargetException error){if(error.getCause() instanceof RuntimeException runtime)throw runtime;throw error;}
    }
    @Test void automaticEntryUsesServerExactRequestAndReallyDispatches()throws Exception {
        var id=accept();var result=automatic(id);assertEquals("SENT",result.status());assertEquals(2L,jobs.selectCount());assertEquals(2L,recipients.selectCount());assertEquals(2,platform.size());
        var capture=org.mockito.ArgumentCaptor.forClass(DccDistributionRosterSource.Request.class);verify(source).resolve(capture.capture());
        assertEquals(List.of("61"),capture.getValue().savedDepartmentIds());assertEquals(42L,capture.getValue().controlledFileId());assertEquals("round-42",capture.getValue().approvalProcessInstanceId());
        assertEquals(time,files.selectById(42L).getActivatedTime());assertEquals("ACTIVE",files.selectById(42L).getStatus());
        assertTrue(recipients.selectList().stream().allMatch(row->row.getAcknowledgedAt()==null && row.getReadAt()==null));
    }
    @Test void missingSourceOrPolicyNotReadyPersistTruthfulFailure()throws Exception {
        var id=accept();when(providers.orderedStream()).thenAnswer(ignored->Stream.empty());assertEquals("ROSTER_SOURCE_MISSING",automatic(id).errorCode());
        assertEquals(0L,jobs.selectCount());assertEquals(0L,recipients.selectCount());assertEquals(0,platform.size());
        when(providers.orderedStream()).thenAnswer(ignored->Stream.of(source));when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.NotReady("ROSTER_POLICY_NOT_READY","尚无正式政策"));
        assertEquals("ROSTER_POLICY_NOT_READY",automatic(id).errorCode());assertEquals("FAILED",batches.selectById(id).getStatus());assertEquals(time,files.selectById(42L).getActivatedTime());
    }
    @Test void technicalSourceFailurePersistsAndPropagatesInsteadOfEmptyRosterSuccess()throws Exception {
        var id=accept();when(source.resolve(any())).thenThrow(new IllegalStateException("formal directory unavailable"));
        assertThrows(IllegalStateException.class,()->automatic(id));assertEquals("ROSTER_SOURCE_FAILED",batches.selectById(id).getErrorCode());assertEquals(0L,jobs.selectCount());assertEquals(0,platform.size());
    }
    @Test void frozenBatchRetryNeverResolvesTodaysRosterAndReusesPlatformIdentity()throws Exception {
        var id=accept();automatic(id);var original=batches.selectById(id).getSnapshotPayloadHash();var receiptIds=recipients.selectList().stream().map(DccControlledFileDistributionRecipientDO::getId).toList();
        var job=jobs.selectList().get(0);jdbc.update("UPDATE dcc_controlled_file_message_job SET status='FAILED',sent_at=NULL,error_message='lost acknowledgement' WHERE id=?",job.getId());jdbc.update("UPDATE dcc_distribution_batch SET status='FAILED' WHERE id=?",id);
        when(source.resolve(any())).thenThrow(new AssertionError("must never resolve changed current roster"));
        assertEquals("SENT",automatic(id).status());verify(source,times(1)).resolve(any());assertEquals(2,platform.size());assertEquals(original,batches.selectById(id).getSnapshotPayloadHash());
        assertEquals(receiptIds,recipients.selectList().stream().map(DccControlledFileDistributionRecipientDO::getId).toList());assertEquals(time,jobs.selectById(job.getId()).getSentAt());
    }
    @Test void sentReplayReturnsOriginalFactWithoutProviderOrNewWrites()throws Exception {var id=accept();automatic(id);long count=audits.selectCount();automatic(id);verify(source,times(1)).resolve(any());assertEquals(count,audits.selectCount());assertEquals(2L,attempts.selectCount());}
    @Test void invalidProviderSnapshotFailsBeforeAnyObligations()throws Exception {
        var id=accept();when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.Ready(new DccDistributionRosterSnapshot(1L,43L,"B/1","round-42",time,"formal-fixture-policy",roster().departments()),"fixture","policy"));
        assertEquals("ROSTER_SNAPSHOT_INVALID",automatic(id).errorCode());assertEquals(0L,jobs.selectCount());assertEquals(0L,recipients.selectCount());
    }
    @Test void concurrentAutomaticCallbacksFreezeOnlyOnce()throws Exception {
        var id=accept();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);
        try{var action=(java.util.concurrent.Callable<DccDistributionBatchResult>)()->{cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);try{start.await();return automatic(id);}finally{cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}};
            var first=pool.submit(action);var second=pool.submit(action);start.countDown();first.get(20,java.util.concurrent.TimeUnit.SECONDS);second.get(20,java.util.concurrent.TimeUnit.SECONDS);
            verify(source,times(1)).resolve(any());assertEquals(2L,jobs.selectCount());assertEquals(2L,recipients.selectCount());assertEquals(2,platform.size());
        }finally{pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);}
    }
    @Test void realHumanRetryAndNotReadyAuditCommitTogetherWhileSourceValuesRemainServerOwned()throws Exception {
        var policy=new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setPolicyVersion("IC31_MEMORY_POLICY");
        policy.setOperationId("dcc.distribution.batch.retry");policy.setSourceType("SERVICE_METHOD");policy.setSourceLocator("cn.iocoder.yudao.module.dcc.service.file.DccDistributionTaskServiceImpl#retryAutomaticDistribution");
        policy.setDomain("DCC");policy.setSubjectType("DCC_DISTRIBUTION_BATCH");policy.setActionType("UPDATE");policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");policy.setSignaturePolicy("NOT_REQUIRED");
        policy.setStatePolicy("PRESENT_TO_PRESENT");policy.setRetentionClass("GXP_CONTROLLED_DOCUMENT");policy.setTestIds("IC31");policy.setOwner("TASK_FIXTURE");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
        var accountApi=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        when(accountApi.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setUsername("dcc-operator").setNickname("真实文控测试操作者"));
        var humanAudit=new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl();wire(humanAudit,"auditEventMapper",audits,"ledgerSequenceMapper",sequences,"policyOperationMapper",policies,"adminUserApi",accountApi);
        wire(batchService,"gxpAuditService",humanAudit);
        var actor=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor,null));
        var id=accept();when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.NotReady("ROSTER_POLICY_NOT_READY","正式政策尚未配置"));
        assertEquals("FAILED",batchService.retryAutomaticBatch(id,99L).status());assertEquals(0L,jobs.selectCount());assertEquals(0L,recipients.selectCount());
        var command=audits.selectList().stream().filter(row->row.getOperationId().equals("dcc.distribution.batch.retry")).findFirst().orElseThrow();assertEquals(99L,command.getActorId());
        assertTrue(audits.selectList().stream().filter(row->!row.getOperationId().equals("dcc.distribution.batch.retry")).allMatch(row->row.getActorId()==-1L));
        assertSame(actor,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser());
    }
    @Test void missingHumanRetryPolicyRollsBackNewRosterAndJobsAndRecordsOnlyTechnicalFailure()throws Exception {
        var actor=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor,null));
        var id=accept();wire(distributionService,"messageDeliveryService",realDelivery);
        assertThrows(RuntimeException.class,()->batchService.retryAutomaticBatch(id,99L));
        assertEquals(0L,jobs.selectCount());assertEquals(0L,recipients.selectCount());assertEquals(0,platform.size());assertNull(batches.selectById(id).getSnapshotPayloadHash());assertEquals("FAILED",batches.selectById(id).getStatus());
    }
    @Test void damagedFrozenRosterCannotFetchFreshProviderAndCreateReplacementObligations()throws Exception {
        var id=accept();automatic(id);var recipient=recipients.selectList().get(0);jdbc.update("UPDATE dcc_controlled_file_distribution_recipient SET user_id=999 WHERE id=?",recipient.getId());
        assertThrows(RuntimeException.class,()->automatic(id));verify(source,times(1)).resolve(any());assertEquals(2L,jobs.selectCount());assertEquals(2L,recipients.selectCount());assertEquals(2,platform.size());
    }
    @Test void multipleSourcesAndMalformedNotReadyCannotBeApprovedByChoosingFirstBean()throws Exception {
        var id=accept();when(providers.orderedStream()).thenAnswer(ignored->Stream.of(source,source));assertEquals("ROSTER_SOURCE_AMBIGUOUS",automatic(id).errorCode());verifyNoInteractions(source);
        when(providers.orderedStream()).thenAnswer(ignored->Stream.of(source));when(source.resolve(any())).thenReturn(new DccDistributionRosterSource.NotReady("",null));
        assertEquals("ROSTER_SOURCE_INVALID",automatic(id).errorCode());assertEquals(0L,jobs.selectCount());assertEquals(0,platform.size());
    }
    @Test void formalSourceFailureWithoutRuntimeFailPolicyPropagatesAndNeverClaimsDurableFailed()throws Exception {
        var id=accept();when(providers.orderedStream()).thenAnswer(ignored->Stream.empty());jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.fail'");
        assertThrows(RuntimeException.class,()->automatic(id));assertEquals("PENDING",batches.selectById(id).getStatus());assertNull(batches.selectById(id).getSnapshotPayloadHash());assertEquals(0L,jobs.selectCount());
    }
}
