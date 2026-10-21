package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.*;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentProjection.*;

@Component
public class EdhrReleaseContentResolver implements ApprovalContentResolver {
    private final MesProEdhrReleaseService releases;
    private final EdhrReleasePrecheckContentReader checks;
    private final EdhrReleaseContentSemanticValidator semantic;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completion;
    private final MesIndependentBatchPrerequisiteReceiptService independent;
    private final MesProEdhrFourMaterialGateService materials;
    private final FileService files;
    @jakarta.annotation.Resource private EdhrActiveOrderDossierManifest dossierManifest;
    public EdhrReleaseContentResolver(MesProEdhrReleaseService releases, EdhrReleasePrecheckContentReader checks,
            EdhrReleaseContentSemanticValidator semantic, MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completion,
            MesIndependentBatchPrerequisiteReceiptService independent, MesProEdhrFourMaterialGateService materials, FileService files) {
        this.releases = releases; this.checks = checks; this.semantic = semantic; this.completion = completion; this.independent = independent; this.materials = materials; this.files = files;
    }
    public ApprovalModuleCode moduleCode() { return ApprovalModuleCode.EDHR; }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PreparedApprovalContent prepare(Long actor, String type, String id, String process) {
        if (!"EDHR_WORK_TASK".equals(type) || process != null || id == null || !id.matches("[1-9][0-9]*")) throw new IllegalArgumentException("EDHR_CONTENT_IDENTITY_INVALID");
        return read(actor, Long.valueOf(id), false);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public PreparedApprovalContent lockAndAuthorize(Long actor, ApprovalIdentity identity) {
        var content = read(actor, Long.valueOf(identity.sourceTaskId()), true);
        if (!identity.equals(content.identity())) throw new IllegalArgumentException("EDHR_CONTENT_IDENTITY_CHANGED"); return content;
    }
    private PreparedApprovalContent read(Long actor, Long taskId, boolean locked) {
        var graph = releases.readForApproval(taskId, actor, locked); var task = graph.task(); var release = graph.transaction(); var batch = graph.batch(); var origin = graph.origin();
        var identity = new ApprovalIdentity(graph.tenantId(), "EDHR", "EDHR_WORK_TASK", taskId.toString(), null, "EDHR_RELEASE", release.getId().toString());
        var identityJson = object("tenantId", graph.tenantId(), "ownerCode", "EDHR", "sourceTaskType", "EDHR_WORK_TASK", "sourceTaskId", taskId, "processInstanceId", null, "domainSubjectType", "EDHR_RELEASE", "domainSubjectId", release.getId());
        var userIds = JsonNodeFactory.instance.arrayNode(); Arrays.stream(task.getCandidateUserSnapshot().split(",", -1)).map(Long::valueOf).sorted().forEach(id -> userIds.add(id.toString()));
        var responsibility = object("sourceType", task.getResponsibilitySourceType(), "sourceKey", task.getResponsibilitySourceKey(), "sourceVersion", task.getResponsibilitySourceVersion(), "sourceDigest", task.getResponsibilitySourceDigest(), "scopeJsonRaw", null);
        if (task.getResponsibilityScopeJson() != null) throw new IllegalArgumentException("EDHR_RESPONSIBILITY_SCOPE_UNREGISTERED");
        var taskJson = object("id", task.getId(), "type", task.getTaskType(), "businessScopeType", task.getBusinessScopeType(), "businessScopeId", task.getBusinessScopeId(), "batchExecutionId", task.getBatchExecutionId(), "workOrderId", task.getWorkOrderId(), "status", task.getStatus(), "assignmentBranch", graph.managed() ? "MANAGED" : "GENERAL", "assigneeUserId", task.getAssigneeUserId(), "candidate", object("sourceType", task.getCandidateSourceType(), "sourceId", task.getCandidateSourceId(), "userSnapshot", task.getCandidateUserSnapshot(), "userIds", userIds), "responsibility", responsibility);
        var releaseJson = object("id", release.getId(), "version", version(release.getVersion()), "releaseCode", release.getReleaseCode(), "batch", object("id", batch.getId(), "batchExecutionCode", release.getBatchExecutionCode(), "batchCode", release.getBatchCode(), "workOrderId", release.getWorkOrderId(), "workOrderCode", release.getWorkOrderCode()), "product", object("id", release.getProductId(), "code", release.getProductCode(), "name", release.getProductName()), "route", object("id", release.getRouteId(), "code", release.getRouteCode(), "name", release.getRouteName(), "versionId", batch.getRouteVersionId(), "versionNo", batch.getRouteVersionNo()), "dhrStatus", release.getDhrStatus(), "inspectionStatus", release.getInspectionStatus(), "deviationStatus", release.getDeviationStatus(), "reworkStatus", release.getReworkStatus(), "scrapStatus", release.getScrapStatus(), "inventoryStatus", release.getInventoryStatus(), "releaseStatus", release.getReleaseStatus(), "checkCount", object("required", release.getRequiredCheckCount(), "failed", release.getFailedCheckCount(), "blocking", release.getBlockingCheckCount()), "precheckSnapshot", checks.read(release, locked));
        var originJson = object("id", origin.getId(), "tenantId", origin.getTenantId(), "batchExecutionId", origin.getBatchExecutionId(), "entryType", origin.getEntryType(), "originKey", origin.getOriginKey(), "activeOrderId", origin.getActiveOrderId(), "workOrderId", origin.getWorkOrderId(), "completionTransactionId", origin.getCompletionTransactionId(), "completionVersion", version(origin.getCompletionVersion()), "completionBackfillReceiptId", origin.getCompletionBackfillReceiptId(), "completionBackfillReceiptHash", origin.getCompletionBackfillReceiptHash(), "pickListBindingId", origin.getPickListBindingId(), "pickListId", origin.getPickListId(), "pickListBindingVersion", version(origin.getPickListBindingVersion()), "hasActualLoss", origin.getHasActualLoss(), "sourceSnapshotHash", origin.getSourceSnapshotHash(), "batchProvisionReceiptId", origin.getBatchProvisionReceiptId(), "batchProvisionStatus", origin.getBatchProvisionStatus(), "sourceCredentialId", origin.getSourceCredentialId(), "sourceCredentialHash", origin.getSourceCredentialHash(), "sourceBundleHash", origin.getSourceBundleHash(), "idempotencyKey", origin.getIdempotencyKey(), "relationStatus", origin.getRelationStatus());
        boolean active = "ACTIVE_ORDER_COMPLETION".equals(origin.getEntryType());
        JsonNode completionJson = active ? completion(completion.requireVerifiedByReceiptId(origin.getCompletionBackfillReceiptId(), graph.tenantId()).receipt()) : null;
        JsonNode independentJson = active ? null : independent(independent.getCreationVerifiedByReceiptId(graph.tenantId(), origin.getSourceCredentialId(), origin.getEntryType(), origin.getSourceSnapshotHash()).ownerReceipt());
        var attachments = new ArrayList<PreviewAttachment>();
        JsonNode manifest=null;
        JsonNode activeFacts=null;
        boolean activeFormal=graph.activeOrderFact()!=null;
        if(activeFormal) {
            var dossier = dossierManifest.read(graph.tenantId(), origin.getActiveOrderId(), graph.applicationId(), batch.getId(), locked);
            manifest = dossier.manifest();
            attachments.addAll(dossier.attachments());
            var fact=graph.activeOrderFact(); var proof=fact.completionCreation();
            activeFacts=object("applicationId",fact.applicationId(),"batchExecutionId",fact.batchExecutionId(),
                    "formalFactsHash",fact.formalFactsHash(),"sourceSnapshotHash",fact.sourceSnapshotHash(),
                    "completionReceiptId",proof.sourceReceiptId(),"completionReceiptHash",proof.sourceHash(),
                    "completionCreationEventId",proof.creationEventId(),"completionCreationEventHash",proof.creationEventHash(),
                    "pqcSignatureId",fact.pqcSignatureId(),"pqcEvidenceHash",fact.pqcEvidenceHash(),"pqcCreationEventId",fact.pqcCreationEventId());
        } else {
        var evaluated = materials.evaluate(batch.getId()); var receipt = graph.materialFact().ownerReceipt();
        if (!evaluated.ready() || !Objects.equals(evaluated.manifestHash(), receipt.getManifestHash())) throw new IllegalArgumentException("EDHR_MATERIAL_CONTENT_CHANGED");
        var materialList = JsonNodeFactory.instance.arrayNode();
        evaluated.materials().stream().sorted(Comparator.comparing(a -> a.getFieldKey())).forEach(a -> {
            materialList.add(object("type", a.getFieldKey(), "attachmentId", a.getId(), "batchExecutionId", a.getBatchExecutionId(), "batchTaskId", a.getBatchTaskId(), "fieldKey", a.getFieldKey(), "attachmentGroupKey", a.getAttachmentGroupKey(), "versionNo", version(a.getVersionNo()), "fileId", a.getFileId(), "fileName", a.getFileName(), "fileSize", a.getFileSize(), "contentType", a.getContentType(), "sha256", a.getSha256(), "attachmentHash", a.getAttachmentHash(), "sourceSnapshotHash", origin.getSourceSnapshotHash()));
            var file = files.getFile(a.getFileId()); if (file == null || !Objects.equals(file.getSize(), a.getFileSize())) throw new IllegalArgumentException("EDHR_MATERIAL_FILE_MISSING");
            try {
                byte[] bytes=files.getFileContent(file.getConfigId(),file.getPath());
                if(bytes==null || bytes.length!=a.getFileSize() || !cn.hutool.crypto.digest.DigestUtil.sha256Hex(bytes).equals(a.getSha256())) throw new IllegalArgumentException("EDHR_MATERIAL_FILE_INTEGRITY_FAILED");
            } catch(RuntimeException e) {throw e;} catch(Exception e) {throw new IllegalStateException("EDHR_MATERIAL_FILE_READ_FAILED",e);}
            attachments.add(new PreviewAttachment(a.getFileId().toString(), a.getFileName(), version(a.getVersionNo()), a.getSha256(), a.getFileSize().toString(), a.getContentType(), files.presignGetUrl(file.getUrl(), 900)));
        });
        var types = JsonNodeFactory.instance.arrayNode(); receipt.getMaterialTypeKeys().stream().sorted().forEach(types::add);
        manifest = object("receiptId", receipt.getReceiptId(), "batchExecutionId", receipt.getBatchExecutionId(), "gateStatus", receipt.getGateStatus(), "materialTypeKeys", types, "manifestHash", receipt.getManifestHash(), "sourceSnapshotHash", receipt.getSourceSnapshotHash(), "materialVersionSetHash", receipt.getMaterialVersionSetHash(), "receiptHash", receipt.getReceiptHash(), "issuedBy", receipt.getIssuedBy(), "auditEventId", receipt.getAuditEventId(), "version", version(receipt.getVersion()), "materials", materialList);
        }
        var trace = graph.sourceContext(); var finalization = object("sourceKind", active ? "ACTIVE_ORDER" : "INDEPENDENT", "authorizationBranch", graph.managed() ? "MANAGED" : "GENERAL", "applicationId", graph.applicationId(), "origin", originJson, "trace", object("originLinkId", trace.getOriginLinkId(), "traceLinkHash", trace.getTraceLinkHash(), "sourceSnapshotHash", trace.getSourceSnapshotHash(), "relationStatus", trace.getRelationStatus()), "sourceRelation", active ? "FLOW4_COMPLETION_BACKFILL_RECEIPT" : "INDEPENDENT_PREREQUISITE_RECEIPT", "sourceSnapshotHash", origin.getSourceSnapshotHash(), "activeOrderVersion", version(graph.activeOrderVersion()), "completionBackfillReceipt", completionJson, "independentPrerequisiteReceipt", independentJson, "materialManifest", manifest);
        if(activeFormal) finalization.set("activeOrderFormalFacts",activeFacts);
        String contentPolicy=activeFormal?"approval-content-active-order-v2":"approval-content-v1";
        var root = object("contentPolicyVersion", contentPolicy, "scopeCode", "EDHR_RELEASE", "identity", identityJson, "workTask", taskJson, "release", releaseJson, "finalization", finalization);
        var guard = object("id", batch.getId(), "tenantId", batch.getTenantId(), "workOrderId", batch.getWorkOrderId(), "status", batch.getStatus(), "provisioningStatus", batch.getProvisioningStatus(), "aggregateHash", batch.getAggregateHash(), "routeVersionId", batch.getRouteVersionId(), "routeVersionNo", batch.getRouteVersionNo(), "routeSnapshotJsonRaw", batch.getRouteSnapshotJson(), "attemptNo", batch.getAttemptNo(), "activeContextKey", batch.getActiveContextKey(), "sourceRejectedBatchExecutionId", batch.getSourceRejectedBatchExecutionId(), "supersededByBatchExecutionId", batch.getSupersededByBatchExecutionId(), "reexecutedByChangeEventId", batch.getReexecutedByChangeEventId());
        return new PreparedApprovalContent(identity, contentPolicy, semantic.subjectVersion(root, guard), ApprovalContentJson.hash(root), "EDHR_RELEASE", activeFormal?"eDHR活跃订单正式完工、PQC放行与上市放行正文":"eDHR放行正文、正式来源、预检及四项材料", ApprovalContentJson.canonical(root), attachments, List.of("APPROVE"));
    }
    private static JsonNode evidence(List<MesBatchExecutionSourceEvidence> source) {
        if (source == null) return null; var result = JsonNodeFactory.instance.arrayNode(); source.stream().sorted(Comparator.comparing(MesBatchExecutionSourceEvidence::getSourceType).thenComparing(MesBatchExecutionSourceEvidence::getSourceId)).forEach(e -> result.add(object("sourceType", e.getSourceType(), "sourceId", e.getSourceId(), "sourceVersion", e.getSourceVersion(), "sourceSnapshotHash", e.getSourceSnapshotHash(), "payloadHash", e.getPayloadHash(), "signature", e.getSignature()))); return result;
    }
    private static JsonNode sourceIds(String raw) {
        if (raw == null) return null;
        var parsed = ApprovalContentJson.parse(raw);
        if (!parsed.isArray()) throw new IllegalArgumentException("COMPLETION_SOURCE_IDS_INVALID");
        var result = JsonNodeFactory.instance.arrayNode();
        for (var value : parsed) {
            long id;
            // The formal JsonUtils producer encodes Long values outside JavaScript's safe range as strings.
            if (value.isTextual()) {
                String text = value.textValue();
                if (!text.matches("[1-9][0-9]*")) throw new IllegalArgumentException("COMPLETION_SOURCE_ID_INVALID");
                try { id = Long.parseLong(text); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("COMPLETION_SOURCE_ID_INVALID", e); }
            } else if (value.isIntegralNumber() && value.canConvertToLong()) {
                id = value.longValue();
            } else throw new IllegalArgumentException("COMPLETION_SOURCE_ID_INVALID");
            if (id <= 0) throw new IllegalArgumentException("COMPLETION_SOURCE_ID_INVALID");
            result.add(Long.toString(id));
        }
        return result;
    }
    private static JsonNode completion(MesFlow6CompletionBackfillReceipt r) {
        var picks = JsonNodeFactory.instance.arrayNode(); if (r.getPickListSources() == null) throw new IllegalArgumentException("COMPLETION_PICKLIST_SOURCE_MISSING"); r.getPickListSources().stream().sorted(Comparator.comparing(MesBatchExecutionPickListSource::getPickListBindingId)).forEach(p -> picks.add(object("pickListBindingId", p.getPickListBindingId(), "pickListId", p.getPickListId(), "bindingVersion", version(p.getBindingVersion()), "sourceSnapshotHash", p.getSourceSnapshotHash())));
        return object("receiptId", r.getReceiptId(), "tenantId", r.getTenantId(), "activeOrderId", r.getActiveOrderId(), "workOrderId", r.getWorkOrderId(), "workOrderCode", r.getWorkOrderCode(), "batchCode", r.getBatchCode(), "routeId", r.getRouteId(), "routeVersionId", r.getRouteVersionId(), "requestIdempotencyKey", r.getRequestIdempotencyKey(), "createdAt", r.getCreatedAt(), "sourceSnapshotHash", r.getSourceSnapshotHash(), "formalSourceSnapshotJsonRaw", r.getFormalSourceSnapshotJson(), "signatureSnapshotJsonRaw", r.getSignatureSnapshotJson(), "completionVersion", version(r.getCompletionVersion()), "expectedActiveOrderVersion", version(r.getExpectedActiveOrderVersion()), "completionTransactionId", r.getCompletionTransactionId(), "completionEventId", r.getCompletionEventId(), "sourceVersion", r.getSourceVersion(), "sourceBundleHash", r.getSourceBundleHash(), "receiptVersion", r.getReceiptVersion(), "payloadHash", r.getPayloadHash(), "auditEventId", r.getAuditEventId(), "receiptHash", r.getReceiptHash(), "status", r.getStatus(), "batchRecordStatus", r.getBatchRecordStatus(), "processInspectionStatus", r.getProcessInspectionStatus(), "batchRecordId", r.getBatchRecordId(), "processInspectionId", r.getProcessInspectionId(), "batchRecordSourceIds", sourceIds(r.getBatchRecordSourceIdsJson()), "processInspectionSourceIds", sourceIds(r.getProcessInspectionSourceIdsJson()), "hasActualLoss", r.getHasActualLoss(), "lossConditionFactsJsonRaw", r.getLossConditionFactsJson(), "lossQuantity", r.getLossQuantity(), "lossReportStatus", r.getLossReportStatus(), "lossRecordId", r.getLossRecordId(), "zeroLossConfirmationSnapshotRaw", r.getZeroLossConfirmationSnapshot(), "pickListSources", picks, "sourceEvidence", evidence(r.getSourceEvidence()));
    }
    private static JsonNode independent(MesIndependentBatchPrerequisiteReceipt r) {
        return object("receiptId", r.getReceiptId(), "tenantId", r.getTenantId(), "entryType", r.getEntryType(), "workOrderId", r.getWorkOrderId(), "workOrderCode", r.getWorkOrderCode(), "routeId", r.getRouteId(), "routeVersionId", r.getRouteVersionId(), "routeVersion", r.getRouteVersion(), "batchCode", r.getBatchCode(), "sourceRelationId", r.getSourceRelationId(), "sourceRelationVersion", r.getSourceRelationVersion(), "sourceRelationSnapshotHash", r.getSourceRelationSnapshotHash(), "sourceObjectType", r.getSourceObjectType(), "sourceObjectId", r.getSourceObjectId(), "materialSourceType", r.getMaterialSourceType(), "materialSourceId", r.getMaterialSourceId(), "sourceContextHash", r.getSourceContextHash(), "sourceSnapshotHash", r.getSourceSnapshotHash(), "businessReason", r.getBusinessReason(), "issuerSystem", r.getIssuerSystem(), "issuerUserId", r.getIssuerUserId(), "issuerUserRole", r.getIssuerUserRole(), "issuedAt", r.getIssuedAt(), "expiresAt", r.getExpiresAt(), "revokedAt", r.getRevokedAt(), "revocationReason", r.getRevocationReason(), "credentialVersion", version(r.getCredentialVersion()), "status", r.getStatus(), "canonicalPayloadRaw", r.getCanonicalPayload(), "receiptHash", r.getReceiptHash(), "payloadHash", r.getPayloadHash(), "signature", r.getSignature(), "auditEventId", r.getAuditEventId(), "idempotencyKey", r.getIdempotencyKey(), "sourceEvidence", evidence(r.getSourceEvidence()));
    }
}
