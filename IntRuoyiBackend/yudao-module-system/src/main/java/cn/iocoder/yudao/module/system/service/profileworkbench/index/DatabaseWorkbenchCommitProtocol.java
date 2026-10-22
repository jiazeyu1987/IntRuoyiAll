package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchCommitProtocol;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.util.*;
import java.util.function.Supplier;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class DatabaseWorkbenchCommitProtocol implements WorkbenchCommitProtocol {
    private final ProjectionRepository repository;
    private final TransactionTemplate transactions;
    private final Object transactionResource=new Object();
    public DatabaseWorkbenchCommitProtocol(ProjectionRepository repository,PlatformTransactionManager manager) {
        this.repository=repository; transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transactions.setTimeout(10);
    }
    private record Held(Scope scope,long tenantId) { }
    private <T>T transaction(Scope scope,long tenant,Supplier<T> body) {
        if(TransactionSynchronizationManager.isActualTransactionActive()) {
            Held held=(Held)TransactionSynchronizationManager.getResource(transactionResource);
            if(held==null) throw new IllegalStateException("Workbench fence must be entered before the business transaction");
            if(held.scope()!=scope||held.tenantId()!=tenant) throw new IllegalStateException("Nested workbench scope/lock-order change");
            // Nested callers participate in the outer batch; they must not independently advance a head.
            throw new IllegalStateException("Nested commit protocol must contribute to the root's complete fact collection");
        }
        return transactions.execute(status->{
            TransactionSynchronizationManager.bindResource(transactionResource,new Held(scope,tenant));
            try { return body.get(); }
            finally { TransactionSynchronizationManager.unbindResource(transactionResource); }
        });
    }
    @Override public <T>T tenantWrite(long tenant,boolean securityChanged,Supplier<T> business,Supplier<List<Change>> facts) {
        if(tenant<0||business==null||facts==null) throw INVALID_QUERY.exception();
        return transaction(Scope.TENANT,tenant,()->{
            repository.lockGlobal(false); Tenant current=repository.lockTenant(tenant,true);
            T result=business.get(); List<Change> changes=List.copyOf(facts.get());
            if(changes.isEmpty()) throw PROJECTION_BATCH_INVALID.exception();
            long next=Math.addExact(current.head(),1); String batch="TENANT:"+tenant+":"+next;
            List<Event> events=new ArrayList<>();
            for(int i=0;i<changes.size();i++) {
                Change c=changes.get(i); events.add(new Event(UUID.randomUUID().toString(),Scope.TENANT,tenant,next,batch,i,changes.size(),4,c.kind(),c.task(),c.authorization(),null));
            }
            repository.append(new Batch(Scope.TENANT,tenant,next,batch,events));
            repository.advanceTenant(current,new Tenant(tenant,next,securityChanged?Math.addExact(current.securityEpoch(),1):current.securityEpoch(),current.activeGeneration()));
            return result;
        });
    }
    @Override public <T>T globalWrite(boolean securityChanged,String revision,Supplier<T> business) {
        text(revision,128); Objects.requireNonNull(business);
        return transaction(Scope.GLOBAL,0,()->{
            Global current=repository.lockGlobal(true); T result=business.get(); long next=Math.addExact(current.head(),1);
            String key="GLOBAL:"+next;
            Event event=new Event(UUID.randomUUID().toString(),Scope.GLOBAL,0,next,key,0,1,4,EventKind.CONTROL,null,null,revision);
            repository.append(new Batch(Scope.GLOBAL,0,next,key,List.of(event)));
            repository.advanceGlobal(current,new Global(next,securityChanged?Math.addExact(current.securityEpoch(),1):current.securityEpoch(),revision));
            return result;
        });
    }
}
