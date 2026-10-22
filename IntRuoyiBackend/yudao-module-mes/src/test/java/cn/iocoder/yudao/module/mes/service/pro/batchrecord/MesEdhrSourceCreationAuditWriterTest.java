package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrMaterialGateReceiptDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrSourceCreationAuditWriterTest {
    @org.junit.jupiter.api.BeforeEach void tenant(){cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);}
    @org.junit.jupiter.api.AfterEach void clearTenant(){cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    @Test void ledgerIsAcquiredBeforeAuthority() {
        var audit = mock(GxpAuditService.class);
        var authority = mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class);
        var writer = new MesEdhrSourceCreationAuditWriter(audit, mock(MesEdhrSourceCreationBindingStore.class),
                mock(MesEdhrSourceOwnerIntegrity.class), mock(GxpCreationAuditVerificationService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "approvalAuthority", authority);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        try {
            writer.acquireLedgerLock();
            var order = inOrder(audit, authority);
            order.verify(audit).acquireLedgerLock();
            order.verify(authority).lockTenantApprovalAuthority(1L);
        } finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    }    @Test void failedRealAuditNeverCreatesAnImmutableBinding() throws Exception {
        var audit = mock(GxpAuditService.class);
        var bindings = mock(MesEdhrSourceCreationBindingStore.class);
        var integrity = mock(MesEdhrSourceOwnerIntegrity.class);
        var events = mock(GxpCreationAuditVerificationService.class);
        var row = row();
        when(integrity.materialTypes(row)).thenReturn((ArrayNode)new ObjectMapper().readTree(row.getMaterialTypeKeysJson()));
        when(audit.append(any())).thenThrow(new IllegalStateException("AUDIT_UNAVAILABLE"));
        var writer = new MesEdhrSourceCreationAuditWriter(audit, bindings, integrity, events);
        assertThrows(IllegalStateException.class, () -> writer.material(row));
        verifyNoInteractions(bindings);
    }
    @Test void auditReplayCannotBindANewMaterialOwnerRow() throws Exception {
        var audit = mock(GxpAuditService.class);
        var bindings = mock(MesEdhrSourceCreationBindingStore.class);
        var integrity = mock(MesEdhrSourceOwnerIntegrity.class);
        var row = row();
        when(integrity.materialTypes(row)).thenReturn((ArrayNode)new ObjectMapper().readTree(row.getMaterialTypeKeysJson()));
        when(audit.append(any())).thenReturn(new GxpAuditAppendResult(9L, 1L, "a".repeat(64), true));
        var writer = new MesEdhrSourceCreationAuditWriter(audit, bindings, integrity, mock(GxpCreationAuditVerificationService.class));
        assertThrows(IllegalStateException.class, () -> writer.material(row));
        verifyNoInteractions(bindings);
    }
    private static MesProEdhrMaterialGateReceiptDO row() {
        return new MesProEdhrMaterialGateReceiptDO().setId(4L).setTenantId(1L).setBatchExecutionId(2L)
            .setReceiptId("material-4").setVersion(1).setIssuedBy(3L).setGateStatus("MATERIALS_READY")
            .setMaterialTypeKeysJson("[\"FINISHED_PRODUCT_INSPECTION_RECORD\",\"FINISHED_PRODUCT_INSPECTION_REPORT\",\"INCOMING_INSPECTION_REPORT\",\"STERILIZATION_REPORT\"]")
            .setSourceSnapshotHash("a".repeat(64)).setManifestHash("b".repeat(64)).setMaterialVersionSetHash("c".repeat(64))
            .setReceiptHash("d".repeat(64)).setAuditEventId("material-gate:original");
    }
}
