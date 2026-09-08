package cn.iocoder.yudao.module.mes;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesRouteProductionProcessConfigSchemaTest {

    @Test
    void routeProcessLossReasonProjectionMustHaveStableVersionIdentity() throws Exception {
        Class<?> doClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessLossReasonDO");

        TableName tableName = doClass.getAnnotation(TableName.class);
        assertEquals("mes_pro_route_process_loss_reason", tableName.value());

        Set<String> fieldNames = Arrays.stream(doClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertTrue(fieldNames.containsAll(Set.of(
                "routeVersionId", "routeProcessId", "processId", "reasonCode", "reasonName",
                "enabled", "remark", "sort")));

        String sql = Files.readString(Path.of("..", "sql", "mysql",
                "20260908_mes_route_production_process_config_snapshot.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_route_process_loss_reason`"));
        assertTrue(sql.contains("`route_version_id` bigint NOT NULL"));
        assertTrue(sql.contains("`route_process_id` bigint NOT NULL"));
        assertTrue(sql.contains("`process_id` bigint NOT NULL"));
        assertTrue(sql.contains("UNIQUE KEY `uk_mes_route_process_loss_reason`"));
        assertTrue(sql.contains("KEY `idx_mes_route_process_loss_reason_process`"));
    }

    @Test
    void activeOrderProcessSnapshotMustPersistUnifiedProductionConfigEnvelope() throws Exception {
        Class<?> doClass = Class.forName(
                "cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO");
        Set<String> fieldNames = Arrays.stream(doClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertTrue(fieldNames.containsAll(Set.of(
                "lossReasonSnapshotJson", "lossReasonSnapshotSha256", "overagePercentSnapshot",
                "productionConfigSnapshotJson", "productionConfigSnapshotSha256",
                "productionConfigMigrationSource", "productionConfigMigratedAt")));

        String sql = Files.readString(Path.of("..", "sql", "mysql",
                "20260908_mes_route_production_process_config_snapshot.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("'loss_reason_snapshot_json'"));
        assertTrue(sql.contains("longtext DEFAULT NULL COMMENT ''冻结损耗原因快照''"));
        assertTrue(sql.contains("'loss_reason_snapshot_sha256'"));
        assertTrue(sql.contains("char(64) DEFAULT NULL COMMENT ''冻结损耗原因快照哈希''"));
        assertTrue(sql.contains("`overage_percent_snapshot`"));
        assertTrue(sql.contains("'production_config_snapshot_json'"));
        assertTrue(sql.contains("longtext DEFAULT NULL COMMENT ''冻结统一生产工序配置快照''"));
        assertTrue(sql.contains("'production_config_snapshot_sha256'"));
        assertTrue(sql.contains("char(64) DEFAULT NULL COMMENT ''冻结统一生产工序配置快照哈希''"));
        assertTrue(sql.contains("'production_config_migration_source'"));
        assertTrue(sql.contains("varchar(32) DEFAULT NULL COMMENT ''生产工序配置迁移来源：ROUTE_VERSION/CUTOVER_CURRENT_CONFIG''"));
        assertTrue(sql.contains("'production_config_migrated_at'"));
        assertTrue(sql.contains("datetime DEFAULT NULL COMMENT ''生产工序配置迁移时间''"));
        assertTrue(sql.contains("CHECK (`production_config_migration_source` IN ('ROUTE_VERSION','CUTOVER_CURRENT_CONFIG'))"));
    }
}
