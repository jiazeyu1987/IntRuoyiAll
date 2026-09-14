package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccDataScopePermissionSchemaTest {

    @Test
    void migrationCreatesExplicitDataScopePermissionsWithoutGrantingWenkongRole() throws Exception {
        Path projectDir = Path.of("").toAbsolutePath().normalize();
        if (!Files.isDirectory(projectDir.resolve("sql/mysql"))) {
            projectDir = projectDir.getParent();
        }
        String sql = Files.readString(projectDir.resolve(
                "sql/mysql/20260813_dcc_explicit_data_scope_permissions.sql"), StandardCharsets.UTF_8);

        assertTrue(sql.contains("dcc:project-code:scope:all"));
        assertTrue(sql.contains("dcc:controlled-file:scope:all"));
        assertTrue(sql.contains("role_admin.`code` IN ('super_admin', 'admin')"));
        assertTrue(sql.contains("CONVERT(UNHEX("));
        assertFalse(sql.contains("SELECT 990230"));
        assertFalse(sql.contains("SELECT 990231"));
        assertFalse(sql.contains("'wenkong'"));
        assertFalse(sql.contains("'doc_control'"));
    }

    @Test
    void migrationCreatesAssignmentCandidateLatestVersionIndex() throws Exception {
        Path projectDir = Path.of("").toAbsolutePath().normalize();
        if (!Files.isDirectory(projectDir.resolve("sql/mysql"))) {
            projectDir = projectDir.getParent();
        }
        String sql = Files.readString(projectDir.resolve(
                "sql/mysql/20260813_dcc_project_assignment_candidate_index.sql"), StandardCharsets.UTF_8);

        assertTrue(sql.contains("idx_dcc_controlled_file_assignment_latest"));
        assertTrue(sql.contains("(`tenant_id`, `master_id`, `deleted`, `id`)"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
    }
}
