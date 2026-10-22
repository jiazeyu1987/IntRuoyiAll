package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Strict protected operator facts and current read-only development policy/schema gate. No write SQL.
 */
@Component
public class DccLegacyMaintenanceGate {

    static final String DATABASE = "ruoyi-vue-pro", UUID = "92ca05d0-aec8-11f1-a944-02b4e226a5ef";

    static final String POLICY_SHA = "661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894", COVERAGE_SHA = "3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d";

    static final String SCHEMA_CONTRACT_SHA = "f1ec2ec8207b34f0427d021f717c4859d6b71715fbdc436706263ed0fd7e9f0a";

    static final String SCHEMA_VALIDATOR_SHA = "667a81edf468e1212afef8a01ae5cb996bd62737cfba7db6138e30259e0db2d4";

    static final String MIGRATION = "20261003_dcc_legacy_source_name_occupancy", MIGRATION_SHA = "621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a";

    static final String POLICY_VERSION = "2026-10-dcc-integration-01";

    static final String[] DEVELOPMENT_FIELDS = { "status", "tenantId", "policyVersion", "policyHash", "coverageReportHash" };

    static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    final JdbcTemplate jdbc;

    private final DataSource source;

    private final Environment environment;

    public DccLegacyMaintenanceGate(DataSource source, Environment environment) {
        this.source = Objects.requireNonNull(source);
        this.environment = Objects.requireNonNull(environment);
        jdbc = new JdbcTemplate(source);
    }

    void requireDevelopmentEnvironment() throws Exception {
        Set<String> profiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (!profiles.contains("local-maintenance") || profiles.stream().anyMatch(
                Set.of("prod", "production", "backup", "test", "unit-test")::contains))
            throw invalid("DEVELOPMENT_PROFILE_REQUIRED");
        // Use the actual shared DataSource connection, including the executor's bound transaction.
        var connection = DataSourceUtils.getConnection(source);
        try {
            String url = connection.getMetaData().getURL();
            if (url == null || !url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):23306/ruoyi-vue-pro(?:\\?.*)?"))
                throw invalid("DEVELOPMENT_LOCAL_DATASOURCE_REQUIRED");
        } finally {
            DataSourceUtils.releaseConnection(connection, source);
        }
        var actual = jdbc.queryForMap("SELECT DATABASE() AS database_name,@@server_uuid AS server_uuid,@@session.time_zone AS time_zone");
        if (!DATABASE.equals(actual.get("database_name")) || !UUID.equals(actual.get("server_uuid"))
                || !"+08:00".equals(actual.get("time_zone")))
            throw invalid("ACTUAL_SOURCE_DATABASE_TIMEZONE_MISMATCH");
    }

    record Descriptor(Path path, String sha256) {
    }

    record Input(Path protectedRoot, String requestSha, String scopeId, String manifestSha, Descriptor manifest, Descriptor facts, Descriptor bytesReceipt, Descriptor bytesResults, Descriptor decision, Descriptor development, Descriptor policy, Descriptor schemaProof, String reason, String requestId) {
    }

