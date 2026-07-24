package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProRouteFlowConfigMapperTest extends BaseDbUnitTest {

    private static final Long ROUTE_ID = 922119L;
    private static final Long TENANT_ID = 1L;
    private static final String USE_TYPE_BATCH = "BATCH";

    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private DataSource dataSource;

    @Test
    void deleteByRouteIdAndUseType_shouldPhysicallyDeleteTopLevelConfigWhenDeletedHistoryExists() {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_test_route_flow_config_deleted
                ON mes_pro_route_flow_config(tenant_id, route_id, use_type, deleted)
                """);
        insertRouteFlowConfig(jdbcTemplate, 1L, true);
        insertRouteFlowConfig(jdbcTemplate, 2L, false);

        routeFlowConfigMapper.deleteByRouteIdAndUseType(ROUTE_ID, USE_TYPE_BATCH);

        assertEquals(0, countRows("mes_pro_route_flow_config"));
    }

    @Test
    void deleteByRouteIdAndUseType_shouldPhysicallyDeleteProcessConfigWhenDeletedHistoryExists() {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_test_route_flow_process_config_deleted
                ON mes_pro_route_flow_process_config(tenant_id, route_process_id, use_type, deleted)
                """);
        insertRouteFlowProcessConfig(jdbcTemplate, 11L, true);
        insertRouteFlowProcessConfig(jdbcTemplate, 12L, false);

        routeFlowProcessConfigMapper.deleteByRouteIdAndUseType(ROUTE_ID, USE_TYPE_BATCH);

        assertEquals(0, countRows("mes_pro_route_flow_process_config"));
    }

    @Test
    void deleteByRouteIdAndUseType_shouldPhysicallyDeleteProcessBatchRecordWhenDeletedHistoryExists() {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_test_route_flow_process_batch_record_deleted
                ON mes_pro_route_flow_process_batch_record(
                    tenant_id, route_flow_process_config_id, batch_record_report_id, form_slot_type, record_category, deleted
                )
                """);
        insertRouteFlowProcessBatchRecord(jdbcTemplate, 21L, true);
        insertRouteFlowProcessBatchRecord(jdbcTemplate, 22L, false);

        routeFlowProcessBatchRecordMapper.deleteByRouteIdAndUseType(ROUTE_ID, USE_TYPE_BATCH);

        assertEquals(0, countRows("mes_pro_route_flow_process_batch_record"));
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private void insertRouteFlowConfig(JdbcTemplate jdbcTemplate, Long id, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO mes_pro_route_flow_config
                    (id, route_id, use_type, enabled, deleted, tenant_id)
                VALUES (?, ?, ?, TRUE, ?, ?)
                """, id, ROUTE_ID, USE_TYPE_BATCH, deleted, TENANT_ID);
    }

    private void insertRouteFlowProcessConfig(JdbcTemplate jdbcTemplate, Long id, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO mes_pro_route_flow_process_config
                    (id, route_flow_config_id, route_id, route_process_id, use_type, enabled, execution_mode,
                     production_quantity_factor, deleted, tenant_id)
                VALUES (?, 1001, ?, 9001, ?, TRUE, 'SERIAL', 1.000000, ?, ?)
                """, id, ROUTE_ID, USE_TYPE_BATCH, deleted, TENANT_ID);
    }

    private void insertRouteFlowProcessBatchRecord(JdbcTemplate jdbcTemplate, Long id, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                    (id, route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                     form_slot_type, record_category, report_sort, deleted, tenant_id)
                VALUES (?, 2001, ?, 9001, ?, 'REPORT-1', 'MAIN', 'BATCH_RECORD', 1, ?, ?)
                """, id, ROUTE_ID, USE_TYPE_BATCH, deleted, TENANT_ID);
    }

    private int countRows(String tableName) {
        return jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE route_id = ? AND tenant_id = ?",
                Integer.class,
                ROUTE_ID,
                TENANT_ID
        );
    }
}
