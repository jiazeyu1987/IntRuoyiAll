package cn.iocoder.yudao.server;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundle;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Spring startup ordering; database and bundle boundaries are explicit test doubles. */
class GxpAuditPolicyStartupGuardTest {

    private static final String HASH = "a".repeat(64);
    private static final String GUARD_PACKAGE = "cn.iocoder.yudao.server.gxpaudit";
    private final TenantMapper tenants = mock(TenantMapper.class);
    private final GxpAuditPolicyBundleLoader loader = mock(GxpAuditPolicyBundleLoader.class);
    private final AtomicInteger ready = new AtomicInteger();
    private final AtomicInteger jobs = new AtomicInteger();
    private final AtomicInteger reads = new AtomicInteger();
    private List<GxpAuditPolicyActivationDO> latest = List.of();
    private RuntimeException readFailure;
    // Name the planned contract without referencing a missing method: RED must be startup behavior.
    private final GxpAuditPolicyActivationMapper activations = mock(GxpAuditPolicyActivationMapper.class,
            invocation -> {
                assertEquals("selectLatestAcrossTenants", invocation.getMethod().getName());
                assertEquals(0, invocation.getArguments().length);
                assertTrue(TenantContextHolder.isIgnore(), "cross-tenant read must explicitly ignore tenant filtering");
                reads.incrementAndGet();
                if (readFailure != null) {
                    throw readFailure;
                }
                return latest;
            });

    @AfterEach
    void clearOwnedContext() {
        TenantContextHolder.clear();
    }

    @Test
    void rejectsSecondActivatedTenantMismatchBeforeJobsAndReady() {
        matchingTenants();
        latest = List.of(activation(101L, HASH), activation(202L, "b".repeat(64)));

        RuntimeException failure = rejectedStartup();
        assertTrue(causeMessages(failure).contains("202"), "failure must identify mismatching tenant");

        verifyReadOnly();
    }

    @Test
    void checksAllLatestActivationsBeforeSuccessfulStartup() {
        matchingTenants();

        try (ConfigurableApplicationContext ignored = start()) {
            assertEquals(1, ready.get());
            assertEquals(1, jobs.get());
            assertTrue(ignored.containsBean("gxpAuditPolicyStartupGuard"),
                    "default stereotype scanning must discover the production component");
            verifyReadOnly();
        }
    }

    @Test
    void tenantWithoutActivationStartsWithoutSeedingAnything() {
        bundle();

        try (ConfigurableApplicationContext ignored = start()) {
            assertEquals(1, ready.get());
            verifyReadOnly();
        }
    }

    @Test
    void invalidOrMissingBundleFailsBeforeJobsAndReady() {
        RuntimeException cause = new IllegalStateException("test: bundle resource/schema invalid");
        when(loader.load()).thenThrow(cause);

        assertCause(rejectedStartup(), cause);
        verify(loader).load();
        verifyNoMoreInteractions(loader);
        verifyNoInteractions(tenants, activations);
    }

