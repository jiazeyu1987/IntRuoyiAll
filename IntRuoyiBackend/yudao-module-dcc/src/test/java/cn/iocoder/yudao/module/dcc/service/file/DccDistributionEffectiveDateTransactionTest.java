package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageService;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.NOTIFY_SEND_TEMPLATE_PARAM_MISS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Real DCC outbox/attempt/audit transactions in the parent's private H2 fixture.
 * The platform port runs the real mandatory-parameter validator; its delivery evidence is isolated.
 * This is not a platform notification integration test or a browser E2E test.
 */
class DccDistributionEffectiveDateTransactionTest extends DccDistributionOutboxTransactionTest {

    private static final LocalDateTime ACTUAL_EFFECTIVE_TIME = LocalDateTime.of(2026, 10, 10, 23, 59, 58);
    private static final LocalDateTime SELECTED_EFFECTIVE_TIME = LocalDateTime.of(2026, 10, 9, 22, 0);
    private static final LocalDate PRESET_DATE = LocalDate.of(2026, 10, 8);

    private NotifyMessageSendApi platform;
    private NotifyMessageService platformRead;
    private DccControlledFileMessageDeliveryService delivery;
    private DccControlledFileMessageJobDO target;

    @ParameterizedTest
    @CsvSource({
            "AFTER_STANDARDIZATION_SIGNATURE,",
            "AFTER_STANDARDIZATION_SIGNATURE,2026-10-08",
            "DELAYED,",
            "DELAYED,2026-10-08"
    })
    void newModesUseActualEffectiveDateOnlyAfterBusinessCommit(String mode, String presetDate) {
        prepare(mode, presetDate, ACTUAL_EFFECTIVE_TIME);

        new TransactionTemplate(tx).executeWithoutResult(status -> {
            // Caller parameters cannot override the exact committed distribution facts.
            delivery.dispatchMessageJob(target, Map.of("effectiveDate", "2099-12-31"));
            verifyNoInteractions(platform, platformRead);
            assertEquals("PENDING", jobs.selectById(target.getId()).getStatus());
        });

        assertSentWithDate(ACTUAL_EFFECTIVE_TIME.toLocalDate().toString());
        assertEquals(presetDate == null ? null : LocalDate.parse(presetDate), files.selectById(42L).getEffectiveDate());
        assertEquals(ACTUAL_EFFECTIVE_TIME, files.selectById(42L).getEffectiveTime());
        assertEquals(1L, attempts.selectCount());
        assertEquals("SENT", attempts.selectList().get(0).getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "AFTER_STANDARDIZATION_SIGNATURE,",
            "AFTER_STANDARDIZATION_SIGNATURE,2026-10-08",
            "DELAYED,",
            "DELAYED,2026-10-08"
    })
    void newModesWithoutActualFactFailBeforeAnyPlatformCall(String mode, String presetDate) {
        // activated_time remains present, and DELAYED has its distinct selected time.
        prepare(mode, presetDate, null);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> delivery.replayMessageJob(target));

        var failed = jobs.selectById(target.getId());
        assertAll(
                () -> verifyNoInteractions(platform, platformRead),
                () -> assertEquals("FAILED", failed.getStatus()),
                () -> assertNotNull(failed.getErrorMessage()),
                () -> assertFalse(failed.getErrorMessage().isBlank()),
                () -> assertFalse(failure.getMessage().contains("模板参数"), "DCC must reject missing facts before template delivery"),
                () -> assertNull(failed.getSentAt()),
                () -> assertNull(files.selectById(42L).getEffectiveTime())
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "PRESET_DATE")
    void presetAndLegacyKeepTheirExistingEffectiveDate(String mode) {
        prepare(mode, PRESET_DATE.toString(), ACTUAL_EFFECTIVE_TIME);

        delivery.replayMessageJob(target);

        assertSentWithDate(PRESET_DATE.toString());
    }

    @Test
    void rolledBackBusinessTransactionNeverDispatchesThePreparedJob() {
        prepare("AFTER_STANDARDIZATION_SIGNATURE", null, ACTUAL_EFFECTIVE_TIME);

        new TransactionTemplate(tx).executeWithoutResult(status -> {
            delivery.dispatchMessageJob(target, Map.of());
            status.setRollbackOnly();
        });

        verifyNoInteractions(platform, platformRead);
        assertEquals("PENDING", jobs.selectById(target.getId()).getStatus());
        assertNull(jobs.selectById(target.getId()).getSentAt());
        assertEquals(0L, attempts.selectCount());
    }

