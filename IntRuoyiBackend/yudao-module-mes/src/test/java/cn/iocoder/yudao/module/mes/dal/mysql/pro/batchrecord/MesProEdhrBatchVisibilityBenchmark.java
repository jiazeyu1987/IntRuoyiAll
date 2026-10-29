package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

/** Opt-in isolated MySQL benchmark. Supply a new, empty test schema URL; never connects by default. */
public final class MesProEdhrBatchVisibilityBenchmark {
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("A new empty task-owned JDBC schema URL is required");
        try(var db=DriverManager.getConnection(args[0],"root","")) {
            MesProEdhrBatchVisibilitySqlTest.schema(db);
            indexes(db);
            seed(db,1,500);
            analyze(db);
            measure(db,500);
            seed(db,501,5000);
            analyze(db);
            measure(db,5000);
        }
    }
    private static void analyze(Connection db) throws Exception {
        if(!Boolean.getBoolean("edhr.visibility.analyze"))return;
        for(String table:List.of("mes_pro_edhr_batch_execution","mes_pro_edhr_batch_execution_origin",
                "mes_pro_edhr_batch_execution_task","mes_pro_edhr_work_task","mes_pro_edhr_release_transaction",
                "mes_pro_edhr_market_release_handover","mes_pro_process_pool_active_order_release_application",
                "mes_pro_process_pool_active_order","mes_pro_edhr_process_form_permission_rule","mes_pro_batch_record_report"))
            execute(db,"ANALYZE TABLE "+table);
    }
    private static void indexes(Connection db) throws Exception {
        // Exact relevant indexes from repository migrations; no benchmark-only optimization indexes.
        execute(db,"ALTER TABLE mes_pro_edhr_batch_execution ADD status INT DEFAULT 10, ADD create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        execute(db,"CREATE INDEX idx_mes_pro_edhr_batch_execution_status ON mes_pro_edhr_batch_execution(tenant_id,status,create_time)"); // 20260608
        execute(db,"ALTER TABLE mes_pro_edhr_batch_execution_origin ADD origin_key VARCHAR(180) DEFAULT ''");
        execute(db,"CREATE UNIQUE INDEX uk_mes_edhr_trace_origin_key ON mes_pro_edhr_batch_execution_origin(tenant_id,batch_execution_id,origin_key,deleted)"); // 20260822
        execute(db,"CREATE UNIQUE INDEX uk_mes_pro_edhr_batch_task_process_report ON mes_pro_edhr_batch_execution_task(tenant_id,batch_execution_id,route_process_id,batch_record_sort,deleted)"); // 20260608
        execute(db,"CREATE INDEX idx_mes_pro_edhr_batch_task_status ON mes_pro_edhr_batch_execution_task(tenant_id,batch_execution_id,status)");
        execute(db,"ALTER TABLE mes_pro_edhr_work_task ADD batch_task_id BIGINT");
        execute(db,"CREATE INDEX idx_mes_pro_edhr_work_task_batch ON mes_pro_edhr_work_task(tenant_id,batch_execution_id,batch_task_id)"); // 20260611
        execute(db,"CREATE UNIQUE INDEX uk_mes_pro_edhr_release_transaction_batch ON mes_pro_edhr_release_transaction(tenant_id,batch_execution_id,deleted)"); // 20260618
        execute(db,"CREATE INDEX idx_market_handover_batch ON mes_pro_edhr_market_release_handover(tenant_id,batch_execution_id,id)"); // 20261027
        execute(db,"CREATE INDEX idx_market_handover_recipient ON mes_pro_edhr_market_release_handover(tenant_id,recipient_user_id,batch_execution_id)");
        execute(db,"CREATE INDEX idx_mes_pp_release_batch_history ON mes_pro_process_pool_active_order_release_application(tenant_id,batch_execution_id,deleted)"); // 20260922
        execute(db,"ALTER TABLE mes_pro_edhr_process_form_permission_rule MODIFY batch_record_report_id VARCHAR(128), MODIFY rule_type VARCHAR(32), MODIFY scope_key VARCHAR(64), ADD signature_cell_key VARCHAR(128) DEFAULT ''");
        execute(db,"CREATE UNIQUE INDEX uk_mes_pro_edhr_process_form_rule ON mes_pro_edhr_process_form_permission_rule(tenant_id,route_process_id,batch_record_report_id,batch_record_version_id,rule_type,scope_key,signature_cell_key,deleted)"); // 20260706
        execute(db,"CREATE INDEX idx_mes_pro_edhr_process_form_rule_route_report ON mes_pro_edhr_process_form_permission_rule(tenant_id,route_process_id,batch_record_report_id,batch_record_version_id)");
        execute(db,"ALTER TABLE mes_pro_batch_record_report MODIFY report_id VARCHAR(32)");
        execute(db,"CREATE UNIQUE INDEX uk_mes_batch_record_report_report_id ON mes_pro_batch_record_report(report_id)"); // 20260514
    }
    private static void seed(Connection db,int start,int end) throws Exception {
        if(start==1) {
            execute(db,"INSERT INTO mes_pro_process_pool_active_order(id,work_order_id,route_id,route_version_id,leader_user_id) VALUES(20,30,40,50,71),(21,30,40,50,72)");
            execute(db,"INSERT INTO mes_pro_batch_record_report(id,report_id,batch_record_definition_id,batch_record_version_id) VALUES(1,'r',10,20)");
            execute(db,"INSERT INTO mes_pro_edhr_process_form_permission_rule(id,route_process_id,batch_record_report_id,batch_record_version_id,rule_type,scope_key) VALUES(1,100,'r',20,'FILL','ALL')");
        }
        db.setAutoCommit(false);
        try(var batches=db.prepareStatement("INSERT INTO mes_pro_edhr_batch_execution(id,work_order_id,route_id,route_version_id) VALUES(?,30,40,50)");
            var origins=db.prepareStatement("INSERT INTO mes_pro_edhr_batch_execution_origin(id,batch_execution_id,active_order_id,work_order_id) VALUES(?,?,?,30)");
            var transactions=db.prepareStatement("INSERT INTO mes_pro_edhr_release_transaction(id,batch_execution_id) VALUES(?,?)");
            var work=db.prepareStatement("INSERT INTO mes_pro_edhr_work_task(id,batch_execution_id,task_type,business_scope_type,business_scope_id,ownership_locked,status,candidate_user_snapshot) VALUES(?,?,'RELEASE_APPROVE','RELEASE_TRANSACTION',?,1,'DONE','71,72')");
            var tasks=db.prepareStatement("INSERT INTO mes_pro_edhr_batch_execution_task(id,batch_execution_id,node_type,route_process_id,route_process_sort,batch_record_sort,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,status) VALUES(?,?,'ROUTE_FORM',100,1,?,'r',10,20,'MAIN',?)")) {
            for(int id=start;id<=end;id++) {
                batches.setInt(1,id);batches.addBatch();
                origins.setInt(1,id);origins.setInt(2,id);origins.setInt(3,id%3==0?20:21);origins.addBatch();
                if(id%3==1) {
                    transactions.setInt(1,id);transactions.setInt(2,id);transactions.addBatch();
                    work.setInt(1,id);work.setInt(2,id);work.setInt(3,id);work.addBatch();
                }
                for(int slot=1;slot<=3;slot++) {
                    tasks.setInt(1,id*10+slot);tasks.setInt(2,id);tasks.setInt(3,slot);tasks.setInt(4,slot==1?40:slot==2?10:0);tasks.addBatch();
                }
            }
            for(PreparedStatement statement:List.of(batches,origins,transactions,work,tasks))statement.executeBatch();
            db.commit();
        } finally { db.setAutoCommit(true); }
    }
    private static void measure(Connection db,int size) throws Exception {
        String base=" FROM mes_pro_edhr_batch_execution WHERE tenant_id=1 AND deleted=0 AND ";
        String[] sql={
                "SELECT EXISTS(SELECT 1"+base+MesProEdhrBatchVisibilitySql.invalidManagerSnapshot(71L)+")",
                "SELECT EXISTS(SELECT 1"+base+MesProEdhrBatchVisibilitySql.duplicateApplication(71L)+")",
                "SELECT EXISTS(SELECT 1"+base+MesProEdhrBatchVisibilitySql.invalidPqcSnapshot(71L)+")",
                "SELECT EXISTS(SELECT 1"+base+MesProEdhrBatchVisibilitySql.duplicateReport(71L)+")",
                "SELECT r.id FROM mes_pro_edhr_process_form_permission_rule r WHERE EXISTS(SELECT 1"+base+
                        MesProEdhrBatchVisibilitySql.handoverReadable(71L)+" AND NOT "+MesProEdhrBatchVisibilitySql.earlierGrant(71L)+" AND "+MesProEdhrBatchVisibilitySql.taskForRule()+")",
                "SELECT COUNT(*)"+base+MesProEdhrBatchVisibilitySql.visible(71L,false,List.of(1L)),
                "SELECT id"+base+MesProEdhrBatchVisibilitySql.visible(71L,false,List.of(1L))+" ORDER BY id DESC LIMIT 20"};
        String[] names={"manager-invalid","duplicate-application","pqc-invalid","duplicate-report","effective-rules","count","page"};
        for(int run=1;run<=5;run++) {
            long total=System.nanoTime();
            for(int i=0;i<sql.length;i++) {
                long begin=System.nanoTime();int rows=0;long first=-1;
                try(var statement=db.createStatement();var result=statement.executeQuery(sql[i])) {
                    while(result.next()) {if(rows==0)first=result.getLong(1);rows++;}
                }
                if((i<4&&first!=0)||(i==4&&rows!=1)||(i==5&&first!=size)||(i==6&&rows!=20))throw new AssertionError(names[i]+" unexpected result");
                System.out.printf("batches=%d phase=%s run=%d stage=%s ms=%.2f rows=%d first=%d%n",size,run<=2?"warmup":"measure",run,names[i],(System.nanoTime()-begin)/1_000_000.0,rows,first);
            }
            System.out.printf("batches=%d phase=%s run=%d TOTAL_ms=%.2f%n",size,run<=2?"warmup":"measure",run,(System.nanoTime()-total)/1_000_000.0);
        }
        try(var statement=db.createStatement();var result=statement.executeQuery("EXPLAIN FORMAT=JSON "+sql[5])) {
            result.next();java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),"edhr-visibility-rootfix-count-explain-"+size+"-analyzed-"+Boolean.getBoolean("edhr.visibility.analyze")+".json"),result.getString(1));
        }
    }
    private static void execute(Connection db,String sql)throws Exception{try(var statement=db.createStatement()){statement.execute(sql);}}
}
