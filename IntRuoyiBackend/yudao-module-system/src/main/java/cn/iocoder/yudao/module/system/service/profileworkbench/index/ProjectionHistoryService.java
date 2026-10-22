package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;

@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionHistoryService {
    private final ProjectionRepository controls;
    private final ProjectionHistoryRepository history;
    private final Evidence evidence;
    private final ProjectionCodec codec;
    private final Clock clock;
    @Autowired public ProjectionHistoryService(ProjectionRepository controls,ProjectionHistoryRepository history,Evidence evidence,ObjectMapper mapper) {
        this(controls,history,evidence,mapper,Clock.systemUTC());
    }
    public ProjectionHistoryService(ProjectionRepository controls,ProjectionHistoryRepository history,Evidence evidence,ObjectMapper mapper,Clock clock) {
        this.controls=controls;this.history=history;this.evidence=evidence;this.codec=new ProjectionCodec(mapper);this.clock=clock;
    }
    private Snapshot snapshot(Request request,String operation,boolean exclusive) {
        if(request.target()==Target.OUTBOX&&request.sequence()==null)throw new IllegalArgumentException("Explicit batch sequence required");
        Global global=controls.lockGlobal(request.scope()==Scope.GLOBAL);
        List<Tenant> tenants=new ArrayList<>();
        if(request.scope()==Scope.GLOBAL) {
            for(long tenant:history.controlTenants(request.budget().maxConsumers()))tenants.add(controls.lockTenant(tenant,exclusive));
        } else tenants.add(controls.lockTenant(request.tenant(),exclusive));
        if(request.target()==Target.VERSION)controls.requireRetained(request.generation(),request.tenant());
        return history.snapshot(request,operation,global,tenants,exclusive);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public void initialize(Request request) {
        if(request.target()!=Target.OUTBOX)throw new IllegalArgumentException("Scope initialization is only for outbox");
        controls.lockGlobal(request.scope()==Scope.GLOBAL);
        if(request.scope()==Scope.TENANT)controls.lockTenant(request.tenant(),true);
        evidence.requireApproved(new ApprovalScope("init",request,null),clock.millis());
        history.initializeFloor(request.scope(),request.tenant());
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan register(Request request) {
        Snapshot snapshot=snapshot(request,"register",true);
        evidence.requireApproved(new ApprovalScope("register",request,null),clock.millis());
        history.register(request,snapshot);return plan(request,snapshot);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan observe(Request request) {
        Snapshot snapshot=snapshot(request,"observe",true);long now=clock.millis();
        if(snapshot.observation()!=null)snapshot.observation().matches(request,now);
        evidence.requireApproved(new ApprovalScope("observe",request,null),now);
        Observation observation=snapshot.observation()==null?observed(request,now):snapshot.observation();
        history.observe(request,snapshot,observation);
        return plan(request,new Snapshot(snapshot.global(),snapshot.tenants(),snapshot.globalFloor(),snapshot.tenantFloor(),snapshot.consumers(),snapshot.sequence(),snapshot.batchKey(),snapshot.identityHash(),observation,snapshot.items()));
    }
    private Plan plan(Request request,Snapshot snapshot) {Candidate c=new Candidate(request,snapshot);return new Plan(c,codec.hash(c));}
    private Plan preview(Request request,boolean exclusive) {
        Snapshot snapshot=snapshot(request,"preview",exclusive);
        if(snapshot.observation()==null)throw new IllegalStateException("Explicit observation required before payload cleanup");
        long now=clock.millis();snapshot.observation().matches(request,now);
        if(now<snapshot.observation().purgeNotBeforeMillis())throw new IllegalStateException("History protection window has not elapsed");
        return plan(request,snapshot);
    }
    // SELECT only; shared locking reads require a READ WRITE transaction.
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan preview(Request request) {return preview(request,false);}
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public Plan purge(Request request,String planHash) {
        ProjectionRetention.hash(planHash);Plan plan=preview(request,true);
        if(!plan.planHash().equals(planHash))throw new IllegalStateException("History plan is stale; preview again");
        long now=clock.millis();plan.candidate().snapshot().observation().matches(request,now);
        if(now<plan.candidate().snapshot().observation().purgeNotBeforeMillis())throw new IllegalStateException("Clock moved backwards before history cleanup");
        evidence.requireApproved(new ApprovalScope("purge",request,planHash),now);
        history.purge(request,plan.candidate().snapshot());return plan;
    }
}
