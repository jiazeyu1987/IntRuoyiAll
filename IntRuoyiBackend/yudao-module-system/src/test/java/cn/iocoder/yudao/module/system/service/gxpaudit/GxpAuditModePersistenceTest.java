package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GxpAuditModePersistenceTest {
    @AfterEach void restore() {
        new GxpAuditDevelopmentMode(new MockEnvironment(), GxpAuditMode.FORMAL);
    }

    @Test void platformChangeAppliesOnlyAtNextStartupAndOverridesOldEnvironmentFlags() {
        var environment = GxpAuditDevelopmentModeTest.local()
                .withProperty("yudao.gxp-audit.development-disabled", "true");
        var row = row("DEVELOPMENT_DISABLED");
        var mapper = mock(ConfigMapper.class);
        when(mapper.selectByKey(GxpAuditMode.CONFIG_KEY)).thenReturn(row);
        new GxpAuditDevelopmentMode(environment, mapper);
        assertTrue(GxpAuditDevelopmentMode.disabled());

        // Platform deployment updates the stored startup mode; tenant CRUD cannot modify it.
        row.setValue("FORMAL");

        assertEquals("FORMAL", row.getValue());
        assertTrue(GxpAuditDevelopmentMode.disabled(), "Saving must not hot-switch an in-flight transaction");
        new GxpAuditDevelopmentMode(environment, mapper);
        assertEquals(GxpAuditMode.FORMAL, GxpAuditDevelopmentMode.currentMode());
        assertFalse(GxpAuditDevelopmentMode.disabled());
    }

    @Test void missingConfigurationStopsStartupWithoutUsingEnvironmentFlags() {
        var mapper = mock(ConfigMapper.class);
        assertThrows(IllegalStateException.class, () -> new GxpAuditDevelopmentMode(GxpAuditDevelopmentModeTest.local(), mapper));
    }

    @ParameterizedTest @ValueSource(strings = {"", "formal", " FORMAL", "false", "MOCK"})
    void invalidPersistedModeStopsStartup(String value) {
        var mapper = mock(ConfigMapper.class);
        when(mapper.selectByKey(GxpAuditMode.CONFIG_KEY)).thenReturn(row(value));
        assertThrows(IllegalArgumentException.class, () -> new GxpAuditDevelopmentMode(new MockEnvironment(), mapper));
    }

    @Test void disabledSpringContextDoesNotLoadAnyPolicy() {
        var mapper = mock(ConfigMapper.class);
        when(mapper.selectByKey(GxpAuditMode.CONFIG_KEY)).thenReturn(row("DEVELOPMENT_DISABLED"));
        var loader = mock(GxpAuditPolicyBundleLoader.class);
        new ApplicationContextRunner()
                .withUserConfiguration(GxpAuditRuntimeConfiguration.class, GxpAuditDevelopmentMode.class)
                .withBean(ConfigMapper.class, () -> mapper)
                .withBean(GxpAuditPolicyBundleLoader.class, () -> loader)
                .withPropertyValues("spring.profiles.active=local",
                        "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1/test")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(GxpLocalDevelopmentPolicy.class).isEmpty());
                    assertTrue(GxpAuditDevelopmentMode.disabled());
                    verifyNoInteractions(loader);
                });
    }

    private static ConfigDO row(String value) {
        var row = new ConfigDO(); row.setId(1L); row.setConfigKey(GxpAuditMode.CONFIG_KEY);
        row.setValue(value); row.setVisible(true); row.setType(1);
        return row;
    }
}
