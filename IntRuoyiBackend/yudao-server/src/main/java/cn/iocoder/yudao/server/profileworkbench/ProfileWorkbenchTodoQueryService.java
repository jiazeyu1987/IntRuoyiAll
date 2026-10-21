package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.service.profileworkbench.*;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="profile.workbench.query-engine",havingValue="s3",matchIfMissing=true)
public class ProfileWorkbenchTodoQueryService implements WorkbenchQueryApplication {
    @Resource private WorkbenchSourceRegistry registry;
    @Resource private ProfileWorkbenchTodoReadTransaction reader;
    @Resource private WorkbenchQueryTokens tokens;
    @Resource private WorkbenchVisibilityContextInitializer initializer;
    @Resource private WorkbenchVisibilityWriteTransaction writer;
    private record Identity(long tenant, long user) { }
    private Identity identity() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Workbench authorization requires no ambient transaction");
        Long user=SecurityFrameworkUtils.getLoginUserId(), tenant=TenantContextHolder.getTenantId();
        if (user==null || user<=0 || tenant==null || tenant<0) throw IDENTITY_CONTEXT_MISSING.exception();
        return new Identity(tenant,user);
    }
    private void authorize(Identity identity,List<String> ids) {
        for (String id:ids) {
            boolean authorized;
            try { authorized=registry.require(id).isAuthorized(identity.user,identity.tenant); }
            catch (ServiceException failure) { throw failure; }
            catch (RuntimeException failure) { throw SOURCE_QUERY_FAILED.exception(failure); }
            if (!authorized) throw SOURCE_FORBIDDEN.exception();
        }
    }
    public WorkbenchSourceRegistry.Directory directory() {
        Identity identity=identity(); return registry.directory(identity.user,identity.tenant);
    }
    public WorkbenchContextResponse context(WorkbenchContextRequest request) {
        long start=System.nanoTime(); Identity identity=identity();
        if (request==null || request.querySpec()==null) throw INVALID_QUERY.exception();
        WorkbenchQueryTokens.epoch(request.contextEpoch()); registry.requireRevision(request.registryRevision());
        List<String> ids=registry.normalize(request.sources()); authorize(identity,ids);
        WorkbenchContextRequest normalized=new WorkbenchContextRequest(request.registryRevision(),ids,request.contextEpoch(),request.querySpec().normalized());
        ProfileWorkbenchTodoReadTransaction.checkBudget(start);
        initializer.initialize(identity.tenant,identity.user); // Ordinary write proxy commits before readonly reader.
        return reader.issue(identity.tenant,identity.user,normalized,ids,start);
    }
    private WorkbenchQueryTokens.Claim claim(String token,String epoch,Identity identity) {
        var claim=tokens.verify(token,epoch,identity.tenant,identity.user);
        registry.requireRevision(claim.registryRevision());
        if (!registry.normalize(claim.sources()).equals(claim.sources())) throw QUERY_TOKEN_INVALID.exception();
        authorize(identity,claim.sources()); return claim;
    }
    public ProfileWorkbenchTodoPageRespVO page(String token,String epoch,int page,int size) {
        long start=System.nanoTime(); Identity identity=identity();
        return reader.page(token,claim(token,epoch,identity),page,size,start);
    }
    public ProfileWorkbenchTodoCountRespVO count(String token,String epoch) {
        long start=System.nanoTime(); Identity identity=identity();
        return reader.count(token,claim(token,epoch,identity),start);
    }
    public VisibilityResponse visibility(WorkbenchVisibilityRequest request) {
        Identity identity=identity(); if(request==null)throw INVALID_QUERY.exception();
        var claim=claim(request.queryVersion(),request.contextEpoch(),identity);
        String key=request.taskKey();
        if(key==null || key.isBlank() || key.length()>160 || !("hide".equals(request.operation()) || "restore".equals(request.operation()))) throw INVALID_QUERY.exception();
        int colon=key.indexOf(':'); if(colon<1 || colon==key.length()-1)throw INVALID_QUERY.exception();
        String sourceId=key.substring(0,colon),businessId=key.substring(colon+1);
        if(businessId.isBlank() || businessId.length()>80 || !claim.sources().contains(sourceId))throw TASK_NOT_ELIGIBLE.exception();
        SourceAdapter source=registry.require(sourceId);
        QueryContext q=claim.querySpec().context(identity.tenant,identity.user,claim.visibilityVersion()).partition(null);
        var result=writer.write(identity.tenant,identity.user,claim.visibilityVersion(),key,"hide".equals(request.operation()),()->{
            tokens.requireAlive(claim);
            // Recheck formal authority after the personal lock, in the ordinary write transaction (USE audit is writable).
            authorize(identity,List.of(sourceId));
            TaskSummary row=source.resolveVisibilityIdentity(q,businessId);
            tokens.requireAlive(claim); return row;
        });
        return new VisibilityResponse(result.changed(),result.visibilityVersion(),request.contextEpoch());
    }
}
