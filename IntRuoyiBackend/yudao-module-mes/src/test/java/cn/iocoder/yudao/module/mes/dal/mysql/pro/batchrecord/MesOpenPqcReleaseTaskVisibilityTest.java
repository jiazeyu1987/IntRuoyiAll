package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesOpenPqcReleaseTaskVisibilityTest {
    private Connection db;

    @BeforeEach
    void setUp() throws Exception {
        db = DriverManager.getConnection("jdbc:h2:mem:pqc_release_ncr_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        execute("CREATE TABLE mes_pro_edhr_work_task(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,task_type VARCHAR,business_scope_type VARCHAR,business_scope_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,status VARCHAR)");
        execute("CREATE TABLE mes_pro_process_pool_active_order_release_application(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,active_order_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT)");
        execute("CREATE TABLE mes_pro_edhr_nonconformance_review(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,active_order_id BIGINT,work_order_id BIGINT,batch_execution_id BIGINT,review_status VARCHAR)");
        execute("INSERT INTO mes_pro_edhr_work_task VALUES(1,1,0,'PQC_PRODUCTION_RELEASE','RELEASE_APPLICATION',50,20,NULL,'TODO')");
        execute("INSERT INTO mes_pro_process_pool_active_order_release_application VALUES(50,1,0,30,20,70)");
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void pendingReviewRemovesReleaseFromPageCountAndIdentityWithoutCompletingTask() throws Exception {
        check(1);
        execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,0,30,20,70,'pending_review')");
        check(0);
        assertEquals(1, scalar("SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE status='TODO'"));
        execute("UPDATE mes_pro_edhr_nonconformance_review SET review_status='closed'");
        check(1);
    }

    @Test
    void matchesWorkOrderAndBatchEvenWithoutReleaseApplication() throws Exception {
        execute("DELETE FROM mes_pro_process_pool_active_order_release_application");
        execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,0,NULL,20,NULL,'pending_review')");
        check(0);
        execute("UPDATE mes_pro_edhr_work_task SET work_order_id=NULL,batch_execution_id=70");
        execute("UPDATE mes_pro_edhr_nonconformance_review SET work_order_id=NULL,batch_execution_id=70");
        check(0);
    }

    @Test
    void usesApplicationLinksWhenTaskHasNoBatchOrWorkOrder() throws Exception {
        execute("UPDATE mes_pro_edhr_work_task SET work_order_id=NULL");
        execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,0,30,NULL,NULL,'pending_review')");
        check(0);
        execute("UPDATE mes_pro_edhr_nonconformance_review SET active_order_id=NULL,work_order_id=20");
        check(0);
        execute("UPDATE mes_pro_edhr_nonconformance_review SET work_order_id=NULL,batch_execution_id=70");
        check(0);
        execute("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id=2");
        check(1);
        execute("UPDATE mes_pro_process_pool_active_order_release_application SET tenant_id=1,deleted=1");
        check(1);
    }

    @Test
    void ignoresUnrelatedDeletedAndOtherTenantReviewsAndPreservesOtherTasks() throws Exception {
        execute("INSERT INTO mes_pro_edhr_nonconformance_review VALUES(1,1,0,31,21,71,'pending_review')");
        check(1);
        execute("UPDATE mes_pro_edhr_nonconformance_review SET active_order_id=30,work_order_id=20,batch_execution_id=70,tenant_id=2");
        check(1);
        execute("UPDATE mes_pro_edhr_nonconformance_review SET tenant_id=1,deleted=1");
        check(1);
        execute("UPDATE mes_pro_edhr_nonconformance_review SET deleted=0");
        check(0);
        execute("UPDATE mes_pro_edhr_work_task SET task_type='FILL'");
        check(1);
        execute("UPDATE mes_pro_edhr_work_task SET task_type='PQC_RELEASE_CORRECTION'");
        check(1);
    }

    private void check(long expected) throws Exception {
        String from = " FROM mes_pro_edhr_work_task WHERE tenant_id=1 AND deleted=0 AND status IN ('TODO','OVERDUE') AND "
                + MesOpenWorkTaskVisibility.OPEN_PQC_RELEASE_SQL;
        assertEquals(expected, scalar("SELECT COUNT(*)" + from), "pending count");
        assertEquals(expected, scalar("SELECT COUNT(*) FROM (SELECT id" + from + " ORDER BY id LIMIT 20) page"), "pending page");
        assertEquals(expected, scalar("SELECT COUNT(*)" + from + " AND id=1"), "task identity");
    }

    private long scalar(String sql) throws Exception {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (var statement = db.createStatement()) {
            statement.execute(sql);
        }
    }
}
