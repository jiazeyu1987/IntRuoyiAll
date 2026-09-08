package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesRouteProductionProcessConfigSchemaTest {

    @Test
    void routeProcessLossReasonProjectionMustHaveStableVersionIdentity() throws Exception {
        Class<?> projection = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessLossReasonDO");
        assertEquals("mes_pro_route_process_loss_reason", tableName(projection));
        assertField(projection, "routeVersionId", Long.class);
        assertField(projection, "routeProcessId", Long.class);
        assertField(projection, "processId", Long.class);
        assertField(projection, "reasonCode", String.class);
        assertField(projection, "reasonName", String.class);
        assertField(projection, "enabled", Boolean.class);
        assertField(projection, "remark", String.class);
        assertField(projection, "sort", Integer.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260908_mes_route_production_process_config_snapshot.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_route_process_loss_reason`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_route_process_loss_reason` "
                + "(`tenant_id`, `route_version_id`, `route_process_id`, `reason_code`, `deleted`)"));
        assertTrue(sql.contains("KEY `idx_mes_route_process_loss_reason_process` "
                + "(`tenant_id`, `route_process_id`, `process_id`, `enabled`)"));
    }

    @Test
    void activeOrderProcessSnapshotMustPersistUnifiedProductionConfigEnvelope() throws Exception {
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "lossReasonSnapshotJson", String.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "lossReasonSnapshotSha256", String.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "overagePercentSnapshot", BigDecimal.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "productionConfigSnapshotJson", String.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "productionConfigSnapshotSha256", String.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "productionConfigMigrationSource", String.class);
        assertField(MesProcessPoolActiveOrderProcessSnapshotDO.class, "productionConfigMigratedAt", LocalDateTime.class);

        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260908_mes_route_production_process_config_snapshot.sql"), StandardCharsets.UTF_8);
        for (String column : List.of("loss_reason_snapshot_json", "loss_reason_snapshot_sha256",
                "overage_percent_snapshot", "production_config_snapshot_json",
                "production_config_snapshot_sha256", "production_config_migration_source",
                "production_config_migrated_at")) {
            assertTrue(sql.contains("`" + column + "`"), column + " must be in runtime migration");
        }
        assertTrue(sql.contains("CHECK (`production_config_migration_source` IN "
                + "('ROUTE_VERSION','CUTOVER_CURRENT_CONFIG'))"));
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
