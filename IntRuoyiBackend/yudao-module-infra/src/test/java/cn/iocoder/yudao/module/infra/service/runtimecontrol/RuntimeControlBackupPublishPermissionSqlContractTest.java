package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlBackupPublishPermissionSqlContractTest {

    private static final Path MIGRATION = Path.of(System.getProperty("user.dir"))
            .resolve("../sql/mysql/20260922_runtime_control_backup_publish_permission.sql")
            .normalize();
    private static final Path PREFLIGHT = Path.of(System.getProperty("user.dir"))
            .resolve("../sql/mysql/target-preflight/20260922_runtime_control_backup_publish_permission.preflight.sql")
            .normalize();

    @Test
    void migrationMustCreateTheDedicatedPublishPermissionWithoutOverwritingConflicts() throws Exception {
        assertTrue(Files.isRegularFile(MIGRATION), "backup publish permission migration must exist");
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
        assertTrue(sql.contains("infra:runtime-control:publish-backup"));
        assertTrue(sql.contains("运行控制台发布审查服务器"));
        assertTrue(sql.contains("RELEASE_BACKUP_PUBLISH_PARENT_AMBIGUOUS"));
        assertTrue(sql.contains("RELEASE_BACKUP_PUBLISH_PERMISSION_CONFLICT"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("START TRANSACTION"));
        assertTrue(sql.contains("COMMIT"));
        assertTrue(sql.contains("ROLLBACK"));
        assertTrue(sql.contains("`name`, `permission`, `type`"));
        assertTrue(sql.contains("`parent_id`, `path`, `icon`, `component`"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(!sql.contains("900105"), "menu primary key must be allocated by MySQL");
        assertTrue(!sql.contains("RELEASE_BACKUP_PUBLISH_MENU_ID_CONFLICT"));
        assertTrue(!sql.contains("(`id`, `name`, `permission`"), "menu insert must not provide a fixed id");
        assertTrue(!sql.contains("ON DUPLICATE KEY UPDATE"));
    }

    @Test
    void targetPreflightMustDetectParentAndPermissionIdentityDrift() throws Exception {
        assertTrue(Files.isRegularFile(PREFLIGHT), "backup publish permission preflight must exist");
        String sql = Files.readString(PREFLIGHT, StandardCharsets.UTF_8);
        assertTrue(sql.contains("migrationId=20260922_runtime_control_backup_publish_permission"));
        assertTrue(sql.contains("TARGET_PREFLIGHT_PASS:20260922_runtime_control_backup_publish_permission"));
        assertTrue(sql.contains("TARGET_PREFLIGHT_BLOCKED:20260922_runtime_control_backup_publish_permission"));
        assertTrue(sql.contains("infra:runtime-control:publish-backup"));
        assertTrue(sql.contains("parent_id"));
    }
}
