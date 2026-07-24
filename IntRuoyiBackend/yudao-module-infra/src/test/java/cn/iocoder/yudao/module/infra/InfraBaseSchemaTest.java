package cn.iocoder.yudao.module.infra;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraBaseSchemaTest {

    @Test
    void mysqlBaselineShouldUseLongblobForInfraFileContent() throws Exception {
        Path projectDir = findProjectDir();
        Path schemaFile = projectDir.resolve("sql/mysql/ruoyi-vue-pro.sql");

        assertTrue(Files.exists(schemaFile), "MySQL baseline schema must exist");
        String schema = Files.readString(schemaFile);
        String createBlock = findCreateBlock(schema, "infra_file_content");

        assertNotNull(createBlock, "infra_file_content table definition must exist in MySQL baseline");
        assertTrue(Pattern.compile("`content`\\s+longblob\\s+not\\s+null", Pattern.CASE_INSENSITIVE)
                        .matcher(createBlock).find(),
                "infra_file_content.content must use longblob in MySQL baseline");
    }

    @Test
    void mysqlMigrationShouldUpgradeInfraFileContentToLongblob() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260517_infra_file_content_longblob.sql");

        assertTrue(Files.exists(migrationFile), "infra_file_content longblob migration must exist");
        String migration = Files.readString(migrationFile);

        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(migration).find(),
                "infra file migration must not contain destructive table operations");
        assertTrue(migration.contains("ALTER TABLE `infra_file_content`"),
                "infra file migration must alter infra_file_content");
        assertTrue(Pattern.compile("MODIFY\\s+COLUMN\\s+`content`\\s+LONGBLOB\\s+NOT\\s+NULL", Pattern.CASE_INSENSITIVE)
                        .matcher(migration).find(),
                "infra file migration must upgrade content to longblob");
    }

    private static String findCreateBlock(String schema, String tableName) {
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+`?" + Pattern.quote(tableName)
                        + "`?\\s*\\(([^;]+?)\\)\\s*(?:ENGINE|;)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(schema);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-infra".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
