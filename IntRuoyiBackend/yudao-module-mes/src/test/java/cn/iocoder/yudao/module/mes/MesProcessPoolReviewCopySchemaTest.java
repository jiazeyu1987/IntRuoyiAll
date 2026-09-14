package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyFieldDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolReviewCopySchemaTest {

    @Test
    void shouldCreateReviewCopyTables() throws Exception {
        assertEquals("mes_pro_process_pool_review_copy", tableName(MesProcessPoolReviewCopyDO.class));
        assertEquals("mes_pro_process_pool_review_copy_field", tableName(MesProcessPoolReviewCopyFieldDO.class));

        assertField(MesProcessPoolReviewCopyDO.class, "eventId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "processPoolId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "routeId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "processId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "feedbackSourceType", String.class);
        assertField(MesProcessPoolReviewCopyDO.class, "feedbackSourceId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "recordbookSourceType", String.class);
        assertField(MesProcessPoolReviewCopyDO.class, "recordbookSourceId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "rawPayloadSnapshot", String.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewStatus", String.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewerUserId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewerSignatureId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewerSignatureUserId", Long.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewerSignatureSnapshot", String.class);
        assertField(MesProcessPoolReviewCopyDO.class, "reviewedAt", LocalDateTime.class);

        assertField(MesProcessPoolReviewCopyFieldDO.class, "reviewCopyId", Long.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "eventId", Long.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "sourceQuantityFragmentId", Long.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "fieldCode", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "fieldName", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "rawValue", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "correctedValue", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "ruleType", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "lowerLimit", BigDecimal.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "upperLimit", BigDecimal.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "valueType", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "affectsAllocation", Boolean.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "feedbackSourceType", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "feedbackSourceId", Long.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "recordbookSourceType", String.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "recordbookSourceId", Long.class);
        assertField(MesProcessPoolReviewCopyFieldDO.class, "templateFieldMetadataJson", String.class);

        String sql = Files.readString(resolveBackendPath("sql/mysql/20260730_mes_process_pool_review_copy.sql"),
                StandardCharsets.UTF_8);
        String normalizedSql = sql.replace("\r\n", "\n");
        assertTrue(normalizedSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260730_mes_process_pool_fifo_allocation; type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_review_copy`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_review_copy_field`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_review_copy_signature` (`tenant_id`, `reviewer_signature_id`, `deleted`)"));
        assertTrue(sql.contains("KEY `idx_mes_pp_review_copy_event` (`tenant_id`, `event_id`)"));
        assertTrue(sql.contains("KEY `idx_mes_pp_review_copy_field_event` (`tenant_id`, `event_id`, `field_code`)"));
        assertFalse(sql.contains("mes_pro_feedback_surplus_pool"),
                "F5 review copy must belong to process pool and must not reuse surplus pool");
    }

    private static String tableName(Class<?> clazz) {
        return clazz.getAnnotation(TableName.class).value();
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
