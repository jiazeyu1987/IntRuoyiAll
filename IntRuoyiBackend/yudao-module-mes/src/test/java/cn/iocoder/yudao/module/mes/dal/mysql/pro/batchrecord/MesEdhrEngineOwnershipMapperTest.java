package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the production annotated queries against an isolated in-memory schema. */
class MesEdhrEngineOwnershipMapperTest {
    @Test void exactQueriesExcludeOtherTenantsProcessesTasksAndDeletedRowsWithoutHidingDuplicates() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:edhr-owner-" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE mes_pro_batch_record_execution (id BIGINT, tenant_id BIGINT, revision_no INT, status INT, process_instance_id VARCHAR, process_definition_key VARCHAR, deleted INT)");
            statement.execute("CREATE TABLE mes_pro_batch_record_approval_snapshot (id BIGINT, tenant_id BIGINT, execution_id BIGINT, process_instance_id VARCHAR, process_definition_id VARCHAR, process_definition_key VARCHAR, current_bpm_task_id VARCHAR, current_task_definition_key VARCHAR, approval_status VARCHAR, deleted INT)");
            statement.execute("CREATE TABLE mes_pro_edhr_work_task (id BIGINT, tenant_id BIGINT, execution_id BIGINT, bpm_task_id VARCHAR, task_type VARCHAR, status VARCHAR, assignee_user_id BIGINT, candidate_user_snapshot VARCHAR, deleted INT)");
            statement.execute("INSERT INTO mes_pro_batch_record_execution VALUES (301,122,2,1,'p','k',0),(301,123,2,1,'other','k',0),(301,122,2,1,'p','k',1)");
            statement.execute("INSERT INTO mes_pro_batch_record_approval_snapshot VALUES (20,122,301,'p',NULL,'k','t','node','SUBMITTED',0),(21,122,301,'p',NULL,'k','t','node','SUBMITTED',0),(22,123,301,'other',NULL,'k','t','node','SUBMITTED',0),(23,122,301,'p',NULL,'k','t','node','SUBMITTED',1)");
            statement.execute("INSERT INTO mes_pro_edhr_work_task VALUES (401,122,301,'t','REVIEW','TODO',7,NULL,0),(402,122,301,'t','APPROVE','TODO',7,NULL,0),(403,123,301,'t','REVIEW','TODO',7,NULL,0),(404,122,302,'t','REVIEW','TODO',7,NULL,0),(405,122,301,'other','REVIEW','TODO',7,NULL,0),(406,122,301,'t','FILL','TODO',7,NULL,0),(407,122,301,'t','REVIEW','TODO',7,NULL,1)");
        }
        Configuration configuration = new Configuration(new Environment("edhr-owner", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(MesEdhrEngineOwnershipMapper.class);
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
            var mapper = session.getMapper(MesEdhrEngineOwnershipMapper.class);
            var execution = mapper.selectExecutions(122L, 301L);
            assertEquals(1, execution.size()); assertEquals(122L, execution.get(0).getTenantId());
            assertEquals(2, mapper.selectSnapshots(122L, 301L).size(), "duplicate facts must reach the policy");
            var tasks = mapper.selectWorkTasks(122L, 301L, "t");
            assertEquals(java.util.Set.of(401L, 402L), tasks.stream().map(MesEdhrEngineOwnershipMapper.WorkTaskFact::getId).collect(java.util.stream.Collectors.toSet()));
            assertTrue(tasks.stream().allMatch(task -> task.getTenantId().equals(122L) && task.getExecutionId().equals(301L)
                    && task.getBpmTaskId().equals("t") && task.getAssigneeUserId().equals(7L)));
            assertTrue(mapper.selectWorkTasks(122L, 301L, "absent").isEmpty());
        }
    }
}
