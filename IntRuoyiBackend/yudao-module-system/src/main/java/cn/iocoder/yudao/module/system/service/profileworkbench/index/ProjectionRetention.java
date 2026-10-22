package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import java.util.List;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Irreversible retirement of one tenant generation. Durations are explicit seconds. */
public final class ProjectionRetention {
    private ProjectionRetention() { }
    public static final long TOKEN_MAX_SECONDS=300;
    public enum State { RETIRED, PURGED }
    public enum Stage { AUDIENCE, TASK, AUTHORIZATION, EVENT, METADATA }
    public record Request(long tenant,String generation,Long retentionSeconds,Long rollbackProtectionSeconds,
                          String reason,String approvalReference) {
        public Request {
            scope(tenant,generation);
            if(retentionSeconds==null||rollbackProtectionSeconds==null||retentionSeconds<=0||rollbackProtectionSeconds<=0)
                throw new IllegalArgumentException("Explicit positive retention and rollback protection seconds required");
            text(reason,500);text(approvalReference,128);
            delay(retentionSeconds,rollbackProtectionSeconds);
        }
    }
    public record Retirement(Request request,State state,long retiredAtMillis,long purgeNotBeforeMillis) {
        public Retirement {
            if(request==null||state==null||retiredAtMillis<0
                ||purgeNotBeforeMillis!=Math.addExact(retiredAtMillis,delay(request.retentionSeconds(),request.rollbackProtectionSeconds())))
                throw new IllegalArgumentException("Invalid immutable retirement");
        }
    }
    public record Metadata(long tenantGlobalCheckpoint,long tenantCheckpoint,boolean complete,String revision,String sourcesHash) { }
    public record Row(List<Object> keys,String rowHash,String contentHash) {
        public Row { keys=List.copyOf(keys);hash(rowHash);if(contentHash!=null)hash(contentHash); }
    }
    public record Selection(Stage stage,List<Row> rows) {
        public Selection { if(stage==null)throw new IllegalArgumentException("Stage required");rows=List.copyOf(rows);
            if((stage==Stage.METADATA)!=rows.isEmpty())throw new IllegalArgumentException("Invalid cleanup batch"); }
    }
    public record Candidate(Retirement retirement,Global global,Tenant tenant,Metadata metadata,int batchSize,Selection selection) { }
    public record Plan(Candidate candidate,String planHash,boolean hasMore) { }
    public record ApprovalScope(String operation,Request request,Integer batchSize,String planHash) { }
    public interface Evidence {
        Request configuredRequest(long tenant,String generation,String approvalReference);
        void requireApproved(ApprovalScope scope,boolean abandoningIncomplete,long nowMillis);
    }
    public static void scope(long tenant,String generation) {
        if(tenant<0||generation==null||!generation.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Explicit tenant and generation required");
    }
    public static void text(String value,int max) {
        if(value==null||value.isBlank()||value.length()>max)throw new IllegalArgumentException("Explicit bounded text required");
    }
    public static int batch(Integer value) {
        if(value==null||value<1||value>1000)throw new IllegalArgumentException("Explicit batchSize must be 1..1000");
        return value;
    }
    public static void hash(String value) {
        if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Exact SHA-256 plan/content hash required");
    }
    public static long delay(long retention,long rollback) {
        return Math.addExact(Math.multiplyExact(Math.max(retention,rollback),1000),Math.multiplyExact(TOKEN_MAX_SECONDS,1000));
    }
}
