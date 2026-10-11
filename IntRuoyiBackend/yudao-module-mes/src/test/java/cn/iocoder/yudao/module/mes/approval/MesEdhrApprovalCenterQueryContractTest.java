package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Executes generated predicates on isolated H2 data; MySQL sorting/EXPLAIN remains a separate gate. */
public class MesEdhrApprovalCenterQueryContractTest {
    private MesProEdhrWorkTaskMapper work;
    private MesActiveOrderHandoffTaskMapper handoffs;

    @BeforeEach
    void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "work"), MesProEdhrWorkTaskDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "handoff"), MesActiveOrderHandoffTaskDO.class);
        work = mock(MesProEdhrWorkTaskMapper.class, Answers.CALLS_REAL_METHODS);
        handoffs = mock(MesActiveOrderHandoffTaskMapper.class, Answers.CALLS_REAL_METHODS);
    }

    @Test
    void assignedApproveAndReleaseApproveIntersectCandidatesOnceAndExactTokensExclude112() throws Exception {
        try (Connection db = fixture()) {
            insert(db, 1, "APPROVE", 12, "12", null, 1, 0);
            insert(db, 2, "RELEASE_APPROVE", 12, "12,112", null, 1, 0);
            insert(db, 3, "FILL", 112, "112", null, 1, 0);
            insert(db, 4, "FILL", 112, "12,112", null, 1, 0);
            insert(db, 5, "FILL", 12, "112", null, 1, 0);
            assertEquals(List.of(1L, 2L, 4L), ids(db, work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, false, "TODO")));
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(db, work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(), null, 1L, false, "TODO")));
        }
    }

    @Test
    void terminalBatchExclusionArchiveExceptionTenantAndDeletedApplyBeforeUnionCount() throws Exception {
        try (Connection db = fixture()) {
            db.createStatement().execute("INSERT INTO mes_pro_edhr_batch_execution VALUES (7,1,0,30),(8,1,0,40),(9,2,0,40)");
            insert(db, 1, "REVIEW", 12, "12", 7L, 1, 0);
            insert(db, 2, "ARCHIVE", 12, "12", 7L, 1, 0);
            insert(db, 3, "ARCHIVE", 12, "12", 8L, 1, 0);
            insert(db, 4, "APPROVE", 12, "12", 9L, 1, 0);
            insert(db, 5, "APPROVE", 12, "12", null, 2, 0);
            insert(db, 6, "APPROVE", 12, "12", null, 1, 1);
            assertEquals(List.of(2L, 4L), ids(db, work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, false, "TODO")));
        }
    }

    @Test
    void keywordFiltersBothBranchesAndNodeFilterOnlyAppliesToCandidateBranch() throws Exception {
        try (Connection db = fixture()) {
            insert(db, 1, "APPROVE", 12, "12", null, 1, 0);
            insert(db, 2, "FILL", 112, "12", null, 1, 0);
            var req = new MesProEdhrWorkTaskPageReqVO();
            req.setBatchCode("batch-1");
            req.setNodeTypes(List.of("UNKNOWN"));
            assertEquals(List.of(1L), ids(db, work.approvalCenterQuery(req, 12L, 1L, false, "TODO")));
            req.setBatchCode("missing");
            assertEquals(List.of(), ids(db, work.approvalCenterQuery(req, 12L, 1L, false, "TODO")));
        }
    }

    @Test
    void countAnd400PrefixUseSamePredicateWithoutPageParamCapOrDuplicateSubtraction() {
        AtomicReference<LambdaQueryWrapperX<MesProEdhrWorkTaskDO>> counted = new AtomicReference<>();
        AtomicReference<LambdaQueryWrapperX<MesProEdhrWorkTaskDO>> listed = new AtomicReference<>();
        doAnswer(call -> { counted.set(call.getArgument(0)); return 401L; }).when(work).selectCount(any(Wrapper.class));
        doAnswer(call -> { listed.set(call.getArgument(0)); return List.of(); }).when(work).selectList(any(Wrapper.class));
        assertEquals(401L, work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, false, "TODO", 400L).getTotal());
        String predicate = counted.get().getSqlSegment();
        String prefix = listed.get().getSqlSegment();
        assertTrue(prefix.startsWith(predicate));
        assertEquals(counted.get().getParamNameValuePairs(), listed.get().getParamNameValuePairs());
        assertTrue(prefix.endsWith("ORDER BY create_time IS NULL ASC, create_time DESC, CAST(id AS BINARY) ASC LIMIT 400"));
        work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, false, "TODO", (long) Integer.MAX_VALUE + 1);
        assertTrue(listed.get().getSqlSegment().endsWith("LIMIT 2147483648"));
    }

    @Test
    void doneKeepsFormalActorRelationAndSortsCompletedTimeBeforePrefix() {
        var query = work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, true, "TODO");
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("rt.approved_by"));
        assertTrue(sql.contains("rt.tenant_id = mes_pro_edhr_work_task.tenant_id"));
        assertTrue(sql.contains("pa.pqc_decided_at = mes_pro_edhr_work_task.completed_at"));
        assertTrue(sql.contains("nr.closed_at = mes_pro_edhr_work_task.completed_at"));
        assertTrue(query.getParamNameValuePairs().containsValue("DONE"));
        assertFalse(work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(), null, 1L, true, "TODO").getSqlSegment().contains("rt.approved_by"));
        AtomicReference<Wrapper<MesProEdhrWorkTaskDO>> listed = new AtomicReference<>();
        doReturn(1L).when(work).selectCount(any(Wrapper.class));
        doAnswer(call -> { listed.set(call.getArgument(0)); return List.of(); }).when(work).selectList(any(Wrapper.class));
        work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(), 12L, 1L, true, "TODO", 400);
        assertTrue(listed.get().getSqlSegment().contains("ORDER BY completed_at IS NULL ASC, completed_at DESC"));
    }

    @Test
    void handoffCurrentOwnerNotificationNullAndTenantAreGeneratedBeforeWindow() {
        var query = handoffs.approvalCenterQuery(1L, 12L, false, false, "  PQC_%  ");
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("notification_only IS NULL OR notification_only ="));
        assertTrue(sql.contains("FIND_IN_SET("));
        assertTrue(sql.contains("ao.tenant_id = mes_active_order_handoff_task.tenant_id"));
        assertTrue(sql.contains("ao.deleted = 0 AND CAST(ao.active_status AS BINARY) = CAST('ACTIVE' AS BINARY)"));
        assertTrue(sql.contains("CAST(status AS BINARY) = CAST("));
        assertTrue(query.getParamNameValuePairs().containsValue("TODO"));
        assertTrue(sql.contains("LOCATE(CAST("));
        assertTrue(sql.contains("本人生产退回更正"));
        assertTrue(query.getParamNameValuePairs().containsValue("PQC_%"));
        assertFalse(sql.contains("completed_by"));
        var done = handoffs.approvalCenterQuery(1L, 12L, false, true, null);
        assertTrue(done.getSqlSegment().contains("completed_by ="));
        assertTrue(done.getSqlSegment().contains("FIND_IN_SET("));
        assertFalse(done.getSqlSegment().contains("active_status"));
        var global = handoffs.approvalCenterQuery(1L, 12L, true, true, null);
        assertFalse(global.getSqlSegment().contains("completed_by"));
        assertFalse(global.getSqlSegment().contains("FIND_IN_SET"));
        assertTrue(global.getSqlSegment().contains("tenant_id ="));
    }

    @Test void generatedDonePredicateRetainsDisabledFormalPqcAndQaActorsButRejectsTenantAndDeleted() throws Exception {
        try (Connection db = fixture()) {
            for (String column : List.of("business_scope_type VARCHAR","business_scope_id BIGINT","work_order_id BIGINT",
                    "reason VARCHAR","completed_at TIMESTAMP","review_source_type VARCHAR","review_source_id BIGINT")) {
                db.createStatement().execute("ALTER TABLE mes_pro_edhr_work_task ADD " + column);
            }
            db.createStatement().execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,deleted INT,status INT)");
            db.createStatement().execute("CREATE TABLE mes_pro_edhr_release_transaction(id BIGINT,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,work_order_id BIGINT,release_status VARCHAR,approved_by BIGINT)");
            db.createStatement().execute("CREATE TABLE mes_pro_process_pool_active_order_release_application(id BIGINT,tenant_id BIGINT,deleted INT,pqc_release_work_task_id BIGINT,work_order_id BIGINT,active_order_id BIGINT,batch_execution_id BIGINT,pqc_decided_by BIGINT,pqc_decided_at TIMESTAMP,pqc_decision VARCHAR,application_status VARCHAR,pqc_reject_reason VARCHAR,qa_closure_review_id BIGINT)");
            db.createStatement().execute("CREATE TABLE mes_pro_edhr_nonconformance_review(id BIGINT,tenant_id BIGINT,deleted INT,review_status VARCHAR,active_order_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,closed_at TIMESTAMP,qa_user_id BIGINT,disposition VARCHAR)");
            db.createStatement().execute("INSERT INTO system_users VALUES(12,1,0,1)");
            db.createStatement().execute("INSERT INTO mes_pro_edhr_work_task(id,tenant_id,deleted,task_type,assignee_user_id,status,business_scope_type,business_scope_id,work_order_id,reason,completed_at,review_source_type,review_source_id) VALUES"
                    + "(1,1,FALSE,'PQC_PRODUCTION_RELEASE',999,'DONE','RELEASE_APPLICATION',91,93,'APPROVE','2026-10-07 10:00:00',NULL,NULL),"
                    + "(2,1,FALSE,'PQC_PRODUCTION_RELEASE',999,'DONE','RELEASE_APPLICATION',92,93,'REJECT','2026-10-07 10:00:00',NULL,NULL),"
                    + "(3,1,FALSE,'PQC_PRODUCTION_RELEASE',999,'DONE','RELEASE_APPLICATION',94,93,'NONCONFORMANCE_VOID','2026-10-07 10:00:00','EDHR_NONCONFORMANCE_REVIEW',97)");
            db.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES"
                    + "(91,1,0,1,93,180,92,12,'2026-10-07 10:00:00','APPROVE','MANAGER_RELEASE_PENDING',NULL,NULL),"
                    + "(92,1,0,2,93,180,92,12,'2026-10-07 10:00:00','REJECT','NONCONFORMANCE_VOID','原拒绝意见',96),"
                    + "(94,1,0,3,93,180,92,NULL,NULL,NULL,'NONCONFORMANCE_VOID',NULL,97)");
            db.createStatement().execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES"
                    + "(96,1,0,'closed',180,93,92,'2026-10-07 11:00:00',18,'void'),"
                    + "(97,1,0,'closed',180,93,92,'2026-10-07 10:00:00',12,'void')");
            assertEquals(List.of(1L,2L,3L),ids(db,work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(),12L,1L,true,"TODO")));
            db.createStatement().execute("UPDATE system_users SET status=0");
            assertEquals(List.of(1L,2L,3L),ids(db,work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(),12L,1L,true,"TODO")));
            db.createStatement().execute("UPDATE system_users SET tenant_id=2");
            assertEquals(List.of(),ids(db,work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(),12L,1L,true,"TODO")));
            db.createStatement().execute("UPDATE system_users SET tenant_id=1,deleted=1");
            assertEquals(List.of(),ids(db,work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(),12L,1L,true,"TODO")));
        }
    }

    @Test
    void handoffCountAndPrefixShareKeywordAndEligibility() {
        AtomicReference<LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>> counted = new AtomicReference<>();
        AtomicReference<LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>> listed = new AtomicReference<>();
        doAnswer(call -> {
            LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO> query = call.getArgument(0);
            if (query.getSqlSegment().contains("task_type IS NULL")) return 0L;
            counted.set(query); return 500L;
        }).when(handoffs).selectCount(any(Wrapper.class));
        doAnswer(call -> { listed.set(call.getArgument(0)); return List.of(); }).when(handoffs).selectList(any(Wrapper.class));
        assertEquals(500L, handoffs.selectApprovalCenterPrefix(1L, 12L, false, true, "null", 400L).getTotal());
        assertTrue(listed.get().getSqlSegment().startsWith(counted.get().getSqlSegment()));
        assertEquals(counted.get().getParamNameValuePairs(), listed.get().getParamNameValuePairs());
        assertTrue(listed.get().getSqlSegment().endsWith("CAST(id AS BINARY) ASC LIMIT 400"));
        assertTrue(listed.get().getSqlSegment().contains("completed_at IS NULL ASC, completed_at DESC"));
    }

    @Test
    void actualHandoffPredicateRejectsInactiveOtherTenantDeletedNotificationAndToken112() throws Exception {
        try (Connection db = fixture()) {
            db.createStatement().execute("CREATE ALIAS FIND_IN_SET FOR 'cn.iocoder.yudao.module.mes.approval.MesEdhrApprovalCenterQueryContractTest.findInSet'");
            db.createStatement().execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT, tenant_id BIGINT, deleted INT, active_status VARCHAR)");
            db.createStatement().execute("CREATE TABLE mes_active_order_handoff_task(id BIGINT, tenant_id BIGINT, deleted BOOLEAN, notification_only BOOLEAN, candidate_user_snapshot VARCHAR, completed_by BIGINT, active_order_id BIGINT, status VARCHAR)");
            db.createStatement().execute("INSERT INTO mes_pro_process_pool_active_order VALUES (1,1,0,'ACTIVE'),(2,1,0,'SUPERSEDED'),(3,2,0,'ACTIVE'),(4,1,1,'ACTIVE')");
            db.createStatement().execute("INSERT INTO mes_active_order_handoff_task VALUES"
                    + "(1,1,FALSE,NULL,'12',NULL,1,'TODO'),(2,1,FALSE,FALSE,'12,112',NULL,1,'TODO'),"
                    + "(3,1,FALSE,TRUE,'12',NULL,1,'TODO'),(4,1,FALSE,NULL,'12',NULL,2,'TODO'),"
                    + "(5,1,FALSE,NULL,'12',NULL,3,'TODO'),(6,1,FALSE,NULL,'12',NULL,4,'TODO'),"
                    + "(7,1,TRUE,NULL,'12',NULL,1,'TODO'),(8,2,FALSE,NULL,'12',NULL,3,'TODO'),"
                    + "(9,1,FALSE,NULL,'112',NULL,1,'TODO'),(10,1,FALSE,NULL,'12',12,2,'DONE'),"
                    + "(11,1,FALSE,NULL,'12',112,2,'DONE'),(12,1,FALSE,NULL,'112',12,2,'DONE')");
            assertEquals(List.of(1L, 2L), idsForTable(db, handoffs.approvalCenterQuery(1L,12L,false,false,null), "mes_active_order_handoff_task"));
            assertEquals(List.of(1L, 2L, 9L), idsForTable(db, handoffs.approvalCenterQuery(1L,12L,true,false,null), "mes_active_order_handoff_task"));
            assertEquals(List.of(10L), idsForTable(db, handoffs.approvalCenterQuery(1L,12L,false,true,null), "mes_active_order_handoff_task"));
            assertEquals(List.of(10L, 11L, 12L), idsForTable(db, handoffs.approvalCenterQuery(1L,12L,true,true,null), "mes_active_order_handoff_task"));
        }
    }

    public static int findInSet(String actor, String snapshot) {
        return snapshot == null ? 0 : Arrays.asList(snapshot.split(",", -1)).indexOf(actor) + 1;
    }

    @Test
    void unknownHandoffTypeCannotBeHiddenByKeywordCaseExpression() {
        // Propagation only; the read-only MySQL CTE gate separately executes the actual invalid-type count.
        doReturn(1L).when(handoffs).selectCount(any(Wrapper.class));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> handoffs.selectApprovalCenterPrefix(1L, 12L, false, false, "missing", 400L));
        verify(handoffs, never()).selectList(any(Wrapper.class));
    }

    @Test
    void completeProductionBoundSqlRetainsTenantFiltersAndBinaryWindowAcrossAllViewsWithoutJdbc() throws Exception {
        // Generate injected production statements, not SELECT stubs; this test never opens a connection.
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var database = new GlobalConfig.DbConfig().setLogicDeleteValue("1").setLogicNotDeleteValue("0");
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig()
                .setDbConfig(database).setSqlInjector(new DefaultSqlInjector()));
        configuration.addMapper(MesProEdhrWorkTaskMapper.class);
        configuration.addMapper(MesActiveOrderHandoffTaskMapper.class);
        for (Class<?> related : List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO.class,
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO.class,
                cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO.class)) {
            var assistant = new MapperBuilderAssistant(configuration, "parser-related-metadata");
            assistant.setCurrentNamespace(related.getName());
            TableInfoHelper.initTableInfo(assistant, related);
        }
        var properties = new TenantProperties();
        properties.setIgnoreTables(Set.of("dcc_product_catalog"));
        var handler = new TenantDatabaseInterceptor(properties);
        var tenant = new TenantLineInnerInterceptor(handler);
        var pagination = new PaginationInnerInterceptor();
        TenantContextHolder.setTenantId(7L);
        try {
            assertFalse(handler.ignoreTable("mes_pro_edhr_work_task"));
            assertFalse(handler.ignoreTable("mes_active_order_handoff_task"));
            var workCounts = new ArrayList<LambdaQueryWrapperX<MesProEdhrWorkTaskDO>>();
            var workLists = new ArrayList<LambdaQueryWrapperX<MesProEdhrWorkTaskDO>>();
            var handoffCounts = new ArrayList<LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>>();
            var handoffLists = new ArrayList<LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>>();
            // Only capture wrappers from real default prefix methods; returned rows/counts are not DB evidence.
            doAnswer(call -> { workCounts.add(call.getArgument(0)); return 0L; }).when(work).selectCount(any(Wrapper.class));
            doAnswer(call -> { workLists.add(call.getArgument(0)); return List.of(); }).when(work).selectList(any(Wrapper.class));
            doAnswer(call -> { handoffCounts.add(call.getArgument(0)); return 0L; }).when(handoffs).selectCount(any(Wrapper.class));
            doAnswer(call -> { handoffLists.add(call.getArgument(0)); return List.of(); }).when(handoffs).selectList(any(Wrapper.class));
            int statements = 0;
            for (boolean done : List.of(false, true)) for (boolean global : List.of(false, true)) {
                for (List<String> nodes : List.of(List.<String>of(), List.of("FINISHED_PRODUCT_INSPECTION_RECORD"), List.of("UNKNOWN"))) {
                    workCounts.clear(); workLists.clear();
                    var request = new MesProEdhrWorkTaskPageReqVO();
                    request.setNodeTypes(nodes);
                    work.selectApprovalCenterPrefix(request, global ? null : 12L, 7L, done, "TODO", 400L);
                    assertEquals(1, workCounts.size()); assertEquals(1, workLists.size());
                    String countSql = assertParsedBoundSql(configuration, tenant, pagination, MesProEdhrWorkTaskMapper.class,
                            "selectCount", workCounts.get(0), false, done);
                    String listSql = assertParsedBoundSql(configuration, tenant, pagination, MesProEdhrWorkTaskMapper.class,
                            "selectList", workLists.get(0), true, done);
                    assertEquals(((Select) JsqlParserGlobal.parse(countSql)).getPlainSelect().getWhere().toString(),
                            ((Select) JsqlParserGlobal.parse(listSql)).getPlainSelect().getWhere().toString(),
                            "tenant-processed work count/list must retain identical eligibility");
                    assertEquals(workCounts.get(0).getParamNameValuePairs(), workLists.get(0).getParamNameValuePairs());
                    if (!done && nodes.contains("FINISHED_PRODUCT_INSPECTION_RECORD")) {
                        for (String sql : List.of(countSql, listSql)) {
                            assertTrue(sql.contains("node_type IN ('FINISHED_PRODUCT_INSPECTION_RECORD')"));
                            assertTrue(sql.contains("mes_pro_edhr_batch_execution_task"));
                        }
                    }
                    if (!done && nodes.contains("UNKNOWN")) {
                        assertTrue(countSql.contains("1 = 0")); assertTrue(listSql.contains("1 = 0"));
                    }
                    if (done && !global) {
                        for (String sql : List.of(countSql, listSql)) {
                            assertTrue(sql.contains("rt.approved_by"));
                            assertTrue(sql.contains("rt.tenant_id = 7"));
                            assertTrue(sql.contains("pa.tenant_id = 7"));
                            assertTrue(sql.contains("nr.tenant_id = 7"));
                        }
                    }
                    statements += 2;
                }
                for (String keyword : Arrays.asList(null, "PQC_%\\Literal")) {
                    handoffCounts.clear(); handoffLists.clear();
                    handoffs.selectApprovalCenterPrefix(7L, 12L, global, done, keyword, 400L);
                    assertEquals(2, handoffCounts.size()); assertEquals(1, handoffLists.size());
                    String invalidSql = assertParsedBoundSql(configuration, tenant, pagination, MesActiveOrderHandoffTaskMapper.class,
                            "selectCount", handoffCounts.get(0), false, done);
                    String countSql = assertParsedBoundSql(configuration, tenant, pagination, MesActiveOrderHandoffTaskMapper.class,
                            "selectCount", handoffCounts.get(1), false, done);
                    String listSql = assertParsedBoundSql(configuration, tenant, pagination, MesActiveOrderHandoffTaskMapper.class,
                            "selectList", handoffLists.get(0), true, done);
                    assertEquals(((Select) JsqlParserGlobal.parse(countSql)).getPlainSelect().getWhere().toString(),
                            ((Select) JsqlParserGlobal.parse(listSql)).getPlainSelect().getWhere().toString(),
                            "tenant-processed handoff count/list must retain identical eligibility");
                    assertTrue(invalidSql.contains("task_type IS NULL"));
                    assertTrue(Pattern.compile("task_type IS NULL OR CAST\\(task_type AS BINARY\\) NOT IN ")
                            .matcher(invalidSql).find(), "NULL rejection must share the parenthesized invalid-type branch");
                    assertTrue(Pattern.compile("CAST\\(task_type AS BINARY\\) NOT IN \\(CAST\\(\\? AS BINARY\\)"
                            + "(?:, CAST\\(\\? AS BINARY\\)){7}\\)").matcher(invalidSql).find(),
                            "all eight formal whitelist values must be bound binary operands");
                    var formalTypes = cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.TYPES
                            .stream().sorted().toList();
                    BoundSql invalidBound = configuration.getMappedStatement(MesActiveOrderHandoffTaskMapper.class.getName()
                            + ".selectCount").getBoundSql(Map.of("ew", handoffCounts.get(0)));
                    var boundTypes = invalidBound.getParameterMappings().stream()
                            .map(mapping -> mapping.getProperty().substring("ew.paramNameValuePairs.".length()))
                            .map(name -> handoffCounts.get(0).getParamNameValuePairs().get(name))
                            .filter(formalTypes::contains).toList();
                    assertEquals(formalTypes, boundTypes, "the sole formal TYPES set must supply all ordered bindings");
                    assertFalse(invalidSql.contains("LOCATE("), "unknown types are checked before keyword filtering");
                    assertEquals(handoffCounts.get(1).getParamNameValuePairs(), handoffLists.get(0).getParamNameValuePairs());
                    for (String sql : List.of(invalidSql, countSql, listSql)) {
                        assertTrue(sql.contains("CAST(status AS BINARY) = CAST(? AS BINARY)"));
                        assertEquals(!done, sql.contains("CAST(ao.active_status AS BINARY) = CAST('ACTIVE' AS BINARY)"));
                        if (!done) assertTrue(sql.contains("ao.tenant_id = 7"));
                    }
                    for (var wrapper : List.of(handoffCounts.get(0), handoffCounts.get(1), handoffLists.get(0))) {
                        assertTrue(wrapper.getParamNameValuePairs().containsValue(done ? "DONE" : "TODO"));
                        assertFalse(wrapper.getParamNameValuePairs().containsValue(done ? "TODO" : "DONE"));
                    }
                    for (String sql : List.of(countSql, listSql)) {
                        assertEquals(keyword != null, sql.contains("LOCATE("));
                        assertEquals(!global, sql.contains("FIND_IN_SET("));
                        assertEquals(done && !global, sql.contains("completed_by ="));
                        assertEquals(!done, sql.contains("CAST(ao.active_status AS BINARY) = CAST('ACTIVE' AS BINARY)"));
                        if (!done) assertTrue(sql.contains("ao.tenant_id = 7"));
                        if (keyword != null) assertFalse(sql.contains(keyword), "literal keyword must remain parameter-bound");
                    }
                    if (keyword != null) assertTrue(handoffCounts.get(1).getParamNameValuePairs().containsValue(keyword));
                    statements += 3;
                }
            }
            assertEquals(48, statements, "complete injected count/list/invalid SQL parser cases");
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String assertParsedBoundSql(MybatisConfiguration configuration, TenantLineInnerInterceptor tenant,
                                        PaginationInnerInterceptor pagination, Class<?> mapper, String method,
                                        LambdaQueryWrapperX<?> wrapper, boolean list, boolean done) throws Exception {
        var mapped = configuration.getMappedStatement(mapper.getName() + "." + method);
        Map<String, Object> parameters = Map.of("ew", wrapper);
        BoundSql bound = mapped.getBoundSql(parameters);
        var mappings = List.copyOf(bound.getParameterMappings());
        assertTrue(bound.getSql().stripLeading().startsWith("SELECT"));
        assertFalse(bound.getSql().contains("#{"), "production BoundSql must bind every wrapper parameter");
        // Same production ordering: tenant first, pagination second; no IPage means no pagination count rewrite.
        tenant.beforeQuery(null, mapped, parameters, RowBounds.DEFAULT, null, bound);
        assertTrue(pagination.willDoQuery(null, mapped, parameters, RowBounds.DEFAULT, null, bound));
        pagination.beforeQuery(null, mapped, parameters, RowBounds.DEFAULT, null, bound);
        assertEquals(mappings, bound.getParameterMappings());
        var parsed = assertInstanceOf(Select.class, JsqlParserGlobal.parse(bound.getSql())).getPlainSelect();
        assertEquals(TableInfoHelper.getTableInfo(mapper == MesProEdhrWorkTaskMapper.class
                ? MesProEdhrWorkTaskDO.class : MesActiveOrderHandoffTaskDO.class).getTableName(),
                assertInstanceOf(net.sf.jsqlparser.schema.Table.class, parsed.getFromItem()).getName());
        String predicate = parsed.getWhere().toString();
        var rootTenant = assertInstanceOf(EqualsTo.class,
                assertInstanceOf(AndExpression.class, parsed.getWhere()).getRightExpression());
        assertEquals("tenant_id", assertInstanceOf(Column.class, rootTenant.getLeftExpression()).getColumnName());
        assertEquals(7L, assertInstanceOf(LongValue.class, rootTenant.getRightExpression()).getValue(),
                "production tenant predicate must be added at the root, outside the candidate union");
        assertTrue(predicate.contains("deleted = 0"));
        if (list) {
            assertTrue(parsed.getSelectItems().size() > 1, "list must include complete production entity projection");
            assertTrue(parsed.getSelectItems().stream().anyMatch(item -> "task_type".equals(item.toString())));
            var order = parsed.getOrderByElements();
            assertEquals(3, order.size());
            String time = done ? "completed_at" : "create_time";
            var missing = assertInstanceOf(IsNullExpression.class, order.get(0).getExpression());
            assertFalse(missing.isNot());
            assertEquals(time, assertInstanceOf(Column.class, missing.getLeftExpression()).getColumnName());
            assertTrue(order.get(0).isAsc());
            assertEquals(time, assertInstanceOf(Column.class, order.get(1).getExpression()).getColumnName());
            assertFalse(order.get(1).isAsc());
            var id = assertInstanceOf(CastExpression.class, order.get(2).getExpression());
            assertEquals("id", assertInstanceOf(Column.class, id.getLeftExpression()).getColumnName());
            assertEquals("BINARY", id.getColDataType().getDataType().toUpperCase(Locale.ROOT));
            assertTrue(order.get(2).isAsc());
            assertEquals(400L, assertInstanceOf(LongValue.class, parsed.getLimit().getRowCount()).getValue());
        } else {
            assertEquals(1, parsed.getSelectItems().size());
            assertTrue(parsed.getSelectItems().get(0).toString().toUpperCase(Locale.ROOT).startsWith("COUNT("));
            assertTrue(parsed.getOrderByElements() == null || parsed.getOrderByElements().isEmpty());
            assertNull(parsed.getLimit());
        }
        return bound.getSql();
    }

    private Connection fixture() throws SQLException {
        Connection db = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        db.createStatement().execute("CREATE TABLE mes_pro_edhr_work_task (id BIGINT, tenant_id BIGINT, deleted BOOLEAN, task_type VARCHAR, assignee_user_id BIGINT, candidate_user_snapshot VARCHAR, batch_execution_id BIGINT, batch_code VARCHAR, batch_task_id BIGINT, status VARCHAR)");
        db.createStatement().execute("CREATE TABLE mes_pro_edhr_batch_execution (id BIGINT,tenant_id BIGINT,deleted INT,status INT)");
        return db;
    }

    private void insert(Connection db, long id, String type, long owner, String candidates, Long batch, long tenant, int deleted) throws SQLException {
        try (PreparedStatement sql = db.prepareStatement("INSERT INTO mes_pro_edhr_work_task VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            sql.setLong(1, id); sql.setLong(2, tenant); sql.setBoolean(3, deleted != 0); sql.setString(4, type);
            sql.setLong(5, owner); sql.setString(6, candidates); sql.setObject(7, batch);
            sql.setString(8, "batch-" + id); sql.setObject(9, null); sql.setString(10, "TODO"); sql.executeUpdate();
        }
    }

    private List<Long> ids(Connection db, LambdaQueryWrapperX<MesProEdhrWorkTaskDO> query) throws SQLException {
        return idsForTable(db, query, "mes_pro_edhr_work_task");
    }

    private List<Long> idsForTable(Connection db, LambdaQueryWrapperX<?> query, String table) throws SQLException {
        String segment = query.getSqlSegment();
        var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}").matcher(segment);
        var values = new ArrayList<Object>();
        while (matcher.find()) values.add(query.getParamNameValuePairs().get(matcher.group(1)));
        String sql = "SELECT id FROM " + table + " WHERE " + matcher.replaceAll("?") + " ORDER BY id";
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
            var ids = new ArrayList<Long>();
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) ids.add(rows.getLong(1)); }
            return ids;
        }
    }
}
