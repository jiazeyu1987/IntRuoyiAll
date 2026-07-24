package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRouteProcessSingleEntryMigrationContractTest {

    @Test
    void migration_shouldEnforceSingleIncomingAndPersistDependencySnapshots() throws Exception {
        Path migration = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve(Path.of("sql", "mysql",
                        "20260710_mes_route_process_single_entry_multi_exit.sql"));
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertTrue(sql.contains("uk_mes_route_process_flow_target"));
        assertTrue(sql.contains("target_route_process_id`, `deleted"));
        assertTrue(sql.contains("route process flow contains multiple incoming edges"));
        assertTrue(sql.contains("mes_pro_schedule_order_process"));
        assertTrue(sql.contains("mes_pro_edhr_batch_execution_task"));
        assertTrue(sql.contains("predecessor_route_process_id"));
        assertTrue(sql.contains("root_process_flag"));
    }
}
