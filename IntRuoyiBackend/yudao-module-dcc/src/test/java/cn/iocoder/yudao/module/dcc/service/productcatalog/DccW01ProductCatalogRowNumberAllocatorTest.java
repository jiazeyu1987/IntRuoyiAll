package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Dedicated H2 fixture; no changes to public schema, no live database or shared Maven target. */
class DccW01ProductCatalogRowNumberAllocatorTest {
    static final AtomicInteger DATABASE_IDS = new AtomicInteger();
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    DccProductCatalogRowNumberAllocator allocator;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false);
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:w01_sequence_" + DATABASE_IDS.incrementAndGet() + ";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        jdbc = new JdbcTemplate(source); tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        String fixture;
        try (var input = getClass().getResourceAsStream("/sql/create_tables.sql")) {
            if (input == null) throw new IllegalStateException("Missing existing H2 catalog fixture resource");
            fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String marker = "CREATE TABLE IF NOT EXISTS `dcc_product_catalog`";
        int start = fixture.indexOf(marker), end = fixture.indexOf(';', start);
        if (start < 0 || end < start) throw new IllegalStateException("Missing existing H2 catalog schema");
        jdbc.execute(fixture.substring(start, end + 1));
        assertTrue(fixture.substring(start, end + 1).contains("uk_dcc_product_catalog_source_row"), "public fixture must retain actual unique scope");
        int sequenceStart = fixture.indexOf("CREATE TABLE IF NOT EXISTS dcc_product_catalog_source_sequence"), sequenceEnd = fixture.indexOf(';', sequenceStart);
        if (sequenceStart < 0 || sequenceEnd < sequenceStart) throw new IllegalStateException("Missing IC12 shared sequence schema");
        jdbc.execute(fixture.substring(sequenceStart, sequenceEnd + 1));
        allocator = new DccProductCatalogRowNumberAllocator(); ReflectionTestUtils.setField(allocator, "jdbc", jdbc);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    void sequence(String source, long last) {
        jdbc.update("INSERT INTO dcc_product_catalog_source_sequence(data_source,last_row_no) VALUES(?,?)", source, last);
    }
    void catalog(String source, int row, long tenant, boolean deleted) {
        jdbc.update("INSERT INTO dcc_product_catalog(data_source,original_row_no,product,tenant_id,deleted) VALUES(?,?,?,?,?)",
                source, row, "正式产品" + row, tenant, deleted);
    }
    long last(String source) { return jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source=?", Long.class, source); }
    Future<Integer> asyncWrite(ExecutorService pool, String source, long tenant, CountDownLatch allocated, CountDownLatch release,
                               CountDownLatch attempted) {
        return pool.submit(() -> {
            TenantContextHolder.setTenantId(tenant); TenantContextHolder.setIgnore(false);
            if (attempted != null) attempted.countDown();
            try { return tx.execute(status -> {
                int row = allocator.allocate(source);
                if (allocated != null) allocated.countDown();
                if (release != null) await(release);
                catalog(source, row, tenant, false); return row;
            }); } finally { TenantContextHolder.clear(); }
        });
    }
    static void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test lock release timed out"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
    }
    @Test void sameSourceAcrossTenantsLocksUntilCommitThenUsesDistinctNumbers() throws Exception {
        catalog("瑛泰产品", 2, 1, false); catalog("瑛泰产品", 186, 2, true); sequence("瑛泰产品", 186);
        var pool = Executors.newFixedThreadPool(2); var allocated = new CountDownLatch(1); var release = new CountDownLatch(1);
        try {
            var first = asyncWrite(pool, "瑛泰产品", 1, allocated, release, null); assertTrue(allocated.await(10, TimeUnit.SECONDS));
            var attempted = new CountDownLatch(1);
            var second = asyncWrite(pool, "瑛泰产品", 2, null, null, attempted);
            assertTrue(attempted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            release.countDown(); assertEquals(187, first.get(10, TimeUnit.SECONDS)); assertEquals(188, second.get(10, TimeUnit.SECONDS));
            assertEquals(188L, last("瑛泰产品")); assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog WHERE original_row_no=186 AND tenant_id=2 AND deleted=1", Integer.class));
        } finally { release.countDown(); pool.shutdownNow(); }
    }
    @Test void differentSourcesDoNotWaitOnEachOthersSequenceRow() throws Exception {
        sequence("瑛泰产品", 186); sequence("隔离来源B", 1);
        var pool = Executors.newFixedThreadPool(2); var allocated = new CountDownLatch(1); var release = new CountDownLatch(1);
        try {
            var first = asyncWrite(pool, "瑛泰产品", 1, allocated, release, null); assertTrue(allocated.await(10, TimeUnit.SECONDS));
            var second = asyncWrite(pool, "隔离来源B", 2, null, null, null);
            assertEquals(2, second.get(5, TimeUnit.SECONDS)); release.countDown(); assertEquals(187, first.get(10, TimeUnit.SECONDS));
        } finally { release.countDown(); pool.shutdownNow(); }
    }
    @Test void failedBusinessTransactionRollsBackCatalogAndSequenceThenReusesUncommittedNumber() {
        sequence("瑛泰产品", 186);
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            int row = allocator.allocate("瑛泰产品"); assertEquals(187, row); catalog("瑛泰产品", row, 1, false);
            throw new IllegalStateException("Actual downstream failure");
        }));
        assertEquals(186L, last("瑛泰产品")); assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog", Integer.class));
        int next = tx.execute(status -> allocator.allocate("瑛泰产品")); assertEquals(187, next);
    }
    @Test void emptyInitializedSourceStartsAtHistoricalExcelRowTwo() {
        sequence("瑛泰产品", 1); int next = tx.execute(status -> allocator.allocate("瑛泰产品")); assertEquals(2, next);
    }
    @Test void missingSequenceRejectsWithoutLazyMaxOrInitialization() {
        catalog("瑛泰产品", 186, 1, false);
        var error = assertThrows(IllegalStateException.class, () -> tx.execute(status -> allocator.allocate("瑛泰产品")));
        assertTrue(error.getMessage().contains("NOT_INITIALIZED"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog_source_sequence", Integer.class));
    }
    @Test void overflowAndInvalidHighWaterFailWithoutAnyWrite() {
        for (long value : new long[] {0, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            jdbc.update("DELETE FROM dcc_product_catalog_source_sequence"); sequence("瑛泰产品", value);
            assertThrows(IllegalStateException.class, () -> tx.execute(status -> allocator.allocate("瑛泰产品")));
            assertEquals(value, last("瑛泰产品"));
        }
    }
    @Test void foreignWriterUniqueCollisionPropagatesAndRollsBackAllocatedHighWater() {
        sequence("瑛泰产品", 186); catalog("瑛泰产品", 187, 2, false);
        assertThrows(DuplicateKeyException.class, () -> tx.execute(status -> {
            int next = allocator.allocate("瑛泰产品"); catalog("瑛泰产品", next, 1, false); return next;
        }));
        assertEquals(186L, last("瑛泰产品")); assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog", Integer.class));
    }
    @Test void noTransactionIgnoredTenantAndMalformedSourceRejectBeforeAllocation() {
        sequence("瑛泰产品", 186);
        assertThrows(IllegalStateException.class, () -> allocator.allocate("瑛泰产品"));
        TenantContextHolder.setIgnore(true);
        assertThrows(IllegalArgumentException.class, () -> tx.execute(status -> allocator.allocate("瑛泰产品")));
        TenantContextHolder.setIgnore(false);
        for (String invalid : Arrays.asList(null, "", " 瑛泰产品", "瑛泰产品 ", "x".repeat(65)))
            assertThrows(IllegalArgumentException.class, () -> tx.execute(status -> allocator.allocate(invalid)));
        assertEquals(186L, last("瑛泰产品"));
    }
}
