package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MesTeamEmployeeOwnershipConcurrencyTest {

    private String databaseUrl;

    @BeforeEach
    void createIsolatedSchema() throws SQLException {
        databaseUrl = "jdbc:h2:mem:employee_ownership_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000";
        try (Connection connection = connect()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("sql/edhr-formal-employee-ownership.sql"));
        }
    }

    @Test
    void concurrentNewAssociationsPermitOnlyOneEnabledOwner() throws Exception {
        assertOnlyOneCommit(
                "INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (1,1,101,7,'FORMAL',true,false)",
                "INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (2,1,102,7,'FORMAL',true,false)");
        assertEquals(1, enabledOwners());
    }

    @Test
    void concurrentReenablesPermitOnlyOneEnabledOwner() throws Exception {
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (1,1,101,7,'FORMAL',false,false)");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (2,1,102,7,'FORMAL',false,false)");
        assertOnlyOneCommit(
                "UPDATE mes_pro_process_pool_team_employee_profile SET enabled=true WHERE id=1",
                "UPDATE mes_pro_process_pool_team_employee_profile SET enabled=true WHERE id=2");
        assertEquals(1, enabledOwners());
    }

    @Test
    void disablingOldOwnerAllowsNewOwnerAndPreservesHistory() throws Exception {
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (1,1,101,7,'FORMAL',true,false)");
        execute("UPDATE mes_pro_process_pool_team_employee_profile SET enabled=false WHERE id=1");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (2,1,102,7,'FORMAL',true,false)");
        assertEquals(1, enabledOwners());
        try (Connection connection = connect(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_profile")) {
            assertTrue(rows.next());
            assertEquals(2, rows.getInt(1));
        }
    }

    @Test
    void deletedRowsOtherTenantsAndTemporaryEmployeesDoNotOccupyFormalOwner() throws Exception {
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (1,1,101,7,'SYSTEM',true,true)");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (2,1,102,7,'FORMAL',true,false)");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (3,2,103,7,'FORMAL',true,false)");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (4,1,104,NULL,'TEMPORARY',true,false)");
        execute("INSERT INTO mes_pro_process_pool_team_employee_profile (id,tenant_id,leader_user_id,system_user_id,employee_type,enabled,deleted) VALUES (5,1,105,NULL,'TEMPORARY',true,false)");
        assertEquals(1, enabledOwners());
    }

    private void assertOnlyOneCommit(String first, String second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var a = executor.submit(transaction(first, ready, start));
            var b = executor.submit(transaction(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int commits = (a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, commits, "同租户同员工的两个独立事务只能有一个启用成功");
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private Callable<Boolean> transaction(String sql, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            try (Connection connection = connect()) {
                connection.setAutoCommit(false);
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate(sql);
                    connection.commit();
                    return true;
                } catch (SQLException error) {
                    connection.rollback();
                    if (!"23505".equals(error.getSQLState())) {
                        throw error;
                    }
                    return false;
                }
            }
        };
    }

    private int enabledOwners() throws SQLException {
        try (Connection connection = connect(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM mes_pro_process_pool_team_employee_profile"
                     + " WHERE tenant_id=1 AND system_user_id=7 AND enabled=true AND deleted=false")) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl, "sa", "");
    }
}
