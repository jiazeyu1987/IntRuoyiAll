package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesFrontlineInitialAllocationSchemaTest {

    @Test
    void migrationMustAllowAllocationBeforeLeaderReviewAndDeclareFormalMode() throws Exception {
        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260814_mes_frontline_selected_initial_allocation.sql"), StandardCharsets.UTF_8);

        assertTrue(sql.contains("MODIFY COLUMN `review_id` bigint DEFAULT NULL"));
        assertTrue(sql.contains("MODIFY COLUMN `last_review_id` bigint DEFAULT NULL"));
        assertTrue(sql.contains("FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM"));
        assertTrue(sql.contains("table_name = 'mes_pro_process_pool_order_process_completion'"));
        assertTrue(sql.contains("column_name = 'last_review_id'"));
    }

    private static Path resolveBackendPath(String relativePath) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relativePath);
        }
        return cwd.resolve("IntRuoyiBackend").resolve(relativePath);
    }
}
