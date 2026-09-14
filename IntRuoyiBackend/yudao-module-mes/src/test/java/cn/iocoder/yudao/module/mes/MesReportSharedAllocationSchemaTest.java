package cn.iocoder.yudao.module.mes;

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

class MesReportSharedAllocationSchemaTest {

    private static final String MIGRATION =
            "sql/mysql/20260809_mes_process_pool_report_shared_allocation.sql";

    @Test
    void shouldDeclareVersionedCurrentAllocationAndAuditContracts() throws Exception {
        Path migrationPath = resolveBackendPath(MIGRATION);
        assertTrue(Files.exists(migrationPath), "shared allocation migration must exist");
        String sql = Files.readString(migrationPath, StandardCharsets.UTF_8);
        String normalizedSql = sql.replace("\r\n", "\n");

        assertTrue(normalizedSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260809_mes_process_pool_active_order_manual_sort; "
                + "type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation_state`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_report_alloc_state_event` "
                + "(`tenant_id`, `event_id`, `deleted`)"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation_adjustment_audit`"));
        assertTrue(sql.contains("`current_version` int NOT NULL"));
        assertTrue(sql.contains("`lifecycle_status` varchar(32) NOT NULL"));
        assertTrue(sql.contains("`created_version` int NOT NULL"));
        assertTrue(sql.contains("`superseded_version` int DEFAULT NULL"));
        assertTrue(sql.contains("`report_allocation_version` int NOT NULL"));
        assertTrue(sql.contains("`source_allocation_id` bigint"));
        assertTrue(sql.contains("`before_quantity` decimal(24,6) NOT NULL"));
        assertTrue(sql.contains("`after_quantity` decimal(24,6) NOT NULL"));
        assertTrue(sql.contains("`delta_quantity` decimal(24,6) NOT NULL"));
        assertTrue(sql.contains("`change_source` varchar(32) NOT NULL"));
        assertTrue(sql.contains("'INITIAL_BASELINE'"));
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(sql.split("NOT EXISTS", -1).length >= 3,
                "state and baseline backfills must both be idempotent");
        assertFalse(sql.contains("release_snapshot"));
        assertFalse(sql.contains("released_snapshot"));
        assertFalse(sql.contains("`release_status`"));
    }

    @Test
    void shouldExposeStateAllocationAuditAndFragmentLifecycleFields() throws Exception {
        Class<?> stateClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team."
                        + "MesProcessPoolReportAllocationStateDO");
        assertEquals("mes_pro_process_pool_report_allocation_state", tableName(stateClass));
        assertField(stateClass, "eventId", Long.class);
        assertField(stateClass, "currentVersion", Integer.class);
        assertField(stateClass, "lastIdempotencyKey", String.class);
        assertField(stateClass, "lastRequestHash", String.class);
        assertField(stateClass, "lastChangedBy", Long.class);
        assertField(stateClass, "lastChangedAt", LocalDateTime.class);

        Class<?> allocationClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team."
                        + "MesProcessPoolReportAllocationDO");
        assertField(allocationClass, "lifecycleStatus", String.class);
        assertField(allocationClass, "createdVersion", Integer.class);
        assertField(allocationClass, "supersededVersion", Integer.class);

        Class<?> auditClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team."
                        + "MesProcessPoolReportAllocationAdjustmentAuditDO");
        assertEquals("mes_pro_process_pool_report_allocation_adjustment_audit", tableName(auditClass));
        assertField(auditClass, "eventId", Long.class);
        assertField(auditClass, "allocationVersion", Integer.class);
        assertField(auditClass, "sourceAllocationId", Long.class);
        assertField(auditClass, "activeOrderId", Long.class);
        assertField(auditClass, "workOrderId", Long.class);
        assertField(auditClass, "routeProcessId", Long.class);
        assertField(auditClass, "processId", Long.class);
        assertField(auditClass, "beforeQuantity", BigDecimal.class);
        assertField(auditClass, "afterQuantity", BigDecimal.class);
        assertField(auditClass, "deltaQuantity", BigDecimal.class);
        assertField(auditClass, "actorUserId", Long.class);
        assertField(auditClass, "adjustmentReason", String.class);
        assertField(auditClass, "allocationMode", String.class);
        assertField(auditClass, "changeSource", String.class);
        assertField(auditClass, "occurredAt", LocalDateTime.class);

        Class<?> fragmentClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool."
                        + "MesProcessPoolFifoAllocationLineDO");
        assertField(fragmentClass, "reportAllocationVersion", Integer.class);
        assertField(fragmentClass, "lifecycleStatus", String.class);
        assertField(fragmentClass, "supersededVersion", Integer.class);
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
