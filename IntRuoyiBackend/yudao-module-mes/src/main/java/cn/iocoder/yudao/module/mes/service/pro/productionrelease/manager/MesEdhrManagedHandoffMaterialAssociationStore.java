package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service
public class MesEdhrManagedHandoffMaterialAssociationStore {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper workTasks;
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper batchTasks;
    private final MesEdhrManagedHandoffMaterialAssociationMapper mapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private final GxpCreationAuditVerificationService events;
    @jakarta.annotation.Resource private MesEdhrActiveOrderReleaseFactService activeOrderFacts;
    public MesEdhrManagedHandoffMaterialAssociationStore(MesEdhrManagedHandoffMaterialAssociationMapper mapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper applications, GxpCreationAuditVerificationService events) {
        this.mapper=mapper; this.applications=applications; this.events=events;
    }
    @Transactional(propagation=Propagation.MANDATORY, rollbackFor=Exception.class)
    public void insert(MesEdhrManagedHandoffMaterialAssociationDO row, GxpAuditAppendResult audit) {
        if (audit == null || audit.replayed()) throw new IllegalStateException("HANDOFF_APPEND_FAILED");
        row.setHandoffAuditEventId(audit.eventId()).setHandoffAuditEventHash(audit.eventHash());
        validate(row);
        if (mapper.insert(row)!=1 || row.getId()==null || row.getId()<=0) throw new IllegalStateException("HANDOFF_ASSOCIATION_INSERT_FAILED");
        if (!row.equals(one(mapper.selectByKey(row.getTenantId(),row.getHandoffKey())))) throw new IllegalStateException("HANDOFF_ASSOCIATION_READBACK_FAILED");
    }
    public MesEdhrManagedHandoffMaterialAssociationDO requireCurrentVerified(Long tenant, MesProEdhrReleaseTransactionDO tx, Long task) {
        var row=one(mapper.selectByTask(tenant,tx.getId(),task)); validate(row);
        var app=applications.selectById(row.getApplicationId());
        JsonNode payload=parse(row.getAssociationPayloadJson());
        if (app==null || !Objects.equals(app.getTenantId(),tenant) || !Objects.equals(app.getReleaseTransactionId(),tx.getId())
                || !Objects.equals(app.getReleaseApprovalWorkTaskId(),task) || !"MANAGER_RELEASE_PENDING".equals(app.getApplicationStatus())
                || !Objects.equals(app.getReportSnapshotHash(),payload.get("reportSnapshotHash").textValue())
                || app.getVersion()==null || app.getVersion()<payload.get("initializedApplicationVersion").intValue()
                // Handover/reclaim advances the release transaction version while preserving
                // the same verified source association for the current owner.
                || tx.getVersion()==null || tx.getVersion() < payload.get("releaseTransactionVersion").intValue()
                || !cn.hutool.crypto.digest.DigestUtil.sha256Hex(tx.getPrecheckSnapshotJson()).equals(payload.get("precheckSnapshotHash").textValue()))
            throw new IllegalStateException("HANDOFF_CURRENT_APPLICATION_MISMATCH");
        requireSourceRequest(row, app);
        if("MANAGED_ACTIVE_ORDER_ASSOCIATION_V1".equals(payload.path("associationProtocol").asText())) {
            var fact=activeOrderFacts.require(app.getId(),tx.getBatchExecutionId(),app.getReportSnapshotHash());
            var proof=fact.completionCreation();
            if(!Objects.equals(proof.sourceRowId(),payload.path("completionSourceRowId").longValue())
                    || !Objects.equals(proof.sourceReceiptId(),payload.path("completionReceiptId").asText())
                    || !Objects.equals(proof.sourceVersion(),payload.path("completionVersion").asText())
                    || !Objects.equals(proof.sourceHash(),payload.path("completionReceiptHash").asText())
                    || (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && (!Objects.equals(proof.creationEventId(),payload.path("completionCreationEventId").longValue())
                    || !Objects.equals(proof.creationEventHash(),payload.path("completionCreationEventHash").asText())))
                    || !Objects.equals(fact.sourceSnapshotHash(),payload.path("sourceSnapshotHash").asText())
                    || !Objects.equals(fact.pqcSignatureId(),payload.path("pqcSignatureId").longValue())
                    || !Objects.equals(fact.pqcEvidenceHash(),payload.path("pqcEvidenceHash").asText())
                    || (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && !Objects.equals(fact.pqcCreationEventId(),payload.path("pqcCreationEventId").longValue())))
                throw new IllegalStateException("ACTIVE_ORDER_HANDOFF_PROVENANCE_MISMATCH");
        }
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return row;
        var event=events.requireVerifiedEvent(tenant,row.getHandoffAuditEventId());
        if (!Objects.equals(event.eventHash(),row.getHandoffAuditEventHash())
                || !"mes.market-release.manager-stage.initialize".equals(event.columns().operationId())
                || !Objects.equals(event.columns().requestId(),row.getHandoffKey())
                || !Objects.equals(event.columns().actorId(),payload.get("sourceActorId").longValue())
                || !event.relations().contains(new GxpAuditRelation("SOURCE","MANAGED_HANDOFF_MATERIAL_ASSOCIATION",row.getAssociationId(),"1",row.getAssociationHash())))
            throw new IllegalStateException("HANDOFF_AUDIT_MISMATCH");
        return row;
    }
    public MesEdhrManagedHandoffMaterialAssociationDO requireReplay(Long tenant,String key) { var row=one(mapper.selectByKey(tenant,key));validate(row);if (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) events.requireVerifiedEvent(tenant,row.getHandoffAuditEventId());return row; }
    private void requireSourceRequest(MesEdhrManagedHandoffMaterialAssociationDO row,
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO app) {
        JsonNode request = parse(row.getHandoffRequestJson());
        var sourceTask = workTasks.selectById(row.getSourceWorkTaskId());
        if (sourceTask == null || !Objects.equals(sourceTask.getBatchExecutionId(), app.getBatchExecutionId()))
            throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
        if ("MANAGED_PQC_APPROVE".equals(row.getSourceMethod())) {
            JsonNode receipt = parse(app.getDossierSummaryJson());
            if (!Objects.equals(app.getPqcReleaseWorkTaskId(), sourceTask.getId())
                    || !"APPROVE".equals(receipt.path("decision").asText())
                    || !Objects.equals(receipt.path("decisionIdempotencyKey").asText(), row.getSourceIdempotencyKey())
                    || !Objects.equals(receipt.path("decisionPayloadHash").asText(), request.path("sourcePayloadHash").asText())
                    || !request.path("reportEvidenceOwnerHashes").isArray() || !request.path("reportEvidenceOwnerHashes").isEmpty())
                throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
        } else {
            var task = batchTasks.selectById(sourceTask.getBatchTaskId());
            if (task == null) throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
            JsonNode completion = parse(task.getSpecialPayloadJson());
            if (!Objects.equals(completion.path("releaseReportIdempotencyKey").asText(), row.getSourceIdempotencyKey())
                    || !Objects.equals(completion.path("releaseReportPayloadHash").asText(), request.path("sourcePayloadHash").asText()))
                throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
            var hashes = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            batchTasks.selectListByBatchExecutionId(app.getBatchExecutionId()).stream()
                .filter(t -> t.getSpecialPayloadJson() != null && parse(t.getSpecialPayloadJson()).path("releaseReportEvidence").asBoolean())
                .sorted(java.util.Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getNodeType)).forEach(t -> {
                    var evidence = cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence.fromPayloadJson(t.getSpecialPayloadJson());
                    if (evidence == null) throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
                    hashes.add(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("type", t.getNodeType())
                        .put("ownerHash", cn.hutool.crypto.digest.DigestUtil.sha256Hex(evidence.toPayloadJson())));
                });
            if (hashes.size() != 4 || !hashes.equals(request.path("reportEvidenceOwnerHashes")))
                throw new IllegalStateException("HANDOFF_SOURCE_REQUEST_UNPROVEN");
        }
    }    public static JsonNode parse(String raw) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper(com.fasterxml.jackson.core.JsonFactory.builder().enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw); }
        catch(java.io.IOException e){throw new IllegalStateException("HANDOFF_JSON_INVALID",e);}
    }
    private static void validate(MesEdhrManagedHandoffMaterialAssociationDO row) {
        var encoded=MesEdhrSourceCreationCodec.encodeManagedHandoffAssociation(parse(row.getAssociationPayloadJson()));
        JsonNode p=parse(row.getAssociationPayloadJson());
        String key=MesEdhrSourceCreationCodec.managedHandoffAuditKey(row.getTenantId(),row.getApplicationId(),row.getSourceMethod(),
                row.getSourceWorkTaskId(),row.getSourceExpectedApplicationVersion(),row.getSourceIdempotencyKey());
        if (!encoded.canonicalJson().equals(row.getAssociationPayloadJson()) || !encoded.sha256().equals(row.getAssociationHash())
                || !key.equals(row.getHandoffKey()) || !Objects.equals(row.getAssociationId(),p.get("associationId").textValue())
                || !Objects.equals(row.getReleaseTransactionId(),p.get("releaseTransactionId").longValue())
                || !Objects.equals(row.getManagerWorkTaskId(),p.get("managerWorkTaskId").longValue())
                || !MesEdhrSourceCreationCodec.encodeManagedHandoffRequestV1(parse(row.getHandoffRequestJson())).sha256().equals(p.get("handoffRequestHash").textValue())
                || (!cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled() && (row.getHandoffAuditEventId()==null || row.getHandoffAuditEventId()<=0 || row.getHandoffAuditEventHash()==null
                || !row.getHandoffAuditEventHash().matches("[0-9a-f]{64}")))) throw new IllegalStateException("HANDOFF_ASSOCIATION_MISMATCH");
    }
    private static MesEdhrManagedHandoffMaterialAssociationDO one(List<MesEdhrManagedHandoffMaterialAssociationDO> rows) {
        if(rows==null || rows.size()!=1)throw new IllegalStateException("HANDOFF_ASSOCIATION_MISSING_OR_DUPLICATE");return rows.get(0);
    }
}
