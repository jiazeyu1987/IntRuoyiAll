package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesFixedTestOrderDownstreamCleanupMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Execute the real annotated dynamic SQL against isolated H2 data; never touches a project DB. */
class MesFixedTestOrderDownstreamScopeSqlTest {
    @Test
    void ownershipExistenceProbeMustNotHideForeignTenantParents() throws Exception {
        var method = MesFixedTestOrderDownstreamCleanupMapper.class.getMethod("countOwnershipConflicts",
                Long.class, Long.class, List.class, List.class, List.class, List.class);
        var ignore = method.getAnnotation(com.baomidou.mybatisplus.annotation.InterceptorIgnore.class);
        org.junit.jupiter.api.Assertions.assertNotNull(ignore,
                "Tenant rewriting would hide foreign parents and falsely classify them as absent");
        assertEquals("true", ignore.tenantLine());
    }

    @Test
    void ncrOwnershipConflictsCoverWorkOrderActiveOrderAndBatchBothDirections() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order VALUES "
                    + "(999,1,72)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES "
                    + "(1,1,72,81,NULL),(2,1,72,NULL,301),(3,1,71,999,NULL),"
                    + "(4,1,71,NULL,999),(5,1,NULL,81,999),(6,1,71,81,301),"
                    + "(7,2,72,81,301),(8,1,72,999,999)");
            assertEquals(5L, conflicts(connection, List.of(301L), List.of(), List.of()));
        }
    }

    @Test
    void bpmQueryDistinguishesExecutionReviewFromBatchVoidEvenWhenIdsOverlap() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("CREATE TABLE mes_pro_batch_record_execution (id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(80))");
            connection.createStatement().execute("CREATE TABLE mes_pro_edhr_record_change_event (batch_execution_id BIGINT,tenant_id BIGINT,bpm_process_instance_id VARCHAR(80))");
            connection.createStatement().execute("CREATE TABLE bpm_business_approval_request (tenant_id BIGINT,data_domain VARCHAR(30),system_code VARCHAR(30),object_type VARCHAR(80),object_id VARCHAR(30),action_code VARCHAR(40),process_instance_id VARCHAR(80))");
            connection.createStatement().execute("CREATE TABLE bpm_form_action_instance (tenant_id BIGINT,data_domain VARCHAR(30),system_code VARCHAR(30),object_type VARCHAR(80),object_id VARCHAR(30),action_code VARCHAR(40),bpm_process_instance_id VARCHAR(80))");
            connection.createStatement().execute("INSERT INTO mes_pro_batch_record_execution VALUES (401,1,'execution'),(301,1,'wrong-execution'),(401,2,'wrong-tenant')");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_record_change_event VALUES (301,1,'change'),(401,1,'wrong-change')");
            for (String table : List.of("bpm_business_approval_request", "bpm_form_action_instance")) {
                connection.createStatement().execute("INSERT INTO " + table + " VALUES "
                        + "(1,'MES','MES','EDHR_BATCH_EXECUTION','401','SUBMIT_REVIEW','review'),"
                        + "(1,'MES','MES','EDHR_BATCH_EXECUTION','401x','SUBMIT_REVIEW','wrong-numeric-coercion'),"
                        + "(1,'MES','MES','EDHR_BATCH_EXECUTION','301','VOID','void'),"
                        + "(1,'MES','MES','EDHR_BATCH_EXECUTION','301','SUBMIT_REVIEW','wrong-review'),"
                        + "(1,'MES','MES','EDHR_BATCH_EXECUTION','401','VOID','wrong-void'),"
                        + "(1,'MES','OTHER','EDHR_BATCH_EXECUTION','301','VOID','wrong-system'),"
                        + "(2,'MES','MES','EDHR_BATCH_EXECUTION','301','VOID','wrong-tenant')");
            }
            var method = MesFixedTestOrderDownstreamCleanupMapper.class.getMethod("selectProcessInstanceIds", Long.class, List.class, List.class);
            Map<String, Object> params = Map.of("tenantId", 1L, "batchIds", List.of(301L), "executionIds", List.of(401L));
            BoundSql sql = new XMLLanguageDriver().createSqlSource(new Configuration(),
                    String.join(" ", method.getAnnotation(Select.class).value()), Map.class).getBoundSql(params);
            try (var statement = connection.prepareStatement(sql.getSql())) {
                bind(statement, sql, params);
                java.util.Set<String> ids = new java.util.HashSet<>();
                try (var rows = statement.executeQuery()) { while (rows.next()) ids.add(rows.getString(1)); }
                assertEquals(java.util.Set.of("execution", "change", "review", "void"), ids);
            }
        }
    }
    @Test
    void bothDirectionsOfWorkTaskAndDeviationOwnershipConflictAreRejected() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES "
                    + "(999,1,72)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_work_task VALUES "
                    + "(1,1,72,301,NULL,NULL,NULL),(2,1,71,999,NULL,NULL,NULL),"
                    + "(3,1,NULL,NULL,401,NULL,NULL),(4,1,71,NULL,999,NULL,NULL),"
                    + "(5,1,72,NULL,NULL,'RELEASE_APPLICATION',601),"
                    + "(6,1,71,NULL,NULL,'RELEASE_APPLICATION',999),"
                    + "(7,2,72,301,NULL,NULL,NULL),(8,1,72,999,NULL,NULL,NULL),"
                    + "(9,1,71,301,401,'RELEASE_APPLICATION',601)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_deviation VALUES "
                    + "(1,1,72,301),(2,1,71,999),(3,2,72,301),(4,1,NULL,301),(5,1,71,301)");
            assertEquals(7L, conflicts(connection, List.of(301L), List.of(401L), List.of(601L)));
        }
    }

    @Test
    void emptyScopeRejectsForeignReferencesAndAllowsTrulyBatchlessWorkOrderTasks() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_work_task VALUES "
                    + "(1,1,71,NULL,NULL,NULL,NULL),(2,1,71,301,NULL,NULL,NULL),"
                    + "(3,1,71,NULL,401,NULL,NULL),(4,1,71,NULL,NULL,'RELEASE_APPLICATION',601)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_deviation VALUES (1,1,71,301),(2,1,71,NULL)");
            assertEquals(3L, conflicts(connection, List.of(), List.of(), List.of()));
        }
    }

    @Test
    void orphanParentIsAllowedOnlyForExplicitWorkOrderOwnership() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_work_task VALUES "
                    + "(10,1,71,NULL,NULL,'RELEASE_APPLICATION',900),"
                    + "(11,1,71,NULL,NULL,'RELEASE_APPLICATION',901),"
                    + "(12,1,71,NULL,NULL,'RELEASE_APPLICATION',902),"
                    + "(13,1,71,NULL,NULL,'RELEASE_APPLICATION',903),"
                    + "(14,1,NULL,301,NULL,'RELEASE_APPLICATION',904)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES "
                    + "(10,1,71,910,NULL),(11,1,71,911,NULL),(12,1,71,912,NULL),"
                    + "(13,1,71,913,NULL),(14,1,NULL,914,301)");
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES "
                    + "(901,1,72),(902,2,71),(903,1,71)");
            connection.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order VALUES "
                    + "(911,1,72),(912,2,71),(913,1,71)");

            assertEquals(8L, conflicts(connection, List.of(301L), List.of(), List.of(163L)));
        }
    }

    @Test
    void missingWorkOrderOwnershipWithOrphanParentIsRejected() throws Exception {
        try (Connection connection = database()) {
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_work_task VALUES "
                    + "(20,1,NULL,301,NULL,'RELEASE_APPLICATION',920)");
            connection.createStatement().execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES "
                    + "(20,1,NULL,921,301)");

            assertEquals(2L, conflicts(connection, List.of(301L), List.of(), List.of(163L)));
        }
    }

    private Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:scope_" + UUID.randomUUID() + ";MODE=MySQL");
        connection.createStatement().execute("CREATE TABLE mes_pro_edhr_work_task (id BIGINT, tenant_id BIGINT,"
                + "work_order_id BIGINT, batch_execution_id BIGINT, execution_id BIGINT, business_scope_type VARCHAR(80), business_scope_id BIGINT)");
        connection.createStatement().execute("CREATE TABLE mes_pro_edhr_deviation (id BIGINT, tenant_id BIGINT, work_order_id BIGINT, batch_execution_id BIGINT)");
        connection.createStatement().execute("CREATE TABLE mes_pro_edhr_nonconformance_review (id BIGINT, tenant_id BIGINT, work_order_id BIGINT, active_order_id BIGINT, batch_execution_id BIGINT)");
        connection.createStatement().execute("CREATE TABLE mes_pro_process_pool_active_order_release_application (id BIGINT, tenant_id BIGINT, work_order_id BIGINT)");
        connection.createStatement().execute("CREATE TABLE mes_pro_process_pool_active_order (id BIGINT, tenant_id BIGINT, work_order_id BIGINT)");
        return connection;
    }

    private long conflicts(Connection connection, List<Long> batches, List<Long> executions, List<Long> applications) throws Exception {
        var method = MesFixedTestOrderDownstreamCleanupMapper.class.getMethod("countOwnershipConflicts",
                Long.class, Long.class, List.class, List.class, List.class, List.class);
        Map<String, Object> params = Map.of("tenantId", 1L, "workOrderId", 71L, "batchIds", batches,
                "executionIds", executions, "applicationIds", applications, "activeOrderIds", List.of(81L));
        BoundSql sql = new XMLLanguageDriver().createSqlSource(new Configuration(),
                String.join(" ", method.getAnnotation(Select.class).value()), Map.class).getBoundSql(params);
        try (var statement = connection.prepareStatement(sql.getSql())) {
            bind(statement, sql, params);
            try (var rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private void bind(java.sql.PreparedStatement statement, BoundSql sql, Map<String, Object> params) throws Exception {
        int index = 1;
        for (var mapping : sql.getParameterMappings()) {
            String name = mapping.getProperty();
            statement.setObject(index++, sql.hasAdditionalParameter(name) ? sql.getAdditionalParameter(name) : params.get(name));
        }
    }
}
