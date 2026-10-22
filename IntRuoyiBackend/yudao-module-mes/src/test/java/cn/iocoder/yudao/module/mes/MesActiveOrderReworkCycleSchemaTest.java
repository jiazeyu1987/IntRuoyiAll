package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class MesActiveOrderReworkCycleSchemaTest {

    @Test
    void qaOnlyUpgradeCanKeepRouteVersionAndRetainPreviousOrder() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:qa-upgrade-cycle;MODE=MySQL")) {
            connection.createStatement().execute(Files.readString(
                    Path.of("src/test/resources/sql/edhr_rework_cycle.sql")));
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order "
                    + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status) "
                    + "VALUES (1, 1, 100, 200, 300, 'VERSION_UPGRADED')");
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order "
                    + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status) "
                    + "VALUES (2, 1, 100, 200, 300, 'ACTIVE')");
            assertThrows(SQLException.class, () -> connection.createStatement().execute(
                    "INSERT INTO mes_pro_process_pool_active_order "
                            + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status) "
                            + "VALUES (3, 1, 100, 200, 300, 'ACTIVE')"));
        }
    }

    @Test
    void currentCycleAndReviewUniquenessSupportTwoReworksWithoutDeletingHistory() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:rework-cycle;MODE=MySQL")) {
            String schema = Files.readString(Path.of("src/test/resources/sql/edhr_rework_cycle.sql"));
            connection.createStatement().execute(schema);
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order "
                    + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status) "
                    + "VALUES (1, 1, 100, 200, 300, 'COMPLETED')");
            assertThrows(SQLException.class, () -> connection.createStatement().execute(
                    "INSERT INTO mes_pro_process_pool_active_order "
                            + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status) "
                            + "VALUES (2, 1, 100, 200, 300, 'ACTIVE')"));
            for (long nextId = 2; nextId <= 3; nextId++) {
                connection.createStatement().execute("UPDATE mes_pro_process_pool_active_order "
                        + "SET business_status='REWORKED' WHERE id=" + (nextId - 1));
                connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order "
                        + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status, "
                        + "rework_source_active_order_id, rework_review_id) VALUES (" + nextId
                        + ", 1, 100, 200, 300, 'ACTIVE', " + (nextId - 1) + ", " + (900 + nextId) + ")");
            }
            assertThrows(SQLException.class, () -> connection.createStatement().execute(
                    "INSERT INTO mes_pro_process_pool_active_order "
                            + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status, rework_review_id) "
                            + "VALUES (4, 1, 101, 200, 300, 'ACTIVE', 903)"));
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order "
                    + "(id, tenant_id, work_order_id, route_id, route_version_id, business_status, rework_review_id) "
                    + "VALUES (5, 2, 100, 200, 300, 'ACTIVE', 903)");
            try (var rows = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id=1")) {
                assertTrue(rows.next());
                assertEquals(3, rows.getInt(1));
            }
        }
    }

    @Test
    void reworkIdentityAndCurrentUniquenessAreExplicitAndHistoryIsPreserved() throws Exception {
        assertDoesNotThrow(() -> MesProcessPoolActiveOrderDO.class.getDeclaredField("reworkSourceActiveOrderId"));
        assertDoesNotThrow(() -> MesProcessPoolActiveOrderDO.class.getDeclaredField("reworkReviewId"));
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("sql/mysql"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        String sql = Files.readString(root.resolve("sql/mysql/20260928_mes_active_order_rework_cycle.sql"));
        assertTrue(sql.contains("GENERATED ALWAYS AS"));
        assertTrue(sql.contains("'REWORKED'"));
        assertTrue(sql.contains("uk_mes_pp_active_order"));
        assertTrue(sql.contains("uk_mes_pp_rework_review"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("information_schema.statistics"));
        assertFalse(sql.matches("(?is).*\\bDELETE\\s+FROM\\s+`?mes_pro_process_pool_active_order.*"));
        assertFalse(sql.matches("(?is).*\\bUPDATE\\s+`?mes_pro_process_pool_active_order\\b.*"));
    }
}
