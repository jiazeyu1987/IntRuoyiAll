package cn.iocoder.yudao.module.system.service.profileworkbench.index;
import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchProjectionExport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionConsistencyTest {
    final ProjectionRepository repository=mock(ProjectionRepository.class);
    final ProjectionBatchConsumer.GlobalEffects effects=mock(ProjectionBatchConsumer.GlobalEffects.class);
    final Checkpoint old=new Checkpoint("g",2,0,0,0,false);
    final Global global=new Global(0,0,"r");final Tenant tenant=new Tenant(2,1,0,null);
    @BeforeEach void setup(){when(repository.lockGlobal(false)).thenReturn(global);when(repository.lockTenant(2,true)).thenReturn(tenant);when(repository.checkpoint("g",2)).thenReturn(old);}
    Event control(String id,int ordinal,int size){return new Event(id,Scope.TENANT,2,1,"b",ordinal,size,4,EventKind.CONTROL,null,null,"r");}
    @Test void incompleteBatchCannotAdvanceCheckpointOrApplyAnyEvent(){
        when(repository.events(Scope.TENANT,2,1)).thenReturn(List.of(control("a",0,2)));
        assertThrows(RuntimeException.class,()->new ProjectionBatchConsumer(repository,effects).consumeTenant("g",2));
        verify(repository,never()).recordEvent(anyString(),anyLong(),any());verify(repository,never()).advanceCheckpoint(any(),any());
    }
    @Test void explicitlyDisabledManifestAllowsOnlyCompleteEmptyCollections(){
        assertTrue(new FrozenExport(2,new Watermark(0,0),"r",Set.of(),List.of(),List.of()).tasks().isEmpty());
        Task tombstone=new Task(2,"WORK_ORDER","WORK_ORDER:9",2,true,"9",null,null,null,null,null,null,3,null,null,null);
        assertThrows(IllegalArgumentException.class,()->new FrozenExport(2,new Watermark(0,0),"r",Set.of(),List.of(tombstone),List.of()));
    }
    @Test void failingBatchDoesNotAcknowledgePartialConsumption(){
        Event event=control("a",0,1);when(repository.events(Scope.TENANT,2,1)).thenReturn(List.of(event));
        when(repository.recordEvent("g",2,event)).thenThrow(new IllegalStateException("ledger conflict"));
        assertThrows(IllegalStateException.class,()->new ProjectionBatchConsumer(repository,effects).consumeTenant("g",2));verify(repository,never()).advanceCheckpoint(any(),any());
    }
    @Test void duplicateCompleteBatchCanAcknowledgeWithoutReapplyingFacts(){
        Event event=control("a",0,1);when(repository.events(Scope.TENANT,2,1)).thenReturn(List.of(event));when(repository.recordEvent("g",2,event)).thenReturn(false);
        new ProjectionBatchConsumer(repository,effects).consumeTenant("g",2);
        verify(repository).advanceCheckpoint(old,new Checkpoint("g",2,0,0,1,false));verify(repository,never()).applyTask(anyString(),any());verify(repository,never()).applyAuthorization(anyString(),any());
    }
    ProjectionGenerationService generation(ProjectionGenerationService.ActivationEvidence evidence){
        WorkbenchProjectionExport exporter=mock(WorkbenchProjectionExport.class);when(exporter.sourceIds()).thenReturn(Set.of("WORK_ORDER"));
        when(exporter.exportTasks(2,new Watermark(0,1))).thenReturn(List.of());when(exporter.exportAuthorizations(2,new Watermark(0,1))).thenReturn(List.of());
        when(repository.generationSources("g",2,"r")).thenReturn(Set.of("WORK_ORDER"));
        return new ProjectionGenerationService(repository,List.of(exporter),new ObjectMapper().findAndRegisterModules(),evidence);
    }
    @Test void mixedWatermarkCannotReachApprovalOrPublish(){
        var evidence=mock(ProjectionGenerationService.ActivationEvidence.class);
        assertThrows(RuntimeException.class,()->generation(evidence).verifyAndPublish("g",2,Set.of("WORK_ORDER")));
        verifyNoInteractions(evidence);verify(repository,never()).publish(any(),anyString());
    }
    @Test void exactCollectionStillRequiresRealActivationApproval(){
        when(repository.checkpoint("g",2)).thenReturn(new Checkpoint("g",2,0,0,1,false));
        when(repository.allTasks("g",2)).thenReturn(List.of());when(repository.allAuthorizations("g",2)).thenReturn(List.of());
        var evidence=mock(ProjectionGenerationService.ActivationEvidence.class);doThrow(new IllegalStateException("runtime approval absent")).when(evidence).requireApproved(anyLong(),anyString(),any(),anyString());
        assertThrows(IllegalStateException.class,()->generation(evidence).verifyAndPublish("g",2,Set.of("WORK_ORDER")));
        verify(repository,never()).setComplete(any());verify(repository,never()).publish(any(),anyString());
    }
    @Test void fullFormalAbsenceProofKeepsTombstoneLedgerButAllowsExactCollectionComparison(){
        when(repository.checkpoint("g",2)).thenReturn(new Checkpoint("g",2,0,0,1,false));
        Task tombstone=new Task(2,"WORK_ORDER","WORK_ORDER:9",2,true,"9",null,null,null,null,null,null,3,null,null,null);
        when(repository.allTasks("g",2)).thenReturn(List.of(tombstone));when(repository.allAuthorizations("g",2)).thenReturn(List.of());
        var evidence=mock(ProjectionGenerationService.ActivationEvidence.class);var generation=generation(evidence);
        String hash=generation.verifyAndPublish("g",2,Set.of("WORK_ORDER"));assertEquals(generation.collectionHash(List.of(),List.of()),hash);
        var order=inOrder(evidence,repository);order.verify(evidence).requireApproved(2,"g",new Watermark(0,1),hash);order.verify(repository).setComplete(new Checkpoint("g",2,0,0,1,false));order.verify(repository).publish(tenant,"g");
    }
    @Test void retiredRebuildFailsBeforeAnyFormalExporterOrGenerationWrite() {
        WorkbenchProjectionExport exporter=mock(WorkbenchProjectionExport.class);
        doThrow(new IllegalStateException("retired")).when(repository).requireRetained("g",2);
        var service=new ProjectionGenerationService(repository,List.of(exporter),new ObjectMapper(),mock(ProjectionGenerationService.ActivationEvidence.class));
        assertThrows(IllegalStateException.class,()->service.rebuild("g",2,Set.of("WORK_ORDER")));
        verifyNoInteractions(exporter);verify(repository,never()).createGeneration(anyString(),anyLong(),any(),anyString(),any());
    }
    @Test void retiredConsumerFailsBeforeEventsGlobalEffectsOrApplication() {
        when(repository.checkpoint("g",2)).thenThrow(new IllegalStateException("retired"));
        var consumer=new ProjectionBatchConsumer(repository,effects);
        assertThrows(IllegalStateException.class,()->consumer.consumeTenant("g",2));
        assertThrows(IllegalStateException.class,()->consumer.consumeGlobalForTenant("g",2));
        verifyNoInteractions(effects);verify(repository,never()).events(any(),anyLong(),anyLong());
        verify(repository,never()).recordEvent(anyString(),anyLong(),any());verify(repository,never()).applyTask(anyString(),any());
        verify(repository,never()).advanceCheckpoint(any(),any());
    }

}
