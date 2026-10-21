package cn.iocoder.yudao.module.mes.approval.content;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentFixtures.*;
class EdhrSignedDossierReaderTest {
    @Test void traceReadsImmutableSignedManifestAndRejectsDifferentSnapshotOrTenant() {
        var signatures=mock(ElectronicSignatureVerifiedFactService.class);
        var semantic=mock(EdhrReleaseContentSemanticValidator.class);
        var fact=mock(VerifiedSignatureFact.class);
        var reader=new EdhrSignedDossierReader(signatures,semantic);
        String manifest=ApprovalContentJson.canonical(o("revision","3","files",a(o("fileId","9","sha256",H))));
        var snapshot=o("approvalContentPolicyVersion","approval-content-active-order-v2","signatureId","8","signedDossierManifestJson",manifest);
        var decision=MesProEdhrReleaseDecisionDO.builder().id(5L).releaseTransactionId(20L).batchExecutionId(30L)
                .signoffEvidenceHash(H).auditSnapshotJson(snapshot.toString()).build();
        var content=o("contentPolicyVersion","approval-content-active-order-v2","workTask",o("batchExecutionId","30"),
                "finalization",o("materialManifest",ApprovalContentJson.parse(manifest)));
        when(signatures.requireVerifiedFact(8L)).thenReturn(fact);when(fact.tenantId()).thenReturn(1L);
        when(fact.subjectId()).thenReturn("EDHR_RELEASE:20");when(fact.evidenceHash()).thenReturn(H);
        when(fact.canonicalContentJson()).thenReturn(o("action","APPROVE","approvalContent",content).toString());
        assertEquals(manifest,reader.read(decision,20L,30L,1L));verify(semantic).validate(content);
        assertThrows(IllegalStateException.class,()->reader.read(decision,20L,30L,2L));
        snapshot.put("signedDossierManifestJson","{}");decision.setAuditSnapshotJson(snapshot.toString());
        assertThrows(IllegalStateException.class,()->reader.read(decision,20L,30L,1L));
    }
    @Test void historicalAbsenceIsExplicitAndUnknownPolicyFails() {
        var signatures=mock(ElectronicSignatureVerifiedFactService.class);
        var reader=new EdhrSignedDossierReader(signatures,mock(EdhrReleaseContentSemanticValidator.class));
        var decision=MesProEdhrReleaseDecisionDO.builder().releaseTransactionId(20L).batchExecutionId(30L).auditSnapshotJson("{}").build();
        assertNull(reader.read(decision,20L,30L,1L));verifyNoInteractions(signatures);
        decision.setAuditSnapshotJson(o("approvalContentPolicyVersion","unknown").toString());
        assertThrows(IllegalStateException.class,()->reader.read(decision,20L,30L,1L));
    }
}
