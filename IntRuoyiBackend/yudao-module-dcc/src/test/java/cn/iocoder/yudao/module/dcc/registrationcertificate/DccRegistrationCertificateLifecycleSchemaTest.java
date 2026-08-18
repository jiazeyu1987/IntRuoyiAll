package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
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

class DccRegistrationCertificateLifecycleSchemaTest extends BaseDbUnitTest {

    private static final Set<String> LIFECYCLE_TABLES = Set.of(
            "dcc_registration_certificate_lifecycle_event",
            "dcc_registration_certificate_activation_replay",
            "dcc_registration_certificate_supporting_document",
            "dcc_registration_certificate_change",
            "dcc_registration_certificate_change_item");

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+if\\s+not\\s+exists\\s+`([^`]+)`");

    @Resource
    private DataSource dataSource;

    @Test
    void migrationShouldDeclareLifecycleTablesAndFailFastContracts() throws Exception {
        Path backendRoot = findBackendRoot();
        Path migration = backendRoot.resolve("sql/mysql/20260818_dcc_registration_certificate_lifecycle.sql");
        assertTrue(Files.isRegularFile(migration), "T05-S lifecycle migration must exist");

        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        String normalized = sql.toLowerCase(Locale.ROOT);
        assertTrue(normalized.startsWith("-- release-migration: allowedenvironments=test,backup,prod; "
                        + "dependson=20260817_dcc_registration_certificate_core; type=schema; risklevel=high"),
                "lifecycle migration metadata and direct dependency must match the frozen contract");
        assertEquals(LIFECYCLE_TABLES, createdLifecycleTables(sql),
                "T05-S must create exactly the shared lifecycle tables");
        assertTrue(normalized.contains("signal sqlstate '45000'"),
                "partial or incompatible lifecycle schema must fail fast");

        assertContainsAll(normalized,
                "assert_dcc_registration_certificate_lifecycle_contract",
                "dcc registration certificate lifecycle requires core schema",
                "dcc registration certificate lifecycle partial schema detected",
                "dcc registration certificate lifecycle column contract mismatch",
                "dcc registration certificate lifecycle index contract mismatch",
                "unique key `uk_dcc_reg_cert_lifecycle_event_key` (`tenant_id`, `event_key`)",
                "unique key `uk_dcc_reg_cert_lifecycle_sequence` (`tenant_id`, `certificate_id`, `event_sequence`)",
                "unique key `uk_dcc_reg_cert_activation_source` (`tenant_id`, `activation_event_id`, `source_event_id`)",
                "unique key `uk_dcc_reg_cert_support_open` (`tenant_id`, `certificate_id`, `document_type`, `open_unique_flag`)",
                "unique key `uk_dcc_reg_cert_change_event` (`tenant_id`, `event_id`)",
                "unique key `uk_dcc_reg_cert_change_item_type` (`tenant_id`, `change_id`, `item_type`)",
                "constraint `chk_dcc_reg_cert_lifecycle_event_type` check",
                "constraint `chk_dcc_reg_cert_lifecycle_sequence` check",
                "constraint `chk_dcc_reg_cert_activation_replay_order` check",
                "constraint `chk_dcc_reg_cert_support_status` check",
                "constraint `chk_dcc_reg_cert_support_reject_reason` check",
                "constraint `chk_dcc_reg_cert_change_status` check",
                "constraint `chk_dcc_reg_cert_change_selected_count` check",
                "constraint `chk_dcc_reg_cert_change_item_type` check",
                "constraint `chk_dcc_reg_cert_change_item_value` check");
        assertFalse(normalized.contains("'other'"), "lifecycle persisted code sets must not define OTHER");
    }

    @Test
    void h2FixtureShouldLoadLifecycleTablesAndEnforcePortableConstraints() throws Exception {
        Set<String> present = new LinkedHashSet<>();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT LOWER(TABLE_NAME)
                       FROM INFORMATION_SCHEMA.TABLES
                      WHERE LOWER(TABLE_NAME) LIKE 'dcc_registration_certificate%'
                     """)) {
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    present.add(resultSet.getString(1));
                }
            }
        }
        assertTrue(present.containsAll(LIFECYCLE_TABLES),
                "H2 fixture must include every shared lifecycle table");

        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (id, tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at)
                    VALUES (?, 1, 10, 1001, 2001, 2002, 3001, 3002, ?, 'ACTIVATION_APPLIED',
                       ?, 7, 3, 99, '{}', ?)
                    """)) {
                statement.setLong(1, 1L);
                statement.setString(2, "event:activation:1");
                statement.setInt(3, 1);
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 9, 0)));
                assertEquals(1, statement.executeUpdate());
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_activation_replay
                      (id, tenant_id, activation_event_id, source_event_id, certificate_id, source_sequence,
                       applied_sequence, replay_result, detail_json)
                    VALUES (?, 1, 1, 1, 1001, 1, 1, 'APPLIED', '{}')
                    """)) {
                statement.setLong(1, 10L);
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 11L);
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_supporting_document
                      (id, tenant_id, owner_company_id, certificate_id, version_id, document_type, status,
                       open_unique_flag, row_version, uploaded_at, uploaded_by)
                    VALUES (?, 1, 10, 1001, 2002, 'RENEWAL_ACCEPTANCE_RECEIPT',
                       'PENDING_CONFIRMATION', 1001, 1, ?, 99)
                    """)) {
                statement.setLong(1, 20L);
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 9, 1)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 21L);
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_supporting_document
                      (id, tenant_id, owner_company_id, certificate_id, version_id, document_type, status,
                       row_version, uploaded_at, uploaded_by)
                    VALUES (22, 1, 10, 1001, 2002, 'RENEWAL_SUPPLEMENT_NOTICE',
                       'REJECTED', 1, ?, 99)
                    """)) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 9, 2)));
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_change
                      (id, tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                       resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                       selected_item_count, status, row_version, actor_id, applied_at)
                    VALUES (30, 1, 10, 1001, 2002, 3002, 3003, 1, ?, '[\"PRODUCT_NAME\"]',
                       1, 'APPLIED', 1, 99, ?)
                    """)) {
                statement.setDate(1, java.sql.Date.valueOf(LocalDate.of(2026, 8, 18)));
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 9, 3)));
                assertEquals(1, statement.executeUpdate());
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_change_item
                      (id, tenant_id, change_id, item_type, before_value_json, after_value_json, sort_order)
                    VALUES (?, 1, 30, 'PRODUCT_NAME', '{\"value\":\"old\"}', '{\"value\":\"new\"}', 1)
                    """)) {
                statement.setLong(1, 40L);
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 41L);
                assertThrows(SQLException.class, statement::executeUpdate);
            }
        }

        String fixture = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertContainsAll(fixture,
                "constraint `chk_dcc_reg_cert_lifecycle_event_type` check",
                "constraint `chk_dcc_reg_cert_activation_replay_order` check",
                "constraint `chk_dcc_reg_cert_support_reject_reason` check",
                "constraint `chk_dcc_reg_cert_change_item_value` check");
        assertFalse(fixture.contains("json_length("),
                "H2 fixture must not fake MySQL JSON generated-column contracts");
    }

    @Test
    void cleanFixtureShouldDeleteLifecycleChildrenBeforeCoreRows() throws Exception {
        String clean = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/clean.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertOrder(clean,
                "delete from `dcc_registration_certificate_change_item`",
                "delete from `dcc_registration_certificate_change`",
                "delete from `dcc_registration_certificate_supporting_document`",
                "delete from `dcc_registration_certificate_activation_replay`",
                "delete from `dcc_registration_certificate_lifecycle_event`",
                "delete from `dcc_registration_certificate_audit`");
    }

    @Test
    void lifecycleErrorCodeAllocationShouldBeReservedForBehaviorTasks() throws Exception {
        String errorCodes = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java"),
                StandardCharsets.UTF_8);
        for (int code = 240; code <= 259; code++) {
            assertEquals(1, count(errorCodes, "1_080_000_" + code),
                    "registration-certificate lifecycle error code " + code + " must be allocated exactly once");
        }
    }

    private static Set<String> createdLifecycleTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (LIFECYCLE_TABLES.contains(table)) {
                tables.add(table);
            }
        }
        return tables;
    }

    private static void assertContainsAll(String content, String... fragments) {
        Arrays.stream(fragments).forEach(fragment ->
                assertTrue(content.contains(fragment), "missing lifecycle schema contract: " + fragment));
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