    @Test
    void failedJobReplayReadsTheNowCommittedActualFactWithTheSameJobIdentity() {
        prepare("DELAYED", null, null);
        Long originalJobId = target.getId();
        Long originalDistributionId = target.getBusinessId();
        Long originalBatchId = target.getDistributionBatchId();
        assertThrows(RuntimeException.class, () -> delivery.replayMessageJob(target));
        verifyNoInteractions(platform, platformRead);
        assertEquals("FAILED", jobs.selectById(originalJobId).getStatus());

        // Explicit private-fixture setup, never a runtime data repair or accepted UI action.
        new TransactionTemplate(tx).executeWithoutResult(status -> assertEquals(1, jdbc.update(
                "UPDATE dcc_controlled_file SET effective_time=? WHERE id=42", ACTUAL_EFFECTIVE_TIME)));
        delivery.replayMessageJob(target);
        // Even a stale caller must not cause another platform call after durable SENT.
        delivery.replayMessageJob(target);

        assertSentWithDate(ACTUAL_EFFECTIVE_TIME.toLocalDate().toString());
        var stored = jobs.selectById(originalJobId);
        assertEquals(originalDistributionId, stored.getBusinessId());
        assertEquals(originalBatchId, stored.getDistributionBatchId());
        assertEquals(2L, jobs.selectCount(), "the original two recipient obligations are preserved");
        assertEquals(2L, recipients.selectCount());
    }

    @Test
    void formalTemplateStillRequiresEffectiveDate() {
        ServiceException failure = assertThrows(ServiceException.class, () -> new NotifySendServiceImpl()
                .validateTemplateParams(distributionTemplate(), Map.of("title", "SOP", "version", "B/1")));
        assertEquals(NOTIFY_SEND_TEMPLATE_PARAM_MISS.getCode(), failure.getCode());
        assertTrue(failure.getMessage().contains("effectiveDate"));
    }

    private void prepare(String mode, String presetDate, LocalDateTime actualTime) {
        assertEquals(1, jdbc.update("""
                UPDATE dcc_controlled_file SET effective_mode=?,effective_date=?,effective_time=?,
                  delayed_effective_time=?,controlled_time=? WHERE id=42
                """, mode, presetDate == null ? null : LocalDate.parse(presetDate), actualTime,
                "DELAYED".equals(mode) ? SELECTED_EFFECTIVE_TIME : null,
                "DELAYED".equals(mode) ? time.minusDays(2) : ACTUAL_EFFECTIVE_TIME.plusSeconds(1)));
        Long batchId = accept();
        assertEquals("DISPATCH_READY", batchService.prepareAndDispatchBatch(batchId, roster(), true).status());
        target = jobs.selectList().stream().filter(row -> row.getRecipientUserId().equals(99L)).findFirst().orElseThrow();

        platform = mock(NotifyMessageSendApi.class);
        platformRead = mock(NotifyMessageService.class);
        var mandatoryValidator = new NotifySendServiceImpl();
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(call -> {
            var request = call.getArgument(0, NotifySendSingleToUserIdempotentReqDTO.class);
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "platform call must be outside the DCC transaction");
            assertEquals("PENDING", jobs.selectById(target.getId()).getStatus());
            assertEquals(1L, attempts.selectCount());
            assertEquals("STARTED", attempts.selectList().get(0).getStatus(), "attempt identity must already be committed");
            mandatoryValidator.validateTemplateParams(distributionTemplate(), request.getTemplateParams());
            return 901L;
        });
        var evidence = NotifyMessageDO.builder().id(901L).tenantId(1L).userId(target.getRecipientUserId()).userType(2)
                .templateCode("dcc_distribution").businessKey("DCC_MESSAGE_JOB:1:" + target.getId()).build();
        evidence.setCreateTime(time.plusMinutes(1));
        when(platformRead.getNotifyMessage(901L)).thenReturn(evidence);
        delivery = new DccControlledFileMessageDeliveryService();
        wire(delivery, "messageJobMapper", jobs, "notifyMessageSendApi", platform, "notifyMessageService", platformRead,
                "controlledFileMapper", files, "distributionMapper", distributions, "distributionRecipientMapper", recipients,
                "transactionManager", tx, "dispatchAttemptService", attemptService);
    }

    private static NotifyTemplateDO distributionTemplate() {
        // Exact required params in sql/mysql/20260513_dcc_notify_template_seed.sql; template is not weakened.
        return NotifyTemplateDO.builder().code("dcc_distribution").params(List.of("title", "version", "effectiveDate")).build();
    }

    private void assertSentWithDate(String expectedDate) {
        var request = ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(platform).sendSingleMessageIdempotentlyToAdmin(request.capture());
        assertEquals(expectedDate, request.getValue().getTemplateParams().get("effectiveDate"));
        assertEquals("DCC_MESSAGE_JOB:1:" + target.getId(), request.getValue().getBusinessKey());
        assertEquals("42", request.getValue().getTemplateParams().get("controlledFileId"));
        var stored = jobs.selectById(target.getId());
        assertEquals("SENT", stored.getStatus());
        assertEquals(time.plusMinutes(1), stored.getSentAt());
        assertTrue(recipients.selectList().stream().allMatch(row -> row.getReadAt() == null && row.getAcknowledgedAt() == null));
    }
}
