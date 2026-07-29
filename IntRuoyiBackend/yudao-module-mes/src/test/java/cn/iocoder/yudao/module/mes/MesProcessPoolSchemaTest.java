package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
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
        assertNotEquals("mes_pro_feedback_surplus_pool", tableName(MesProProcessPoolDO.class));

        assertField(MesProProcessPoolEventDO.class, "poolId", Long.class);
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
        assertField(MesProProcessPoolEventDO.class, "recordbookSourceType", String.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookSourceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "rawPayload", String.class);
        assertField(MesProProcessPoolEventDO.class, "serverSubmitTime", java.time.LocalDateTime.class);
        assertField(MesProProcessPoolEventDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "signatureUserId", Long.class);

        assertField(MesProProcessPoolQuantityFragmentDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "sourceQuantityType", String.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "totalQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "allocatedQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "availableQuantity", java.math.BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "allocationStatus", String.class);

        assertField(MesProProcessPoolPqcRecordDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "inspectionResult", String.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "actualEmployeeId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "serverSubmitTime", java.time.LocalDateTime.class);

        String sql = Files.readString(resolveBackendPath("sql/mysql/20260730_mes_process_pool_foundation.sql"),
                StandardCharsets.UTF_8);
        assertTrue(sql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260729_dcc_product_catalog_remove_subsidiary_source; type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_quantity_fragment`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_pqc_record`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pro_process_pool_event_signature` (`tenant_id`, `signature_id`, `deleted`)"));
        assertTrue(sql.contains("KEY `idx_mes_pro_process_pool_event_time` (`tenant_id`, `server_submit_time`)"));
        assertFalse(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool`"),
                "F1 must not reuse the feedback surplus pool as the process pool table");
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
