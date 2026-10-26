package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceOwnerIntegrity.*;

@Service
public class MesEdhrReleaseSubmissionMaterialAssociationStore {
    private final MesEdhrReleaseSubmissionMaterialAssociationMapper mapper;
    private final GxpCreationAuditVerificationService events;
    private final MesEdhrSourceCreationVerificationService creation;
    public MesEdhrReleaseSubmissionMaterialAssociationStore(MesEdhrReleaseSubmissionMaterialAssociationMapper mapper,
            GxpCreationAuditVerificationService events, MesEdhrSourceCreationVerificationService creation) {
        this.mapper = mapper; this.events = events; this.creation = creation;
    }

    public MesEdhrReleaseSubmissionMaterialAssociationDO build(Long tenant, MesProEdhrReleaseTransactionDO tx,
            MesProEdhrReleaseTransactionEventDO event, Long task, String method, MesEdhrVerifiedMaterialGateFact material) {
        MesEdhrSourceCreationBindingStore.tenant(tenant);
        var receipt = material.ownerReceipt(); var proof = material.provenance();
        MesEdhrReleaseSubmissionMaterialAssociationDO row = new MesEdhrReleaseSubmissionMaterialAssociationDO()
                .setTenantId(tenant).setAssociationId(UUID.randomUUID().toString()).setAssociationProtocol("SUBMISSION_MATERIAL_ASSOCIATION_V1")
                .setAssociationVersion(1).setReleaseTransactionId(tx.getId()).setBatchExecutionId(tx.getBatchExecutionId())
                .setSubmissionEventId(event.getId()).setSubmissionTransactionVersion(tx.getVersion()).setApprovalWorkTaskId(task)
                .setSourceMethod(method).setSubmitIdempotencyKey(tx.getSubmitIdempotencyKey()).setSubmittedBy(tx.getSubmittedBy())
                .setSubmissionKeyDigest(keyDigest(tenant, tx.getId(), tx.getSubmitIdempotencyKey()))
                .setSubmissionEvidenceHash(event.getEvidenceHash()).setPrecheckSnapshotHash(sha(tx.getPrecheckSnapshotJson()))
                .setMaterialSourceRowId(proof.sourceRowId()).setMaterialReceiptId(receipt.getReceiptId()).setMaterialVersion(receipt.getVersion())
                .setMaterialReceiptHash(receipt.getReceiptHash()).setMaterialManifestHash(receipt.getManifestHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash()).setMaterialCreationEventId(proof.creationEventId())
                .setMaterialCreationEventHash(proof.creationEventHash());
        var encoded = MesEdhrSourceCreationCodec.encodeSubmissionMaterialAssociationV1(payload(row));
        return row.setAssociationPayloadJson(encoded.canonicalJson()).setAssociationHash(encoded.sha256());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void insert(MesEdhrReleaseSubmissionMaterialAssociationDO row, GxpAuditAppendResult event) {
        MesEdhrSourceCreationBindingStore.tenant(row.getTenantId());
        if (event == null || event.replayed()) throw fail("SUBMISSION_AUDIT_APPEND_FAILED");
        row.setSubmissionAuditEventId(event.eventId()).setSubmissionAuditEventHash(event.eventHash());
        validate(row);
        if (mapper.insert(row) != 1 || row.getId() == null || row.getId() <= 0) throw fail("SUBMISSION_ASSOCIATION_INSERT_FAILED");
        var stored = one(mapper.selectBySubmission(row.getTenantId(), row.getReleaseTransactionId(), row.getSubmissionKeyDigest()));
        if (!row.equals(stored)) throw fail("SUBMISSION_ASSOCIATION_READBACK_FAILED");
    }

    public MesEdhrReleaseSubmissionMaterialAssociationDO requireCurrentVerified(Long tenant, MesProEdhrReleaseTransactionDO tx, Long task) {
        MesEdhrSourceCreationBindingStore.tenant(tenant);
        var row = one(mapper.selectBySubmission(tenant, tx.getId(), keyDigest(tenant, tx.getId(), tx.getSubmitIdempotencyKey())));
        validate(row);
        require(Objects.equals(row.getSubmitIdempotencyKey(), tx.getSubmitIdempotencyKey())
                && Objects.equals(row.getApprovalWorkTaskId(), task) && Objects.equals(row.getBatchExecutionId(), tx.getBatchExecutionId())
                && Objects.equals(row.getSubmissionTransactionVersion(), tx.getVersion())
                && Objects.equals(row.getSubmittedBy(), tx.getSubmittedBy())
                && Objects.equals(row.getPrecheckSnapshotHash(), sha(tx.getPrecheckSnapshotJson())), "SUBMISSION_CURRENT_MISMATCH");
        var proof = creation.requireMaterialCreation(tenant, row.getBatchExecutionId(), row.getMaterialReceiptId());
        require(Objects.equals(proof.sourceRowId(), row.getMaterialSourceRowId())
                && Objects.equals(proof.sourceHash(), row.getMaterialReceiptHash())
                && (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() || Objects.equals(proof.creationEventId(), row.getMaterialCreationEventId())
                && Objects.equals(proof.creationEventHash(), row.getMaterialCreationEventHash())), "SUBMISSION_MATERIAL_MISMATCH");
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return row;
        var audit = events.requireVerifiedEvent(tenant, row.getSubmissionAuditEventId());
        require(Objects.equals(audit.eventHash(), row.getSubmissionAuditEventHash())
                && "mes.market-release.submit".equals(audit.columns().operationId())
                && Objects.equals(audit.columns().actorId(), row.getSubmittedBy())
                && Objects.equals(audit.columns().requestId(), row.getSubmitIdempotencyKey())
                && audit.relations().contains(associationRelation(row)), "SUBMISSION_AUDIT_MISMATCH");
        return row;
    }

    public MesEdhrReleaseSubmissionMaterialAssociationDO requireReplay(Long tenant, Long releaseId, String key,
            String method, Long actor, MesProEdhrReleaseTransactionEventDO sourceEvent, String reason) {
        MesEdhrSourceCreationBindingStore.tenant(tenant);
        var row = one(mapper.selectBySubmission(tenant, releaseId, keyDigest(tenant, releaseId, key)));
        validate(row);
        require(Objects.equals(row.getSourceMethod(), method) && Objects.equals(row.getSubmittedBy(), actor)
                && Objects.equals(row.getSubmissionEventId(), sourceEvent.getId())
                && Objects.equals(row.getSubmissionEvidenceHash(), sourceEvent.getEvidenceHash())
                && Objects.equals(sourceEvent.getReason(), reason), "SUBMISSION_REPLAY_REQUEST_MISMATCH");
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) {
            var proof = creation.requireMaterialCreation(tenant, row.getBatchExecutionId(), row.getMaterialReceiptId());
            require(Objects.equals(proof.sourceRowId(), row.getMaterialSourceRowId()) && Objects.equals(proof.sourceHash(), row.getMaterialReceiptHash()), "SUBMISSION_REPLAY_MATERIAL_MISMATCH");
            return row;
        }
        var audit = events.requireVerifiedEvent(tenant, row.getSubmissionAuditEventId());
        require(Objects.equals(audit.eventHash(), row.getSubmissionAuditEventHash())
                && "mes.market-release.submit".equals(audit.columns().operationId())
                && Objects.equals(audit.columns().actorId(), actor) && Objects.equals(audit.columns().requestId(), key)
                && audit.relations().contains(associationRelation(row)), "SUBMISSION_REPLAY_AUDIT_MISMATCH");
        var proof = creation.requireMaterialCreation(tenant, row.getBatchExecutionId(), row.getMaterialReceiptId());
        require(Objects.equals(proof.creationEventId(), row.getMaterialCreationEventId())
                && Objects.equals(proof.creationEventHash(), row.getMaterialCreationEventHash()), "SUBMISSION_REPLAY_MATERIAL_MISMATCH");
        return row;
    }
    public static GxpAuditRelation associationRelation(MesEdhrReleaseSubmissionMaterialAssociationDO row) {
        return new GxpAuditRelation("SOURCE", "RELEASE_SUBMISSION_MATERIAL_ASSOCIATION", row.getAssociationId(), "1", row.getAssociationHash());
    }