    Input read(Path root, Path request, String expectedHash) throws Exception {
        if (root == null || !Files.isDirectory(root) || Files.isSymbolicLink(root))
            throw invalid("PROTECTED_ROOT_INVALID");
        Path actualRoot = root.toRealPath();
        byte[] raw = bounded(request, actualRoot, 65536);
        if (!hash(raw).equals(expectedHash))
            throw invalid("REQUEST_BYTES_DRIFT");
        JsonNode r = JSON.readTree(raw);
        fields(r, "schemaVersion", "authorization", "manifest", "facts", "bytesReceipt", "bytesResults", "historicalDecision", "developmentPolicy", "policyFile", "schemaProof", "scopeId", "reason", "requestId");
        if (!r.get("schemaVersion").isInt() || r.get("schemaVersion").intValue() != 1)
            throw invalid("REQUEST_SCHEMA_INVALID");
        String scope = text(r, "scopeId");
        if (!scope.matches("[A-Za-z0-9_-]{1,64}"))
            throw invalid("SCOPE_INVALID");
        String reason = text(r, "reason"), requestId = text(r, "requestId");
        if (reason.length() > 500 || requestId.length() > 128)
            throw invalid("REASON_REQUEST_OVERFLOW");
        var authorization = descriptor(r.get("authorization"), actualRoot);
        JsonNode a = JSON.readTree(load(authorization, actualRoot));
        fields(a, "status", "userAnswerReceived", "actualAnswer", "sourceQuestionId", "sourceMessageReference", "scopeId", "manifestSha256", "claimCount", "versionCount", "nameGroupCount", "tenantId", "database", "serverUuid", "preserveHistoricalRows", "allowObjectRecovery", "allowAuditPolicyConfiguration", "allowSchemaMigration");
        var manifest = descriptor(r.get("manifest"), actualRoot);
        if (!"EXACT_LEGACY_SCOPE_REGISTRATION_USER_APPROVED".equals(text(a, "status")) || !a.get("userAnswerReceived").isBoolean() || !a.get("userAnswerReceived").booleanValue() || !Set.of("授权按此方案执行", "授权按此方案执行（建议）", "同意按此方案执行", "授权条件齐备后按方案执行（建议）").contains(text(a, "actualAnswer")) || !scope.equals(text(a, "scopeId")) || !manifest.sha256().equals(text(a, "manifestSha256")) || !a.get("claimCount").isInt() || a.get("claimCount").intValue() != 25 || !a.get("versionCount").isInt() || a.get("versionCount").intValue() != 39 || !a.get("nameGroupCount").isInt() || a.get("nameGroupCount").intValue() != 13 || !a.get("tenantId").isTextual() || !"1".equals(a.get("tenantId").textValue()) || !DATABASE.equals(text(a, "database")) || !UUID.equals(text(a, "serverUuid")) || !trueBoolean(a, "preserveHistoricalRows") || !falseBoolean(a, "allowObjectRecovery") || !falseBoolean(a, "allowAuditPolicyConfiguration") || !falseBoolean(a, "allowSchemaMigration"))
            throw invalid("EXACT_REGISTRATION_APPROVAL_REQUIRED");
        text(a, "sourceQuestionId");
        text(a, "sourceMessageReference");
        var input = new Input(actualRoot, expectedHash, scope, manifest.sha256(), manifest, descriptor(r.get("facts"), actualRoot), descriptor(r.get("bytesReceipt"), actualRoot), descriptor(r.get("bytesResults"), actualRoot), descriptor(r.get("historicalDecision"), actualRoot), descriptor(r.get("developmentPolicy"), actualRoot), descriptor(r.get("policyFile"), actualRoot), descriptor(r.get("schemaProof"), actualRoot), reason, requestId);
        for (var d : List.of(input.manifest(), input.facts(), input.bytesReceipt(), input.bytesResults(), input.decision(), input.development(), input.policy(), input.schemaProof())) load(d, actualRoot);
        return input;
    }

    DccLegacyNameVerifiedScope verifiedScope(Input input) throws Exception {
        for (var d : List.of(input.manifest(), input.facts(), input.bytesReceipt(), input.bytesResults(), input.decision(), input.development(), input.policy(), input.schemaProof())) load(d, input.protectedRoot());
        var scope = DccLegacyNameVerifiedScope.fromProtectedArtifact(input.manifest().path(), input.manifestSha(), input.facts().path(), input.bytesReceipt().path(), input.bytesResults().path(), input.decision().path());
        if (!input.scopeId().equals(scope.manifest.scopeId()) || !input.reason().equals(scope.manifest.reason()) || !input.requestId().equals(scope.manifest.requestId()) || scope.manifest.evidence().size() != 39 || scope.manifest.evidence().stream().map(DccLegacyNameVerifiedScope.Evidence::claimId).distinct().count() != 25 || scope.manifest.evidence().stream().map(DccLegacyNameVerifiedScope.Evidence::sourceName).distinct().count() != 13)
            throw invalid("EXACT_25_39_13_SCOPE_REQUIRED");
        return scope;
    }

