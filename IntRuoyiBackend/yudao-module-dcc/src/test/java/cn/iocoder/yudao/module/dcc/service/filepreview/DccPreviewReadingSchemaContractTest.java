package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePreviewViewSessionMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class DccPreviewReadingSchemaContractTest {
    private String schema() throws Exception {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path backend = "yudao-module-dcc".equals(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
        return Files.readString(backend.resolve("sql/mysql/20261015_dcc_preview_reading_session.sql"), StandardCharsets.UTF_8);
    }
    @Test void migrationCreatesOnlyNewEvidenceAndEnforcesClientIdempotencyAndOneActiveSession() throws Exception {
        String sql = schema();
        assertTrue(sql.startsWith("-- release-migration:"));
        assertTrue(sql.contains("dependsOn=20260513_dcc_base_schema,20260528_dcc_controlled_file_protection"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"));
        assertTrue(sql.contains("GENERATED ALWAYS AS (CASE WHEN `ended_at` IS NULL THEN 1 ELSE NULL END) STORED"));
        assertTrue(sql.contains("UNIQUE KEY `uk_dcc_preview_client` (`tenant_id`, `access_event_id`, `user_id`, `client_session_id`)"));
        assertTrue(sql.contains("UNIQUE KEY `uk_dcc_preview_active` (`tenant_id`, `resource_type`, `controlled_file_id`, `user_id`, `active_marker`)"));
        assertFalse(sql.matches("(?is).*\\b(UPDATE|INSERT|DELETE|ALTER)\\s+(TABLE|INTO|FROM)?\\s*`?dcc_.*"));
        assertFalse(sql.contains("`viewer_token`"));
    }
    @Test void mapperUsesResourceLocksTenantScopedSessionsAndExactVersionSummary() {
        for (var method : DccControlledFilePreviewViewSessionMapper.class.getDeclaredMethods()) {
            String sql = String.join(" ", method.getAnnotation(Select.class).value());
            if (!method.getName().equals("lockInfraFile")) assertTrue(sql.contains("tenant_id=#{tenantId}"), method.getName());
            if (method.getName().endsWith("AfterResourceLock")) assertFalse(sql.contains("FOR UPDATE"), method.getName());
            else if (!method.getName().equals("selectSummary")) assertTrue(sql.contains("FOR UPDATE"), method.getName());
            else {
                assertTrue(sql.contains("file_version_no=#{version}"));
                assertTrue(sql.contains("user_id=#{userId}"));
                assertTrue(sql.contains("MIN(started_at)"));
                assertTrue(sql.contains("SUM(accumulated_seconds)"));
                assertFalse(sql.contains("SUM(TIMESTAMPDIFF"));
            }
        }
    }
}
