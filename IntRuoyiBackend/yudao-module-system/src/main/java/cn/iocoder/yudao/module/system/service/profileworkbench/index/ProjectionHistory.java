package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Permanent identities are separate from disposable payloads. Times are observation times, never commit times. */
public final class ProjectionHistory {
    private ProjectionHistory() { }
    public enum Target { OUTBOX, VERSION }
    public record Budget(Integer maxEvents,Integer maxRows,Long maxPayloadBytes,Integer maxConsumers) {
        public Budget {
            bounded(maxEvents,10000);bounded(maxRows,10000);bounded(maxConsumers,100000);
            if(maxPayloadBytes==null||maxPayloadBytes<1||maxPayloadBytes>67108864L)throw new IllegalArgumentException("Explicit bounded payload budget required");
        }
        public void rows(long count) {if(count>maxRows)throw new IllegalStateException("History row budget exceeded");}
        public void bytes(long count) {if(count>maxPayloadBytes)throw new IllegalStateException("History payload budget exceeded");}
    }
    public record Request(Target target,Scope scope,long tenant,String generation,Long sequence,Long retentionSeconds,
                          Long rollbackProtectionSeconds,Long maximumClockSkewSeconds,String reason,String approvalReference,Budget budget) {
        public Request {
            Objects.requireNonNull(target);Objects.requireNonNull(scope);Objects.requireNonNull(budget);
            if(tenant<0||(scope==Scope.GLOBAL&&tenant!=0))throw new IllegalArgumentException("Explicit history scope required");
            if(target==Target.VERSION) {if(scope!=Scope.TENANT||sequence!=null)throw new IllegalArgumentException("Version scope must be TENANT without sequence");ProjectionRetention.scope(tenant,generation);}
            else if(generation!=null)throw new IllegalArgumentException("Outbox is independent of generation");
            if(sequence!=null&&sequence<1)throw new IllegalArgumentException("Explicit positive batch sequence required");
            text(reason,500);
            if(approvalReference==null||!approvalReference.matches("[A-Za-z0-9_-]{1,32}"))throw new IllegalArgumentException("Explicit bounded approval reference required");
            delay(retentionSeconds,rollbackProtectionSeconds,maximumClockSkewSeconds);
        }
    }
    public record Observation(long observedAtMillis,long purgeNotBeforeMillis,long retentionSeconds,
                              long rollbackSeconds,long clockSkewSeconds,String approvalReference) {
        public Observation {
            if(observedAtMillis<0||purgeNotBeforeMillis!=Math.addExact(observedAtMillis,delay(retentionSeconds,rollbackSeconds,clockSkewSeconds)))
                throw new IllegalArgumentException("Invalid immutable observation window");
            text(approvalReference,32);
        }
        public void matches(Request r,long now) {
            if(retentionSeconds!=r.retentionSeconds||rollbackSeconds!=r.rollbackProtectionSeconds||clockSkewSeconds!=r.maximumClockSkewSeconds
                ||!approvalReference.equals(r.approvalReference)||now<observedAtMillis)
                throw new IllegalStateException("Observation parameters are immutable or clock moved backwards");
        }
    }
    public record Consumer(long tenant,String generation,long globalCheckpoint,long tenantGlobalCheckpoint,
                           long tenantCheckpoint,boolean complete,String activeGeneration,long tenantHead,long securityEpoch,
                           String registryRevision,String sourcesHash,long tenantFloor) { }
    public record Item(String kind,List<Object> keys,String contentHash,String rowHash,long payloadBytes,
                       Long closedVersionTo,List<Long> members) {
        public Item {keys=List.copyOf(keys);members=List.copyOf(members);ProjectionRetention.hash(contentHash);ProjectionRetention.hash(rowHash);}
    }
    public record Snapshot(Global global,List<Tenant> tenants,long globalFloor,long tenantFloor,List<Consumer> consumers,
                           long sequence,String batchKey,String identityHash,Observation observation,List<Item> items) {
        public Snapshot {tenants=List.copyOf(tenants);consumers=List.copyOf(consumers);items=List.copyOf(items);}
    }
    public record Candidate(Request request,Snapshot snapshot) { }
    public record Plan(Candidate candidate,String planHash) { }
    public record ApprovalScope(String operation,Request request,String planHash) { }
    public interface Evidence {
        Request configuredRequest(String reference);
        void requireApproved(ApprovalScope scope,long now);
    }
    static long delay(Long retention,Long rollback,Long skew) {
        if(retention==null||rollback==null||skew==null||retention<1||rollback<1||skew<0)throw new IllegalArgumentException("All retention and clock bounds are required");
        return Math.multiplyExact(Math.addExact(Math.addExact(Math.max(retention,rollback),ProjectionRetention.TOKEN_MAX_SECONDS),Math.multiplyExact(2,skew)),1000);
    }
    static Observation observed(Request r,long now) {
        return new Observation(now,Math.addExact(now,delay(r.retentionSeconds,r.rollbackProtectionSeconds,r.maximumClockSkewSeconds)),r.retentionSeconds,r.rollbackProtectionSeconds,r.maximumClockSkewSeconds,r.approvalReference);
    }
    private static void bounded(Integer n,int maximum) {if(n==null||n<1||n>maximum)throw new IllegalArgumentException("Explicit bounded history budget required");}
}
