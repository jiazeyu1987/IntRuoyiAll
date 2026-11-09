package cn.iocoder.yudao.module.bpm.approval.service;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalUnownedHistoryQueryTest {
    @Test void onlyDoneHistoryAllowsMissingOwnerAndStillChecksTenantActorAndEngineIntegrity() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:history_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false", "sa", "");
        try (var keepAlive = source.getConnection()) {
            var jdbc = new JdbcTemplate(source);
            String taskColumns = "ID_ VARCHAR(128),PROC_INST_ID_ VARCHAR(128),PROC_DEF_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),ASSIGNEE_ VARCHAR(32),TASK_DEF_KEY_ VARCHAR(128),END_TIME_ TIMESTAMP,SUSPENSION_STATE_ INT";
            jdbc.execute("CREATE TABLE ACT_HI_TASKINST(" + taskColumns + ")");
            jdbc.execute("CREATE TABLE ACT_RU_TASK(" + taskColumns + ")");
            jdbc.execute("CREATE TABLE ACT_HI_PROCINST(PROC_INST_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),BUSINESS_KEY_ VARCHAR(255))");
            jdbc.execute("CREATE TABLE ACT_RE_PROCDEF(ID_ VARCHAR(128),KEY_ VARCHAR(128),TENANT_ID_ VARCHAR(32))");
            jdbc.execute("CREATE TABLE bpm_approval_process_ownership(id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(128),process_definition_id VARCHAR(128),process_definition_key VARCHAR(128),business_key VARCHAR(255),projection_code VARCHAR(64),authority_type VARCHAR(64),authority_id VARCHAR(64),authority_version VARCHAR(64))");
            jdbc.execute("CREATE TABLE dcc_registration_certificate_bpm_binding(tenant_id BIGINT,bpm_process_instance_id VARCHAR(128),request_id BIGINT,deleted INT)");
            jdbc.execute("CREATE TABLE dcc_registration_certificate_access_request(tenant_id BIGINT,id BIGINT,bpm_process_instance_id VARCHAR(128),certificate_id BIGINT,deleted INT)");
            jdbc.execute("CREATE TABLE dcc_registration_certificate(tenant_id BIGINT,id BIGINT,current_version_id BIGINT,current_snapshot_id BIGINT,deleted INT)");
            jdbc.execute("CREATE TABLE dcc_registration_certificate_version(tenant_id BIGINT,id BIGINT,certificate_id BIGINT,deleted INT)");
            jdbc.execute("CREATE TABLE dcc_registration_certificate_snapshot(tenant_id BIGINT,id BIGINT,version_id BIGINT,deleted INT)");
            jdbc.execute("CREATE TABLE bpm_business_approval_request(tenant_id BIGINT,id BIGINT,process_instance_id VARCHAR(128),process_definition_key VARCHAR(128),object_version VARCHAR(64))");
            jdbc.execute("CREATE TABLE bpm_form_action_instance(tenant_id BIGINT,id BIGINT,bpm_process_instance_id VARCHAR(128),object_version VARCHAR(64))");
            jdbc.execute("CREATE TABLE dcc_controlled_file(tenant_id BIGINT,id BIGINT,process_instance_id VARCHAR(128),process_definition_key VARCHAR(128),version_no VARCHAR(64),status VARCHAR(32))");
            for (String table : List.of("bpm_oa_leave", "crm_contract", "crm_receivable"))
                jdbc.execute("CREATE TABLE " + table + "(tenant_id BIGINT,id BIGINT,process_instance_id VARCHAR(128))");
            jdbc.execute("INSERT INTO ACT_HI_TASKINST VALUES('task','process','definition','1','7','approval',CURRENT_TIMESTAMP,1)");
            jdbc.execute("INSERT INTO ACT_RU_TASK SELECT * FROM ACT_HI_TASKINST");
            jdbc.execute("INSERT INTO ACT_HI_PROCINST VALUES('process','1','original-file')");
            jdbc.execute("INSERT INTO ACT_RE_PROCDEF VALUES('definition','original-definition','1')");
            var configuration = new Configuration();
            String resource = "mapper/approval/ApprovalFlowableTaskQueryMapper.xml";
            try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource))) {
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
            var p = new HashMap<String,Object>();
            p.put("tenant", 1L); p.put("tenantText", "1"); p.put("userText", "7");
            p.put("done", true); p.put("global", false); p.put("projection", "BPM");
            p.put("startNode", "StartUserNode"); p.put("processIds", List.of("process"));
            assertEquals(0, number(jdbc, configuration, "countBrokenProvenance", p));
            assertEquals(1, number(jdbc, configuration, "count", p));
            assertEquals("process", rows(jdbc, configuration, "unownedHistoricProcesses", p).get(0).values().iterator().next());
            assertTrue(rows(jdbc, configuration, "ownerships", p).isEmpty());
            p.put("projection", "DCC_TASK");
            assertEquals(0, number(jdbc, configuration, "count", p), "Missing proof never invents a DCC owner");
            p.put("projection", "BPM"); p.put("userText", "8");
            assertEquals(0, number(jdbc, configuration, "count", p));
            p.put("userText", "7"); p.put("tenantText", "2");
            assertEquals(0, number(jdbc, configuration, "count", p));
            p.put("tenantText", "1"); p.put("global", true); p.put("done", false);
            assertEquals(1, number(jdbc, configuration, "countBrokenProvenance", p), "TODO still requires ownership");
            p.put("done", true);
            jdbc.execute("INSERT INTO bpm_approval_process_ownership VALUES(1,1,'process','wrong-definition','original-definition','original-file','NATIVE_BPM','BPM_START','process','1')");
            assertEquals(1, number(jdbc, configuration, "countBrokenProvenance", p), "Existing contradictory evidence still fails");
            jdbc.execute("DELETE FROM bpm_approval_process_ownership");
            jdbc.execute("DELETE FROM ACT_HI_PROCINST");
            assertEquals(1, number(jdbc, configuration, "countBrokenProvenance", p), "Missing history cannot be displayed");
        }
    }

    private static List<Map<String,Object>> rows(JdbcTemplate jdbc, Configuration c, String id, Map<String,Object> p) {
        var bound = c.getMappedStatement("cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper." + id).getBoundSql(p);
        Object[] args = bound.getParameterMappings().stream().map(m -> bound.hasAdditionalParameter(m.getProperty())
                ? bound.getAdditionalParameter(m.getProperty()) : p.get(m.getProperty())).toArray();
        return jdbc.queryForList(bound.getSql().replace(" AS BINARY)", " AS VARBINARY)")
                .replace(" AS CHAR)", " AS VARCHAR)").replace("<=>", "IS NOT DISTINCT FROM"), args);
    }

    private static long number(JdbcTemplate jdbc, Configuration c, String id, Map<String,Object> p) {
        return ((Number) rows(jdbc, c, id, p).get(0).values().iterator().next()).longValue();
    }
}
