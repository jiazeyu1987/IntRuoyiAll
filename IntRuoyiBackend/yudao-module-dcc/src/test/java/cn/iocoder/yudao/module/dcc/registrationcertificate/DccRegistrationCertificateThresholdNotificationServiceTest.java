package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold.DccRegistrationCertificateThresholdDeliveryResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold.DccRegistrationCertificateThresholdNotificationService;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateThresholdNotificationService.class,
        DccRegistrationCertificateThresholdNotificationServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateThresholdNotificationServiceTest extends BaseDbUnitTest {

    private static final String TEMPLATE = "DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER";

    @Resource
    private DccRegistrationCertificateThresholdNotificationService service;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @BeforeEach
    void resetMocks() {
        reset(notifyMessageSendApi);
    }

    @Test
    void sendDeliveryUsesStableBusinessKeyAndReplayReturnsSameMessageId() {
        insertPendingDelivery(1L, 1001L, 2001L, 3001L, "DELIVERY-KEY-1");
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(99001L);

        DccRegistrationCertificateThresholdDeliveryResult sent = service.sendDelivery(1L, 3001L);
        DccRegistrationCertificateThresholdDeliveryResult replay = service.sendDelivery(1L, 3001L);

        assertEquals("SENT", sent.status());
        assertEquals(99001L, sent.notifyMessageId());
        assertEquals(99001L, replay.notifyMessageId());
        assertEquals("SENT", deliveryStatus(3001L));
        verify(notifyMessageSendApi, times(1)).sendSingleMessageIdempotentlyToAdmin(any());

        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> captor = ArgumentCaptor.forClass(
                NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageIdempotentlyToAdmin(captor.capture());
        assertEquals(7001L, captor.getValue().getUserId());
        assertEquals(TEMPLATE, captor.getValue().getTemplateCode());
        assertEquals("DELIVERY-KEY-1", captor.getValue().getBusinessKey());
        assertEquals(Map.of(
                "certificateId", 1001L,
                "thresholdLevel", "T_8",
                "dueDate", "2026-03-31",
                "businessDate", "2026-03-23"), captor.getValue().getTemplateParams());
    }

    @Test
    void ackGapReplayUsesSameBusinessKeyAndCanMarkSent() {
        insertPendingDelivery(1L, 1002L, 2002L, 3002L, "DELIVERY-KEY-ACK");
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(99002L);

        DccRegistrationCertificateThresholdDeliveryResult replay = service.sendDelivery(1L, 3002L);

        assertEquals("SENT", replay.status());
        assertEquals(99002L, replay.notifyMessageId());
        assertEquals(Long.valueOf(99002L), jdbcTemplate.queryForObject("""
                SELECT notify_message_id
                  FROM dcc_registration_certificate_reminder_delivery
                 WHERE id = 3002
                """, Long.class));
    }

    @Test
    void emptyMessageIdFailsAndNeverMarksSent() {
        insertPendingDelivery(1L, 1003L, 2003L, 3003L, "DELIVERY-KEY-EMPTY");
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.sendDelivery(1L, 3003L));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED.getCode(), exception.getCode());
        assertEquals("FAILED", deliveryStatus(3003L));
        assertEquals(null, jdbcTemplate.queryForObject("""
                SELECT notify_message_id
                  FROM dcc_registration_certificate_reminder_delivery
                 WHERE id = 3003
                """, Long.class));
    }

    @Test
    void platformFailureFailsDeliveryAndNeverMarksSent() {
        insertPendingDelivery(1L, 1004L, 2004L, 3004L, "DELIVERY-KEY-FAIL");
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any()))
                .thenThrow(new IllegalStateException("template disabled"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.sendDelivery(1L, 3004L));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED.getCode(), exception.getCode());
        assertEquals("FAILED", deliveryStatus(3004L));
        assertNotNull(jdbcTemplate.queryForObject("""
                SELECT last_failure_reason
                  FROM dcc_registration_certificate_reminder_delivery
                 WHERE id = 3004
                """, String.class));
    }

    private void insertPendingDelivery(Long tenantId, Long certificateId, Long occurrenceId,
                                       Long deliveryId, String deliveryKey) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_reminder_occurrence
                  (id, tenant_id, run_id, owner_company_id, certificate_id, version_id,
                   reminder_type, threshold_level, business_date, due_date, event_key,
                   status, detail_json)
                VALUES (?, ?, 9001, 501, ?, 8001, 'CERTIFICATE_EXPIRY', 'T_8',
                        DATE '2026-03-23', DATE '2026-03-31', ?, 'PENDING_DELIVERY', '{}')
                """, occurrenceId, tenantId, certificateId, "OCC-" + deliveryKey);
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_reminder_delivery
                  (id, tenant_id, occurrence_id, recipient_user_id, recipient_company_id,
                   delivery_key, status, attempt_count, detail_json)
                VALUES (?, ?, ?, 7001, 501, ?, 'PENDING', 0, '{}')
                """, deliveryId, tenantId, occurrenceId, deliveryKey);
    }

    private String deliveryStatus(Long deliveryId) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                  FROM dcc_registration_certificate_reminder_delivery
                 WHERE id = ?
                """, String.class, deliveryId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        NotifyMessageSendApi notifyMessageSendApi() {
            return mock(NotifyMessageSendApi.class);
        }
    }
}
