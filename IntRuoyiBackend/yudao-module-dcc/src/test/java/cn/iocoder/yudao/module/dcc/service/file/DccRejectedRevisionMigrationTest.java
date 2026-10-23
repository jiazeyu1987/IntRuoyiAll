package cn.iocoder.yudao.module.dcc.service.file;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class DccRejectedRevisionMigrationTest {
    @Test void forwardMigrationsKeepHistoryAndRequireExistingIdentityAndExactPredecessorUniqueness() throws Exception {
        var sql=Files.readString(Path.of("../sql/mysql/20261003_dcc_revision_failed_attempt_identity.sql"));
        assertTrue(sql.contains("dependsOn=20261002_dcc_c_initial_candidate_identity"));
        assertTrue(sql.contains("information_schema.COLUMNS"));assertTrue(sql.contains("information_schema.STATISTICS"));
        assertTrue(sql.contains("revision_attempt_no>1"));assertTrue(sql.contains("ELSE '' END"));assertTrue(sql.contains("GENERATED ALWAYS"));
        assertTrue(sql.contains("uk_dcc_c_rework_predecessor(tenant_id,master_id,rework_predecessor_controlled_file_id)"));
        assertFalse(sql.matches("(?is).*\\b(UPDATE|DELETE FROM|INSERT INTO)\\s+dcc_controlled_file\\b.*"));
        assertFalse(sql.contains("ADD COLUMN IF NOT EXISTS"));
        var config=Files.readString(Path.of("../sql/mysql/20261003_dcc_project_reviewer_configuration.sql"));
        assertFalse(config.matches("(?is).*\\b(UPDATE|INSERT INTO)\\s+dcc_project_product_create_request\\b.*"));
        assertTrue(config.contains("configured_reviewer_user_id"));assertTrue(config.contains("tenant_id BIGINT NOT NULL PRIMARY KEY"));
        assertFalse(config.contains("'admin'"));
    }
}