    void currentDevelopmentPolicy(Input input) throws Exception {
        requireDevelopmentEnvironment();
        requireDevelopmentPolicy(input);
        String version = POLICY_VERSION;
        verifyCurrentSchemaProof(input);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation') AND ENGINE='InnoDB'", Long.class) != 3L)
            throw invalid("SIDECAR_SCHEMA_REQUIRED");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy' AND sha256='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a' AND status='APPLIED' AND target_environment='test' AND deleted=b'0'", Long.class) != 1L)
            throw invalid("EXACT_SIDECAR_MIGRATION_REQUIRED");
        Map<?, ?> policy = new Yaml(new SafeConstructor(new LoaderOptions())).load(new String(load(input.policy(), input.protectedRoot()), StandardCharsets.UTF_8));
        if (!version.equals(policy.get("policyVersion")) || !(policy.get("operations") instanceof List<?> operations) || operations.size() != 34)
            throw invalid("EXACT_POLICY_DECLARATION_REQUIRED");
        String[] fields = { "operationId", "sourceType", "sourceLocator", "domain", "subjectType", "actionType", "reasonPolicy", "signaturePolicy", "statePolicy", "retentionClass", "testIds", "owner", "applicability" };
        String[] columns = { "operation_id", "source_type", "source_locator", "domain", "subject_type", "action_type", "reason_policy", "signature_policy", "state_policy", "retention_class", "test_ids", "owner", "applicability" };
        int count = 0;
        for (Object item : operations) {
            if (!(item instanceof Map<?, ?> operation))
                throw invalid("POLICY_OPERATION_INVALID");
            String operationId = String.valueOf(operation.get("operationId"));
            if (!operationId.startsWith("dcc.") || operationId.equals("dcc.controlled-file.publish"))
                continue;
            count++;
            var actual = jdbc.queryForList("SELECT * FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=?", operationId);
            if (actual.size() != 1)
                throw invalid("EXACT_26_RUNTIME_POLICY_REQUIRED");
            var r = actual.get(0);
            requireValue(r, "policy_version", version);
            for (int i = 0; i < fields.length; i++) {
                Object value = operation.get(fields[i]);
                if (fields[i].equals("testIds") && value instanceof List<?> list)
                    value = "[" + String.join(", ", list.stream().map(String::valueOf).toList()) + "]";
                requireValue(r, columns[i], value);
            }
            if (!bitTrue(r.get("active")) || !bitFalse(r.get("deleted")))
                throw invalid("RUNTIME_POLICY_INACTIVE");
        }
        if (count != 26)
            throw invalid("EXACT_26_OPERATION_SCOPE_REQUIRED");
    }

    void requireDevelopmentPolicy(Input input) throws Exception {
        JsonNode declaration = JSON.readTree(load(input.development(), input.protectedRoot()));
        fields(declaration, DEVELOPMENT_FIELDS);
        if (!"DEVELOPMENT_TEST_ONLY".equals(text(declaration, "status"))
                || !"1".equals(text(declaration, "tenantId"))
                || !POLICY_VERSION.equals(text(declaration, "policyVersion"))
                || !POLICY_SHA.equals(text(declaration, "policyHash"))
                || !COVERAGE_SHA.equals(text(declaration, "coverageReportHash"))
                || !POLICY_SHA.equals(input.policy().sha256()))
            throw invalid("EXACT_DEVELOPMENT_POLICY_REQUIRED");
        Map<?, ?> policy = new Yaml(new SafeConstructor(new LoaderOptions())).load(
                new String(load(input.policy(), input.protectedRoot()), StandardCharsets.UTF_8));
        if (policy == null || !POLICY_VERSION.equals(policy.get("policyVersion")))
            throw invalid("EXACT_POLICY_DECLARATION_REQUIRED");
    }

