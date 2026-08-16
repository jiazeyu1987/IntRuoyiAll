package cn.iocoder.yudao.module.mdm;

import cn.iocoder.yudao.module.mdm.dal.mysql.enterprise.MdmEnterpriseMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.h2.tools.RunScript;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdmEnterpriseSchemaContractTest {

    @Test
    void migrationAndFixturesMustOwnTheCompleteEnterpriseCompanyScopeSchema() throws Exception {
        Path backendRoot = resolveBackendRoot();
        Path migration = backendRoot.resolve("sql/mysql/20260816_mdm_enterprise_company_scope.sql");
        Path createFixture = backendRoot.resolve("yudao-module-mdm/src/test/resources/sql/create_tables.sql");
        Path cleanFixture = backendRoot.resolve("yudao-module-mdm/src/test/resources/sql/clean.sql");

        assertTrue(Files.exists(migration), "SP-00 enterprise/company migration must exist");
        assertTrue(Files.exists(createFixture), "MDM schema fixture must exist");
        assertTrue(Files.exists(cleanFixture), "MDM cleanup fixture must exist");

        String migrationSql = Files.readString(migration, StandardCharsets.UTF_8);
        String fixtureSql = Files.readString(createFixture, StandardCharsets.UTF_8);
        String cleanSql = Files.readString(cleanFixture, StandardCharsets.UTF_8);
        assertTrue(migrationSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260607_product_master_data; type=schema; riskLevel=medium"));
        assertCompleteSchema(migrationSql, "`", "UNIQUE KEY");
        assertCompleteSchema(fixtureSql, "\"", "CONSTRAINT");
        assertTrue(cleanSql.contains("DELETE FROM \"mdm_role_company_scope\""));
        assertTrue(cleanSql.contains("DELETE FROM \"mdm_user_company_scope\""));
        assertTrue(cleanSql.contains("DELETE FROM \"mdm_enterprise\""));
        assertFalse(migrationSql.contains("dept_id"));
        assertFalse(migrationSql.contains("department"));
        assertApprovedEnterpriseTypeContract(migrationSql, fixtureSql);
        assertTrue(migrationSql.contains("-- Recovery: MySQL DDL auto-commits"));
        assertTrue(migrationSql.contains("-- Rollback before business use:"));
        assertTrue(migrationSql.contains("-- Rollback after business use:"));
        assertTrue(migrationSql.contains("-- Unique-key policy: business keys remain permanently reserved after soft deletion"));
    }

    @Test
    void mysqlMigrationMustFailFastOnExistingSchemaMismatchBeforeCreatingMissingTables() throws Exception {
        String migrationSql = Files.readString(resolveBackendRoot()
                .resolve("sql/mysql/20260816_mdm_enterprise_company_scope.sql"), StandardCharsets.UTF_8);
        String normalizedSql = migrationSql.replaceAll("\\s+", " ");
        String procedure = "CREATE PROCEDURE assert_mdm_enterprise_company_scope_contract(IN require_complete BOOLEAN)";
        assertTrue(normalizedSql.contains(procedure));
        assertTrue(normalizedSql.contains("information_schema.TABLES"));
        assertTrue(normalizedSql.contains("information_schema.COLUMNS"));
        assertTrue(normalizedSql.contains("information_schema.STATISTICS"));
        assertTrue(normalizedSql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(normalizedSql.contains("COLUMN_TYPE"));
        assertTrue(normalizedSql.contains("IS_NULLABLE"));
        assertTrue(normalizedSql.contains("COLUMN_DEFAULT"));
        assertTrue(normalizedSql.contains("ORDINAL_POSITION"));
        assertTrue(normalizedSql.contains("NON_UNIQUE"));
        assertTrue(normalizedSql.contains("SEQ_IN_INDEX"));
        assertTrue(normalizedSql.contains("GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')"));
        assertTrue(normalizedSql.contains("REPLACE(check_constraint.CHECK_CLAUSE, CHAR(92), '')"),
                "MySQL 8 escapes CHECK string literals with backslashes in information_schema");
        assertTrue(normalizedSql.contains("uk_mdm_enterprise_tenant_code")
                && normalizedSql.contains("tenant_id,enterprise_code"));
        assertTrue(normalizedSql.contains("uk_mdm_user_company_scope_tenant_user_company")
                && normalizedSql.contains("tenant_id,user_id,company_id"));
        assertTrue(normalizedSql.contains("uk_mdm_role_company_scope_tenant_role_company")
                && normalizedSql.contains("tenant_id,role_id,company_id"));
        int preflightCall = normalizedSql.indexOf("CALL assert_mdm_enterprise_company_scope_contract(FALSE)");
        int firstCreate = normalizedSql.indexOf("CREATE TABLE IF NOT EXISTS `mdm_enterprise`");
        int postflightCall = normalizedSql.indexOf("CALL assert_mdm_enterprise_company_scope_contract(TRUE)");
        int lastCreate = normalizedSql.indexOf("CREATE TABLE IF NOT EXISTS `mdm_role_company_scope`");
        assertTrue(preflightCall > 0 && preflightCall < firstCreate,
                "existing tables must be validated before any missing table is created");
        assertTrue(postflightCall > lastCreate,
                "complete schema must be validated after all missing tables are created");
    }

    @Test
    void enterpriseClassificationMapperMustInspectRequestedIdsAcrossTenantAndLogicalDeleteBoundaries()
            throws Exception {
        Method method = assertDoesNotThrow(
                () -> MdmEnterpriseMapper.class.getMethod("selectClassificationByIds", java.util.Collection.class),
                "mapper must expose the raw classification query");
        assertEquals(1, method.getParameterCount(), "raw classification query must accept IDs only");
        InterceptorIgnore interceptorIgnore = method.getAnnotation(InterceptorIgnore.class);
        assertNotNull(interceptorIgnore, "classification query must explicitly document tenant interception bypass");
        assertEquals("true", interceptorIgnore.tenantLine());
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select, "classification query must be explicit annotation SQL");
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("select id, tenant_id"));
        assertTrue(sql.contains("deleted"));
        assertTrue(sql.contains("where id in"));
        assertFalse(sql.contains("tenant_id ="), "caller tenant must not shape raw classification rows");
        assertFalse(sql.contains("deleted ="), "deleted rows must remain visible for explicit classification");
    }

    @Test
    void fixtureLoadsAndTenantScopedBusinessKeysAllowOnlyOneConcurrentWinner() throws Exception {
        Path backendRoot = resolveBackendRoot();
        Path createFixture = backendRoot.resolve("yudao-module-mdm/src/test/resources/sql/create_tables.sql");
        Path cleanFixture = backendRoot.resolve("yudao-module-mdm/src/test/resources/sql/clean.sql");
        String jdbcUrl = "jdbc:h2:mem:mdm_enterprise_" + UUID.randomUUID()
                + ";MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            RunScript.execute(connection, Files.newBufferedReader(createFixture, StandardCharsets.UTF_8));
        }
        try {
            assertOneConcurrentWinner(jdbcUrl, """
                INSERT INTO "mdm_enterprise"
                    ("enterprise_code", "name", "type", "status", "revision", "tenant_id")
                VALUES ('COMP-CONCURRENT', 'Concurrent company', 'OWNED_COMPANY', 'ENABLE', 1, 11)
                """);
            assertOneConcurrentWinner(jdbcUrl, """
                INSERT INTO "mdm_user_company_scope"
                    ("user_id", "company_id", "status", "revision", "tenant_id")
                VALUES (701, 101, 'ENABLE', 1, 11)
                """);
            assertOneConcurrentWinner(jdbcUrl, """
                INSERT INTO "mdm_role_company_scope"
                    ("role_id", "company_id", "status", "revision", "tenant_id")
                VALUES (801, 101, 'ENABLE', 1, 11)
                """);
            executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_enterprise"
                    ("enterprise_code", "name", "type", "status", "revision", "tenant_id")
                VALUES ('COMP-CONCURRENT', 'Other tenant company', 'OWNED_COMPANY', 'ENABLE', 1, 12)
                """);
            executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_enterprise"
                    ("enterprise_code", "name", "type", "status", "revision", "tenant_id")
                VALUES ('TRUST-ALLOWED', 'Entrusted party', 'ENTRUSTED_PARTY', 'ENABLE', 1, 11)
                """);
            SQLException invalidType = assertThrows(SQLException.class, () -> executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_enterprise"
                    ("enterprise_code", "name", "type", "status", "revision", "tenant_id")
                VALUES ('OLD-TYPE', 'Old type', 'EXTERNAL_ENTERPRISE', 'ENABLE', 1, 11)
                """));
            assertEquals("23513", invalidType.getSQLState());
            executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_user_company_scope"
                    ("user_id", "company_id", "status", "revision", "tenant_id")
                VALUES (701, 101, 'ENABLE', 1, 12)
                """);
            executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_role_company_scope"
                    ("role_id", "company_id", "status", "revision", "tenant_id")
                VALUES (801, 101, 'ENABLE', 1, 12)
                """);
            assertEquals(3, countRows(jdbcUrl, "mdm_enterprise"));
            assertEquals(2, countRows(jdbcUrl, "mdm_user_company_scope"));
            assertEquals(2, countRows(jdbcUrl, "mdm_role_company_scope"));
            assertSoftDeletedBusinessKeyRemainsReserved(jdbcUrl,
                    "UPDATE \"mdm_enterprise\" SET \"deleted\" = TRUE WHERE \"tenant_id\" = 11 "
                            + "AND \"enterprise_code\" = 'COMP-CONCURRENT'",
                    """
                    INSERT INTO "mdm_enterprise"
                        ("enterprise_code", "name", "type", "status", "revision", "tenant_id")
                    VALUES ('COMP-CONCURRENT', 'Replacement company', 'OWNED_COMPANY', 'ENABLE', 1, 11)
                    """);
            assertSoftDeletedBusinessKeyRemainsReserved(jdbcUrl,
                    "UPDATE \"mdm_user_company_scope\" SET \"deleted\" = TRUE WHERE \"tenant_id\" = 11 "
                            + "AND \"user_id\" = 701 AND \"company_id\" = 101",
                    """
                    INSERT INTO "mdm_user_company_scope"
                        ("user_id", "company_id", "status", "revision", "tenant_id")
                    VALUES (701, 101, 'ENABLE', 1, 11)
                    """);
            assertSoftDeletedBusinessKeyRemainsReserved(jdbcUrl,
                    "UPDATE \"mdm_role_company_scope\" SET \"deleted\" = TRUE WHERE \"tenant_id\" = 11 "
                            + "AND \"role_id\" = 801 AND \"company_id\" = 101",
                    """
                    INSERT INTO "mdm_role_company_scope"
                        ("role_id", "company_id", "status", "revision", "tenant_id")
                    VALUES (801, 101, 'ENABLE', 1, 11)
                    """);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
                RunScript.execute(connection, Files.newBufferedReader(cleanFixture, StandardCharsets.UTF_8));
            }
            assertEquals(0, countRows(jdbcUrl, "mdm_enterprise"));
            assertEquals(0, countRows(jdbcUrl, "mdm_user_company_scope"));
            assertEquals(0, countRows(jdbcUrl, "mdm_role_company_scope"));
        } finally {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
            }
        }
    }

    private int executeUpdate(String jdbcUrl, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        }
    }

    private int countRows(String jdbcUrl, String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void assertOneConcurrentWinner(String jdbcUrl, String insertSql) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> contender = () -> {
            start.await(5, TimeUnit.SECONDS);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                 PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.executeUpdate();
                return "SUCCESS";
            } catch (SQLException exception) {
                if ("23505".equals(exception.getSQLState())) {
                    return "DUPLICATE";
                }
                throw exception;
            }
        };
        try {
            Future<String> first = executor.submit(contender);
            Future<String> second = executor.submit(contender);
            start.countDown();
            List<String> outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter("SUCCESS"::equals).count());
            assertEquals(1, outcomes.stream().filter("DUPLICATE"::equals).count());
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertSoftDeletedBusinessKeyRemainsReserved(String jdbcUrl, String softDeleteSql,
                                                               String duplicateInsertSql) throws Exception {
        assertEquals(1, executeUpdate(jdbcUrl, softDeleteSql));
        SQLException duplicate = assertThrows(SQLException.class, () -> executeUpdate(jdbcUrl, duplicateInsertSql));
        assertEquals("23505", duplicate.getSQLState());
    }

    private void assertCompleteSchema(String sql, String quote, String uniqueKeyword) {
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + quote + "mdm_enterprise" + quote));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + quote + "mdm_user_company_scope" + quote));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + quote + "mdm_role_company_scope" + quote));
        assertTrue(sql.contains(quote + "enterprise_code" + quote));
        assertTrue(sql.contains(quote + "type" + quote));
        assertTrue(sql.contains(quote + "status" + quote));
        assertTrue(sql.contains(quote + "revision" + quote));
        assertTrue(sql.contains(uniqueKeyword + " " + quote + "uk_mdm_enterprise_tenant_code" + quote));
        assertTrue(sql.contains(quote + "tenant_id" + quote + ", " + quote + "enterprise_code" + quote));
        assertTrue(sql.contains(uniqueKeyword + " " + quote
                + "uk_mdm_user_company_scope_tenant_user_company" + quote));
        assertTrue(sql.contains(quote + "tenant_id" + quote + ", " + quote + "user_id" + quote
                + ", " + quote + "company_id" + quote));
        assertTrue(sql.contains(uniqueKeyword + " " + quote
                + "uk_mdm_role_company_scope_tenant_role_company" + quote));
        assertTrue(sql.contains(quote + "tenant_id" + quote + ", " + quote + "role_id" + quote
                + ", " + quote + "company_id" + quote));
    }

    private void assertApprovedEnterpriseTypeContract(String migrationSql, String fixtureSql) {
        assertEquals(Set.of("OWNED_COMPANY", "ENTRUSTED_PARTY"),
                Arrays.stream(MdmEnterpriseTypeEnum.values()).map(MdmEnterpriseTypeEnum::getType)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(MdmEnterpriseTypeEnum.isValid("OWNED_COMPANY"));
        assertTrue(MdmEnterpriseTypeEnum.isValid("ENTRUSTED_PARTY"));
        assertFalse(MdmEnterpriseTypeEnum.isValid("EXTERNAL_ENTERPRISE"));
        assertTrue(migrationSql.contains("CHECK (`type` IN ('OWNED_COMPANY', 'ENTRUSTED_PARTY'))"));
        assertTrue(fixtureSql.contains("CHECK (\"type\" IN ('OWNED_COMPANY', 'ENTRUSTED_PARTY'))"));
        assertFalse(migrationSql.contains("EXTERNAL_ENTERPRISE"));
        assertFalse(fixtureSql.contains("EXTERNAL_ENTERPRISE"));
    }

    private Path resolveBackendRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("yudao-module-mdm/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("INTRUOYI_BACKEND_ROOT_MISSING");
    }

}
