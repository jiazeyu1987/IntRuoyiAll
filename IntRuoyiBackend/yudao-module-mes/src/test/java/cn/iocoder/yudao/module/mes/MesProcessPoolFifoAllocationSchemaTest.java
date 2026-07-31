package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolFifoAllocationSchemaTest {

    @Test
    void shouldUseProductionWorkOrderPlannedStartTimeForFifoOrderingContract() throws Exception {
        assertEquals(LocalDateTime.class, MesProWorkOrderDO.class.getDeclaredField("plannedStartTime").getType());

        String h2Schema = Files.readString(Path.of("src", "test", "resources", "sql", "create_tables.sql"),
                StandardCharsets.UTF_8);
        assertTrue(h2Schema.contains("\"mes_pro_work_order\""));
        assertTrue(h2Schema.contains("\"planned_start_time\" timestamp DEFAULT NULL"));
    }

    @Test
    void shouldDeclareAllocationLineContractWithoutScheduleTargets() throws Exception {
        Set<String> fields = Arrays.stream(MesProcessPoolFifoAllocationLineDO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        for (String requiredField : Set.of(
                "processPoolId",
                "sourceEventId",
                "sourceQuantityFragmentId",
                "sourceFragmentQuantity",
                "targetWorkOrderId",
                "targetWorkOrderCode",
                "targetRouteProcessId",
                "targetProcessId",
                "allocatedQuantity")) {
            assertTrue(fields.contains(requiredField), "allocation line must expose " + requiredField);
        }
        assertFalse(fields.contains("targetScheduleOrderId"));
        assertFalse(fields.contains("targetScheduleOrderProcessId"));

        String migration = Files.readString(Path.of("..", "sql", "mysql",
                "20260730_mes_process_pool_fifo_allocation.sql"), StandardCharsets.UTF_8);
        assertTrue(migration.contains("`mes_pro_process_pool_fifo_allocation_line`"));
        assertTrue(migration.contains("`target_work_order_id` bigint NOT NULL"));
        assertTrue(migration.contains("`target_route_process_id` bigint NOT NULL"));
        assertFalse(migration.contains("target_schedule_order_id"));
        assertFalse(migration.contains("target_schedule_order_process_id"));
    }
}
