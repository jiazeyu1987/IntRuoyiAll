package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.DccRegistrationCertificateReminderConfigController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfig;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfigUpdateCommand;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@Import({
        DccRegistrationCertificateConfigService.class,
        DccRegistrationCertificateConfigServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateConfigServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateConfigService service;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;

    private static Map<String, List<Long>> recipients() {
        return Map.of("T_30", List.of(101L, 102L), "T_8", List.of(102L),
                "T_2", List.of(103L), "T_1", List.of(104L));
    }

    @Test
    void getOrCreateCreatesTenantDefaultOnce() {
        DccRegistrationCertificateReminderConfig config = assertDoesNotThrow(() -> service.getOrCreate(1L));

        assertNotNull(config.id());
        assertEquals(1L, config.tenantId());
        assertEquals(Boolean.TRUE, config.enabled());
        assertEquals("09:00", config.dailyRunTime());
        assertEquals("Asia/Shanghai", config.timezone());
        assertEquals("[30,8,2,1]", config.thresholdDaysJson());
        assertEquals("{}", config.thresholdRecipientUserIdsJson());
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
                        false, "10:30", recipients(), tenantOne.rowVersion())));

        assertEquals(tenantOne.id(), updated.id());
        assertEquals(Boolean.FALSE, updated.enabled());
        assertEquals("10:30", updated.dailyRunTime());
        assertEquals("Asia/Shanghai", updated.timezone());
        assertEquals(tenantOne.rowVersion() + 1, updated.rowVersion());
        assertEquals(recipients(), DccRegistrationCertificateConfigService.parseThresholdRecipientUserIds(
                updated.thresholdRecipientUserIdsJson()));
        assertEquals("09:00", service.getOrCreate(2L).dailyRunTime());
        assertEquals(tenantTwo.rowVersion(), service.getOrCreate(2L).rowVersion());

        ArgumentCaptor<SystemEntitlementSyncReqDTO> entitlementCaptor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(entitlementCaptor.capture());
        assertEquals("DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW",
                entitlementCaptor.getValue().getPolicyCode());
        assertEquals(java.util.Set.of(101L, 102L, 103L, 104L),
                entitlementCaptor.getValue().getResolvedUserIds());
    }

    @Test
    void staleOrInvalidUpdateFailsUnchanged() {
        DccRegistrationCertificateReminderConfig config = service.getOrCreate(1L);

        ServiceException stale = assertThrows(ServiceException.class, () -> service.update(
                1L, 99L, new DccRegistrationCertificateReminderConfigUpdateCommand(
                        false, "10:30", recipients(), config.rowVersion() + 1)));
        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT.getCode(), stale.getCode());
        assertEquals("09:00", service.getOrCreate(1L).dailyRunTime());
        assertEquals(config.rowVersion(), service.getOrCreate(1L).rowVersion());

        ServiceException invalidTime = assertThrows(ServiceException.class, () -> service.update(
                1L, 99L, new DccRegistrationCertificateReminderConfigUpdateCommand(
                        false, "9:00", recipients(), config.rowVersion())));
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
