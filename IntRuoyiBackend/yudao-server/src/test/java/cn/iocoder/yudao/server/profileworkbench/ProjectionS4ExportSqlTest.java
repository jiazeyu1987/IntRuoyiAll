package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Parses current production XML and evaluates actual fixed OGNL bindings, without a connection. */
class ProjectionS4ExportSqlTest {
    private Path backend() {
        Path path=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(path!=null){if(Files.isDirectory(path.resolve("IntRuoyiBackend")))return path.resolve("IntRuoyiBackend");if(path.getFileName().toString().equals("IntRuoyiBackend"))return path;path=path.getParent();}
        throw new IllegalStateException("Current repository production XML missing");
    }
    private String sql(String module,String mapper,String method) throws Exception {
        Path path=backend().resolve("yudao-module-"+module+"/src/main/resources/mapper/profileworkbench/"+mapper+".xml");
        Configuration configuration=new Configuration();
        try(var input=Files.newInputStream(path)){new XMLMapperBuilder(input,configuration,path.toString(),configuration.getSqlFragments()).parse();}
        var statement=configuration.getMappedStatement("cn.iocoder.yudao.module."+module+".workbench."+mapper+"."+method);
        var bound=statement.getBoundSql(Map.of("q",ProjectionExportSupport.query(2)));
        assertNotNull(statement.getTimeout());assertEquals(10,statement.getTimeout());
        assertTrue(bound.getParameterMappings().stream().allMatch(p->p.getProperty().equals("q.tenantId")));
        String sql=bound.getSql().replaceAll("\\s+"," ");assertFalse(sql.contains("${"));
        assertFalse(sql.contains("system_profile_workbench_task_visibility"));assertFalse(sql.contains("LIMIT"));return sql;
    }
    @Test void edhrFullExportRetainsParentStateGatesAndAllExactAudienceFacts() throws Exception {
        String sql=sql("mes","MesProjectionExportMapper","exportEdhr");
        assertTrue(sql.contains("status IN ('TODO','OVERDUE')"));assertTrue(sql.contains("mes_pro_edhr_batch_execution"));
        assertTrue(sql.contains("candidate_user_snapshot"));assertTrue(sql.contains("assignee_user_id AS owner_user_id"));
        assertFalse(sql.contains("FIND_IN_SET"));assertFalse(sql.contains("assignee_user_id = ?"));
    }
    @Test void dccFullExportsKeepFormalRecipientAndTrainingRoots() throws Exception {
        String distribution=sql("dcc","DccProjectionExportMapper","exportDistribution");
        String training=sql("dcc","DccProjectionExportMapper","exportTraining");
        assertTrue(distribution.contains("p.acknowledged_at IS NULL"));assertTrue(distribution.contains("distribution_medium = 'PUBLIC_FOLDER'"));
        assertTrue(distribution.contains("f.status IN ('ACTIVE','SUPERSEDED','OBSOLETE')"));
        for(String sql:new String[]{distribution,training}){assertTrue(sql.contains("p.user_id AS owner_user_id"));assertFalse(sql.contains("p.user_id = ?"));assertTrue(sql.contains("p.tenant_id = ? AND p.deleted = 0"));}
        assertTrue(training.contains("p.required_view_seconds ELSE 600"));assertTrue(training.contains("p.acknowledged_at IS NULL"));
    }

    @Test void handoffExportRetainsCandidateAudienceAndExpiresOnCompletionOrCycleClosure() throws Exception {
        String sql=sql("mes","MesProjectionExportMapper","exportHandoff");
        assertTrue(sql.contains("h.candidate_user_snapshot"));assertFalse(sql.contains("FIND_IN_SET"));
        assertTrue(sql.contains("h.status='TODO' AND h.notification_only=0"));
        assertTrue(sql.contains("h.task_type IN ('QA_DECISION_HANDOFF','QA_REVIEW')"));assertTrue(sql.contains("ao.active_status='ACTIVE'"));
        assertTrue(sql.contains("ao.tenant_id=h.tenant_id"));assertTrue(sql.contains("CONCAT('EDHR_WORK_TASK:handoff:',h.id)"));
    }

    @Test void handoffMembershipAppearsAndCompletionVoidOrClosedCycleDisappears() throws Exception {
        String export=sql("mes","MesProjectionExportMapper","exportHandoff");
        // Exercise the production membership predicates, not MySQL-only display/collation expressions.
        // The full projection is checked against MySQL through the deployed workbench page.
        int from=export.indexOf(" FROM mes_active_order_handoff_task h ");
        assertTrue(from>0);
        assertTrue(export.contains("USING utf8mb4) COLLATE utf8mb4_unicode_ci"));
        String sql="SELECT CONCAT('handoff:',h.id) AS business_id, h.candidate_user_snapshot"+export.substring(from);
        try(var db=java.sql.DriverManager.getConnection("jdbc:h2:mem:handoff_workbench;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            try(var st=db.createStatement()) {
                st.execute("CREATE TABLE mes_active_order_handoff_task(id BIGINT,tenant_id BIGINT,deleted INT,task_type VARCHAR(40),status VARCHAR(20),notification_only INT,active_order_id BIGINT,reason VARCHAR(100),create_time TIMESTAMP,action_url VARCHAR(100),candidate_user_snapshot VARCHAR(100))");
                st.execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,deleted INT,active_status VARCHAR(20))");
                st.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(90,2,0,'ACTIVE')");
                st.execute("INSERT INTO mes_active_order_handoff_task VALUES(77,2,0,'QA_DECISION_HANDOFF','TODO',0,90,'rework：处理',CURRENT_TIMESTAMP,'/formal','3,4')");
                assertEquals(1,handoffRows(db,sql));
                st.execute("UPDATE mes_active_order_handoff_task SET status='DONE'");assertEquals(0,handoffRows(db,sql));
                st.execute("UPDATE mes_active_order_handoff_task SET status='TODO',notification_only=1");assertEquals(0,handoffRows(db,sql));
                st.execute("UPDATE mes_active_order_handoff_task SET notification_only=0");
                st.execute("UPDATE mes_pro_process_pool_active_order SET active_status='CLOSED'");assertEquals(0,handoffRows(db,sql));
            }
        }
    }
    private int handoffRows(java.sql.Connection db,String sql) throws Exception {
        try(var query=db.prepareStatement(sql)) {query.setLong(1,2);try(var rows=query.executeQuery()) {int count=0;while(rows.next()){assertEquals("handoff:77",rows.getString("business_id"));assertEquals("3,4",rows.getString("candidate_user_snapshot"));count++;}return count;}}
    }
}