    @Test
    void crossTenantReadFailureCannotBecomeNoActivations() {
        bundle();
        readFailure = new IllegalStateException("test: activation read unavailable");

        assertCause(rejectedStartup(), readFailure);
        assertNull(TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
        verifyReadOnly();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "deleted", "orphan"})
    void systemTenantStateMustNotExcludeExistingActivation(String state) {
        bundle();
        TenantDO systemRow = TenantDO.builder().id(202L).status("disabled".equals(state) ? 1 : 0).build();
        systemRow.setDeleted("deleted".equals(state));
        when(tenants.selectById(202L)).thenReturn("orphan".equals(state) ? null : systemRow);
        // ENABLE-list mocks return empty: no system-tenant filtering is allowed.
        latest = List.of(activation(101L, HASH), activation(202L, "b".repeat(64)));

        RuntimeException failure = rejectedStartup();
        assertTrue(causeMessages(failure).contains("202"), state + " activation must be checked");
        verifyReadOnly();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresExistingContextAfterSuccessfulCrossTenantRead(boolean ignoredBefore) {
        matchingTenants();
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(ignoredBefore);
        try (ConfigurableApplicationContext ignored = start()) {
            assertEquals(999L, TenantContextHolder.getTenantId());
            assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
            verifyReadOnly();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresExistingContextWhenCrossTenantReadThrows(boolean ignoredBefore) {
        bundle();
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(ignoredBefore);
        readFailure = new IllegalStateException("test: read failed with existing tenant context");
        assertCause(rejectedStartup(), readFailure);
        assertEquals(999L, TenantContextHolder.getTenantId());
        assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
        verifyReadOnly();
    }

    @Test
    void defaultScanSliceIsInsideRealServerScanRoots() {
        String[] roots = YudaoServerApplication.class.getAnnotation(SpringBootApplication.class).scanBasePackages();
        assertTrue(Arrays.stream(roots).map(root -> root.replace("${yudao.info.base-package}", "cn.iocoder.yudao"))
                .anyMatch(root -> GUARD_PACKAGE.startsWith(root + ".")));
        ComponentScan scan = StartupSlice.class.getAnnotation(ComponentScan.class);
        assertTrue(scan.useDefaultFilters());
        assertEquals(0, scan.includeFilters().length, "no regex may force an unannotated guard into the context");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresContextAfterHashMismatch(boolean ignoredBefore) {
        bundle();
        latest = List.of(activation(202L, "b".repeat(64)));
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(ignoredBefore);
        assertTrue(causeMessages(rejectedStartup()).contains("202"));
        assertEquals(999L, TenantContextHolder.getTenantId());
        assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
        verifyReadOnly();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsMissingPersistedHash(String hash) {
        bundle();
        latest = List.of(activation(202L, hash));
        assertTrue(causeMessages(rejectedStartup()).contains("202"));
        verifyReadOnly();
    }

    private RuntimeException rejectedStartup() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            // Close even the unexpectedly successful RED context; never leave lifecycle work running.
            try (ConfigurableApplicationContext ignored = start()) {
                assertNotNull(ignored);
            }
        });
        assertEquals(0, ready.get(), "a rejected policy must never reach Ready");
        assertEquals(0, jobs.get(), "a rejected policy must fail before lifecycle jobs start");
        return failure;
    }

    private ConfigurableApplicationContext start() {
        Long tenantBefore = TenantContextHolder.getTenantId();
        boolean ignoreBefore = TenantContextHolder.isIgnore();
        SpringApplication app = new SpringApplication(StartupSlice.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setRegisterShutdownHook(false);
        // Do not load application-local.yaml or connect any production infrastructure.
        app.setDefaultProperties(Map.of("spring.config.name", "act01-isolated-startup-test",
                "spring.main.banner-mode", "off"));
        app.addInitializers(context -> {
            context.getBeanFactory().registerSingleton("tenantMapper", tenants);
            context.getBeanFactory().registerSingleton("gxpAuditPolicyActivationMapper", activations);
            context.getBeanFactory().registerSingleton("gxpAuditPolicyBundleLoader", loader);
            context.getBeanFactory().registerSingleton("jobLifecycleSentinel", new SmartLifecycle() {
                private boolean running;
                @Override public void start() {
                    assertEquals(1, reads.get(), "guard read must finish before any lifecycle job");
                    assertEquals(tenantBefore, TenantContextHolder.getTenantId());
                    assertEquals(ignoreBefore, TenantContextHolder.isIgnore());
                    jobs.incrementAndGet();
                    running = true;
                }
                @Override public void stop() { running = false; }
                @Override public boolean isRunning() { return running; }
                @Override public int getPhase() { return Integer.MIN_VALUE; }
            });
        });
        app.addListeners(event -> {
            if (event instanceof ApplicationReadyEvent) {
                ready.incrementAndGet();
            }
        });
        return app.run();
    }

    // Uses default stereotype filters: removing @Component must break behavior tests.
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = GUARD_PACKAGE)
    static class StartupSlice { }

    private void bundle() {
        when(loader.load()).thenReturn(new GxpAuditPolicyBundle("gxp-audit-policy.v2", "test-v1",
                "DRAFT", "test-only", HASH, "c".repeat(64), "{}", "", "", null));
    }

    private void matchingTenants() {
        bundle();
        latest = List.of(activation(101L, HASH), activation(202L, HASH));
    }

    private void verifyReadOnly() {
        verify(loader).load();
        verifyNoMoreInteractions(loader);
        verifyNoInteractions(tenants);
        assertEquals(1, reads.get(), "read all latest activations independent of current tenant");
        assertEquals(1, mockingDetails(activations).getInvocations().size(), "no write or extra query permitted");
    }

    private static String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cursor = failure; cursor != null; cursor = cursor.getCause()) {
            messages.append(cursor.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private static GxpAuditPolicyActivationDO activation(long tenantId, String hash) {
        GxpAuditPolicyActivationDO row = new GxpAuditPolicyActivationDO();
        row.setTenantId(tenantId);
        row.setPolicyHash(hash);
        return row;
    }

    private static void assertCause(Throwable failure, Throwable expected) {
        Throwable cursor = failure;
        while (cursor != null && cursor != expected) {
            cursor = cursor.getCause();
        }
        assertSame(expected, cursor, "original startup failure must remain traceable");
    }
}
