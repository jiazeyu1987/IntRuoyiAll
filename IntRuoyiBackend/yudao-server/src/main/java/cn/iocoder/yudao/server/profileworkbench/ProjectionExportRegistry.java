package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionCodec;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionBatchConsumer;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionExportRegistry implements ProjectionBatchConsumer.GlobalEffects {
    private final List<WorkbenchProjectionExport> exporters;
    private final Set<String> sources;
    private final ProjectionCodec codec;
    public ProjectionExportRegistry(List<WorkbenchProjectionExport> exporters,WorkbenchSourceRegistry registry,ObjectMapper mapper) {
        this.exporters=exporters.stream().filter(e->!Collections.disjoint(e.sourceIds(),registry.enabledSourceIds())).toList();codec=new ProjectionCodec(mapper);Set<String> all=new HashSet<>();
        for(var exporter:this.exporters) for(String source:exporter.sourceIds()) {
            if(!all.add(source)||!registry.require(source).descriptor().ownerModule().equals(exporter.ownerModule())) throw new IllegalStateException("Projection exporter source/module mismatch");
        }
        if(!all.equals(registry.enabledSourceIds())) throw new IllegalStateException("Missing enabled projection exporter");sources=Set.copyOf(all);
    }
    public FrozenExport export(long tenant,Global global,Tenant control) {
        List<Task> tasks=new ArrayList<>();List<Authorization> auth=new ArrayList<>();Watermark watermark=new Watermark(global.head(),control.head());
        for(var exporter:exporters) {tasks.addAll(exporter.exportTasks(tenant,watermark));auth.addAll(exporter.exportAuthorizations(tenant,watermark));}
        return new FrozenExport(tenant,watermark,global.registryRevision(),sources,tasks,auth);
    }
    public List<WorkbenchCommitProtocol.Change> changes(FrozenExport before,FrozenExport after) {
        List<WorkbenchCommitProtocol.Change> changes=new ArrayList<>();Map<String,Task> current=new HashMap<>();
        after.tasks().forEach(t->current.put(t.taskKey(),t));
        // A complete refresh keeps every identity at the same commit vector. Capacity cost is an activation gate.
        after.tasks().forEach(t->changes.add(WorkbenchCommitProtocol.Change.task(t)));
        for(Task t:before.tasks()) if(!current.containsKey(t.taskKey())) changes.add(WorkbenchCommitProtocol.Change.task(
            new Task(t.tenantId(),t.sourceId(),t.taskKey(),ProjectionExportSupport.version(after.watermark()),true,t.businessId(),null,null,null,null,null,null,t.sourceRank(),null,null,null)));
        Set<List<Object>> authorityKeys=new HashSet<>();after.authorizations().forEach(a->{authorityKeys.add(key(a));changes.add(WorkbenchCommitProtocol.Change.authorization(a));});
        for(Authorization a:before.authorizations()) if(!authorityKeys.contains(key(a))) changes.add(WorkbenchCommitProtocol.Change.authorization(
            new Authorization(a.tenantId(),a.sourceId(),a.userId(),a.taskKey(),a.policyId(),ProjectionExportSupport.version(after.watermark()),true,a.validFrom(),a.validUntil(),a.grantKey())));
        return List.copyOf(changes);
    }
    private static List<Object> key(Authorization a) {return List.of(a.sourceId(),a.userId(),a.taskKey(),a.policyId(),a.grantKey());}
    @Override public void apply(String generation,long tenant,Batch batch,ProjectionRepository repository) {
        Global global=repository.lockGlobal(false);Tenant control=repository.lockTenant(tenant,true);
        if(batch.sequence()>global.head()) throw PROJECTION_BATCH_INVALID.exception();
        FrozenExport current=export(tenant,global,control);
        FrozenExport previous=new FrozenExport(tenant,new Watermark(global.head(),control.head()),global.registryRevision(),sources,
            repository.allTasks(generation,tenant).stream().filter(t->!t.tombstone()).toList(),repository.allAuthorizations(generation,tenant).stream().filter(a->!a.revoked()).toList());
        for(var change:changes(previous,current)) {
            if(change.kind()==EventKind.TASK) repository.applyTask(generation,change.task());
            else repository.applyAuthorization(generation,change.authorization());
        }
    }
}