    void verifyCurrentSchemaProof(Input input) throws Exception {
        JsonNode proof = JSON.readTree(load(input.schemaProof(), input.protectedRoot()));
        fields(proof, "status", "database", "serverUuid", "migrationId", "migrationSqlSha256", "validatedContract", "completedSourceJournal", "validatorReceipt", "showCreateTableSha256");
        if (!"ACTUAL_SOURCE_SIDECAR_SCHEMA_FIRST_REPEAT_VERIFIED".equals(text(proof, "status")) || !DATABASE.equals(text(proof, "database")) || !UUID.equals(text(proof, "serverUuid")) || !MIGRATION.equals(text(proof, "migrationId")) || !MIGRATION_SHA.equals(text(proof, "migrationSqlSha256")))
            throw invalid("ACTUAL_VALIDATED_SCHEMA_RECEIPT_REQUIRED");
        var contract = descriptor(proof.get("validatedContract"), input.protectedRoot());
        if (!SCHEMA_CONTRACT_SHA.equals(contract.sha256()))
            throw invalid("FIXED_SCHEMA_CONTRACT_REQUIRED");
        load(contract, input.protectedRoot());
        var journalDescriptor = descriptor(proof.get("completedSourceJournal"), input.protectedRoot());
        JsonNode journal = JSON.readTree(load(journalDescriptor, input.protectedRoot()));
        if (!"G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS".equals(text(journal, "status")) || !DATABASE.equals(text(journal, "database")) || !MIGRATION.equals(text(journal, "migrationId")) || !MIGRATION_SHA.equals(text(journal, "sqlSha256")) || !trueBoolean(journal, "databaseWriteAttempted") || journal.get("steps") == null || !journal.get("steps").isArray() || journal.get("steps").size() != 2)
            throw invalid("ACTUAL_SOURCE_SCHEMA_JOURNAL_REQUIRED");
        text(journal, "operationId");
        load(descriptor(journal.get("originalBefore"), input.protectedRoot()), input.protectedRoot());
        for (int i = 0; i < 2; i++) {
            var step = journal.get("steps").get(i);
            String phase = i == 0 ? "first" : "repeat";
            if (step == null || !step.isObject() || !phase.equals(step.path("phase").asText()) || !"POSTFLIGHT_AND_ORIGINAL_ROWS_PASS".equals(step.path("phaseStatus").asText()))
                throw invalid("ACTUAL_SOURCE_SCHEMA_PHASE_REQUIRED");
            var material = descriptor(step.get("material"), input.protectedRoot());
            load(material, input.protectedRoot());
            if (!material.sha256().equals(text(step, "materialSha256")))
                throw invalid("ACTUAL_SOURCE_SCHEMA_MATERIAL_REQUIRED");
            for (String artifact : List.of("postflight", "originalAfter", "newLedger")) load(descriptor(step.get(artifact), input.protectedRoot()), input.protectedRoot());
            var execution = step.get("execution");
            if (execution == null || !execution.isObject() || !execution.path("exitCode").isInt() || execution.path("exitCode").intValue() != 0 || !DATABASE.equals(text(execution, "database")) || !material.sha256().equals(text(execution, "sqlSha256")))
                throw invalid("ACTUAL_SOURCE_SCHEMA_EXECUTION_REQUIRED");
            load(descriptor(execution.get("stdout"), input.protectedRoot()), input.protectedRoot());
            if (load(descriptor(execution.get("stderr"), input.protectedRoot()), input.protectedRoot()).length != 0)
                throw invalid("ACTUAL_SOURCE_SCHEMA_STDERR_REQUIRED");
        }
        var shapes = proof.get("showCreateTableSha256");
        fields(shapes, "dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation");
        // Root must run the pinned reviewed driver validator on the full journal; this narrow entry does not reimplement its SQL/schema parser.
        JsonNode validation = JSON.readTree(load(descriptor(proof.get("validatorReceipt"), input.protectedRoot()), input.protectedRoot()));
        fields(validation, "status", "database", "serverUuid", "sourceJournalSha256", "schemaContractSha256", "showCreateTableSha256", "validatorSourceSha256");
        if (!"ROOT_REPLAYED_CURRENT_SCHEMA_DRIVER_PASS".equals(text(validation, "status")) || !DATABASE.equals(text(validation, "database")) || !UUID.equals(text(validation, "serverUuid")) || !journalDescriptor.sha256().equals(text(validation, "sourceJournalSha256")) || !SCHEMA_CONTRACT_SHA.equals(text(validation, "schemaContractSha256")) || !SCHEMA_VALIDATOR_SHA.equals(text(validation, "validatorSourceSha256")) || !shapes.equals(validation.get("showCreateTableSha256")))
            throw invalid("ROOT_ACTUAL_SCHEMA_VALIDATION_REQUIRED");
        for (String table : List.of("dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation")) {
            String expected = text(shapes, table);
            if (!expected.matches("[0-9a-f]{64}"))
                throw invalid("EXACT_SCHEMA_HASH_REQUIRED");
            var rows = jdbc.queryForList("SHOW CREATE TABLE `" + table + "`");
            if (rows.size() != 1)
                throw invalid("EXACT_CURRENT_SCHEMA_REQUIRED");
            Object ddl = rows.get(0).get("Create Table");
            if (!(ddl instanceof String sql) || !hash(normalizeShowCreate(sql).getBytes(StandardCharsets.UTF_8)).equals(expected))
                throw invalid("CURRENT_EXACT_SCHEMA_DRIFT");
        }
    }

