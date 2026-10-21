package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePreviewViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePreviewViewSessionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.LocalCacheScope;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicit opt-in: real InnoDB, real service proxy and mapper; never the running business database. */
class DccReadingSessionMysqlIT {
    static final String DATABASE = "dcc_e24_reading_lock_test_20261020";
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 4, 0);
    static final DccRequestAuditContext AUDIT = new DccRequestAuditContext("127.0.0.1", "E24 isolated JUnit", "e24-lock-test");
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    DccOnlineFilePreviewReadingSessionService service;
    ExecutorService pool;
    volatile CyclicBarrier afterClientRead;
    volatile CountDownLatch inserted, releaseInsert, attemptedLock;

    @BeforeEach void setup() throws Exception {
        String url = System.getenv("DCC_READING_TEST_JDBC_URL");
        assertNotNull(url, "Root must provision the task-only MySQL database explicitly");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:23306/" + DATABASE + "(?:\\?.*)?"), "Business database URL forbidden");
        var ds = new DriverManagerDataSource(url, System.getenv("DCC_READING_TEST_JDBC_USER"), System.getenv("DCC_READING_TEST_JDBC_PASSWORD"));
        jdbc = new JdbcTemplate(ds);
        assertEquals(DATABASE, jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals("REPEATABLE-READ", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
        jdbc.execute("CREATE TABLE IF NOT EXISTS infra_file(id BIGINT PRIMARY KEY, deleted TINYINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
        Path migration = Path.of("sql/mysql/20261015_dcc_preview_reading_session.sql");
        if (!Files.exists(migration)) migration = Path.of("../sql/mysql/20261015_dcc_preview_reading_session.sql");
        jdbc.execute(Files.readString(migration));
        jdbc.update("DELETE FROM dcc_controlled_file_preview_view_session");
        jdbc.update("DELETE FROM infra_file");
        jdbc.update("INSERT INTO infra_file(id) VALUES(7001),(7002)");
        var manager = new DataSourceTransactionManager(ds);
        tx = new TransactionTemplate(manager);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setLocalCacheScope(LocalCacheScope.STATEMENT);
        config.addMapper(DccControlledFilePreviewViewSessionMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(config);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
        var actual = new SqlSessionTemplate(factory.getObject()).getMapper(DccControlledFilePreviewViewSessionMapper.class);
        var mapper = mock(DccControlledFilePreviewViewSessionMapper.class, call -> {
            String name = call.getMethod().getName();
            if (name.equals("lockInfraFile") && attemptedLock != null) attemptedLock.countDown();
            Object result;
            try { result = call.getMethod().invoke(actual, call.getArguments()); }
            catch (InvocationTargetException error) { throw error.getCause(); }
            if (name.startsWith("selectClient") && afterClientRead != null) afterClientRead.await(10, TimeUnit.SECONDS);
            if (name.equals("insert") && call.getArguments().length == 1 && inserted != null) {
                inserted.countDown(); assertTrue(releaseInsert.await(10, TimeUnit.SECONDS));
            }
            return result;
        });
        var target = new DccOnlineFilePreviewReadingSessionService();
        ReflectionTestUtils.setField(target, "sessionMapper", mapper);
        ReflectionTestUtils.setField(target, "clock", Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        var events = mock(DccControlledFileAccessEventMapper.class);
        when(events.selectOne(any())).thenAnswer(call -> {
            // An ordinary authorization SELECT establishes the same RR snapshot as production.
            jdbc.queryForObject("SELECT COUNT(*) FROM infra_file", Long.class);
            return event(currentFile.get());
        });
        ReflectionTestUtils.setField(target, "accessEventMapper", events);
        var traces = mock(DccControlledFileWatermarkTraceMapper.class);
        when(traces.selectOne(any())).thenAnswer(call -> trace(currentFile.get()));
        ReflectionTestUtils.setField(target, "watermarkTraceMapper", traces);
        ReflectionTestUtils.setField(target, "viewerTokenService", mock(DccViewerTokenService.class));
        ReflectionTestUtils.setField(target, "businessFileAccessService", mock(BusinessFileAccessService.class));
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        service = (DccOnlineFilePreviewReadingSessionService) proxy.getProxy();
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach void close() throws Exception {
        if (releaseInsert != null) releaseInsert.countDown();
        if (pool != null) { pool.shutdownNow(); assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS)); }
        TenantContextHolder.clear(); currentFile.remove();
    }

    @Test void differentFilesStartWithoutMutualGapLockDeadlock() throws Exception {
        afterClientRead = new CyclicBarrier(2);
        var first = asyncStart(7001, "first");
        var second = asyncStart(7002, "second");
        assertNotNull(first.get(15, TimeUnit.SECONDS).sessionId());
        assertNotNull(second.get(15, TimeUnit.SECONDS).sessionId());
        assertEquals(2, count("active_marker=1"));
    }

    @Test void sameFileWaiterReplacesCommittedSessionAndStoppedReplayNeverReopens() throws Exception {
        inserted = new CountDownLatch(1); releaseInsert = new CountDownLatch(1);
        var first = asyncStart(7001, "first");
        assertTrue(inserted.await(10, TimeUnit.SECONDS));
        attemptedLock = new CountDownLatch(1);
        var second = asyncStart(7001, "second");
        assertTrue(attemptedLock.await(10, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS));
        releaseInsert.countDown();
        var old = first.get(15, TimeUnit.SECONDS);
        var current = second.get(15, TimeUnit.SECONDS);
        assertNotEquals(old.sessionId(), current.sessionId());
        assertEquals(1, count("active_marker=1"));
        assertEquals(1, count("ended_reason='REPLACED' AND accumulated_seconds=0 AND ended_at=last_heartbeat_at"));
        withFile(7001, () -> {
            assertEquals(old.sessionId(), service.start(99L, 7001L, request(7001, "first"), AUDIT).sessionId());
            service.stop(99L, 7001L, new DccPreviewReadingSessionStopReqVO(current.sessionId(), "token", "event-7001", "trace-7001", "token-id", "nonce"), AUDIT);
            assertNotNull(service.start(99L, 7001L, request(7001, "second"), AUDIT).endedAt());
            return null;
        });
        assertEquals(2, count("1=1")); assertEquals(0, count("active_marker=1"));
    }

    @Test void callerSnapshotCannotHideACommittedClientReplay() throws Exception {
        withFile(7001, () -> tx.execute(status -> {
            assertEquals(0, count("1=1")); // Outer RR snapshot predates the independent committed start.
            try {
                var committed = asyncStart(7001, "same-client").get(15, TimeUnit.SECONDS);
                var replay = service.start(99L, 7001L, request(7001, "same-client"), AUDIT);
                assertEquals(committed.sessionId(), replay.sessionId());
            } catch (Exception error) { throw new AssertionError(error); }
            return null;
        }));
        assertEquals(1, count("1=1"));
    }

    static final ThreadLocal<Long> currentFile = new ThreadLocal<>();
    <T> T withFile(long file, Callable<T> action) throws Exception {
        currentFile.set(file); TenantContextHolder.setTenantId(31L);
        try { return action.call(); } finally { TenantContextHolder.clear(); currentFile.remove(); }
    }
    Future<DccPreviewReadingSessionRespVO> asyncStart(long file, String client) {
        return pool.submit(() -> withFile(file, () -> service.start(99L, file, request(file, client), AUDIT)));
    }
    int count(String condition) { return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_preview_view_session WHERE " + condition, Integer.class); }
    static DccPreviewReadingSessionStartReqVO request(long file, String client) {
        return new DccPreviewReadingSessionStartReqVO(client, "token", "event-" + file, "trace-" + file, "token-id", "nonce");
    }
    static DccControlledFileAccessEventDO event(long file) {
        return DccControlledFileAccessEventDO.builder().id(file + 1000).tenantId(31L).controlledFileId(file).fileVersionNo("INFRA_FILE:" + file)
                .accessEventCode("event-" + file).userId(99L).accessType("PREVIEW").purpose("ONLINE_FILE_PREVIEW").result("SUCCESS").occurredAt(NOW.minusSeconds(30)).build();
    }
    static DccControlledFileWatermarkTraceDO trace(long file) {
        return DccControlledFileWatermarkTraceDO.builder().traceCode("trace-" + file).accessEventId(file + 1000).accessEventCode("event-" + file)
                .controlledFileId(file).fileVersionNo("INFRA_FILE:" + file).userId(99L).build();
    }
}
