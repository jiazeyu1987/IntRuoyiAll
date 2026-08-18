package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateReminderSchemaTest extends BaseDbUnitTest {

    private static final Set<String> REMINDER_TABLES = Set.of(
            "dcc_registration_certificate_reminder_config",
            "dcc_registration_certificate_daily_run",
            "dcc_registration_certificate_reminder_occurrence",
            "dcc_registration_certificate_reminder_delivery");

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+if\\s+not\\s+exists\\s+`([^`]+)`");

    @Resource
    private DataSource dataSource;

    @Test
    void migrationShouldDeclareReminderTablesJobSeedAndFailFastContracts() throws Exception {
        Path backendRoot = findBackendRoot();
        Path migration = backendRoot.resolve("sql/mysql/20260818_dcc_registration_certificate_reminder.sql");
        assertTrue(Files.isRegularFile(migration), "T07-S reminder migration must exist");

        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        String normalized = sql.toLowerCase(Locale.ROOT);
        assertTrue(normalized.startsWith("-- release-migration: allowedenvironments=test,backup,prod; "
                        + "dependson=20260818_dcc_registration_certificate_lifecycle; type=schema; risklevel=high"),
                "reminder migration metadata and direct dependency must match the frozen contract");
        assertEquals(REMINDER_TABLES, createdReminderTables(sql),
                "T07-S must create exactly the shared reminder config/run/occurrence/delivery tables");
        assertTrue(normalized.contains("signal sqlstate '45000'"),
                "partial or incompatible reminder schema must fail fast");

        assertContainsAll(normalized,
                "assert_dcc_registration_certificate_reminder_contract",
                "dcc registration certificate reminder requires lifecycle schema",
                "dcc registration certificate reminder requires infra job schema",
                "dcc registration certificate reminder partial schema detected",
                "dcc registration certificate reminder column contract mismatch",
                "dcc registration certificate reminder index contract mismatch",
                "unique key `uk_dcc_reg_cert_reminder_config_active` (`tenant_id`, `active_unique_flag`)",
                "unique key `uk_dcc_reg_cert_daily_run_date` (`tenant_id`, `business_date`)",
                "unique key `uk_dcc_reg_cert_daily_run_key` (`tenant_id`, `run_key`)",
                "unique key `uk_dcc_reg_cert_reminder_occurrence_key` (`tenant_id`, `event_key`)",
                "unique key `uk_dcc_reg_cert_reminder_occurrence_run` (`tenant_id`, `run_id`, `certificate_id`, `reminder_type`, `threshold_level`)",
                "unique key `uk_dcc_reg_cert_reminder_delivery_key` (`tenant_id`, `delivery_key`)",
                "unique key `uk_dcc_reg_cert_reminder_delivery_recipient` (`tenant_id`, `occurrence_id`, `recipient_user_id`)",
                "constraint `chk_dcc_reg_cert_reminder_config_time` check",
                "constraint `chk_dcc_reg_cert_daily_run_status` check",
                "constraint `chk_dcc_reg_cert_daily_run_failure` check",
                "constraint `chk_dcc_reg_cert_reminder_occurrence_type` check",
                "constraint `chk_dcc_reg_cert_reminder_occurrence_threshold` check",
                "constraint `chk_dcc_reg_cert_reminder_occurrence_status` check",
                "constraint `chk_dcc_reg_cert_reminder_occurrence_suppression` check",
                "constraint `chk_dcc_reg_cert_reminder_delivery_status` check",
                "constraint `chk_dcc_reg_cert_reminder_delivery_message` check",
                "registrationcertificatereminderdailyjob",
                "0 0 9 * * ?");
        assertFalse(normalized.contains("'other'"), "reminder persisted code sets must not define OTHER");
    }

    @Test
    void h2FixtureShouldLoadReminderTablesAndEnforcePortableConstraints() throws Exception {
        Set<String> present = new LinkedHashSet<>();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT LOWER(TABLE_NAME)
                       FROM INFORMATION_SCHEMA.TABLES
                      WHERE LOWER(TABLE_NAME) LIKE 'dcc_registration_certificate_reminder%'
                         OR LOWER(TABLE_NAME) = 'dcc_registration_certificate_daily_run'
                     """)) {
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    present.add(resultSet.getString(1));
                }
            }
        }
        assertEquals(REMINDER_TABLES, present,
                "H2 fixture must include every shared reminder table and no extra reminder tables");

        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_config
                      (id, tenant_id, active_unique_flag, enabled, daily_run_time, timezone,
                       threshold_days_json, row_version)
                    VALUES (?, 1, 1, TRUE, ?, 'Asia/Shanghai', '[30,8,2,1]', 1)
                    """)) {
                statement.setLong(1, 1L);
                statement.setString(2, "09:00");
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 2L);
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_config
                      (id, tenant_id, active_unique_flag, enabled, daily_run_time, timezone,
                       threshold_days_json, row_version)
                    VALUES (3, 2, 2, TRUE, '9:00', 'Asia/Shanghai', '[30,8,2,1]', 1)
                    """)) {
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_daily_run
                      (id, tenant_id, business_date, run_key, status, retry_count, started_at, detail_json)
                    VALUES (?, 1, ?, ?, ?, 0, ?, '{}')
                    """)) {
                statement.setLong(1, 10L);
                statement.setDate(2, java.sql.Date.valueOf(LocalDate.of(2026, 8, 18)));
                statement.setString(3, "daily:20260818");
                statement.setString(4, "RUNNING");
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 9, 0)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 11L);
                statement.setString(3, "daily:20260818:duplicate");
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_daily_run
                      (id, tenant_id, business_date, run_key, status, retry_count, started_at, detail_json)
                    VALUES (12, 1, ?, 'daily:failed', 'FAILED', 1, ?, '{}')
                    """)) {
                statement.setDate(1, java.sql.Date.valueOf(LocalDate.of(2026, 8, 19)));
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2026, 8, 19, 9, 0)));
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_occurrence
                      (id, tenant_id, run_id, owner_company_id, certificate_id, version_id,
                       reminder_type, threshold_level, business_date, due_date, event_key,
                       status, detail_json)
                    VALUES (?, 1, 10, 100, 1001, 2001, 'CERTIFICATE_EXPIRY', 'T_30',
                       ?, ?, ?, 'PENDING_DELIVERY', '{}')
                    """)) {
                statement.setLong(1, 20L);
                statement.setDate(2, java.sql.Date.valueOf(LocalDate.of(2026, 8, 18)));
                statement.setDate(3, java.sql.Date.valueOf(LocalDate.of(2026, 9, 17)));
                statement.setString(4, "occurrence:1001:T_30");
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 21L);
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_occurrence
                      (id, tenant_id, run_id, owner_company_id, certificate_id, version_id,
                       reminder_type, threshold_level, business_date, due_date, event_key,
                       status, detail_json)
                    VALUES (22, 1, 10, 100, 1001, 2001, 'CERTIFICATE_EXPIRY', 'T_8',
                       ?, ?, 'occurrence:1001:T_8', 'SUPPRESSED', '{}')
                    """)) {
                statement.setDate(1, java.sql.Date.valueOf(LocalDate.of(2026, 8, 18)));
                statement.setDate(2, java.sql.Date.valueOf(LocalDate.of(2026, 8, 26)));
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_delivery
                      (id, tenant_id, occurrence_id, recipient_user_id, delivery_key,
                       status, attempt_count, detail_json)
                    VALUES (?, 1, 20, 9001, ?, 'SENT', 1, '{}')
                    """)) {
                statement.setLong(1, 30L);
                statement.setString(2, "delivery:20:9001");
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_reminder_delivery
                      (id, tenant_id, occurrence_id, recipient_user_id, delivery_key,
                       status, notify_message_id, attempt_count, detail_json)
                    VALUES (?, 1, 20, 9001, ?, 'PENDING', NULL, 0, '{}')
                    """)) {
                statement.setLong(1, 31L);
                statement.setString(2, "delivery:20:9001");
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 32L);
                statement.setString(2, "delivery:20:9001:duplicate");
                assertThrows(SQLException.class, statement::executeUpdate);
            }
        }

        String fixture = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertContainsAll(fixture,
                "constraint `chk_dcc_reg_cert_reminder_config_time` check",
                "constraint `chk_dcc_reg_cert_daily_run_failure` check",
                "constraint `chk_dcc_reg_cert_reminder_occurrence_suppression` check",
                "constraint `chk_dcc_reg_cert_reminder_delivery_message` check");
        assertFalse(fixture.contains("json_length("),
                "H2 fixture must not fake MySQL JSON generated-column contracts");
    }

    @Test
    void cleanFixtureShouldDeleteReminderChildrenBeforeLifecycleRows() throws Exception {
        String clean = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/clean.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertOrder(clean,
                "delete from `dcc_registration_certificate_reminder_delivery`",
                "delete from `dcc_registration_certificate_reminder_occurrence`",
                "delete from `dcc_registration_certificate_daily_run`",
                "delete from `dcc_registration_certificate_reminder_config`",
                "delete from `dcc_registration_certificate_lifecycle_event`");
    }

    @Test
    void reminderErrorCodeAllocationShouldBeReservedForBehaviorTasks() throws Exception {
        String errorCodes = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java"),
                StandardCharsets.UTF_8);
        for (int code = 260; code <= 279; code++) {
            assertEquals(1, count(errorCodes, "1_080_000_" + code),
                    "registration-certificate reminder error code " + code + " must be allocated exactly once");
        }
    }

    private static Set<String> createdReminderTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (REMINDER_TABLES.contains(table)) {
                tables.add(table);
            }
        }
        return tables;
    }

    private static void assertContainsAll(String content, String... fragments) {
        Arrays.stream(fragments).forEach(fragment ->
                assertTrue(content.contains(fragment), "missing reminder schema contract: " + fragment));
    }

    private static void assertOrder(String content, String... fragments) {
        int cursor = -1;
        for (String fragment : fragments) {
            int index = content.indexOf(fragment);
            assertTrue(index > cursor, "expected cleanup order after previous fragment: " + fragment);
            cursor = index;
        }
    }

    private static int count(String text, String fragment) {
        int result = 0;
        for (int index = 0; (index = text.indexOf(fragment, index)) >= 0; index += fragment.length()) {
            result++;
        }
        return result;
    }

    private static Path findBackendRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml")) && Files.isDirectory(current.resolve("sql/mysql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate IntRuoyiBackend root");
    }
}
