package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateSchemaContractTest extends BaseDbUnitTest {

    private static final Set<String> CORE_TABLES = Set.of(
            "dcc_registration_certificate",
            "dcc_registration_certificate_version",
            "dcc_registration_certificate_snapshot",
            "dcc_registration_certificate_snapshot_entrusted",
            "dcc_registration_certificate_file",
            "dcc_registration_certificate_audit");

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+if\\s+not\\s+exists\\s+`([^`]+)`");

    @Resource
    private DataSource dataSource;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;

    @Test
    void migrationShouldDeclareOnlyTheSixFrozenCoreTablesAndFailFastContracts() throws Exception {
        Path backendRoot = findBackendRoot();
        Path migration = backendRoot.resolve("sql/mysql/20260817_dcc_registration_certificate_core.sql");
        assertTrue(Files.isRegularFile(migration), "T04-A core migration must exist");

        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        String normalized = sql.toLowerCase(Locale.ROOT);
        assertTrue(normalized.startsWith("-- release-migration: allowedenvironments=test,backup,prod; "
                        + "dependson=20260816_mdm_enterprise_company_scope,20260718_controlled_content_lifecycle; "
                        + "type=schema; risklevel=high"),
                "migration metadata and direct dependencies must match the frozen contract");
        assertEquals(CORE_TABLES, createdRegistrationTables(sql),
                "T04-A must create exactly the six frozen registration-certificate tables");
        assertFalse(normalized.contains("dcc_registration_certificate_change"));
        assertFalse(normalized.contains("dcc_registration_certificate_supporting_document"));
        assertTrue(normalized.contains("signal sqlstate '45000'"),
                "partial or incompatible schema must fail fast");

        assertContainsAll(normalized,
                "unique key `uk_dcc_reg_cert_version_no` (`tenant_id`, `certificate_id`, `version_no`)",
                "unique key `uk_dcc_reg_cert_current` (`tenant_id`, `certificate_id`, `current_unique_flag`)",
                "unique key `uk_dcc_reg_cert_pending` (`tenant_id`, `certificate_id`, `pending_unique_flag`)",
                "unique key `uk_dcc_reg_cert_snapshot_revision` (`tenant_id`, `version_id`, `revision_no`)",
                "unique key `uk_dcc_reg_cert_entrusted` (`tenant_id`, `snapshot_id`, `enterprise_id`)",
                "unique key `uk_dcc_reg_cert_bound_file` (`tenant_id`, `bound_file_unique_flag`)",
                "unique key `uk_dcc_reg_cert_audit_event` (`tenant_id`, `event_key`)",
                "json_length(`entrusted_enterprises_json`)",
                "constraint `chk_dcc_reg_cert_production_relation` check",
                "constraint `chk_dcc_reg_cert_file_status` check",
                "constraint `chk_dcc_reg_cert_file_binding` check",
                "constraint `chk_dcc_reg_cert_audit_event_key` check",
                "constraint `chk_dcc_reg_cert_audit_result` check",
                "constraint `chk_dcc_reg_cert_audit_trusted_identity` check",
                "constraint `chk_dcc_reg_cert_audit_trace` check",
                "`owner_company_id` bigint default null comment 'trusted owning company enterprise id'",
                "`certificate_id` bigint default null comment 'trusted registration certificate aggregate id'",
                "`requested_owner_company_id` bigint default null comment 'caller-requested owning company id'",
                "`requested_certificate_id` bigint default null comment 'caller-requested certificate id'",
                "`business_file_id` bigint default null comment 'registration certificate business file id'",
                "`result` varchar(32) not null comment 'success or failure result'",
                "`result_code` varchar(64) default null comment 'stable operation result code'",
                "`request_trace_id` varchar(128) not null comment 'request trace id'",
                "select `status` into linked_version_status",
                "cross-version snapshot reattachment is forbidden",
                "if linked_version_status is null or linked_version_status <> 'draft' then",
                "select version_row.`status` into entrusted_version_status",
                "cross-snapshot entrusted projection reattachment is forbidden",
                "if entrusted_version_status is null or entrusted_version_status <> 'draft' then",
                "invalid registration certificate version status transition",
                "invalid registration certificate file status transition",
                "formal registration certificate master cannot return to draft",
                "voided registration certificate master status is terminal",
                "lower(actual_column.column_type) <> expected_column.column_type",
                "actual_table.column_count <> expected_table.column_count",
                "create temporary table tmp_dcc_reg_cert_expected_column",
                "create temporary table tmp_dcc_reg_cert_expected_generation",
                "create temporary table tmp_dcc_reg_cert_expected_check",
                "legacy registrant_name not null drift",
                "modify column `registrant_name` varchar(255) default null comment 'registrant name snapshot'",
                "legacy production relation check drift",
                "drop check `chk_dcc_reg_cert_production_relation`",
                "add constraint `chk_dcc_reg_cert_production_relation` check",
                "(((entrusted_production=0x01)or(self_production=0x01))and(((entrusted_production=0x01)and(entrusted_enterprise_count>=1))or((entrusted_production=0x00)and(entrusted_enterprise_count=0))))",
                "(((entrusted_production=0x00)and(self_production=0x00)and(entrusted_enterprise_count=0))or(((entrusted_production=0x01)or(self_production=0x01))and(((entrusted_production=0x01)and(entrusted_enterprise_count>=1))or((entrusted_production=0x00)and(entrusted_enterprise_count=0)))))",
                "default charset=utf8mb4 collate=utf8mb4_unicode_ci",
                "dcc registration certificate core exact check expression mismatch",
                "dcc registration certificate core exact generated expression mismatch",
                "trigger `trg_dcc_reg_cert_master_immutable_bu`",
                "trigger `trg_dcc_reg_cert_master_immutable_bd`",
                "trigger `trg_dcc_reg_cert_version_immutable_bu`",
                "trigger `trg_dcc_reg_cert_version_immutable_bd`",
                "trigger `trg_dcc_reg_cert_snapshot_immutable_bu`",
                "trigger `trg_dcc_reg_cert_snapshot_immutable_bd`",
                "trigger `trg_dcc_reg_cert_entrusted_immutable_bu`",
                "trigger `trg_dcc_reg_cert_entrusted_immutable_bd`",
                "trigger `trg_dcc_reg_cert_file_immutable_bu`",
                "trigger `trg_dcc_reg_cert_file_immutable_bd`",
                "trigger `trg_dcc_reg_cert_audit_immutable_bu`",
                "trigger `trg_dcc_reg_cert_audit_immutable_bd`");
        assertFalse(normalized.contains("((((entrusted_production=0x00)and(self_production=0x00)and(entrusted_enterprise_count=0)))or"),
                "production relation expected CHECK must match MySQL 8 normalized parenthesization");
        assertFalse(normalized.contains("'other'"), "core persisted code sets must not define OTHER");
    }

    @Test
    void h2FixtureShouldLoadPortableCoreTablesWithoutFakingMysqlJsonLength() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS dcc_registration_certificate_change (id BIGINT)");
        }
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
        assertTrue(present.containsAll(CORE_TABLES), "H2 fixture must include every frozen core table");

        String fixture = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertFalse(fixture.contains("json_length("),
                "H2 fixture must not fake the MySQL JSON_LENGTH generated-column contract");
        assertContainsAll(fixture,
                "constraint `chk_dcc_reg_cert_file_status` check",
                "constraint `chk_dcc_reg_cert_audit_result` check",
                "constraint `chk_dcc_reg_cert_audit_trusted_identity` check",
                "constraint `chk_dcc_reg_cert_audit_trace` check",
                "`owner_company_id` bigint null",
                "`certificate_id` bigint null",
                "`requested_owner_company_id` bigint null",
                "`requested_certificate_id` bigint null",
                "`business_file_id` bigint null",
                "`result` varchar(32) not null",
                "`result_code` varchar(64) null",
                "`request_trace_id` varchar(128) not null");
    }

    @Test
    void mysqlVerificationScriptShouldCreateAndCleanupEachOwnedSchemaIndependently() throws Exception {
        String script = Files.readString(findBackendRoot().resolve(
                        "script/tests/test-dcc-registration-certificate-core-mysql.ps1"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        assertContainsAll(script,
                "function new-ownedschema",
                "function remove-ownedschemas",
                "$registry.add($schema)",
                "$cleanuperrors",
                "[allowemptycollection()]",
                "new-ownedschema -schema $schema -registry $createdschemas",
                "new-ownedschema -schema $partialschema -registry $createdschemas",
                "new-ownedschema -schema $incompatibleschema -registry $createdschemas",
                "$cleanuperrors.add($_.exception.message)",
                "remove-ownedschemas -schemas $createdschemas",
                "cleanup failed after all owned schemas were attempted");
        assertFalse(script.contains("create isolated schemas"),
                "owned schemas must not be created in one unregistered multi-statement batch");
    }

    @Test
    void javaPersistenceAndErrorCodeAllocationShouldMatchTheCoreContract() throws Exception {
        String[] dataObjects = {
                "DccRegistrationCertificateDO",
                "DccRegistrationCertificateVersionDO",
                "DccRegistrationCertificateSnapshotDO",
                "DccRegistrationCertificateSnapshotEntrustedDO",
                "DccRegistrationCertificateFileDO",
                "DccRegistrationCertificateAuditDO"
        };
        for (String dataObject : dataObjects) {
            assertTrue(classExists("cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject." + dataObject),
                    dataObject + " must exist");
        }
        String[] mappers = {
                "DccRegistrationCertificateMapper",
                "DccRegistrationCertificateVersionMapper",
                "DccRegistrationCertificateSnapshotMapper",
                "DccRegistrationCertificateSnapshotEntrustedMapper",
                "DccRegistrationCertificateFileMapper",
                "DccRegistrationCertificateAuditMapper"
        };
        for (String mapper : mappers) {
            assertTrue(classExists("cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql." + mapper),
                    mapper + " must exist for downstream tasks");
        }
        Class<?> baseMapper = Class.forName("com.baomidou.mybatisplus.core.mapper.BaseMapper");
        for (String appendOnlyMapper : Arrays.asList(
                "DccRegistrationCertificateSnapshotMapper",
                "DccRegistrationCertificateSnapshotEntrustedMapper",
                "DccRegistrationCertificateAuditMapper")) {
            Class<?> mapper = Class.forName(
                    "cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql." + appendOnlyMapper);
            assertFalse(baseMapper.isAssignableFrom(mapper),
                    appendOnlyMapper + " must not expose generic update or delete operations");
        }

        Class<?> auditDataObject = Class.forName(
                "cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject."
                        + "DccRegistrationCertificateAuditDO");
        for (String field : List.of("ownerCompanyId", "certificateId", "requestedOwnerCompanyId",
                "requestedCertificateId", "businessFileId", "result", "resultCode", "requestTraceId")) {
            assertTrue(hasDeclaredField(auditDataObject, field),
                    "audit persistence model must expose " + field);
        }
        String auditMapper = Files.readString(findBackendRoot().resolve(
                        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/"
                                + "dal/mysql/DccRegistrationCertificateAuditMapper.java"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertContainsAll(auditMapper, "owner_company_id", "certificate_id", "requested_owner_company_id",
                "requested_certificate_id", "business_file_id", "result", "result_code", "request_trace_id");

        String errorCodes = Files.readString(findBackendRoot().resolve(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java"),
                StandardCharsets.UTF_8);
        for (int code = 208; code <= 219; code++) {
            assertEquals(1, count(errorCodes, "1_080_000_" + code),
                    "registration-certificate error code " + code + " must be allocated exactly once");
        }
    }

    @Test
    void appendOnlyMappersShouldInsertAndReadThePortableFixture() {
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(11L)
                .revisionNo(1)
                .productName("Product A")
                .registrantName("Registrant A")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,\"enterpriseName\":\"Factory A\"}]")
                .effectiveAt(LocalDateTime.of(2026, 8, 17, 8, 0))
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, snapshotMapper.insert(snapshot));
        assertTrue(snapshot.getId() > 0);
        assertEquals("Product A", snapshotMapper.selectById(snapshot.getId()).getProductName());

        DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                DccRegistrationCertificateSnapshotEntrustedDO.builder()
                        .snapshotId(snapshot.getId())
                        .enterpriseId(30L)
                        .enterpriseNameSnapshot("Factory A")
                        .sortOrder(1)
                        .build();
        entrusted.setTenantId(1L);
        assertEquals(1, entrustedMapper.insert(entrusted));
        assertEquals(List.of(30L), entrustedMapper.selectListBySnapshotId(snapshot.getId()).stream()
                .map(DccRegistrationCertificateSnapshotEntrustedDO::getEnterpriseId)
                .toList());

        DccRegistrationCertificateAuditDO audit = DccRegistrationCertificateAuditDO.builder()
                .tenantId(1L)
                .certificateId(1L)
                .versionId(11L)
                .snapshotId(snapshot.getId())
                .eventKey("certificate:1:formalized")
                .eventType("FORMALIZED")
                .detailJson("{}")
                .occurredAt(LocalDateTime.of(2026, 8, 17, 8, 1))
                .creator("1")
                .build();
        setRequiredField(audit, "ownerCompanyId", 10L);
        setRequiredField(audit, "businessFileId", 500L);
        setRequiredField(audit, "result", "SUCCESS");
        setRequiredField(audit, "resultCode", "OK");
        setRequiredField(audit, "requestTraceId", "trace-t04a-h2-1");
        assertEquals(1, auditMapper.insert(audit));
        assertEquals(List.of("certificate:1:formalized"),
                auditMapper.selectListByCertificateId(1L).stream()
                        .map(DccRegistrationCertificateAuditDO::getEventKey)
                        .toList());
    }

    private static Set<String> createdRegistrationTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (table.startsWith("dcc_registration_certificate")) {
                tables.add(table);
            }
        }
        return tables;
    }

    private static void assertContainsAll(String content, String... fragments) {
        Arrays.stream(fragments).forEach(fragment ->
                assertTrue(content.contains(fragment), "missing schema contract: " + fragment));
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean hasDeclaredField(Class<?> type, String fieldName) {
        try {
            type.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    private static void setRequiredField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("required persistence field is absent: " + fieldName, exception);
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
