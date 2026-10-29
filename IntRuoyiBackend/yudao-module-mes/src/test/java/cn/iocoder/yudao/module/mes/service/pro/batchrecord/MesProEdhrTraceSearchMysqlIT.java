package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTraceSearchSql;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchSearchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTraceIdentityMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchVisibilityMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRule;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit integration test: missing task-owned MySQL is a failure, never a skipped PASS. */
class MesProEdhrTraceSearchMysqlIT {
    private static Connection db;
    private static SqlSessionFactory sessions;
    private static final String B = "mes_pro_edhr_batch_execution";
    private static final String P = "mes_pro_process_pool_";
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final List<String> tables = new ArrayList<>();

    @BeforeAll static void setup() throws Exception {
        String container = System.getenv().getOrDefault("EDHR_TRACE_MYSQL_CONTAINER", "edhr-rootfix-mysql-20261027");
        assertEquals("edhr-rootfix", com.alibaba.fastjson.JSON.parseObject(docker("inspect", "--format", "{{json .Config.Labels}}", container)).getString("codex.task"),
                "Refuse to write fixtures outside this task's isolated container");
        String port = docker("port", container, "3306/tcp").trim();
        assertTrue(port.startsWith("127.0.0.1:"), "Expected isolated loopback MySQL port");
        db = DriverManager.getConnection("jdbc:mysql://" + port + "/edhr_rootfix?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=LOCAL&rewriteBatchedStatements=true", "root", "");
        table(B, "status INT DEFAULT 20,work_order_id BIGINT DEFAULT 1,route_id BIGINT DEFAULT 1,route_version_id BIGINT DEFAULT 1");
        table("mes_pro_edhr_batch_execution_origin", "batch_execution_id BIGINT,active_order_id BIGINT,work_order_id BIGINT DEFAULT 1,completion_backfill_receipt_id BIGINT,KEY origin_batch(tenant_id,batch_execution_id)");
        table(P + "report_allocation", "active_order_id BIGINT,event_id BIGINT,review_id BIGINT,lifecycle_status VARCHAR(20),KEY alloc_order(tenant_id,active_order_id)");
        table(P + "event", "actual_employee_id BIGINT,signature_user_id BIGINT,server_submit_time DATETIME(6),raw_payload LONGTEXT");
        table(P + "submission_review", "leader_user_id BIGINT,review_signature_user_id BIGINT,reviewed_at DATETIME(6)");
        table("mes_pqc_process_inspection_aggregate_detail", "active_order_id BIGINT,source_pqc_record_id BIGINT,review_id BIGINT,selected_equipment_id BIGINT,selected_equipment_code VARCHAR(100),selected_equipment_name VARCHAR(100),selected_equipment_number VARCHAR(100),KEY detail_order(tenant_id,active_order_id)");
        table(P + "pqc_record", "actual_employee_id BIGINT,signature_user_id BIGINT,server_submit_time DATETIME(6)");
        table(P + "active_order_pick_list_binding", "active_order_id BIGINT,binding_status VARCHAR(20),KEY binding_order(tenant_id,active_order_id)");
        table(P + "active_order_pick_list_binding_item", "binding_id BIGINT,material_number VARCHAR(100),material_name VARCHAR(100),lot_number VARCHAR(100),KEY item_binding(tenant_id,binding_id)");
        table(P + "active_order_completion_receipt", "formal_source_snapshot_json LONGTEXT");
        table("mes_pro_edhr_release_transaction", "batch_execution_id BIGINT,submitted_by BIGINT,submitted_at DATETIME(6),approved_by BIGINT,approved_at DATETIME(6),KEY release_batch(tenant_id,batch_execution_id)");
        table(P + "active_order_release_application", "batch_execution_id BIGINT,applied_by BIGINT,applied_at DATETIME(6),pqc_decided_by BIGINT,pqc_decided_at DATETIME(6),active_order_id BIGINT,work_order_id BIGINT,route_id BIGINT,route_version_id BIGINT,pqc_release_work_task_id BIGINT,application_status VARCHAR(80),pqc_decision VARCHAR(40),"
                + "current_batch_execution_id BIGINT GENERATED ALWAYS AS (CASE WHEN application_status='NONCONFORMANCE_REWORK' OR (application_status='PQC_RELEASE_REJECTED' AND pqc_decision='NONCONFORMANCE_REWORK') THEN NULL ELSE batch_execution_id END) STORED,KEY app_batch(tenant_id,current_batch_execution_id)");
        table("mes_pro_edhr_market_release_handover","batch_execution_id BIGINT,original_user_id BIGINT,recipient_user_id BIGINT,recalled_at DATETIME,KEY handover_batch(tenant_id,batch_execution_id)");
        table("mes_pro_edhr_work_task","batch_execution_id BIGINT,work_order_id BIGINT,route_id BIGINT,task_type VARCHAR(80),business_scope_type VARCHAR(80),ownership_locked TINYINT,status VARCHAR(40),reason VARCHAR(40),business_scope_id BIGINT,candidate_user_snapshot VARCHAR(200),create_time DATETIME,KEY task_batch(tenant_id,batch_execution_id)");
        table(P+"active_order","work_order_id BIGINT,route_id BIGINT,route_version_id BIGINT,leader_user_id BIGINT");
        table("mes_pro_edhr_batch_execution_task","batch_execution_id BIGINT,required_flag TINYINT,node_type VARCHAR(60),batch_record_report_id VARCHAR(80),process_code VARCHAR(80),status INT,required_policy VARCHAR(80),skipped_by BIGINT,skipped_at DATETIME,route_process_sort INT,route_process_id BIGINT,batch_record_sort INT,form_slot_type VARCHAR(40),batch_record_definition_id BIGINT,batch_record_version_id BIGINT,KEY batch_task(tenant_id,batch_execution_id)");
        table("mes_pro_edhr_process_form_permission_rule","enabled TINYINT,rule_type VARCHAR(40),batch_record_report_id VARCHAR(80),batch_record_version_id BIGINT,route_process_id BIGINT,scope_key VARCHAR(100),candidate_source_type VARCHAR(40),candidate_source_ids VARCHAR(100)");
        table("mes_pro_batch_record_report","report_id VARCHAR(80),batch_record_definition_id BIGINT,batch_record_version_id BIGINT");
        table(P + "team_employee_profile", "employee_code VARCHAR(100),employee_name VARCHAR(100),display_name VARCHAR(100)");
        table("system_users", "username VARCHAR(100),nickname VARCHAR(100)");
        table(P + "team_device", "device_code VARCHAR(100),device_name VARCHAR(100)");
        sql("INSERT INTO " + P + "team_employee_profile(id,employee_name) VALUES(11,'Operator')");
        sql("INSERT INTO system_users(id,username) VALUES(12,'Reviewer')");
        sql("INSERT INTO " + P + "team_device(id,device_name) VALUES(31,'Master Device')");
        try (PreparedStatement p = db.prepareStatement("INSERT INTO " + B + "(id) VALUES(?)")) {
            for (int id = 1; id <= 10000; id++) { p.setInt(1,id); p.addBatch(); }
            p.executeBatch();
        }
        sql("INSERT INTO " + B + "(id,tenant_id) VALUES(10001,2)");
        sql("INSERT INTO " + B + "(id,deleted) VALUES(10002,1)");
        live(1,1,0); live(10001,2,0); live(10002,1,0);
        archive(2,false,false); archive(3,true,true);
        sql("INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,completion_backfill_receipt_id) VALUES(4,4,4),(5,5,5)");
        sql("INSERT INTO " + P + "active_order_completion_receipt(id,formal_source_snapshot_json) VALUES(4,'broken-json'),(5,'broken-json')");
        sql("INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id,submitted_by,submitted_at) VALUES(5,5,12,'2026-10-20 10:00:00')");
        sql("INSERT INTO mes_pro_edhr_market_release_handover(id,batch_execution_id,original_user_id,recipient_user_id) VALUES(1,1,100,101),(3,3,100,101)");
        sql("INSERT INTO mes_pro_edhr_market_release_handover(id,tenant_id,batch_execution_id,original_user_id) VALUES(10001,2,10001,100)");
        sql("INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id) VALUES(2,2)");
        sql("INSERT INTO mes_pro_edhr_work_task(id,batch_execution_id,task_type,business_scope_type,ownership_locked,status,business_scope_id,candidate_user_snapshot) VALUES(2,2,'RELEASE_APPROVE','RELEASE_TRANSACTION',1,'TODO',2,'100')");
        sql("INSERT INTO "+P+"team_employee_profile(id,employee_name) VALUES(13,'Pqc Operator')");
        sql("INSERT INTO system_users(id,username) VALUES(14,'InspectorLead')");
        sql("INSERT INTO system_users(id,username) VALUES(15,'Reviewer Hidden')");
        sql("INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id,submitted_by) VALUES(9,9,15)");
        sql("INSERT INTO "+P+"pqc_record(id,actual_employee_id,server_submit_time) VALUES(61,13,'2026-10-22 10:00:00')");
        sql("INSERT INTO "+P+"submission_review(id,leader_user_id,reviewed_at) VALUES(62,14,'2026-10-22 11:00:00')");
        sql("INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,active_order_id,completion_backfill_receipt_id) VALUES(6,6,6,NULL),(7,7,7,7)");
        sql("INSERT INTO mes_pqc_process_inspection_aggregate_detail(id,active_order_id,source_pqc_record_id,review_id,selected_equipment_id,selected_equipment_name) VALUES(6,6,61,62,31,'Inspection Rig')");
        try(var p=db.prepareStatement("INSERT INTO "+P+"active_order_completion_receipt(id,formal_source_snapshot_json) VALUES(7,?)")) {
            p.setString(1,"{\"productionFacts\":{\"events\":[],\"reviews\":[]},\"pqcDetails\":[{\"sourcePqcRecordId\":61,\"reviewId\":62,\"selectedEquipmentId\":31,\"selectedEquipmentName\":\"Inspection Rig\"}],\"pickListBindingItems\":[]}");p.executeUpdate();
        }
        var cfg=new MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setEnvironment(new Environment("trace-mysql",new JdbcTransactionFactory(),
                new UnpooledDataSource("com.mysql.cj.jdbc.Driver",db.getMetaData().getURL(),"root","")));
        var plugins=new MybatisPlusInterceptor();
        DataPermissionRule userRange=new DataPermissionRule(){
            public Set<String> getTableNames(){return Set.of("system_users");}
            public Expression getExpression(String table,net.sf.jsqlparser.expression.Alias alias){
                String column=(alias==null?table:alias.getName())+".id";
                return new net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList<>(new net.sf.jsqlparser.expression.operators.conditional.OrExpression(
                        new net.sf.jsqlparser.expression.operators.relational.EqualsTo(new net.sf.jsqlparser.schema.Column(column),new LongValue(12)),
                        new net.sf.jsqlparser.expression.operators.relational.EqualsTo(new net.sf.jsqlparser.schema.Column(column),new LongValue(14))));
            }
        };
        plugins.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionRuleHandler(new DataPermissionRuleFactoryImpl(List.of(userRange)))));
        plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){
            public Expression getTenantId(){return new LongValue(1);}
        }));
        plugins.addInnerInterceptor(new PaginationInnerInterceptor());
        cfg.addInterceptor(plugins);
        cfg.addMapper(MesProEdhrBatchSearchMapper.class);
        cfg.addMapper(MesProEdhrTraceIdentityMapper.class);
        cfg.addMapper(MesProEdhrBatchExecutionMapper.class);
        cfg.addMapper(MesProEdhrBatchVisibilityMapper.class);
        sessions=new MybatisSqlSessionFactoryBuilder().build(cfg);
    }

    @AfterAll static void cleanup() throws Exception {
        if (db == null) return;
        for (int i=tables.size()-1;i>=0;i--) sql("DROP TABLE " + tables.get(i));
        db.close();
    }

    @Test void liveAndFrozenArrayAndGroupedFactsHaveSameAndSemantics() throws Exception {
        var req = new EdhrBatchExecutionPageReqVO();
        req.setTracePerson("Operator"); req.setTraceEquipment("Device");
        req.setTraceMaterial("Mat-A"); req.setTraceMaterialLot("Lot-A");
        req.setTraceBusinessTime(new LocalDateTime[]{WHEN,WHEN});
        var facts=List.of(
                new MesProEdhrReverseTraceModels.CatalogItem().setCategory(MesProEdhrReverseTraceModels.Category.PERSON).setEvidenceKey("PERSON:EMPLOYEE:11").setSavedValue("11").setRecordedAt(WHEN),
                new MesProEdhrReverseTraceModels.CatalogItem().setCategory(MesProEdhrReverseTraceModels.Category.EQUIPMENT).setEvidenceKey("EQUIPMENT:PRODUCTION:31").setSavedValue("31").setSourceContext(Map.of("deviceName","Saved Device")),
                new MesProEdhrReverseTraceModels.CatalogItem().setCategory(MesProEdhrReverseTraceModels.Category.MATERIAL).setSourceContext(Map.of("materialNumber","Mat-A","lotNumber","Lot-A")),
                new MesProEdhrReverseTraceModels.CatalogItem().setCategory(MesProEdhrReverseTraceModels.Category.MATERIAL).setSourceContext(Map.of("materialNumber","Mat-B","lotNumber","Lot-B")));
        assertTrue(MesProEdhrBatchLookupService.matches(facts,req,Set.of(11L),Set.of(),Set.of(31L)));
        assertEquals(List.of(3L,2L,1L), ids(req,20,0));
        assertEquals(3L, count(req));
        assertEquals(List.of(2L),ids(req,1,1));
        req.setTraceMaterialLot("Lot-B");
        assertFalse(MesProEdhrBatchLookupService.matches(facts,req,Set.of(11L),Set.of(),Set.of(31L)));
        assertEquals(List.of(),ids(req,20,0), "Material and lot must match the same saved row");
        req.setTraceMaterial("' OR 1=1 --");
        assertEquals(0,count(req), "Search strings must remain JDBC bound values");
    }

    @Test void personAndTimeCanMatchDifferentFactsAndBadUnmatchedJsonDoesNotBlock() throws Exception {
        var req = new EdhrBatchExecutionPageReqVO(); req.setTracePerson("Reviewer");
        req.setTraceBusinessTime(new LocalDateTime[]{WHEN,WHEN});
        assertEquals(List.of(5L,3L,2L,1L),ids(req,20,0));
        req.setTraceEquipment("Device");
        assertEquals(List.of(3L,2L,1L),ids(req,20,0));
        req.setTracePerson("No such actor");
        assertEquals(List.of(),ids(req,20,0));
    }

    @Test void selectivePageOverTenThousandBatchesIsBoundedAndUsesOriginIndex() throws Exception {
        var req = new EdhrBatchExecutionPageReqVO(); req.setTraceMaterial("Mat-A");
        assertEquals(3,count(req));
        assertEquals(2,ids(req,2,0).size());
        Query q = query(req);
        try (PreparedStatement p = prepare("EXPLAIN FORMAT=JSON " + q.sql + " ORDER BY " + B + ".id DESC LIMIT 2",q.values);
             ResultSet r=p.executeQuery()) {
            assertTrue(r.next()); String plan=r.getString(1);
            assertTrue(plan.contains("origin_batch"),plan);
            System.out.println("TRACE_SEARCH_MYSQL_EXPLAIN=" + plan);
        }
    }

    @Test void pqcUsesExactRecordReviewAndSavedEquipmentWithoutProductionDeviceNamespaceLeak() throws Exception {
        var req=new EdhrBatchExecutionPageReqVO();req.setTracePerson("Pqc Operator");
        assertEquals(List.of(7L,6L),ids(req,20,0));
        req.setTracePerson("InspectorLead");assertEquals(List.of(7L,6L),ids(req,20,0));
        req.setTraceEquipment("Inspection Rig");assertEquals(List.of(7L,6L),ids(req,20,0));
        req.setTraceEquipment("Master Device");assertEquals(List.of(),ids(req,20,0));
        req.setTracePerson(null);req.setTraceEquipment(null);
        req.setTraceBusinessTime(new LocalDateTime[]{WHEN.plusDays(2),WHEN.plusDays(2)});
        assertEquals(List.of(7L,6L),ids(req,20,0));
    }

    @Test void matchedCorruptReceiptStillFailsOriginalFormalReceiptValidator() throws Exception {
        var req=new EdhrBatchExecutionPageReqVO();req.setTracePerson("Reviewer");
        assertTrue(ids(req,20,0).contains(5L));
        String raw;
        try(var s=db.createStatement();var r=s.executeQuery("SELECT formal_source_snapshot_json FROM "+P+"active_order_completion_receipt WHERE id=5")){assertTrue(r.next());raw=r.getString(1);}
        var batch=new MesProEdhrBatchExecutionDO().setId(5L).setWorkOrderId(1L).setRouteId(1L).setRouteVersionId(1L);batch.setTenantId(1L);
        var origin=new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(5L).setActiveOrderId(5L).setWorkOrderId(1L)
                .setCompletionBackfillReceiptId(5L).setCompletionVersion(1).setCompletionBackfillReceiptHash("receipt-hash").setSourceSnapshotHash("source-hash");origin.setTenantId(1L);
        var receipt=new MesProcessPoolActiveOrderCompletionReceiptDO().setId(5L).setActiveOrderId(5L).setWorkOrderId(1L).setRouteId(1L).setRouteVersionId(1L)
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setCompletedVersion(1).setBatchRecordId(1L).setProcessInspectionId(1L).setFormalSourceSnapshotJson(raw)
                .setLossConditionFactsJson("{}").setSourceSnapshotHash("source-hash").setReceiptHash("receipt-hash");receipt.setTenantId(1L);
        assertTrue(assertThrows(IllegalStateException.class,()->MesProEdhrReverseTraceReceiptValidator.validate(batch,origin,receipt))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test void deletedAndForeignTenantSourcesCannotContributeFacts() throws Exception {
        var req=new EdhrBatchExecutionPageReqVO();req.setTraceMaterial("Mat-A");
        try {
            sql("UPDATE "+P+"active_order_pick_list_binding_item SET deleted=1 WHERE id=1");
            sql("UPDATE "+P+"active_order_completion_receipt SET tenant_id=2 WHERE id=2");
            sql("UPDATE mes_pro_edhr_batch_execution_origin SET deleted=1 WHERE id=3");
            assertEquals(List.of(),ids(req,20,0));assertEquals(0,count(req));
        } finally {
            sql("UPDATE "+P+"active_order_pick_list_binding_item SET deleted=0 WHERE id=1");
            sql("UPDATE "+P+"active_order_completion_receipt SET tenant_id=1 WHERE id=2");
            sql("UPDATE mes_pro_edhr_batch_execution_origin SET deleted=0 WHERE id=3");
        }
    }

    @Test void originalIdentityMapperStillAppliesDataPermissionBeforeJsonSearch() throws Exception {
        try(var session=sessions.openSession()) {
            assertEquals(Set.of(12L),session.getMapper(MesProEdhrTraceIdentityMapper.class).userIds(1L,"Reviewer"));
        }
        var request=new EdhrBatchExecutionPageReqVO();request.setTracePerson("Reviewer Hidden");
        assertEquals(List.of(),ids(request,20,0));assertEquals(0,count(request));
    }

    @Test void retiredReleaseRoundsContributeNeitherPersonNorBusinessTime() throws Exception {
        try {
            sql("INSERT INTO "+P+"active_order_release_application(id,batch_execution_id,application_status,pqc_decision,applied_by,applied_at,pqc_decided_by,pqc_decided_at) VALUES"
                    + "(201,10,'NONCONFORMANCE_REWORK',NULL,14,'2026-10-25 10:00:00',14,'2026-10-25 10:00:00'),"
                    + "(202,10,'PQC_RELEASE_REJECTED','NONCONFORMANCE_REWORK',14,'2026-10-25 10:00:00',14,'2026-10-25 10:00:00'),"
                    + "(203,11,'PQC_RELEASE_PENDING',NULL,14,'2026-10-25 10:00:00',NULL,NULL)");
            var req=new EdhrBatchExecutionPageReqVO();req.setTracePerson("InspectorLead");
            assertEquals(List.of(11L,7L,6L),ids(req,20,0));
            var releaseTime=WHEN.plusDays(5);req.setTraceBusinessTime(new LocalDateTime[]{releaseTime,releaseTime});
            assertEquals(List.of(11L),ids(req,20,0));assertEquals(1,count(req));
            req.setTracePerson(null);assertEquals(List.of(11L),ids(req,20,0));
        } finally {sql("DELETE FROM "+P+"active_order_release_application WHERE id IN (201,202,203)");}
    }

    @Test void realVisibilityPreparationComposesWithTraceCountPageAndIntegrityCursor() throws Exception {
        TenantContextHolder.setTenantId(1L);
        try(var session=sessions.openSession()) {
            var batches=session.getMapper(MesProEdhrBatchExecutionMapper.class);
            var visibilityMapper=session.getMapper(MesProEdhrBatchVisibilityMapper.class);
            var searches=session.getMapper(MesProEdhrBatchSearchMapper.class);
            var visibility=new MesProEdhrBatchVisibilityQueryService();
            var overview=org.mockito.Mockito.mock(MesProEdhrBatchExecutionVisibilityService.class);
            var candidates=org.mockito.Mockito.mock(MesProEdhrCandidateResolver.class);
            ReflectionTestUtils.setField(visibility,"batchMapper",batches);
            ReflectionTestUtils.setField(visibility,"visibilityMapper",visibilityMapper);
            ReflectionTestUtils.setField(visibility,"visibilityService",overview);
            ReflectionTestUtils.setField(visibility,"candidateResolver",candidates);
            var request=new EdhrBatchExecutionPageReqVO();request.setTraceMaterial("Mat-A");
            var scope=MesProEdhrTraceSearchScope.empty();
            var access=visibility.prepare(request,100L,scope);
            var query=access.apply(batches.buildPageQuery(request));
            MesProEdhrTraceSearchSql.apply(query,request,scope);query.getExpression().getOrderBy().clear();
            assertEquals(3,searches.selectSearchCount(1L,query));
            assertEquals(List.of(3L),searches.selectSearchPage(1L,query,0,1).stream().map(MesProEdhrBatchExecutionDO::getId).toList());
            assertEquals(List.of(2L),searches.selectSearchPage(1L,query,1,1).stream().map(MesProEdhrBatchExecutionDO::getId).toList());
            var denied=visibility.prepare(request,200L,scope).apply(batches.buildPageQuery(request));
            MesProEdhrTraceSearchSql.apply(denied,request,scope);denied.getExpression().getOrderBy().clear();
            assertEquals(0,searches.selectSearchCount(1L,denied));assertTrue(searches.selectSearchPage(1L,denied,0,1).isEmpty());
            org.mockito.Mockito.verifyNoInteractions(candidates);
            var integrityRequest=new EdhrBatchExecutionPageReqVO();
            var integrity=visibility.prepare(integrityRequest,100L,scope).apply(batches.buildPageQuery(integrityRequest));
            integrity.getExpression().getOrderBy().clear();
            assertEquals(3L,searches.selectIntegrityUpperId(1L,integrity));
            assertEquals(List.of(1L,2L),searches.selectIntegrityChunk(1L,integrity,0,3,2).stream().map(MesProEdhrBatchExecutionDO::getId).toList());
            assertEquals(List.of(3L),searches.selectIntegrityChunk(1L,integrity,2,3,2).stream().map(MesProEdhrBatchExecutionDO::getId).toList());
            assertTrue(searches.selectIntegrityChunk(1L,integrity,3,3,2).isEmpty());
        } finally {TenantContextHolder.clear();}
    }

    private static void live(int id,int tenant,int deleted) throws Exception {
        sql("INSERT INTO mes_pro_edhr_batch_execution_origin(id,tenant_id,batch_execution_id,active_order_id) VALUES("+id+","+tenant+","+id+","+id+")");
        sql("INSERT INTO " + P + "report_allocation(id,tenant_id,active_order_id,event_id,review_id,lifecycle_status) VALUES("+id+","+tenant+","+id+","+id+",12,'CURRENT')");
        try (var s=db.prepareStatement("INSERT INTO " + P + "event(id,tenant_id,actual_employee_id,server_submit_time,raw_payload) VALUES(?,?,11,?,?)")) {
            s.setInt(1,id);s.setInt(2,tenant);s.setObject(3,WHEN);s.setString(4,"{\"materialDetails\":[{\"selectedDevices\":[{\"deviceId\":31,\"deviceName\":\"Saved Device\"}]}]}");s.executeUpdate();
        }
        if(id==1) sql("INSERT INTO " + P + "submission_review(id,leader_user_id,review_signature_user_id,reviewed_at) VALUES(12,12,12,'2026-10-21 10:00:00')");
        sql("INSERT INTO " + P + "active_order_pick_list_binding(id,tenant_id,active_order_id,binding_status) VALUES("+id+","+tenant+","+id+",'BOUND')");
        sql("INSERT INTO " + P + "active_order_pick_list_binding_item(id,tenant_id,binding_id,material_number,lot_number) VALUES("+id+","+tenant+","+id+",'Mat-A','Lot-A')");
        if(id==1) sql("INSERT INTO " + P + "active_order_pick_list_binding_item(id,binding_id,material_number,lot_number) VALUES(11,1,'Mat-B','Lot-B')");
    }

    private static void archive(int id,boolean grouped,boolean epoch) throws Exception {
        sql("INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,completion_backfill_receipt_id) VALUES("+id+","+id+","+id+")");
        String time=epoch?String.valueOf(WHEN.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()):"\"2026-10-20T10:00:00\"";
        String events="[{\"actualEmployeeId\":11,\"serverSubmitTime\":"+time+",\"rawPayload\":\"{\\\"selectedDevices\\\":[{\\\"deviceId\\\":31,\\\"deviceName\\\":\\\"Saved Device\\\"}]}\"}]";
        String items="[{\"materialNumber\":\"Mat-A\",\"lotNumber\":\"Lot-A\"},{\"materialNumber\":\"Mat-B\",\"lotNumber\":\"Lot-B\"}]";
        String json="{\"productionFacts\":{\"events\":"+(grouped?"{\"group\":"+events+"}":events)+",\"reviews\":[{\"leaderUserId\":12,\"reviewedAt\":\"2026-10-21 10:00:00\"}]},\"pickListBindingItems\":"+(grouped?"{\"group\":"+items+"}":items)+",\"pqcDetails\":[]}";
        try(var p=db.prepareStatement("INSERT INTO "+P+"active_order_completion_receipt(id,formal_source_snapshot_json) VALUES(?,?)")){p.setInt(1,id);p.setString(2,json);p.executeUpdate();}
    }

    private static LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> wrapper(EdhrBatchExecutionPageReqVO request) {
        var wrapper=new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>();
        Set<Long> users=Set.of();
        if(request.getTracePerson()!=null && !request.getTracePerson().isBlank()) {
            try(var session=sessions.openSession()) {
                users=session.getMapper(MesProEdhrTraceIdentityMapper.class).userIds(1L,request.getTracePerson().trim());
            }
        }
        MesProEdhrTraceSearchSql.apply(wrapper,request,new MesProEdhrTraceSearchScope(users));
        return wrapper;
    }
    private static Query query(EdhrBatchExecutionPageReqVO request) {
        Map<String,Object> parameters=Map.of("ew",wrapper(request),"tenantId",1L,"offset",0L,"limit",2);
        var cfg=sessions.getConfiguration();
        BoundSql bound=cfg.getMappedStatement(MesProEdhrBatchSearchMapper.class.getName()+".selectSearchCount").getBoundSql(parameters);
        List<Object> values=new ArrayList<>();
        for(var m:bound.getParameterMappings()) values.add(cfg.newMetaObject(parameters).getValue(m.getProperty()));
        return new Query(bound.getSql().replace("SELECT COUNT(*)", "SELECT "+B+".id"),values);
    }

    private static List<Long> ids(EdhrBatchExecutionPageReqVO req,int limit,int offset) throws Exception {
        try(var session=sessions.openSession()) {
            return session.getMapper(MesProEdhrBatchSearchMapper.class).selectSearchPage(1L,wrapper(req),offset,limit)
                    .stream().map(MesProEdhrBatchExecutionDO::getId).toList();
        }
    }
    private static long count(EdhrBatchExecutionPageReqVO req) throws Exception {
        try(var session=sessions.openSession()) {
            return session.getMapper(MesProEdhrBatchSearchMapper.class).selectSearchCount(1L,wrapper(req));
        }
    }
    private static PreparedStatement prepare(String sql,List<Object> values)throws Exception{
        var p=db.prepareStatement(sql);for(int i=0;i<values.size();i++)p.setObject(i+1,values.get(i));return p;
    }
    private static void table(String name,String columns)throws Exception{
        sql("CREATE TABLE "+name+"(id BIGINT PRIMARY KEY,tenant_id BIGINT NOT NULL DEFAULT 1,deleted TINYINT NOT NULL DEFAULT 0"+(columns.isEmpty()?"":","+columns)+") CHARACTER SET utf8mb4");tables.add(name);
    }
    private static void sql(String sql)throws Exception{try(var s=db.createStatement()){s.execute(sql);}}
    private static String docker(String...args)throws Exception{
        List<String> cmd=new ArrayList<>(List.of("docker"));cmd.addAll(List.of(args));
        Process p=new ProcessBuilder(cmd).redirectErrorStream(true).start();String output=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0,p.waitFor(),output);return output;
    }
    private record Query(String sql,List<Object> values){}
}
