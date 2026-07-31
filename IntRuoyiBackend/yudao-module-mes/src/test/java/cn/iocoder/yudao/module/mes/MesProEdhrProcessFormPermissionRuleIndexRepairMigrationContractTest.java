package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrProcessFormPermissionRuleIndexRepairMigrationContractTest {

    private static final String MIGRATION_FILE =
            "sql/mysql/20260723_mes_edhr_process_form_permission_rule_version_index_repair.sql";

    @Test
    void migrationReplacesIntermediateUniqueIndexWhenScopeKeyIsMissing() throws Exception {
        String migration = normalize(readMigration());

        assertTrue(migration.contains("batch_record_version_id"),
                "index repair must keep route-versioned permission rules version scoped");
        assertTrue(migration.contains("scope_key"),
                "index repair must include assist-row scope_key in the unique key");
        assertTrue(migration.contains("signature_cell_key"),
                "index repair must keep signature cell key in the unique key");
        assertTrue(Pattern.compile("SUM\\(CASE WHEN column_name = 'scope_key' THEN 1 ELSE 0 END\\) = 0",
                        Pattern.CASE_INSENSITIVE).matcher(migration).find(),
                "index repair must also replace intermediate indexes that already include version but miss scope_key");
        assertTrue(Pattern.compile("ADD UNIQUE KEY uk_mes_pro_edhr_process_form_rule \\(tenant_id, route_process_id, batch_record_report_id, batch_record_version_id, rule_type, scope_key, signature_cell_key, deleted\\)",
                        Pattern.CASE_INSENSITIVE).matcher(migration).find(),
                "recreated unique key must include batch_record_version_id and scope_key");
        assertFalse(Pattern.compile("\\b(DELETE\\s+FROM\\s+`?mes_|TRUNCATE\\s+TABLE|DROP\\s+TABLE\\s+`?mes_)",
                        Pattern.CASE_INSENSITIVE).matcher(migration).find(),
                "index repair must not destructively remove MES business data");
    }

    private static String readMigration() throws Exception {
        Path migration = findProjectDir().resolve(MIGRATION_FILE);
        assertTrue(Files.exists(migration), "Migration file must exist: " + MIGRATION_FILE);
        return Files.readString(migration, StandardCharsets.UTF_8);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("`", "")
                .replace("\"", "")
                .replace("\r", "\n");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
