package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit MySQL integration regression: every fixture table is TEMPORARY on one physical connection. */
class ApprovalRegistrationMaterialMysqlIT {
    @Test void newPendingUploadSearchesItsFrozenNumberAndProductWithLiteralLike() throws Exception {
        try(var f=new Fixture()) {
            f.request(1,1,"UPLOAD_CERTIFICATE",upload("新证_100%!","冻结产品_100%!"),false);
            f.assertRows(false,"新证_100%!","task-1");
            f.assertRows(false,"冻结产品_100%!","task-1");
            f.assertRows(false,"新证X100Z!");
            f.assertRows(false,"冻结产品X100Z!");
            f.assertRows(false,"current-only-number");
            f.assertRows(false,"current-only-product");
        }
    }

    @Test void doneAndPendingKeepEachRequestMaterialAfterCurrentVersionChanges() throws Exception {
        try(var f=new Fixture()) {
            f.request(1,1,"UPLOAD_CERTIFICATE",upload("original-number","original-product"),true);
            f.request(2,1,"UPLOAD_CERTIFICATE",upload("second-number","second-product"),false);
            f.exec("UPDATE dcc_registration_certificate SET current_version_id=900,current_snapshot_id=901 WHERE tenant_id=1 AND id=77");
            f.exec("UPDATE dcc_registration_certificate_version SET certificate_no='later-number' WHERE id=900");
            f.exec("UPDATE dcc_registration_certificate_snapshot SET product_name='later-product' WHERE id=901");
            f.assertRows(true,"original-number","task-1");
            f.assertRows(true,"original-product","task-1");
            f.assertRows(true,"second-number");
            f.assertRows(true,"later-number");
            f.assertRows(true,"later-product");
            f.assertRows(false,"second-number","task-2");
            f.assertRows(false,"second-product","task-2");
            f.assertRows(false,"original-number");
            f.assertRows(false,"later-product");
        }
    }

    @Test void keywordNeverGrantsCandidateAccessOrCrossesTenantAndProjection() throws Exception {
        try(var f=new Fixture()) {
            f.request(1,1,"UPLOAD_CERTIFICATE",upload("private-number","private-product"),false);
            f.request(2,2,"UPLOAD_CERTIFICATE",upload("foreign-number","foreign-product"),false);
            f.assertRows(false,"private-number","task-1");
            f.p.put("userId",8L);f.p.put("userText","8");
            f.assertRows(false,"private-number");
            f.p.put("userId",7L);f.p.put("userText","7");
            f.p.put("registrationUpload",false);f.assertRows(false,"private-number");
            f.p.put("registrationUpload",true);f.assertRows(false,"foreign-number");
            f.p.put("global",true);f.assertRows(false,"foreign-product");
            f.exec("UPDATE bpm_approval_process_ownership SET projection_code='DCC_TASK' WHERE id=1");
            f.assertRows(false,"private-number");
            f.exec("UPDATE bpm_approval_process_ownership SET projection_code='NATIVE_BUSINESS' WHERE id=1");
            f.p.put("global",false);
            f.exec("UPDATE dcc_registration_certificate_access_request SET tenant_id=2 WHERE id=1");
            f.assertRows(false,"private-number");
        }
    }

    @Test void onlyKnownUploadAndDownloadDetailContractsAreSearchable() throws Exception {
        try(var f=new Fixture()) {
            f.request(1,1,"UPLOAD_CERTIFICATE",Map.of("operation","OTHER","certificateNo","unverified-number","productName","unverified-product"),false);
            f.request(2,1,"DOWNLOAD_FILE",Map.of("requestType","DOWNLOAD_FILE","certificateNo","download-number","productName","unsupported-product"),false);
            f.request(3,1,"DOWNLOAD_FILE",Map.of("operation","UPLOAD_CERTIFICATE","certificateNo","wrong-contract"),true);
            var foreign=new HashMap<String,Object>(upload("unused","foreign-product"));foreign.put("certificateNo",null);
            f.request(4,1,"UPLOAD_CERTIFICATE",foreign,false);
            f.request(5,1,"UPLOAD_CERTIFICATE",Map.of("operation","UPLOAD_CERTIFICATE","certificateNo",Map.of("bad","object-number"),"productName",List.of("array-product")),false);
            f.assertRows(false,"unverified-number");f.assertRows(false,"unverified-product");
            f.assertRows(false,"null");f.assertRows(false,"foreign-product","task-4");
            f.assertRows(false,"object-number");f.assertRows(false,"array-product");
            f.assertRows(false,"download-number","task-2");
            f.assertRows(false,"unsupported-product");f.assertRows(true,"wrong-contract");
            f.exec("UPDATE ACT_RU_TASK SET ASSIGNEE_='7' WHERE ID_='task-2'");
            f.exec("INSERT INTO ACT_HI_TASKINST SELECT ID_,PROC_INST_ID_,PROC_DEF_ID_,TENANT_ID_,NAME_,ASSIGNEE_,SUSPENSION_STATE_,CREATE_TIME_,'approval',CURRENT_TIMESTAMP FROM ACT_RU_TASK WHERE ID_='task-2'");
            f.assertRows(true,"download-number","task-2");
            f.assertRows(true,"current-only-number");
        }
    }

