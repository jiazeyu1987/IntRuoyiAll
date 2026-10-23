package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GxpLocalDevelopmentPolicyTest {
    private final GxpAuditPolicyBundleLoader loader = new GxpAuditPolicyBundleLoader();

    @Test
    void disabledByDefaultAndExplicitlyFalse() {
        var context = new ApplicationContextRunner()
                .withUserConfiguration(GxpLocalDevelopmentPolicy.class, GxpAuditPolicyBundleLoader.class);
        context.run(c -> assertEquals(0, c.getBeansOfType(GxpLocalDevelopmentPolicy.class).size()));
        context.withPropertyValues(GxpLocalDevelopmentPolicy.PROPERTY + "=false")
                .run(c -> assertEquals(0, c.getBeansOfType(GxpLocalDevelopmentPolicy.class).size()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "prod", "dev", "test", "local,prod", "local,test"})
    void explicitFlagCannotEnableOutsideExclusiveLocalProfile(String profiles) {
        new ApplicationContextRunner()
                .withUserConfiguration(GxpLocalDevelopmentPolicy.class, GxpAuditPolicyBundleLoader.class)
                .withPropertyValues(GxpLocalDevelopmentPolicy.PROPERTY + "=true", "spring.profiles.active=" + profiles)
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "jdbc:mysql://db.example:3306/db", "jdbc:mysql://192.168.1.2/db",
            "jdbc:mysql://localhost.evil/db", "jdbc:mysql://localhost,remote/db", "jdbc:h2:mem:test"})
    void rejectsMissingOrRemoteMasterBeforeLoadingPolicy(String url) {
        var env = localEnvironment();
        env.setProperty("spring.datasource.dynamic.datasource.master.url", url);
        var unused = mock(GxpAuditPolicyBundleLoader.class);
        assertThrows(IllegalStateException.class, () -> new GxpLocalDevelopmentPolicy(env, unused));
        verifyNoInteractions(unused);
    }

    @Test
    void remoteSlaveAlsoRejected() {
        var env = localEnvironment().withProperty("spring.datasource.dynamic.datasource.slave.url", "jdbc:mysql://remote/db");
        assertThrows(IllegalStateException.class, () -> new GxpLocalDevelopmentPolicy(env, loader));
    }

    @Test
    void explicitLocalModeUsesValidatedPackagedOperationsAndDistinctVersion() {
        new ApplicationContextRunner()
                .withUserConfiguration(GxpLocalDevelopmentPolicy.class, GxpAuditPolicyBundleLoader.class)
                .withPropertyValues(GxpLocalDevelopmentPolicy.PROPERTY + "=true", "spring.profiles.active=local",
                        "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.2:23306/local")
                .run(c -> {
                    assertNull(c.getStartupFailure());
                    var policy = c.getBean(GxpLocalDevelopmentPolicy.class);
                    assertEquals("LOCAL-DEV-" + loader.load().policyHash(), policy.eventPolicyVersion());
                    assertNull(policy.findOperation(1L, "unknown"));
                    var operation = policy.findOperation(1L, "dcc.project-file-placement.upload-bind");
                    assertNotNull(operation);
                    assertEquals(1L, operation.getTenantId());
                    assertEquals(policy.eventPolicyVersion(), operation.getPolicyVersion());
                    operation.setActive(false);
                    assertTrue(policy.findOperation(2L, operation.getOperationId()).getActive());
                    assertEquals(2L, policy.findOperation(2L, operation.getOperationId()).getTenantId());
                });
    }

    @Test
    void localModeCannotProduceFormalActivation() {
        var service = new GxpAuditPolicyActivationServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "localDevelopmentPolicy",
                new GxpLocalDevelopmentPolicy(localEnvironment(), loader));
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.activate(null));
        assertTrue(failure.getMessage().contains("disable-local-development-policy-before-formal-activation"));
    }

    @Test
    void malformedBundleFailureIsNotSuppressed() {
        var broken = mock(GxpAuditPolicyBundleLoader.class);
        var failure = new IllegalStateException("invalid packaged policy");
        when(broken.load()).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new GxpLocalDevelopmentPolicy(localEnvironment(), broken)));
    }

    static MockEnvironment localEnvironment() {
        var environment = new MockEnvironment().withProperty(
                "spring.datasource.dynamic.datasource.master.url", "jdbc:mysql://127.0.0.2:23306/local");
        environment.setActiveProfiles("local");
        return environment;
    }
}
