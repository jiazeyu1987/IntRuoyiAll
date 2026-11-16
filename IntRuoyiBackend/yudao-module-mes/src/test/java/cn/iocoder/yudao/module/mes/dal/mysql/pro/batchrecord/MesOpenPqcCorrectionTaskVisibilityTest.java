package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

/** Executes the production open-task predicate against isolated, explicitly synthetic business rows. */
public class MesOpenPqcCorrectionTaskVisibilityTest {
    public static void main(String[] args) throws Exception {
        new MesOpenPqcCorrectionTaskVisibilityTest().onlyTheCurrentMutableRejectedRoundRemainsOpen();
        System.out.println("PASS PQC correction SQL: authoritative identity, latest round, order/batch lifecycle, NCR and VOID");
    }

    @Test
    public void onlyTheCurrentMutableRejectedRoundRemainsOpen() throws Exception {
        try (var db = DriverManager.getConnection("jdbc:h2:mem:pqc_correction_visibility_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            schema(db);
            execute(db, "INSERT INTO mes_pro_edhr_work_task VALUES(1,1,0,'PQC_RELEASE_CORRECTION','RELEASE_APPLICATION',50,20,70,10,'PQC_REJECTION',80,1)");
            check(db, false, "missing authoritative sources");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order_release_application(id,tenant_id,deleted,active_order_id,work_order_id,batch_execution_id,application_status,pqc_decision,applied_by,pqc_release_work_task_id) VALUES(50,1,0,30,20,70,'PQC_RELEASE_REJECTED','REJECT',10,80)");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order VALUES(30,1,0,20,10,'ACTIVE','ACTIVE')");
            execute(db, "INSERT INTO mes_pro_work_order VALUES(20,1,0,0,2)");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution VALUES(70,1,0,20,20)");
            check(db, true, "original applicant owns current rejected round after production completion");
            for (int state : new int[]{15,40,50,60}) {
                execute(db, "UPDATE mes_pro_edhr_batch_execution SET status=" + state);
                check(db, false, "frozen/released/closed/void batch " + state);
            }
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET status=20");
            for (String state : new String[]{"FROZEN", "CLOSED", "VOIDED", "RELEASED", "CANCELED"}) {
                execute(db, "UPDATE mes_pro_process_pool_active_order SET business_status='" + state + "'");
                check(db, false, "inactive business state " + state);
            }
            execute(db, "UPDATE mes_pro_process_pool_active_order SET business_status='ACTIVE',active_status='REMOVED'");
            check(db, false, "removed active order");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET active_status='ACTIVE',leader_user_id=11");
            check(db, false, "former applicant no longer current leader");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET leader_user_id=10");
            execute(db, "UPDATE mes_pro_work_order SET temporary_frozen=1");
            check(db, false, "temporarily frozen work order");
            execute(db, "UPDATE mes_pro_work_order SET temporary_frozen=0,status=3");
            check(db, false, "canceled production work order");
            execute(db, "UPDATE mes_pro_work_order SET status=2");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order_release_application(id,tenant_id,deleted,active_order_id,work_order_id,batch_execution_id,application_status,pqc_decision,applied_by,pqc_release_work_task_id) VALUES(51,1,0,30,20,70,'PQC_RELEASE_PENDING',NULL,10,81)");
            check(db, false, "new application round supersedes old correction");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET deleted=1 WHERE id=51");
            check(db, true, "deleted next round does not shadow current");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET deleted=0,tenant_id=2 WHERE id=51");
            check(db, true, "foreign tenant next round cannot shadow local");
            for (String mutation : new String[]{"applied_by=11", "batch_execution_id=71", "work_order_id=21", "pqc_release_work_task_id=81", "pqc_decision='NONCONFORMANCE_REWORK'", "application_status='PQC_RELEASE_PENDING'", "tenant_id=2"}) {
                execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET " + mutation + " WHERE id=50");
                check(db, false, "application identity mismatch " + mutation);
                execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET applied_by=10,batch_execution_id=70,work_order_id=20,pqc_release_work_task_id=80,pqc_decision='REJECT',application_status='PQC_RELEASE_REJECTED',tenant_id=1 WHERE id=50");
            }
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET tenant_id=2");
            check(db, false, "foreign batch identity");
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET tenant_id=1,work_order_id=21");
            check(db, false, "batch work order mismatch");
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET work_order_id=20");
            execute(db, "INSERT INTO mes_pro_edhr_nonconformance_review(id,tenant_id,deleted,work_order_id,batch_execution_id,active_order_id,source_type,review_status,disposition) VALUES(1,1,0,20,NULL,NULL,'ACTIVE_ORDER','pending_review',NULL)");
            check(db, false, "work-order pending NCR");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed',disposition='void'");
            check(db, false, "void disposition remains blocking");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET disposition='release'");
            check(db, true, "closed non-void outcome");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET work_order_id=99,batch_execution_id=70,review_status='pending_review'");
            check(db, false, "batch pending NCR despite different work-order link");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET batch_execution_id=NULL,active_order_id=30");
            check(db, false, "active-order PQC submission frozen");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET source_type='PQC_RELEASE'");
            check(db, true, "unrelated historical release review does not freeze current active-order submission");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET source_type='ACTIVE_ORDER',tenant_id=2");
            check(db, true, "foreign tenant review cannot freeze local correction");
            execute(db, "INSERT INTO mes_pro_edhr_record_change_event VALUES(1,1,0,70,'BATCH','VOID','DRAFT')");
            for (String state : new String[]{"DRAFT", "SUBMITTED", "APPROVED"}) {
                execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='" + state + "'");
                check(db, false, "pending VOID " + state);
            }
            execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='REJECTED'");
            check(db, true, "rejected VOID no longer blocks");
            execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='DRAFT',tenant_id=2");
            check(db, true, "foreign tenant VOID cannot freeze local correction");
            execute(db, "UPDATE mes_pro_edhr_record_change_event SET deleted=1");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='MARKET_RELEASE_WITHDRAWN',pqc_decision='APPROVE',release_transaction_id=90 WHERE id=50");
            execute(db, "UPDATE mes_pro_edhr_work_task SET review_source_type='MARKET_WITHDRAWAL',review_source_id=90");
            check(db, false, "withdrawal needs its authoritative transaction");
            execute(db, "INSERT INTO mes_pro_edhr_release_transaction VALUES(90,1,0,70,'WITHDRAWN')");
            check(db, true, "original applicant can correct withdrawn market release while PQC remains approved");
            execute(db, "UPDATE mes_pro_edhr_release_transaction SET release_status='PENDING_APPROVAL'");
            check(db, false, "pending transaction cannot masquerade as withdrawal");
            execute(db, "UPDATE mes_pro_edhr_release_transaction SET release_status='WITHDRAWN',tenant_id=2");
            check(db, false, "foreign withdrawal transaction");
            execute(db, "UPDATE mes_pro_edhr_release_transaction SET tenant_id=1");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='MARKET_RELEASE_REJECTED' WHERE id=50");
            execute(db, "UPDATE mes_pro_edhr_work_task SET review_source_type='MARKET_REJECTION'");
            execute(db, "UPDATE mes_pro_edhr_release_transaction SET release_status='REJECTED'");
            check(db, false, "market rejection cannot be corrected before QA concession");
            execute(db, "INSERT INTO mes_pro_edhr_nonconformance_review(id,tenant_id,deleted,work_order_id,batch_execution_id,active_order_id,source_type,source_id,review_status,disposition,qa_user_id,closed_at,remark) VALUES(2,1,0,20,70,30,'ACTIVE_ORDER',30,'closed','concession_release',7,CURRENT_TIMESTAMP,'上市放行不予批准；原放行事务:90')");
            check(db, true, "formal QA concession opens original applicant correction");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET disposition='rework' WHERE id=2");
            check(db, false, "QA rework uses its own production chain");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET disposition='concession_release' WHERE id=2");
            execute(db, "UPDATE mes_pro_edhr_work_task SET ownership_locked=0");
            check(db, false, "unfrozen responsibility");
            execute(db, "UPDATE mes_pro_edhr_work_task SET task_type='FILL'");
            check(db, true, "other task types preserve existing predicates");
        }
    }

