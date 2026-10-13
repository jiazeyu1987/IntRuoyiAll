package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionRetentionTest {
    final ProjectionRepository controls=mock(ProjectionRepository.class);
    final ProjectionRetentionRepository repository=mock(ProjectionRetentionRepository.class);
    final Evidence evidence=mock(Evidence.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final Request request=new Request(2,"old",60L,90L,"replacement verified","approval-1");
    final Retirement retirement=new Retirement(request,State.RETIRED,1000,391000);
    final Metadata metadata=new Metadata(0,0,true,"r",ProjectionCodec.hashText("[]"));
    final Row row=new Row(List.of("task",1L,2L),ProjectionCodec.hashText("row"),null);
    final Selection selection=new Selection(Stage.AUDIENCE,List.of(row));
    @BeforeEach void setup() {
        when(controls.lockGlobal(false)).thenReturn(new Global(0,0,"r"));
        when(controls.lockTenant(2,false)).thenReturn(new Tenant(2,0,0,"active"));
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,0,0,"active"));
        when(repository.metadata(2,"old")).thenReturn(metadata);
        when(repository.lockRetirement(2,"old",false)).thenReturn(retirement);
        when(repository.lockRetirement(2,"old",true)).thenReturn(retirement);
        when(repository.nextBatch(eq(2L),eq("old"),anyInt(),anyBoolean())).thenReturn(selection);
    }
    ProjectionRetentionService service(long time) {
        return new ProjectionRetentionService(controls,repository,evidence,mapper,Clock.fixed(Instant.ofEpochMilli(time),ZoneOffset.UTC));
    }
    @Test void missingNonpositiveAndOverflowDurationsNeverReceiveDefaults() {
        for(Long value:new Long[]{null,0L,-1L}) {
            assertThrows(IllegalArgumentException.class,()->new Request(2,"old",value,90L,"reason","a"));
            assertThrows(IllegalArgumentException.class,()->new Request(2,"old",60L,value,"reason","a"));
        }
        assertThrows(ArithmeticException.class,()->new Request(2,"old",Long.MAX_VALUE,90L,"reason","a"));
        assertThrows(IllegalArgumentException.class,()->new Request(2,"old",60L,90L," ","a"));
        assertThrows(IllegalArgumentException.class,()->new Request(-1,"old",60L,90L,"reason","a"));
        assertThrows(IllegalArgumentException.class,()->new Request(2,"bad.sql",60L,90L,"reason","a"));
    }
    @Test void retirementTakesLargerWindowAndTokenBoundFromServiceClock() {
        when(repository.lockRetirement(2,"old",true)).thenReturn(null);
        assertEquals(retirement,service(1000).retire(request));
        var order=inOrder(controls,repository,evidence);
        order.verify(controls).lockGlobal(false);order.verify(controls).lockTenant(2,true);
        order.verify(repository).lockRetirement(2,"old",true);order.verify(repository).metadata(2,"old");
        order.verify(evidence).requireApproved(new ApprovalScope("retire",request,null,null),false,1000);
        order.verify(repository).insert(retirement);
        assertEquals(391000,retirement.purgeNotBeforeMillis());
        assertEquals(300,TOKEN_MAX_SECONDS);
        assertThrows(ArithmeticException.class,()->service(Long.MAX_VALUE).retire(request));
    }
    @Test void activeGenerationCannotRetireAndIncompleteCandidateRequiresExplicitAbandonment() {
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,0,0,"old"));
        assertThrows(IllegalStateException.class,()->service(1000).retire(request));verifyNoInteractions(repository,evidence);
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,0,0,"active"));
        when(repository.lockRetirement(2,"old",true)).thenReturn(null);
        when(repository.metadata(2,"old")).thenReturn(new Metadata(0,0,false,"r","hash"));
        doThrow(new IllegalStateException("abandon approval absent")).when(evidence).requireApproved(any(),eq(true),anyLong());
        assertThrows(IllegalStateException.class,()->service(1000).retire(request));verify(repository,never()).insert(any());
    }
    @Test void retirementRetryKeepsOriginalTimesAndConflictingParametersFail() {
        assertEquals(retirement,service(2000).retire(request));verify(repository,never()).insert(any());verify(repository,never()).metadata(anyLong(),anyString());
        var changed=new Request(2,"old",30L,90L,"replacement verified","approval-1");
        assertThrows(IllegalStateException.class,()->service(2000).retire(changed));
        assertThrows(IllegalStateException.class,()->service(999).retire(request));
        when(repository.lockRetirement(2,"old",true)).thenReturn(new Retirement(request,State.PURGED,1000,391000));
        assertThrows(IllegalStateException.class,()->service(400000).retire(request));
    }
    @Test void windowIsInclusiveAtDueTimeAndPreviewHasZeroWrites() {
        assertThrows(IllegalStateException.class,()->service(390999).preview(request,1));
        Plan plan=service(391000).preview(request,1);assertTrue(plan.hasMore());
        assertEquals(plan.planHash(),service(391001).preview(request,1).planHash());
        verify(repository,never()).insert(any());verify(repository,never()).deleteRows(anyLong(),anyString(),any());verify(repository,never()).finish(any());verifyNoInteractions(evidence);
    }
    @ParameterizedTest @ValueSource(ints={0,-1,1001})
    void invalidBatchStopsBeforeAnyRepositoryAction(int size) {
        assertThrows(IllegalArgumentException.class,()->service(391000).preview(request,size));verifyNoInteractions(controls,repository,evidence);
    }
    @Test void missingBatchAndInvalidHashFailBeforeLocks() {
        assertThrows(IllegalArgumentException.class,()->service(391000).preview(request,null));
        assertThrows(IllegalArgumentException.class,()->service(391000).purge(request,1,null));verifyNoInteractions(controls,repository,evidence);
    }
    @ParameterizedTest @ValueSource(ints={1,1000})
    void executionLocksRegeneratesAndDeletesExactlyOneApprovedBatch(int size) {
        Plan plan=service(391000).preview(request,size);clearInvocations(controls,repository,evidence);
        assertEquals(plan,service(391000).purge(request,size,plan.planHash()));
        var order=inOrder(controls,repository,evidence);
        order.verify(controls).lockGlobal(false);order.verify(controls).lockTenant(2,true);
        order.verify(repository).lockRetirement(2,"old",true);order.verify(repository).metadata(2,"old");
        order.verify(repository).nextBatch(2,"old",size,true);
        order.verify(evidence).requireApproved(new ApprovalScope("purge",request,size,plan.planHash()),false,391000);
        order.verify(repository).deleteRows(2,"old",selection);verify(repository,never()).finish(any());
    }
    @Test void changedRowsHeadsScopeOrApprovalRejectWithoutDeletion() {
        Plan plan=service(391000).preview(request,1);
        when(repository.nextBatch(2,"old",1,true)).thenReturn(new Selection(Stage.AUDIENCE,List.of(new Row(List.of("other",1L,2L),ProjectionCodec.hashText("other"),null))));
        assertThrows(IllegalStateException.class,()->service(391000).purge(request,1,plan.planHash()));
        when(repository.nextBatch(2,"old",1,true)).thenReturn(selection);
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,1,0,"active"));
        assertThrows(IllegalStateException.class,()->service(391000).purge(request,1,plan.planHash()));
        when(controls.lockTenant(2,true)).thenReturn(new Tenant(2,0,0,"active"));
        doThrow(new IllegalStateException("approval missing")).when(evidence).requireApproved(any(),anyBoolean(),anyLong());
        assertThrows(IllegalStateException.class,()->service(391000).purge(request,1,plan.planHash()));
        verify(repository,never()).deleteRows(anyLong(),anyString(),any());
    }
    @Test void missingPurgedAndActiveLedgerCannotCreateAnEmptySuccess() {
        when(repository.lockRetirement(2,"old",false)).thenReturn(null);
        assertThrows(IllegalStateException.class,()->service(391000).preview(request,1));
        when(repository.lockRetirement(2,"old",false)).thenReturn(new Retirement(request,State.PURGED,1000,391000));
        assertThrows(IllegalStateException.class,()->service(391000).preview(request,1));
        when(controls.lockTenant(2,false)).thenReturn(new Tenant(2,0,0,"old"));
        assertThrows(IllegalStateException.class,()->service(391000).preview(request,1));verify(repository,never()).nextBatch(anyLong(),anyString(),anyInt(),anyBoolean());
    }
    @Test void metadataIsOnlyFinalizedAfterFourTablesAreEmpty() {
        when(repository.nextBatch(eq(2L),eq("old"),anyInt(),anyBoolean())).thenReturn(new Selection(Stage.METADATA,List.of()));
        Plan plan=service(391000).preview(request,1);assertFalse(plan.hasMore());
        service(391000).purge(request,1,plan.planHash());verify(repository).finish(retirement);verify(repository,never()).deleteRows(anyLong(),anyString(),any());
    }
    static class Manager extends AbstractPlatformTransactionManager {
        int commits,rollbacks;boolean readOnly;
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object tx,TransactionDefinition definition) {
            assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,definition.getIsolationLevel());
            assertEquals(10,definition.getTimeout());readOnly=definition.isReadOnly();
        }
        protected void doCommit(DefaultTransactionStatus status){commits++;}
        protected void doRollback(DefaultTransactionStatus status){rollbacks++;}
    }
    @Test void actualSpringProxyUsesLockCapableZeroWritePreviewAndRollsBackDeleteFailure() {
        Manager manager=new Manager();ProxyFactory factory=new ProxyFactory(service(391000));factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        var proxy=(ProjectionRetentionService)factory.getProxy();
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return selection;})
            .when(repository).nextBatch(2,"old",1,false);
        Plan plan=proxy.preview(request,1);assertFalse(manager.readOnly);assertEquals(1,manager.commits);
        doAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());throw new IllegalStateException("database failure");})
            .when(repository).deleteRows(2,"old",selection);
        assertThrows(IllegalStateException.class,()->proxy.purge(request,1,plan.planHash()));
        assertFalse(manager.readOnly);assertEquals(1,manager.commits);assertEquals(1,manager.rollbacks);verify(repository,never()).finish(any());
    }
}