    private static void validate(MesEdhrReleaseSubmissionMaterialAssociationDO row) {
        var encoded = MesEdhrSourceCreationCodec.encodeSubmissionMaterialAssociationV1(payload(row));
        require(Objects.equals(encoded.canonicalJson(), row.getAssociationPayloadJson())
                && Objects.equals(encoded.sha256(), row.getAssociationHash()), "SUBMISSION_ASSOCIATION_PAYLOAD_MISMATCH");
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return;
        MesEdhrSourceCreationBindingStore.positive(row.getSubmissionAuditEventId());
        MesEdhrSourceCreationBindingStore.hash(row.getSubmissionAuditEventHash());
    }
    private static MesEdhrReleaseSubmissionMaterialAssociationDO one(List<MesEdhrReleaseSubmissionMaterialAssociationDO> rows) {
        require(rows != null && rows.size() == 1, "SUBMISSION_ASSOCIATION_MISSING_OR_DUPLICATE"); return rows.get(0);
    }
    private static ObjectNode payload(MesEdhrReleaseSubmissionMaterialAssociationDO r) {
        return JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1").put("associationProtocol", r.getAssociationProtocol())
                .put("associationId", r.getAssociationId()).put("associationVersion", r.getAssociationVersion()).put("tenantId", r.getTenantId())
                .put("releaseTransactionId", r.getReleaseTransactionId()).put("batchExecutionId", r.getBatchExecutionId())
                .put("submissionEventId", r.getSubmissionEventId()).put("submissionTransactionVersion", r.getSubmissionTransactionVersion())
                .put("approvalWorkTaskId", r.getApprovalWorkTaskId()).put("sourceMethod", r.getSourceMethod())
                .put("submitIdempotencyKey", r.getSubmitIdempotencyKey()).put("submittedBy", r.getSubmittedBy())
                .put("submissionEvidenceHash", r.getSubmissionEvidenceHash()).put("precheckSnapshotHash", r.getPrecheckSnapshotHash())
                .put("materialSourceRowId", r.getMaterialSourceRowId()).put("materialReceiptId", r.getMaterialReceiptId())
                .put("materialVersion", r.getMaterialVersion()).put("materialReceiptHash", r.getMaterialReceiptHash())
                .put("materialManifestHash", r.getMaterialManifestHash()).put("sourceSnapshotHash", r.getSourceSnapshotHash())
                .put("materialCreationEventId", r.getMaterialCreationEventId()).put("materialCreationEventHash", r.getMaterialCreationEventHash());
    }
    public static String keyDigest(Long tenant, Long release, String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("SUBMISSION_KEY_REQUIRED");
        return MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(tenant, release, key);
    }
}
