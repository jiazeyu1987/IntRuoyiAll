package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Explicit -Dtest opt-in gate. No running-library mutation; missing private connection inputs fail. */
class MesEdhrApprovalCenterMySqlReadOnlyIT {
    private static final String WORK = "mes_pro_edhr_work_task";
    private static final String HANDOFF = "mes_active_order_handoff_task";
    // Only these explicitly named, non-secret columns may be read into the independent oracle.
    private static final Map<String,String> SCHEMA = Map.of(
            "system_tenant", "id,name,status,deleted",
            "system_users", "id,tenant_id,deleted,status,username",
            WORK, "id,tenant_id,deleted,task_type,status,assignee_user_id,candidate_user_snapshot,batch_execution_id,batch_task_id,batch_code,work_order_code,process_name,work_order_id,business_scope_type,business_scope_id,review_source_type,review_source_id,reason,create_time,completed_at",
            "mes_pro_edhr_batch_execution", "id,tenant_id,deleted,status",
            "mes_pro_edhr_batch_execution_task", "id,tenant_id,deleted,node_type",
            HANDOFF, "id,tenant_id,deleted,task_type,status,notification_only,candidate_user_snapshot,completed_by,active_order_id,work_order_id,reason,create_time,completed_at",
            "mes_pro_process_pool_active_order", "id,tenant_id,deleted,active_status",
            "mes_pro_edhr_release_transaction", "id,tenant_id,deleted,batch_execution_id,work_order_id,release_status,approved_by",
            "mes_pro_process_pool_active_order_release_application", "id,tenant_id,deleted,pqc_release_work_task_id,work_order_id,active_order_id,batch_execution_id,pqc_decided_by,pqc_decided_at,pqc_decision,application_status,pqc_reject_reason,qa_closure_review_id",
            "mes_pro_edhr_nonconformance_review", "id,tenant_id,deleted,review_status,active_order_id,work_order_id,batch_execution_id,closed_at,qa_user_id,disposition");

