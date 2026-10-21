package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import cn.iocoder.yudao.module.system.service.profileworkbench.WorkbenchVisibilityWriteTransaction;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.query-engine",havingValue="s4")
public class IndexQueryTransaction {
    private final ProjectionRepository repository;
    private final WorkbenchVisibilityVersionMapper versions;
    private final WorkbenchVisibilityWriteTransaction visibility;
    private final WorkbenchSourceRegistry registry;
    private final IndexQueryTokens tokens;
    private final ProjectionCodec codec;
    public IndexQueryTransaction(ProjectionRepository repository,WorkbenchVisibilityVersionMapper versions,
                                 WorkbenchVisibilityWriteTransaction visibility,WorkbenchSourceRegistry registry,
                                 IndexQueryTokens tokens,ObjectMapper mapper) {
        this.repository=repository;this.versions=versions;this.visibility=visibility;this.registry=registry;this.tokens=tokens;codec=new ProjectionCodec(mapper);
    }
    private record Locked(Global global,Tenant tenant,long visibility) { }
    // MySQL READ ONLY forbids the shared locking reads used by this consistency fence.
    // Query endpoints still perform SELECT only inside a READ WRITE transaction.
    private Locked lock(long tenant,long user,boolean personalExclusive) {
        Global global=repository.lockGlobal(false); Tenant control=repository.lockTenant(tenant,false);
        if(control.activeGeneration()==null) throw PROJECTION_CONTEXT_MISSING.exception();
        ProjectionFence.caughtUp(global,control,repository.checkpoint(control.activeGeneration(),tenant));
        registry.requireRevision(global.registryRevision());
        Long version=personalExclusive?versions.exclusive(tenant,user):versions.shared(tenant,user);
        if(version==null||version<1) throw VISIBILITY_CONTEXT_MISSING.exception();
        return new Locked(global,control,version);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public void initialize(long tenant,long user) {
        repository.lockGlobal(false);repository.lockTenant(tenant,false);
        versions.initialize(tenant,user);
        Long value=versions.shared(tenant,user);if(value==null||value<1) throw VISIBILITY_CONTEXT_MISSING.exception();
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public WorkbenchSourceRegistry.Directory directory(long tenant,long user) {
        Locked locked=lock(tenant,user,false); Instant at=tokens.now();
        List<String> ids=repository.authorizedSources(locked.tenant.activeGeneration(),tenant,user,at);
        ids=registry.normalize(ids);
        Instant boundary=repository.nextAuthorizationBoundary(locked.tenant.activeGeneration(),tenant,user,
            repository.generationSources(locked.tenant.activeGeneration(),tenant,locked.global.registryRevision()).stream().toList(),at);
        List<SourceDescriptor> descriptors=ids.stream().map(id->registry.require(id).descriptor()).toList();
        if(boundary!=null&&!tokens.now().isBefore(boundary)) throw VERSION_EXPIRED.exception();
        return new WorkbenchSourceRegistry.Directory(1,locked.global.registryRevision(),descriptors);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public WorkbenchContextResponse issue(long tenant,long user,WorkbenchContextRequest request,long start) {
        Locked locked=lock(tenant,user,false); registry.requireRevision(request.registryRevision());
        List<String> sources=registry.normalize(request.sources()); Instant at=tokens.now();
        Set<String> generationSources=repository.generationSources(locked.tenant.activeGeneration(),tenant,locked.global.registryRevision());
        if(!generationSources.containsAll(sources)) throw SOURCE_NOT_REGISTERED.exception();
        List<String> authorized=repository.authorizedSources(locked.tenant.activeGeneration(),tenant,user,at);
        if(!authorized.containsAll(sources)) throw SOURCE_FORBIDDEN.exception();
        Instant boundary=repository.nextAuthorizationBoundary(locked.tenant.activeGeneration(),tenant,user,generationSources.stream().sorted().toList(),at);
        WorkbenchQuerySpec spec=request.querySpec().normalized();
        ProjectionFence.Version version=new ProjectionFence.Version(locked.tenant.activeGeneration(),request.registryRevision(),sources,
            locked.global.head(),locked.tenant.head(),locked.global.securityEpoch(),locked.tenant.securityEpoch(),locked.visibility,at,boundary,codec.hash(spec));
        var issued=tokens.issue(tenant,user,request.contextEpoch(),spec,version);
        tokens.alive(issued.claim()); ProfileWorkbenchTodoReadTransaction.checkBudget(start);
        return new WorkbenchContextResponse(4,request.registryRevision(),sources,request.contextEpoch(),Long.toString(locked.visibility),issued.token(),issued.claim().expiresAt(),spec);
    }
    private ProjectionRepository.Query query(IndexQueryTokens.Claim claim,boolean personalExclusive) {
        Locked locked=lock(claim.tenant(),claim.user(),personalExclusive); tokens.alive(claim);
        List<String> sources=registry.normalize(claim.version().sources());
        ProjectionFence.current(claim.version(),locked.global,locked.tenant,locked.visibility,locked.global.registryRevision(),sources,codec.hash(claim.querySpec()),tokens.now());
        if(!repository.generationSources(locked.tenant.activeGeneration(),claim.tenant(),locked.global.registryRevision()).containsAll(sources)) throw SNAPSHOT_MISMATCH.exception();
        return new ProjectionRepository.Query(claim.version().generation(),claim.tenant(),claim.user(),sources,claim.version().authorizationAt(),claim.querySpec().context(claim.tenant(),claim.user(),locked.visibility));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public ProfileWorkbenchTodoCountRespVO count(String token,IndexQueryTokens.Claim claim,long start) {
        var query=query(claim,false);var totals=repository.totals(query);tokens.alive(claim);ProfileWorkbenchTodoReadTransaction.checkBudget(start);
        return new ProfileWorkbenchTodoCountRespVO(4,claim.version().registryRevision(),token,claim.contextEpoch(),Long.toString(claim.version().visibilityVersion()),totals.filtered(),totals.business(),totals.hidden(),LocalDateTime.ofInstant(tokens.now(),ZoneOffset.UTC));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public ProfileWorkbenchTodoPageRespVO page(String token,IndexQueryTokens.Claim claim,int pageNo,int pageSize,long start) {
        if(pageNo<1||pageSize<10||pageSize>100) throw INVALID_QUERY.exception();
        var query=query(claim,false);var totals=repository.totals(query);
        long max=totals.filtered()==0?1:(totals.filtered()-1)/pageSize+1;int effective=(int)Math.min(pageNo,max);
        long offset=Math.multiplyExact((long)effective-1,pageSize);int expected=(int)Math.min(pageSize,Math.max(0,totals.filtered()-offset));
        List<TaskSummary> rows=repository.page(query,offset,pageSize);
        if(rows.size()!=expected) throw SOURCE_CONTRACT_INVALID.exception();
        Comparator<TaskSummary> order=SortContract.comparator(query.spec());
        for(int i=1;i<rows.size();i++) if(order.compare(rows.get(i-1),rows.get(i))>=0) throw SOURCE_CONTRACT_INVALID.exception();
        tokens.alive(claim);ProfileWorkbenchTodoReadTransaction.checkBudget(start);
        return new ProfileWorkbenchTodoPageRespVO(4,claim.version().registryRevision(),token,claim.contextEpoch(),Long.toString(claim.version().visibilityVersion()),rows,totals.filtered(),totals.business(),totals.hidden(),effective,pageSize,LocalDateTime.ofInstant(tokens.now(),ZoneOffset.UTC));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public WorkbenchQueryApplication.VisibilityResponse visibility(IndexQueryTokens.Claim claim,String key,boolean hide) {
        var query=query(claim,true);
        var result=visibility.writeJoined(claim.tenant(),claim.user(),claim.version().visibilityVersion(),key,hide,()->{
            tokens.alive(claim);return repository.eligibleTask(query,key);
        });
        tokens.alive(claim);
        return new WorkbenchQueryApplication.VisibilityResponse(result.changed(),result.visibilityVersion(),claim.contextEpoch());
    }
}
