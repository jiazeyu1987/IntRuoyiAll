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

class DccRegistrationCertificateAccessSchemaTest extends BaseDbUnitTest {

    private static final Set<String> ACCESS_TABLES = Set.of(
            "dcc_registration_certificate_access_request",
            "dcc_registration_certificate_access_request_file",
            "dcc_registration_certificate_bpm_binding",
            "dcc_registration_certificate_grant",
            "dcc_registration_certificate_download_consumption",
            "dcc_registration_certificate_access_audit");

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+if\\s+not\\s+exists\\s+`([^`]+)`");

    @Resource
    private DataSource dataSource;

    @Test
    void migrationShouldDeclareAccessRequestGrantDownloadAndAuditContracts() throws Exception {
        Path backendRoot = findBackendRoot();
        Path migration = backendRoot.resolve("sql/mysql/20260818_dcc_registration_certificate_access.sql");
        assertTrue(Files.isRegularFile(migration), "T06-S access migration must exist");

        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        String normalized = sql.toLowerCase(Locale.ROOT);
        assertTrue(normalized.startsWith("-- release-migration: allowedenvironments=test,backup,prod; "
                        + "dependson=20260818_dcc_registration_certificate_reminder; type=schema; risklevel=high"),
                "access migration metadata and direct dependency must match the approved D-011 contract");
        assertEquals(ACCESS_TABLES, createdAccessTables(sql),
                "T06-S must create exactly request/request-file/BPM/grant/consumption/audit tables");
        assertTrue(normalized.contains("signal sqlstate '45000'"),
                "partial or incompatible access schema must fail fast");

        assertContainsAll(normalized,
                "assert_dcc_registration_certificate_access_contract",
                "dcc registration certificate access requires reminder schema",
                "dcc registration certificate access partial schema detected",
                "dcc registration certificate access column contract mismatch",
                "dcc registration certificate access index contract mismatch",
                "unique key `uk_dcc_reg_cert_access_request_key` (`tenant_id`, `request_key`)",
                "unique key `uk_dcc_reg_cert_access_request_bpm` (`tenant_id`, `bpm_process_instance_id`)",
                "unique key `uk_dcc_reg_cert_request_file_scope` (`tenant_id`, `request_id`, `business_file_id`)",
                "unique key `uk_dcc_reg_cert_bpm_binding_business` (`tenant_id`, `business_key`)",
                "unique key `uk_dcc_reg_cert_grant_key` (`tenant_id`, `grant_key`)",
                "unique key `uk_dcc_reg_cert_grant_request_file` (`tenant_id`, `request_file_id`, `grant_type`)",
                "unique key `uk_dcc_reg_cert_download_once` (`tenant_id`, `grant_id`, `business_file_id`, `success_unique_flag`)",
                "unique key `uk_dcc_reg_cert_access_audit_key` (`tenant_id`, `event_key`)",
                "constraint `chk_dcc_reg_cert_access_request_type` check",
                "constraint `chk_dcc_reg_cert_access_request_status` check",
                "constraint `chk_dcc_reg_cert_access_request_project` check",
                "constraint `chk_dcc_reg_cert_request_file_download` check",
                "constraint `chk_dcc_reg_cert_bpm_binding_status` check",
                "constraint `chk_dcc_reg_cert_grant_type` check",
                "constraint `chk_dcc_reg_cert_grant_status` check",
                "constraint `chk_dcc_reg_cert_grant_window` check",
                "constraint `chk_dcc_reg_cert_download_result` check",
                "constraint `chk_dcc_reg_cert_access_audit_result` check");
        assertFalse(normalized.contains("'other'"), "SP-06 persisted code sets must not define OTHER");
    }

    @Test
    void h2FixtureShouldLoadAccessTablesAndEnforcePortableConstraints() throws Exception {
        Set<String> present = new LinkedHashSet<>();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT LOWER(TABLE_NAME)
                       FROM INFORMATION_SCHEMA.TABLES
                      WHERE LOWER(TABLE_NAME) IN (
                         'dcc_registration_certificate_access_request',
                         'dcc_registration_certificate_access_request_file',
                         'dcc_registration_certificate_bpm_binding',
                         'dcc_registration_certificate_grant',
                         'dcc_registration_certificate_download_consumption',
                         'dcc_registration_certificate_access_audit')
                     """)) {
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    present.add(resultSet.getString(1));
                }
            }
        }
        assertEquals(ACCESS_TABLES, present,
                "H2 fixture must include every shared SP-06 access table and no substitute table");

        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_access_request
                      (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                       request_type, request_key, purpose, project_code_id, status, requested_at, detail_json)
                    VALUES (?, 1, 10, 1001, 99, ?, ?, 'view old cert', ?, 'SUBMITTED', ?, '{}')
                    """)) {
                statement.setLong(1, 1L);
                statement.setString(2, "VIEW_OLD_CERTIFICATE");
                statement.setString(3, "req:view:1");
                statement.setObject(4, null);
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 0)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 2L);
                statement.setString(2, "DOWNLOAD_FILE");
                statement.setString(3, "req:download:missing-project");
                assertThrows(SQLException.class, statement::executeUpdate,
                        "download requests must carry an approved project code at submission");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_access_request
                      (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                       request_type, request_key, purpose, project_code_id, status, requested_at, detail_json)
                    VALUES (3, 1, 10, 1001, 99, 'VIEW_OLD_CERTIFICATE', 'req:view:1',
                            'duplicate key', NULL, 'SUBMITTED', ?, '{}')
                    """)) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 1)));
                assertThrows(SQLException.class, statement::executeUpdate);
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_access_request_file
                      (id, tenant_id, request_id, business_file_id, file_kind, download_requested, status)
                    VALUES (?, 1, 1, 5001, 'REGISTRATION_CERTIFICATE', ?, 'REQUESTED')
                    """)) {
                statement.setLong(1, 10L);
                statement.setBoolean(2, false);
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 11L);
                statement.setBoolean(2, false);
                assertThrows(SQLException.class, statement::executeUpdate,
                        "same request and file may appear only once");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_bpm_binding
                      (id, tenant_id, request_id, business_key, bpm_process_instance_id, status, created_at)
                    VALUES (?, 1, 1, ?, ?, 'RUNNING', ?)
                    """)) {
                statement.setLong(1, 20L);
                statement.setString(2, "bpm:req:view:1");
                statement.setString(3, "proc-1");
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 2)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 21L);
                statement.setString(2, "bpm:req:view:2");
                assertThrows(SQLException.class, statement::executeUpdate,
                        "one BPM process instance cannot bind to two requests");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_grant
                      (id, tenant_id, request_id, request_file_id, owner_company_id, certificate_id,
                       business_file_id, grantee_user_id, grant_type, grant_key, status,
                       granted_at, expires_at, detail_json)
                    VALUES (?, 1, 1, 10, 10, 1001, 5001, 99, ?, ?, 'ACTIVE', ?, ?, '{}')
                    """)) {
                statement.setLong(1, 30L);
                statement.setString(2, "DOWNLOAD");
                statement.setString(3, "grant:download:5001");
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 3)));
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.of(2026, 8, 19, 10, 3)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 31L);
                statement.setString(3, "grant:download:5001:duplicate-file");
                assertThrows(SQLException.class, statement::executeUpdate,
                        "one request file can produce only one grant of the same type");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_grant
                      (id, tenant_id, request_id, request_file_id, owner_company_id, certificate_id,
                       business_file_id, grantee_user_id, grant_type, grant_key, status,
                       granted_at, expires_at, detail_json)
                    VALUES (32, 1, 1, 10, 10, 1001, 5001, 99, 'DOWNLOAD', 'grant:bad-window',
                            'ACTIVE', ?, ?, '{}')
                    """)) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 4)));
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 4)));
                assertThrows(SQLException.class, statement::executeUpdate,
                        "grant expiry must be strictly after granted_at");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_download_consumption
                      (id, tenant_id, grant_id, business_file_id, attempt_key, result,
                       success_unique_flag, started_at, detail_json)
                    VALUES (?, 1, 30, 5001, ?, ?, ?, ?, '{}')
                    """)) {
                statement.setLong(1, 40L);
                statement.setString(2, "attempt:pre-start-failure");
                statement.setString(3, "FAILED_BEFORE_START");
                statement.setObject(4, null);
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 5)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 41L);
                statement.setString(2, "attempt:success");
                statement.setString(3, "SUCCESS");
                statement.setInt(4, 1);
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 42L);
                statement.setString(2, "attempt:second-success");
                assertThrows(SQLException.class, statement::executeUpdate,
                        "a granted file may have at most one successful consumption");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO dcc_registration_certificate_access_audit
                      (id, tenant_id, request_id, grant_id, business_file_id, actor_user_id,
                       event_type, event_key, result, occurred_at, detail_json)
                    VALUES (?, 1, 1, 30, 5001, 99, 'DOWNLOAD_ATTEMPT', ?, ?, ?, '{}')
                    """)) {
                statement.setLong(1, 50L);
                statement.setString(2, "audit:download:1");
                statement.setString(3, "SUCCESS");
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.of(2026, 8, 18, 10, 6)));
                assertEquals(1, statement.executeUpdate());
                statement.setLong(1, 51L);
                assertThrows(SQLException.class, statement::executeUpdate,
                        "audit event keys are tenant-scoped idempotency keys");
            }
        }
    }

    @Test
    void cleanFixtureShouldDeleteAccessChildrenBeforeCertificateRows() throws Exception {
        String clean = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/clean.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertOrder(clean,
                "delete from `dcc_registration_certificate_access_audit`",
                "delete from `dcc_registration_certificate_download_consumption`",
                "delete from `dcc_registration_certificate_grant`",
                "delete from `dcc_registration_certificate_bpm_binding`",
                "delete from `dcc_registration_certificate_access_request_file`",
                "delete from `dcc_registration_certificate_access_request`",
                "delete from `dcc_registration_certificate_reminder_delivery`");
    }

    @Test
    void accessErrorCodeAllocationShouldBeReservedForBehaviorTasks() throws Exception {
        String errorCodes = Files.readString(findBackendRoot().resolve(
                        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java"),
                StandardCharsets.UTF_8);
        for (int code = 280; code <= 299; code++) {
            assertEquals(1, count(errorCodes, "1_080_000_" + code),
                    "registration-certificate access error code " + code + " must be allocated exactly once");
        }
    }

    private static Set<String> createdAccessTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (ACCESS_TABLES.contains(table)) {
                tables.add(table);
            }
        }
        return tables;
    }

    private static void assertContainsAll(String content, String... fragments) {
        Arrays.stream(fragments).forEach(fragment ->
                assertTrue(content.contains(fragment), "missing access schema contract: " + fragment));
    }

    private static void assertOrder(String content, String... fragments) {
        int cursor = -1;
        for (String fragment : fragments) {
            int index = content.indexOf(fragment);
            assertTrue(index > cursor, "expected cleanup order after previous fragment: " + fragment);
            cursor = index;
        }
    }

    private static int count(String content, String needle) {
        int count = 0;
        int cursor = 0;
        while ((cursor = content.indexOf(needle, cursor)) >= 0) {
            count++;
            cursor += needle.length();
        }
        return count;
    }

    private static Path findBackendRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("sql/mysql"))
                    && Files.isDirectory(current.resolve("yudao-module-dcc"))) {
                return current;
            }
            Path nested = current.resolve("IntRuoyiBackend");
            if (Files.isDirectory(nested.resolve("sql/mysql"))
                    && Files.isDirectory(nested.resolve("yudao-module-dcc"))) {
                return nested;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate IntRuoyiBackend from " + Path.of("").toAbsolutePath());
    }
}