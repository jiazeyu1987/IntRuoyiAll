package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.job.DccRegistrationCertificateReminderDailyJob;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.threshold.DccRegistrationCertificateThresholdNotificationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateRecipientResolver;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateReminderDailyJob.class,
        DccRegistrationCertificateConfigService.class,
        DccRegistrationCertificateDailyRunService.class,
        DccRegistrationCertificateReminderService.class,
        DccRegistrationCertificateRecipientResolver.class,
        DccRegistrationCertificateThresholdNotificationService.class,
        DccRegistrationCertificateReminderJobRuntimeTest.DbTestConfiguration.class
})
class DccRegistrationCertificateReminderJobRuntimeTest extends BaseDbUnitTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 18);
    private static final String PERMISSION = "dcc:registration-certificate:reminder:receive";
    private static final String PARAM = """
            {"actorId":99,"roleIds":[1001],"permission":"dcc:registration-certificate:reminder:receive"}
            """;

    @Resource
    private DccRegistrationCertificateReminderDailyJob job;
    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private MdmCompanyScopeApi companyScopeApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private DccRegistrationCertificateActivationService activationService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetMocks() {
        reset(tenantFrameworkService, companyScopeApi, notifyMessageSendApi, activationService);
    }

    @Test
    void jobBeanUsesSeededHandlerNameAndFailsFastWithoutRecipientContract() {
        assertInstanceOf(JobHandler.class, job);

        ServiceException exception = assertThrows(ServiceException.class, () -> job.execute(" "));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED.getCode(), exception.getCode());
        assertEquals(0, countDailyRuns());
    }

    @Test
    void oneTenantFailureFailsTopLevelAndRetryDoesNotDuplicateSuccessfulTenant() {
        insertTenantOneDueCandidate();
        insertActiveCertificate(2L, 2001L, 2201L, null, 520L, LocalDate.of(2026, 8, 26));
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(1L, 2L));
        when(companyScopeApi.resolveRecipientUserIds(eq(501L), eq(List.of(1001L)), eq(PERMISSION)))
                .thenReturn(new LinkedHashSet<>(List.of(7001L)));
        when(companyScopeApi.resolveRecipientUserIds(eq(520L), eq(List.of(1001L)), eq(PERMISSION)))
                .thenThrow(new IllegalStateException("tenant-2 recipient source unavailable"))
                .thenReturn(new LinkedHashSet<>(List.of(7002L)));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any()))
                .thenReturn(99001L, 99002L);

        AtomicBoolean activatedBeforeNotify = new AtomicBoolean(false);
        when(activationService.activateDueCandidate(any())).thenAnswer(invocation -> {
            DccRegistrationCertificateActivationCommand command = invocation.getArgument(0);
            assertEquals(1L, command.tenantId());
            assertEquals(1001L, command.certificateId());
            activateTenantOneCandidate(command);
            activatedBeforeNotify.set(true);
            return new DccRegistrationCertificateActivationResult(1001L, 1101L, 1102L, 5102L, true);
        });
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any())).thenAnswer(invocation -> {
            assertTrue(activatedBeforeNotify.get(), "candidate activation must happen before threshold delivery");
            return deliveryCount(1L) == 0 ? 99001L : 99002L;
        });

        IllegalStateException firstFailure = assertThrows(IllegalStateException.class, () -> job.execute(PARAM));

        assertTrue(firstFailure.getMessage().contains("tenantId=2"));
        assertEquals("SUCCESS", dailyStatus(1L));
        assertEquals("FAILED", dailyStatus(2L));
        assertEquals(0, dailyRetryCount(2L));
        assertEquals(1, sentDeliveryCount(1L));
        assertEquals(0, deliveryCount(2L));
        verify(activationService, times(1)).activateDueCandidate(any());

        String retrySummary = assertDoesNotThrow(() -> job.execute(PARAM));

        assertTrue(retrySummary.contains("成功=1"));
        assertEquals("SUCCESS", dailyStatus(1L));
        assertEquals("SUCCESS", dailyStatus(2L));
        assertEquals(1, dailyRetryCount(2L));
        assertEquals(1, sentDeliveryCount(1L), "successful tenant must not duplicate deliveries on retry");
        assertEquals(1, sentDeliveryCount(2L));
        verify(notifyMessageSendApi, times(2)).sendSingleMessageIdempotentlyToAdmin(any());
        ArgumentCaptor<DccRegistrationCertificateActivationCommand> captor =
                ArgumentCaptor.forClass(DccRegistrationCertificateActivationCommand.class);
        verify(activationService).activateDueCandidate(captor.capture());
        assertEquals("registration-certificate-reminder-activation:1001:1102:2026-08-18",
                captor.getValue().idempotencyKey());
    }

    @Test
    void retryWithFailedDeliveryKeepsTenantFailedAndDoesNotClaimSuccess() {
        insertActiveCertificate(1L, 3001L, 3301L, null, 530L, LocalDate.of(2026, 8, 26));
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(1L));
        when(companyScopeApi.resolveRecipientUserIds(eq(530L), eq(List.of(1001L)), eq(PERMISSION)))
                .thenReturn(new LinkedHashSet<>(List.of(7301L)));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any()))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> job.execute(PARAM));

        assertEquals("FAILED", dailyStatus(1L));
        assertEquals(0, dailyRetryCount(1L));
        assertEquals(1, failedDeliveryCount(1L));

        IllegalStateException retryFailure = assertThrows(IllegalStateException.class, () -> job.execute(PARAM));

        assertTrue(retryFailure.getMessage().contains("tenantId=1"));
        assertEquals("FAILED", dailyStatus(1L));
        assertEquals(1, dailyRetryCount(1L));
        assertEquals(0, sentDeliveryCount(1L));
        assertEquals(1, failedDeliveryCount(1L));
        verify(notifyMessageSendApi, times(1)).sendSingleMessageIdempotentlyToAdmin(any());
    }

    private void insertTenantOneDueCandidate() {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, tenant_id, owner_company_id, product_master_id, first_obtained_date,
                   current_version_id, pending_version_id, current_snapshot_id, status, row_version)
                VALUES (1001, 1, 501, 601, DATE '2023-01-01', 1101, 1102, 5101, 'ACTIVE', 3)
                """);
        insertVersion(1L, 1001L, 1101L, "CURRENT", LocalDate.of(2026, 12, 31),
                LocalDate.of(2023, 1, 2));
        insertVersion(1L, 1001L, 1102L, "PENDING_EFFECTIVE", LocalDate.of(2026, 9, 17),
                BUSINESS_DATE);
    }

    private void insertActiveCertificate(Long tenantId, Long certificateId, Long versionId,
                                         Long pendingVersionId, Long ownerCompanyId, LocalDate expiryDate) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate
                  (id, tenant_id, owner_company_id, product_master_id, first_obtained_date,
                   current_version_id, pending_version_id, current_snapshot_id, status, row_version)
                VALUES (?, ?, ?, 601, DATE '2023-01-01', ?, ?, ?, 'ACTIVE', 1)
                """, certificateId, tenantId, ownerCompanyId, versionId, pendingVersionId, versionId + 3000);
        insertVersion(tenantId, certificateId, versionId, "CURRENT", expiryDate, LocalDate.of(2023, 1, 2));
    }

    private void insertVersion(Long tenantId, Long certificateId, Long versionId, String status,
                               LocalDate expiryDate, LocalDate effectiveDate) {
        Integer currentFlag = "CURRENT".equals(status) ? 1 : null;
        Integer pendingFlag = "PENDING_EFFECTIVE".equals(status) ? 1 : null;
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_version
                  (id, tenant_id, certificate_id, version_no, version_type, certificate_no,
                   approval_date, effective_date, expiry_date, classification, category_changed,
                   status, current_unique_flag, pending_unique_flag, formalized_at, formalized_by)
                VALUES (?, ?, ?, ?, 'INITIAL_CERTIFICATE', ?, DATE '2023-01-01',
                        ?, ?, 'CLASS-III', 0, ?, ?, ?, CURRENT_TIMESTAMP, 9)
                """, versionId, tenantId, certificateId,
                "PENDING_EFFECTIVE".equals(status) ? 2 : 1,
                "CERT-" + certificateId + "-" + versionId,
                effectiveDate, expiryDate, status, currentFlag, pendingFlag);
    }

    private void activateTenantOneCandidate(DccRegistrationCertificateActivationCommand command) {
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate
                   SET current_version_id = ?, pending_version_id = NULL, current_snapshot_id = ?,
                       row_version = row_version + 1
                 WHERE tenant_id = ? AND id = ?
                """, command.pendingVersionId(), 5102L, command.tenantId(), command.certificateId());
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET status = 'OLD', current_unique_flag = NULL
                 WHERE tenant_id = ? AND id = ?
                """, command.tenantId(), command.currentVersionId());
        jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET status = 'CURRENT', current_unique_flag = 1, pending_unique_flag = NULL
                 WHERE tenant_id = ? AND id = ?
                """, command.tenantId(), command.pendingVersionId());
    }

    private int countDailyRuns() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_daily_run
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private String dailyStatus(Long tenantId) {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM dcc_registration_certificate_daily_run WHERE tenant_id = ?
                """, String.class, tenantId);
    }

    private int dailyRetryCount(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT retry_count FROM dcc_registration_certificate_daily_run WHERE tenant_id = ?
                """, Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    private int deliveryCount(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_reminder_delivery WHERE tenant_id = ?
                """, Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    private int sentDeliveryCount(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_reminder_delivery
                 WHERE tenant_id = ? AND status = 'SENT'
                """, Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    private int failedDeliveryCount(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_reminder_delivery
                 WHERE tenant_id = ? AND status = 'FAILED'
                """, Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-18T01:00:00Z"), BUSINESS_ZONE));
        }

        @Bean
        TenantFrameworkService tenantFrameworkService() {
            return mock(TenantFrameworkService.class);
        }

        @Bean
        MdmCompanyScopeApi mdmCompanyScopeApi() {
            return mock(MdmCompanyScopeApi.class);
        }

        @Bean
        NotifyMessageSendApi notifyMessageSendApi() {
            return mock(NotifyMessageSendApi.class);
        }

        @Bean
        DccRegistrationCertificateActivationService dccRegistrationCertificateActivationService() {
            return mock(DccRegistrationCertificateActivationService.class);
        }
    }
}