    private static Map<String,Object> upload(String number,String product) {
        return Map.of("operation","UPLOAD_CERTIFICATE","certificateNo",number,"productName",product);
    }

    private static class Fixture implements AutoCloseable {
        final Connection connection;
        final SqlSession session;
        final ApprovalFlowableTaskQueryMapper mapper;
        final Map<String,Object> p=new HashMap<>();
        Fixture() throws Exception {
            String config=System.getProperty("approval.mysql.localConfig");
            assertNotNull(config,"Explicit approval.mysql.localConfig required; no skipped or simulated MySQL run");
            String text=Files.readString(Path.of(config));
            String master=text.substring(text.indexOf("        master:"),text.indexOf("        slave:"));
            connection=DriverManager.getConnection(field(master,"url"),field(master,"username"),field(master,"password"));
            assertTrue(connection.getMetaData().getDatabaseProductVersion().startsWith("8."),"Actual MySQL 8 required");
            // All DDL must succeed before fixture DML. The session can only use this exact connection.
            temp("ACT_RU_TASK","ID_ VARCHAR(128),PROC_INST_ID_ VARCHAR(128),PROC_DEF_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),NAME_ VARCHAR(128),ASSIGNEE_ VARCHAR(32),SUSPENSION_STATE_ INT,CREATE_TIME_ DATETIME(3)");
            temp("ACT_HI_TASKINST","ID_ VARCHAR(128),PROC_INST_ID_ VARCHAR(128),PROC_DEF_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),NAME_ VARCHAR(128),ASSIGNEE_ VARCHAR(32),SUSPENSION_STATE_ INT,CREATE_TIME_ DATETIME(3),TASK_DEF_KEY_ VARCHAR(128),END_TIME_ DATETIME(3)");
            temp("ACT_HI_PROCINST","PROC_INST_ID_ VARCHAR(128),TENANT_ID_ VARCHAR(32),BUSINESS_KEY_ VARCHAR(255)");
            temp("ACT_RE_PROCDEF","ID_ VARCHAR(128),KEY_ VARCHAR(128),TENANT_ID_ VARCHAR(32)");
            temp("bpm_approval_process_ownership","id BIGINT,tenant_id BIGINT,process_instance_id VARCHAR(128) COLLATE utf8mb4_bin,projection_code VARCHAR(64),authority_type VARCHAR(64),authority_id VARCHAR(64) COLLATE utf8mb4_bin");
            temp("bpm_approval_selected_candidate","tenant_id BIGINT,process_instance_id VARCHAR(128),user_id BIGINT,selection_kind VARCHAR(64)");
            temp("ACT_RU_IDENTITYLINK","TASK_ID_ VARCHAR(128),TYPE_ VARCHAR(32),USER_ID_ VARCHAR(32),GROUP_ID_ VARCHAR(32)");
            temp("dcc_registration_certificate_bpm_binding","tenant_id BIGINT,bpm_process_instance_id VARCHAR(128),request_id BIGINT,business_key VARCHAR(255),status VARCHAR(32),deleted INT");
            temp("dcc_registration_certificate_access_request","tenant_id BIGINT,id BIGINT,bpm_process_instance_id VARCHAR(128),certificate_id BIGINT,status VARCHAR(32),request_type VARCHAR(64),request_key VARCHAR(128),deleted INT,detail_json JSON");
            temp("dcc_controlled_file","tenant_id BIGINT,id BIGINT,title VARCHAR(128),file_number VARCHAR(128),status VARCHAR(32)");
            temp("dcc_registration_certificate","tenant_id BIGINT,id BIGINT,current_version_id BIGINT,current_snapshot_id BIGINT,deleted INT");
            temp("dcc_registration_certificate_version","tenant_id BIGINT,id BIGINT,certificate_id BIGINT,certificate_no VARCHAR(128),deleted INT");
            temp("dcc_registration_certificate_snapshot","tenant_id BIGINT,id BIGINT,version_id BIGINT,product_name VARCHAR(128),deleted INT");
            var c=new Configuration(new Environment("isolated-temp-tables",new JdbcTransactionFactory(),new SingleConnectionDataSource(connection,true)));
            String resource="mapper/approval/ApprovalFlowableTaskQueryMapper.xml";
            try(var in=Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource))) {
                new XMLMapperBuilder(in,c,resource,c.getSqlFragments()).parse();
            }
            session=new SqlSessionFactoryBuilder().build(c).openSession(connection);
            assertSame(connection,session.getConnection());
            mapper=session.getMapper(ApprovalFlowableTaskQueryMapper.class);
            p.put("tenant",1L);p.put("tenantText","1");p.put("userId",7L);p.put("userText","7");
            p.put("global",false);p.put("projection","BPM");p.put("candidateGroups",List.of());
            p.put("registrationUpload",true);p.put("registrationDownload",true);p.put("startNode","StartUserNode");
            p.put("limit",20L);p.put("offset",0L);
            exec("INSERT INTO dcc_registration_certificate VALUES(1,77,NULL,NULL,0)");
            exec("INSERT INTO dcc_registration_certificate_version VALUES(1,900,77,'current-only-number',0)");
            exec("INSERT INTO dcc_registration_certificate_snapshot VALUES(1,901,900,'current-only-product',0)");
        }
        void temp(String table,String columns) throws SQLException {
            // Match the actual Flowable, ownership identity, and registration table collations.
            String charset=table.startsWith("ACT_")?"utf8mb3 COLLATE utf8mb3_bin"
                    :table.startsWith("dcc_registration_")?"utf8mb4 COLLATE utf8mb4_unicode_ci"
                    :"utf8mb4 COLLATE utf8mb4_0900_ai_ci";
            try(var statement=connection.createStatement()) {statement.execute("CREATE TEMPORARY TABLE "+table+" ("+columns+") DEFAULT CHARACTER SET "+charset);}
        }
        void exec(String sql,Object...args) throws SQLException {
            try(var statement=connection.prepareStatement(sql)) {
                for(int i=0;i<args.length;i++)statement.setObject(i+1,args[i]);
                statement.executeUpdate();
            }
        }
        void request(long id,long tenant,String type,Map<String,Object> detail,boolean done) throws SQLException {
            String process="process-"+id,business="business-"+id,definition="definition-"+id,task="task-"+id;
            exec("INSERT INTO ACT_RE_PROCDEF VALUES(?,'dcc-registration-certificate-access',?)",definition,""+tenant);
            exec("INSERT INTO ACT_HI_PROCINST VALUES(?,?,?)",process,""+tenant,business);
            exec("INSERT INTO bpm_approval_process_ownership VALUES(?,?,?,'NATIVE_BUSINESS','REGISTRATION_REQUEST',?)",id,tenant,process,""+id);
            exec("INSERT INTO bpm_approval_selected_candidate VALUES(?,?,7,'REGISTRATION_CERTIFICATE')",tenant,process);
            exec("INSERT INTO dcc_registration_certificate_bpm_binding VALUES(?,?,?,?,'RUNNING',0)",tenant,process,id,business);
            exec("INSERT INTO dcc_registration_certificate_access_request VALUES(?,?,?,77,?,?,?,0,?)",tenant,id,process,done?"APPROVED":"BPM_BOUND",type,"request-"+id,JsonUtils.toJsonString(detail));
            if(done)exec("INSERT INTO ACT_HI_TASKINST VALUES(?,?,?,?,'registration','7',1,CURRENT_TIMESTAMP,'approval',CURRENT_TIMESTAMP)",task,process,definition,""+tenant);
            else exec("INSERT INTO ACT_RU_TASK VALUES(?,?,?,?,'registration',NULL,1,CURRENT_TIMESTAMP)",task,process,definition,""+tenant);
        }
        void assertRows(boolean done,String keyword,String...expected) {
            session.clearCache();p.put("done",done);p.put("keyword",ApprovalFlowableTaskQueryService.literalKeyword(keyword));
            assertEquals(expected.length,mapper.count(p),"count: "+keyword);
            assertEquals(List.of(expected),mapper.window(p),"window: "+keyword);
        }
        public void close() throws Exception {session.close();connection.close();}
        static String field(String text,String name) {
            var matcher=Pattern.compile("(?m)^\\s+"+name+":\\s*([^#\\r\\n]+)").matcher(text);
            if(!matcher.find())throw new IllegalStateException("Missing datasource "+name);
            return matcher.group(1).trim().replaceAll("^['\"]|['\"]$","");
        }
    }
}
