package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchTaskTopologySnapshotBackfillMigrationContractTest {

    @Test
    void migration_shouldBackfillLegacyEdhrTasksByDistinctRouteProcess() throws Exception {
        Path migration = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve(Path.of("sql", "mysql",
                        "20260710_mes_edhr_batch_task_topology_snapshot_backfill.sql"));
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertTrue(sql.contains("dependsOn=20260710_mes_schedule_order_topology_snapshot_backfill"));
        assertTrue(sql.contains("mes_pro_edhr_batch_execution_task"));
        assertTrue(sql.contains("node_type = 'ROUTE_FORM'"));
        assertTrue(sql.contains("GROUP BY task.tenant_id, task.batch_execution_id, task.route_process_id"));
        assertTrue(sql.contains("LAG(process_snapshot.route_process_id) OVER"));
        assertTrue(sql.contains("PARTITION BY process_snapshot.tenant_id, process_snapshot.batch_execution_id"));
        assertTrue(sql.contains("legacy eDHR topology snapshot is partially populated"));
        assertTrue(sql.contains("legacy eDHR topology snapshot has ambiguous process ordering"));
        assertTrue(sql.contains("IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')"));
        assertTrue(sql.contains("eDHR topology snapshot backfill verification failed"));
    }
}
