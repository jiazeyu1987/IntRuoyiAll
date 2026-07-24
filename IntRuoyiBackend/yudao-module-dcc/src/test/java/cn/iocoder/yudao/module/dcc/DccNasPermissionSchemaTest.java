package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccNasPermissionSchemaTest {

    private static final List<String> NAS_ACL_TABLES = List.of(
            "dcc_nas_acl_snapshot",
            "dcc_nas_acl_directory_snapshot",
            "dcc_nas_acl_descriptor",
            "dcc_nas_acl_ace",
            "dcc_nas_acl_identity_mapping",
            "dcc_nas_acl_restore_plan",
            "dcc_nas_acl_restore_plan_item",
            "dcc_nas_acl_restore_log"
    );
    private static final List<String> REQUIRED_BASE_COLUMNS = List.of(
            "tenant_id", "deleted", "create_time", "update_time"
    );
    private static final Map<String, String> REQUIRED_UNIQUE_KEYS = Map.of(
            "dcc_nas_acl_snapshot", "uk_dcc_nas_acl_snapshot_key",
            "dcc_nas_acl_directory_snapshot", "uk_dcc_nas_acl_dir_snapshot_path",
            "dcc_nas_acl_descriptor", "uk_dcc_nas_acl_descriptor_hash",
            "dcc_nas_acl_ace", "uk_dcc_nas_acl_ace_order",
            "dcc_nas_acl_identity_mapping", "uk_dcc_nas_acl_identity_sid",
            "dcc_nas_acl_restore_plan", "uk_dcc_nas_acl_restore_plan_key",
            "dcc_nas_acl_restore_plan_item", "uk_dcc_nas_acl_restore_item_dir",
            "dcc_nas_acl_restore_log", "uk_dcc_nas_acl_restore_log_attempt"
    );
    private static final Map<String, Integer> REQUIRED_RESTORE_PLAN_VERSION_COLUMNS = Map.of(
            "semantic_policy_version", 64,
            "identity_mapping_version", 64
    );
    private static final Map<String, Map<String, Integer>> REQUIRED_RESTORE_HASH_COLUMNS = Map.of(
            "dcc_nas_acl_restore_plan_item", Map.of(
                    "expected_after_hash", 128,
                    "actual_after_hash", 128
            ),
            "dcc_nas_acl_restore_log", Map.of(
                    "before_hash", 128,
                    "expected_after_hash", 128,
                    "actual_after_hash", 128
            )
    );

    @Test
    void mysqlRuntimeAndTestSchemaShouldContainNasAclSnapshotRestoreTables() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = readRuntimeMysqlSchema(projectDir);
        String testSchema = Files.readString(
                projectDir.resolve("yudao-module-dcc/src/test/resources/sql/create_tables.sql"));

        List<String> failures = new ArrayList<>();
        failures.addAll(findNasAclSchemaFailures(runtimeSchema, "runtime mysql"));
        failures.addAll(findNasAclSchemaFailures(testSchema, "dcc test"));
        assertTrue(failures.isEmpty(), String.join("; ", failures));
    }

    private static String readRuntimeMysqlSchema(Path projectDir) throws Exception {
        Path mysqlDir = projectDir.resolve("sql/mysql");
        assertTrue(Files.isDirectory(mysqlDir), "runtime mysql schema directory must exist");
        try (var stream = Files.list(mysqlDir)) {
            List<Path> sqlFiles = stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
            assertTrue(!sqlFiles.isEmpty(), "runtime mysql schema files must exist");
            return sqlFiles.stream()
                    .map(DccNasPermissionSchemaTest::readString)
                    .collect(Collectors.joining("\n"));
        }
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read SQL file: " + path, ex);
        }
    }

    private static List<String> findNasAclSchemaFailures(String schema, String schemaName) {
        List<String> failures = new ArrayList<>();
        for (String tableName : NAS_ACL_TABLES) {
            String createBlock = findCreateBlock(schema, tableName);
            if (createBlock == null) {
                failures.add("Missing table " + tableName + " in " + schemaName + " schema");
                continue;
            }
            for (String column : REQUIRED_BASE_COLUMNS) {
                if (!containsColumn(createBlock, column)) {
                    failures.add("Missing column " + tableName + "." + column + " in " + schemaName + " schema");
                }
            }
            String keyName = REQUIRED_UNIQUE_KEYS.get(tableName);
            if (!containsKeyOrIndex(createBlock, keyName)) {
                failures.add("Missing key/index " + keyName + " on " + tableName + " in " + schemaName + " schema");
            }
            if ("dcc_nas_acl_restore_plan".equals(tableName)) {
                for (Map.Entry<String, Integer> entry : REQUIRED_RESTORE_PLAN_VERSION_COLUMNS.entrySet()) {
                    if (!hasVarcharLengthAtLeast(createBlock, entry.getKey(), entry.getValue())) {
                        failures.add("Column " + tableName + "." + entry.getKey() + " must be varchar("
                                + entry.getValue() + ") or wider in " + schemaName + " schema");
                    }
                }
            }
            Map<String, Integer> requiredHashColumns = REQUIRED_RESTORE_HASH_COLUMNS.get(tableName);
            if (requiredHashColumns != null) {
                for (Map.Entry<String, Integer> entry : requiredHashColumns.entrySet()) {
                    if (!hasVarcharLengthAtLeast(createBlock, entry.getKey(), entry.getValue())) {
                        failures.add("Column " + tableName + "." + entry.getKey() + " must be varchar("
                                + entry.getValue() + ") or wider in " + schemaName + " schema");
                    }
                }
            }
        }
        return failures;
    }

    private static String findCreateBlock(String schema, String tableName) {
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?" + Pattern.quote(tableName)
                        + "`?\\s*\\(([^;]+?)\\)\\s*(?:ENGINE|;)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(schema);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean containsColumn(String createBlock, String column) {
        return Pattern.compile("`" + Pattern.quote(column) + "`\\s+", Pattern.CASE_INSENSITIVE)
                .matcher(createBlock).find();
    }

    private static boolean containsKeyOrIndex(String createBlock, String keyName) {
        return Pattern.compile("`" + Pattern.quote(keyName) + "`", Pattern.CASE_INSENSITIVE)
                .matcher(createBlock).find();
    }

    private static boolean hasVarcharLengthAtLeast(String createBlock, String column, int expectedLength) {
        Matcher matcher = Pattern.compile("`" + Pattern.quote(column) + "`\\s+(?:var)?char\\((\\d+)\\)",
                        Pattern.CASE_INSENSITIVE)
                .matcher(createBlock);
        return matcher.find() && Integer.parseInt(matcher.group(1)) >= expectedLength;
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-dcc".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
