package cn.iocoder.yudao.module.bpm.workbench;

import cn.iocoder.yudao.module.system.api.workbench.index.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport.*;

@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class EdhrVoidProjectionExport implements WorkbenchProjectionExport {
    private final EdhrVoidWorkbenchSource source;
    private final EdhrVoidWorkbenchMapper mapper;
    private final WorkbenchAuthorityExport authority;
    public EdhrVoidProjectionExport(EdhrVoidWorkbenchSource source,EdhrVoidWorkbenchMapper mapper,WorkbenchAuthorityExport authority) {
        this.source=source;this.mapper=mapper;this.authority=authority;
    }
    @Override public String ownerModule(){return "bpm";}
    @Override public Set<String> sourceIds(){return Set.of(source.descriptor().sourceId());}
    @Override public Set<String> writerIds(){return Set.of("bpm.edhr-void.formal-task");}
    private List<WorkbenchAuthorityExport.Grant> grants(long tenant) {
        List<WorkbenchAuthorityExport.Grant> intersection=null;
        // The target page and its real approve/reject endpoints require all three permissions.
        // Intersect grant validity intervals as well as users, so temporary grants never broaden access.
        for(String permission:EdhrVoidWorkbenchSource.REQUIRED_PERMISSIONS) {
            var next=authority.exportGrants(tenant,new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_PERMISSION,Set.of(permission)));
            if(intersection==null){intersection=List.copyOf(next);continue;}
            Map<Long,List<WorkbenchAuthorityExport.Grant>> byUser=new HashMap<>();
            for(var grant:next)byUser.computeIfAbsent(grant.userId(),key->new ArrayList<>()).add(grant);
            List<WorkbenchAuthorityExport.Grant> joined=new ArrayList<>();
            for(var left:intersection)for(var right:byUser.getOrDefault(left.userId(),List.of())) {
                var from=left.validFrom().isAfter(right.validFrom())?left.validFrom():right.validFrom();
                var until=left.validUntil()==null?right.validUntil():right.validUntil()==null?left.validUntil():
                        left.validUntil().isBefore(right.validUntil())?left.validUntil():right.validUntil();
                if(until!=null&&!from.isBefore(until))continue;
                String key=cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(List.of(permission,left.grantKey(),right.grantKey())));
                joined.add(new WorkbenchAuthorityExport.Grant(left.userId(),key,from,until));
            }
            intersection=List.copyOf(joined);
        }
        return Objects.requireNonNull(intersection);
    }
    @Override public List<Task> exportTasks(long tenant,Watermark watermark) {
        var rows=mapper.rows(source.parameters(query(tenant),true));
        if(rows.isEmpty())return List.of();
        // Enumerate only tenant permission holders, then ask the very same engine responsibility query as S3.
        Map<String,Set<Long>> members=new HashMap<>();
        for(long user:grants(tenant).stream().mapToLong(WorkbenchAuthorityExport.Grant::userId).distinct().toArray()) {
            var q=query(tenant);q.setUserId(user);q.freezeVisibility(1);
            for(var row:mapper.rows(source.parameters(q,false)))
                members.computeIfAbsent(row.getTaskKey(),key->new HashSet<>()).add(user);
        }
        List<Task> result=new ArrayList<>();
        for(var row:rows) {
            var audience=members.get(row.getTaskKey());
            // A task without any authorized tenant reader is outside the personal workbench.
            if(audience!=null&&!audience.isEmpty())result.add(task(tenant,watermark,source.descriptor(),row,source.action(row),AudienceKind.CANDIDATE_SET,audience));
        }
        return List.copyOf(result);
    }
    @Override public List<Authorization> exportAuthorizations(long tenant,Watermark watermark) {
        return authorizations(tenant,watermark,source.descriptor(),exportTasks(tenant,watermark),grants(tenant));
    }
}
