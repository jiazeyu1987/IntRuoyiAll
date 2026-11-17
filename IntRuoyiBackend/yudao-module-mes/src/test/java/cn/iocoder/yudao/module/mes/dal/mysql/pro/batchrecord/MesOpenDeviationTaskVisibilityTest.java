package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesOpenDeviationTaskVisibilityTest {
    private Connection db;
    private static final String[] TYPES = {"DEVIATION_RECEIVE", "DEVIATION_CORRECT", "DEVIATION_VERIFY", "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE"};

    @BeforeEach void setUp() throws Exception {
        db = DriverManager.getConnection("jdbc:h2:mem:deviation_visibility_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        execute("CREATE TABLE mes_pro_edhr_work_task(id BIGINT,tenant_id BIGINT,task_type VARCHAR,batch_execution_id BIGINT,business_scope_type VARCHAR,business_scope_id BIGINT,work_order_id BIGINT)");
        execute("CREATE TABLE mes_pro_edhr_batch_execution(id BIGINT,tenant_id BIGINT,deleted INT,status INT)");
        execute("CREATE TABLE mes_pro_edhr_deviation(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,active_order_id BIGINT,work_order_id BIGINT)");
        execute("CREATE TABLE mes_pro_process_pool_active_order(id BIGINT,tenant_id BIGINT,deleted INT,active_status VARCHAR,business_status VARCHAR,work_order_id BIGINT)");
        execute("CREATE TABLE mes_pro_work_order(id BIGINT,tenant_id BIGINT,deleted INT,status INT)");
        execute("INSERT INTO mes_pro_edhr_work_task VALUES(1,186,'DEVIATION_RECEIVE',70,'EDHR_DEVIATION',27,20)");
        execute("INSERT INTO mes_pro_edhr_batch_execution VALUES(70,186,0,20)");
        execute("INSERT INTO mes_pro_edhr_deviation VALUES(27,186,0,'OPEN',30,20)");
        execute("INSERT INTO mes_pro_process_pool_active_order VALUES(30,186,0,'ACTIVE','ACTIVE',20)");
        execute("INSERT INTO mes_pro_work_order VALUES(20,186,0,1)");
    }
    @AfterEach void tearDown() throws Exception { db.close(); }

    @Test void everyDeviationResponsibilityUsesTheProductionOrderRegardlessOfBatchState() throws Exception {
        for (String type : TYPES) {
            execute("UPDATE mes_pro_edhr_work_task SET task_type='" + type + "'");
            for (int state : new int[]{0,10,15,20,30,40,50,60}) {
                execute("UPDATE mes_pro_edhr_batch_execution SET status=" + state);
                check(true, type + " ignores batch=" + state);
            }
            execute("UPDATE mes_pro_edhr_batch_execution SET tenant_id=1,deleted=1");
            check(true, type + " batch never supplies order authority");
            execute("UPDATE mes_pro_edhr_work_task SET batch_execution_id=NULL");
            check(true, type + " production before batch creation");
            execute("UPDATE mes_pro_edhr_work_task SET batch_execution_id=71");
            check(true, type + " order remains authority when batch is absent");
        }
    }

    @Test void completedAndReleasedOrdersStillExposeTheirOpenDeviation() throws Exception {
        for (String active : new String[]{"ACTIVE", "CLOSED"}) for (String business : new String[]{"ACTIVE", "COMPLETED", "RELEASED"}) {
            execute("UPDATE mes_pro_process_pool_active_order SET active_status='" + active + "',business_status='" + business + "'");
            for (int status : new int[]{1,2}) {
                execute("UPDATE mes_pro_work_order SET status=" + status);
                check(true, active + "/" + business + "/" + status);
            }
        }
    }

    @Test void closedDeletedAndForeignDeviationsNeverSupplyOpenTaskAuthority() throws Exception {
        rejectThenRestore("mes_pro_edhr_deviation", "status='CLOSED'", "status='OPEN'");
        rejectThenRestore("mes_pro_edhr_deviation", "deleted=1", "deleted=0");
        rejectThenRestore("mes_pro_edhr_deviation", "tenant_id=1", "tenant_id=186");
        rejectThenRestore("mes_pro_edhr_work_task", "business_scope_type='OTHER'", "business_scope_type='EDHR_DEVIATION'");
        rejectThenRestore("mes_pro_edhr_work_task", "business_scope_id=999", "business_scope_id=27");
        rejectThenRestore("mes_pro_edhr_work_task", "work_order_id=999", "work_order_id=20");
    }

    @Test void explicitWorkOrderRemainsAuthorityWithoutAnActiveExecutionOrBatch() throws Exception {
        execute("UPDATE mes_pro_edhr_deviation SET active_order_id=NULL");
        execute("UPDATE mes_pro_edhr_work_task SET batch_execution_id=NULL");
        check(true, "legacy deviation has explicitly recorded production order");
        execute("UPDATE mes_pro_edhr_deviation SET active_order_id=999");
        check(true, "execution source never substitutes for production order");
        execute("UPDATE mes_pro_process_pool_active_order SET active_status='REMOVED',business_status='REMOVED',deleted=1");
        check(true, "execution lifecycle cannot hide an open production-order deviation");
    }

    @Test void missingDeletedCancelledAndForeignOrdersFailClosedWithoutGuessing() throws Exception {
        for (int status : new int[]{0,3}) rejectThenRestore("mes_pro_work_order", "status=" + status, "status=1");
        rejectThenRestore("mes_pro_work_order", "deleted=1", "deleted=0");
        rejectThenRestore("mes_pro_work_order", "tenant_id=1", "tenant_id=186");
        rejectThenRestore("mes_pro_work_order", "id=999", "id=20");

    }

    @Test void ordinaryTasksRetainTheirBatchLifecycleIncludingArchiveAndCorrectionExceptions() throws Exception {
        for (String type : new String[]{"FILL","REVIEW","APPROVE","REWORK","CLOSE"}) {
            execute("UPDATE mes_pro_edhr_work_task SET task_type='" + type + "'");
            for (int state : new int[]{0,10,15,20}) { execute("UPDATE mes_pro_edhr_batch_execution SET status=" + state); check(true, type); }
            for (int state : new int[]{30,40,50,60}) { execute("UPDATE mes_pro_edhr_batch_execution SET status=" + state); check(false, type); }
        }
        execute("UPDATE mes_pro_edhr_work_task SET task_type='ARCHIVE'");
        execute("UPDATE mes_pro_edhr_batch_execution SET status=30");
        check(true,"archive");
        execute("UPDATE mes_pro_edhr_work_task SET task_type='RECORD_CHANGE_APPROVE'");
        execute("UPDATE mes_pro_edhr_batch_execution SET status=50");
        check(true,"approved record change on rejected batch");
        execute("UPDATE mes_pro_edhr_work_task SET task_type='PQC_RELEASE_CORRECTION'");
        execute("UPDATE mes_pro_edhr_batch_execution SET status=30");
        check(true,"PQC correction on closed batch");
    }

    private void rejectThenRestore(String table, String mutation, String restoration) throws Exception {
        execute("UPDATE " + table + " SET " + mutation);
        check(false, table + ":" + mutation);
        execute("UPDATE " + table + " SET " + restoration);
        check(true, "restored " + table);
    }
    private void check(boolean expected, String scenario) throws Exception {
        String type;
        try(var s=db.createStatement(); var r=s.executeQuery("SELECT task_type FROM mes_pro_edhr_work_task")) {r.next(); type=r.getString(1);}
        String lockedStatus;
        try(var s=db.createStatement(); var r=s.executeQuery("SELECT b.status FROM mes_pro_edhr_work_task t LEFT JOIN mes_pro_edhr_batch_execution b ON b.id=t.batch_execution_id AND b.tenant_id=t.tenant_id AND b.deleted=0")) {
            r.next(); lockedStatus=r.getString(1); if(lockedStatus==null) lockedStatus="NULL";
        }
        for(String predicate:new String[]{MesOpenWorkTaskVisibility.batchSql(null),MesOpenWorkTaskVisibility.batchSql(type),MesOpenWorkTaskVisibility.LOCKED_SOURCE_SQL.replace("#{lockedBatchStatus}",lockedStatus)}) {
            String from=" FROM mes_pro_edhr_work_task WHERE " + predicate;
            assertEquals(expected?1:0,scalar("SELECT COUNT(*)"+from),scenario+" count");
            assertEquals(expected?1:0,scalar("SELECT COUNT(*) FROM (SELECT id"+from+" ORDER BY id LIMIT 20) page"),scenario+" page");
            assertEquals(expected?1:0,scalar("SELECT COUNT(*)"+from+" AND id=1"),scenario+" identity");
        }
    }
    private long scalar(String sql) throws Exception { try(var s=db.createStatement();var r=s.executeQuery(sql)) {r.next();return r.getLong(1);} }
    private void execute(String sql) throws Exception { try(var s=db.createStatement()) {s.execute(sql);} }
}