    private static void check(Connection db, boolean expected, String scenario) throws Exception {
        String where = " FROM mes_pro_edhr_work_task WHERE tenant_id=1 AND deleted=0 AND " + MesOpenPqcCorrectionTaskVisibility.OPEN_SQL;
        long total = scalar(db, "SELECT COUNT(*)" + where);
        if (total != (expected ? 1 : 0)) throw new AssertionError(scenario + ": count=" + total);
        long page = scalar(db, "SELECT COUNT(*) FROM (SELECT id" + where + " ORDER BY id LIMIT 20) correction_page");
        long identity = scalar(db, "SELECT COUNT(*)" + where + " AND id=1");
        if (page != total || identity != total) throw new AssertionError(scenario + ": query shapes disagree");
    }
    @Test
    public void generatedCurrentBatchKeepsWithdrawnHistoryButAllowsOneNewRound() throws Exception {
        var migration = java.nio.file.Path.of("sql/mysql/20261028_mes_market_withdrawal_correction_round.sql");
        if (!java.nio.file.Files.exists(migration)) migration = java.nio.file.Path.of("../sql/mysql/20261028_mes_market_withdrawal_correction_round.sql");
        String sql = java.nio.file.Files.readString(migration);
        int start = sql.indexOf("(CASE WHEN");
        int end = sql.indexOf(" END)", start) + " END)".length();
        String expression = sql.substring(start, end);
        try (var db = DriverManager.getConnection("jdbc:h2:mem:correction_unique_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            execute(db, "CREATE TABLE rounds(id BIGINT PRIMARY KEY,application_status VARCHAR,pqc_decision VARCHAR,batch_execution_id BIGINT,current_batch_execution_id BIGINT GENERATED ALWAYS AS " + expression + ",UNIQUE(current_batch_execution_id))");
            execute(db, "INSERT INTO rounds(id,application_status,pqc_decision,batch_execution_id) VALUES(1,'MARKET_RELEASE_WITHDRAWN','APPROVE',70),(2,'PQC_RELEASE_PENDING',NULL,70)");
            org.junit.jupiter.api.Assertions.assertEquals(2, scalar(db, "SELECT COUNT(*) FROM rounds WHERE batch_execution_id=70"));
            org.junit.jupiter.api.Assertions.assertEquals(1, scalar(db, "SELECT COUNT(*) FROM rounds WHERE current_batch_execution_id=70"));
            org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> execute(db,
                    "INSERT INTO rounds(id,application_status,pqc_decision,batch_execution_id) VALUES(3,'PQC_RELEASE_PENDING',NULL,70)"));
            org.junit.jupiter.api.Assertions.assertEquals(1, scalar(db, "SELECT COUNT(*) FROM rounds WHERE id=1 AND pqc_decision='APPROVE'"));
        }
    }

