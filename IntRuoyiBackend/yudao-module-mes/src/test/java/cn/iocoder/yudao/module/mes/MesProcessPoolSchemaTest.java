package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolSchemaTest {

    @Test
    void shouldCreateDedicatedProcessPoolTables() throws Exception {
        assertEquals("mes_pro_process_pool", tableName(MesProProcessPoolDO.class));
        assertEquals("mes_pro_process_pool_event", tableName(MesProProcessPoolEventDO.class));
        assertEquals("mes_pro_process_pool_quantity_fragment", tableName(MesProProcessPoolQuantityFragmentDO.class));
        assertEquals("mes_pro_process_pool_pqc_record", tableName(MesProProcessPoolPqcRecordDO.class));
        assertEquals("mes_pqc_process_inspection_aggregate_detail",
                tableName(MesPqcProcessInspectionAggregateDetailDO.class));
        assertNotEquals("mes_pro_feedback_surplus_pool", tableName(MesProProcessPoolDO.class));

        assertField(MesProProcessPoolEventDO.class, "poolId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "eventIdempotencyKey", String.class);
        assertField(MesProProcessPoolEventDO.class, "workOrderId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "routeId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "routeProcessId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "processId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "actualEmployeeId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "deviceAccountId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "deviceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "workstationId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "templateType", String.class);
        assertField(MesProProcessPoolEventDO.class, "feedbackSourceType", String.class);
        assertField(MesProProcessPoolEventDO.class, "feedbackSourceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookEntryId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookSourceType", String.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookSourceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "rawPayload", String.class);
        assertField(MesProProcessPoolEventDO.class, "serverSubmitTime", java.time.LocalDateTime.class);
        assertField(MesProProcessPoolEventDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "signatureUserId", Long.class);

        assertField(MesProProcessPoolQuantityFragmentDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "productionSubmitEventId", Long.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "sourceQuantityType", String.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "totalQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "allocatedQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "availableQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "allocationStatus", String.class);

        assertField(MesProProcessPoolPqcRecordDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "productionSubmitEventId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "inspectionResult", String.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "actualEmployeeId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "serverSubmitTime", java.time.LocalDateTime.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "processInspectionAggregationStatus", String.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "processInspectionReviewId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "processInspectionAggregatedAt", java.time.LocalDateTime.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "sourcePqcRecordId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "sourcePieceDetailId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "eventId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "reviewId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "productionSubmitEventId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "pqcTaskId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "regulationVersionId", Long.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "inspectionType", String.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "roundNo", Integer.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "sampleNo", Integer.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "itemCode", String.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "measuredValue", String.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "judgement", String.class);
        assertField(MesPqcProcessInspectionAggregateDetailDO.class, "aggregatedAt", java.time.LocalDateTime.class);

        String sql = Files.readString(resolveBackendPath("sql/mysql/20260730_mes_process_pool_foundation.sql"),
                StandardCharsets.UTF_8);
        String normalizedSql = sql.replace("\r\n", "\n");
        assertTrue(normalizedSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260512_mes_base_schema; type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_quantity_fragment`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_pqc_record`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pro_process_pool_event_signature` (`tenant_id`, `signature_id`, `deleted`)"));
        assertTrue(sql.contains("KEY `idx_mes_pro_process_pool_event_time` (`tenant_id`, `server_submit_time`)"));
        assertFalse(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool`"),
                "F1 must not reuse the feedback surplus pool as the process pool table");

        String pqcBindingSql = Files.readString(
                resolveBackendPath("sql/mysql/20260803_mes_process_pool_pqc_structured_binding.sql"),
                StandardCharsets.UTF_8);
        assertTrue(pqcBindingSql.contains("production_submit_event_id"));
        assertTrue(pqcBindingSql.contains("idx_mes_pro_process_pool_pqc_submit_event"));
        assertTrue(pqcBindingSql.contains("requires formal production_submit_event_id backfill"));

        String idempotencySql = Files.readString(
                resolveBackendPath("sql/mysql/20260803_mes_process_pool_event_idempotency.sql"),
                StandardCharsets.UTF_8);
        assertTrue(idempotencySql.contains("event_idempotency_key"));
        assertTrue(idempotencySql.contains("recordbook_entry_id"));
        assertTrue(idempotencySql.contains("uk_mes_pro_process_pool_event_idem"));
        assertTrue(idempotencySql.contains("requires formal event_idempotency_key backfill"));

        String quantityFragmentRootSql = Files.readString(resolveBackendPath(
                        "sql/mysql/20260803_mes_process_pool_quantity_fragment_submit_root.sql"),
                StandardCharsets.UTF_8);
        assertTrue(quantityFragmentRootSql.contains("production_submit_event_id"));
        assertTrue(quantityFragmentRootSql.contains("idx_mes_pro_process_pool_fragment_submit_event"));
        assertTrue(quantityFragmentRootSql.contains("requires formal PRODUCTION_SUBMIT root event backfill"));

        String pqcAggregationSql = Files.readString(resolveBackendPath(
                "sql/mysql/20260803_mes_process_pool_pqc_process_inspection_aggregation.sql"),
                StandardCharsets.UTF_8);
        assertTrue(pqcAggregationSql.contains("dependsOn=20260803_mes_process_pool_pqc_event_source"));
        assertTrue(pqcAggregationSql.contains("`process_inspection_aggregation_status` varchar(32) NOT NULL DEFAULT ''PENDING''"));
        assertTrue(pqcAggregationSql.contains("`process_inspection_review_id` bigint DEFAULT NULL"));
        assertTrue(pqcAggregationSql.contains("`process_inspection_aggregated_at` datetime DEFAULT NULL"));
        assertTrue(pqcAggregationSql.contains("KEY `idx_mes_pp_pqc_process_inspection`"));

        String processInspectionDetailSql = Files.readString(resolveBackendPath(
                "sql/mysql/20260805_mes_pqc_process_inspection_aggregate_detail.sql"), StandardCharsets.UTF_8);
        assertTrue(processInspectionDetailSql.contains(
                "dependsOn=20260803_mes_process_pool_pqc_process_inspection_aggregation,20260803_mes_pqc_item_equipment_standard_snapshot"));
        assertTrue(processInspectionDetailSql.contains(
                "CREATE TABLE IF NOT EXISTS `mes_pqc_process_inspection_aggregate_detail`"));
        assertTrue(processInspectionDetailSql.contains("`source_piece_detail_id` bigint NOT NULL"));
        assertTrue(processInspectionDetailSql.contains("`regulation_version_id` bigint NOT NULL"));
        assertTrue(processInspectionDetailSql.contains("`round_no` int NOT NULL"));
        assertTrue(processInspectionDetailSql.contains("UNIQUE KEY `uk_mes_pqc_process_inspection_aggregate`"));
        assertTrue(processInspectionDetailSql.contains("KEY `idx_mes_pqc_process_inspection_submit_event`"));
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
