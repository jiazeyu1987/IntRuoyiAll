package cn.iocoder.yudao.module.mdm;

import org.junit.jupiter.api.Test;
import org.h2.tools.RunScript;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                INSERT INTO "mdm_user_company_scope"
                    ("user_id", "company_id", "status", "revision", "tenant_id")
                VALUES (701, 101, 'ENABLE', 1, 12)
                """);
            executeUpdate(jdbcUrl, """
                INSERT INTO "mdm_role_company_scope"
                    ("role_id", "company_id", "status", "revision", "tenant_id")
                VALUES (801, 101, 'ENABLE', 1, 12)
                """);
            assertEquals(2, countRows(jdbcUrl, "mdm_enterprise"));
            assertEquals(2, countRows(jdbcUrl, "mdm_user_company_scope"));
            assertEquals(2, countRows(jdbcUrl, "mdm_role_company_scope"));
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

    private void executeUpdate(String jdbcUrl, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
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
