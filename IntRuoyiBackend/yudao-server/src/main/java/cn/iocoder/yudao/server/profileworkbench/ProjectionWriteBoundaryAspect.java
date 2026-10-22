package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTransactionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTxCApplicationService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTxCProducer;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRepository;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import javax.sql.DataSource;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Composition-root advice runs before ordinary business transaction advice, including GET recovery roots. */
@Aspect @Component @Order(-1000) @DS("master")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionWriteBoundaryAspect {
    private static final Set<String> GLOBAL_OWNERS=Set.of("MenuServiceImpl","TenantServiceImpl","TenantPackageServiceImpl","SystemConfigPackageServiceImpl","SystemEntitlementPolicyServiceImpl");
    // These current owners only deliver new unbound notices or append independent audit/delivery facts.
    // A future write to an indexed formal table still fails the Mapper guard.
    private static final Set<String> NON_INDEX_ROOTS=Set.of("DccControlledFileMessageDeliveryService","DccPublicationNotificationDispatchOrchestrator",
        "DccPublicationNotificationTransactionServiceImpl","DccElectronicSignatureFailureAuditServiceImpl","DccControlledFileAccessAuditService",
        "DccControlledFileFinalizationFailureService","MesActiveOrderHandoffDeliveryTransactionService","MesReleaseTaskNotificationPlatformSender",
        "MesIndependentBatchPrerequisiteReceiptServiceImpl","NotifySendServiceImpl");
    // Current user queries have been traced to reads only; lockUserForSessionMutation
    // deliberately belongs to its caller's existing session transaction.
    private static final Set<String> USER_READS=Set.of("getUserByUsername","getUserByMobile","getUserPage","getUser",
        "getUserListByDeptIds","getUserListByPostIds","getUserList","validateUserList","getUserListByNickname",
        "getGenericAccountUserList","validateOldPassword","getUserListByStatus","isPasswordMatch","lockUserForSessionMutation");
    private final ProjectionRepository repository;
    private final ObjectProvider<ProjectionExportRegistry> exports;
    private final TransactionTemplate transactions;
    private final TransactionTemplate traceTransactions;
    private final DataSource dataSource;
    public ProjectionWriteBoundaryAspect(ProjectionRepository repository,ObjectProvider<ProjectionExportRegistry> exports,PlatformTransactionManager manager,DataSource dataSource) {
        this.repository=repository;this.exports=exports;this.dataSource=dataSource;
        transactions=new TransactionTemplate(manager);transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);transactions.setTimeout(10);
        traceTransactions=new TransactionTemplate(manager,transactions);
        traceTransactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Around("(execution(public * cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime.*(..)) || execution(public * cn.iocoder.yudao.module.mes.service..*(..)) || execution(public * cn.iocoder.yudao.module.dcc.service..*(..)) || execution(public * cn.iocoder.yudao.module.bpm.service..*(..)) || execution(public * cn.iocoder.yudao.module.showroom.workflow.service..*(..)) || execution(public * cn.iocoder.yudao.module.showroom.content.service..*(..)) || execution(public * cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl.reviewTask(..)) || execution(public * cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl.sign(..)) || execution(public * cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl.signAuthorized(..)) || execution(public * cn.iocoder.yudao.module.system.service.auth.AdminAuthServiceImpl.resetPassword(..)) || execution(public * cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl.create*AccessToken(..)) || execution(public * cn.iocoder.yudao.module.system.service.permission..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.user..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.tenant..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.entitlement..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.configpackage..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.dept..*(..)) || execution(public * cn.iocoder.yudao.module.system.service.notify..*(..)) || @annotation(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchWriteBoundary) || @within(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchWriteBoundary))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // Tx-C orchestration runs after Tx-B commits. Only its actual independent
        // attempt owns a fence; the success and failure attempts must never share one.
        if(point.getTarget() instanceof MesProEdhrBatchTraceTxCApplicationService
            ||point.getTarget() instanceof MesProEdhrBatchTraceTxCProducer) return point.proceed();
        if(point.getTarget() instanceof MesProEdhrBatchTraceTransactionService
            &&point.getSignature().getName().equals("execute"))
            return traceWrite((TransactionCallback<?>)point.getArgs()[0]);
        if(point.getTarget() instanceof cn.iocoder.yudao.module.system.api.workbench.SourceAdapter) return point.proceed();
        if(NON_INDEX_ROOTS.contains(point.getTarget().getClass().getSimpleName())
            ||(point.getTarget().getClass().getSimpleName().equals("NotifyMessageServiceImpl")&&point.getSignature().getName().equals("createNotifyMessage"))
            ||(point.getTarget().getClass().getSimpleName().equals("DccControlledFileSourceGovernanceExecutionService")
                &&Set.of("recordGroupFailure","recordGroupBlocked").contains(point.getSignature().getName()))) return point.proceed();
        if(GLOBAL_OWNERS.contains(point.getTarget().getClass().getSimpleName())
            &&Set.of("get","select","valid","check","count","build","export").stream().anyMatch(prefix->point.getSignature().getName().startsWith(prefix)))
            return point.proceed();
        boolean global=GLOBAL_OWNERS.contains(point.getTarget().getClass().getSimpleName())
            ||(point.getTarget().getClass().getSimpleName().equals("SystemEntitlementServiceImpl")&&point.getSignature().getName().contains("Policy"));
        var method=AopUtils.getMostSpecificMethod(((MethodSignature)point.getSignature()).getMethod(),point.getTarget().getClass());
        WorkbenchWriteBoundary declared=AnnotatedElementUtils.findMergedAnnotation(method,WorkbenchWriteBoundary.class);
        if(declared==null) declared=AnnotatedElementUtils.findMergedAnnotation(point.getTarget().getClass(),WorkbenchWriteBoundary.class);
        if(declared!=null) global=declared.value()==Scope.GLOBAL;
        // Pure queries retain their caller's transaction and avoid a full tenant export.
        // Any unexpected relevant mutation is still rejected by the Mapper guard.
        if(declared==null && point.getTarget().getClass().getSimpleName().equals("AdminUserServiceImpl")
            &&USER_READS.contains(point.getSignature().getName())) return point.proceed();
        boolean jdbcImport=point.getTarget().getClass().getSimpleName().equals("SystemConfigPackageServiceImpl")
            &&Set.of("importPackage","replaceAllConfig").contains(point.getSignature().getName());
        if(point.getTarget().getClass().getSimpleName().equals("TenantServiceImpl")&&point.getSignature().getName().equals("createTenant"))
            return fenced(true,false,()->{
                Object result=point.proceed();
                if(!(result instanceof Long id)||id<0) throw new IllegalStateException("Formal new tenant identity missing");
                repository.initializeControls(id,repository.lockGlobal(true).registryRevision());
                return result;
            });
        return fenced(global,jdbcImport,point::proceed);
    }
    @FunctionalInterface public interface Business {Object run() throws Throwable;}
    public Object engineWrite(Business business) throws Throwable {return fenced(false,false,business);}
    private Object traceWrite(TransactionCallback<?> callback) throws Throwable {
        Objects.requireNonNull(callback);
        // A live fenced parent still owns the tenant lock; Tx-C belongs after its
        // commit, not inside that lock in a second connection.
        if(ProjectionWriteContext.CURRENT.get()!=null)
            throw new IllegalStateException("Tx-C requires the parent workbench fence to finish first");
        long tenant=TenantContextHolder.getRequiredTenantId();
        DynamicDataSourceContextHolder.push("master");
        try {
            return traceTransactions.execute(status->inTransaction(false,false,tenant,()->callback.doInTransaction(status)));
        } catch(BusinessFailure failure) {throw failure.getCause();}
        finally {DynamicDataSourceContextHolder.poll();}
    }
    private Object fenced(boolean global,boolean jdbcImport,Business business) throws Throwable {
        ProjectionWriteContext held=ProjectionWriteContext.CURRENT.get();
        if(held!=null) {
            if(global&&held.scope!=Scope.GLOBAL) {held.failed=true;throw new IllegalStateException("Global root nested under tenant workbench fence");}
            return business.run();
        }
        Long tenant=TenantContextHolder.getTenantId();
        // Public non-tenant reads retain their original behavior; any attempted relevant write is rejected by the Mapper guard.
        if(!global&&tenant==null) return business.run();
        if(TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Workbench root reached after business transaction started");
        DynamicDataSourceContextHolder.push("master");
        try {
            return transactions.execute(status->inTransaction(global,jdbcImport,tenant,business));
        } catch(BusinessFailure failure) {throw failure.getCause();}
        finally {DynamicDataSourceContextHolder.poll();}
    }
    private Object inTransaction(boolean global,boolean jdbcImport,Long tenant,Business business) {
        Global head=repository.lockGlobal(global);
        Tenant control=global?null:repository.lockTenant(tenant,true);
        FrozenExport before=global?null:exports.getObject().export(tenant,head,control);
        ProjectionWriteContext context=new ProjectionWriteContext(global?Scope.GLOBAL:Scope.TENANT,global?0:tenant,DataSourceUtils.getConnection(dataSource));
        ProjectionWriteContext.CURRENT.set(context);
        try {
            Object result;
            try {result=business.run();} catch(Throwable failure) {throw new BusinessFailure(failure);}
            if(context.failed) throw new IllegalStateException("A formal write failed inside the complete projection batch");
            if(!context.changed&&!jdbcImport) return result;
            long next=Math.addExact(global?head.head():control.head(),1);
            String key=(global?"GLOBAL":"TENANT:"+tenant)+":"+next;
            List<Event> events=new ArrayList<>();
            if(global) {
                events.add(new Event(UUID.randomUUID().toString(),Scope.GLOBAL,0,next,key,0,1,4,EventKind.CONTROL,null,null,head.registryRevision()));
            } else {
                FrozenExport after=exports.getObject().export(tenant,head,new Tenant(tenant,next,control.securityEpoch(),control.activeGeneration()));
                List<WorkbenchCommitProtocol.Change> changes=exports.getObject().changes(before,after);
                if(changes.isEmpty()) events.add(new Event(UUID.randomUUID().toString(),Scope.TENANT,tenant,next,key,0,1,4,EventKind.CONTROL,null,null,head.registryRevision()));
                for(int i=0;i<changes.size();i++) {
                    var change=changes.get(i);events.add(new Event(UUID.randomUUID().toString(),Scope.TENANT,tenant,next,key,i,changes.size(),4,change.kind(),change.task(),change.authorization(),null));
                }
            }
            repository.append(new Batch(context.scope,context.tenant,next,key,events));
            if(global) repository.advanceGlobal(head,new Global(next,Math.addExact(head.securityEpoch(),1),head.registryRevision()));
            else repository.advanceTenant(control,new Tenant(tenant,next,context.securityChanged?Math.addExact(control.securityEpoch(),1):control.securityEpoch(),control.activeGeneration()));
            return result;
        } finally {ProjectionWriteContext.CURRENT.remove();}
    }
    private static final class BusinessFailure extends RuntimeException {BusinessFailure(Throwable cause){super(cause);}}
}
