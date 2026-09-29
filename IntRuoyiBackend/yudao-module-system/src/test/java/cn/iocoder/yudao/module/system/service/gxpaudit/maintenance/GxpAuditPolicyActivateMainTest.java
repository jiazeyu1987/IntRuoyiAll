package cn.iocoder.yudao.module.system.service.gxpaudit.maintenance;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic evidence and accounts are confined to an independently named in-memory database. */
class GxpAuditPolicyActivateMainTest {
    @TempDir Path temporary;
    private final ObjectMapper json = new ObjectMapper();
    private final GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load();
    private final Map<String, String> env = new HashMap<>();
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private ObjectNode approval;
    private Path approvalFile;
    private Path reportFile;
    private static final List<String> TABLES = List.of("gxp_audit_policy_version", "gxp_audit_policy_operation",
            "gxp_audit_policy_activation", "gxp_audit_event", "gxp_audit_event_relation", "gxp_audit_ledger_sequence");

    @BeforeEach
    void privateFixture() throws Exception {
        String url = "jdbc:h2:mem:maintenance_" + UUID.randomUUID()
                + ";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value";
        dataSource = new DriverManagerDataSource(url, "sa", "fixture-db-password");
        jdbc = new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("sql/create_tables.sql")).execute(dataSource);
        jdbc.update("INSERT INTO system_tenant (id,name,contact_user_id,contact_name,status,package_id,expire_time,account_count) "
                + "VALUES (71,'maintenance fixture',7101,'fixture',0,0,'2099-01-01',10)");
        jdbc.update("INSERT INTO system_users (id,tenant_id,username,canonical_username,nickname,password) VALUES (7101,71,?,?,?,?)",
                "maintenance.fixture", "maintenance.fixture", "Maintenance fixture", new BCryptPasswordEncoder().encode("fixture-operator-password"));
        jdbc.update("INSERT INTO system_role (id,tenant_id,name,code,sort,status,type) VALUES (711,71,'fixture','tenant_admin',1,0,1)");
        jdbc.update("INSERT INTO system_user_role (tenant_id,user_id,role_id) VALUES (71,7101,711)");
        reportFile = temporary.resolve("synthetic-boundaries.jsonl");
        Files.writeString(reportFile, "{\"decision\":\"REGISTERED\",\"candidate\":\"synthetic-H2-fixture-only\"}\n");
        approvalFile = temporary.resolve("synthetic-coverage-report.json");
        approval = json.createObjectNode().put("schemaVersion", "gxp-coverage-report.v1")
                .put("status", "PASS").put("approvalReference", bundle.approvalReference())
                .put("policyVersion", bundle.policyVersion()).put("policyHash", bundle.policyHash())
                .put("artifactHash", bundle.artifactHash()).put("coverageMode", bundle.policyNode().path("coverageScope").path("mode").asText())
                .put("unresolvedCount", 0).put("reviewedOutOfScopeCount", 0)
                .put("boundarySha256", DigestUtil.sha256Hex(Files.readAllBytes(reportFile)));
        env.put("GXP_ACTIVATION_DB_URL", url);
        env.put("GXP_ACTIVATION_DB_USER", "sa");
        env.put("GXP_ACTIVATION_DB_PASSWORD", "fixture-db-password");
        env.put("GXP_ACTIVATION_TENANT_ID", "71");
        env.put("GXP_ACTIVATION_OPERATOR", "maintenance.fixture");
        env.put("GXP_ACTIVATION_OPERATOR_PASSWORD", "fixture-operator-password");
        env.put("GXP_ACTIVATION_REQUEST_ID", "maintenance-fixture-r1");
        env.put("GXP_ACTIVATION_APPROVAL_REFERENCE", bundle.approvalReference());
        env.put("GXP_ACTIVATION_COVERAGE_FILE", approvalFile.toString());
        env.put("GXP_ACTIVATION_BOUNDARY_FILE", reportFile.toString());
        env.put("GXP_ACTIVATION_WRITERS_STOPPED", "true");
        writeApproval();
    }

    @Test
    void realServiceCommitsAndIdenticalRequestReplaysWithoutWrites() throws Exception {
        var first = GxpAuditPolicyActivateMain.run(env);
        assertFalse(first.replayed());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(bundle.policyHash(), first.policyHash());
        assertEquals(1, count("gxp_audit_policy_version"));
        assertEquals(bundle.policyNode().path("operations").size(), count("gxp_audit_policy_operation"));
        assertEquals(1, count("gxp_audit_policy_activation"));
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(1, count("gxp_audit_event_relation"));
        assertEquals("maintenance.fixture", jdbc.queryForObject("SELECT actor_username FROM gxp_audit_event", String.class));
        assertEquals(7101L, jdbc.queryForObject("SELECT approved_by FROM gxp_audit_policy_version", Long.class));
        assertEquals(env.get("GXP_ACTIVATION_COVERAGE_SHA256"), jdbc.queryForObject("SELECT coverage_report_hash FROM gxp_audit_policy_version", String.class));
        assertEquals(bundle.artifactHash(), jdbc.queryForObject("SELECT artifact_hash FROM gxp_audit_policy_version", String.class));
        assertEquals(jdbc.queryForObject("SELECT activation_hash FROM gxp_audit_policy_activation", String.class),
                jdbc.queryForObject("SELECT target_hash FROM gxp_audit_event_relation", String.class));
        List<Map<String, Object>> before = jdbc.queryForList("SELECT * FROM gxp_audit_event");
        var replay = GxpAuditPolicyActivateMain.run(env);
        assertTrue(replay.replayed());
        assertEquals(first.activationId(), replay.activationId());
        assertEquals(before, jdbc.queryForList("SELECT * FROM gxp_audit_event"));
        assertEquals(2L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class));
        assertNull(TenantContextHolder.getTenantId());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void relationFailureRollsBackEarlierRealWrites() {
        // The failure is after real version, operation, activation, event and ledger writes.
        jdbc.execute("ALTER TABLE gxp_audit_event_relation ADD CONSTRAINT fixture_reject_relation CHECK (target_type <> 'GXP_POLICY_ACTIVATION')");
        var failure = assertThrows(DataIntegrityViolationException.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertTrue(failure.getMessage().contains("fixture_reject_relation"), failure.getMessage());
        assertNoWrites();
        assertNull(TenantContextHolder.getTenantId());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @ValueSource(strings = {"policyHash", "artifactHash", "policyVersion", "approvalReference",
            "status", "boundarySha256", "schemaVersion", "coverageMode"})
    void mismatchedApprovalIsRejectedBeforeWrites(String field) throws Exception {
        approval.put(field, "incorrect");
        writeApproval();
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void failedGateIsRejectedEvenIfAllDigestsMatch() throws Exception {
        approval.put("unresolvedCount", 1);
        writeApproval();
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void failedBoundaryCannotBeHiddenByPassingSummary() throws Exception {
        Files.writeString(reportFile, "{\"decision\":\"STALE_APPROVAL\"}\n");
        approval.put("boundarySha256", DigestUtil.sha256Hex(Files.readAllBytes(reportFile)));
        writeApproval();
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GXP_ACTIVATION_DB_URL", "GXP_ACTIVATION_DB_USER", "GXP_ACTIVATION_DB_PASSWORD",
            "GXP_ACTIVATION_OPERATOR", "GXP_ACTIVATION_OPERATOR_PASSWORD", "GXP_ACTIVATION_TENANT_ID",
            "GXP_ACTIVATION_REQUEST_ID", "GXP_ACTIVATION_APPROVAL_REFERENCE", "GXP_ACTIVATION_COVERAGE_SHA256",
            "GXP_ACTIVATION_COVERAGE_FILE", "GXP_ACTIVATION_BOUNDARY_FILE", "GXP_ACTIVATION_WRITERS_STOPPED"})
    void missingRequiredInputRefusesWithoutWrites(String key) {
        env.remove(key);
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void tamperedApprovalAndReportAreRejected() throws Exception {
        Files.writeString(approvalFile, Files.readString(approvalFile) + " ");
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        writeApproval();
        Files.writeString(reportFile, Files.readString(reportFile) + " ");
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void wrongCredentialsRejectWithoutAuthenticationContext() {
        env.put("GXP_ACTIVATION_OPERATOR_PASSWORD", "wrong-fixture-password");
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPDATE system_users SET status=1", "UPDATE system_users SET login_locked=1",
            "UPDATE system_users SET password_credential_status='RESET_REQUIRED'", "UPDATE system_users SET deleted=true",
            "UPDATE system_users SET tenant_id=72", "DELETE FROM system_user_role", "UPDATE system_role SET status=1",
            "UPDATE system_role SET tenant_id=72", "UPDATE system_tenant SET status=1",
            "UPDATE system_tenant SET expire_time='2000-01-01'", "DELETE FROM system_tenant"})
    void inactiveOrUnauthorizedDatabaseIdentityRefuses(String mutation) {
        jdbc.update(mutation);
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void watermarkDriftRejectsWithoutAdditionalWrites() throws Exception {
        GxpAuditPolicyActivateMain.run(env);
        jdbc.update("UPDATE gxp_audit_ledger_sequence SET next_ledger_sequence=99");
        env.put("GXP_ACTIVATION_REQUEST_ID", "fixture-second");
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertEquals(1, count("gxp_audit_policy_activation"));
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(99L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class));
    }

    @Test
    void newRequestCannotRebindExistingVersionToDifferentCoverageBytes() throws Exception {
        GxpAuditPolicyActivateMain.run(env);
        env.put("GXP_ACTIVATION_REQUEST_ID", "fixture-new-request");
        // Both reports are valid JSON and semantically equal; approved evidence identity is byte-exact.
        Files.writeString(approvalFile, Files.readString(approvalFile) + "\n");
        env.put("GXP_ACTIVATION_COVERAGE_SHA256", DigestUtil.sha256Hex(Files.readAllBytes(approvalFile)));
        var failure = assertThrows(IllegalArgumentException.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertTrue(failure.getMessage().contains("existing-version-evidence-mismatch"));
        assertEquals(1, count("gxp_audit_policy_activation"));
        assertEquals(1, count("gxp_audit_event"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVIEWED_OUT_OF_RELEASE_SCOPE", "REVIEWED_MIXED_SCOPE"})
    void r1ReviewCountMustEqualBoundBoundaryEvidence(String decision) throws Exception {
        assertEquals("R1", bundle.policyNode().path("coverageScope").path("mode").asText());
        Files.writeString(reportFile, "{\"decision\":\"" + decision + "\",\"candidate\":\"synthetic-H2-fixture-only\"}\n");
        approval.put("boundarySha256", DigestUtil.sha256Hex(Files.readAllBytes(reportFile)));
        writeApproval();
        assertThrows(IllegalArgumentException.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
        approval.put("reviewedOutOfScopeCount", 1);
        writeApproval();
        assertFalse(GxpAuditPolicyActivateMain.run(env).replayed());
        assertEquals(1, count("gxp_audit_policy_activation"));
    }

    @Test
    void fullCoverageCannotUseR1PolicyAndScopeExclusions() throws Exception {
        approval.put("coverageMode", "FULL_COVERAGE").put("reviewedOutOfScopeCount", 1);
        writeApproval();
        assertThrows(IllegalArgumentException.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void exactDelegatedEffectIsNotCountedAsAnOutOfScopeWrite() throws Exception {
        Files.writeString(reportFile, "{\"decision\":\"REVIEWED_DELEGATED_EFFECT\",\"candidate\":\"synthetic-H2-fixture-only\"}\n");
        approval.put("boundarySha256", DigestUtil.sha256Hex(Files.readAllBytes(reportFile)));
        writeApproval();
        assertFalse(GxpAuditPolicyActivateMain.run(env).replayed());
        assertEquals(1, count("gxp_audit_policy_activation"));
    }

    @Test
    void duplicateJsonKeysCannotReplaceFailingStatus() throws Exception {
        String duplicate = Files.readString(approvalFile).replace("\"status\":\"PASS\"", "\"status\":\"FAIL\",\"status\":\"PASS\"");
        Files.writeString(approvalFile, duplicate);
        env.put("GXP_ACTIVATION_COVERAGE_SHA256", DigestUtil.sha256Hex(Files.readAllBytes(approvalFile)));
        assertThrows(Exception.class, () -> GxpAuditPolicyActivateMain.run(env));
        assertNoWrites();
    }

    @Test
    void minimalContextContainsOnlyMaintenanceInfrastructure() {
        try (var context = GxpAuditPolicyActivateMain.openContext(dataSource)) {
            assertTrue(AopUtils.isAopProxy(context.getBean(GxpAuditPolicyActivationService.class)));
            assertTrue(AopUtils.isAopProxy(context.getBean(GxpAuditService.class)));
            for (String name : context.getBeanDefinitionNames()) {
                Class<?> type = context.getType(name);
                assertNotNull(type);
                String qualified = type.getName();
                assertFalse(qualified.contains("WebServer") || qualified.contains("Scheduler")
                        || qualified.contains("AutoConfiguration") || qualified.contains("StartupGuard"), qualified);
                if (qualified.startsWith("cn.iocoder.yudao.module.")) {
                    assertTrue(qualified.contains(".gxpaudit.") || qualified.endsWith("TenantMapper"), qualified);
                }
            }
            assertTrue(jdbc.queryForList("SELECT * FROM gxp_audit_policy_activation").isEmpty());
        }
    }

    private void writeApproval() throws Exception {
        Files.writeString(approvalFile, json.writeValueAsString(approval));
        env.put("GXP_ACTIVATION_COVERAGE_SHA256", DigestUtil.sha256Hex(Files.readAllBytes(approvalFile)));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertNoWrites() {
        TABLES.forEach(table -> assertEquals(0, count(table), table));
    }
}
