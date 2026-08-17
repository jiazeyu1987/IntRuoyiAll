package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateAuditIdempotencySchemaTest extends BaseDbUnitTest {

    @Resource
    private DataSource dataSource;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;

    @Test
    void migrationShouldFreezeTrustedAndRequestedIdentityAsSeparateFacts() throws Exception {
        String migration = readBackendFile("sql/mysql/20260817_dcc_registration_certificate_core.sql")
                .toLowerCase(Locale.ROOT);

        assertContainsAll(migration,
                "('dcc_registration_certificate_audit', 'owner_company_id', 'bigint', 'yes', false)",
                "('dcc_registration_certificate_audit', 'certificate_id', 'bigint', 'yes', false)",
                "('dcc_registration_certificate_audit', 'requested_owner_company_id', 'bigint', 'yes', false)",
                "('dcc_registration_certificate_audit', 'requested_certificate_id', 'bigint', 'yes', false)",
                "union all select 'dcc_registration_certificate_audit', "
                        + "'chk_dcc_reg_cert_audit_trusted_identity'",
                "('dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_trusted_identity'",
                "`owner_company_id` bigint default null comment 'trusted owning company enterprise id'",
                "`certificate_id` bigint default null comment 'trusted registration certificate aggregate id'",
                "`requested_owner_company_id` bigint default null comment 'caller-requested owning company id'",
                "`requested_certificate_id` bigint default null comment 'caller-requested certificate id'",
                "constraint `chk_dcc_reg_cert_audit_trusted_identity` check",
                "unique key `uk_dcc_reg_cert_audit_event` (`tenant_id`, `event_key`)",
                "trigger `trg_dcc_reg_cert_audit_immutable_bu`",
                "trigger `trg_dcc_reg_cert_audit_immutable_bd`");
    }

    @Test
    void portableFixtureShouldAcceptValidTerminalOutcomesAndRejectUntrustedCombinations() {
        assertDoesNotThrow(() -> insertAudit("success-1", "SUCCESS", 10L, 20L, null, null));
        assertDoesNotThrow(() -> insertAudit("failure-1", "FAILURE", null, null, 30L, 40L));
        assertDoesNotThrow(() -> insertAudit("failure-no-request-id", "FAILURE", null, null, null, null));
        assertDoesNotThrow(() -> insertAudit("failure-with-trusted-id", "FAILURE", 10L, 20L, 10L, 20L));

        assertThrows(SQLException.class,
                () -> insertAudit("success-without-trusted-id", "SUCCESS", null, null, 30L, 40L));
        assertThrows(SQLException.class,
                () -> insertAudit("success-with-zero-trusted-id", "SUCCESS", 0L, 20L, null, null));
        assertThrows(SQLException.class,
                () -> insertAudit("failure-with-partial-trusted-id", "FAILURE", 10L, null, 10L, 20L));
        assertThrows(SQLException.class,
                () -> insertAudit("failure-with-zero-trusted-id", "FAILURE", 0L, 20L, 10L, 20L));
        assertThrows(SQLException.class,
                () -> insertAudit("success-1", "SUCCESS", 10L, 20L, null, null),
                "one tenant-scoped event key must have one terminal outcome");
    }

    @Test
    void persistenceModelAndMapperShouldPersistRequestedIdentityWithoutTreatingItAsTrusted() throws Exception {
        Class<?> auditType = DccRegistrationCertificateAuditDO.class;
        assertNotNull(assertDoesNotThrow(() -> auditType.getDeclaredField("requestedOwnerCompanyId")));
        assertNotNull(assertDoesNotThrow(() -> auditType.getDeclaredField("requestedCertificateId")));

        String mapper = readBackendFile("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/"
                + "registrationcertificate/dal/mysql/DccRegistrationCertificateAuditMapper.java")
                .toLowerCase(Locale.ROOT);
        assertContainsAll(mapper,
                "requested_owner_company_id", "requested_certificate_id",
                "#{requestedownercompanyid}", "#{requestedcertificateid}");

        DccRegistrationCertificateAuditDO failure = DccRegistrationCertificateAuditDO.builder()
                .tenantId(2L)
                .eventKey("failure-mapper-1")
                .eventType("FORMALIZE_FAILED")
                .actorId(99L)
                .result("FAILURE")
                .resultCode("PREREQUISITE_MISSING")
                .requestTraceId("trace-failure-mapper-1")
                .detailJson("{}")
                .occurredAt(LocalDateTime.of(2026, 8, 17, 16, 0))
                .creator("99")
                .build();
        setField(failure, "requestedOwnerCompanyId", 30L);
        setField(failure, "requestedCertificateId", 40L);

        assertEquals(1, auditMapper.insert(failure));
        DccRegistrationCertificateAuditDO stored =
                auditMapper.selectByTenantIdAndEventKey(2L, "failure-mapper-1");
        assertNotNull(stored);
        assertEquals(30L, getField(stored, "requestedOwnerCompanyId"));
        assertEquals(40L, getField(stored, "requestedCertificateId"));
        assertNull(stored.getOwnerCompanyId());
        assertNull(stored.getCertificateId());
    }

    @Test
    void mysqlVerificationShouldProbeTerminalRowsAndEveryNewRepeatMigrationContract() throws Exception {
        String script = readBackendFile("script/tests/test-dcc-registration-certificate-core-mysql.ps1")
                .toLowerCase(Locale.ROOT);
        assertContainsAll(script,
                "trusted success audit fixture",
                "failure audit with requested identity fixture",
                "failure audit without requested identity fixture",
                "failure audit with trusted identity fixture",
                "success audit without trusted identity",
                "success audit with zero trusted identity",
                "failure audit with partial trusted identity",
                "failure audit with zero trusted identity",
                "terminal audit outcome readback",
                "break audit trusted identity check expression",
                "incompatible audit column nullability",
                "incompatible requested identity column");
    }

    private void insertAudit(String eventKey, String result,
                             Long ownerCompanyId, Long certificateId,
                             Long requestedOwnerCompanyId, Long requestedCertificateId) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO dcc_registration_certificate_audit
                       (tenant_id, owner_company_id, certificate_id,
                        requested_owner_company_id, requested_certificate_id,
                        event_key, event_type, result, result_code, request_trace_id,
                        detail_json, occurred_at, creator)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setLong(1, 1L);
            setLong(statement, 2, ownerCompanyId);
            setLong(statement, 3, certificateId);
            setLong(statement, 4, requestedOwnerCompanyId);
            setLong(statement, 5, requestedCertificateId);
            statement.setString(6, eventKey);
            statement.setString(7, "COMMAND_TERMINAL");
            statement.setString(8, result);
            statement.setString(9, result.equals("SUCCESS") ? "OK" : "FAILED");
            statement.setString(10, "trace-" + eventKey);
            statement.setString(11, "{}");
            statement.setObject(12, LocalDateTime.of(2026, 8, 17, 16, 0));
            statement.setString(13, "99");
            statement.executeUpdate();
        }
    }

    private static void setLong(java.sql.PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String readBackendFile(String relativePath) throws Exception {
        return Files.readString(findBackendRoot().resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static void assertContainsAll(String content, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(content.contains(fragment), "missing audit schema contract: " + fragment);
        }
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
