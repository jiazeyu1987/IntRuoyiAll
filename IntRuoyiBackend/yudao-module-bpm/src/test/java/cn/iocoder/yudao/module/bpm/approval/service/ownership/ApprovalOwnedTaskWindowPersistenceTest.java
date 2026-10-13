package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Loads the production count/window mapper; only its binary cast type is adapted for task-owned H2. */
class ApprovalOwnedTaskWindowPersistenceTest {
    @Test void formalPartitionAndCandidateUnionKeepSameCountAcross200And1000Boundaries() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:owned_window_"+UUID.randomUUID()
                +";MODE=MySQL;DATABASE_TO_UPPER=false","sa","");
        var jdbc=new JdbcTemplate(source);
        try(var fixtureConnection=source.getConnection()) {
        jdbc.execute("CREATE TABLE ACT_RU_TASK(ID_ VARCHAR(128),PROC_INST_ID_ VARCHAR(128),PROC_DEF_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),NAME_ VARCHAR(128),ASSIGNEE_ VARCHAR(32),SUSPENSION_STATE_ INT,CREATE_TIME_ TIMESTAMP)");
        jdbc.execute("CREATE TABLE ACT_HI_PROCINST(PROC_INST_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),BUSINESS_KEY_ VARCHAR(255))");
        jdbc.execute("CREATE TABLE ACT_RE_PROCDEF(ID_ VARCHAR(128),KEY_ VARCHAR(128),TENANT_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE bpm_approval_process_ownership(id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(128),process_definition_id VARCHAR(128),process_definition_key VARCHAR(128),business_key VARCHAR(255),projection_code VARCHAR(64),authority_type VARCHAR(64),authority_id VARCHAR(64),authority_version VARCHAR(64),created_at TIMESTAMP,writer_code VARCHAR(64),evidence_digest VARCHAR(64))");
        jdbc.execute("CREATE TABLE bpm_approval_selected_candidate(tenant_id BIGINT,process_instance_id VARCHAR(128),user_id BIGINT,selection_kind VARCHAR(64))");
        jdbc.execute("CREATE TABLE ACT_RU_IDENTITYLINK(TASK_ID_ VARCHAR(128),TYPE_ VARCHAR(32),USER_ID_ VARCHAR(32),GROUP_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE ACT_ID_MEMBERSHIP(USER_ID_ VARCHAR(32),GROUP_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_bpm_binding(tenant_id BIGINT,bpm_process_instance_id VARCHAR(128),request_id BIGINT,business_key VARCHAR(255),status VARCHAR(32),deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_access_request(tenant_id BIGINT,id BIGINT,bpm_process_instance_id VARCHAR(128),certificate_id BIGINT,status VARCHAR(32),request_type VARCHAR(64),request_key VARCHAR(128),deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate(tenant_id BIGINT,id BIGINT,current_version_id BIGINT,current_snapshot_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_version(tenant_id BIGINT,id BIGINT,certificate_id BIGINT,certificate_no VARCHAR(128),deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_snapshot(tenant_id BIGINT,id BIGINT,version_id BIGINT,product_name VARCHAR(128),deleted INT)");
        jdbc.update("INSERT INTO ACT_RE_PROCDEF VALUES('definition','native',122)");
        for(int i=0;i<1207;i++) {
            String id=String.format("task-%04d",i),process="process-"+i;
            jdbc.update("INSERT INTO ACT_RU_TASK VALUES(?,?, 'definition','122','needle','7',1,?)",id,process,LocalDateTime.of(2026,10,10,12,0).minusSeconds(i));
            jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES(?,'122',?)",process,"business-"+i);
            fact(jdbc,i+1,process,i==203 || i==1001);
        }
        // This exact selected user also has an engine candidate link: OR must count the task once.
        jdbc.update("UPDATE ACT_RU_TASK SET ASSIGNEE_=NULL WHERE ID_='task-0200'");
        jdbc.update("INSERT INTO ACT_RU_IDENTITYLINK VALUES('task-0200','candidate','7',NULL)");
        jdbc.update("INSERT INTO bpm_approval_selected_candidate VALUES(122,'process-200',7,'REGISTRATION_CERTIFICATE')");
        jdbc.update("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(122,'process-200',200,'business-200','RUNNING',0)");
        jdbc.update("INSERT INTO dcc_registration_certificate_access_request VALUES(122,200,'process-200',NULL,'BPM_BOUND','UPLOAD_CERTIFICATE','request-200',0)");
        // A registration-only task past the old 1000 boundary must remain eligible.
        jdbc.update("UPDATE ACT_RU_TASK SET ASSIGNEE_=NULL WHERE ID_='task-1005'");
        jdbc.update("INSERT INTO bpm_approval_selected_candidate VALUES(122,'process-1005',7,'REGISTRATION_CERTIFICATE')");
        jdbc.update("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(122,'process-1005',1005,'business-1005','RUNNING',0)");
        jdbc.update("INSERT INTO dcc_registration_certificate_access_request VALUES(122,1005,'process-1005',NULL,'BPM_BOUND','DOWNLOAD_FILE','request-1005',0)");
        var config=new Configuration();String resource="mapper/approval/ApprovalFlowableTaskQueryMapper.xml";
        try(var in=Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource))) {
            new XMLMapperBuilder(in,config,resource,config.getSqlFragments()).parse();
        }
        var p=new HashMap<String,Object>();p.put("tenant",122L);p.put("tenantText","122");p.put("userId",7L);p.put("userText","7");
        p.put("done",false);p.put("global",false);p.put("projection","BPM");p.put("candidateGroups",List.of());
        p.put("registrationUpload",true);p.put("registrationDownload",true);p.put("keyword","%needle%");p.put("limit",200L);
        long total=count(jdbc,config,p);assertEquals(1205L,total);
        var all=new ArrayList<String>();
        for(long offset=0;offset<total;offset+=200) {
            p.put("offset",offset);assertEquals(total,count(jdbc,config,p));all.addAll(window(jdbc,config,p));
        }
        assertEquals(total,all.size());assertEquals(total,new HashSet<>(all).size());
        assertEquals("task-0200",all.get(200));assertEquals("task-0204",all.get(203));
        assertEquals("task-1002",all.get(1000));assertFalse(all.contains("task-0203"));assertFalse(all.contains("task-1001"));
        // The same persisted selection predicate controls count and rows when permission is removed.
        p.put("registrationDownload",false);assertEquals(1204L,count(jdbc,config,p));
        p.put("offset",1000L);assertFalse(window(jdbc,config,p).contains("task-1005"));
        p.put("registrationDownload",true);assertEquals(total,count(jdbc,config,p));
        p.put("candidateGroups",null);assertEquals(total,count(jdbc,config,p));
        p.put("candidateGroups",List.of());
        p.put("offset",429496729200L);assertTrue(window(jdbc,config,p).isEmpty());assertEquals(total,count(jdbc,config,p));
        p.put("projection","DCC_TASK");p.put("offset",0L);assertEquals(2L,count(jdbc,config,p));
        assertEquals(List.of("task-0203","task-1001"),window(jdbc,config,p));
        p.put("projection","BPM");p.put("keyword","%absent%");assertEquals(0L,count(jdbc,config,p));assertTrue(window(jdbc,config,p).isEmpty());
        }
    }
    private static void fact(JdbcTemplate jdbc,long id,String process,boolean dcc) {
        var row=new BpmApprovalProcessOwnershipDO();row.setId(id);row.setTenantId(122L);row.setProcessInstanceId(process);
        row.setProcessDefinitionId("definition");row.setProcessDefinitionKey("native");row.setBusinessKey("business-"+(id-1));
        row.setProjectionCode(dcc?"DCC_TASK":"NATIVE_BPM");row.setAuthorityType(dcc?"CONTROLLED_FILE":"BPM_START");
        row.setAuthorityId(Long.toString(id));row.setAuthorityVersion("1");row.setWriterCode(dcc?"DCC_CONTROLLED_FILE_SUBMIT":"BPM_MANUAL_START");
        if(id==201 || id==1006) {
            long request=id-1;row.setProjectionCode("NATIVE_BUSINESS");row.setAuthorityType("REGISTRATION_REQUEST");
            row.setAuthorityId(Long.toString(request));row.setAuthorityVersion(cn.hutool.crypto.digest.DigestUtil.sha256Hex("request-"+request));
            row.setWriterCode("REGISTRATION_CERTIFICATE_SUBMIT");
        }
        row.setCreatedAt(LocalDateTime.of(2026,10,10,12,0));row.setEvidenceDigest(ApprovalProcessOwnershipService.digest(row));
        ApprovalProcessOwnershipService.verify(row);
        jdbc.update("INSERT INTO bpm_approval_process_ownership VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",row.getId(),row.getTenantId(),row.getProcessInstanceId(),row.getProcessDefinitionId(),row.getProcessDefinitionKey(),row.getBusinessKey(),row.getProjectionCode(),row.getAuthorityType(),row.getAuthorityId(),row.getAuthorityVersion(),row.getCreatedAt(),row.getWriterCode(),row.getEvidenceDigest());
    }
    private static Object[] args(org.apache.ibatis.mapping.BoundSql sql,Map<String,Object> p) {
        return sql.getParameterMappings().stream().map(m->sql.hasAdditionalParameter(m.getProperty())
                ?sql.getAdditionalParameter(m.getProperty()):p.get(m.getProperty())).toArray();
    }
    private static org.apache.ibatis.mapping.BoundSql bound(Configuration c,String id,Map<String,Object> p) {
        return c.getMappedStatement("cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper."+id).getBoundSql(p);
    }
    private static String h2ComparisonSql(org.apache.ibatis.mapping.BoundSql sql) {
        // Explicit task-only H2 mapping: bare BINARY defaults to one byte in H2.
        // VARBINARY preserves complete ID bytes; production SQL and assertions stay unchanged.
        return sql.getSql().replace(" AS BINARY)", " AS VARBINARY)");
    }
    private static long count(JdbcTemplate jdbc,Configuration c,Map<String,Object> p) {
        var sql=bound(c,"count",p);return Objects.requireNonNull(jdbc.queryForObject(h2ComparisonSql(sql),Long.class,args(sql,p)));
    }
    private static List<String> window(JdbcTemplate jdbc,Configuration c,Map<String,Object> p) {
        var sql=bound(c,"window",p);return jdbc.query(h2ComparisonSql(sql),(rs,index)->rs.getString(1),args(sql,p));
    }
}
