package cn.iocoder.yudao.module.system.api.workbench.index;

import cn.iocoder.yudao.module.system.api.workbench.ActionEnvelope;
import cn.iocoder.yudao.module.system.api.workbench.RowOrderContract;
import cn.iocoder.yudao.module.system.api.workbench.SortContract;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/** Neutral persisted facts. Authoritative business identities remain in module-owned payloads. */
public final class ProjectionModel {
    private ProjectionModel() { }
    public enum Scope { GLOBAL, TENANT }
    public enum AudienceKind { PERSON, CANDIDATE_SET, TENANT_PERMISSION_QUEUE }
    public enum EventKind { TASK, AUTHORIZATION, CONTROL }
    public record Global(long head, long securityEpoch, String registryRevision) {
        public Global { positiveOrZero(head); positiveOrZero(securityEpoch); text(registryRevision,128); }
    }
    public record Tenant(long tenantId, long head, long securityEpoch, String activeGeneration) {
        public Tenant { positiveOrZero(tenantId); positiveOrZero(head); positiveOrZero(securityEpoch);
            if(activeGeneration!=null) text(activeGeneration,64); }
    }
    public record Watermark(long globalHead, long tenantHead) {
        public Watermark { positiveOrZero(globalHead); positiveOrZero(tenantHead); }
    }
    public record Checkpoint(String generation, long tenantId, long globalCheckpoint,
                             long tenantGlobalCheckpoint, long tenantCheckpoint, boolean complete) {
        public Checkpoint { text(generation,64); positiveOrZero(tenantId); positiveOrZero(globalCheckpoint);
            positiveOrZero(tenantGlobalCheckpoint); positiveOrZero(tenantCheckpoint); }
    }
    public record Audience(AudienceKind kind, Set<Long> members, String policyId, long version) {
        public Audience {
            Objects.requireNonNull(kind); Objects.requireNonNull(members); members=Set.copyOf(members);
            positive(version); text(policyId,128);
            if(members.stream().anyMatch(id->id==null||id<=0)
                || (kind==AudienceKind.PERSON && members.size()!=1)
                || (kind==AudienceKind.CANDIDATE_SET && members.isEmpty())
                || (kind==AudienceKind.TENANT_PERMISSION_QUEUE && !members.isEmpty()))
                throw new IllegalArgumentException("Invalid exact audience");
        }
    }
    public record Task(long tenantId, String sourceId, String taskKey, long entityVersion, boolean tombstone,
                       String businessId, String taskType, String source, String detail, String statusLabel,
                       LocalDateTime createdAt, LocalDateTime dueAt, int sourceRank, String rowOrderKey,
                       ActionEnvelope action, Audience audience) {
        public Task {
            positiveOrZero(tenantId); text(sourceId,64); text(taskKey,160); positive(entityVersion);
            text(businessId,80);
            if(!taskKey.equals(sourceId+":"+businessId)) throw new IllegalArgumentException("Task identity differs");
            if(!tombstone) {
                text(taskType,100); text(source,100); Objects.requireNonNull(detail); Objects.requireNonNull(statusLabel);
                Objects.requireNonNull(action); Objects.requireNonNull(audience);
                RowOrderContract.validate(rowOrderKey); SortContract.validateTime(createdAt); SortContract.validateTime(dueAt);
                if(sourceRank<0) throw new IllegalArgumentException("Negative source rank");
            }
        }
        public TaskSummary summary() {
            if(tombstone) throw new IllegalStateException("Closed projection has no visible summary");
            TaskSummary row=new TaskSummary(); row.setSourceId(sourceId); row.setTaskKey(taskKey); row.setBusinessId(businessId);
            row.setTaskType(taskType); row.setSource(source); row.setDetail(detail); row.setStatusLabel(statusLabel);
            row.setCreatedAt(createdAt); row.setDueAt(dueAt); row.setSourceRank(sourceRank); row.setRowOrderKey(rowOrderKey);
            row.setAction(action); return row;
        }
    }
    /** Empty taskKey denotes source authority; row authority is independently required for a visible task. */
    public record Authorization(long tenantId, String sourceId, long userId, String taskKey, String policyId,
                                long entityVersion, boolean revoked, Instant validFrom, Instant validUntil, String grantKey) {
        public Authorization(long tenantId,String sourceId,long userId,String taskKey,String policyId,long entityVersion,
                             boolean revoked,Instant validFrom,Instant validUntil) {
            this(tenantId,sourceId,userId,taskKey,policyId,entityVersion,revoked,validFrom,validUntil,"default");
        }
        public Authorization {
            positiveOrZero(tenantId); text(sourceId,64); positive(userId); Objects.requireNonNull(taskKey);
            if(taskKey.length()>160 || (!taskKey.isEmpty()&&!taskKey.startsWith(sourceId+":")))
                throw new IllegalArgumentException("Authorization task identity differs");
            text(policyId,128); text(grantKey,64); positive(entityVersion); Objects.requireNonNull(validFrom);
            if(validUntil!=null && !validFrom.isBefore(validUntil)) throw new IllegalArgumentException("Empty authorization interval");
        }
        public boolean effectiveAt(Instant at) {
            return !revoked && !at.isBefore(validFrom) && (validUntil==null || at.isBefore(validUntil));
        }
    }
    public record Event(String eventId, Scope scope, long tenantId, long sequence, String batchKey,
                        int ordinal, int expectedCount, int schemaVersion, EventKind kind,
                        Task task, Authorization authorization, String registryRevision) {
        public Event {
            text(eventId,64); Objects.requireNonNull(scope); positiveOrZero(tenantId); positive(sequence);
            text(batchKey,128); Objects.requireNonNull(kind);
            if(ordinal<0||expectedCount<1||ordinal>=expectedCount||schemaVersion!=4
                || (scope==Scope.GLOBAL && tenantId!=0)) throw new IllegalArgumentException("Invalid event batch identity");
            if((kind==EventKind.TASK && (task==null||authorization!=null||registryRevision!=null||task.tenantId()!=tenantId))
                ||(kind==EventKind.AUTHORIZATION && (authorization==null||task!=null||registryRevision!=null||authorization.tenantId()!=tenantId))
                ||(kind==EventKind.CONTROL && (task!=null||authorization!=null||registryRevision==null)))
                throw new IllegalArgumentException("Invalid event payload");
            if(scope==Scope.GLOBAL && kind!=EventKind.CONTROL) throw new IllegalArgumentException("Global batch requires control facts");
        }
    }
    public record Batch(Scope scope, long tenantId, long sequence, String batchKey, List<Event> events) {
        public Batch {
            Objects.requireNonNull(scope); positiveOrZero(tenantId); positive(sequence); text(batchKey,128);
            events=List.copyOf(events);
            if(events.isEmpty()) throw new IllegalArgumentException("Empty batch");
            Set<String> ids=new HashSet<>(); boolean[] ordinals=new boolean[events.size()];
            for(Event e:events) {
                if(e.scope()!=scope||e.tenantId()!=tenantId||e.sequence()!=sequence||!e.batchKey().equals(batchKey)
                    ||e.expectedCount()!=events.size()||e.ordinal()>=events.size()||ordinals[e.ordinal()]||!ids.add(e.eventId()))
                    throw new IllegalArgumentException("Incomplete or conflicting batch");
                ordinals[e.ordinal()]=true;
            }
            events=events.stream().sorted(Comparator.comparingInt(Event::ordinal)).toList();
        }
    }
    /** Full source collection is an absence proof, not a partial page or an asynchronously scanned snapshot. */
    public record FrozenExport(long tenantId, Watermark watermark, String registryRevision,
                               Set<String> sources, List<Task> tasks, List<Authorization> authorizations) {
        public FrozenExport {
            positiveOrZero(tenantId); Objects.requireNonNull(watermark); text(registryRevision,128);
            sources=Set.copyOf(sources); tasks=List.copyOf(tasks); authorizations=List.copyOf(authorizations);
            // An explicitly empty enabled manifest is a complete empty collection, not a failed export.
            sources.forEach(source->text(source,64));
            Set<String> keys=new HashSet<>();
            for(Task task:tasks) if(task.tenantId()!=tenantId||!sources.contains(task.sourceId())||!keys.add(task.taskKey()))
                throw new IllegalArgumentException("Incomplete or duplicate source export");
            Set<List<Object>> authorityKeys=new HashSet<>();
            for(Authorization a:authorizations) if(a.tenantId()!=tenantId||!sources.contains(a.sourceId())
                ||!authorityKeys.add(List.of(a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey())))
                throw new IllegalArgumentException("Foreign authorization export");
        }
    }
    public static void text(String value,int max) {
        if(value==null||value.isBlank()||value.length()>max) throw new IllegalArgumentException("Invalid bounded identity");
    }
    private static void positive(long value) { if(value<1) throw new IllegalArgumentException("Positive version required"); }
    private static void positiveOrZero(long value) { if(value<0) throw new IllegalArgumentException("Negative identity/version"); }
}
