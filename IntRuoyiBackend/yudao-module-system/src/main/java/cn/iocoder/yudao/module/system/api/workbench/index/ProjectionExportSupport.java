package cn.iocoder.yudao.module.system.api.workbench.index;

import cn.iocoder.yudao.module.system.api.workbench.*;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Shared mechanics only; source policy and formal payload stay in each owning module. */
public final class ProjectionExportSupport {
    private ProjectionExportSupport() { }
    public static long version(Watermark watermark) { return Math.addExact(Math.addExact(watermark.globalHead(),watermark.tenantHead()),1); }
    public static QueryContext query(long tenant) {
        QueryContext query=new QueryContext();query.setTenantId(tenant);query.setUserId(1L);query.setVisibility(null);query.freezeVisibility(1);return query;
    }
    public static Task task(long tenant,Watermark watermark,SourceDescriptor descriptor,TaskSummary row,
                            ActionEnvelope action,AudienceKind kind,Set<Long> members) {
        return new Task(tenant,descriptor.sourceId(),row.getTaskKey(),version(watermark),false,row.getBusinessId(),row.getTaskType(),row.getSource(),row.getDetail(),row.getStatusLabel(),row.getCreatedAt(),row.getDueAt(),descriptor.sortRank(),row.getRowOrderKey(),action,new Audience(kind,members,descriptor.policyId(),version(watermark)));
    }
    public static Set<Long> candidates(Long owner,String tokens) {
        Set<Long> members=new LinkedHashSet<>();if(owner!=null) { if(owner<1) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();members.add(owner); }
        if(tokens!=null&&!tokens.isEmpty()) for(String token:tokens.split(",",-1)) {
            if(!token.matches("[1-9][0-9]*")) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
            try { members.add(Long.parseLong(token)); } catch(NumberFormatException failure) { throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception(failure); }
        }
        if(members.isEmpty()) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();return Set.copyOf(members);
    }
    public static List<Authorization> authorizations(long tenant,Watermark watermark,SourceDescriptor descriptor,
                                                     List<Task> tasks,List<WorkbenchAuthorityExport.Grant> grants) {
        List<Authorization> values=new ArrayList<>();long version=version(watermark);
        for(var grant:grants) {
            values.add(new Authorization(tenant,descriptor.sourceId(),grant.userId(),"","source:"+descriptor.sourceId(),version,false,grant.validFrom(),grant.validUntil(),grant.grantKey()));
            if(tasks.stream().anyMatch(t->t.audience().kind()==AudienceKind.TENANT_PERMISSION_QUEUE))
                values.add(new Authorization(tenant,descriptor.sourceId(),grant.userId(),"",descriptor.policyId(),version,false,grant.validFrom(),grant.validUntil(),grant.grantKey()));
        }
        for(Task task:tasks) if(task.audience().kind()!=AudienceKind.TENANT_PERMISSION_QUEUE)
            for(Long user:task.audience().members()) values.add(new Authorization(tenant,descriptor.sourceId(),user,task.taskKey(),descriptor.policyId(),version,false,Instant.EPOCH,null,"audience"));
        return List.copyOf(values);
    }
}
