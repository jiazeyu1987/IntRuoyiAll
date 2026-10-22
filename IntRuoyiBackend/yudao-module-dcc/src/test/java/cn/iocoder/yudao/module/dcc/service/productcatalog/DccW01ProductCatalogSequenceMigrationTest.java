package cn.iocoder.yudao.module.dcc.service.productcatalog;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the formal DML/predicates on H2; MySQL information_schema/routine/collation checks remain static. */
class DccW01ProductCatalogSequenceMigrationTest {
    static final AtomicInteger IDS = new AtomicInteger();
    JdbcTemplate jdbc;
    String migration;
    @BeforeEach void setup() throws Exception {
        Path repository = Path.of(System.getProperty("w01.repository", System.getProperty("user.dir"))).toAbsolutePath();
        while (repository != null && !Files.exists(repository.resolve("IntRuoyiBackend/sql/mysql/20261011_dcc_product_catalog_source_sequence.sql"))) repository = repository.getParent();
        if (repository == null) throw new IllegalStateException("Formal IC12 migration missing");
        migration = Files.readString(repository.resolve("IntRuoyiBackend/sql/mysql/20261011_dcc_product_catalog_source_sequence.sql"), StandardCharsets.UTF_8);
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:w01_sequence_migration_" + IDS.incrementAndGet() + ";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        String fixture;
        try (var input = getClass().getResourceAsStream("/sql/create_tables.sql")) {
            if (input == null) throw new IllegalStateException("Missing catalog fixture");
            fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int start = fixture.indexOf("CREATE TABLE IF NOT EXISTS `dcc_product_catalog`"), end = fixture.indexOf(';', start);
        jdbc.execute(fixture.substring(start, end + 1));
        String create = extract("CREATE TABLE IF NOT EXISTS dcc_product_catalog_source_sequence \\([\\s\\S]*?;", 0)
                .replace("CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", "")
                .replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
        jdbc.execute(create); // Only H2 syntax adaptation, no replacement initialization algorithm.
    }
    String extract(String regex, int group) {
        var matcher = Pattern.compile(regex).matcher(migration);
        if (!matcher.find()) throw new IllegalStateException("Missing formal migration contract: " + regex);
        return matcher.group(group);
    }
    void initialize() {
        var statements = Pattern.compile("INSERT INTO dcc_product_catalog_source_sequence\\(data_source,last_row_no\\)[\\s\\S]*?;").matcher(migration);
        int count = 0;
        while (statements.find()) { jdbc.update(statements.group()); count++; }
        assertEquals(2, count, "all formal initialization DML must execute");
    }
    void catalog(String source, int row, long tenant, boolean deleted) {
        jdbc.update("INSERT INTO dcc_product_catalog(data_source,original_row_no,product,tenant_id,deleted) VALUES(?,?,?,?,?)",
                source, row, "历史产品" + row, tenant, deleted);
    }
    long last(String source) { return jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source=?", Long.class, source); }
    @Test void formalInitializationIncludesEveryTenantAndDeletedHighestRowWithoutTouchingCatalog() {
        catalog("瑛泰产品", 4, 1, false); catalog("瑛泰产品", 186, 2, true); catalog("旧来源B", 99, 3, true);
        String before = jdbc.queryForList("SELECT * FROM dcc_product_catalog ORDER BY id").toString();
        initialize();
        assertEquals(186L, last("瑛泰产品")); assertEquals(99L, last("旧来源B"));
        assertEquals(before, jdbc.queryForList("SELECT * FROM dcc_product_catalog ORDER BY id").toString());
    }
    @Test void repeatInitializationKeepsAllocatedHighWaterAndPreservesEveryHistoricalColumn() {
        catalog("瑛泰产品", 186, 2, true); initialize();
        jdbc.update("UPDATE dcc_product_catalog_source_sequence SET last_row_no=205 WHERE data_source='瑛泰产品'");
        String before = jdbc.queryForList("SELECT * FROM dcc_product_catalog ORDER BY id").toString();
        initialize(); initialize();
        assertEquals(205L, last("瑛泰产品"));
        assertEquals(before, jdbc.queryForList("SELECT * FROM dcc_product_catalog ORDER BY id").toString());
    }
    @Test void emptySupportedSourceRetainsHistoricalFloorOneAndNextRowTwo() {
        initialize(); assertEquals(1L, last("瑛泰产品"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog_source_sequence", Integer.class));
    }
    @Test void formalIllegalFactsPredicateRejectsWhitespaceAndNonPositiveHistoricalRows() {
        String predicate = extract("IF EXISTS\\((SELECT 1 FROM dcc_product_catalog[\\s\\S]*?)\\)\\s+OR EXISTS", 1)
                .replace("[[:space:]]", "\\s"); // MySQL ICU POSIX whitespace vs H2 Java regex syntax.
        for (String source : new String[] {"", " 瑛泰产品", "瑛泰产品 ", "\t瑛泰产品"}) {
            jdbc.update("DELETE FROM dcc_product_catalog"); catalog(source, 7, 2, true);
            assertFalse(jdbc.queryForList(predicate).isEmpty(), source);
        }
        jdbc.update("DELETE FROM dcc_product_catalog"); catalog("瑛泰产品", 0, 2, true);
        assertFalse(jdbc.queryForList(predicate).isEmpty());
        jdbc.update("DELETE FROM dcc_product_catalog"); catalog("瑛泰产品", 186, 2, true);
        assertTrue(jdbc.queryForList(predicate).isEmpty());
    }
    @Test void formalSequencePredicateRejectsIllegalExistingWaterBeforeAnyDml() {
        String predicate = extract("IF EXISTS\\((SELECT 1 FROM dcc_product_catalog_source_sequence[\\s\\S]*?)\\) THEN", 1);
        for (long water : new long[] {0, -1, 2147483648L}) {
            jdbc.update("DELETE FROM dcc_product_catalog_source_sequence");
            jdbc.update("INSERT INTO dcc_product_catalog_source_sequence(data_source,last_row_no) VALUES('瑛泰产品',?)", water);
            assertFalse(jdbc.queryForList(predicate).isEmpty());
        }
    }
}
