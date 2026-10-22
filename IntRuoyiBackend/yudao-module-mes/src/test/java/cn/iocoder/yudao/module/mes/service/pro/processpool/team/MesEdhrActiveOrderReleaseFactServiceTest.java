package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.*;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrActiveOrderReleaseFactServiceTest {
    @Mock MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Mock MesProEdhrBatchExecutionMapper batches;
    @Mock MesProEdhrBatchExecutionOriginMapper origins;
    @Mock MesProEdhrWorkTaskMapper tasks;
    @Mock MesEdhrSourceCreationVerificationService creation;
    @Mock ElectronicSignatureQueryService signatures;
    @Mock ElectronicSignatureVerifiedFactService verified;
    MesEdhrActiveOrderReleaseFactService service;
    MesProcessPoolActiveOrderReleaseApplicationDO app;
    MesProEdhrBatchExecutionOriginDO origin;
    MesProEdhrWorkTaskDO task;
    String frozen;
    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(1L);
        service=new MesEdhrActiveOrderReleaseFactService(applications,batches,origins,tasks,creation,signatures,verified);
        app=MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication();
        app.setWorkOrderCode("WO-9001").setBatchCode("BATCH-9901").setSourceSnapshotHash("a".repeat(64));
        frozen=MesProductionReleaseFormalFactSnapshots.activeOrderFactsSnapshotHash(app,app.getPqcDecision(),app.getPqcDecidedBy(),app.getPqcDecidedAt());
        app.setReportSnapshotHash(frozen);
        origin=new MesProEdhrBatchExecutionOriginDO().setId(12L).setTenantId(1L).setBatchExecutionId(9901L)
            .setEntryType("ACTIVE_ORDER_COMPLETION").setActiveOrderId(8101L).setWorkOrderId(9001L)
            .setCompletionBackfillReceiptId(302L).setCompletionBackfillReceiptHash("a".repeat(64)).setSourceSnapshotHash("b".repeat(64));
        task=MesTeamLeaderActiveOrderDetailServiceImplTest.pqcTask();
        lenient().when(applications.selectById(8801L)).thenReturn(app);
        lenient().when(batches.selectById(9901L)).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcBatch());
        lenient().when(origins.selectListByBatchExecutionId(9901L)).thenReturn(List.of(origin));
        lenient().when(creation.requireCompletionCreation(1L,302L)).thenReturn(new MesEdhrSourceCreationEvidence(
            "COMPLETION_MAIN_V1",302L,"302","1","a".repeat(64),13L,"c".repeat(64)));
        lenient().when(tasks.selectById(8803L)).thenReturn(task);
        var evidence=MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("正式放行意见",false);
        lenient().when(signatures.getById(7701L)).thenReturn(evidence);
        var fact=mock(ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class,call -> {
            String name=call.getMethod().getName();
            if(name.equals("tenantId")) return 1L;
            if(name.equals("auditEventId")) return 15L;
            if(name.equals("verification")) return MesTeamLeaderActiveOrderDetailServiceImplTest.pqcVerification(evidence,"VALID","CONSISTENT","CONSISTENT","CONSISTENT");
            try { return evidence.getClass().getMethod(name).invoke(evidence); }
            catch (NoSuchMethodException ex) { return org.mockito.Answers.RETURNS_DEFAULTS.answer(call); }
        });
        lenient().when(verified.requireVerifiedFact(7701L)).thenReturn(fact);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void verifiesRealCompletionAndCanonicalPqcWithoutMaterialGate() {
        var result=service.require(8801L,9901L,frozen);
        assertEquals(302L,result.completionCreation().sourceRowId());
        assertEquals(7701L,result.pqcSignatureId()); assertEquals(15L,result.pqcCreationEventId());
        verify(creation).requireCompletionCreation(1L,302L); verify(verified).requireVerifiedFact(7701L);
    }
    @Test void duplicateOriginsCannotSupplyTheSameReceiptTwice() {
        when(origins.selectListByBatchExecutionId(9901L)).thenReturn(List.of(origin,origin));
        assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void tenantMismatchBlocksBeforeSignatureConsumption() {
        app.setTenantId(2L); assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void sourceOwnerMismatchBlocksBeforeSignatureConsumption() {
        origin.setActiveOrderId(8102L); assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void changedCompletionHashBlocksBeforeSignatureConsumption() {
        origin.setCompletionBackfillReceiptHash("d".repeat(64));
        assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void unrelatedPqcTaskCannotAuthorizeRelease() {
        task.setBusinessScopeId(8802L); assertThrows(RuntimeException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void completedTaskWithoutFormalBatchCannotAuthorizeRelease() {
        task.setBatchExecutionId(null);
        assertThrows(RuntimeException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
    @Test void failedSignatureVerificationBlocksRelease() {
        when(verified.requireVerifiedFact(7701L)).thenThrow(new IllegalStateException("creation audit rejected"));
        assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen));
    }
    @Test void changedFormalFactsCannotReuseFrozenAssignment() {
        app.setPqcDecidedBy(3002L); assertThrows(IllegalStateException.class,()->service.require(8801L,9901L,frozen)); verifyNoInteractions(verified);
    }
}
