package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionHistoryTest {
    final ProjectionRepository controls=mock(ProjectionRepository.class);
    final ProjectionHistoryRepository history=mock(ProjectionHistoryRepository.class);
    final Evidence evidence=mock(Evidence.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final Request request=request(Target.OUTBOX,Scope.TENANT,2,null);
    final Global global=new Global(1,0,"r");final Tenant tenant=new Tenant(2,1,0,"active");
    final Observation observation=ProjectionHistory.observed(request,1000);
    static Request request(Target target,Scope scope,long tenant,String generation) {return new Request(target,scope,tenant,generation,target==Target.OUTBOX?1L:null,60L,90L,5L,"history cleanup","approval",new Budget(100,1000,100000L,100));}
    Snapshot snapshot(Observation obs) {return new Snapshot(global,List.of(tenant),0,0,List.of(new Consumer(2,"active",1,1,1,true,"active",1,0,"r",ProjectionCodec.hashText("[]"),0)),1,"batch",ProjectionCodec.hashText("batch"),obs,List.of(new Item("OUTBOX",List.of("event","TENANT",2L,1L,"batch",0),ProjectionCodec.hashText("event"),ProjectionCodec.hashText("row"),1,null,List.of())));}
    @BeforeEach void setup() {
        when(controls.lockGlobal(false)).thenReturn(global);when(controls.lockGlobal(true)).thenReturn(global);
        when(controls.lockTenant(2,false)).thenReturn(tenant);when(controls.lockTenant(2,true)).thenReturn(tenant);
        when(history.snapshot(any(),anyString(),any(),anyList(),anyBoolean())).thenReturn(snapshot(observation));
    }
    ProjectionHistoryService service(long now) {return new ProjectionHistoryService(controls,history,evidence,mapper,Clock.fixed(Instant.ofEpochMilli(now),ZoneOffset.UTC));}
    @Test void parametersAndArithmeticCannotReceiveImplicitDefaults() {
        for(Long missing:new Long[]{null,0L,-1L})assertThrows(IllegalArgumentException.class,()->new Request(Target.OUTBOX,Scope.GLOBAL,0,null,1L,missing,90L,0L,"reason","ref",request.budget()));
        assertThrows(IllegalArgumentException.class,()->new Budget(null,1,1L,1));
        assertThrows(IllegalArgumentException.class,()->new Budget(1,1,1L,100001));
        assertThrows(IllegalArgumentException.class,()->request(Target.VERSION,Scope.GLOBAL,0,"g"));
        assertThrows(IllegalArgumentException.class,()->new Request(Target.OUTBOX,Scope.GLOBAL,0,null,1L,1L,1L,null,"reason","ref",request.budget()));
        assertThrows(ArithmeticException.class,()->new Request(Target.OUTBOX,Scope.GLOBAL,0,null,1L,Long.MAX_VALUE,1L,0L,"reason","ref",request.budget()));
    }
    @Test void observationStartsNowAndIncludesClockBoundAndNeverRunsInPreview() {
        when(history.snapshot(any(),anyString(),any(),anyList(),anyBoolean())).thenReturn(snapshot(null));
        Plan plan=service(1000).observe(request);assertEquals(401000,plan.candidate().snapshot().observation().purgeNotBeforeMillis());
        verify(history).observe(request,snapshot(null),observation);
        assertThrows(IllegalStateException.class,()->service(999999).preview(request));verify(history,never()).purge(any(),any());
    }
    @Test void dueBoundaryIsInclusiveAndPreviewHasZeroWrites() {
        assertThrows(IllegalStateException.class,()->service(400999).preview(request));
        Plan plan=service(401000).preview(request);assertEquals(plan.planHash(),service(401001).preview(request).planHash());
        verify(history,never()).observe(any(),any(),any());verify(history,never()).register(any(),any());verifyNoInteractions(evidence);
    }
    @Test void stalePlansChangedWindowsOrApprovalRefuseDeletion() {
        Plan plan=service(401000).preview(request);
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,2,0,"active"));
        when(history.snapshot(any(),anyString(),any(),anyList(),eq(true))).thenReturn(new Snapshot(global,List.of(new Tenant(2,2,0,"active")),0,0,List.of(),1,"batch",ProjectionCodec.hashText("batch"),observation,snapshot(observation).items()));
        assertThrows(IllegalStateException.class,()->service(401000).purge(request,plan.planHash()));
        when(history.snapshot(any(),anyString(),any(),anyList(),eq(true))).thenReturn(snapshot(observation));
        doThrow(new IllegalStateException("no approval")).when(evidence).requireApproved(any(),anyLong());
        assertThrows(IllegalStateException.class,()->service(401000).purge(request,plan.planHash()));
        assertThrows(IllegalStateException.class,()->service(999).preview(request));
        Request changed=new Request(Target.OUTBOX,Scope.TENANT,2,null,1L,30L,90L,5L,"history cleanup","approval",request.budget());
        assertThrows(IllegalStateException.class,()->service(401000).preview(changed));verify(history,never()).purge(any(),any());
    }
    @Test void approvedExecutionRegeneratesLockedPlanAndUsesSameExactSnapshot() {
        Plan plan=service(401000).preview(request);clearInvocations(controls,history,evidence);
        assertEquals(plan,service(401000).purge(request,plan.planHash()));
        var order=inOrder(controls,history,evidence);
        order.verify(controls).lockGlobal(false);order.verify(controls).lockTenant(2,true);
        order.verify(history).snapshot(request,"preview",global,List.of(tenant),true);
        order.verify(evidence).requireApproved(new ApprovalScope("purge",request,plan.planHash()),401000);
        order.verify(history).purge(request,snapshot(observation));
    }
    @Test void globalCleanupLocksCompleteControlInventoryAndVersionChecksRetirement() {
        Request globalRequest=request(Target.OUTBOX,Scope.GLOBAL,0,null);
        when(history.controlTenants(100)).thenReturn(List.of(2L,3L));when(controls.lockTenant(3,true)).thenReturn(new Tenant(3,1,0,"active"));
        service(1000).observe(globalRequest);
        var order=inOrder(controls,history);order.verify(controls).lockGlobal(true);order.verify(history).controlTenants(100);
        order.verify(controls).lockTenant(2,true);order.verify(controls).lockTenant(3,true);
        Request version=request(Target.VERSION,Scope.TENANT,2,"g");
        doThrow(new IllegalStateException("retired")).when(controls).requireRetained("g",2);
        assertThrows(IllegalStateException.class,()->service(1000).observe(version));
        verify(history,never()).snapshot(eq(version),anyString(),any(),anyList(),anyBoolean());
    }
    @Test void actualSpringTransactionRollsBackWhenAnyDeleteOrFloorWriteFails() {
        var manager=new ProjectionRetentionTest.Manager();var factory=new ProxyFactory(service(401000));factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        var proxy=(ProjectionHistoryService)factory.getProxy();Plan plan=proxy.preview(request);
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());throw new IllegalStateException("second row delete failed");}).when(history).purge(any(),any());
        assertThrows(IllegalStateException.class,()->proxy.purge(request,plan.planHash()));
        assertEquals(1,manager.commits);assertEquals(1,manager.rollbacks);assertFalse(manager.readOnly);
    }
}
