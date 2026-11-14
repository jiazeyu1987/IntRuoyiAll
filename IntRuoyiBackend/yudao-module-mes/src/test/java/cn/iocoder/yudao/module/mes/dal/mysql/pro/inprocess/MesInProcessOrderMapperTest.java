package cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess;

import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.MesInProcessOrderPageReqVO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesInProcessOrderMapperTest {
    @Test void currentTenantOngoingCyclesAndCombinedFilters() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:inprocess;MODE=MySQL", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,work_order_id BIGINT,leader_user_id BIGINT,active_status VARCHAR,business_status VARCHAR,deleted INT,joined_at TIMESTAMP)");
            statement.execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,product_id BIGINT,code VARCHAR,name VARCHAR,batch_code VARCHAR,deleted INT)");
            statement.execute("CREATE TABLE mes_md_item(id BIGINT,tenant_id BIGINT,name VARCHAR,code VARCHAR,deleted INT)");
            statement.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,nickname VARCHAR,deleted INT)");
            statement.execute("CREATE TABLE mes_pro_edhr_nonconformance_review(tenant_id BIGINT,active_order_id BIGINT,work_order_id BIGINT,review_status VARCHAR,disposition VARCHAR,deleted INT)");
            statement.execute("INSERT INTO mes_md_item VALUES(1,1,'导管','P1',0),(2,2,'导管','P2',0)");
            statement.execute("INSERT INTO mes_pro_work_order VALUES(1,1,1,'WO-1','一号工单','LOT-A',0),(2,1,1,'WO-2','二号工单','LOT-B',0),(3,2,2,'WO-3','其他租户','LOT-A',0)");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(1,1,1,10,'ACTIVE','ACTIVE',0,'2026-10-27 10:00:00'),(2,1,2,20,'ACTIVE','ACTIVE',0,'2026-10-27 11:00:00'),(3,1,1,10,'ACTIVE','COMPLETED',0,'2026-10-27 12:00:00'),(4,1,1,10,'REMOVED','VERSION_UPGRADED',0,'2026-10-27 12:00:00'),(5,2,3,30,'ACTIVE','ACTIVE',0,'2026-10-27 12:00:00'),(6,1,1,10,'ACTIVE','ACTIVE',1,'2026-10-27 12:00:00')");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(7,1,1,10,'ACTIVE','VERSION_UPGRADE_PENDING',0,'2026-10-27 09:00:00')");
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(8,1,1,10,'CLOSED','RELEASED',0,'2026-10-27 13:00:00')");
            var query = new MesInProcessOrderPageReqVO();
            assertEquals(List.of(3L,2L,1L,7L), query(connection, query));
            query.setProductName(" 导管 "); query.setBatchCode("LOT-A");
            assertEquals(List.of(3L,1L,7L), query(connection, query));
            query.setWorkOrderCode("WO-2"); assertTrue(query(connection, query).isEmpty());
            query.setWorkOrderCode(null); query.setBatchCode(null); query.setProductName("二号");
            assertEquals(List.of(2L), query(connection, query));
            query.setProductName("' OR 1=1 --"); assertTrue(query(connection, query).isEmpty());
            query.setProductName(null);
            // A pending review or a concession remains in process; only the same cycle's formal void ends it.
            statement.execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,1,'pending_review',NULL,0),(1,2,2,'closed','concession_release',0)");
            statement.execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(2,3,1,'closed','void',0),(1,3,1,'closed','void',1),(1,7,99,'closed','void',0)");
            assertEquals(List.of(3L,2L,1L,7L), query(connection, query));
            statement.execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,1,'closed','void',0)");
            assertEquals(List.of(3L,2L,7L), query(connection, query));
            query.setListFilters("[{\"key\":\"productCode\",\"operator\":\"eq\",\"value\":\"P1\"},{\"key\":\"joinedAt\",\"operator\":\"gte\",\"value\":\"2026-10-27\"}]");
            assertEquals(List.of(3L,2L,7L), query(connection, query));
            assertEquals(List.of(2L), query(connection, query, null,null,1L,1));
            assertEquals(List.of(2L,7L), query(connection, query, java.time.LocalDateTime.of(2026,10,27,12,0),3L,0L,100));
            query.setListFilters(null);
            statement.execute("INSERT INTO mes_pro_process_pool_active_order VALUES(9,1,2,20,'ACTIVE','ACTIVE',0,NULL),(10,1,2,20,'ACTIVE','ACTIVE',0,NULL)");
            assertEquals(List.of(2L,7L,10L,9L), query(connection, query, java.time.LocalDateTime.of(2026,10,27,12,0),3L,0L,100));
            assertEquals(List.of(9L), query(connection, query, null,10L,0L,100));

        }
    }
    @Test void responsibilityQueriesStayWithinTheCurrentStageApplicationAndTenant() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:inprocessOwners;MODE=MySQL", "sa", "")) {
            var st = connection.createStatement();
            st.execute("CREATE TABLE mes_active_order_handoff_task(tenant_id BIGINT,deleted INT,active_order_id BIGINT,status VARCHAR,notification_only INT,task_type VARCHAR,candidate_user_snapshot VARCHAR)");
            st.execute("CREATE TABLE mes_pro_edhr_work_task(id BIGINT,tenant_id BIGINT,deleted INT,work_order_id BIGINT,status VARCHAR,assignee_user_id BIGINT,candidate_user_snapshot VARCHAR,business_scope_type VARCHAR,business_scope_id BIGINT,review_source_id BIGINT)");
            st.execute("CREATE TABLE mes_pro_process_pool_active_order_release_application(id BIGINT,tenant_id BIGINT,deleted INT,active_order_id BIGINT,work_order_id BIGINT,pqc_release_work_task_id BIGINT,release_approval_work_task_id BIGINT)");
            st.execute("INSERT INTO mes_active_order_handoff_task VALUES(1,0,1,'TODO',0,'PRODUCTION_REVIEW','10'),(2,0,1,'TODO',0,'PRODUCTION_REVIEW','99'),(1,0,1,'DONE',0,'PQC_REVIEW','98'),(1,0,2,'TODO',0,'PQC_REVIEW','97')");
            st.execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES(90,1,0,1,1,100,200),(80,1,0,1,1,101,201)");
            st.execute("INSERT INTO mes_pro_edhr_work_task VALUES(100,1,0,1,'TODO',20,'20,30',NULL,NULL,NULL),(101,1,0,1,'TODO',99,'99',NULL,NULL,NULL),(200,1,0,1,'DOING',40,'40,50',NULL,NULL,NULL),(300,1,0,1,'TODO',NULL,'60','ACTIVE_ORDER_DOSSIER',1,90),(301,1,0,1,'TODO',NULL,'98','ACTIVE_ORDER_DOSSIER',1,80),(400,1,0,1,'TODO',10,'10','RELEASE_APPLICATION',90,NULL)");
            assertEquals(List.of("10"), responsibility(connection, null));
            assertEquals(List.of("20,30"), responsibility(connection, "PQC_RELEASE_PENDING"));
            assertEquals(List.of("40"), responsibility(connection, "MANAGER_RELEASE_PENDING"));
            assertEquals(List.of("60"), responsibility(connection, "REPORT_UPLOAD_PENDING"));
            assertEquals(List.of("10"), responsibility(connection, "PQC_RELEASE_REJECTED"));
            assertEquals(List.of(), responsibility(connection, "RELEASED"));
            st.execute("CREATE TABLE mes_pro_process_pool_active_order_version_upgrade_request(tenant_id BIGINT,deleted INT,source_active_order_id BIGINT,approval_process_instance_id VARCHAR)");
            st.execute("CREATE TABLE ACT_RU_TASK(ID_ VARCHAR,PROC_INST_ID_ VARCHAR,TENANT_ID_ VARCHAR,SUSPENSION_STATE_ INT,ASSIGNEE_ VARCHAR)");
            st.execute("CREATE TABLE ACT_RU_IDENTITYLINK(TASK_ID_ VARCHAR,TYPE_ VARCHAR,USER_ID_ VARCHAR,GROUP_ID_ VARCHAR)");
            st.execute("CREATE TABLE ACT_ID_MEMBERSHIP(USER_ID_ VARCHAR,GROUP_ID_ VARCHAR)");
            st.execute("INSERT INTO mes_pro_process_pool_active_order_version_upgrade_request VALUES(1,0,1,'p1'),(2,0,1,'p2')");
            st.execute("INSERT INTO ACT_RU_TASK VALUES('t1','p1','1',1,'70'),('t2','p2','2',1,'99'),('t3','p1','1',2,'98')");
            st.execute("INSERT INTO ACT_RU_IDENTITYLINK VALUES('t1','candidate','71',NULL)");
            assertEquals(List.of("70"), responsibility(connection, "VERSION_UPGRADE_PENDING"));
            st.execute("UPDATE ACT_RU_TASK SET ASSIGNEE_=NULL WHERE ID_='t1'");
            assertEquals(List.of("71"), responsibility(connection, "VERSION_UPGRADE_PENDING"));
            st.execute("UPDATE ACT_RU_IDENTITYLINK SET USER_ID_=NULL,GROUP_ID_='g1'");
            st.execute("INSERT INTO ACT_ID_MEMBERSHIP VALUES('72','g1')");
            assertEquals(List.of("72"), responsibility(connection, "VERSION_UPGRADE_PENDING"));
        }
    }

    private List<String> responsibility(Connection connection, String status) throws Exception {
        var config = new Configuration();
        String script = String.join("", MesInProcessOrderMapper.class.getMethod("selectResponsibilityCandidates",
                Long.class, Long.class, Long.class, String.class, String.class).getAnnotation(Select.class).value());
        var source = new XMLLanguageDriver().createSqlSource(config, script, Map.class);
        var params = new HashMap<String,Object>();
        params.put("tenantId",1L); params.put("activeOrderId",1L); params.put("applicationId",90L);
        params.put("releaseStatus",status); params.put("businessStatus", "VERSION_UPGRADE_PENDING".equals(status) ? status : "COMPLETED");
        var bound = source.getBoundSql(params);
        var mapped = new MappedStatement.Builder(config,"responsibility",source,SqlCommandType.SELECT).build();
        var tenant = new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
                new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
                    public net.sf.jsqlparser.expression.Expression getTenantId() { return new net.sf.jsqlparser.expression.LongValue(1L); }
                    public boolean ignoreTable(String table) { return table.toUpperCase(java.util.Locale.ROOT).startsWith("ACT_"); }
                });
        try (var statement = connection.prepareStatement(tenant.parserSingle(bound.getSql(), null).replace(" AS CHAR)", " AS VARCHAR)"))) {
            new DefaultParameterHandler(mapped,params,bound).setParameters(statement);
            var rows = statement.executeQuery(); var values = new ArrayList<String>();
            while (rows.next()) values.add(rows.getString(1));
            return values;
        }
    }

    private List<Long> query(Connection connection, MesInProcessOrderPageReqVO request) throws Exception {
        return query(connection,request,null,null,0L,100);
    }
    private List<Long> query(Connection connection, MesInProcessOrderPageReqVO request, java.time.LocalDateTime cursorJoinedAt, Long cursorId, long offset, int limit) throws Exception {
        var config = new Configuration();
        String script = String.join("", MesInProcessOrderMapper.class.getMethod("selectCandidates", Long.class, MesInProcessOrderPageReqVO.class, com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class, java.time.LocalDateTime.class, Long.class, long.class, int.class).getAnnotation(Select.class).value());
        var source = new XMLLanguageDriver().createSqlSource(config, script, Map.class);
        var params = new HashMap<String,Object>();
        params.put("tenantId",1L); params.put("q",request); params.put("ew",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.wrapper(request,MesInProcessOrderMapper.STATIC_FIELDS));
        params.put("cursorJoinedAt",cursorJoinedAt); params.put("cursorId",cursorId); params.put("offset",offset); params.put("limit",limit);
        var bound = source.getBoundSql(params);
        var mapped = new MappedStatement.Builder(config,"inprocess",source,SqlCommandType.SELECT).build();
        var tenant = new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
                new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
                    public net.sf.jsqlparser.expression.Expression getTenantId() { return new net.sf.jsqlparser.expression.LongValue(1L); }
                });
        String rewritten = tenant.parserSingle(bound.getSql(), null).replace(" AS CHAR)", " AS VARCHAR)");
        assertTrue(rewritten.contains("leader.tenant_id = 1"));
        assertTrue(rewritten.contains("n.tenant_id = 1"));
        try (var statement = connection.prepareStatement(rewritten)) {
            new DefaultParameterHandler(mapped,params,bound).setParameters(statement);
            var rows = statement.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rows.next()) ids.add(rows.getLong("id"));
            return ids;
        }
    }
}
