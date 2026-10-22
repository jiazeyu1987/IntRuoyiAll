package cn.iocoder.yudao.module.mes.approval.content;
import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;
@Component @RequiredArgsConstructor
public class EdhrSignedDossierReader {
    private final ElectronicSignatureVerifiedFactService signatures;
    private final EdhrReleaseContentSemanticValidator semantic;
    public String read(MesProEdhrReleaseDecisionDO decision, Long transactionId, Long batchId, Long tenantId) {
        if (decision == null || !Objects.equals(decision.getReleaseTransactionId(), transactionId)
                || !Objects.equals(decision.getBatchExecutionId(), batchId)) throw new IllegalStateException("EDHR_SIGNED_DOSSIER_DECISION_MISMATCH");
        var snapshot = ApprovalContentJson.parse(decision.getAuditSnapshotJson());
        String policy = snapshot.path("approvalContentPolicyVersion").asText();
        // Historical decisions explicitly carry no v2 dossier claim.
        if (java.util.Set.of("", "approval-content-v1", "approval-content-active-order-v1").contains(policy)) return null;
        if (!"approval-content-active-order-v2".equals(policy)) throw new IllegalStateException("EDHR_SIGNED_DOSSIER_POLICY_UNKNOWN");
        long signatureId = snapshot.path("signatureId").asLong(0);
        if (signatureId <= 0) throw new IllegalStateException("EDHR_SIGNED_DOSSIER_SIGNATURE_MISSING");
        var fact = signatures.requireVerifiedFact(signatureId);
        var signed = ApprovalContentJson.parse(fact.canonicalContentJson());
        var content = signed.get("approvalContent");
        if (!Objects.equals(fact.tenantId(), tenantId) || !Objects.equals(fact.subjectId(), "EDHR_RELEASE:" + transactionId)
                || !Objects.equals(fact.evidenceHash(), decision.getSignoffEvidenceHash())
                || !"APPROVE".equals(signed.path("action").asText()) || content == null
                || !policy.equals(content.path("contentPolicyVersion").asText())
                || !batchId.toString().equals(content.at("/workTask/batchExecutionId").asText()))
            throw new IllegalStateException("EDHR_SIGNED_DOSSIER_SCOPE_MISMATCH");
        semantic.validate(content);
        String manifest = ApprovalContentJson.canonical(content.at("/finalization/materialManifest"));
        if (!Objects.equals(manifest, snapshot.path("signedDossierManifestJson").asText()))
            throw new IllegalStateException("EDHR_SIGNED_DOSSIER_SNAPSHOT_MISMATCH");
        return manifest;
    }
}
