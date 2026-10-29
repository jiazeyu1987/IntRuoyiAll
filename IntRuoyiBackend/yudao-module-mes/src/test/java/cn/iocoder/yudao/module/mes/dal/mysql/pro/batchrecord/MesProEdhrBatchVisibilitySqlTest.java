package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

/** Executes production SQL in H2 MySQL mode; can also run directly without starting Spring or Maven. */
public class MesProEdhrBatchVisibilitySqlTest {
    @FunctionalInterface public interface LegacyOracle { boolean canView(Connection db, List<Long> matchingRules) throws Exception; }
    private LegacyOracle legacyOracle;
    public void compareWith(LegacyOracle oracle) throws Exception {
        this.legacyOracle = oracle;
        databaseVisibilityAndBoundedPage();
    }
    public static void main(String[] args) throws Exception {
        new MesProEdhrBatchVisibilitySqlTest().databaseVisibilityAndBoundedPage();
        System.out.println("PASS visibility SQL: handover, manager, formal owner/PQC, task/rule priority, invalid evidence, bounded page");
    }

    @Test public void databaseVisibilityAndBoundedPage() throws Exception {
        String url=System.getProperty("edhr.visibility.jdbc", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection db = DriverManager.getConnection(url, System.getProperty("edhr.visibility.user","sa"),
                System.getenv("EDHR_VISIBILITY_TEST_PASSWORD") == null ? "" : System.getenv("EDHR_VISIBILITY_TEST_PASSWORD"))) {
            schema(db);
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution(id,work_order_id,route_id,route_version_id) VALUES(1,30,40,50)");
            check(db, false, List.of(), "unassigned batch");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order(id,work_order_id,route_id,route_version_id,leader_user_id) VALUES(20,30,40,50,71)");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,active_order_id,work_order_id) VALUES(1,1,20,30)");
            check(db, true, List.of(), "formal holder");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,active_order_id,work_order_id) VALUES(2,1,21,30)");
            check(db, false, List.of(), "inconsistent origins");
            execute(db, "DELETE FROM mes_pro_edhr_batch_execution_origin WHERE id=2");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET tenant_id=2");
            check(db, false, List.of(), "foreign tenant owner");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET tenant_id=1");
            execute(db, "INSERT INTO mes_pro_edhr_market_release_handover(id,batch_execution_id,original_user_id,recipient_user_id,recalled_at) VALUES(1,1,72,71,CURRENT_TIMESTAMP)");
            check(db, false, List.of(), "recalled recipient vetoes owner");
            assertNumber(0, scalar(db, "SELECT COUNT(*) FROM mes_pro_edhr_batch_execution WHERE " + MesProEdhrBatchVisibilitySql.visible(71L,true,List.of())), "vetoes overview");
            execute(db, "UPDATE mes_pro_edhr_market_release_handover SET recalled_at=NULL");
            check(db, true, List.of(), "current recipient");
            execute(db, "UPDATE mes_pro_edhr_market_release_handover SET recipient_user_id=73, original_user_id=71");
            check(db, true, List.of(), "original actor");
            execute(db, "DELETE FROM mes_pro_edhr_market_release_handover");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET leader_user_id=72");
            execute(db, "INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id) VALUES(10,1)");
            execute(db, "INSERT INTO mes_pro_edhr_work_task(id,batch_execution_id,task_type,business_scope_type,business_scope_id,ownership_locked,status,candidate_user_snapshot) VALUES(11,1,'RELEASE_APPROVE','RELEASE_TRANSACTION',10,1,'DONE',' +00071 , ,72')");
            check(db, true, List.of(), "manager frozen snapshot numeric grammar");
            execute(db, "UPDATE mes_pro_edhr_work_task SET candidate_user_snapshot='　\u200c+00071\u200c　'");
            check(db, true, List.of(), "Hutool Unicode candidate trimming");
            execute(db, "UPDATE mes_pro_edhr_work_task SET task_type='RELEASE_APPROVE" + "\0".repeat(60) + "X'");
            check(db, false, List.of(), "long enum cannot exploit binary truncation");
            execute(db, "UPDATE mes_pro_edhr_work_task SET task_type='release_approve'");
            check(db, false, List.of(), "manager type is case sensitive");
            execute(db, "UPDATE mes_pro_edhr_work_task SET task_type='RELEASE_APPROVE'");
            execute(db, "INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id) VALUES(12,1)");
            check(db, false, List.of(), "only latest transaction");
            execute(db, "UPDATE mes_pro_edhr_work_task SET business_scope_id=12,candidate_user_snapshot='71,bad'");
            assertProbe(db, MesProEdhrBatchVisibilitySql.invalidManagerSnapshot(71L), true, "malformed manager snapshot");
            execute(db, "UPDATE mes_pro_edhr_work_task SET candidate_user_snapshot='9223372036854775808'");
            assertProbe(db, MesProEdhrBatchVisibilitySql.invalidManagerSnapshot(71L), true, "overflow snapshot");
            execute(db, "UPDATE mes_pro_edhr_work_task SET candidate_user_snapshot='-9223372036854775808,+9223372036854775807,,0'");
            assertProbe(db, MesProEdhrBatchVisibilitySql.invalidManagerSnapshot(71L), false, "valid long boundaries");
            execute(db, "DELETE FROM mes_pro_edhr_work_task");
            execute(db, "INSERT INTO mes_pro_process_pool_active_order_release_application(id,batch_execution_id,active_order_id,work_order_id,route_id,route_version_id,pqc_release_work_task_id,application_status,pqc_decision) VALUES(31,1,20,30,40,50,61,'RELEASED','APPROVE')");
            execute(db, "INSERT INTO mes_pro_edhr_work_task(id,batch_execution_id,work_order_id,route_id,task_type,business_scope_type,business_scope_id,ownership_locked,status,reason,candidate_user_snapshot) VALUES(61,1,30,40,'PQC_PRODUCTION_RELEASE','RELEASE_APPLICATION',31,1,'DONE','APPROVE','71')");
            check(db, true, List.of(), "approved PQC history");
            execute(db, "UPDATE mes_pro_edhr_work_task SET batch_execution_id=NULL");
            check(db, false, List.of(), "approved PQC requires batch association");
            execute(db, "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='PQC_RELEASE_PENDING'");
            execute(db, "UPDATE mes_pro_edhr_work_task SET status='OVERDUE'");
            check(db, true, List.of(), "pending overdue PQC permits null batch");
            execute(db, "UPDATE mes_pro_edhr_work_task SET candidate_user_snapshot='bad',status='CANCELLED'");
            assertProbe(db, MesProEdhrBatchVisibilitySql.invalidPqcSnapshot(71L), true, "PQC snapshot evaluated before status");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET leader_user_id=71");
            assertProbe(db, MesProEdhrBatchVisibilitySql.invalidPqcSnapshot(71L), false, "owner short circuits bad PQC");
            execute(db, "UPDATE mes_pro_process_pool_active_order SET leader_user_id=72");
            execute(db, "DELETE FROM mes_pro_edhr_work_task");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution_task(id,batch_execution_id,node_type,route_process_id,route_process_sort,batch_record_sort,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,status) VALUES(101,1,'ROUTE_FORM',100,1,1,'r',10,20,'MAIN',0),(102,1,'ROUTE_FORM',200,2,1,'r',10,20,'MAIN',10)");
            assertNumber(102, scalar(db, "SELECT " + MesProEdhrBatchVisibilitySql.currentTaskId() + " FROM mes_pro_edhr_batch_execution"), "active before waiting");
            execute(db, "UPDATE mes_pro_edhr_batch_execution_task SET status=40");
            assertNumber(102, scalar(db, "SELECT " + MesProEdhrBatchVisibilitySql.currentTaskId() + " FROM mes_pro_edhr_batch_execution"), "all approved selects last");
            execute(db, "UPDATE mes_pro_edhr_batch_execution_task SET status=45,required_policy='SKIPPABLE_CONTROLLED' WHERE id=101");
            assertNumber(101, scalar(db, "SELECT " + MesProEdhrBatchVisibilitySql.currentTaskId() + " FROM mes_pro_edhr_batch_execution"), "unproven controlled skip remains current");
            execute(db, "UPDATE mes_pro_edhr_batch_execution_task SET status=0 WHERE id=101");
            execute(db, "INSERT INTO mes_pro_edhr_batch_execution_task(id,batch_execution_id,node_type,route_process_id,route_process_sort,batch_record_sort,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,status) VALUES(103,1,'ROUTE_FORM',100,1,2,'r',10,20,'ATTACHMENT',10)");
            assertNumber(101, scalar(db, "SELECT " + MesProEdhrBatchVisibilitySql.currentTaskId() + " FROM mes_pro_edhr_batch_execution"), "MAIN replaces current attachment");
            execute(db, "UPDATE mes_pro_edhr_batch_execution_task SET node_type='\t　' WHERE id=101");
            assertNumber(101, scalar(db, "SELECT " + MesProEdhrBatchVisibilitySql.currentTaskId() + " FROM mes_pro_edhr_batch_execution"), "Unicode blank node preserves inferred route form");
            execute(db, "INSERT INTO mes_pro_batch_record_report(id,report_id,batch_record_definition_id,batch_record_version_id) VALUES(1,'r',10,20)");
            execute(db, "INSERT INTO mes_pro_edhr_process_form_permission_rule(id,route_process_id,batch_record_report_id,batch_record_version_id,rule_type,scope_key) VALUES(1,0,'r',20,'FILL','z')");
            check(db, true, List.of(1L), "report level rule");
            execute(db, "INSERT INTO mes_pro_edhr_process_form_permission_rule(id,route_process_id,batch_record_report_id,batch_record_version_id,rule_type,scope_key) VALUES(2,100,'r',20,'QUALITY_FILL','a')");
            check(db, false, List.of(1L), "any route rule suppresses all report-level types");
            check(db, true, List.of(2L), "route quality rule");
            execute(db, "INSERT INTO mes_pro_edhr_process_form_permission_rule(id,route_process_id,batch_record_report_id,batch_record_version_id,rule_type,scope_key) VALUES(3,100,'r',20,'QUALITY_FILL','b'),(4,100,'r',20,'QUALITY_FILL','a')");
            check(db, false, List.of(2L,3L), "scope then latest ID priority");
            check(db, true, List.of(4L), "effective first rule");
            execute(db, "UPDATE mes_pro_edhr_process_form_permission_rule SET tenant_id=2 WHERE id=4");
            check(db, true, List.of(2L), "foreign tenant rule cannot shadow local priority");
            execute(db, "UPDATE mes_pro_edhr_process_form_permission_rule SET tenant_id=1 WHERE id=4");
            execute(db, "UPDATE mes_pro_edhr_process_form_permission_rule SET batch_record_version_id=NULL");
            check(db, true, List.of(4L), "historical null-version read contract");
            execute(db, "UPDATE mes_pro_batch_record_report SET batch_record_version_id=21");
            check(db, false, List.of(4L), "frozen binding mismatch");
            execute(db, "UPDATE mes_pro_batch_record_report SET batch_record_version_id=20");
            String rulesSql = "SELECT COUNT(*) FROM mes_pro_edhr_process_form_permission_rule r WHERE EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution WHERE " + MesProEdhrBatchVisibilitySql.taskForRule() + ")";
            assertNumber(1, scalar(db, rulesSql), "only effective rule leaves SQL");
            StringBuilder batches=new StringBuilder("INSERT INTO mes_pro_edhr_batch_execution(id,work_order_id,route_id,route_version_id) VALUES");
            StringBuilder origins=new StringBuilder("INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,active_order_id,work_order_id) VALUES");
            for(int id=2;id<=5001;id++) {
                if(id>2) { batches.append(','); origins.append(','); }
                batches.append('(').append(id).append(",30,40,50)");
                origins.append('(').append(id).append(',').append(id).append(",20,30)");
            }
            execute(db,batches.toString()); execute(db,origins.toString());
            execute(db, "UPDATE mes_pro_process_pool_active_order SET leader_user_id=71");
            String where = " FROM mes_pro_edhr_batch_execution WHERE tenant_id=1 AND deleted=0 AND " + MesProEdhrBatchVisibilitySql.visible(71L,false,List.of(4L));
            assertNumber(5001, scalar(db, "SELECT COUNT(*)" + where), "exact large count");
            int returned = 0;
            try (var statement = db.createStatement(); var rows = statement.executeQuery("SELECT id" + where + " ORDER BY id DESC LIMIT 20")) {
                while (rows.next()) returned++;
            }
            assertNumber(20, returned, "database page bounds returned batch rows");
        }
    }
    private void check(Connection db, boolean expected, List<Long> rules, String label) throws Exception {
        assertNumber(expected ? 1 : 0, scalar(db, "SELECT COUNT(*) FROM mes_pro_edhr_batch_execution WHERE tenant_id=1 AND deleted=0 AND "
                + MesProEdhrBatchVisibilitySql.visible(71L,false,rules)), label);
        if (legacyOracle != null) assertNumber(expected ? 1 : 0, legacyOracle.canView(db,rules) ? 1 : 0, "legacy parity: " + label);
    }
    private static void assertProbe(Connection db, String predicate, boolean expected, String label) throws Exception {
        assertNumber(expected ? 1 : 0, scalar(db, "SELECT COUNT(*) FROM mes_pro_edhr_batch_execution WHERE " + predicate), label);
    }
    private static long scalar(Connection db, String sql) throws Exception {
        try (var statement = db.createStatement(); ResultSet rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }
    private static void assertNumber(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
    }
    private static void execute(Connection db, String sql) throws Exception {
        if(sql.startsWith("CREATE TABLE")) sql=sql.replace("VARCHAR", "VARCHAR(1000)");
        try (var statement = db.createStatement()) { statement.execute(sql); }
    }
    static void schema(Connection db) throws Exception {
        String common = "id BIGINT PRIMARY KEY,tenant_id BIGINT DEFAULT 1,deleted INT DEFAULT 0,";
        execute(db,"CREATE TABLE mes_pro_edhr_batch_execution("+common+"work_order_id BIGINT,route_id BIGINT,route_version_id BIGINT)");
        execute(db,"CREATE TABLE mes_pro_edhr_market_release_handover("+common+"batch_execution_id BIGINT,original_user_id BIGINT,recipient_user_id BIGINT,recalled_at TIMESTAMP)");
        execute(db,"CREATE TABLE mes_pro_edhr_release_transaction("+common+"batch_execution_id BIGINT)");
        execute(db,"CREATE TABLE mes_pro_edhr_work_task("+common+"batch_execution_id BIGINT,work_order_id BIGINT,route_id BIGINT,task_type VARCHAR,business_scope_type VARCHAR,business_scope_id BIGINT,ownership_locked INT,status VARCHAR,reason VARCHAR,candidate_user_snapshot VARCHAR,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        execute(db,"CREATE TABLE mes_pro_process_pool_active_order("+common+"work_order_id BIGINT,route_id BIGINT,route_version_id BIGINT,leader_user_id BIGINT)");
        execute(db,"CREATE TABLE mes_pro_edhr_batch_execution_origin("+common+"batch_execution_id BIGINT,work_order_id BIGINT,active_order_id BIGINT)");
        execute(db,"CREATE TABLE mes_pro_process_pool_active_order_release_application("+common+"batch_execution_id BIGINT,work_order_id BIGINT,active_order_id BIGINT,route_id BIGINT,route_version_id BIGINT,pqc_release_work_task_id BIGINT,application_status VARCHAR,pqc_decision VARCHAR)");
        execute(db,"CREATE TABLE mes_pro_edhr_batch_execution_task("+common+"batch_execution_id BIGINT,node_type VARCHAR,process_code VARCHAR,required_flag INT DEFAULT 1,required_policy VARCHAR,route_process_id BIGINT,route_process_sort INT,batch_record_sort INT,batch_record_report_id VARCHAR,batch_record_definition_id BIGINT,batch_record_version_id BIGINT,form_slot_type VARCHAR,status INT,skipped_by BIGINT,skipped_at TIMESTAMP)");
        execute(db,"CREATE TABLE mes_pro_batch_record_report("+common+"report_id VARCHAR,batch_record_definition_id BIGINT,batch_record_version_id BIGINT)");
        execute(db,"CREATE TABLE mes_pro_edhr_process_form_permission_rule("+common+"enabled INT DEFAULT 1,route_process_id BIGINT,batch_record_report_id VARCHAR,batch_record_version_id BIGINT,rule_type VARCHAR,scope_key VARCHAR)");
    }
}
