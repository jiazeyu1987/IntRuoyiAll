package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.DccRegistrationCertificateReminderConfigController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfig;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfigUpdateCommand;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({
        DccRegistrationCertificateConfigService.class,
        DccRegistrationCertificateConfigServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateConfigServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateConfigService service;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void getOrCreateCreatesTenantDefaultOnce() {
        DccRegistrationCertificateReminderConfig config = assertDoesNotThrow(() -> service.getOrCreate(1L));

        assertNotNull(config.id());
        assertEquals(1L, config.tenantId());
        assertEquals(Boolean.TRUE, config.enabled());
        assertEquals("09:00", config.dailyRunTime());
        assertEquals("Asia/Shanghai", config.timezone());
        assertEquals("[30,8,2,1]", config.thresholdDaysJson());
        assertEquals(1, config.rowVersion());
        assertEquals(1, countConfigs(1L));

        DccRegistrationCertificateReminderConfig replay = assertDoesNotThrow(() -> service.getOrCreate(1L));
        assertEquals(config.id(), replay.id());
        assertEquals(1, countConfigs(1L));
    }

    @Test
    void updateUsesTenantAndRevisionAndNeverTouchesAnotherTenant() {
        DccRegistrationCertificateReminderConfig tenantOne = service.getOrCreate(1L);
        DccRegistrationCertificateReminderConfig tenantTwo = service.getOrCreate(2L);

        DccRegistrationCertificateReminderConfig updated = assertDoesNotThrow(() -> service.update(
                1L, 99L, new DccRegistrationCertificateReminderConfigUpdateCommand(
                        false, "10:30", tenantOne.rowVersion())));

        assertEquals(tenantOne.id(), updated.id());
        assertEquals(Boolean.FALSE, updated.enabled());
        assertEquals("10:30", updated.dailyRunTime());
        assertEquals("Asia/Shanghai", updated.timezone());
        assertEquals(tenantOne.rowVersion() + 1, updated.rowVersion());
        assertEquals("09:00", service.getOrCreate(2L).dailyRunTime());
        assertEquals(tenantTwo.rowVersion(), service.getOrCreate(2L).rowVersion());
    }

    @Test
    void staleOrInvalidUpdateFailsUnchanged() {
        DccRegistrationCertificateReminderConfig config = service.getOrCreate(1L);

        ServiceException stale = assertThrows(ServiceException.class, () -> service.update(
                1L, 99L, new DccRegistrationCertificateReminderConfigUpdateCommand(
                        false, "10:30", config.rowVersion() + 1)));
        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT.getCode(), stale.getCode());
        assertEquals("09:00", service.getOrCreate(1L).dailyRunTime());
        assertEquals(config.rowVersion(), service.getOrCreate(1L).rowVersion());

        ServiceException invalidTime = assertThrows(ServiceException.class, () -> service.update(
                1L, 99L, new DccRegistrationCertificateReminderConfigUpdateCommand(
                        false, "9:00", config.rowVersion())));
        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID.getCode(), invalidTime.getCode());
        assertEquals("09:00", service.getOrCreate(1L).dailyRunTime());
    }

    @Test
    void controllerUsesIndependentRegistrationCertificateConfigPermissions() throws Exception {
        RequestMapping classMapping = DccRegistrationCertificateReminderConfigController.class
                .getAnnotation(RequestMapping.class);
        assertEquals("/dcc/registration-certificates/reminder-config", classMapping.value()[0]);

        Method getConfig = DccRegistrationCertificateReminderConfigController.class.getMethod("getConfig");
        assertEquals("@ss.hasPermission('dcc:registration-certificate:config:query')",
                getConfig.getAnnotation(PreAuthorize.class).value());
        assertNotNull(getConfig.getAnnotation(GetMapping.class));

        Method updateConfig = DccRegistrationCertificateReminderConfigController.class.getMethod(
                "updateConfig",
                cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo
                        .DccRegistrationCertificateReminderConfigUpdateReqVO.class);
        assertEquals("@ss.hasPermission('dcc:registration-certificate:config:update')",
                updateConfig.getAnnotation(PreAuthorize.class).value());
        assertNotNull(updateConfig.getAnnotation(PutMapping.class));
    }

    private Integer countConfigs(Long tenantId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_reminder_config
                 WHERE tenant_id = ?
                """, Integer.class, tenantId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
