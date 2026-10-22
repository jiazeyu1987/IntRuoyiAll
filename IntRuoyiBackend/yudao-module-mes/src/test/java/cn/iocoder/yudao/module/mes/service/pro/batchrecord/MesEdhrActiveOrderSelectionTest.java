package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceiptPort;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrActiveOrderSelectionTest {
    @Test void activeOrderAssignmentSelectsOnlyReverifiedCompletionAndPqcFacts() {
        TenantContextHolder.setTenantId(1L);
        try {
            var submissions=mock(MesEdhrReleaseSubmissionMaterialAssociationStore.class);
            var managed=mock(MesEdhrManagedHandoffMaterialAssociationStore.class);
            var materials=mock(MesReleaseMaterialGateReceiptPort.class);
            var active=mock(MesEdhrActiveOrderReleaseFactService.class);
            var registry=new MesEdhrReleaseMaterialSelectionRegistry(submissions,managed,materials);
            ReflectionTestUtils.setField(registry,"activeOrderFacts",active);
            String hash="ACTIVE_ORDER_FACTS:"+"a".repeat(64);
            var tx=new MesProEdhrReleaseTransactionDO().setId(10L).setBatchExecutionId(20L);
            var task=new MesProEdhrWorkTaskDO().setId(50L).setBatchExecutionId(20L).setTaskType("RELEASE_APPROVE")
                .setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(10L);
            var association=new MesEdhrManagedHandoffMaterialAssociationDO().setApplicationId(30L)
                .setAssociationPayloadJson("{\"associationProtocol\":\"MANAGED_ACTIVE_ORDER_ASSOCIATION_V1\",\"reportSnapshotHash\":\""+hash+"\",\"precheckSnapshotHash\":\"p\"}");
            when(managed.requireCurrentVerified(1L,tx,50L)).thenReturn(association);
            var fact=new MesEdhrVerifiedActiveOrderReleaseFact(30L,20L,hash,"b".repeat(64),
                new MesEdhrSourceCreationEvidence("COMPLETION_MAIN_V1",302L,"302","1","c".repeat(64),13L,"d".repeat(64)),14L,"e".repeat(64),15L);
            when(active.require(30L,20L,hash)).thenReturn(fact);
            var selection=registry.requireCurrentVerified(1L,tx,task,true);
            assertSame(fact,selection.activeOrderFact()); assertNull(selection.materialFact());
            assertEquals("MANAGED_ACTIVE_ORDER",selection.sourceKind()); verifyNoInteractions(materials,submissions);
            when(active.require(30L,20L,hash)).thenThrow(new IllegalStateException("PQC verification failed"));
            assertThrows(IllegalStateException.class,()->registry.requireCurrentVerified(1L,tx,task,true));
            verifyNoInteractions(materials,submissions);
        } finally { TenantContextHolder.clear(); }
    }
}
