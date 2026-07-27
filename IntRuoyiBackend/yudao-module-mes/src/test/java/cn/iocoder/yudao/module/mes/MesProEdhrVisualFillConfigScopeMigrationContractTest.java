package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrVisualFillConfigScopeMigrationContractTest {

    private static final String MIGRATION_FILE =
            "sql/mysql/20260727_mes_edhr_visual_fill_config_scope.sql";

    @Test
    void migrationConvertsLegacyFillRuleToExplicitAllScopeWithFrozenCells() throws Exception {
        String migration = normalize(readMigration());

        assertTrue(migration.contains("dependsOn=20260723_mes_edhr_process_form_permission_rule_version_index_repair"),
                "visual fill scope migration must run after version-scoped permission indexes");
        assertTrue(migration.contains("ADD COLUMN scope_key"), "migration must add scope_key");
        assertTrue(migration.contains("ADD COLUMN fillable_scope_json"), "migration must add fillable_scope_json");
        assertTrue(migration.contains("jimu_report"), "migration must use the immutable Jimu report json source");
        assertTrue(migration.contains("json_str"), "migration must parse Jimu json_str");
        assertTrue(migration.contains("JSON_TABLE"), "migration must parse fillable cells through JSON_TABLE");
        assertTrue(migration.contains("$.rows.*.cells.*"), "migration must read cells from the report snapshot");
        assertTrue(migration.contains("$.edhrCellRule.rowIndex"), "migration must use reviewed cell-rule row indexes");
        assertTrue(migration.contains("$.edhrCellRule.columnIndex"), "migration must use reviewed cell-rule column indexes");
        assertTrue(migration.contains("JSON_OBJECT('schemaVersion', 2"),
                "legacy ALL scope must be backfilled as schemaVersion=2 precise cells");
        assertTrue(migration.contains("JSON_ARRAYAGG(JSON_OBJECT("),
                "legacy ALL scope must aggregate precise sourceTableIndex,rowIndex,columnIndex cells");
        assertTrue(migration.contains("SET scope_key = 'ALL'"), "legacy fill rules must become explicit ALL scope");
        assertTrue(migration.contains("SET MESSAGE_TEXT = @missing_version_message"),
                "missing version source must fail fast with the blocking rule id");
        assertTrue(migration.contains("SET MESSAGE_TEXT = @missing_scope_message"),
                "missing fillable cell source must fail fast with the blocking rule id");
        assertFalse(Pattern.compile("\\b(DELETE\\s+FROM\\s+`?mes_|TRUNCATE\\s+TABLE|DROP\\s+TABLE\\s+`?mes_)",
                        Pattern.CASE_INSENSITIVE).matcher(migration).find(),
                "migration must not destructively remove MES business data");
    }

    private static String readMigration() throws Exception {
        Path projectDir = findProjectDir();
        Path migration = projectDir.resolve(MIGRATION_FILE);
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
