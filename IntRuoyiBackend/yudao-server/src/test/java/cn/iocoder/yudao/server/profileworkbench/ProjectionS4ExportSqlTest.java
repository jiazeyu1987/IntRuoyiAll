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
    @Test void workOrderFullExportPreservesSharedQueueEligibilityAndTenantProductLabels() throws Exception {
        String sql=sql("mes","MesProjectionExportMapper","exportWorkOrder");
        assertTrue(sql.contains("w.status = 1 AND w.type = 1 AND w.temporary_frozen = 0"));
        assertTrue(sql.contains("i.tenant_id = ? AND i.deleted = 0"));assertTrue(sql.contains("COLLATE utf8mb4_unicode_ci"));
    }
    @Test void dccFullExportsKeepFormalRecipientAndTrainingRoots() throws Exception {
        String distribution=sql("dcc","DccProjectionExportMapper","exportDistribution");
        String training=sql("dcc","DccProjectionExportMapper","exportTraining");
        assertTrue(distribution.contains("p.acknowledged_at IS NULL"));assertTrue(distribution.contains("distribution_medium = 'PUBLIC_FOLDER'"));
        assertTrue(distribution.contains("f.status IN ('ACTIVE','SUPERSEDED','OBSOLETE')"));
        for(String sql:new String[]{distribution,training}){assertTrue(sql.contains("p.user_id AS owner_user_id"));assertFalse(sql.contains("p.user_id = ?"));assertTrue(sql.contains("p.tenant_id = ? AND p.deleted = 0"));}
        assertTrue(training.contains("p.required_view_seconds ELSE 600"));assertTrue(training.contains("p.acknowledged_at IS NULL"));
    }
}