    static String normalizeShowCreate(String sql) {
        return sql.replaceAll("(?i) AUTO_INCREMENT=[0-9]+(?=\\s|$)", "");
    }

    static boolean bitTrue(Object v) {
        return Boolean.TRUE.equals(v) || v instanceof Number n && n.intValue() == 1 || v instanceof byte[] b && b.length == 1 && b[0] == 1;
    }

    static boolean bitFalse(Object v) {
        return Boolean.FALSE.equals(v) || v instanceof Number n && n.intValue() == 0 || v instanceof byte[] b && b.length == 1 && b[0] == 0;
    }

    static void requireValue(Map<String, Object> row, String key, Object value) {
        if (!Objects.equals(row.get(key) == null ? null : String.valueOf(row.get(key)), value == null ? null : String.valueOf(value)))
            throw invalid("ACTUAL_FACT_MISMATCH_" + key.toUpperCase());
    }

    static Descriptor descriptor(JsonNode d, Path root) throws Exception {
        if (d != null && d.isObject() && d.has("bytes"))
            fields(d, "path", "sha256", "bytes");
        else
            fields(d, "path", "sha256");
        String expected = text(d, "sha256");
        if (!expected.matches("[0-9a-f]{64}"))
            throw invalid("ARTIFACT_HASH_REQUIRED");
        Path path = Path.of(text(d, "path")).toAbsolutePath().normalize();
        byte[] actual = bounded(path, root, 16 * 1024 * 1024);
        if (d.has("bytes") && (!d.get("bytes").isIntegralNumber() || !d.get("bytes").canConvertToLong() || d.get("bytes").longValue() < 0 || d.get("bytes").longValue() != actual.length))
            throw invalid("ARTIFACT_EXACT_BYTES_REQUIRED");
        return new Descriptor(path, expected);
    }

    static byte[] load(Descriptor d, Path root) throws Exception {
        byte[] body = bounded(d.path(), root, 16 * 1024 * 1024);
        if (!hash(body).equals(d.sha256()))
            throw invalid("ARTIFACT_BYTES_DRIFT");
        return body;
    }

    static byte[] bounded(Path path, Path root, int max) throws Exception {
        if (path == null || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path))
            throw invalid("PROTECTED_ARTIFACT_REQUIRED");
        Path real = path.toRealPath();
        if (!real.startsWith(root) || !real.equals(path.toAbsolutePath().normalize()))
            throw invalid("ARTIFACT_SCOPE_ESCAPE");
        try (InputStream in = Files.newInputStream(real, LinkOption.NOFOLLOW_LINKS)) {
            byte[] b = in.readNBytes(max + 1);
            if (b.length > max)
                throw invalid("ARTIFACT_TOO_LARGE");
            return b;
        }
    }

    static String hash(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    static boolean trueBoolean(JsonNode n, String k) {
        return n.get(k) != null && n.get(k).isBoolean() && n.get(k).booleanValue();
    }

    static boolean falseBoolean(JsonNode n, String k) {
        return n.get(k) != null && n.get(k).isBoolean() && !n.get(k).booleanValue();
    }

    static String text(JsonNode n, String k) {
        var v = n.get(k);
        if (v == null || !v.isTextual() || v.textValue().isBlank() || v.textValue().codePoints().anyMatch(Character::isISOControl))
            throw invalid("EXACT_TEXT_REQUIRED");
        return v.textValue();
    }

    static void fields(JsonNode n, String... expected) {
        if (n == null || !n.isObject())
            throw invalid("EXACT_OBJECT_REQUIRED");
        Set<String> keys = new HashSet<>();
        n.fieldNames().forEachRemaining(keys::add);
        if (!keys.equals(Set.of(expected)))
            throw invalid("UNKNOWN_OR_MISSING_FIELD");
    }

    static IllegalArgumentException invalid(String code) {
        return new IllegalArgumentException("DCC_MAINTENANCE_" + code);
    }
}