    /** Actual tables, one read-only snapshot; CTE tests below cover absent business counterexamples. */
    @Test
    void actualAuthorizedTablesMatchIndependentOracleCountsPrefixesEveryPageAndExplain() throws Exception {
        try (Connection db = openLocalReadOnlySnapshot()) {
            try {
                requireSchema(db);
                long connectionId = assertPhysicalSnapshot(db, null);
                var tenants = readRows(db, "SELECT id FROM system_tenant WHERE deleted=0 AND status=0 AND CAST(name AS BINARY)=CAST(? AS BINARY)", "芋道源码");
                assertEquals(1,tenants.size(),"MYSQL_GATE_NAMED_TENANT_RELATION_MISSING_OR_AMBIGUOUS");
                long tenant = number(tenants.get(0),"id");
                assertTrue(tenant>0,"MYSQL_GATE_NAMED_TENANT_RELATION_INVALID");
                var admins = readRows(db, "SELECT id,tenant_id FROM system_users WHERE deleted=0 AND status=0 AND tenant_id=? AND CAST(username AS BINARY)=CAST(? AS BINARY)", tenant,"admin");
                assertEquals(1, admins.size(), "MYSQL_GATE_ADMIN_RELATION_MISSING_OR_AMBIGUOUS");
                long actor = number(admins.get(0), "id");
                assertTrue(actor > 0 && number(admins.get(0),"tenant_id")==tenant, "MYSQL_GATE_ADMIN_RELATION_INVALID");
                var facts = new HashMap<String,Map<Long,Map<String,Object>>>();
                for (var entry : SCHEMA.entrySet()) {
                    // Tenant is only an identity prerequisite; never load package/contact facts.
                    if (entry.getKey().equals("system_tenant")) continue;
                    // Do not read usernames or credentials as part of user facts.
                    String columns = entry.getKey().equals("system_users") ? "id,tenant_id,status" : entry.getValue();
                    var rows = readRows(db, "SELECT " + columns + " FROM " + entry.getKey() + " WHERE tenant_id=? AND deleted=0", tenant);
                    var byId = new HashMap<Long,Map<String,Object>>();
                    for (var row : rows) assertNull(byId.put(number(row,"id"),row), "duplicate formal table identity");
                    facts.put(entry.getKey(), byId);
                }
                assertFalse(facts.get(WORK).isEmpty(), "MYSQL_GATE_FORMAL_WORK_SAMPLE_MISSING");
                assertFalse(facts.get(HANDOFF).isEmpty(), "MYSQL_GATE_FORMAL_HANDOFF_SAMPLE_MISSING");
                String batch = facts.get(WORK).values().stream().map(row -> text(row,"batch_code"))
                        .filter(value -> value != null && !value.isBlank()).sorted().findFirst().orElse(null);
                assertNotNull(batch,"MYSQL_GATE_FORMAL_BATCH_KEYWORD_SAMPLE_MISSING");
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"actual-work"),MesProEdhrWorkTaskDO.class);
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"actual-handoff"),MesActiveOrderHandoffTaskDO.class);
                var work = mock(MesProEdhrWorkTaskMapper.class, Answers.CALLS_REAL_METHODS);
                var handoff = mock(MesActiveOrderHandoffTaskMapper.class, Answers.CALLS_REAL_METHODS);
                doAnswer(call -> count(db, WORK, call.getArgument(0))).when(work).selectCount(any(Wrapper.class));
                doAnswer(call -> count(db, HANDOFF, call.getArgument(0))).when(handoff).selectCount(any(Wrapper.class));
                doAnswer(call -> selectIds(db,"",WORK,call.getArgument(0),false).stream()
                        .map(id -> new MesProEdhrWorkTaskDO().setId(id)).toList()).when(work).selectList(any(Wrapper.class));
                doAnswer(call -> selectIds(db,"",HANDOFF,call.getArgument(0),false).stream()
                        .map(id -> new MesActiveOrderHandoffTaskDO().setId(id)).toList()).when(handoff).selectList(any(Wrapper.class));

                for (boolean done : List.of(false,true)) for (boolean global : List.of(false,true)) {
                    checkActualCase(db,facts,work,handoff,tenant,actor,global,done,"TODO",new MesProEdhrWorkTaskPageReqVO(),null);
                    if (!done) {
                        checkActualCase(db,facts,work,handoff,tenant,actor,global,false,"OVERDUE",new MesProEdhrWorkTaskPageReqVO(),null);
                        var nodes = new MesProEdhrWorkTaskPageReqVO();
                        nodes.setNodeTypes(List.of("FINISHED_PRODUCT_INSPECTION_RECORD"));
                        checkActualCase(db,facts,work,handoff,tenant,actor,global,false,"TODO",nodes,null);
                    }
                    // Exact SQL LIKE semantics are evaluated independently on the batch-code column.
                    var batchKeyword = new MesProEdhrWorkTaskPageReqVO(); batchKeyword.setBatchCode(batch);
                    checkActualCase(db,facts,work,handoff,tenant,actor,global,done,"TODO",batchKeyword,null);
                    checkActualCase(db,facts,work,handoff,tenant,actor,global,done,"TODO",new MesProEdhrWorkTaskPageReqVO(),"QA");
                }
                assertPhysicalSnapshot(db,connectionId);
                // Counts are evidence only; never print rows, usernames or private connection inputs.
                System.out.println("MYSQL_ACTUAL_TABLE_ORACLE: tenant="+tenant+", workRows="+facts.get(WORK).size()
                        +", handoffRows="+facts.get(HANDOFF).size()+"; CTE counterexamples are separate; Spring deployment connection unverified");
                for (String state : List.of("TODO","DONE","CANCELED")) {
                    long workRows=facts.get(WORK).values().stream().filter(row -> state.equals(text(row,"status"))).count();
                    long handoffRows=facts.get(HANDOFF).values().stream().filter(row -> state.equals(text(row,"status"))).count();
                    System.out.println("MYSQL_ACTUAL_SAMPLE_STATE: state="+state+", work="+workRows+", H="+handoffRows);
                }
            } finally { db.rollback(); }
        } catch (SQLException failure) { throw redactedSqlFailure(failure); }
    }

    @Test
    void generatedWorkAndHandoffPredicatesAndBinaryPrefixExecuteOnRealLocalMySqlCtes() throws Exception {
        try (Connection db = openLocalReadOnlySnapshot()) {
            try {
                long connectionId = assertPhysicalSnapshot(db,null);
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"work"),MesProEdhrWorkTaskDO.class);
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"handoff"),MesActiveOrderHandoffTaskDO.class);
                var work = mock(MesProEdhrWorkTaskMapper.class,Answers.CALLS_REAL_METHODS);
                var handoff = mock(MesActiveOrderHandoffTaskMapper.class,Answers.CALLS_REAL_METHODS);
                AtomicReference<LambdaQueryWrapperX<MesProEdhrWorkTaskDO>> workPrefix = new AtomicReference<>();
                doReturn(0L).when(work).selectCount(any(Wrapper.class));
                doAnswer(call -> { workPrefix.set(call.getArgument(0)); return List.of(); }).when(work).selectList(any(Wrapper.class));
                work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(),12L,1L,false,"TODO",400);
                String workFixture = "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<805),"
                        + "mes_pro_edhr_work_task AS (SELECT n id,1 tenant_id,0 deleted,'APPROVE' task_type,12 assignee_user_id,'12,112' candidate_user_snapshot,NULL batch_execution_id,"
                        + "'TODO' status,DATE_SUB(CAST('2026-10-07 01:00:00.123' AS DATETIME(3)),INTERVAL n SECOND) create_time FROM seq),"
                        + "mes_pro_edhr_batch_execution AS (SELECT 1 id,1 tenant_id,0 deleted,10 status) ";
                var ids = selectIds(db,workFixture,"mes_pro_edhr_work_task",workPrefix.get(),false);
                assertEquals(java.util.stream.LongStream.rangeClosed(1,400).boxed().toList(),ids);
                var full = selectIds(db,workFixture,"mes_pro_edhr_work_task",work.approvalCenterQuery(new MesProEdhrWorkTaskPageReqVO(),12L,1L,false,"TODO"),true);
                assertEquals(805,full.size());
                assertEquals(full.subList(0,400),ids);
                explain(db,workFixture,"mes_pro_edhr_work_task",workPrefix.get());
                work.selectApprovalCenterPrefix(new MesProEdhrWorkTaskPageReqVO(),null,1L,true,"TODO",400);
                String doneFixture = "WITH mes_pro_edhr_work_task AS (SELECT 1 id,1 tenant_id,0 deleted,'APPROVE' task_type,'DONE' status,"
                        + "CAST('2020-01-01 01:00:00' AS DATETIME) create_time,CAST('2026-10-07 01:00:00' AS DATETIME) completed_at "
                        + "UNION ALL SELECT 2,1,0,'RELEASE_APPROVE','DONE','2026-10-07 01:00:00','2020-01-01 01:00:00' "
                        + "UNION ALL SELECT 3,1,0,'APPROVE','DONE','2027-01-01 01:00:00',NULL) ";
                assertEquals(List.of(1L,2L,3L),selectIds(db,doneFixture,"mes_pro_edhr_work_task",workPrefix.get(),false));

                String handoffFixture = "WITH mes_pro_process_pool_active_order AS (SELECT 7 id,1 tenant_id,0 deleted,'ACTIVE' active_status UNION ALL SELECT 8,1,0,'SUPERSEDED'),"
                        + "mes_active_order_handoff_task AS (SELECT 10 id,1 tenant_id,0 deleted,NULL notification_only,'12,112' candidate_user_snapshot,NULL completed_by,7 active_order_id,8 work_order_id,"
                        + "'TODO' status,'PRODUCTION_HANDOFF' task_type,'PQC_% Literal' reason,CAST('2026-10-07 01:00:00.123' AS DATETIME(3)) create_time "
                        + "UNION ALL SELECT 2,1,0,0,'12',NULL,7,8,'TODO','PRODUCTION_HANDOFF','PQC_% Literal','2026-10-07 01:00:00.123' "
                        + "UNION ALL SELECT 3,1,0,1,'12',NULL,7,8,'TODO','PRODUCTION_HANDOFF','PQC_% Literal','2026-10-07 01:00:00.123' "
                        + "UNION ALL SELECT 4,1,0,NULL,'112',NULL,7,8,'TODO','PRODUCTION_HANDOFF','PQC_% Literal','2026-10-07 01:00:00.123' "
                        + "UNION ALL SELECT 5,1,0,NULL,'12',NULL,8,8,'TODO','PRODUCTION_HANDOFF','PQC_% Literal','2026-10-07 01:00:00.123') ";
                AtomicReference<LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>> handoffPrefix = new AtomicReference<>();
                doReturn(0L).when(handoff).selectCount(any(Wrapper.class));
                doAnswer(call -> { handoffPrefix.set(call.getArgument(0)); return List.of(); }).when(handoff).selectList(any(Wrapper.class));
                handoff.selectApprovalCenterPrefix(1L,12L,false,false,"PQC_%",400);
                assertEquals(List.of(10L,2L),selectIds(db,handoffFixture,"mes_active_order_handoff_task",handoffPrefix.get(),false));
                assertEquals(List.of(),selectIds(db,handoffFixture,"mes_active_order_handoff_task",handoff.approvalCenterQuery(1L,12L,false,false,"pqc_%"),true));
                explain(db,handoffFixture,"mes_active_order_handoff_task",handoffPrefix.get());
                assertPhysicalSnapshot(db,connectionId);
                System.out.println("MYSQL_CTE_DIALECT_COUNTEREXAMPLES: K400/805, DONE inverse times/null-last, H current/token/notification/literal keyword/UTF8 tie; not actual business fixtures");
            } finally { db.rollback(); }
        } catch (SQLException failure) {
            // JDBC diagnostics may contain account/connection information; report only stable error codes.
            throw redactedSqlFailure(failure);
        }
    }

    @Test
    void binaryIdWindowMatchesIndependentUtf8BaselineAndCenterDecimalOrderOnReadOnlyCtes() throws Exception {
        try (Connection db = openLocalReadOnlySnapshot()) {
            try {
                long connectionId = assertPhysicalSnapshot(db, null);
                // Deliberately independent of both mapper expressions: old ordering remains the baseline oracle.
                String fixture = "WITH sort_fixture AS (SELECT CAST(2 AS SIGNED) id,"
                        + "CAST('2026-10-07 01:00:00' AS DATETIME) create_time,"
                        + "CAST('2026-10-07 02:00:00' AS DATETIME) completed_at"
                        + " UNION ALL SELECT 10,'2026-10-07 01:00:00','2026-10-07 02:00:00'"
                        + " UNION ALL SELECT 100,'2026-10-07 01:00:00','2026-10-07 02:00:00'"
                        + " UNION ALL SELECT 9223372036854775807,'2026-10-07 01:00:00','2026-10-07 02:00:00'"
                        + " UNION ALL SELECT 20,NULL,'2026-10-07 03:00:00'"
                        + " UNION ALL SELECT 200,'2026-10-07 03:00:00',NULL"
                        + " UNION ALL SELECT 3,NULL,NULL"
                        + " UNION ALL SELECT 30,'2026-10-07 02:00:00','2026-10-07 01:00:00') ";
                var created = LocalDateTime.of(2026, 10, 7, 1, 0);
                var completed = LocalDateTime.of(2026, 10, 7, 2, 0);
                var todo = List.of(new Identity("SORT", 2, created), new Identity("SORT", 10, created),
                        new Identity("SORT", 100, created), new Identity("SORT", Long.MAX_VALUE, created),
                        new Identity("SORT", 20, null), new Identity("SORT", 200, created.plusHours(2)),
                        new Identity("SORT", 3, null), new Identity("SORT", 30, created.plusHours(1)));
                var done = List.of(new Identity("SORT", 2, completed), new Identity("SORT", 10, completed),
                        new Identity("SORT", 100, completed), new Identity("SORT", Long.MAX_VALUE, completed),
                        new Identity("SORT", 20, completed.plusHours(1)), new Identity("SORT", 200, null),
                        new Identity("SORT", 3, null), new Identity("SORT", 30, completed.minusHours(1)));
                for (boolean finished : List.of(false, true)) {
                    String time = finished ? "completed_at" : "create_time";
                    var expected = (finished ? done : todo).stream().sorted(TUPLE).map(Identity::id).toList();
                    assertEquals(finished
                            ? List.of(20L,10L,100L,2L,Long.MAX_VALUE,30L,200L,3L)
                            : List.of(200L,30L,10L,100L,2L,Long.MAX_VALUE,20L,3L), expected,
                            "independent center decimal-string/null-last contract");
                    for (int limit : List.of(3, 400)) {
                        String prefix = fixture + "SELECT id FROM sort_fixture ORDER BY " + time
                                + " IS NULL ASC, " + time + " DESC, ";
                        String suffix = " ASC LIMIT " + limit;
                        var baseline = readRows(db, prefix + "CAST(id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_bin" + suffix)
                                .stream().map(row -> number(row, "id")).toList();
                        var binary = readRows(db, prefix + "CAST(id AS BINARY)" + suffix)
                                .stream().map(row -> number(row, "id")).toList();
                        var expectedPrefix = expected.subList(0, Math.min(limit, expected.size()));
                        assertEquals(expectedPrefix, baseline, "old utf8mb4 baseline must match center order");
                        assertEquals(expectedPrefix, binary, "BINARY must preserve center order and window");
                        assertEquals(baseline, binary, "old/new expressions must agree on real MySQL");
                    }
                }
                assertPhysicalSnapshot(db, connectionId);
            } finally { db.rollback(); }
        } catch (SQLException failure) { throw redactedSqlFailure(failure); }
    }

    @Test
    void defaultHandoffPrefixRejectsNoncanonicalTypesAndStatesOnFormalCiPadSpaceReadOnlyCtes() throws Exception {
        try (Connection db = openLocalReadOnlySnapshot()) {
            try {
                long connectionId = assertPhysicalSnapshot(db, null);
                var collation = readRows(db, "SELECT PAD_ATTRIBUTE pad_attribute FROM information_schema.COLLATIONS"
                        + " WHERE COLLATION_NAME='utf8mb4_unicode_ci'");
                assertEquals(1, collation.size(), "formal CI collation prerequisite missing or ambiguous");
                assertEquals("PAD SPACE", text(collation.get(0), "pad_attribute"));
                // Verify the CTE columns' actual collation and broad equality before testing the exact predicates.
                var control = readRows(db, "WITH ci_control AS (SELECT " + ciText("PRODUCTION_HANDOFF") + " task_type,"
                        + ciText("TODO") + " status," + ciText("ACTIVE") + " active_status)"
                        + " SELECT COLLATION(task_type) type_collation,COLLATION(status) status_collation,"
                        + "COLLATION(active_status) active_collation,"
                        + "task_type=" + ciText("production_handoff") + " type_case,task_type=" + ciText("PRODUCTION_HANDOFF ") + " type_space,"
                        + "status=" + ciText("todo") + " status_case,status=" + ciText("TODO ") + " status_space,"
                        + "active_status=" + ciText("active") + " active_case,active_status=" + ciText("ACTIVE ") + " active_space,"
                        + "CAST(task_type AS BINARY)=CAST(" + ciText("production_handoff") + " AS BINARY) binary_type_case,"
                        + "CAST(task_type AS BINARY)=CAST(" + ciText("PRODUCTION_HANDOFF ") + " AS BINARY) binary_type_space,"
                        + "CAST(status AS BINARY)=CAST(" + ciText("todo") + " AS BINARY) binary_status_case,"
                        + "CAST(status AS BINARY)=CAST(" + ciText("TODO ") + " AS BINARY) binary_status_space,"
                        + "CAST(active_status AS BINARY)=CAST(" + ciText("active") + " AS BINARY) binary_active_case,"
                        + "CAST(active_status AS BINARY)=CAST(" + ciText("ACTIVE ") + " AS BINARY) binary_active_space FROM ci_control");
                assertEquals(1, control.size());
                for (String column : List.of("type_collation", "status_collation", "active_collation"))
                    assertEquals("utf8mb4_unicode_ci", text(control.get(0), column));
                for (String column : List.of("type_case", "type_space", "status_case", "status_space", "active_case", "active_space")) {
                    assertEquals(1L, number(control.get(0), column));
                    assertEquals(0L, number(control.get(0), "binary_" + column));
                }
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "ci-handoff"), MesActiveOrderHandoffTaskDO.class);
                var formalTypes = MesActiveOrderHandoffContract.TYPES.stream().sorted().toList();
                for (boolean done : List.of(false, true)) for (boolean global : List.of(false, true)) {
                    String status = done ? "DONE" : "TODO";
                    var sentinels = List.of(ciRow(2, "PRODUCTION_HANDOFF", status, "ACTIVE"),
                            ciRow(10, "PQC_REVIEW", status, "ACTIVE").scope(1, 0, false, "12,112", 12L, 1, 0),
                            ciRow(20, "QA_REVIEW", status, "ACTIVE").times(null, null));
                    for (String type : formalTypes) {
                        for (String keyword : Arrays.asList(null, "absent-ci-keyword", "PQC_%\\Literal", "pqc_%\\Literal"))
                            checkCiPrefix(db, sentinels, ciRow(100, type, status, "ACTIVE"), global, done, keyword);
                        for (String invalid : List.of(type.toLowerCase(Locale.ROOT), type + " "))
                            for (String keyword : Arrays.asList(null, "absent-ci-keyword"))
                                checkCiPrefix(db, sentinels, ciRow(100, invalid, status, "ACTIVE"), global, done, keyword);
                    }
                    // NULL is the existing defensive predicate contract, not a claim of a legal NOT NULL table write.
                    for (String invalid : Arrays.asList("UNKNOWN", null))
                        for (String keyword : Arrays.asList(null, "absent-ci-keyword"))
                            checkCiPrefix(db, sentinels, ciRow(100, invalid, status, "ACTIVE"), global, done, keyword);
                    for (String invalidStatus : Arrays.asList("todo", "Todo", "TODO ", "done", "Done", "DONE ", null))
                        checkCiPrefix(db, sentinels, ciRow(100, "PRODUCTION_HANDOFF", invalidStatus, "ACTIVE"), global, done, null);
                    for (String active : Arrays.asList("ACTIVE", "active", "Active", "ACTIVE ", null))
                        checkCiPrefix(db, sentinels, ciRow(100, "PRODUCTION_HANDOFF", status, active), global, done, null);

                    var unknown = ciRow(100, "UNKNOWN", status, "ACTIVE");
                    for (var outside : List.of(
                            unknown.scope(2, 0, null, "12", 12L, 2, 0),
                            unknown.scope(1, 1, null, "12", 12L, 1, 0),
                            unknown.scope(1, 0, true, "12", 12L, 1, 0),
                            unknown.scope(1, 0, null, "112", 12L, 1, 0),
                            unknown.scope(1, 0, null, "12", 112L, 1, 0),
                            unknown.scope(1, 0, null, "12", 12L, 2, 0),
                            unknown.scope(1, 0, null, "12", 12L, 1, 1),
                            ciRow(100, "UNKNOWN", status, "SUPERSEDED"),
                            ciRow(100, "UNKNOWN", done ? "TODO" : "DONE", "ACTIVE")))
                        checkCiPrefix(db, sentinels, outside, global, done, "absent-ci-keyword");
                }
                assertPhysicalSnapshot(db, connectionId);
            } finally { db.rollback(); }
        } catch (SQLException failure) { throw redactedSqlFailure(failure); }
    }

    private record CiHandoffRow(long id, long tenant, int deleted, Boolean notification, String candidates,
                                Long completedBy, String status, String type, String active, long orderTenant,
                                int orderDeleted, LocalDateTime created, LocalDateTime completed) {
        CiHandoffRow scope(long tenant, int deleted, Boolean notification, String candidates, Long completedBy,
                           long orderTenant, int orderDeleted) {
            return new CiHandoffRow(id, tenant, deleted, notification, candidates, completedBy, status, type, active,
                    orderTenant, orderDeleted, created, completed);
        }
        CiHandoffRow times(LocalDateTime created, LocalDateTime completed) {
            return new CiHandoffRow(id, tenant, deleted, notification, candidates, completedBy, status, type, active,
                    orderTenant, orderDeleted, created, completed);
        }
    }

    private static CiHandoffRow ciRow(long id, String type, String status, String active) {
        return new CiHandoffRow(id, 1, 0, null, "12", 12L, status, type, active, 1, 0,
                LocalDateTime.of(2026, 10, 7, 1, 0), LocalDateTime.of(2026, 10, 7, 2, 0));
    }

    private void checkCiPrefix(Connection db, List<CiHandoffRow> sentinels, CiHandoffRow counterexample,
                               boolean global, boolean done, String keyword) throws SQLException {
        var rows = new ArrayList<>(sentinels); rows.add(counterexample);
        var eligible = rows.stream().filter(row -> row.tenant() == 1 && row.deleted() == 0
                && !Boolean.TRUE.equals(row.notification()) && (done ? "DONE" : "TODO").equals(row.status())
                && (global || (row.candidates() != null && Arrays.asList(row.candidates().split(",", -1)).contains("12")
                && (!done || Objects.equals(12L, row.completedBy()))))
                && (done || (row.orderTenant() == 1 && row.orderDeleted() == 0 && "ACTIVE".equals(row.active())))).toList();
        long invalid = eligible.stream().filter(row -> row.type() == null || !MesActiveOrderHandoffContract.TYPES.contains(row.type())).count();
        String fixture = ciFixture(rows);
        var mapper = mock(MesActiveOrderHandoffTaskMapper.class, Answers.CALLS_REAL_METHODS);
        var operations = new ArrayList<String>();
        var counts = new ArrayList<Long>();
        var countPredicates = new ArrayList<String>();
        doAnswer(call -> {
            LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO> query = call.getArgument(0);
            countPredicates.add(query.getSqlSegment());
            operations.add(counts.isEmpty() ? "INVALID_COUNT" : "TOTAL_COUNT");
            long result = countWithFixture(db, fixture, HANDOFF, query);
            counts.add(result);
            return result;
        }).when(mapper).selectCount(any(Wrapper.class));
        doAnswer(call -> {
            operations.add("PREFIX");
            return selectIds(db, fixture, HANDOFF, call.getArgument(0), false).stream()
                    .map(id -> new MesActiveOrderHandoffTaskDO().setId(id)).toList();
        }).when(mapper).selectList(any(Wrapper.class));
        if (invalid > 0) {
            var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> mapper.selectApprovalCenterPrefix(1L, 12L, global, done, keyword, 400));
            assertEquals(1_076_041_001, failure.getCode());
            assertEquals("未知交接类型", failure.getMessage());
            assertEquals(List.of("INVALID_COUNT"), operations);
            assertEquals(List.of(invalid), counts, "actual generated invalid-type SQL must detect the counterexample");
            verify(mapper, times(1)).selectCount(any(Wrapper.class));
            verify(mapper, never()).selectList(any(Wrapper.class));
        } else {
            var expected = eligible.stream().filter(row -> keyword == null || keyword.isBlank()
                    || (MesActiveOrderHandoffContract.label(row.type()) + " " + (row.id() + 1000) + " 8 PQC_%\\Literal")
                    .contains(keyword.trim()))
                    .map(row -> new Identity("HANDOFF", row.id(), done ? row.completed() : row.created()))
                    .sorted(TUPLE).map(Identity::id).toList();
            var page = mapper.selectApprovalCenterPrefix(1L, 12L, global, done, keyword, 400);
            assertEquals(List.of("INVALID_COUNT", "TOTAL_COUNT", "PREFIX"), operations);
            assertEquals(List.of(0L, (long) expected.size()), counts);
            assertEquals((long) expected.size(), page.getTotal().longValue());
            assertEquals(expected, page.getList().stream().map(MesActiveOrderHandoffTaskDO::getId).toList());
            verify(mapper, times(2)).selectCount(any(Wrapper.class));
            verify(mapper, times(1)).selectList(any(Wrapper.class));
            if (keyword == null) assertTrue(expected.containsAll(List.of(2L, 10L, 20L)), "normative sentinels must survive");
        }
        assertFalse(countPredicates.get(0).contains("LOCATE("), "the actual first count must precede keyword filtering");
    }

    private static String ciFixture(List<CiHandoffRow> rows) {
        String orders = rows.stream().map(row -> "SELECT " + (row.id() + 1000) + " id," + row.orderTenant()
                + " tenant_id," + row.orderDeleted() + " deleted," + ciText(row.active()) + " active_status")
                .collect(java.util.stream.Collectors.joining(" UNION ALL "));
        String tasks = rows.stream().map(row -> "SELECT " + row.id() + " id," + row.tenant() + " tenant_id,"
                + row.deleted() + " deleted," + (row.notification() == null ? "NULL" : row.notification() ? "1" : "0")
                + " notification_only," + ciText(row.candidates()) + " candidate_user_snapshot," + row.completedBy()
                + " completed_by," + (row.id() + 1000) + " active_order_id,8 work_order_id," + ciText(row.status())
                + " status," + ciText(row.type()) + " task_type," + ciText("PQC_%\\Literal") + " reason,"
                + ciTime(row.created()) + " create_time," + ciTime(row.completed()) + " completed_at")
                .collect(java.util.stream.Collectors.joining(" UNION ALL "));
        return "WITH mes_pro_process_pool_active_order AS (" + orders + "),mes_active_order_handoff_task AS (" + tasks + ") ";
    }

    private static String ciText(String value) {
        // Fixed test strings encoded as UTF-8 hex retain backslashes and spaces independently of SQL escape modes.
        return "CONVERT(" + (value == null ? "NULL" : "0x" + HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8)))
                + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private static String ciTime(LocalDateTime value) {
        return value == null ? "CAST(NULL AS DATETIME)" : "CAST('" + value.toString().replace('T', ' ') + "' AS DATETIME)";
    }

    private long countWithFixture(Connection db, String fixture, String table, LambdaQueryWrapperX<?> query) throws SQLException {
        try (PreparedStatement sql = statement(db, fixture + "SELECT COUNT(*) FROM " + table + " WHERE ", query.getSqlSegment(), query);
             ResultSet row = sql.executeQuery()) { assertTrue(row.next()); return row.getLong(1); }
    }

    private record Identity(String type, long id, LocalDateTime time) { }

    private static final Comparator<Identity> TUPLE = Comparator.comparing(Identity::time,
            Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Identity::type)
            .thenComparing(identity -> Long.toString(identity.id())); // Numeric IDs contain UTF-8 ASCII only.

    private void checkActualCase(Connection db,Map<String,Map<Long,Map<String,Object>>> facts,
                                 MesProEdhrWorkTaskMapper work,MesActiveOrderHandoffTaskMapper handoff,
                                 long tenant,long actor,boolean global,boolean done,String status,
                                 MesProEdhrWorkTaskPageReqVO filter,String keyword) throws SQLException {
        Set<Long> matchingBatchCodes = null;
        if (filter.getBatchCode() != null) {
            matchingBatchCodes = new HashSet<>();
            for (var row : readRows(db,"SELECT id FROM " + WORK + " WHERE tenant_id=? AND deleted=0 AND batch_code LIKE ?",
                    tenant,"%"+filter.getBatchCode()+"%")) matchingBatchCodes.add(number(row,"id"));
        }
        var expectedWork = new ArrayList<Identity>();
        for (var row : facts.get(WORK).values()) {
            if (matchingBatchCodes != null && !matchingBatchCodes.contains(number(row,"id"))) continue;
            if (visibleWork(row,facts,actor,global,done,status,filter))
                expectedWork.add(identity(row,"EDHR_WORK_TASK",done));
        }
        var expectedHandoff = new ArrayList<Identity>();
        for (var row : facts.get(HANDOFF).values()) {
            if (!visibleHandoff(row,facts,actor,global,done)) continue;
            assertTrue(MesActiveOrderHandoffContract.TYPES.contains(text(row,"task_type")),
                    "MYSQL_GATE_UNKNOWN_ELIGIBLE_HANDOFF_TYPE");
            if (keyword != null) {
                String searchable = MesActiveOrderHandoffContract.label(text(row,"task_type"))+" "
                        +String.valueOf(row.get("active_order_id"))+" "+String.valueOf(row.get("work_order_id"))
                        +" "+String.valueOf(row.get("reason"));
                if (!searchable.contains(keyword.trim())) continue;
            }
            expectedHandoff.add(identity(row,"ACTIVE_ORDER_HANDOFF",done));
        }
        expectedWork.sort(TUPLE); expectedHandoff.sort(TUPLE);
        var expected = new ArrayList<>(expectedWork); expected.addAll(expectedHandoff); expected.sort(TUPLE);
        String caseName = (done?"DONE":"TODO/"+status)+"/"+(global?"global":"person")
                +(keyword == null?"":"/H-keyword")+(filter.getBatchCode()==null?"":"/batch-keyword")
                +(filter.getNodeTypes()==null?"":"/candidate-node");
        var observed = new ArrayList<Identity>();
        int pages = Math.max(2,(expected.size()+199)/200+1); // Includes K=400 and the empty page after the last row.
        for (int page=1;page<=pages;page++) {
            long k = Math.max(400L,Math.multiplyExact((long)page,200L));
            var actualWork = work.selectApprovalCenterPrefix(filter,global?null:actor,tenant,done,status,k);
            var actualHandoff = handoff.selectApprovalCenterPrefix(tenant,actor,global,done,keyword,k);
            assertEquals((long)expectedWork.size(),actualWork.getTotal(),caseName+" work count");
            assertEquals((long)expectedHandoff.size(),actualHandoff.getTotal(),caseName+" handoff count");
            assertEquals(expectedWork.subList(0,(int)Math.min(k,expectedWork.size())).stream().map(Identity::id).toList(),
                    actualWork.getList().stream().map(MesProEdhrWorkTaskDO::getId).toList(),caseName+" work prefix K="+k);
            assertEquals(expectedHandoff.subList(0,(int)Math.min(k,expectedHandoff.size())).stream().map(Identity::id).toList(),
                    actualHandoff.getList().stream().map(MesActiveOrderHandoffTaskDO::getId).toList(),caseName+" H prefix K="+k);
            var merged = new ArrayList<Identity>();
            for (var row : actualWork.getList()) merged.add(identity(facts.get(WORK).get(row.getId()),"EDHR_WORK_TASK",done));
            for (var row : actualHandoff.getList()) merged.add(identity(facts.get(HANDOFF).get(row.getId()),"ACTIVE_ORDER_HANDOFF",done));
            merged.sort(TUPLE);
            int from = (int)Math.min((long)(page-1)*200,merged.size()), to = (int)Math.min((long)page*200,merged.size());
            var actualPage = merged.subList(from,to);
            int oracleFrom = Math.min((page-1)*200,expected.size()), oracleTo = Math.min(page*200,expected.size());
            assertEquals(expected.subList(oracleFrom,oracleTo),actualPage,caseName+" page="+page);
            observed.addAll(actualPage);
        }
        assertEquals(expected,observed,caseName+" concatenated authorized pages");
        assertEquals(observed.size(),new HashSet<>(observed).size(),caseName+" identities must not repeat");
        var workQuery = work.approvalCenterQuery(filter,global?null:actor,tenant,done,status);
        var handoffQuery = handoff.approvalCenterQuery(tenant,actor,global,done,keyword);
        explainActual(db,WORK,workQuery,done,caseName);
        explainActual(db,HANDOFF,handoffQuery,done,caseName);
        assertPhysicalSnapshot(db,null);
        System.out.println("MYSQL_ACTUAL_CASE: "+caseName+", work="+expectedWork.size()+", H="+expectedHandoff.size()
                +", pages="+pages+", minimumK=400; zero matches do not prove an absent boundary fixture");
    }

    private static boolean visibleWork(Map<String,Object> row,Map<String,Map<Long,Map<String,Object>>> facts,
                                       long actor,boolean global,boolean done,String status,MesProEdhrWorkTaskPageReqVO filter) {
        String type = text(row,"task_type");
        if (!Objects.equals(text(row,"status"),done?"DONE":status)) return false;
        if (done) {
            if (!Set.of("REVIEW","APPROVE","RELEASE_APPROVE","PQC_PRODUCTION_RELEASE").contains(type)) return false;
            return global || (!"PQC_PRODUCTION_RELEASE".equals(type) && equal(row.get("assignee_user_id"),actor))
                    || formalReleaseActor(row,facts,actor) || formalPqcActor(row,facts,actor) || formalQaActor(row,facts,actor);
        }
        var batch = facts.get("mes_pro_edhr_batch_execution").get(nullableNumber(row,"batch_execution_id"));
        long batchStatus = batch == null ? -1 : number(batch,"status");
        boolean open = !Set.of(30L,40L,50L,60L).contains(batchStatus);
        boolean assigned = Set.of("REVIEW","APPROVE","RELEASE_APPROVE").contains(type)
                && (global || equal(row.get("assignee_user_id"),actor)) && open;
        String snapshot = text(row,"candidate_user_snapshot");
        boolean candidate = global || (snapshot != null && (","+snapshot+",").contains(","+actor+","));
        candidate &= open || ("ARCHIVE".equals(type) && !Set.of(40L,50L,60L).contains(batchStatus));
        if (filter.getNodeTypes()!=null && !filter.getNodeTypes().isEmpty()) {
            var node = facts.get("mes_pro_edhr_batch_execution_task").get(nullableNumber(row,"batch_task_id"));
            candidate &= MesProEdhrWorkTaskMapper.PRODUCTION_RELEASE_REPORT_NODE_TYPES.containsAll(filter.getNodeTypes())
                    && node!=null && filter.getNodeTypes().contains(text(node,"node_type"));
        }
        return assigned || candidate;
    }

    private static boolean visibleHandoff(Map<String,Object> row,Map<String,Map<Long,Map<String,Object>>> facts,
                                          long actor,boolean global,boolean done) {
        if (!Objects.equals(text(row,"status"),done?"DONE":"TODO") || Boolean.TRUE.equals(row.get("notification_only"))) return false;
        if (!global) {
            String candidates = text(row,"candidate_user_snapshot");
            if (candidates==null || !Arrays.asList(candidates.split(",",-1)).contains(Long.toString(actor))) return false;
            if (done && !equal(row.get("completed_by"),actor)) return false;
        }
        if (!done) {
            var active = facts.get("mes_pro_process_pool_active_order").get(nullableNumber(row,"active_order_id"));
            return active!=null && "ACTIVE".equals(text(active,"active_status"));
        }
        return true;
    }

    private static boolean formalReleaseActor(Map<String,Object> task,Map<String,Map<Long,Map<String,Object>>> facts,long actor) {
        if (!"RELEASE_APPROVE".equals(text(task,"task_type")) || !"RELEASE_TRANSACTION".equals(text(task,"business_scope_type"))) return false;
        var release = facts.get("mes_pro_edhr_release_transaction").get(nullableNumber(task,"business_scope_id"));
        return release!=null && equal(release.get("batch_execution_id"),task.get("batch_execution_id"))
                && equal(release.get("work_order_id"),task.get("work_order_id"))
                && "RELEASED".equals(text(release,"release_status")) && equal(release.get("approved_by"),actor);
    }

    private static Map<String,Object> formalApplication(Map<String,Object> task,Map<String,Map<Long,Map<String,Object>>> facts) {
        if (!"PQC_PRODUCTION_RELEASE".equals(text(task,"task_type")) || !"RELEASE_APPLICATION".equals(text(task,"business_scope_type"))) return null;
        var application = facts.get("mes_pro_process_pool_active_order_release_application").get(nullableNumber(task,"business_scope_id"));
        if (application==null || !equal(application.get("pqc_release_work_task_id"),task.get("id"))
                || !equal(application.get("work_order_id"),task.get("work_order_id"))
                || (task.get("batch_execution_id")!=null && !equal(task.get("batch_execution_id"),application.get("batch_execution_id")))) return null;
        return application;
    }

    private static boolean formalPqcActor(Map<String,Object> task,Map<String,Map<Long,Map<String,Object>>> facts,long actor) {
        var application = formalApplication(task,facts);
        if (application==null || !positive(application,"active_order_id") || !positive(application,"batch_execution_id")
                || !equal(application.get("pqc_decided_by"),actor) || !historicalActor(facts,actor)
                || !equal(application.get("pqc_decided_at"),task.get("completed_at"))
                || !equal(application.get("pqc_decision"),task.get("reason"))) return false;
        String state = text(application,"application_status"), decision = text(application,"pqc_decision");
        boolean approved = "APPROVE".equals(decision) && Set.of("REPORT_UPLOAD_PENDING","MANAGER_RELEASE_PENDING","RELEASED","NONCONFORMANCE_REWORK","NONCONFORMANCE_VOID").contains(state);
        String rejection = text(application,"pqc_reject_reason");
        boolean rejected = "REJECT".equals(decision) && Set.of("PQC_RELEASE_REJECTED","NONCONFORMANCE_REWORK","NONCONFORMANCE_VOID").contains(state) && rejection!=null && !rejection.trim().isEmpty();
        if (!approved && !rejected) return false;
        if (!Set.of("NONCONFORMANCE_REWORK","NONCONFORMANCE_VOID").contains(state)) return true;
        var review = facts.get("mes_pro_edhr_nonconformance_review").get(nullableNumber(application,"qa_closure_review_id"));
        return matchingClosure(application,review) && review.get("closed_at")!=null && positive(review,"qa_user_id");
    }

    private static boolean formalQaActor(Map<String,Object> task,Map<String,Map<Long,Map<String,Object>>> facts,long actor) {
        var application = formalApplication(task,facts);
        if (application==null || !"EDHR_NONCONFORMANCE_REVIEW".equals(text(task,"review_source_type"))) return false;
        var review = facts.get("mes_pro_edhr_nonconformance_review").get(nullableNumber(task,"review_source_id"));
        return review!=null && equal(application.get("qa_closure_review_id"),review.get("id"))
                && matchingClosure(application,review) && equal(review.get("qa_user_id"),actor) && historicalActor(facts,actor)
                && application.get("pqc_decision")==null && application.get("pqc_decided_by")==null && application.get("pqc_decided_at")==null
                && equal(review.get("closed_at"),task.get("completed_at")) && equal(application.get("application_status"),task.get("reason"));
    }

    private static boolean matchingClosure(Map<String,Object> application,Map<String,Object> review) {
        if (review==null || !"closed".equals(text(review,"review_status"))
                || !equal(review.get("active_order_id"),application.get("active_order_id"))
                || !equal(review.get("work_order_id"),application.get("work_order_id"))
                || (review.get("batch_execution_id")!=null && !equal(review.get("batch_execution_id"),application.get("batch_execution_id")))) return false;
        return ("rework".equals(text(review,"disposition")) && "NONCONFORMANCE_REWORK".equals(text(application,"application_status")))
                || ("void".equals(text(review,"disposition")) && "NONCONFORMANCE_VOID".equals(text(application,"application_status")));
    }

    private static boolean historicalActor(Map<String,Map<Long,Map<String,Object>>> facts,long actor) {
        var user = facts.get("system_users").get(actor);
        // facts are already constrained to the exact tenant and deleted=0; status is a current writing prerequisite.
        return user!=null;
    }

    private static Identity identity(Map<String,Object> row,String type,boolean done) {
        Object time = row.get(done?"completed_at":"create_time");
        return new Identity(type,number(row,"id"),time==null?null:((Timestamp)time).toLocalDateTime());
    }

    private static boolean equal(Object left,Object right) {
        if (left==null || right==null) return false; // SQL equality does not equate two NULLs.
        if (left instanceof Number a && right instanceof Number b) return a.longValue()==b.longValue();
        return left.equals(right);
    }
    private static String text(Map<String,Object> row,String field) { Object value=row.get(field); return value==null?null:value.toString(); }
    private static Long nullableNumber(Map<String,Object> row,String field) { Object value=row.get(field); return value==null?null:((Number)value).longValue(); }
    private static long number(Map<String,Object> row,String field) { return Objects.requireNonNull(nullableNumber(row,field),"MYSQL_GATE_REQUIRED_NUMERIC_FACT_MISSING: "+field); }
    private static boolean positive(Map<String,Object> row,String field) { Long value=nullableNumber(row,field); return value!=null && value>0; }

    private static Connection openLocalReadOnlySnapshot() throws SQLException {
        String url=required("EDHR_MYSQL_JDBC_URL");
        URI endpoint;
        try {
            if (!url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
            endpoint=URI.create(url.substring("jdbc:".length()));
        } catch (RuntimeException invalid) { throw new IllegalStateException("MYSQL_GATE_INVALID_LOCAL_URL"); }
        assertTrue(Set.of("localhost","127.0.0.1","::1","[::1]").contains(endpoint.getHost()), "authorized local host required");
        assertNull(endpoint.getUserInfo(),"credentials must stay in private environment inputs");
        assertFalse(url.toLowerCase(Locale.ROOT).matches(".*[?&](user|password)=.*"),"credentials must stay outside JDBC URL");
        Connection db=DriverManager.getConnection(url,required("EDHR_MYSQL_USER"),required("EDHR_MYSQL_PASSWORD"));
        try {
            assertEquals("MySQL",db.getMetaData().getDatabaseProductName(),"H2 cannot substitute for MySQL");
            assertTrue(db.getMetaData().getDatabaseMajorVersion()>=8,"MySQL8 required");
            db.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            db.setReadOnly(true); db.setAutoCommit(false);
            return db;
        } catch (SQLException | RuntimeException | Error failure) {
            try { db.close(); } catch (SQLException close) { failure.addSuppressed(redactedSqlFailure(close)); }
            throw failure;
        }
    }

    private static AssertionError redactedSqlFailure(SQLException failure) {
        return new AssertionError("MYSQL_READ_ONLY_GATE_SQL_ERROR: state="+failure.getSQLState()+", code="+failure.getErrorCode());
    }

    private static long assertPhysicalSnapshot(Connection db,Long expectedConnection) throws SQLException {
        assertTrue(db.isReadOnly()); assertFalse(db.getAutoCommit());
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ,db.getTransactionIsolation());
        try (PreparedStatement sql=db.prepareStatement("SELECT CONNECTION_ID(),@@session.transaction_isolation,@@session.transaction_read_only");
             ResultSet row=sql.executeQuery()) {
            assertTrue(row.next()); long id=row.getLong(1);
            assertEquals("REPEATABLE-READ",row.getString(2)); assertEquals(1,row.getInt(3),"server read-only session required");
            if (expectedConnection!=null) assertEquals(expectedConnection.longValue(),id,"oracle/count/prefix must use same physical connection");
            return id;
        }
    }

    private static void requireSchema(Connection db) throws SQLException {
        for (var entry : SCHEMA.entrySet()) {
            var columns=new HashSet<String>();
            try (PreparedStatement sql=db.prepareStatement("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                sql.setString(1,entry.getKey());
                try (ResultSet rows=sql.executeQuery()) { while(rows.next()) columns.add(rows.getString(1)); }
            }
            for (String required : entry.getValue().split(",")) assertTrue(columns.contains(required),
                    "MYSQL_GATE_SCHEMA_COLUMN_MISSING: "+entry.getKey()+"."+required);
            try (PreparedStatement sql=db.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                sql.setString(1,entry.getKey());
                try (ResultSet row=sql.executeQuery()) {
                    assertTrue(row.next(),"MYSQL_GATE_TABLE_MISSING: "+entry.getKey());
                    assertEquals("InnoDB",row.getString(1),"snapshot requires transactional formal tables: "+entry.getKey());
                }
            }
        }
        // Independent pre-repair character/collation baseline; final production prefixes use CAST(id AS BINARY).
        try (PreparedStatement sql=db.prepareStatement("SELECT CAST(10 AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_bin < CAST(2 AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_bin");
             ResultSet row=sql.executeQuery()) { assertTrue(row.next()); assertTrue(row.getBoolean(1),"UTF8 numeric ID string tie required"); }
    }

    private static List<Map<String,Object>> readRows(Connection db,String query,Object... values) throws SQLException {
        try (PreparedStatement sql=db.prepareStatement(query)) {
            for (int i=0;i<values.length;i++) sql.setObject(i+1,values[i]);
            var result=new ArrayList<Map<String,Object>>();
            try (ResultSet rows=sql.executeQuery()) {
                ResultSetMetaData metadata=rows.getMetaData();
                while (rows.next()) {
                    var row=new HashMap<String,Object>();
                    for (int column=1;column<=metadata.getColumnCount();column++) {
                        Object value=rows.getObject(column);
                        if (value!=null && metadata.getColumnType(column)==Types.TIMESTAMP) value=rows.getTimestamp(column);
                        if (value!=null && (metadata.getColumnType(column)==Types.BIT || metadata.getColumnType(column)==Types.BOOLEAN)) value=rows.getBoolean(column);
                        row.put(metadata.getColumnLabel(column),value);
                    }
                    result.add(row);
                }
            }
            return result;
        }
    }

    private long count(Connection db,String table,LambdaQueryWrapperX<?> query) throws SQLException {
        try (PreparedStatement sql=statement(db,"SELECT COUNT(*) FROM "+table+" WHERE ",query.getSqlSegment(),query);
             ResultSet row=sql.executeQuery()) { assertTrue(row.next()); return row.getLong(1); }
    }

    private void explainActual(Connection db,String table,LambdaQueryWrapperX<?> query,boolean done,String caseName) throws SQLException {
        recordPlan(db,table,query,"EXPLAIN SELECT COUNT(*) FROM "+table+" WHERE ",query.getSqlSegment(),caseName+"/count");
        String time=done?"completed_at":"create_time";
        recordPlan(db,table,query,"EXPLAIN SELECT id FROM "+table+" WHERE ",query.getSqlSegment()
                +" ORDER BY "+time+" IS NULL ASC,"+time+" DESC,CAST(id AS BINARY) ASC LIMIT 400",caseName+"/K400");
    }

    private void recordPlan(Connection db,String table,LambdaQueryWrapperX<?> query,String prefix,String segment,String caseName) throws SQLException {
        try (PreparedStatement sql=statement(db,prefix,segment,query); ResultSet plan=sql.executeQuery()) {
            int rows=0;
            while (plan.next()) {
                rows++;
                System.out.println("MYSQL_ACTUAL_EXPLAIN: source="+table+", case="+caseName+", table="+plan.getString("table")
                        +", access="+plan.getString("type")+", key="+plan.getString("key")+", rows="+plan.getObject("rows")
                        +", filtered="+plan.getObject("filtered")+", extra="+plan.getString("Extra"));
            }
            assertTrue(rows>0,"actual-table EXPLAIN must produce a plan; no performance claim follows merely from a plan");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("MYSQL_READ_ONLY_GATE_PREREQUISITE_MISSING: " + name);
        return value;
    }

    private List<Long> selectIds(Connection db,String fixture,String table,LambdaQueryWrapperX<?> query,boolean orderFull) throws SQLException {
        String segment = query.getSqlSegment();
        // Full-list oracle deliberately keeps the independent pre-repair character/collation expression.
        if (orderFull) segment += " ORDER BY create_time IS NULL ASC,create_time DESC,CAST(id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_bin ASC";
        try (PreparedStatement sql = statement(db,fixture + "SELECT id FROM " + table + " WHERE ",segment,query)) {
            var result = new ArrayList<Long>();
            try (ResultSet rows = sql.executeQuery()) { while(rows.next()) result.add(rows.getLong(1)); }
            return result;
        }
    }

    private void explain(Connection db,String fixture,String table,LambdaQueryWrapperX<?> query) throws SQLException {
        try (PreparedStatement sql = statement(db,"EXPLAIN " + fixture + "SELECT id FROM " + table + " WHERE ",query.getSqlSegment(),query);
             ResultSet plan = sql.executeQuery()) { assertTrue(plan.next(),"EXPLAIN must return a plan"); }
    }

    private PreparedStatement statement(Connection db,String prefix,String segment,LambdaQueryWrapperX<?> query) throws SQLException {
        var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}").matcher(segment);
        var values = new ArrayList<Object>();
        while(matcher.find()) values.add(query.getParamNameValuePairs().get(matcher.group(1)));
        var statement = db.prepareStatement(prefix + matcher.replaceAll("?"));
        for(int i=0;i<values.size();i++) statement.setObject(i+1,values.get(i));
        return statement;
    }
}
