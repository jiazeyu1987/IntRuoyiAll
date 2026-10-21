package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.service.notify.*;
import jakarta.annotation.Resource;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** H2 proves transaction-boundary placement, not MySQL REPEATABLE READ snapshot behavior. */
class DccDistributionPlatformEvidenceTransactionTest extends DccDistributionOutboxTransactionTest {
    private static final long TEMPLATE_ID = 690015L;
    @Resource NotifyMessageMapper platformMessages;
    @Resource NotifyTemplateMapper platformTemplates;
    private final List<ReadObservation> reads = new ArrayList<>();
    private DccControlledFileMessageDeliveryService delivery;
    private Object outerConnectionHolder;
    private String corruptEvidence;
    private int sendCalls;

    @AfterEach void cleanPrivatePlatformRows() {
        jdbc.update("DELETE FROM system_notify_message WHERE template_id=?", TEMPLATE_ID);
        jdbc.update("DELETE FROM system_notify_template WHERE id=?", TEMPLATE_ID);
    }

    @Test void afterCommitTwoRecipientsReadRealPlatformEvidenceOutsideTheOldDccResources() {
        Long batchId = accept();
        // The parent fixture isolates delivery during prepare's own REQUIRES_NEW commit.
        batchService.prepareAndDispatchBatch(batchId, roster(), true);
        var pending = jobs.selectList();
        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(job -> "PENDING".equals(job.getStatus())));
        assertEquals(0L, platformMessages.selectCount());
        wireRealPlatform(jobs);
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            outerConnectionHolder = TransactionSynchronizationManager.getResource(dataSource);
            assertNotNull(outerConnectionHolder);
            for (var job : pending) delivery.dispatchMessageJob(job, Map.of());
            assertEquals(0, sendCalls, "business commit must precede all platform sends");
        });

        assertEquals(2, reads.size(), "both formal recipient callbacks must reach the real mapper read");
        assertTrue(reads.stream().allMatch(read -> read.message() != null), "real platform messages must be readable");
        assertAll(
                () -> assertTrue(reads.stream().noneMatch(ReadObservation::activeTransaction),
                        "platform evidence reads must remain inside NOT_SUPPORTED, including afterCommit"),
                () -> assertTrue(reads.stream().noneMatch(ReadObservation::oldConnectionBound),
                        "evidence reads must not resume the committed DCC connection"),
                () -> assertAllSentAndExact(batchId)
        );
        for (var job : jobs.selectList()) delivery.replayMessageJob(job);
        assertEquals(2, sendCalls, "durably SENT jobs must never call platform send again");
        assertEquals(2L, platformMessages.selectCount());
    }

    @Test void realPlatformCommitThenLostDccAckReplaysTheOriginalMessageAndTimestamp() {
        var failedOnce = new AtomicBoolean();
        var failingJobs = mock(DccControlledFileMessageJobMapper.class, call -> {
            if (call.getMethod().getName().equals("updateById")
                    && call.getArgument(0) instanceof DccControlledFileMessageJobDO update
                    && "SENT".equals(update.getStatus()) && failedOnce.compareAndSet(false, true))
                throw new IllegalStateException("private fixture: late DCC ACK lost");
            try { return call.getMethod().invoke(jobs, call.getArguments()); }
            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
        });
        wireRealPlatform(failingJobs);
        Long batchId = accept();
        batchService.prepareAndDispatchBatch(batchId, roster(), true);
        var failed = jobs.selectList().stream().filter(row -> "FAILED".equals(row.getStatus())).findFirst().orElseThrow();
        assertEquals(2L, platformMessages.selectCount());
        var original = platformMessages.selectByBusinessKey(1L, businessKey(failed));
        assertNotNull(original);
        assertTrue(failed.getErrorMessage().contains("late DCC ACK lost"));

        delivery.replayMessageJob(failed);

        var recovered = platformMessages.selectByBusinessKey(1L, businessKey(failed));
        assertEquals(original.getId(), recovered.getId());
        assertEquals(original.getCreateTime(), recovered.getCreateTime());
        assertEquals(original.getTemplateParams(), recovered.getTemplateParams());
        assertEquals(2L, platformMessages.selectCount());
        assertEquals(3L, attempts.selectCount());
        assertAllSentAndExact(batchId);
        delivery.replayMessageJob(failed);
        assertEquals(3, sendCalls, "one failed ACK replay, no replay of SENT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "TENANT", "RECIPIENT", "USER_TYPE", "TEMPLATE", "BUSINESS_KEY"})
    void actualDatabaseMissingOrMismatchedEvidenceFailsOneJobAndContinuesTheOtherRecipient(String mismatch) {
        wireRealPlatform(jobs);
        corruptEvidence = mismatch;
        Long batchId = accept();
        batchService.prepareAndDispatchBatch(batchId, roster(), true);

        var all = jobs.selectList();
        assertEquals(2, all.size());
        var failed = all.stream().filter(row -> "FAILED".equals(row.getStatus())).findFirst().orElseThrow();
        assertTrue(failed.getErrorMessage().contains("platform send evidence is missing or mismatched"));
        assertNull(failed.getSentAt());
        assertEquals(1L, all.stream().filter(row -> "SENT".equals(row.getStatus())).count());
        assertEquals(2, sendCalls, "a failed recipient callback must not block the next recipient");
        assertEquals(2, reads.size(), "getNotifyMessage was not replaced by a fixture response");
        assertEquals(1L, attempts.selectList().stream().filter(row -> "FAILED".equals(row.getStatus())).count());
        assertEquals(1L, attempts.selectList().stream().filter(row -> "SENT".equals(row.getStatus())).count());
        assertEquals("FAILED", batches.selectById(batchId).getStatus());
        assertTrue(recipients.selectList().stream().allMatch(row -> row.getReadAt() == null && row.getAcknowledgedAt() == null));
    }

    private void wireRealPlatform(DccControlledFileMessageJobMapper deliveryJobs) {
        var template = NotifyTemplateDO.builder().id(TEMPLATE_ID).name("private distribution fixture")
                .code("dcc_distribution").type(2).nickname("private DCC")
                .content("受控文件《{title}》版本 {version} 已正式下发，请及时查阅。")
                .params(List.of("title", "version", "effectiveDate")).status(0).build();
        platformTemplates.insert(template);
        var templates = new NotifyTemplateServiceImpl();
        wire(templates, "notifyTemplateMapper", platformTemplates);
        var messages = new NotifyMessageServiceImpl();
        wire(messages, "notifyMessageMapper", platformMessages);
        var send = new NotifySendServiceImpl();
        wire(send, "notifyTemplateService", templates, "notifyMessageService", messages);
        var transactionalSend = new ProxyFactory(send);
        transactionalSend.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        var api = new NotifyMessageSendApiImpl();
        wire(api, "notifySendService", (NotifySendService) transactionalSend.getProxy());
        var observedApi = new ProxyFactory(api);
        observedApi.addAdvice((MethodInterceptor) invocation -> {
            if (!invocation.getMethod().getName().equals("sendSingleMessageIdempotentlyToAdmin"))
                return invocation.proceed();
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "platform send must be suspended");
            sendCalls++;
            Long id = (Long) invocation.proceed(); // Actual platform service transaction has committed.
            if (sendCalls == 1 && corruptEvidence != null) corruptPrivateRow(id, corruptEvidence);
            return id;
        });
        var observedReader = new ProxyFactory(messages);
        observedReader.addAdvice((MethodInterceptor) invocation -> {
            if (!invocation.getMethod().getName().equals("getNotifyMessage")) return invocation.proceed();
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            boolean oldBound = outerConnectionHolder != null
                    && TransactionSynchronizationManager.getResource(dataSource) == outerConnectionHolder;
            var message = (NotifyMessageDO) invocation.proceed(); // Never stubbed: real selectById.
            reads.add(new ReadObservation(active, oldBound, message));
            return message;
        });
        delivery = new DccControlledFileMessageDeliveryService();
        wire(delivery, "messageJobMapper", deliveryJobs, "controlledFileMapper", files,
                "distributionMapper", distributions, "distributionRecipientMapper", recipients,
                "notifyMessageSendApi", observedApi.getProxy(), "notifyMessageService", observedReader.getProxy(),
                "transactionManager", tx, "dispatchAttemptService", attemptService);
        wire(distributionService, "messageDeliveryService", delivery);
    }

    private void corruptPrivateRow(Long id, String mismatch) {
        // Fault injection touches only freshly created rows in the asserted private H2 database.
        String sql = switch (mismatch) {
            case "MISSING" -> "DELETE FROM system_notify_message WHERE id=?";
            case "TENANT" -> "UPDATE system_notify_message SET tenant_id=2 WHERE id=?";
            case "RECIPIENT" -> "UPDATE system_notify_message SET user_id=999 WHERE id=?";
            case "USER_TYPE" -> "UPDATE system_notify_message SET user_type='1' WHERE id=?";
            case "TEMPLATE" -> "UPDATE system_notify_message SET template_code='private-other' WHERE id=?";
            case "BUSINESS_KEY" -> "UPDATE system_notify_message SET business_key='private-other-key' WHERE id=?";
            default -> throw new IllegalArgumentException(mismatch);
        };
        assertEquals(1, jdbc.update(sql, id));
    }

    private void assertAllSentAndExact(Long batchId) {
        assertEquals("SENT", batches.selectById(batchId).getStatus());
        assertEquals(2L, jobs.selectCount());
        assertEquals(2L, recipients.selectCount());
        for (var job : jobs.selectList()) {
            assertEquals("SENT", job.getStatus());
            var message = platformMessages.selectByBusinessKey(1L, businessKey(job));
            assertNotNull(message);
            assertEquals(job.getRecipientUserId(), message.getUserId());
            assertEquals(2, message.getUserType());
            assertEquals(job.getTemplateCode(), message.getTemplateCode());
            assertNotNull(message.getCreateTime());
            assertEquals(message.getCreateTime(), job.getSentAt());
            assertEquals("42", message.getTemplateParams().get("controlledFileId"));
            assertEquals("2026-10-11", message.getTemplateParams().get("effectiveDate"));
        }
        assertTrue(recipients.selectList().stream().allMatch(row -> row.getReadAt() == null && row.getAcknowledgedAt() == null));
    }

    private static String businessKey(DccControlledFileMessageJobDO job) { return "DCC_MESSAGE_JOB:1:" + job.getId(); }
    private record ReadObservation(boolean activeTransaction, boolean oldConnectionBound, NotifyMessageDO message) {}
}
