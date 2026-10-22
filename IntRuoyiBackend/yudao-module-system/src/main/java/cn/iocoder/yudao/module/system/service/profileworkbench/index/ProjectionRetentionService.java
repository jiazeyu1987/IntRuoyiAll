package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;

/** One explicit tenant, one irreversible retirement or one approved bounded cleanup batch. */
@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionRetentionService {
    private final ProjectionRepository controls;
    private final ProjectionRetentionRepository repository;
    private final Evidence evidence;
    private final ProjectionCodec codec;
    private final Clock clock;
    @Autowired
    public ProjectionRetentionService(ProjectionRepository controls,ProjectionRetentionRepository repository,Evidence evidence,ObjectMapper mapper) {
        this(controls,repository,evidence,mapper,Clock.systemUTC());
    }
    public ProjectionRetentionService(ProjectionRepository controls,ProjectionRetentionRepository repository,Evidence evidence,ObjectMapper mapper,Clock clock) {
        this.controls=controls;this.repository=repository;this.evidence=evidence;this.codec=new ProjectionCodec(mapper);this.clock=clock;
    }
    private static void inactive(Request request,Tenant tenant) {
        if(tenant.tenantId()!=request.tenant()||request.generation().equals(tenant.activeGeneration()))
            throw new IllegalStateException("Active generation cannot be retired or purged");
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Retirement retire(Request request) {
        controls.lockGlobal(false);Tenant tenant=controls.lockTenant(request.tenant(),true);inactive(request,tenant);
        Retirement existing=repository.lockRetirement(request.tenant(),request.generation(),true);
        long now=clock.millis();
        if(existing!=null) {
            if(existing.state()!=State.RETIRED||!existing.request().equals(request)||now<existing.retiredAtMillis())
                throw new IllegalStateException("Retirement parameters are immutable or clock moved backwards");
            evidence.requireApproved(new ApprovalScope("retire",request,null,null),false,now);
            return existing;
        }
        Metadata metadata=repository.metadata(request.tenant(),request.generation());
        evidence.requireApproved(new ApprovalScope("retire",request,null,null),!metadata.complete(),now);
        Retirement retirement=new Retirement(request,State.RETIRED,now,Math.addExact(now,delay(request.retentionSeconds(),request.rollbackProtectionSeconds())));
        repository.insert(retirement);return retirement;
    }
    private Plan plan(Request request,int size,boolean exclusive) {
        Global global=controls.lockGlobal(false);Tenant tenant=controls.lockTenant(request.tenant(),exclusive);inactive(request,tenant);
        Retirement retirement=repository.lockRetirement(request.tenant(),request.generation(),exclusive);
        if(retirement==null||retirement.state()!=State.RETIRED||!retirement.request().equals(request))
            throw new IllegalStateException("Matching RETIRED ledger required");
        if(clock.millis()<retirement.purgeNotBeforeMillis())throw new IllegalStateException("Retention or rollback protection window has not elapsed");
        Metadata metadata=repository.metadata(request.tenant(),request.generation());
        Selection selection=repository.nextBatch(request.tenant(),request.generation(),size,exclusive);
        Candidate candidate=new Candidate(retirement,global,tenant,metadata,size,selection);
        return new Plan(candidate,codec.hash(candidate),selection.stage()!=Stage.METADATA);
    }
    // MySQL shared locking reads require a READ WRITE transaction; preview performs no DML.
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan preview(Request request,Integer batchSize) {
        return plan(request,batch(batchSize),false);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan purge(Request request,Integer batchSize,String planHash) {
        int size=batch(batchSize);hash(planHash);
        Plan plan=plan(request,size,true);
        if(!plan.planHash().equals(planHash))throw new IllegalStateException("Cleanup plan is stale; preview again");
        long approvedAt=clock.millis();
        if(approvedAt<plan.candidate().retirement().purgeNotBeforeMillis())
            throw new IllegalStateException("Clock moved backwards before cleanup approval");
        evidence.requireApproved(new ApprovalScope("purge",request,size,planHash),false,approvedAt);
        if(plan.candidate().selection().stage()==Stage.METADATA)repository.finish(plan.candidate().retirement());
        else repository.deleteRows(request.tenant(),request.generation(),plan.candidate().selection());
        return plan;
    }
}
