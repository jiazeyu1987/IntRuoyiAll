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
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesActiveOrderTransferTraceSchemaTest {

    @Test
    void activeOrderTransferTraceSchemaMustBindTransfersShipmentsReturnsAndBatchStock() throws Exception {
        Class<?> traceClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO");
        assertEquals("mes_pro_process_pool_active_order_transfer_trace", tableName(traceClass));

        assertField(traceClass, "activeOrderId", Long.class);
        assertField(traceClass, "workOrderId", Long.class);
        assertField(traceClass, "routeId", Long.class);
        assertField(traceClass, "routeVersionId", Long.class);
        assertField(traceClass, "sourceType", String.class);
        assertField(traceClass, "direction", String.class);
        assertField(traceClass, "transferId", Long.class);
        assertField(traceClass, "transferLineId", Long.class);
        assertField(traceClass, "transferDetailId", Long.class);
        assertField(traceClass, "materialStockId", Long.class);
        assertField(traceClass, "batchId", Long.class);
        assertField(traceClass, "itemId", Long.class);
        assertField(traceClass, "quantity", BigDecimal.class);
        assertField(traceClass, "sourceObjectType", String.class);
        assertField(traceClass, "sourceObjectId", String.class);
        assertField(traceClass, "sourceObjectCode", String.class);
        assertField(traceClass, "sourceStatus", String.class);
        assertField(traceClass, "sourceOccurredAt", LocalDateTime.class);
        assertField(traceClass, "idempotencyKey", String.class);
        assertField(traceClass, "sourceSnapshotJson", String.class);

        Class.forName(
                "cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper");
        Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderTransferTraceService");

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260802_mes_process_pool_active_order_transfer_trace.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_transfer_trace`"));
        assertTrue(sql.contains("`active_order_id` bigint NOT NULL COMMENT '统一活跃订单ID'"));
        assertTrue(sql.contains("`source_type` varchar(32) NOT NULL COMMENT '来源类型：TRANSFER/SHIPMENT/REPLENISHMENT/RETURN/BATCH_TRACE'"));
        assertTrue(sql.contains("`material_stock_id` bigint DEFAULT NULL COMMENT '库存台账ID'"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_pp_active_order_transfer_trace`"));
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
