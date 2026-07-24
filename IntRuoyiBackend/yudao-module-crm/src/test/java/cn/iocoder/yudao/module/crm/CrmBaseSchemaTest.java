package cn.iocoder.yudao.module.crm;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmBaseSchemaTest {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "@TableName\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*private\\s+(?!static)(?:final\\s+)?[\\w<>?,\\s]+?\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*;",
            Pattern.MULTILINE);
    private static final List<String> BASE_COLUMNS = List.of(
            "create_time", "update_time", "creator", "updater", "deleted", "tenant_id");

    @Test
    void mysqlSchemaShouldCoverEveryCrmDoTableAndColumn() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaFile = projectDir.resolve("sql/mysql/20260512_crm_base_schema.sql");

        assertTrue(Files.exists(schemaFile), "CRM MySQL schema file must exist");
        String schema = Files.readString(schemaFile);
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(schema).find(), "CRM schema repair must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?crm_", Pattern.CASE_INSENSITIVE)
                .matcher(schema).find(), "CRM schema repair must not delete CRM data");

        Path doRoot = projectDir.resolve(
                "yudao-module-crm/src/main/java/cn/iocoder/yudao/module/crm/dal/dataobject");
        List<Path> doFiles;
        try (var stream = Files.walk(doRoot)) {
            doFiles = stream
                    .filter(path -> path.getFileName().toString().endsWith("DO.java"))
                    .sorted()
                    .toList();
        }

        int coveredTables = 0;
        for (Path doFile : doFiles) {
            String source = Files.readString(doFile);
            String tableName = parseTableName(source);
            if (tableName == null || !tableName.startsWith("crm_")) {
                continue;
            }
            coveredTables++;
            String createBlock = findCreateBlock(schema, tableName);
            assertNotNull(createBlock, "Missing idempotent CREATE TABLE for " + tableName);
            for (String column : expectedColumns(source)) {
                assertTrue(Pattern.compile("`" + Pattern.quote(column) + "`\\s+", Pattern.CASE_INSENSITIVE)
                                .matcher(createBlock).find(),
                        "Missing column " + tableName + "." + column);
            }
        }

        assertTrue(coveredTables > 0, "CRM DO table scan must find tables");
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
                "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?" + Pattern.quote(tableName)
                        + "`?\\s*\\(([^;]+?)\\)\\s*ENGINE",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(schema);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-crm".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
