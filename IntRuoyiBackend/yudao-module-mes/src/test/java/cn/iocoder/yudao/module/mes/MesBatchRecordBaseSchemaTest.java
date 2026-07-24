package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesBatchRecordBaseSchemaTest {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "@TableName\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*private\\s+(?!static)(?:final\\s+)?[\\w<>?,\\s]+?\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*;",
            Pattern.MULTILINE);
    private static final List<String> BASE_COLUMNS = List.of(
            "create_time", "update_time", "creator", "updater", "deleted", "tenant_id");

    @Test
    void runtimeAndTestSchemasShouldCoverBatchRecordTables() throws Exception {
        Path projectDir = findProjectDir();
        Path runtimeSchemaFile = projectDir.resolve("sql/mysql/20260512_mes_base_schema.sql");
        Path archiveSchemaFile = projectDir.resolve("sql/mysql/20260525_edhr_archive_schema.sql");
        Path fieldAuditSchemaFile = projectDir.resolve("sql/mysql/20260526_edhr_field_audit_schema.sql");
        Path approvalArchiveSchemaFile = projectDir.resolve("sql/mysql/20260526_edhr_approval_archive_schema_contract.sql");
        Path domainTraceSchemaFile = projectDir.resolve("sql/mysql/20260528_edhr_domain_trace_schema.sql");
        Path archiveWormGuardSchemaFile = projectDir.resolve("sql/mysql/20260528_edhr_archive_worm_guard.sql");
        Path multiSignatureSchemaFile = projectDir.resolve("sql/mysql/20260611_mes_edhr_multi_signature_approval.sql");
        Path rejectionRevisionSchemaFile = projectDir.resolve("sql/mysql/20260611_mes_edhr_rejection_revision_flow.sql");
        Path voidReopenSupplementSchemaFile = projectDir.resolve("sql/mysql/20260612_mes_edhr_void_reopen_supplement.sql");
        Path tailGoalSchemaFile = projectDir.resolve("sql/mysql/20260615_mes_edhr_tail_four_goals.sql");
        Path testSchemaFile = projectDir.resolve("yudao-module-mes/src/test/resources/sql/create_tables.sql");

        assertTrue(Files.exists(runtimeSchemaFile), "MES MySQL base schema file must exist");
        assertTrue(Files.exists(archiveSchemaFile), "MES archive schema file must exist");
        assertTrue(Files.exists(fieldAuditSchemaFile), "MES field audit schema file must exist");
        assertTrue(Files.exists(approvalArchiveSchemaFile), "MES approval archive schema file must exist");
        assertTrue(Files.exists(domainTraceSchemaFile), "MES domain trace schema file must exist");
        assertTrue(Files.exists(archiveWormGuardSchemaFile), "MES archive WORM guard schema file must exist");
        assertTrue(Files.exists(multiSignatureSchemaFile), "MES multi signature schema file must exist");
        assertTrue(Files.exists(rejectionRevisionSchemaFile), "MES rejection revision schema file must exist");
        assertTrue(Files.exists(voidReopenSupplementSchemaFile), "MES void/reopen/supplement schema file must exist");
        assertTrue(Files.exists(tailGoalSchemaFile), "MES eDHR tail goals schema file must exist");
        assertTrue(Files.exists(testSchemaFile), "MES test schema file must exist");

        String runtimeSchema = Files.readString(runtimeSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(archiveSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(fieldAuditSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(approvalArchiveSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(domainTraceSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(archiveWormGuardSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(multiSignatureSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(rejectionRevisionSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(voidReopenSupplementSchemaFile, StandardCharsets.UTF_8)
                + "\n" + Files.readString(tailGoalSchemaFile, StandardCharsets.UTF_8);
        String testSchema = Files.readString(testSchemaFile, StandardCharsets.UTF_8);
        assertSchemaIsNonDestructive(runtimeSchema, "runtime");
        assertSchemaIsNonDestructive(testSchema, "test");

        for (Path doFile : batchRecordDoFiles(projectDir)) {
            assertTrue(Files.exists(doFile), "Batch-record DO file must exist: " + doFile);
            String source = Files.readString(doFile);
            String tableName = parseTableName(source);
            assertNotNull(tableName, "Batch-record DO must declare @TableName: " + doFile);
            assertTrue(tableName.startsWith("mes_pro_batch_record_"),
                    "Batch-record DO must map to mes_pro_batch_record_* table: " + doFile);

            assertSchemaHasExpectedColumns(runtimeSchema, tableName, expectedColumns(source), "runtime");
            assertSchemaHasExpectedColumns(testSchema, tableName, expectedColumns(source), "test");
        }
    }

    private static List<Path> batchRecordDoFiles(Path projectDir) {
        return List.of(
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordTemplateDO.java"),
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionDO.java"),
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionSignatureDO.java"),
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionArchiveDO.java"),
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProBatchRecordExecutionArchiveEventDO.java"),
                projectDir.resolve(
                        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecordreport/MesProBatchRecordReportDO.java"));
    }

    private static void assertSchemaIsNonDestructive(String schema, String schemaName) {
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "MES " + schemaName + " schema must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?mes_", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "MES " + schemaName + " schema must not delete MES data");
    }

    private static void assertSchemaHasExpectedColumns(String schema, String tableName, List<String> columns,
                                                       String schemaName) {
        String createBlock = findCreateBlock(schema, tableName);
        assertNotNull(createBlock, "Missing CREATE TABLE for " + tableName + " in " + schemaName + " schema");
        for (String column : columns) {
            assertTrue(columnExistsInCreateBlock(createBlock, column) || columnExistsInAlterTable(schema, tableName, column),
                    "Missing column " + tableName + "." + column + " in " + schemaName + " schema");
        }
    }

    private static boolean columnExistsInCreateBlock(String createBlock, String column) {
        return Pattern.compile("(?:[`\"])" + Pattern.quote(column) + "(?:[`\"])\\s+|"
                        + Pattern.quote(column) + "\\s+", Pattern.CASE_INSENSITIVE)
                .matcher(createBlock).find();
    }

    private static boolean columnExistsInAlterTable(String schema, String tableName, String column) {
        Pattern pattern = Pattern.compile(
                "ALTER\\s+TABLE\\s+[`\"]?" + Pattern.quote(tableName) + "[`\"]?\\s+ADD\\s+COLUMN\\s+[`\"]?"
                        + Pattern.quote(column) + "[`\"]?\\s+",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (pattern.matcher(schema).find()) {
            return true;
        }
        Pattern helperPattern = Pattern.compile(
                "ensure_[a-z0-9_]+_column\\s*\\(\\s*['\"]" + Pattern.quote(tableName)
                        + "['\"]\\s*,\\s*['\"]" + Pattern.quote(column) + "['\"]",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return helperPattern.matcher(schema).find();
    }

    private static String parseTableName(String source) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static List<String> expectedColumns(String source) {
        List<String> columns = new ArrayList<>();
        Matcher matcher = FIELD_PATTERN.matcher(source);
        while (matcher.find()) {
            columns.add(camelToSnake(matcher.group(1)));
        }
        for (String column : BASE_COLUMNS) {
            if (!columns.contains(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private static String findCreateBlock(String schema, String tableName) {
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+[`\"]?" + Pattern.quote(tableName)
                        + "[`\"]?\\s*\\(([^;]+?)\\)\\s*(?:ENGINE|;)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(schema);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