    private static long scalar(Connection db, String sql) throws Exception {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }
    private static void execute(Connection db, String sql) throws Exception { try (var statement = db.createStatement()) { statement.execute(sql); } }
    private static void schema(Connection db) throws Exception {
        execute(db, "CREATE TABLE mes_pro_edhr_work_task(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,task_type VARCHAR,business_scope_type VARCHAR,business_scope_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,assignee_user_id BIGINT,review_source_type VARCHAR,review_source_id BIGINT,ownership_locked INT)");
        execute(db, "CREATE TABLE mes_pro_process_pool_active_order(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,leader_user_id BIGINT,active_status VARCHAR,business_status VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_process_pool_active_order_release_application(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,active_order_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,application_status VARCHAR,pqc_decision VARCHAR,applied_by BIGINT,pqc_release_work_task_id BIGINT,release_transaction_id BIGINT)");
        execute(db, "CREATE TABLE mes_pro_edhr_release_transaction(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,release_status VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,status INT)");
        execute(db, "CREATE TABLE mes_pro_work_order(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,temporary_frozen INT,status INT)");
        execute(db, "CREATE TABLE mes_pro_edhr_nonconformance_review(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,batch_execution_id BIGINT,active_order_id BIGINT,source_type VARCHAR,review_status VARCHAR,disposition VARCHAR,source_id BIGINT,qa_user_id BIGINT,closed_at TIMESTAMP,remark VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_edhr_record_change_event(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,target_scope VARCHAR,change_type VARCHAR,change_status VARCHAR)");
    }
}
