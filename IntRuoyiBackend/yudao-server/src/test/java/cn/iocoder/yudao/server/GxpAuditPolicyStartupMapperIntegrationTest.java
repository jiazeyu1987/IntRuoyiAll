package cn.iocoder.yudao.server;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundle;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual mapper, SQL and project tenant plugin on owned H2; not MySQL or production approval evidence. */
class GxpAuditPolicyStartupMapperIntegrationTest {

    private static final String HASH = "a".repeat(64);
    private final GxpAuditPolicyBundleLoader loader = mock(GxpAuditPolicyBundleLoader.class);
    private final SqlEvidence evidence = new SqlEvidence();
    private final AtomicInteger jobs = new AtomicInteger();
    private final AtomicInteger ready = new AtomicInteger();
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private List<Map<String, Object>> originalRows;

    @BeforeEach
    void prepareOwnedDatabase() {
        TenantContextHolder.clear();
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:startup_" + UUID.randomUUID()
                + ";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE gxp_audit_policy_activation (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, "
                + "policy_version VARCHAR(100), policy_hash VARCHAR(64), previous_activation_hash VARCHAR(64), "
                + "request_id VARCHAR(100), actor_id BIGINT, approval_reference VARCHAR(100), "
                + "activated_at_utc TIMESTAMP, effective_after_sequence BIGINT, "
                + "canonical_activation_json CLOB, activation_hash VARCHAR(64))");
        jdbc.execute("CREATE TABLE system_tenant (id BIGINT PRIMARY KEY, status INT, deleted BOOLEAN)");
        jdbc.update("INSERT INTO system_tenant VALUES (101, 1, false), (202, 0, true), (999, 0, false)");
        // 101 disabled, 202 deleted, 303 orphan. Interleaved history deliberately disagrees with latest.
        insert(10, 101, "b".repeat(64));
        insert(20, 202, "b".repeat(64));
        insert(30, 303, "b".repeat(64));
        insert(40, 101, HASH);
        insert(50, 202, HASH);
        insert(60, 303, HASH);
        insert(70, 999, HASH);
        originalRows = rows();
        when(loader.load()).thenReturn(new GxpAuditPolicyBundle("gxp-audit-policy.v2", "test-v1", "DRAFT",
                "test-only", HASH, "c".repeat(64), "{}", "", "", null));
    }

    @AfterEach
    void closeOwnedDatabase() {
        TenantContextHolder.clear();
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void latestRowsAcrossAllTenantsThenNormalReadsRemainIsolated(boolean ignoredBefore) {
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(ignoredBefore);
        try (ConfigurableApplicationContext context = start()) {
            assertEquals(1, ready.get());
            assertEquals(1, jobs.get());
            assertEquals(999L, TenantContextHolder.getTenantId());
            assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
            GxpAuditPolicyActivationMapper mapper = context.getBean(GxpAuditPolicyActivationMapper.class);
            List<GxpAuditPolicyActivationDO> all = TenantUtils.executeIgnore(mapper::selectLatestAcrossTenants);
            assertEquals(List.of(40L, 50L, 60L, 70L), all.stream().map(GxpAuditPolicyActivationDO::getId).toList());
            assertEquals(List.of(101L, 202L, 303L, 999L), all.stream().map(GxpAuditPolicyActivationDO::getTenantId).toList());
            System.out.println("ACT01 actual latest tenant/id rows: 101/40,202/50,303/60,999/70");
            TenantContextHolder.setIgnore(false);
            assertEquals(List.of(70L), mapper.selectLatestAcrossTenants().stream()
                    .map(GxpAuditPolicyActivationDO::getId).toList());
            assertNull(mapper.selectLatest(101L), "restored tenant 999 must not read tenant 101");
            assertEquals(70L, mapper.selectLatest(999L).getId());
            assertEquals(originalRows, rows(), "startup and reads must not mutate any activation");
            assertEquals(1, context.getBean(MybatisPlusInterceptor.class).getInterceptors().stream()
                    .filter(TenantLineInnerInterceptor.class::isInstance).count());
            assertTrue(evidence.sql.stream().anyMatch(sql -> sql.contains("999")),
                    "actual prepared SQL must contain tenant filtering after ignore ends");
            assertTrue(evidence.bindings.stream().anyMatch(values -> values.contains(101L)),
                    "record actual parameter binding for isolated selectLatest");
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 202L, 303L})
    void matchingHistoryCannotHideMismatchingLatestForDisabledDeletedOrOrphanTenant(long tenant) {
        jdbc.update("UPDATE gxp_audit_policy_activation SET policy_hash=?", HASH);
        jdbc.update("UPDATE gxp_audit_policy_activation SET policy_hash=? WHERE tenant_id=? "
                + "AND id=(SELECT MAX(id) FROM gxp_audit_policy_activation WHERE tenant_id=?)",
                "b".repeat(64), tenant, tenant);
        List<Map<String, Object>> before = rows();
        TenantContextHolder.setTenantId(999L);
        RuntimeException failure = rejected();
        assertTrue(messages(failure).contains(Long.toString(tenant)));
        assertEquals(999L, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
        assertEquals(before, rows());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realSqlFailurePropagatesAndRestoresContext(boolean ignoredBefore) {
        jdbc.execute("DROP TABLE gxp_audit_policy_activation"); // only this test's UUID memory database
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(ignoredBefore);
        RuntimeException failure = rejected();
        assertTrue(messages(failure).toLowerCase().contains("gxp_audit_policy_activation"));
        assertEquals(999L, TenantContextHolder.getTenantId());
        assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
        assertFalse(evidence.sql.isEmpty(), "failure must reach actual JDBC preparation");
    }

    @Test
    void emptyActivationTableLoadsBundleAndDoesNotSeed() {
        jdbc.update("DELETE FROM gxp_audit_policy_activation");
        try (ConfigurableApplicationContext ignored = start()) {
            assertEquals(1, ready.get());
            assertNull(TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            assertTrue(rows().isEmpty());
            verify(loader).load();
        }
    }

    private ConfigurableApplicationContext start() {
        Long tenantBefore = TenantContextHolder.getTenantId();
        boolean ignoredBefore = TenantContextHolder.isIgnore();
        SpringApplication app = new SpringApplication(QuerySlice.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setRegisterShutdownHook(false);
        app.setDefaultProperties(Map.of("spring.config.name", "act01-isolated-query-test", "spring.main.banner-mode", "off"));
        app.addInitializers(context -> {
            context.getBeanFactory().registerSingleton("dataSource", dataSource);
            context.getBeanFactory().registerSingleton("sqlEvidence", evidence);
            context.getBeanFactory().registerSingleton("gxpAuditPolicyBundleLoader", loader);
            context.getBeanFactory().registerSingleton("queryJobSentinel", new SmartLifecycle() {
                private boolean running;
                @Override public void start() {
                    assertFalse(evidence.sql.isEmpty(), "actual query must complete before jobs start");
                    assertEquals(tenantBefore, TenantContextHolder.getTenantId());
                    assertEquals(ignoredBefore, TenantContextHolder.isIgnore());
                    jobs.incrementAndGet();
                    running = true;
                }
                @Override public void stop() { running = false; }
                @Override public boolean isRunning() { return running; }
                @Override public int getPhase() { return Integer.MIN_VALUE; }
            });
        });
        app.addListeners(event -> { if (event instanceof ApplicationReadyEvent) { ready.incrementAndGet(); } });
        return app.run();
    }

    private RuntimeException rejected() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ConfigurableApplicationContext ignored = start()) { assertNotNull(ignored); }
        });
        assertEquals(0, ready.get());
        assertEquals(0, jobs.get());
        return failure;
    }

    private void insert(long id, long tenant, String hash) {
        // Version/time order intentionally differs from identity order: latest means MAX(id).
        jdbc.update("INSERT INTO gxp_audit_policy_activation (id,tenant_id,policy_version,policy_hash,activated_at_utc) "
                + "VALUES (?,?,?,?,?)", id, tenant, id < 40 ? "z-old" : "a-new", hash,
                java.sql.Timestamp.valueOf(id < 40 ? "2030-01-01 00:00:00" : "2020-01-01 00:00:00"));
    }

    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("SELECT id,tenant_id,policy_version,policy_hash,activated_at_utc "
                + "FROM gxp_audit_policy_activation ORDER BY id");
    }

    private static String messages(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            result.append(cause.getMessage()).append('\n');
        }
        return result.toString();
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.iocoder.yudao.server.gxpaudit")
    static class QuerySlice {
        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            // Invoke the exact production installation method, without unrelated Web/Redis beans.
            new YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(new TenantProperties(), interceptor);
            return interceptor;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor interceptor,
                                           SqlEvidence evidence) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            factory.setPlugins(evidence, interceptor);
            return factory.getObject();
        }

        @Bean
        MapperFactoryBean<GxpAuditPolicyActivationMapper> gxpAuditPolicyActivationMapper(SqlSessionFactory factory) {
            MapperFactoryBean<GxpAuditPolicyActivationMapper> bean = new MapperFactoryBean<>(GxpAuditPolicyActivationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    static class SqlEvidence implements Interceptor {
        final List<String> sql = new ArrayList<>();
        final List<List<Object>> bindings = new ArrayList<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            BoundSql bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            try {
                return invocation.proceed();
            } finally {
                sql.add(bound.getSql());
                List<Object> values = new ArrayList<>();
                bound.getParameterMappings().forEach(mapping -> values.add(bound.hasAdditionalParameter(mapping.getProperty())
                        ? bound.getAdditionalParameter(mapping.getProperty())
                        : SystemMetaObject.forObject(bound.getParameterObject()).getValue(mapping.getProperty())));
                bindings.add(values);
                System.out.println("ACT01 actual SQL: " + bound.getSql() + " bindings=" + values);
            }
        }
    }
}
