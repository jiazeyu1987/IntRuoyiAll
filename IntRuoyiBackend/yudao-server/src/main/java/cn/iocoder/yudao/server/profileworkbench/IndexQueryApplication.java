package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service
@ConditionalOnProperty(name="profile.workbench.query-engine",havingValue="s4")
public class IndexQueryApplication implements WorkbenchQueryApplication {
    private final IndexQueryTransaction transaction;
    private final IndexQueryTokens tokens;
    public IndexQueryApplication(IndexQueryTransaction transaction,IndexQueryTokens tokens) { this.transaction=transaction;this.tokens=tokens; }
    private record Identity(long tenant,long user) { }
    private Identity identity() {
        if(TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Workbench entry requires no ambient transaction");
        Long tenant=TenantContextHolder.getTenantId(),user=SecurityFrameworkUtils.getLoginUserId();
        if(tenant==null||tenant<0||user==null||user<1) throw IDENTITY_CONTEXT_MISSING.exception(); return new Identity(tenant,user);
    }
    @Override public WorkbenchSourceRegistry.Directory directory() {
        Identity id=identity();transaction.initialize(id.tenant,id.user);return transaction.directory(id.tenant,id.user);
    }
    @Override public WorkbenchContextResponse context(WorkbenchContextRequest request) {
        long start=System.nanoTime();Identity id=identity();
        if(request==null||request.querySpec()==null) throw INVALID_QUERY.exception();
        WorkbenchQueryTokens.epoch(request.contextEpoch());transaction.initialize(id.tenant,id.user);
        return transaction.issue(id.tenant,id.user,request,start);
    }
    @Override public ProfileWorkbenchTodoPageRespVO page(String token,String epoch,int page,int size) {
        long start=System.nanoTime();Identity id=identity();return transaction.page(token,tokens.verify(token,epoch,id.tenant,id.user),page,size,start);
    }
    @Override public ProfileWorkbenchTodoCountRespVO count(String token,String epoch) {
        long start=System.nanoTime();Identity id=identity();return transaction.count(token,tokens.verify(token,epoch,id.tenant,id.user),start);
    }
    @Override public VisibilityResponse visibility(WorkbenchVisibilityRequest request) {
        Identity id=identity();if(request==null||request.taskKey()==null||request.taskKey().isBlank()||request.taskKey().length()>160
            ||(!"hide".equals(request.operation())&&!"restore".equals(request.operation()))) throw INVALID_QUERY.exception();
        var claim=tokens.verify(request.queryVersion(),request.contextEpoch(),id.tenant,id.user);
        return transaction.visibility(claim,request.taskKey(),"hide".equals(request.operation()));
    }
}
