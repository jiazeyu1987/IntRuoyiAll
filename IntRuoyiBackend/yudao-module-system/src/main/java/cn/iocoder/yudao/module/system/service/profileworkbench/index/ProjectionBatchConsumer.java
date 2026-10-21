package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionBatchConsumer {
    /** Global security export must prove the full affected and unaffected tenant result under these fences. */
    public interface GlobalEffects {
        void apply(String generation,long tenantId,Batch batch,ProjectionRepository repository);
    }
    private final ProjectionRepository repository;
    private final GlobalEffects globalEffects;
    public ProjectionBatchConsumer(ProjectionRepository repository,GlobalEffects globalEffects) {
        this.repository=repository; this.globalEffects=globalEffects;
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public void consumeTenant(String generation,long tenantId) {
        repository.lockGlobal(false); Tenant tenant=repository.lockTenant(tenantId,true);
        Checkpoint checkpoint=repository.checkpoint(generation,tenantId);
        if(checkpoint.tenantCheckpoint()==tenant.head()) return;
        if(checkpoint.tenantCheckpoint()>tenant.head()) throw PROJECTION_BATCH_INVALID.exception();
        long next=Math.addExact(checkpoint.tenantCheckpoint(),1);
        List<Event> events=repository.events(Scope.TENANT,tenantId,next);
        Batch batch=complete(Scope.TENANT,tenantId,next,events);
        for(Event event:batch.events()) {
            if(!repository.recordEvent(generation,tenantId,event)) continue;
            switch(event.kind()) {
                case TASK -> repository.applyTask(generation,event.task());
                case AUTHORIZATION -> repository.applyAuthorization(generation,event.authorization());
                case CONTROL -> { /* Explicit no-task-effect acknowledgment for this tenant commit. */ }
            }
        }
        repository.advanceCheckpoint(checkpoint,new Checkpoint(generation,tenantId,checkpoint.globalCheckpoint(),checkpoint.tenantGlobalCheckpoint(),next,checkpoint.complete()));
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public void consumeGlobalForTenant(String generation,long tenantId) {
        Global global=repository.lockGlobal(false); repository.lockTenant(tenantId,true);
        Checkpoint checkpoint=repository.checkpoint(generation,tenantId);
        if(checkpoint.tenantGlobalCheckpoint()==global.head()) return;
        if(checkpoint.tenantGlobalCheckpoint()>global.head()) throw PROJECTION_BATCH_INVALID.exception();
        long next=Math.addExact(checkpoint.tenantGlobalCheckpoint(),1);
        Batch batch=complete(Scope.GLOBAL,0,next,repository.events(Scope.GLOBAL,0,next));
        // Even an unaffected tenant passes through the authoritative explicit effect acknowledgment.
        globalEffects.apply(generation,tenantId,batch,repository);
        for(Event event:batch.events()) repository.recordEvent(generation,tenantId,event);
        long globalCheckpoint=checkpoint.globalCheckpoint();
        if(globalCheckpoint<next) {
            if(globalCheckpoint+1!=next) throw PROJECTION_BATCH_INVALID.exception();
            globalCheckpoint=next;
        }
        repository.advanceCheckpoint(checkpoint,new Checkpoint(generation,tenantId,globalCheckpoint,next,checkpoint.tenantCheckpoint(),checkpoint.complete()));
    }
    public static Batch complete(Scope scope,long tenant,long sequence,List<Event> events) {
        if(events==null||events.isEmpty()) throw PROJECTION_NOT_CAUGHT_UP.exception();
        try { return new Batch(scope,tenant,sequence,events.get(0).batchKey(),events); }
        catch(IllegalArgumentException failure) { throw PROJECTION_BATCH_INVALID.exception(failure); }
    }
}
