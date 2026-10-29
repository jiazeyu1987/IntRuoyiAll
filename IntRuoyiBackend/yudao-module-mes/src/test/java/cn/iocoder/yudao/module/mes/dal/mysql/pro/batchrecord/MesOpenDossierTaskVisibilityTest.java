package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

/** Runs the production predicate against isolated, explicitly synthetic H2 business records. */
public class MesOpenDossierTaskVisibilityTest {
    public static void main(String[] args) throws Exception {
        new MesOpenDossierTaskVisibilityTest().businessEligibilityDoesNotDependOnNullableTaskBatchId();
        System.out.println("PASS dossier SQL: active order, tenant/work-order identity, latest application round, batch lifecycle, NCR and void exclusions");
    }

    @Test
    public void businessEligibilityDoesNotDependOnNullableTaskBatchId() throws Exception {
        try (var db = DriverManager.getConnection("jdbc:h2:mem:dossier_visibility_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            schema(db);
            execute(db, "INSERT INTO mes_pro_edhr_work_task VALUES(1,1,0,'DOSSIER_COMPLETE','ACTIVE_ORDER_DOSSIER',10,20,'OTHER_FILE',NULL,NULL)");
            check(db, false, "missing active order");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order VALUES(10,1,0,20,'ACTIVE','ACTIVE')");
            check(db, true, "pre-application duty with null task batch id");
            for (String state : new String[]{"FROZEN", "CLOSED", "VOIDED", "RELEASED"}) {
                execute(db, "UPDATE mes_pro_process_pool_active_order SET business_status='" + state + "'");
                check(db, false, "terminal/frozen active order " + state);
            }
            execute(db, "UPDATE mes_pro_process_pool_active_order SET business_status='ACTIVE', active_status='REMOVED'");
            check(db, false, "removed order");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET active_status='ACTIVE',tenant_id=2");
            check(db, false, "other tenant order");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET tenant_id=1,work_order_id=21");
            check(db, false, "different formal work order");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET work_order_id=20");

            execute(db, "INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,0,20,NULL,'pending_review',NULL)");
            check(db, false, "pending work-order NCR");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed',disposition='void'");
            check(db, false, "closed void disposition remains blocking");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET disposition='release'");
            check(db, true, "closed non-void review");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET review_status='pending_review',tenant_id=2");
            check(db, true, "foreign-tenant NCR cannot freeze local order");

            execute(db, "INSERT INTO mes_pro_process_pool_active_order_release_application VALUES(50,1,0,10,20,NULL,'PQC_RELEASE_PENDING')");
            execute(db, "UPDATE mes_pro_edhr_work_task SET review_source_id=50");
            check(db, true, "bound latest application before batch is created");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order_release_application VALUES(51,1,0,10,20,NULL,'PQC_RELEASE_PENDING')");
            check(db, false, "old application duty cannot cross rounds");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET deleted=1 WHERE id=51");
            check(db, true, "deleted round is outside formal latest selection");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET deleted=0,tenant_id=2 WHERE id=51");
            check(db, true, "foreign tenant round does not shadow current application");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET batch_execution_id=70 WHERE id=50");
            check(db, false, "formal batch association requires existing batch even when task batch is null");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution VALUES(70,1,0,20,20)");
            check(db, true, "current mutable formal batch");
            for (int state : new int[]{15,40,50,60}) {
                execute(db, "UPDATE mes_pro_edhr_batch_execution SET status=" + state);
                check(db, false, "actual application batch lifecycle " + state);
            }
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET status=20,tenant_id=2");
            check(db, false, "foreign tenant batch cannot satisfy association");
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET tenant_id=1,work_order_id=21");
            check(db, false, "application batch work-order mismatch");
            execute(db, "UPDATE mes_pro_edhr_batch_execution SET work_order_id=20");
            for (String status : new String[]{"RELEASED", "BATCH_VOIDED"}) {
                execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='" + status + "' WHERE id=50");
                check(db, false, "terminal application " + status);
            }
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='PQC_RELEASE_REJECTED' WHERE id=50");
            check(db, true, "rejected current round allows actual supplement");
            execute(db, "INSERT INTO mes_pro_edhr_nonconformance_review VALUES(2,1,0,99,70,'pending_review',NULL)");
            check(db, false, "formal batch pending NCR blocks even if review work-order reference differs");
            execute(db, "UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed' WHERE id=2");

            execute(db, "INSERT INTO mes_pro_edhr_record_change_event VALUES(1,1,0,70,'BATCH','VOID','DRAFT')");
            for (String status : new String[]{"DRAFT", "SUBMITTED", "APPROVED"}) {
                execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='" + status + "'");
                check(db, false, "pending batch void " + status);
            }
            execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='REJECTED'");
            check(db, true, "rejected void no longer blocks");
            execute(db, "UPDATE mes_pro_edhr_record_change_event SET change_status='DRAFT',tenant_id=2");
            check(db, true, "foreign-tenant void cannot freeze local order");

            execute(db, "UPDATE mes_pro_edhr_work_task SET review_source_type='OLD_REPORT_TYPE'");
            check(db, false, "legacy report key cannot serve as active dossier category");
            execute(db, "UPDATE mes_pro_edhr_work_task SET review_source_type='OTHER_FILE',business_scope_type='RELEASE_APPLICATION'");
            check(db, false, "wrong task scope");
            execute(db, "UPDATE mes_pro_edhr_work_task SET task_type='FILL'");
            check(db, true, "other task types keep their existing business predicates");
        }
    }

    private static void check(Connection db, boolean expected, String scenario) throws Exception {
        String where = " FROM mes_pro_edhr_work_task WHERE tenant_id=1 AND deleted=0 AND " + MesOpenDossierTaskVisibility.OPEN_SQL;
        long total = scalar(db, "SELECT COUNT(*)" + where);
        if (total != (expected ? 1 : 0)) throw new AssertionError(scenario + ": expected " + expected + ", count=" + total);
        // A chunk/export and an identity lookup must consume exactly the same business predicate.
        long chunk = scalar(db, "SELECT COUNT(*) FROM (SELECT id" + where + " ORDER BY id LIMIT 20) dossier_page");
        long identity = scalar(db, "SELECT COUNT(*)" + where + " AND id=1");
        if (chunk != total || identity != total) throw new AssertionError(scenario + ": predicate differs by query shape");
    }
    private static long scalar(Connection db, String sql) throws Exception {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }
    private static void execute(Connection db, String sql) throws Exception { try (var statement = db.createStatement()) { statement.execute(sql); } }
    private static void schema(Connection db) throws Exception {
        execute(db, "CREATE TABLE mes_pro_edhr_work_task(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,task_type VARCHAR,business_scope_type VARCHAR,business_scope_id BIGINT,work_order_id BIGINT,review_source_type VARCHAR,review_source_id BIGINT,batch_execution_id BIGINT)");
        execute(db, "CREATE TABLE mes_pro_process_pool_active_order(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,active_status VARCHAR,business_status VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_process_pool_active_order_release_application(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,active_order_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,application_status VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,status INT)");
        execute(db, "CREATE TABLE mes_pro_edhr_nonconformance_review(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,work_order_id BIGINT,batch_execution_id BIGINT,review_status VARCHAR,disposition VARCHAR)");
        execute(db, "CREATE TABLE mes_pro_edhr_record_change_event(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,batch_execution_id BIGINT,target_scope VARCHAR,change_type VARCHAR,change_status VARCHAR)");
    }
}
