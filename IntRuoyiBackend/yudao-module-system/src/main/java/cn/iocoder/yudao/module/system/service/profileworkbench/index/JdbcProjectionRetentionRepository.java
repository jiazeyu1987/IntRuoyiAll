package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.util.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;

/** Fixed table/key allowlist. Never removes outbox, global identity or formal business rows. */
@Repository @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class JdbcProjectionRetentionRepository implements ProjectionRetentionRepository {
    private final JdbcTemplate jdbc;
    private final ProjectionCodec codec;
    public JdbcProjectionRetentionRepository(DataSource dataSource,ObjectMapper mapper) {
        jdbc=new JdbcTemplate(dataSource);jdbc.setQueryTimeout(10);codec=new ProjectionCodec(mapper);
    }
    private static void transaction(boolean write) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()
            ||(write&&TransactionSynchronizationManager.isCurrentTransactionReadOnly()))
            throw new IllegalStateException("Retention requires the owning master transaction");
    }
    private static void one(int count) {
        if(count!=1)throw new IllegalStateException("Cleanup row count changed; entire batch must roll back");
    }
    private static String table(Stage stage) {
        return switch(stage) {
            case AUDIENCE -> "system_workbench_task_audience";
            case TASK -> "system_workbench_task_projection";
            case AUTHORIZATION -> "system_workbench_authorization_projection";
            case EVENT -> "system_workbench_consumed_event";
            case METADATA -> throw new IllegalArgumentException("Metadata has a separate finalization contract");
        };
    }
    private static List<String> keys(Stage stage) {
        return switch(stage) {
            case AUDIENCE -> List.of("task_key","entity_version","user_id");
            case TASK -> List.of("task_key","entity_version");
            case AUTHORIZATION -> List.of("source_id","user_id","task_key","policy_id","grant_key","entity_version");
            case EVENT -> List.of("event_id");
            case METADATA -> throw new IllegalArgumentException("Metadata is not a row batch");
        };
    }
    @Override public Retirement lockRetirement(long tenant,String generation,boolean exclusive) {
        transaction(exclusive);scope(tenant,generation);
        List<Retirement> rows=jdbc.query("SELECT state,retired_at_millis,purge_not_before_millis,retention_seconds,rollback_protection_seconds,reason,approval_reference FROM system_workbench_generation_retirement WHERE generation=? AND tenant_id=? "+(exclusive?"FOR UPDATE":"LOCK IN SHARE MODE"),
            (r,n)->new Retirement(new Request(tenant,generation,r.getLong(4),r.getLong(5),r.getString(6),r.getString(7)),
                State.valueOf(r.getString(1)),r.getLong(2),r.getLong(3)),generation,tenant);
        if(rows.size()>1)throw new IllegalStateException("Retirement identity duplicated");
        return rows.isEmpty()?null:rows.get(0);
    }
    @Override public Metadata metadata(long tenant,String generation) {
        transaction(false);scope(tenant,generation);
        List<Metadata> rows=jdbc.query("SELECT tenant_global_checkpoint,tenant_checkpoint,complete,registry_revision,sources FROM system_workbench_generation WHERE generation=? AND tenant_id=? LOCK IN SHARE MODE",
            (r,n)->new Metadata(r.getLong(1),r.getLong(2),r.getBoolean(3),r.getString(4),ProjectionCodec.hashText(r.getString(5))),generation,tenant);
        if(rows.size()!=1)throw new IllegalStateException("Target tenant generation metadata missing");
        return rows.get(0);
    }
    @Override public void insert(Retirement retirement) {
        transaction(true);Request r=retirement.request();
        if(retirement.state()!=State.RETIRED)throw new IllegalArgumentException("New retirement must be RETIRED");
        one(jdbc.update("INSERT INTO system_workbench_generation_retirement(generation,tenant_id,state,retired_at_millis,purge_not_before_millis,retention_seconds,rollback_protection_seconds,reason,approval_reference) VALUES(?,?,?,?,?,?,?,?,?)",
            r.generation(),r.tenant(),retirement.state().name(),retirement.retiredAtMillis(),retirement.purgeNotBeforeMillis(),r.retentionSeconds(),r.rollbackProtectionSeconds(),r.reason(),r.approvalReference()));
    }
    private Row row(Stage stage,Map<String,Object> values) {
        List<Object> identity=keys(stage).stream().map(values::get).toList();
        Map<String,String> normalized=new TreeMap<>();
        for(var entry:values.entrySet()) {
            Object value=entry.getValue();
            if("payload".equals(entry.getKey())) {
                if(!(value instanceof String payload)||!ProjectionCodec.hashText(payload).equals(values.get("content_hash")))
                    throw new IllegalStateException("Retired payload/hash mismatch");
                normalized.put("payload_hash",ProjectionCodec.hashText(payload));
            } else normalized.put(entry.getKey(),value==null?null:
                value instanceof byte[] bytes?HexFormat.of().formatHex(bytes):value.toString());
        }
        return new Row(identity,codec.hash(normalized),(String)values.get("content_hash"));
    }
    @Override public Selection nextBatch(long tenant,String generation,int batchSize,boolean exclusive) {
        transaction(exclusive);scope(tenant,generation);batch(batchSize);
        for(Stage stage:List.of(Stage.AUDIENCE,Stage.TASK,Stage.AUTHORIZATION,Stage.EVENT)) {
            List<Map<String,Object>> values=jdbc.queryForList("SELECT * FROM "+table(stage)+" WHERE generation=? AND tenant_id=? ORDER BY "+String.join(",",keys(stage))+" LIMIT ? "+(exclusive?"FOR UPDATE":"LOCK IN SHARE MODE"),generation,tenant,batchSize);
            if(!values.isEmpty())return new Selection(stage,values.stream().map(v->row(stage,v)).toList());
        }
        return new Selection(Stage.METADATA,List.of());
    }
    @Override public void deleteRows(long tenant,String generation,Selection selection) {
        transaction(true);scope(tenant,generation);
        if(selection.rows().isEmpty()||selection.rows().size()>1000)throw new IllegalArgumentException("One non-empty bounded batch required");
        Stage stage=selection.stage();List<String> names=keys(stage);
        String where=String.join(" AND ",names.stream().map(k->k+"=?").toList());
        for(Row row:selection.rows()) {
            if(row.keys().size()!=names.size())throw new IllegalArgumentException("Exact composite key required");
            List<Object> args=new ArrayList<>();args.add(generation);args.add(tenant);args.addAll(row.keys());
            if(stage!=Stage.AUDIENCE) {
                hash(row.contentHash());args.add(row.contentHash());
            }
            one(jdbc.update("DELETE FROM "+table(stage)+" WHERE generation=? AND tenant_id=? AND "+where+(stage==Stage.AUDIENCE?"":" AND content_hash=?"),args.toArray()));
        }
    }
    @Override public void finish(Retirement retirement) {
        transaction(true);Request r=retirement.request();
        if(retirement.state()!=State.RETIRED||nextBatch(r.tenant(),r.generation(),1,true).stage()!=Stage.METADATA)
            throw new IllegalStateException("All target projection rows must be empty before finalization");
        one(jdbc.update("DELETE FROM system_workbench_generation WHERE generation=? AND tenant_id=?",r.generation(),r.tenant()));
        one(jdbc.update("UPDATE system_workbench_generation_retirement SET state='PURGED' WHERE generation=? AND tenant_id=? AND state='RETIRED' AND retired_at_millis=? AND purge_not_before_millis=?",
            r.generation(),r.tenant(),retirement.retiredAtMillis(),retirement.purgeNotBeforeMillis()));
    }
}
