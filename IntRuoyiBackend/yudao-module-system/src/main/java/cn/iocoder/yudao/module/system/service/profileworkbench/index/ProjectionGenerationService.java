package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchProjectionExport;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionGenerationService {
    public interface ActivationEvidence {
        void requireApproved(long tenant,String generation,Watermark watermark,String collectionHash);
    }
    private final ProjectionRepository repository;
    private final List<WorkbenchProjectionExport> exporters;
    private final ProjectionCodec codec;
    private final ActivationEvidence evidence;
    public ProjectionGenerationService(ProjectionRepository repository,List<WorkbenchProjectionExport> exporters,
                                       com.fasterxml.jackson.databind.ObjectMapper mapper,ActivationEvidence evidence) {
        this.repository=repository;this.exporters=List.copyOf(exporters);this.codec=new ProjectionCodec(mapper);this.evidence=evidence;
    }
    private FrozenExport export(long tenant,Global global,Tenant control,Set<String> expected) {
        Set<String> actual=new HashSet<>(); List<Task> tasks=new ArrayList<>();List<Authorization> authorizations=new ArrayList<>();
        Watermark watermark=new Watermark(global.head(),control.head());
        for(WorkbenchProjectionExport exporter:exporters) {
            if(Collections.disjoint(exporter.sourceIds(),expected)) continue;
            if(!expected.containsAll(exporter.sourceIds())) throw SOURCE_CONTRACT_INVALID.exception();
            for(String source:exporter.sourceIds()) if(!actual.add(source)) throw SOURCE_CONTRACT_INVALID.exception();
            tasks.addAll(exporter.exportTasks(tenant,watermark)); authorizations.addAll(exporter.exportAuthorizations(tenant,watermark));
        }
        if(!actual.equals(expected)) throw SOURCE_NOT_REGISTERED.exception();
        return new FrozenExport(tenant,watermark,global.registryRevision(),actual,tasks,authorizations);
    }
    /** All module scans and initial loading finish before releasing the frozen master transaction. */
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Watermark rebuild(String generation,long tenant,Set<String> sources) {
        Global global=repository.lockGlobal(false); Tenant control=repository.lockTenant(tenant,false);
        repository.requireRetained(generation,tenant);
        FrozenExport snapshot=export(tenant,global,control,sources);
        repository.createGeneration(generation,tenant,snapshot.watermark(),snapshot.registryRevision(),snapshot.sources());
        for(Task task:snapshot.tasks()) repository.applyTask(generation,task);
        for(Authorization authorization:snapshot.authorizations()) repository.applyAuthorization(generation,authorization);
        return snapshot.watermark();
    }
    /** Exact same-watermark comparison and atomic pointer publication; shared fences prevent source drift. */
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public String verifyAndPublish(String generation,long tenant,Set<String> sources) {
        Global global=repository.lockGlobal(false); Tenant control=repository.lockTenant(tenant,true);
        Checkpoint checkpoint=repository.checkpoint(generation,tenant);
        if(checkpoint.globalCheckpoint()!=global.head()||checkpoint.tenantGlobalCheckpoint()!=global.head()
            ||checkpoint.tenantCheckpoint()!=control.head()) throw PROJECTION_NOT_CAUGHT_UP.exception();
        if(!repository.generationSources(generation,tenant,global.registryRevision()).equals(sources)) throw SNAPSHOT_MISMATCH.exception();
        FrozenExport current=export(tenant,global,control,sources);
        String expected=collectionHash(current.tasks(),current.authorizations());
        // A complete formal scan proves absence; retained deletion/revocation versions remain in the ledger.
        String actual=collectionHash(repository.allTasks(generation,tenant).stream().filter(t->!t.tombstone()).toList(),
            repository.allAuthorizations(generation,tenant).stream().filter(a->!a.revoked()).toList());
        if(!expected.equals(actual)) throw PROJECTION_BATCH_INVALID.exception();
        evidence.requireApproved(tenant,generation,current.watermark(),actual);
        if(!checkpoint.complete()) repository.setComplete(checkpoint);
        repository.publish(control,generation); return actual;
    }
    /** The same gate is used for a manually requested rollback to a retained generation. */
    public String collectionHash(List<Task> tasks,List<Authorization> authorizations) {
        List<String> values=new ArrayList<>(); tasks.forEach(t->values.add("T:"+codec.encode(t)));
        authorizations.forEach(a->values.add("A:"+codec.encode(a))); Collections.sort(values); return codec.hash(values);
    }

    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public String verify(String generation,long tenant,Set<String> sources) {
        Global global=repository.lockGlobal(false);Tenant control=repository.lockTenant(tenant,true);
        Checkpoint checkpoint=repository.checkpoint(generation,tenant);
        if(checkpoint.globalCheckpoint()!=global.head()||checkpoint.tenantGlobalCheckpoint()!=global.head()||checkpoint.tenantCheckpoint()!=control.head())
            throw PROJECTION_NOT_CAUGHT_UP.exception();
        if(!repository.generationSources(generation,tenant,global.registryRevision()).equals(sources)) throw SNAPSHOT_MISMATCH.exception();
        FrozenExport current=export(tenant,global,control,sources);
        String expected=collectionHash(current.tasks(),current.authorizations());
        String actual=collectionHash(repository.allTasks(generation,tenant).stream().filter(t->!t.tombstone()).toList(),repository.allAuthorizations(generation,tenant).stream().filter(a->!a.revoked()).toList());
        if(!expected.equals(actual)) throw PROJECTION_BATCH_INVALID.exception();
        return actual;
    }
}
