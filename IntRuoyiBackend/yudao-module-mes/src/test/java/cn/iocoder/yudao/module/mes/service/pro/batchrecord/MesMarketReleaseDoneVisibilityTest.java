package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesMarketReleaseDoneVisibilityTest extends BaseDbUnitTest {
    @Resource
    private MesProEdhrWorkTaskMapper tasks;
    @Resource
    private MesProEdhrReleaseTransactionMapper transactions;
    @Resource
    private DataSource dataSource;
    @Resource private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void clearTenant() {
        jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE request_idempotency_key='PQC-done-request'");
        jdbc.update("DELETE FROM system_users WHERE id=346 AND username='pqc346'");
        jdbc.update("DELETE FROM system_users WHERE id=345 AND username='qa345-byte'");
        TenantContextHolder.clear();
    }

    @Test
    void actualSignerSeesCompletedMarketTaskWithoutReassigningFrozenOwner() {
        var task = completedMarketTask(releasedTransaction(347L));
        assertEquals(List.of(task.getId()), tasks.selectApprovalCenterDonePage(request(), 347L)
                .getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertEquals(List.of(task.getId()), tasks.selectDonePage(request(), 347L)
                .getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertEquals(1L, tasks.countMy(347L, "RELEASE_APPROVE", "DONE"));
        assertEquals(1L, tasks.selectById(task.getId()).getAssigneeUserId());
        assertEquals("1,347,348", tasks.selectById(task.getId()).getCandidateUserSnapshot());
    }

    @Test
    void unusedCandidateCannotSeeSomeoneElsesCompletedTask() {
        completedMarketTask(releasedTransaction(347L));
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 348L).getTotal());
        assertEquals(0L, tasks.selectDonePage(request(), 348L).getTotal());
        assertEquals(0L, tasks.countMy(348L, "RELEASE_APPROVE", "DONE"));
    }

    @Test
    void assignedOwnerAndGlobalViewKeepExistingVisibility() {
        completedMarketTask(releasedTransaction(347L));
        assertEquals(1L, tasks.selectApprovalCenterDonePage(request(), 1L).getTotal());
        assertEquals(1L, tasks.selectApprovalCenterDonePage(request(), null).getTotal());
    }

    @Test
    void pendingTransactionCannotManufactureCompletedSignerVisibility() {
        var transaction = releasedTransaction(347L).setReleaseStatus("APPROVING");
        transactions.updateById(transaction);
        completedMarketTask(transaction);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void deletedTransactionCannotGrantVisibility() {
        var transaction = releasedTransaction(347L);
        completedMarketTask(transaction);
        transactions.deleteById(transaction.getId());
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void wrongBusinessScopeCannotUseMarketApprovalIdentity() {
        var task = completedMarketTask(releasedTransaction(347L)).setBusinessScopeType("BATCH_TASK");
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void differentBatchCannotUseMarketApprovalIdentity() {
        var task = completedMarketTask(releasedTransaction(347L)).setBatchExecutionId(99L);
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void differentWorkOrderCannotUseMarketApprovalIdentity() {
        var task = completedMarketTask(releasedTransaction(347L)).setWorkOrderId(88L);
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void otherTenantTransactionCannotGrantVisibility() {
        TenantContextHolder.setTenantId(2L);
        var transaction = releasedTransaction(347L);
        TenantContextHolder.setTenantId(1L);
        var task = completedMarketTask(transaction);
        assertEquals(2L, jdbc.queryForObject("SELECT tenant_id FROM mes_pro_edhr_release_transaction WHERE id = ?", Long.class, transaction.getId()));
        assertEquals(1L, jdbc.queryForObject("SELECT tenant_id FROM mes_pro_edhr_work_task WHERE id = ?", Long.class, task.getId()));
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void anotherTransactionIdentityCannotGrantVisibility() {
        var mine = releasedTransaction(347L);
        var someoneElses = releasedTransaction(348L);
        var task = completedMarketTask(mine).setBusinessScopeId(someoneElses.getId());
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void reviewTaskCannotBorrowMarketApprovalIdentity() {
        var task = completedMarketTask(releasedTransaction(347L)).setTaskType("REVIEW");
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 347L).getTotal());
    }

    @Test
    void signingCompletedTaskDoesNotGrantPendingTaskVisibility() {
        var task = completedMarketTask(releasedTransaction(347L)).setStatus("TODO");
        tasks.updateById(task);
        assertEquals(0L, tasks.selectApprovalCenterTodoPage(request(), 347L, "TODO").getTotal());
        assertEquals(0L, tasks.countMy(347L, "RELEASE_APPROVE", "DONE"));
    }

    @Test
    void unspecifiedCountStatusKeepsOriginalAssignedOwnerScope() {
        completedMarketTask(releasedTransaction(347L));
        assertEquals(0L, tasks.countMy(347L, "RELEASE_APPROVE", null));
        assertEquals(1L, tasks.countMy(1L, "RELEASE_APPROVE", null));
    }

    @Test
    void filtersAndOrdinaryAssignedReviewTasksRemainEffective() {
        completedMarketTask(releasedTransaction(347L));
        var review = new MesProEdhrWorkTaskDO().setTaskCode("REVIEW-347").setTaskType("REVIEW")
                .setBusinessScopeType("BATCH_TASK").setBusinessScopeId(100L)
                .setAssigneeUserId(347L).setStatus("DONE").setActionUrl("/review")
                .setCompletedAt(LocalDateTime.now());
        tasks.insert(review);
        var filtered = request();
        filtered.setTaskType("REVIEW");
        assertEquals(List.of(review.getId()), tasks.selectApprovalCenterDonePage(filtered, 347L)
                .getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        filtered.setTaskType("RELEASE_APPROVE");
        filtered.setWorkOrderCode("UNRELATED");
        assertEquals(0L, tasks.selectApprovalCenterDonePage(filtered, 347L).getTotal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVE", "REJECT"})
    void actualPqcDeciderSeesOwnDoneInBothListsWithoutReassigningOwner(String decision) {
        var task = completedPqcTask(decision);
        assertEquals(List.of(task.getId()), tasks.selectApprovalCenterDonePage(request(), 346L)
                .getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertEquals(List.of(task.getId()), tasks.selectDonePage(request(), 346L)
                .getList().stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertEquals(1L, tasks.countMy(346L, "PQC_PRODUCTION_RELEASE", "DONE"));
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 1L).getTotal(), "unused assigned owner is not the PQC decider");
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 348L).getTotal(), "unused candidate is not the PQC decider");
        assertEquals(0L, tasks.selectMyPage(request(), 346L, "TODO").getTotal());
        assertEquals(1L, tasks.selectApprovalCenterDonePage(request(), null).getTotal());
        assertEquals(1L, tasks.selectById(task.getId()).getAssigneeUserId());
        assertEquals("1,346,348", tasks.selectById(task.getId()).getCandidateUserSnapshot());
    }

    @ParameterizedTest(name = "{0} cannot grant completed PQC visibility")
    @ValueSource(strings = {"applicationTenant", "taskTenant", "applicationDeleted", "currentTaskFk", "applicationOrder",
            "taskOrder", "taskScope", "taskApplication", "taskType", "taskStatus", "taskBatch", "applicationBatch",
            "applicationActive", "decision", "pending", "nullActor", "zeroActor", "otherActor", "nullTime", "otherTime",
            "taskReason", "userMissing", "userDisabled", "userTenant", "userDeleted"})
    void pqcDoneRequiresExactCurrentApplicationAndEnabledActualUser(String broken) {
        var task = completedPqcTask("APPROVE");
        Long applicationId = task.getBusinessScopeId();
        switch (broken) {
            case "applicationTenant" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id=2 WHERE id=?", applicationId);
            case "taskTenant" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id=2 WHERE id=?", task.getId());
            case "applicationDeleted" -> applications.deleteById(applicationId);
            case "currentTaskFk" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_release_work_task_id=? WHERE id=?", task.getId()+1, applicationId);
            case "applicationOrder" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET work_order_id=88 WHERE id=?", applicationId);
            case "taskOrder" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET work_order_id=88 WHERE id=?", task.getId());
            case "taskScope" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_type='BATCH_EXECUTION' WHERE id=?", task.getId());
            case "taskApplication" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET business_scope_id=? WHERE id=?", applicationId+1, task.getId());
            case "taskType" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET task_type='REVIEW' WHERE id=?", task.getId());
            case "taskStatus" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET status='TODO' WHERE id=?", task.getId());
            case "taskBatch" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET batch_execution_id=99 WHERE id=?", task.getId());
            case "applicationBatch" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET batch_execution_id=NULL WHERE id=?", applicationId);
            case "applicationActive" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET active_order_id=0 WHERE id=?", applicationId);
            case "decision" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decision='NONCONFORMANCE_REWORK' WHERE id=?", applicationId);
            case "pending" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status='PQC_RELEASE_PENDING' WHERE id=?", applicationId);
            case "nullActor" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_by=NULL WHERE id=?", applicationId);
            case "zeroActor" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_by=0 WHERE id=?", applicationId);
            case "otherActor" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_by=348 WHERE id=?", applicationId);
            case "nullTime" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_at=NULL WHERE id=?", applicationId);
            case "otherTime" -> jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET pqc_decided_at=? WHERE id=?", task.getCompletedAt().plusSeconds(1), applicationId);
            case "taskReason" -> jdbc.update("UPDATE mes_pro_edhr_work_task SET reason='REJECT' WHERE id=?", task.getId());
            case "userMissing" -> jdbc.update("DELETE FROM system_users WHERE id=346");
            case "userDisabled" -> jdbc.update("UPDATE system_users SET status=1 WHERE id=346");
            case "userTenant" -> jdbc.update("UPDATE system_users SET tenant_id=2 WHERE id=346");
            case "userDeleted" -> jdbc.update("UPDATE system_users SET deleted=TRUE WHERE id=346");
            default -> throw new AssertionError(broken);
        }
        assertEquals(0L, tasks.selectApprovalCenterDonePage(request(), 346L).getTotal());
        assertEquals(0L, tasks.selectDonePage(request(), 346L).getTotal());
        assertEquals(0L, tasks.countMy(346L, "PQC_PRODUCTION_RELEASE", "DONE"));
        if (!"taskType".equals(broken)) {
            assertEquals(0L, tasks.selectDonePage(request(), 1L).getTotal(), "invalid PQC facts cannot fall back to its original owner");
        }
    }

    @Test
    void pqcDoneFilteringAndPaginationUseFormalTaskRowsOnce() {
        var task = completedPqcTask("APPROVE");
        var filter = request(); filter.setTaskType("PQC_PRODUCTION_RELEASE");
        assertEquals(1L, tasks.selectApprovalCenterDonePage(filter, 346L).getTotal());
        filter.setPageSize(1); filter.setPageNo(2);
        assertEquals(1L, tasks.selectApprovalCenterDonePage(filter, 346L).getTotal());
        assertEquals(List.of(), tasks.selectApprovalCenterDonePage(filter, 346L).getList());
        filter.setPageNo(1); filter.setWorkOrderCode("UNRELATED");
        assertEquals(0L, tasks.selectApprovalCenterDonePage(filter, 346L).getTotal());
        assertEquals("DONE", tasks.selectById(task.getId()).getStatus());
    }

    @ParameterizedTest(name = "formal {0} must byte-match task reason [{1}]")
    @CsvSource({"APPROVE,APPROVE,1", "REJECT,REJECT,1", "APPROVE,approve,0", "APPROVE,Approve,0",
            "APPROVE,APPROVE_extra,0", "APPROVE,APPROVX,0", "APPROVE,' APPROVE',0", "APPROVE,'APPROVE ',0",
            "REJECT,reject,0", "REJECT,Reject,0", "REJECT,REJECT_extra,0", "REJECT,RETURN,0",
            "APPROVE,REJECT,0", "REJECT,APPROVE,0"})
    void pqcDoneDecisionRequiresFullByteExactReason(String decision, String reason, long expected) throws Exception {
        var task = completedPqcTask(decision);
        jdbc.update("UPDATE mes_pro_edhr_work_task SET reason=? WHERE id=?", reason, task.getId());
        assertEquals(expected, fullLengthH2PqcDoneCount());
        assertEquals(1L, tasks.selectById(task.getId()).getAssigneeUserId());
        assertEquals("1,346,348", tasks.selectById(task.getId()).getCandidateUserSnapshot());
    }

    @ParameterizedTest(name = "QA {0} closure must byte-match task reason [{1}]")
    @CsvSource({"rework,NONCONFORMANCE_REWORK,1", "void,NONCONFORMANCE_VOID,1",
            "rework,nonconformance_rework,0", "void,nonconformance_void,0",
            "rework,NONCONFORMANCE_REWORK_extra,0", "void,NONCONFORMANCE_VOID_extra,0",
            "rework,' NONCONFORMANCE_REWORK',0", "void,'NONCONFORMANCE_VOID ',0",
            "rework,NONCONFORMANCE_VOID,0", "void,NONCONFORMANCE_REWORK,0"})
    void qaDoneClosureRequiresFullByteExactReason(String disposition, String reason, long expected) throws Exception {
        var task = completedQaClosureTask(disposition);
        jdbc.update("UPDATE mes_pro_edhr_work_task SET reason=? WHERE id=?", reason, task.getId());
        assertEquals(expected, fullLengthH2PqcDoneCount(345L));
        assertEquals(1L, tasks.selectById(task.getId()).getAssigneeUserId());
        assertEquals("1,346,348", tasks.selectById(task.getId()).getCandidateUserSnapshot());
    }

    @Test
    void byteExactQaClosureStillRequiresActualActorAndCurrentReviewBinding() throws Exception {
        var task = completedQaClosureTask("void");
        assertEquals(1L, fullLengthH2PqcDoneCount(345L));
        assertEquals(0L, fullLengthH2PqcDoneCount(346L), "the unused PQC candidate is not the QA closer");
        assertEquals(0L, fullLengthH2PqcDoneCount(1L), "the frozen owner is not the QA closer");
        jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET qa_closure_review_id=987002 WHERE id=?",
                task.getBusinessScopeId());
        assertEquals(0L, fullLengthH2PqcDoneCount(345L), "matching bytes cannot replace the formal current closure FK");
    }

    private MesProEdhrWorkTaskDO completedQaClosureTask(String disposition) {
        var task = completedPqcTask("APPROVE");
        LocalDateTime closedAt = task.getCompletedAt();
        String closure = "NONCONFORMANCE_" + disposition.toUpperCase(java.util.Locale.ROOT);
        jdbc.update("INSERT INTO system_users (id,username,nickname,status,tenant_id) VALUES (345,'qa345-byte','QA closer',0,1)");
        jdbc.update("INSERT INTO mes_pro_edhr_nonconformance_review"
                + "(id,review_code,source_type,source_id,active_order_id,batch_execution_id,work_order_id,"
                + "review_status,nonconformance_reason,qa_user_id,frozen_at,closed_at,disposition,tenant_id)"
                + " VALUES(987001,'QA-done-byte','ACTIVE_ORDER',414,414,1229,990274,'closed','formal QA closure',345,?,?,?,1)",
                closedAt.minusHours(1), closedAt, disposition);
        jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status=?,"
                + "qa_closure_review_id=987001,pqc_decision=NULL,pqc_decided_by=NULL,pqc_decided_at=NULL WHERE id=?",
                closure, task.getBusinessScopeId());
        jdbc.update("UPDATE mes_pro_edhr_work_task SET reason=?,review_source_type='EDHR_NONCONFORMANCE_REVIEW',"
                + "review_source_id=987001 WHERE id=?", closure, task.getId());
        return tasks.selectById(task.getId());
    }

    @Test
    void fullByteComparisonRejectsValuesThatCaseInsensitiveTextTreatsAsEqual() {
        // H2 VARCHAR_IGNORECASE proves the byte predicate resists text folding, not MySQL collation support.
        for (String decision : List.of("APPROVE", "REJECT")) {
            String pair = " FROM (SELECT CAST(? AS VARCHAR_IGNORECASE) AS value) a"
                    + " CROSS JOIN (SELECT CAST(? AS VARCHAR) AS value) b";
            assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT a.value = b.value" + pair,
                    Boolean.class, decision, decision.toLowerCase(java.util.Locale.ROOT)));
            assertEquals(Boolean.FALSE, jdbc.queryForObject(
                    "SELECT CAST(a.value AS VARBINARY) = CAST(b.value AS VARBINARY)" + pair,
                    Boolean.class, decision, decision.toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private long fullLengthH2PqcDoneCount() throws Exception {
        return fullLengthH2PqcDoneCount(346L);
    }

    private long fullLengthH2PqcDoneCount(Long actorId) throws Exception {
        var wrapper = new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>();
        var projection = MesProEdhrWorkTaskMapper.class.getDeclaredMethod("applyDoneTaskVisibility",
                LambdaQueryWrapperX.class, Long.class);
        projection.setAccessible(true);
        projection.invoke(tasks, wrapper, actorId);
        wrapper.eq(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                .eq(MesProEdhrWorkTaskDO::getStatus, "DONE")
                .apply("mes_pro_edhr_work_task.tenant_id = {0}", 1L).eq(MesProEdhrWorkTaskDO::getDeleted, false);
        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("CAST(pa.pqc_decision AS BINARY) = CAST(mes_pro_edhr_work_task.reason AS BINARY)"),
                "The actual mapper must cast both differently collated columns; a scalar test cannot replace this guard");
        assertTrue(sql.contains("CAST(pa.application_status AS BINARY) = CAST(mes_pro_edhr_work_task.reason AS BINARY)"),
                "QA status and task reason must use full byte equality despite different MySQL column collations");
        assertTrue(!sql.contains("AND pa.application_status = mes_pro_edhr_work_task.reason"),
                "No implicit cross-column collation comparison may remain in the actual DONE predicate");
        var bindings = new ArrayList<Object>();
        var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)}").matcher(sql);
        while (matcher.find()) bindings.add(wrapper.getParamNameValuePairs().get(matcher.group(1)));
        // MySQL BINARY keeps all bytes. H2 BINARY defaults to length1; adapt only the test dialect cast.
        // All actual formal actor/current FK/tenant/order/time/decision/status predicates remain unchanged.
        sql = sql.replace(" AS BINARY)", " AS VARBINARY)");
        sql = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)}").matcher(sql).replaceAll("?");
        return jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task " + sql,
                Long.class, bindings.toArray());
    }

    private MesProEdhrWorkTaskDO completedPqcTask(String decision) {
        LocalDateTime time = LocalDateTime.of(2026, 10, 5, 12, 0);
        jdbc.update("INSERT INTO system_users (id,username,nickname,status,tenant_id) VALUES (346,'pqc346','PQC decider',0,1)");
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(414L).setWorkOrderId(990274L)
                .setRouteId(980091L).setRouteVersionId(749L).setBatchExecutionId(1229L)
                .setApplicationStatus("APPROVE".equals(decision) ? "MANAGER_RELEASE_PENDING" : "PQC_RELEASE_REJECTED")
                .setPqcDecision(decision).setPqcDecidedBy(346L).setPqcDecidedAt(time).setPqcRejectReason("returned")
                .setSourceSnapshotHash("source").setRequestIdempotencyKey("PQC-done-request").setBusinessIdempotencyKey("PQC-done-business");
        application.setTenantId(1L); applications.insert(application);
        var task = new MesProEdhrWorkTaskDO().setTaskCode("PQC-RELEASE-"+application.getId()).setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(application.getId()).setWorkOrderId(990274L)
                .setWorkOrderCode("WO-990274").setAssigneeUserId(1L).setCandidateUserSnapshot("1,346,348")
                .setStatus("DONE").setCompletedAt(time).setReason(decision).setActionUrl("/mes/production-release/pqc");
        tasks.insert(task); jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id=1 WHERE id=?", task.getId());
        application.setPqcReleaseWorkTaskId(task.getId()); applications.updateById(application);
        return task;
    }

    private MesProEdhrReleaseTransactionDO releasedTransaction(Long signer) {
        var transaction = new MesProEdhrReleaseTransactionDO().setReleaseCode("MARKET-" + signer)
                .setBatchExecutionId(1227L).setWorkOrderId(990274L)
                .setApprovedBy(signer).setApprovedAt(LocalDateTime.now()).setReleaseStatus("RELEASED");
        transactions.insert(transaction);
        jdbc.update("UPDATE mes_pro_edhr_release_transaction SET tenant_id = ? WHERE id = ?", TenantContextHolder.getTenantId(), transaction.getId());
        return transaction;
    }

    private MesProEdhrWorkTaskDO completedMarketTask(MesProEdhrReleaseTransactionDO transaction) {
        var task = new MesProEdhrWorkTaskDO().setTaskCode("MARKET-TASK-" + transaction.getId())
                .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(transaction.getId()).setBatchExecutionId(1227L)
                .setWorkOrderId(990274L).setWorkOrderCode("WO-990274").setAssigneeUserId(1L)
                .setCandidateUserSnapshot("1,347,348").setStatus("DONE")
                .setCompletedAt(LocalDateTime.now()).setActionUrl("/market-release");
        tasks.insert(task);
        jdbc.update("UPDATE mes_pro_edhr_work_task SET tenant_id = ? WHERE id = ?", TenantContextHolder.getTenantId(), task.getId());
        return task;
    }

    private MesProEdhrWorkTaskPageReqVO request() {
        var request = new MesProEdhrWorkTaskPageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);
        return request;
    }
}
