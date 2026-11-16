package cn.iocoder.yudao.server.profileworkbench;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises production SQL against an isolated fixture, never against the business database. */
class EdhrVoidWorkbenchQueryPersistenceTest {
    private JdbcTemplate jdbc;
    private Configuration configuration;
    private Connection fixture;
    private Map<String,Object> parameters;
    private static final String APPROVAL="cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper.";
    private static final String VOID="cn.iocoder.yudao.module.bpm.workbench.EdhrVoidWorkbenchMapper.";

    @BeforeEach void prepare() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:void_query_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_UPPER=false","sa","");
        fixture=source.getConnection();jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE ACT_RU_TASK(ID_ VARCHAR(128),PROC_INST_ID_ VARCHAR(128),PROC_DEF_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),NAME_ VARCHAR(128),ASSIGNEE_ VARCHAR(32),SUSPENSION_STATE_ INT,CREATE_TIME_ TIMESTAMP,DUE_DATE_ TIMESTAMP)");
        jdbc.execute("CREATE TABLE ACT_HI_PROCINST(PROC_INST_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),BUSINESS_KEY_ VARCHAR(255))");
        jdbc.execute("CREATE TABLE ACT_RE_PROCDEF(ID_ VARCHAR(128),KEY_ VARCHAR(128),TENANT_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE bpm_approval_process_ownership(id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(128),process_definition_id VARCHAR(128),process_definition_key VARCHAR(128),business_key VARCHAR(255),projection_code VARCHAR(64),authority_type VARCHAR(64),authority_id VARCHAR(64),authority_version VARCHAR(64),writer_code VARCHAR(64))");
        jdbc.execute("CREATE TABLE ACT_RU_IDENTITYLINK(TASK_ID_ VARCHAR(128),TYPE_ VARCHAR(32),USER_ID_ VARCHAR(32),GROUP_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE ACT_ID_MEMBERSHIP(USER_ID_ VARCHAR(32),GROUP_ID_ VARCHAR(32))");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_bpm_binding(tenant_id BIGINT,bpm_process_instance_id VARCHAR(128),deleted INT)");
        jdbc.execute("CREATE TABLE bpm_form_action_instance(tenant_id BIGINT,bpm_process_instance_id VARCHAR(128))");
        jdbc.execute("CREATE TABLE bpm_business_approval_request(id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(128),process_definition_key VARCHAR(128),object_version VARCHAR(64),object_id VARCHAR(64),deleted INT,request_status VARCHAR(32),system_code VARCHAR(32),data_domain VARCHAR(32),object_type VARCHAR(64),action_code VARCHAR(32))");
        jdbc.execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE mes_pro_edhr_record_change_event(batch_execution_id BIGINT,tenant_id BIGINT,deleted INT,bpm_process_instance_id VARCHAR(128),change_type VARCHAR(32),target_scope VARCHAR(32),change_status VARCHAR(32))");
        jdbc.execute("CREATE TABLE ACT_RU_VARIABLE(PROC_INST_ID_ VARCHAR(128),NAME_ VARCHAR(64),TEXT_ VARCHAR(128),TASK_ID_ VARCHAR(128))");
        jdbc.update("INSERT INTO ACT_RU_TASK VALUES('task','process','definition','168','void needle','7',1,CURRENT_TIMESTAMP,NULL)");
        jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES('process','168','BUSINESS_APPROVAL:11')");
        jdbc.update("INSERT INTO ACT_RE_PROCDEF VALUES('definition','mes-edhr-batch-execution-void-v1','168')");
        jdbc.update("INSERT INTO bpm_approval_process_ownership VALUES(1,168,'process','definition','mes-edhr-batch-execution-void-v1','BUSINESS_APPROVAL:11','NATIVE_BUSINESS','BUSINESS_REQUEST','11','version-1','BUSINESS_APPROVAL_SUBMIT')");
        jdbc.update("INSERT INTO bpm_business_approval_request VALUES(11,168,'process','mes-edhr-batch-execution-void-v1','version-1','21',0,'PENDING_BPM','MES','MES','EDHR_BATCH_EXECUTION','VOID')");
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution VALUES(21,168,0)");
        jdbc.update("INSERT INTO mes_pro_edhr_record_change_event VALUES(21,168,0,'process','VOID','BATCH','SUBMITTED')");
        jdbc.update("INSERT INTO ACT_RU_VARIABLE VALUES('process','businessType','EDHR_BATCH_EXECUTION_VOID',NULL)");
        parameters=new HashMap<>();parameters.put("tenant",168L);parameters.put("tenantText","168");parameters.put("userId",7L);parameters.put("userText","7");
        parameters.put("done",false);parameters.put("global",false);parameters.put("edhrVoidOnly",true);parameters.put("candidateGroups",List.of());
        parameters.put("projection","BPM");parameters.put("keyword",null);parameters.put("offset",0);parameters.put("limit",10);
        configuration=new Configuration();
        Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(root!=null&&!Files.isDirectory(root.resolve("yudao-module-bpm")))root=root.getParent();
        assertNotNull(root);
        for(String resource:List.of("yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml",
                "yudao-module-bpm/src/main/resources/mapper/approval/ApprovalFlowableTaskQueryMapper.xml",
                "yudao-module-bpm/src/main/resources/mapper/workbench/EdhrVoidWorkbenchMapper.xml")) {
            Path file=root.resolve(resource);
            try(var in=Files.newInputStream(file)){new XMLMapperBuilder(in,configuration,file.toString(),configuration.getSqlFragments()).parse();}
        }
    }
    @AfterEach void close() throws Exception {if(fixture!=null)fixture.close();}
    private long count(String statement) {
        BoundSql bound=configuration.getMappedStatement(statement).getBoundSql(parameters);
        // H2's bare BINARY truncates to one byte. The explicit fixture mapping preserves full ID bytes;
        // its standard null-safe equality is equivalent to MySQL <=>. Production SQL is not changed.
        String sql=bound.getSql().replace(" AS BINARY)"," AS VARBINARY)")
                .replace(" AS CHAR)"," AS VARCHAR)").replace("<=>","IS NOT DISTINCT FROM");
        Object[] args=bound.getParameterMappings().stream().map(m->bound.hasAdditionalParameter(m.getProperty())
                ?bound.getAdditionalParameter(m.getProperty()):SystemMetaObject.forObject(parameters).getValue(m.getProperty())).toArray();
        return Objects.requireNonNull(jdbc.queryForObject(sql,Long.class,args));
    }
    @Test void validFormalAssignedTaskRemainsVisibleWithoutCertificateMasterJoins() {
        assertEquals(0,count(APPROVAL+"countBrokenProvenance"));
        assertEquals(0,count(VOID+"countBrokenDomain"));
        assertEquals(1,count(APPROVAL+"count"));
        parameters.put("keyword","%needle%");assertEquals(1,count(APPROVAL+"count"));
        parameters.put("keyword","%missing%");assertEquals(0,count(APPROVAL+"count"));
    }
    @Test void candidateResponsibilityAndExactTenantAreStillMandatory() {
        parameters.put("userText","8");parameters.put("userId",8L);assertEquals(0,count(APPROVAL+"count"));
        jdbc.update("UPDATE ACT_RU_TASK SET ASSIGNEE_=NULL");
        jdbc.update("INSERT INTO ACT_RU_IDENTITYLINK VALUES('task','participant','8',NULL)");assertEquals(0,count(APPROVAL+"count"));
        jdbc.update("UPDATE ACT_RU_IDENTITYLINK SET TYPE_='candidate'");assertEquals(1,count(APPROVAL+"count"));
        parameters.put("tenant",169L);parameters.put("tenantText","169");assertEquals(0,count(APPROVAL+"count"));
    }
    @Test void missingOrMismatchedOwnershipIsRejected() {
        jdbc.update("UPDATE bpm_approval_process_ownership SET process_instance_id='PROCESS'");
        assertEquals(1,count(APPROVAL+"countBrokenProvenance"));
    }
    @Test void wrongAuthorityCannotBeAcceptedByTheSpecializedVoidQuery() {
        jdbc.update("UPDATE bpm_approval_process_ownership SET authority_type='FORM_ACTION'");
        assertEquals(1,count(APPROVAL+"countBrokenProvenance"));assertEquals(1,count(VOID+"countBrokenDomain"));
    }
    @Test void businessVersionAndBusinessKeyTamperingAreRejected() {
        jdbc.update("UPDATE bpm_business_approval_request SET object_version='changed'");
        assertEquals(1,count(APPROVAL+"countBrokenProvenance"));
        jdbc.update("UPDATE bpm_business_approval_request SET object_version='version-1'");
        jdbc.update("UPDATE ACT_HI_PROCINST SET BUSINESS_KEY_='BUSINESS_APPROVAL:12'");
        assertEquals(1,count(APPROVAL+"countBrokenProvenance"));
    }
    @Test void ambiguousCrossBusinessBindingsRemainRejected() {
        jdbc.update("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(168,'process',0),(168,'process',0)");
        assertEquals(1,count(APPROVAL+"countBrokenProvenance"));
    }
    @Test void withdrawnOrForeignDomainCannotBeExposedAsPendingVoidWork() {
        jdbc.update("UPDATE mes_pro_edhr_record_change_event SET change_status='WITHDRAWN'");
        assertEquals(1,count(VOID+"countBrokenDomain"));
        jdbc.update("UPDATE mes_pro_edhr_record_change_event SET change_status='SUBMITTED',tenant_id=169");
        assertEquals(1,count(VOID+"countBrokenDomain"));
    }
}
