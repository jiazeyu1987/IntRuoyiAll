package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunRecord;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateDailyRunStartResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock.BUSINESS_ZONE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({
        DccRegistrationCertificateDailyRunService.class,
        DccRegistrationCertificateDailyRunServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateDailyRunServiceTest extends BaseDbUnitTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 18);

    @Resource
    private DccRegistrationCertificateDailyRunService service;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void tenantDailyRunStartsAndSucceedsOncePerDay() {
        DccRegistrationCertificateDailyRunStartResult first =
                assertDoesNotThrow(() -> service.startTenantRun(1L, BUSINESS_DATE));
        assertTrue(first.started());
        assertEquals("RUNNING", first.run().status());
        assertEquals("registration-certificate-reminder:1:2026-08-18", first.run().runKey());

        DccRegistrationCertificateDailyRunStartResult duplicateRunning =
                assertDoesNotThrow(() -> service.startTenantRun(1L, BUSINESS_DATE));
        assertFalse(duplicateRunning.started());
        assertEquals(first.run().id(), duplicateRunning.run().id());
        assertEquals(1, countRuns(1L, BUSINESS_DATE));

        DccRegistrationCertificateDailyRunRecord success =
                assertDoesNotThrow(() -> service.markSuccess(1L, first.run().id(), "{\"created\":0}"));
        assertEquals("SUCCESS", success.status());
        assertEquals(0, success.retryCount());

        DccRegistrationCertificateDailyRunStartResult afterSuccess =
                assertDoesNotThrow(() -> service.startTenantRun(1L, BUSINESS_DATE));
        assertFalse(afterSuccess.started());
        assertEquals("SUCCESS", afterSuccess.run().status());
        assertEquals(1, countRuns(1L, BUSINESS_DATE));
    }

    @Test
    void failedRunKeepsReasonAndCanRetrySameDay() {
        DccRegistrationCertificateDailyRunStartResult first = service.startTenantRun(1L, BUSINESS_DATE);
        DccRegistrationCertificateDailyRunRecord failed =
                assertDoesNotThrow(() -> service.markFailed(
                        1L, first.run().id(), "recipient scope missing", "{\"tenant\":\"failed\"}"));
        assertEquals("FAILED", failed.status());
        assertEquals("recipient scope missing", failed.failureReason());
        assertEquals(0, failed.retryCount());

        DccRegistrationCertificateDailyRunStartResult retry =
                assertDoesNotThrow(() -> service.startTenantRun(1L, BUSINESS_DATE));
        assertTrue(retry.started());
        assertEquals(first.run().id(), retry.run().id());
        assertEquals("RUNNING", retry.run().status());
        assertEquals(1, retry.run().retryCount());
        assertEquals(null, retry.run().failureReason());

        DccRegistrationCertificateDailyRunRecord success =
                assertDoesNotThrow(() -> service.markSuccess(1L, retry.run().id(), "{\"retry\":true}"));
        assertEquals("SUCCESS", success.status());
        assertEquals(1, success.retryCount());
    }

    @Test
    void tenantBoundaryAndInvalidTransitionsFailClosed() {
        DccRegistrationCertificateDailyRunStartResult tenantOne = service.startTenantRun(1L, BUSINESS_DATE);
        DccRegistrationCertificateDailyRunStartResult tenantTwo = service.startTenantRun(2L, BUSINESS_DATE);
        assertTrue(tenantTwo.started());
        assertEquals(1, countRuns(1L, BUSINESS_DATE));
        assertEquals(1, countRuns(2L, BUSINESS_DATE));

        ServiceException wrongTenant = assertThrows(ServiceException.class,
                () -> service.markSuccess(2L, tenantOne.run().id(), "{}"));
        assertEquals(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT.getCode(), wrongTenant.getCode());

        ServiceException blankFailure = assertThrows(ServiceException.class,
                () -> service.markFailed(1L, tenantOne.run().id(), " ", "{}"));
        assertEquals(REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED.getCode(), blankFailure.getCode());

        DccRegistrationCertificateDailyRunRecord success = service.markSuccess(1L, tenantOne.run().id(), "{}");
        assertEquals("SUCCESS", success.status());
        ServiceException afterSuccess = assertThrows(ServiceException.class,
                () -> service.markFailed(1L, tenantOne.run().id(), "late failure", "{}"));
        assertEquals(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT.getCode(), afterSuccess.getCode());
    }

    private Integer countRuns(Long tenantId, LocalDate businessDate) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_daily_run
                 WHERE tenant_id = ? AND business_date = ?
                """, Integer.class, tenantId, businessDate);
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
    }
}
