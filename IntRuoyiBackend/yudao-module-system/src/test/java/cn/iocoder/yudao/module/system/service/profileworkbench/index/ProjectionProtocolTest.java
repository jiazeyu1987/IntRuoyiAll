package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.workbench.ActionEnvelope;
import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchCommitProtocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionProtocolTest {
    private final Instant now=Instant.parse("2026-10-10T01:00:00Z");
    private Task task(long version,boolean tombstone) {
        return new Task(2,"SIX","SIX:9",version,tombstone,"9","type","source","detail","status",null,null,0,"0009",
            new ActionEnvelope("six.open",1,Map.of("id","9")),new Audience(AudienceKind.CANDIDATE_SET,Set.of(1L,11L),"six.policy",version));
    }
    private Event event(int ordinal,int count) {
        return new Event("e"+ordinal,Scope.TENANT,2,1,"TENANT:2:1",ordinal,count,4,EventKind.TASK,task(1,false),null,null);
    }
    @Test void completeBatchAllowsMultipleOrdinalsAndSortsThem() {
        Batch batch=new Batch(Scope.TENANT,2,1,"TENANT:2:1",List.of(event(1,2),event(0,2)));
        assertEquals(List.of(0,1),batch.events().stream().map(Event::ordinal).toList());
    }
    @Test void missingOrdinalNeverAdvancesACompleteBatch() {
        ServiceException failure=assertThrows(ServiceException.class,()->ProjectionBatchConsumer.complete(Scope.TENANT,2,1,List.of(event(0,2))));
        assertEquals(PROJECTION_BATCH_INVALID.code(),failure.getCode());
    }
    @Test void duplicateOrdinalAndEventIdentityFail() {
        assertThrows(IllegalArgumentException.class,()->new Batch(Scope.TENANT,2,1,"TENANT:2:1",List.of(event(0,2),event(0,2))));
    }
    @Test void wrongScopeTenantAndSequenceFail() {
        assertThrows(IllegalArgumentException.class,()->new Batch(Scope.TENANT,3,1,"TENANT:2:1",List.of(event(0,1))));
        assertThrows(IllegalArgumentException.class,()->new Batch(Scope.TENANT,2,2,"TENANT:2:1",List.of(event(0,1))));
    }
    @Test void missingWholeBatchIsExplicitlyNotCaughtUp() {
        assertEquals(PROJECTION_NOT_CAUGHT_UP.code(),assertThrows(ServiceException.class,()->ProjectionBatchConsumer.complete(Scope.TENANT,2,1,List.of())).getCode());
    }
    @Test void personAndQueueHaveDifferentOwnershipModels() {
        assertThrows(IllegalArgumentException.class,()->new Audience(AudienceKind.PERSON,Set.of(1L,11L),"p",1));
        assertThrows(IllegalArgumentException.class,()->new Audience(AudienceKind.TENANT_PERMISSION_QUEUE,Set.of(1L),"p",1));
        assertEquals(Set.of(),new Audience(AudienceKind.TENANT_PERMISSION_QUEUE,Set.of(),"p",1).members());
        assertEquals(Set.of(1L,11L),task(1,false).audience().members());
    }
    @Test void authorityUsesHalfOpenIntervalsAndDoesNotNeedAnExpiryJob() {
        Authorization a=new Authorization(2,"SIX",1,"","source:SIX",1,false,now,now.plusSeconds(1));
        assertFalse(a.effectiveAt(now.minusNanos(1)));assertTrue(a.effectiveAt(now));assertFalse(a.effectiveAt(now.plusSeconds(1)));
        assertFalse(new Authorization(2,"SIX",1,"","source:SIX",2,true,now,null).effectiveAt(now));
    }
    @Test void independentGrantKeysAndFutureGrantsArePreserved() {
        Authorization permanent=new Authorization(2,"SIX",1,"","source:SIX",1,false,Instant.EPOCH,null,"permanent:1");
        Authorization future=new Authorization(2,"SIX",1,"","source:SIX",1,false,now.plusSeconds(5),now.plusSeconds(10),"temporary:1");
        assertNotEquals(permanent.grantKey(),future.grantKey());assertTrue(permanent.effectiveAt(now));assertFalse(future.effectiveAt(now));
    }
    @Test void tombstoneCannotProduceAVisibleSummary() {
        assertThrows(IllegalStateException.class,()->task(2,true).summary());
    }
    private ProjectionFence.Version version() {
        return new ProjectionFence.Version("g","r",List.of("SIX"),2,3,4,5,6,now,now.plusSeconds(10),"hash");
    }
    @ParameterizedTest @ValueSource(ints={0,1,2,3,4,5,6,7,8})
    void everyVersionDimensionIsChecked(int dimension) {
        Global global=new Global(dimension==0?9:2,dimension==2?9:4,dimension==5?"different":"r");
        Tenant tenant=new Tenant(2,dimension==1?9:3,dimension==3?9:5,dimension==6?"different":"g");
        assertEquals(SNAPSHOT_MISMATCH.code(),assertThrows(ServiceException.class,()->ProjectionFence.current(version(),global,tenant,
            dimension==4?9:6,global.registryRevision(),dimension==7?List.of():List.of("SIX"),dimension==8?"different":"hash",now)).getCode());
    }
    @Test void globalAndTenantCheckpointsAreIndependentlyRequired() {
        Global global=new Global(2,4,"r");Tenant tenant=new Tenant(2,3,5,"g");
        for(Checkpoint c:List.of(new Checkpoint("g",2,1,2,3,true),new Checkpoint("g",2,2,1,3,true),new Checkpoint("g",2,2,2,2,true),new Checkpoint("g",2,2,2,3,false)))
            assertEquals(PROJECTION_NOT_CAUGHT_UP.code(),assertThrows(ServiceException.class,()->ProjectionFence.caughtUp(global,tenant,c)).getCode());
        assertDoesNotThrow(()->ProjectionFence.caughtUp(global,tenant,new Checkpoint("g",2,2,2,3,true)));
    }
    @Test void exactBoundaryRejectsWholeResponseAndBackwardClockAlsoFails() {
        assertEquals(VERSION_EXPIRED.code(),assertThrows(ServiceException.class,()->ProjectionFence.alive(version(),now.plusSeconds(10))).getCode());
        assertThrows(ServiceException.class,()->ProjectionFence.alive(version(),now.minusNanos(1)));
    }
    @Test void noFiniteAuthorizationBoundaryIsExplicit() {
        var v=new ProjectionFence.Version("g","r",List.of(),0,0,0,0,1,now,null,"hash");
        assertDoesNotThrow(()->ProjectionFence.alive(v,now.plusSeconds(100000)));
    }
    @Test void canonicalHashDoesNotDependOnAudienceSetInsertionOrder() {
        ProjectionCodec codec=new ProjectionCodec(new ObjectMapper().findAndRegisterModules());
        Audience a=new Audience(AudienceKind.CANDIDATE_SET,new LinkedHashSet<>(List.of(1L,11L)),"p",1);
        Audience b=new Audience(AudienceKind.CANDIDATE_SET,new LinkedHashSet<>(List.of(11L,1L)),"p",1);
        assertEquals(codec.hash(a),codec.hash(b));
        assertNotEquals(codec.hash(task(1,false)),codec.hash(task(1,true)));
    }
    static class Manager extends AbstractPlatformTransactionManager {
        int commits,rollbacks;
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object tx,TransactionDefinition definition){assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,definition.getIsolationLevel());}
        protected void doCommit(DefaultTransactionStatus status){commits++;}
        protected void doRollback(DefaultTransactionStatus status){rollbacks++;}
    }
    @Test void rootWriteAcquiresFencesBeforeBusinessAndCommitsEntireBatchBeforeHead() {
        ProjectionRepository repository=mock(ProjectionRepository.class);Manager manager=new Manager();List<String> nodes=new ArrayList<>();
        when(repository.lockGlobal(false)).thenAnswer(i->{nodes.add("global");return new Global(0,0,"r");});
        when(repository.lockTenant(2,true)).thenAnswer(i->{nodes.add("tenant");return new Tenant(2,0,0,"g");});
        doAnswer(i->{nodes.add("batch");Batch b=i.getArgument(0);assertEquals(2,b.events().size());return null;}).when(repository).append(any());
        doAnswer(i->{nodes.add("head");Tenant next=i.getArgument(1);assertEquals(1,next.head());assertEquals(1,next.securityEpoch());return null;}).when(repository).advanceTenant(any(),any());
        var protocol=new DatabaseWorkbenchCommitProtocol(repository,manager);
        assertEquals("ok",protocol.tenantWrite(2,true,()->{nodes.add("business");return "ok";},()->List.of(WorkbenchCommitProtocol.Change.task(task(1,false)),WorkbenchCommitProtocol.Change.task(task(2,true)))));
        assertEquals(List.of("global","tenant","business","batch","head"),nodes);assertEquals(1,manager.commits);assertEquals(0,manager.rollbacks);
    }
    @Test void failedBusinessOrOutboxPropagatesAndRollsBackTheOwningTransaction() {
        ProjectionRepository repository=mock(ProjectionRepository.class);when(repository.lockTenant(2,true)).thenReturn(new Tenant(2,0,0,"g"));
        Manager manager=new Manager();var protocol=new DatabaseWorkbenchCommitProtocol(repository,manager);
        assertThrows(IllegalStateException.class,()->protocol.tenantWrite(2,false,()->{throw new IllegalStateException("business failed");},()->List.of()));
        verify(repository,never()).append(any());assertEquals(1,manager.rollbacks);assertEquals(0,manager.commits);
        doThrow(new IllegalStateException("outbox failed")).when(repository).append(any());
        assertThrows(IllegalStateException.class,()->protocol.tenantWrite(2,false,()->"ok",()->List.of(WorkbenchCommitProtocol.Change.task(task(1,false)))));
        verify(repository,never()).advanceTenant(any(),any());assertEquals(2,manager.rollbacks);assertEquals(0,manager.commits);
    }
}
