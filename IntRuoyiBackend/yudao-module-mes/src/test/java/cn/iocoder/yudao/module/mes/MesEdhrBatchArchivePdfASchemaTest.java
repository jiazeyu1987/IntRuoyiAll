package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesEdhrBatchArchivePdfASchemaTest {

    private static final String MIGRATION = "20260831_mes_edhr_batch_archive_pdfa.sql";

    @Test
    void migrationAddsNullablePdfAArchiveEvidenceWithoutLegacyBackfill() throws Exception {
        String sql = readRepositoryFile("sql/mysql/" + MIGRATION);

        assertTrue(sql.contains("release-migration: allowedEnvironments=test,backup,prod"));
        assertTrue(sql.contains("TABLE_NAME = 'mes_pro_edhr_batch_execution_archive'"));
        for (String column : new String[]{"file_id", "storage_retention_json", "pdfa_profile",
                "pdfa_validation_status", "pdfa_validated_at"}) {
            assertTrue(sql.contains("COLUMN_NAME = '" + column + "'"), "missing idempotent guard for " + column);
            assertTrue(sql.contains("ADD COLUMN `" + column + "`"), "missing additive column " + column);
        }
        assertFalse(sql.toLowerCase().contains("update `mes_pro_edhr_batch_execution_archive`"),
                "legacy archives must not be backfilled as PDF/A");
    }

    @Test
    void h2FixtureContainsPdfAArchiveEvidenceColumns() throws Exception {
        String fixture = readRepositoryFile("yudao-module-mes/src/test/resources/sql/create_tables.sql");
        int tableStart = fixture.indexOf("CREATE TABLE IF NOT EXISTS \"mes_pro_edhr_batch_execution_archive\"");
        int tableEnd = fixture.indexOf(");", tableStart);
        String table = fixture.substring(tableStart, tableEnd);

        for (String column : new String[]{"file_id", "storage_retention_json", "pdfa_profile",
                "pdfa_validation_status", "pdfa_validated_at"}) {
            assertTrue(table.contains("\"" + column + "\""), "H2 fixture missing " + column);
        }
    }

    private String readRepositoryFile(String relativePath) throws Exception {
        Path moduleRoot = Path.of("").toAbsolutePath();
        Path backendRoot = moduleRoot.getFileName() != null
                && "yudao-module-mes".equals(moduleRoot.getFileName().toString())
                ? moduleRoot.getParent() : moduleRoot;
        Path file = backendRoot.resolve(relativePath).normalize();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
