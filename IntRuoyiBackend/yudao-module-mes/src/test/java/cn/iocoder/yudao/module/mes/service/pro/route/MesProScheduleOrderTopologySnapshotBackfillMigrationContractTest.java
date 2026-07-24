package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderTopologySnapshotBackfillMigrationContractTest {

    @Test
    void migration_shouldBackfillOnlyLegacyLinearScheduleOrderSnapshots() throws Exception {
        Path migration = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve(Path.of("sql", "mysql",
                        "20260710_mes_schedule_order_topology_snapshot_backfill.sql"));
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertTrue(sql.contains("dependsOn=20260710_mes_route_process_single_entry_multi_exit"));
        assertTrue(sql.contains("LAG(snapshot.route_process_id) OVER"));
        assertTrue(sql.contains("PARTITION BY snapshot.tenant_id, snapshot.schedule_order_id"));
        assertTrue(sql.contains("predecessor_route_process_id IS NOT NULL"));
        assertTrue(sql.contains("root_process_flag = b'1'"));
        assertTrue(sql.contains("legacy schedule order topology snapshot contains null route process"));
        assertTrue(sql.contains("IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')"));
    }
}
