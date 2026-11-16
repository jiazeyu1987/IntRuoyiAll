package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService.VerifiedAuditEventFact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceOwnerIntegrity.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationProtocolRegistry.Kind.*;

/** Read-only historical provenance. Neither current eligibility nor producer completion is inferred. */
@Service
public class MesEdhrSourceCreationVerificationService {
    @jakarta.annotation.Resource private MesEdhrReleaseSubmissionMaterialAssociationMapper submissionAssociations;
    @jakarta.annotation.Resource private MesEdhrManagedHandoffMaterialAssociationMapper managedAssociations;
    private final MesProEdhrMaterialGateReceiptMapper materials;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completions;
    private final MesEdhrSourceOwnerIntegrity owner;
    private final MesEdhrSourceCreationBindingStore bindings;
    private final GxpCreationAuditVerificationService shared;
    private final MesEdhrSourceCreationProtocolRegistry registry;

    public MesEdhrSourceCreationVerificationService(MesProEdhrMaterialGateReceiptMapper materials, MesProcessPoolActiveOrderCompletionReceiptMapper completions,
            MesEdhrSourceOwnerIntegrity owner, MesEdhrSourceCreationBindingStore bindings,
            GxpCreationAuditVerificationService shared, MesEdhrSourceCreationProtocolRegistry registry) {
        this.materials = materials; this.completions = completions;
        this.owner = owner; this.bindings = bindings; this.shared = shared; this.registry = registry;
    }

    public MesEdhrSourceCreationEvidence requireMaterialCreation(Long tenantId, Long batchExecutionId, String receiptId) {
        MesEdhrSourceCreationBindingStore.tenant(tenantId); MesEdhrSourceCreationBindingStore.positive(batchExecutionId);
        MesEdhrSourceCreationBindingStore.text(receiptId, 128);
        var row = materials.selectByReceiptId(tenantId, batchExecutionId, receiptId);
        require(row != null, "SOURCE_ROW_MISSING"); source(row.getTenantId(), row.getId(), tenantId);
        eq(batchExecutionId, row.getBatchExecutionId()); eq(receiptId, row.getReceiptId()); positiveStored(row.getVersion());
        ArrayNode types = owner.materialTypes(row); String version = row.getVersion().toString();
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return developmentProof("MATERIAL_READY_V1", row.getId(), receiptId, version, row.getReceiptHash());
        var binding = forward(tenantId, "MATERIAL_READY_V1", row.getId(), receiptId, version, row.getReceiptHash());
        var candidates = candidates(tenantId, "EDHR_MATERIAL_GATE_RECEIPT", row.getId(), version, row.getReceiptHash());
        var creation = one(candidates, MATERIAL); ObjectNode state;
        try { state = MesEdhrSourceCreationCodec.decodeMaterialReadyStateV1(bytes(creation.columns().afterStateJson())); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("SOURCE_OWNER_INTEGRITY_FAILED", e); }
        ObjectNode request = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("requestKind", "MATERIAL_READY_REQUEST_V1").put("tenantId", tenantId).put("batchExecutionId", batchExecutionId)
                .put("version", row.getVersion()).put("issuedBy", row.getIssuedBy()).put("sourceSnapshotHash", row.getSourceSnapshotHash())
                .put("manifestHash", row.getManifestHash()).put("materialVersionSetHash", row.getMaterialVersionSetHash());
        request.set("materialTypeKeys", types);
        var encoded = MesEdhrSourceCreationCodec.encodeMaterialReadyRequestV1(request);
        request = MesEdhrSourceCreationCodec.decodeMaterialReadyRequestV1(encoded.utf8Bytes());
        ObjectNode expected = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("stateKind", "MATERIAL_READY_STATE_V1").put("sourceRowId", row.getId()).put("receiptId", receiptId)
                .put("gateStatus", row.getGateStatus()).put("materialTypeKeysJsonRaw", row.getMaterialTypeKeysJson())
                .put("legacyOwnerAuditToken", row.getAuditEventId()).put("receiptHash", row.getReceiptHash())
                .put("requestHashVersion", "MATERIAL_READY_REQUEST_V1").put("requestHash", encoded.sha256());
        expected.set("request", request);
        require(state.equals(expected), "SOURCE_OWNER_INTEGRITY_FAILED");
        String key = MesEdhrSourceCreationCodec.materialCreateAuditKey(tenantId, batchExecutionId, row.getVersion());
        header(creation, "MATERIAL_GATE_RECEIPT:" + receiptId, version, row.getIssuedBy(), "ABSENT", "MATERIALS_READY", version, version,
                "四材料门禁就绪回执创建", "MES_EDHR_MATERIALS_READY", "SYSTEM", key, key);
        eq("{}", creation.columns().beforeStateJson());
        exactRelations(creation, List.of(anchor("EDHR_MATERIAL_GATE_RECEIPT", row.getId(), version, row.getReceiptHash())));
        exactEvidence(creation, List.of(new GxpAuditEvidence("MATERIAL_GATE_RECEIPT", receiptId, version, row.getReceiptHash(), "CREATION")));
        matchBinding(tenantId, binding, creation, state);
        for (var candidate : candidates) if (candidate.kind() != MATERIAL) related(candidate.kind(), candidate.fact(), row, creation);
        return proof("MATERIAL_READY_V1", row.getId(), receiptId, version, row.getReceiptHash(), creation);
    }

    public MesEdhrSourceCreationEvidence requireCompletionCreation(Long tenantId, Long receiptId) {
        MesEdhrSourceCreationBindingStore.tenant(tenantId); MesEdhrSourceCreationBindingStore.positive(receiptId);
        var row = completions.selectByIdAndTenantId(receiptId, tenantId);
        require(row != null, "SOURCE_ROW_MISSING"); source(row.getTenantId(), row.getId(), tenantId); eq(receiptId, row.getId());
        positiveStored(row.getActiveOrderId()); positiveStored(row.getWorkOrderId()); positiveStored(row.getRouteId());
        positiveStored(row.getRouteVersionId()); positiveStored(row.getLeaderUserId()); positiveStored(row.getCompletedBy());
        positiveStored(row.getBatchRecordId()); positiveStored(row.getProcessInspectionId()); eq(row.getLeaderUserId(), row.getCompletedBy());
        require(row.getCompletedAt() != null && row.getBatchCode() != null && !row.getBatchCode().isBlank(), "SOURCE_OWNER_INTEGRITY_FAILED");
        owner.completion(row);
        String version = row.getCompletedVersion().toString();
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return developmentProof("COMPLETION_MAIN_V1", row.getId(), receiptId.toString(), version, row.getReceiptHash());
        var candidates = candidates(tenantId, "COMPLETION_RECEIPT", row.getId(), version, row.getReceiptHash());
        var main = one(candidates, COMPLETION_MAIN); var parts = byKind(candidates, COMPLETION_PART);
        completionHeader(main, row, null);
        JsonNode before = strict(main.columns().beforeStateJson()), after = strict(main.columns().afterStateJson());
        keys(before, Set.of("activeOrderId", "activeStatus", "businessStatus", "version", "affectedRowAuditManifest"));
        keys(after, Set.of("activeOrderId", "completedVersion", "completionReceiptId", "completionStatus", "batchRecordStatus",
                "processInspectionStatus", "batchRecordId", "processInspectionId", "lossReportStatus", "hasActualLoss", "lossQuantity",
                "zeroLossConfirmationSnapshot", "sourceSnapshotHash", "completionReceiptHash", "affectedRowAuditManifest"));
        businessLong(before.get("activeOrderId"), row.getActiveOrderId()); eqNode(before, "activeStatus", TextNode.valueOf("ACTIVE"));
        text(before.get("businessStatus")); eqNode(before, "version", IntNode.valueOf(row.getExpectedVersion()));
        businessLong(after.get("activeOrderId"), row.getActiveOrderId()); businessLong(after.get("completionReceiptId"), row.getId());
        businessLong(after.get("batchRecordId"), row.getBatchRecordId()); businessLong(after.get("processInspectionId"), row.getProcessInspectionId());
        eqNode(after, "completedVersion", IntNode.valueOf(row.getCompletedVersion()));
        for (String field : List.of("completionStatus", "batchRecordStatus", "processInspectionStatus")) eqNode(after, field, TextNode.valueOf("SUCCESS"));
        eqNode(after, "lossReportStatus", TextNode.valueOf(row.getLossReportStatus()));
        eqNode(after, "hasActualLoss", BooleanNode.valueOf(row.getHasActualLoss()));
        require(after.get("lossQuantity").isNumber() && after.get("lossQuantity").decimalValue().compareTo(row.getLossQuantity()) == 0,
                "COMPLETION_PART_MANIFEST_MISMATCH");
        eqNode(after, "zeroLossConfirmationSnapshot", row.getZeroLossConfirmationSnapshot() == null ? NullNode.instance : TextNode.valueOf(row.getZeroLossConfirmationSnapshot()));
        eqNode(after, "sourceSnapshotHash", TextNode.valueOf(row.getSourceSnapshotHash())); eqNode(after, "completionReceiptHash", TextNode.valueOf(row.getReceiptHash()));
        require(before.get("affectedRowAuditManifest").equals(after.get("affectedRowAuditManifest")), "COMPLETION_PART_MANIFEST_MISMATCH");
        JsonNode manifest = after.get("affectedRowAuditManifest");
        verifyParts(row, main, parts, manifest);
        List<GxpAuditRelation> relations = new ArrayList<>(completionRelations(row));
        relations.add(new GxpAuditRelation("SOURCE", "BATCH_RECORD", row.getBatchRecordId().toString(), null, null));
        relations.add(new GxpAuditRelation("SOURCE", "PROCESS_INSPECTION", row.getProcessInspectionId().toString(), null, null));
        if (row.getHasActualLoss()) relations.add(new GxpAuditRelation("SOURCE", "LOSS_REPORT", row.getLossRecordId().toString(), null, row.getLossSourceHash()));
        for (var part : parts) relations.add(new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", part.eventId().toString(), null, part.eventHash()));
        exactRelations(main, relations); exactEvidence(main, completionEvidence(row));
        return proof("COMPLETION_MAIN_V1", row.getId(), receiptId.toString(), version, row.getReceiptHash(), main);
    }

    public String requireCompletionTransactionId(Long tenantId, Long receiptId) {
        MesEdhrSourceCreationEvidence proof = requireCompletionCreation(tenantId, receiptId);
        if (cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditDevelopmentMode.disabled()) return "DEVELOPMENT_DISABLED:COMPLETION:" + receiptId;
        VerifiedAuditEventFact event = shared.requireVerifiedEvent(tenantId, proof.creationEventId());
        eq(proof.creationEventHash(), event.eventHash());
        String transactionId = event.columns().transactionId();
        require(transactionId != null && !transactionId.isBlank(), "SOURCE_TRANSACTION_MISSING");
        return transactionId;
    }

    // Negative IDs are explicit development markers, never IDs of real GXP audit events.
    private static MesEdhrSourceCreationEvidence developmentProof(String protocol, Long row, String receipt, String version, String hash) {
        return new MesEdhrSourceCreationEvidence(protocol, row, receipt, version, hash, -row, "DEVELOPMENT_DISABLED");
    }

    private record Candidate(MesEdhrSourceCreationProtocolRegistry.Kind kind, VerifiedAuditEventFact fact) {}
    private List<Candidate> candidates(Long tenant, String target, Long row, String version, String hash) {
        List<VerifiedAuditEventFact> verified = new ArrayList<>();
        for (Long id : shared.readCandidateEventIdsByTarget(tenant, target, row.toString())) verified.add(shared.requireVerifiedEvent(tenant, id));
        List<Candidate> result = new ArrayList<>();
        for (var fact : verified) {
            require(fact.relations().contains(anchor(target, row, version, hash)), "SOURCE_RELATION_MISMATCH");
            result.add(new Candidate(registry.classify(target, fact), fact));
        }
        return List.copyOf(result);
    }
    private static List<VerifiedAuditEventFact> byKind(List<Candidate> candidates, MesEdhrSourceCreationProtocolRegistry.Kind kind) {
        return candidates.stream().filter(c -> c.kind() == kind).map(Candidate::fact).toList();
    }
    private static VerifiedAuditEventFact one(List<Candidate> candidates, MesEdhrSourceCreationProtocolRegistry.Kind kind) {
        var found = byKind(candidates, kind);
        require(!found.isEmpty(), "SOURCE_CREATION_MISSING"); require(found.size() == 1, "SOURCE_CREATION_DUPLICATE"); return found.get(0);
    }
    private MesEdhrSourceCreationBindingStore.StoredBindingFact forward(Long tenant, String protocol, Long row, String receipt, String version, String hash) {
        var left = bindings.readBySourceRow(tenant, protocol, row); var right = bindings.readByReceiptVersion(tenant, protocol, receipt, version);
        require(!left.isEmpty() && !right.isEmpty(), "SOURCE_BINDING_MISSING");
        require(left.size() == 1 && right.size() == 1, "SOURCE_BINDING_DUPLICATE");
        require(left.get(0).equals(right.get(0)), "SOURCE_BINDING_MISMATCH");
        var fact = left.get(0).fact(); eq(row, fact.sourceRowId()); eq(receipt, fact.sourceReceiptId()); eq(version, fact.sourceVersion()); eq(hash, fact.sourceHash());
        return left.get(0);
    }
    private void matchBinding(Long tenant, MesEdhrSourceCreationBindingStore.StoredBindingFact binding, VerifiedAuditEventFact event, JsonNode state) {
        var f = binding.fact();
        require(Objects.equals(f.creationEventId(), event.eventId()) && Objects.equals(f.creationEventHash(), event.eventHash())
                && Objects.equals(f.requestHashVersion(), text(state.get("requestHashVersion")))
                && Objects.equals(f.requestHash(), text(state.get("requestHash"))), "SOURCE_BINDING_MISMATCH");
        require(List.of(binding).equals(bindings.readByCreationEvent(tenant, event.eventId())), "SOURCE_REVERSE_BINDING_CONFLICT");
    }

    private void related(MesEdhrSourceCreationProtocolRegistry.Kind kind, VerifiedAuditEventFact event,
            MesProEdhrMaterialGateReceiptDO row, VerifiedAuditEventFact creation) {
        var c = event.columns();
        require(c.subjectId() != null && c.subjectId().matches("RELEASE_TRANSACTION:[1-9][0-9]*")
                && c.subjectVersion() != null && c.subjectVersion().matches("[1-9][0-9]*"), "UNKNOWN_CANDIDATE_PROTOCOL");
        long releaseId = decimal(c.subjectId().substring("RELEASE_TRANSACTION:".length()), true);
        long version = decimal(c.subjectVersion(), true);
        require(version <= Integer.MAX_VALUE, "RELATED_STATE_PROTOCOL_INVALID");
        if (kind == RELATED_SUBMISSION) relatedSubmission(event, row, creation, releaseId, (int) version);
        else if (kind == RELATED_HANDOFF) relatedManaged(event, row, creation, releaseId, (int) version);
        else throw fail("UNKNOWN_CANDIDATE_PROTOCOL");
        // Historical association shape gives no current selection or upstream request authority.
    }

    private void relatedSubmission(VerifiedAuditEventFact event, MesProEdhrMaterialGateReceiptDO row,
            VerifiedAuditEventFact creation, long releaseId, int version) {
        var c = event.columns();
        JsonNode before = strict(c.beforeStateJson()), after = strict(c.afterStateJson());
        keys(before, Set.of("transaction", "checkItems", "affectedState"));
        keys(after, Set.of("transaction", "checkItems", "affectedState"));
        require("PRECHECK_PASSED".equals(c.beforeState()) && "PENDING_APPROVAL".equals(c.afterState())
                && Integer.toString(version).equals(c.beforeObjectVersion())
                && Integer.toString(version).equals(c.afterObjectVersion()), "RELATED_STATE_PROTOCOL_INVALID");
        JsonNode b = before.get("transaction"), a = after.get("transaction");
        legacyTransaction(b, releaseId, row.getBatchExecutionId(), version, "PRECHECK_PASSED");
        legacyTransaction(a, releaseId, row.getBatchExecutionId(), version, "PENDING_APPROVAL");
        String key = text(a.get("submitIdempotencyKey"));
        require(legacyLong(a.get("submittedBy")) == c.actorId() && a.hasNonNull("submittedAt"), "SOURCE_ACTOR_MISMATCH");
        eq(key, c.requestId());
        require(c.idempotencyKey() != null && c.idempotencyKey().matches("RELEASE_PREP:[0-9a-f]{64}")
                && "MES_RELEASE_PREPARATION".equals(c.reasonCode()) && "SYSTEM".equals(c.reasonSource())
                && c.reason() != null && !c.reason().isBlank(), "RELATED_STATE_PROTOCOL_INVALID");
        Set<String> mutable = Set.of("releaseStatus", "submitIdempotencyKey", "submittedBy", "submittedAt", "updateTime", "updater");
        Set<String> fields = new HashSet<>(); b.fieldNames().forEachRemaining(fields::add); a.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) if (!mutable.contains(field)) eq(b.get(field), a.get(field));
        legacyChecks(before.get("checkItems"), releaseId); legacyChecks(after.get("checkItems"), releaseId);
        eq(before.get("checkItems"), after.get("checkItems"));
        submissionAffected(before.get("affectedState"), b, row.getBatchExecutionId(), releaseId);
        submissionAffected(after.get("affectedState"), a, row.getBatchExecutionId(), releaseId);
        var association = onlyRelation(event, "RELEASE_SUBMISSION_MATERIAL_ASSOCIATION");
        uuid(association.objectId()); eq("1", association.objectVersion()); hashValue(association.objectHash());
        var original = onlyRelation(event, "RELEASE_SUBMISSION_EVENT");
        decimal(original.objectId(), true); eq(null, original.objectVersion()); hashValue(original.objectHash());
        List<GxpAuditRelation> expected = new ArrayList<>(List.of(
                new GxpAuditRelation("SOURCE", "BATCH_EXECUTION", row.getBatchExecutionId().toString(), null, null),
                original, association, anchor("EDHR_MATERIAL_GATE_RECEIPT", row.getId(), row.getVersion().toString(), row.getReceiptHash()),
                new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", creation.eventId().toString(), null, creation.eventHash())));
        var active = event.relations().stream().filter(r -> "ACTIVE_ORDER".equals(r.objectType())).toList();
        require(active.size() <= 1, "SOURCE_RELATION_MISMATCH");
        if (!active.isEmpty()) {
            var relation = active.get(0); decimal(relation.objectId(), true);
            eq("SOURCE", relation.relationType()); eq(null, relation.objectVersion()); eq(null, relation.objectHash()); expected.add(relation);
        }
        exactRelations(event, expected);
        exactEvidence(event, List.of(new GxpAuditEvidence("RELEASE_SUBMISSION_MATERIAL_ASSOCIATION",
                association.objectId(), "1", association.objectHash(), "SUBMISSION_MATERIAL")));
        // No stored deployment profile proof exists in the released twelve-file boundary.
        // The current mapper (even a matching production mapper) is not historical evidence.
        // Therefore we must not recompute RELEASE_PREP using it or certify unknown history.
        var stored = submissionAssociations.selectBySubmission(row.getTenantId(), releaseId,
                MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(row.getTenantId(), releaseId, key));
        require(stored != null && stored.size() == 1, "SOURCE_SERIALIZATION_PROFILE_UNPROVEN");
        var binding = stored.get(0);
        require(Objects.equals(binding.getAssociationId(), association.objectId())
                && Objects.equals(binding.getAssociationHash(), association.objectHash())
                && Objects.equals(binding.getSubmissionAuditEventId(), event.eventId())
                && Objects.equals(binding.getSubmissionAuditEventHash(), event.eventHash())
                && Objects.equals(binding.getSubmitIdempotencyKey(), key)
                && Objects.equals(binding.getMaterialSourceRowId(), row.getId())
                && Objects.equals(binding.getMaterialCreationEventId(), creation.eventId())
                && Objects.equals(binding.getMaterialCreationEventHash(), creation.eventHash()), "SUBMISSION_ASSOCIATION_MISMATCH");
        JsonNode payload = strict(binding.getAssociationPayloadJson());
        var encoded = MesEdhrSourceCreationCodec.encodeSubmissionMaterialAssociationV1(payload);
        require(encoded.canonicalJson().equals(binding.getAssociationPayloadJson()) && encoded.sha256().equals(binding.getAssociationHash()),
                "SUBMISSION_ASSOCIATION_PAYLOAD_MISMATCH");
        String actualKey = "RELEASE_PREP:" + sha(JsonUtils.toJsonString(List.of("mes.market-release.submit", releaseId, key)));
        eq(actualKey, c.idempotencyKey());
    }

    private void relatedManaged(VerifiedAuditEventFact event, MesProEdhrMaterialGateReceiptDO row,
            VerifiedAuditEventFact creation, long releaseId, int version) {
        var c = event.columns(); JsonNode before = strict(c.beforeStateJson()), after = strict(c.afterStateJson());
        keys(before, Set.of("codecVersion", "priorReleaseStateRaw")); keys(after, Set.of("codecVersion", "associationPayload", "associationHash"));
        eqNode(before, "codecVersion", TextNode.valueOf("MES-OC-JSON-V1")); eqNode(after, "codecVersion", TextNode.valueOf("MES-OC-JSON-V1"));
        require(c.beforeStateJson().equals(canonicalRelated(before)) && c.afterStateJson().equals(canonicalRelated(after)), "RELATED_CANONICAL_BYTES_MISMATCH");
        JsonNode p = after.get("associationPayload"); keys(p, MANAGED_KEYS);
        for (String field : MANAGED_LONGS) dedicatedInteger(p.get(field), Long.MAX_VALUE, false);
        for (String field : List.of("releaseTransactionVersion", "materialVersion")) dedicatedInteger(p.get(field), Integer.MAX_VALUE, false);
        for (String field : List.of("sourceExpectedApplicationVersion", "initializedApplicationVersion")) dedicatedInteger(p.get(field), Integer.MAX_VALUE, true);
        for (String field : MANAGED_HASHES) hashValue(text(p.get(field)));
        for (String field : MANAGED_STRINGS) text(p.get(field));
        eqNode(p, "codecVersion", TextNode.valueOf("MES-OC-JSON-V1"));
        eqNode(p, "associationProtocol", TextNode.valueOf("MANAGED_HANDOFF_MATERIAL_ASSOCIATION_V1"));
        eqNode(p, "associationVersion", IntNode.valueOf(1)); uuid(text(p.get("associationId")));
        String method = text(p.get("sourceMethod"));
        require(Set.of("MANAGED_PQC_APPROVE", "MANAGED_REPORT_COMPLETE").contains(method), "RELATED_STATE_PROTOCOL_INVALID");
        dedicatedEquals(p, "tenantId", row.getTenantId()); dedicatedEquals(p, "batchExecutionId", row.getBatchExecutionId());
        dedicatedEquals(p, "releaseTransactionId", releaseId); dedicatedEquals(p, "releaseTransactionVersion", version);
        dedicatedEquals(p, "sourceActorId", c.actorId()); dedicatedEquals(p, "materialSourceRowId", row.getId());
        dedicatedEquals(p, "materialVersion", row.getVersion()); dedicatedEquals(p, "materialCreationEventId", creation.eventId());
        eqNode(p, "materialReceiptId", TextNode.valueOf(row.getReceiptId())); eqNode(p, "materialReceiptHash", TextNode.valueOf(row.getReceiptHash()));
        eqNode(p, "materialManifestHash", TextNode.valueOf(row.getManifestHash())); eqNode(p, "sourceSnapshotHash", TextNode.valueOf(row.getSourceSnapshotHash()));
        eqNode(p, "materialCreationEventHash", TextNode.valueOf(creation.eventHash()));
        eqNode(p, "businessReadinessSnapshotHash", TextNode.valueOf(sha(text(p.get("businessReadinessSnapshotJsonRaw")))));
        String associationHash = sha(canonicalRelated(p)); eqNode(after, "associationHash", TextNode.valueOf(associationHash));
        String mh1 = managedIdentity(row.getTenantId().toString(), p.get("applicationId").toString(), method,
                p.get("sourceWorkTaskId").toString(), p.get("sourceExpectedApplicationVersion").toString(), text(p.get("sourceIdempotencyKey")));
        eq(mh1, c.requestId()); eq(mh1, c.idempotencyKey());
        require("PENDING_APPROVAL".equals(c.afterState()) && Integer.toString(version).equals(c.afterObjectVersion())
                && "正式上游证据移交管理者审批".equals(c.reason()) && "MES_MANAGER_STAGE_INITIALIZE".equals(c.reasonCode())
                && "SYSTEM".equals(c.reasonSource()), "RELATED_STATE_PROTOCOL_INVALID");
        boolean promotion = !"ABSENT".equals(c.beforeState());
        if (!promotion) {
            eq(null, c.beforeObjectVersion()); require(before.get("priorReleaseStateRaw").isNull(), "RELATED_STATE_PROTOCOL_INVALID");
        } else {
            require(Set.of("PRECHECK_REQUIRED", "PRECHECK_FAILED", "PRECHECK_PASSED").contains(c.beforeState()), "RELATED_STATE_PROTOCOL_INVALID");
            long oldVersion = decimal(c.beforeObjectVersion(), false);
            require(oldVersion < Integer.MAX_VALUE && oldVersion + 1 == version, "RELATED_STATE_PROTOCOL_INVALID");
            JsonNode prior = strict(text(before.get("priorReleaseStateRaw"))); keys(prior, Set.of("transaction", "checkItems"));
            legacyTransaction(prior.get("transaction"), releaseId, row.getBatchExecutionId(), (int) oldVersion, c.beforeState());
            legacyChecks(prior.get("checkItems"), releaseId);
        }
        String id = text(p.get("associationId"));
        exactRelations(event, List.of(new GxpAuditRelation("SUBJECT", "RELEASE_TRANSACTION", Long.toString(releaseId), Integer.toString(version), null),
                new GxpAuditRelation("SOURCE", "RELEASE_APPLICATION", p.get("applicationId").toString(), p.get("initializedApplicationVersion").toString(), text(p.get("reportSnapshotHash"))),
                new GxpAuditRelation("SOURCE", "WORK_TASK", p.get("sourceWorkTaskId").toString(), null, text(p.get("sourcePayloadHash"))),
                new GxpAuditRelation("SOURCE", "MANAGER_RELEASE_WORK_TASK", p.get("managerWorkTaskId").toString(), null, text(p.get("managerCandidateSnapshotHash"))),
                anchor("EDHR_MATERIAL_GATE_RECEIPT", row.getId(), row.getVersion().toString(), row.getReceiptHash()),
                new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", creation.eventId().toString(), null, creation.eventHash()),
                new GxpAuditRelation("SOURCE", "MANAGED_HANDOFF_MATERIAL_ASSOCIATION", id, "1", associationHash)));
        exactEvidence(event, List.of(new GxpAuditEvidence("MANAGED_HANDOFF_MATERIAL_ASSOCIATION", id, "1", associationHash, "MANAGER_HANDOFF")));
        // Legacy prior capture carries the same unproved historical deployment profile.
        var associations = managedAssociations.selectByKey(row.getTenantId(), mh1);
        require(associations != null && associations.size() == 1, "HANDOFF_SOURCE_REQUEST_UNPROVEN");
        var stored = associations.get(0);
        require(Objects.equals(stored.getHandoffAuditEventId(), event.eventId()) && Objects.equals(stored.getHandoffAuditEventHash(), event.eventHash())
                && Objects.equals(stored.getAssociationHash(), associationHash) && Objects.equals(stored.getAssociationId(), id)
                && Objects.equals(stored.getAssociationPayloadJson(), canonicalRelated(p)), "HANDOFF_ASSOCIATION_MISMATCH");
        JsonNode request = strict(stored.getHandoffRequestJson());
        require(MesEdhrSourceCreationCodec.encodeDedicatedObject(request).sha256().equals(text(p.get("handoffRequestHash"))),
                "HANDOFF_SOURCE_REQUEST_UNPROVEN");
        // handoffRequestHash lacks reportEvidenceOwnerHashes; precheckSnapshotHash lacks
        // raw precheck in this payload. Neither can certify upstream/current readiness.
    }

    private static final Set<String> MANAGED_LONGS = Set.of("tenantId", "applicationId", "batchExecutionId", "releaseTransactionId",
            "managerWorkTaskId", "sourceActorId", "sourceWorkTaskId", "materialSourceRowId", "materialCreationEventId");
    private static final Set<String> MANAGED_HASHES = Set.of("sourcePayloadHash", "handoffRequestHash", "businessReadinessSnapshotHash",
            "precheckSnapshotHash", "materialReceiptHash", "materialManifestHash", "sourceSnapshotHash", "materialCreationEventHash");
    private static final Set<String> MANAGED_STRINGS = Set.of("sourceIdempotencyKey", "reportSnapshotHash", "businessReadinessSnapshotJsonRaw",
            "managerCandidateSnapshotHash", "materialReceiptId", "sourceMethod");
    private static final Set<String> MANAGED_KEYS = Set.of("codecVersion", "associationProtocol", "associationId", "associationVersion",
            "tenantId", "applicationId", "batchExecutionId", "releaseTransactionId", "releaseTransactionVersion", "managerWorkTaskId",
            "sourceActorId", "sourceWorkTaskId", "sourceExpectedApplicationVersion", "initializedApplicationVersion", "sourceMethod",
            "sourceIdempotencyKey", "sourcePayloadHash", "handoffRequestHash", "reportSnapshotHash", "businessReadinessSnapshotJsonRaw",
            "businessReadinessSnapshotHash", "managerCandidateSnapshotHash", "precheckSnapshotHash", "materialSourceRowId", "materialReceiptId",
            "materialVersion", "materialReceiptHash", "materialManifestHash", "sourceSnapshotHash", "materialCreationEventId", "materialCreationEventHash");

    private static long dedicatedInteger(JsonNode n, long max, boolean zero) {
        require(n != null && n.isIntegralNumber() && n.canConvertToLong() && n.longValue() >= (zero ? 0 : 1)
                && n.longValue() <= max, "RELATED_STATE_PROTOCOL_INVALID"); return n.longValue();
    }
    private static void dedicatedEquals(JsonNode p, String key, long value) {
        require(dedicatedInteger(p.get(key), Long.MAX_VALUE, true) == value, "RELATED_STATE_PROTOCOL_INVALID");
    }
    private static long decimal(String raw, boolean positive) {
        require(raw != null && raw.matches(positive ? "[1-9][0-9]*" : "0|[1-9][0-9]*"), "RELATED_STATE_PROTOCOL_INVALID");
        try { return Long.parseLong(raw); } catch (NumberFormatException e) { throw new IllegalStateException("RELATED_STATE_PROTOCOL_INVALID", e); }
    }
    private static long legacyLong(JsonNode n) {
        require(n != null && (n.isIntegralNumber() || n.isTextual()), "RELATED_STATE_PROTOCOL_INVALID");
        String raw = n.isTextual() ? n.textValue() : n.toString();
        require(raw.matches("0|-?[1-9][0-9]*"), "RELATED_STATE_PROTOCOL_INVALID");
        final long value;
        try { value = Long.parseLong(raw); } catch (NumberFormatException e) { throw new IllegalStateException("RELATED_STATE_PROTOCOL_INVALID", e); }
        boolean quoted = value <= -9007199254740991L || value >= 9007199254740991L;
        require(n.isTextual() == quoted, "RELATED_STATE_PROTOCOL_INVALID"); return value;
    }
    private static void hashValue(String value) {
        require(value != null && value.matches("[0-9a-f]{64}"), "RELATED_STATE_PROTOCOL_INVALID");
    }
    private static void uuid(String value) {
        require(value != null && value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), "RELATED_STATE_PROTOCOL_INVALID");
    }
    private static GxpAuditRelation onlyRelation(VerifiedAuditEventFact event, String type) {
        var found = event.relations().stream().filter(r -> type.equals(r.objectType())).toList();
        require(found.size() == 1 && "SOURCE".equals(found.get(0).relationType()), "SOURCE_RELATION_MISMATCH"); return found.get(0);
    }

    private static String managedIdentity(String... tuple) {
        require(tuple.length == 6, "RELATED_STATE_PROTOCOL_INVALID");
        StringBuilder value = new StringBuilder("MES-OC-ID-V1\0MANAGED_HANDOFF\0" + "6:");
        for (String part : tuple) { text(TextNode.valueOf(part)); value.append(strictUtf8(part).length).append(':').append(part); }
        return "MH1:" + digestRelated(strictUtf8(value.toString()));
    }
    private static byte[] strictUtf8(String value) {
        try {
            var buffer = StandardCharsets.UTF_8.newEncoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(value));
            byte[] result = new byte[buffer.remaining()]; buffer.get(result); return result;
        } catch (java.nio.charset.CharacterCodingException e) { throw new IllegalStateException("RELATED_STATE_PROTOCOL_INVALID", e); }
    }
    private static String digestRelated(byte[] value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("RELATED_STATE_PROTOCOL_INVALID", e); }
    }
    private static String canonicalRelated(JsonNode n) {
        require(n != null, "RELATED_STATE_PROTOCOL_INVALID"); StringBuilder out = new StringBuilder(); canonicalRelated(n, out); return out.toString();
    }
    private static void canonicalRelated(JsonNode n, StringBuilder out) {
        if (n.isObject()) {
            TreeSet<String> names = new TreeSet<>(); n.fieldNames().forEachRemaining(name -> {
                require(name.chars().allMatch(c -> c >= 0x20 && c < 0x7f), "RELATED_STATE_PROTOCOL_INVALID"); names.add(name);
            });
            out.append('{'); boolean comma = false;
            for (String name : names) { if (comma) out.append(','); comma = true; quoted(name, out); out.append(':'); canonicalRelated(n.get(name), out); }
            out.append('}');
        } else if (n.isTextual()) quoted(n.textValue(), out);
        else if (n.isNull()) out.append("null");
        else if (n.isIntegralNumber() && n.canConvertToLong()) out.append(n.longValue());
        else throw fail("RELATED_STATE_PROTOCOL_INVALID");
    }
    private static void quoted(String value, StringBuilder out) {
        strictUtf8(value); out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b"); case '\t' -> out.append("\\t"); case '\n' -> out.append("\\n");
                case '\f' -> out.append("\\f"); case '\r' -> out.append("\\r");
                default -> { if (ch < 32) out.append(String.format(Locale.ROOT, "\\u00%02X", (int) ch)); else out.append(ch); }
            }
        }
        out.append('"');
    }
    private static final String TRANSACTION_SHAPE = "id:Long|releaseCode:String|batchExecutionId:Long|batchExecutionCode:String|workOrderId:Long|workOrderCode:String|batchCode:String|productId:Long|productCode:String|productName:String|routeId:Long|routeCode:String|routeName:String|dhrStatus:String|inspectionStatus:String|deviationStatus:String|reworkStatus:String|scrapStatus:String|inventoryStatus:String|releaseStatus:String|releaseDecisionId:Long|finalizationPayloadHash:String|requiredCheckCount:Integer|failedCheckCount:Integer|blockingCheckCount:Integer|lastPrecheckAt:LocalDateTime|precheckSnapshotJson:String|submitIdempotencyKey:String|submittedBy:Long|submittedAt:LocalDateTime|approvalIdempotencyKey:String|approvedBy:Long|approvedAt:LocalDateTime|approvalSignoffEvidenceHash:String|approvalSignatureId:Long|approvalOpinion:String|rejectedBy:Long|rejectedAt:LocalDateTime|rejectReason:String|withdrawnBy:Long|withdrawnAt:LocalDateTime|withdrawReason:String|version:Integer|remark:String|createTime:LocalDateTime|updateTime:LocalDateTime|creator:String|updater:String|deleted:Boolean";
    private static final String CHECK_SHAPE = "id:Long|releaseTransactionId:Long|checkCode:String|checkCategory:String|checkName:String|checkResult:String|itemStatus:String|severity:String|responsibilityModule:String|sourceObjectType:String|sourceObjectId:String|sourceObjectCode:String|sourceRecordUrl:String|failureReason:String|remediationSuggestion:String|impactScopeJson:String|evidenceHash:String|checkedAt:LocalDateTime|createTime:LocalDateTime|updateTime:LocalDateTime|creator:String|updater:String|deleted:Boolean";
    private static final String BATCHES_SHAPE = "id:Long|batchExecutionCode:String|workOrderId:Long|batchCode:String|activeContextKey:String|attemptNo:Integer|routeId:Long|routeVersionId:Long|routeSnapshotJson:String|status:Integer|provisioningStatus:String|taskTotal:Integer|taskApprovedCount:Integer|blockedCount:Integer|aggregateHash:String|closeSignatureId:Long|closedBy:Long|closedAt:LocalDateTime";
    private static final String RELEASEAPPLICATIONS_SHAPE = "id:Long|activeOrderId:Long|workOrderId:Long|routeId:Long|routeVersionId:Long|batchExecutionId:Long|releaseTransactionId:Long|releaseApprovalWorkTaskId:Long|pqcReleaseWorkTaskId:Long|pqcDecision:String|pqcDecidedBy:Long|pqcDecidedAt:LocalDateTime|pqcRejectReason:String|applicationStatus:String|sourceSnapshotHash:String|reportSnapshotHash:String|version:Integer|requestIdempotencyKey:String|businessIdempotencyKey:String|blockerSnapshotJson:String|dossierSummaryJson:String|appliedBy:Long|appliedAt:LocalDateTime";
    private static final String RELEASETRANSACTIONS_SHAPE = "id:Long|batchExecutionId:Long|workOrderId:Long|releaseStatus:String|releaseDecisionId:Long|version:Integer|lastPrecheckAt:LocalDateTime|dhrStatus:String|inspectionStatus:String|deviationStatus:String|reworkStatus:String|scrapStatus:String|inventoryStatus:String|requiredCheckCount:Integer|failedCheckCount:Integer|blockingCheckCount:Integer|precheckSnapshotJson:String|finalizationPayloadHash:String|submitIdempotencyKey:String|submittedBy:Long|submittedAt:LocalDateTime|approvalIdempotencyKey:String|approvedBy:Long|approvedAt:LocalDateTime|approvalSignatureId:Long|approvalSignoffEvidenceHash:String|approvalOpinion:String|rejectedBy:Long|rejectedAt:LocalDateTime|rejectReason:String|withdrawnBy:Long|withdrawnAt:LocalDateTime|withdrawReason:String";
    private static final String WORKTASKS_SHAPE = "id:Long|taskCode:String|taskType:String|batchExecutionId:Long|batchTaskId:Long|businessScopeType:String|businessScopeId:Long|executionId:Long|workOrderId:Long|assigneeUserId:Long|candidateSourceType:String|candidateSourceId:Long|candidateUserSnapshot:String|sourceUserId:Long|responsibilitySourceType:String|responsibilitySourceKey:String|responsibilitySourceVersion:String|responsibilitySourceDigest:String|responsibilityScopeJson:String|ownershipLocked:Boolean|ownershipLastTransferredAt:LocalDateTime|ownershipLastTransferredBy:Long|status:String|startedAt:LocalDateTime|completedAt:LocalDateTime|dueTime:LocalDateTime|reason:String|remark:String|actionUrl:String";
    private static final String WORKORDERS_SHAPE = "id:Long|code:String|status:Integer|finishDate:LocalDateTime|releaseDecisionId:Long|releasedBy:Long|releasedAt:LocalDateTime";
    private static final String PRODUCTIONTASKS_SHAPE = "id:Long|workOrderId:Long|status:Integer|finishDate:LocalDateTime|cancelDate:LocalDateTime";

    private static void legacyShape(JsonNode node, String specification) {
        require(node != null && node.isObject(), "RELATED_STATE_PROTOCOL_INVALID");
        Map<String, String> types = new HashMap<>();
        for (String item : specification.split("\\|")) { String[] pair = item.split(":"); types.put(pair[0], pair[1]); }
        node.fields().forEachRemaining(field -> {
            String type = types.get(field.getKey()); JsonNode n = field.getValue();
            require(type != null, "RELATED_STATE_PROTOCOL_INVALID"); if (n.isNull()) return;
            switch (type) {
                case "String" -> require(n.isTextual(), "RELATED_STATE_PROTOCOL_INVALID");
                case "Long" -> legacyLong(n);
                case "Integer" -> require(n.isIntegralNumber() && n.canConvertToInt(), "RELATED_STATE_PROTOCOL_INVALID");
                case "Boolean" -> require(n.isBoolean(), "RELATED_STATE_PROTOCOL_INVALID");
                case "LocalDateTime" -> require(n.isIntegralNumber() && n.canConvertToLong(), "RELATED_STATE_PROTOCOL_INVALID");
                default -> throw fail("RELATED_STATE_PROTOCOL_INVALID");
            }
        });
    }
    private static void legacyTransaction(JsonNode node, long release, long batch, int version, String state) {
        legacyShape(node, TRANSACTION_SHAPE);
        require(legacyLong(node.get("id")) == release && legacyLong(node.get("batchExecutionId")) == batch
                && node.hasNonNull("version") && node.get("version").isIntegralNumber()
                && node.get("version").intValue() == version, "RELATED_STATE_PROTOCOL_INVALID");
        eqNode(node, "releaseStatus", TextNode.valueOf(state));
        if ("PRECHECK_PASSED".equals(state) || "PENDING_APPROVAL".equals(state)) {
            for (String field : List.of("failedCheckCount", "blockingCheckCount")) {
                JsonNode n = node.get(field); require(n == null || n.isNull() || n.intValue() == 0, "RELATED_STATE_PROTOCOL_INVALID");
            }
        }
    }
    private static void legacyChecks(JsonNode checks, long release) {
        require(checks != null && checks.isArray(), "RELATED_STATE_PROTOCOL_INVALID"); long previous = 0;
        for (JsonNode check : checks) {
            legacyShape(check, CHECK_SHAPE); long id = legacyLong(check.get("id"));
            require(id > previous && legacyLong(check.get("releaseTransactionId")) == release, "RELATED_STATE_PROTOCOL_INVALID"); previous = id;
        }
    }
    private static void submissionAffected(JsonNode state, JsonNode transaction, long batch, long release) {
        keys(state, Set.of("batches", "releaseApplications", "releaseTransactions", "workTasks", "workOrders", "productionTasks"));
        Map<String, String> shapes = Map.of("batches", BATCHES_SHAPE, "releaseApplications", RELEASEAPPLICATIONS_SHAPE,
                "releaseTransactions", RELEASETRANSACTIONS_SHAPE, "workTasks", WORKTASKS_SHAPE,
                "workOrders", WORKORDERS_SHAPE, "productionTasks", PRODUCTIONTASKS_SHAPE);
        for (var shape : shapes.entrySet()) {
            JsonNode rows = state.get(shape.getKey()); require(rows.isArray(), "RELATED_STATE_PROTOCOL_INVALID"); long previous = 0;
            for (JsonNode item : rows) {
                legacyShape(item, shape.getValue()); long id = legacyLong(item.get("id"));
                require(id > previous, "RELATED_STATE_PROTOCOL_INVALID"); previous = id;
            }
        }
        for (String field : List.of("releaseApplications", "workOrders", "productionTasks")) require(state.get(field).isEmpty(), "RELATED_STATE_PROTOCOL_INVALID");
        require(state.get("batches").size() == 1 && legacyLong(state.get("batches").get(0).get("id")) == batch
                && state.get("releaseTransactions").size() == 1, "RELATED_STATE_PROTOCOL_INVALID");
        JsonNode projected = state.get("releaseTransactions").get(0);
        require(legacyLong(projected.get("id")) == release && legacyLong(projected.get("batchExecutionId")) == batch, "RELATED_STATE_PROTOCOL_INVALID");
        for (String field : List.of("batchExecutionId", "releaseStatus", "version", "submitIdempotencyKey", "submittedBy", "submittedAt")) {
            eq(transaction.get(field), projected.get(field));
        }
        for (JsonNode task : state.get("workTasks")) {
            JsonNode batchId = task.get("batchExecutionId"), scope = task.get("businessScopeId");
            boolean batchMatch = batchId != null && !batchId.isNull() && legacyLong(batchId) == batch;
            boolean releaseMatch = scope != null && !scope.isNull() && legacyLong(scope) == release
                    && "RELEASE_TRANSACTION".equals(task.path("businessScopeType").asText());
            require(batchMatch || releaseMatch, "RELATED_STATE_PROTOCOL_INVALID");
        }
    }
    private static void header(VerifiedAuditEventFact event, String subject, String version, Long actor,
            String before, String after, String beforeVersion, String afterVersion, String reason, String code, String source, String key, String request) {
        var c = event.columns(); eq(subject, c.subjectId()); eq(version, c.subjectVersion());
        require(Objects.equals(actor, c.actorId()), "SOURCE_ACTOR_MISMATCH");
        eq(before, c.beforeState()); eq(after, c.afterState()); eq(beforeVersion, c.beforeObjectVersion()); eq(afterVersion, c.afterObjectVersion());
        eq(reason, c.reason()); eq(code, c.reasonCode()); eq(source, c.reasonSource()); eq(key, c.idempotencyKey()); eq(request, c.requestId());
    }
    private static void completionHeader(VerifiedAuditEventFact event, MesProcessPoolActiveOrderCompletionReceiptDO row, Integer part) {
        String suffix = part == null ? "" : ":ROWS:" + part;
        header(event, "ACTIVE_ORDER:" + row.getActiveOrderId(), row.getCompletedVersion().toString(), row.getCompletedBy(), "ACTIVE", "COMPLETED",
                row.getExpectedVersion().toString(), row.getCompletedVersion().toString(),
                part == null ? "活跃订单已完成正式回填并生成完工回执" : "活跃订单完工受影响明细完整分段记录",
                "MES_ACTIVE_ORDER_COMPLETE", "SYSTEM", "ACTIVE_ORDER_COMPLETE:" + row.getId() + suffix, "MES-ACTIVE-ORDER-COMPLETE:" + row.getId() + suffix);
    }
    private static void verifyParts(MesProcessPoolActiveOrderCompletionReceiptDO row, VerifiedAuditEventFact main,
            List<VerifiedAuditEventFact> parts, JsonNode manifest) {
        keys(manifest, Set.of("schema", "canonicalization", "beforeHash", "afterHash", "tables", "beforeRowCount", "afterRowCount", "parts"));
        eqNode(manifest, "schema", TextNode.valueOf("COMPLETION_AFFECTED_ROWS_V1")); eqNode(manifest, "canonicalization", TextNode.valueOf("MES_COMPLETION_SOURCE_V1"));
        require(manifest.get("tables").isArray() && manifest.get("parts").isArray() && !manifest.get("parts").isEmpty()
                && parts.size() == manifest.get("parts").size(), "COMPLETION_PART_MANIFEST_MISMATCH");
        List<String> tables = new ArrayList<>();
        for (JsonNode table : manifest.get("tables")) { String name = text(table); require(!tables.contains(name), "COMPLETION_PART_MANIFEST_MISMATCH"); tables.add(name); }
        Map<Long, VerifiedAuditEventFact> events = new HashMap<>();
        for (var part : parts) require(events.put(part.eventId(), part) == null, "COMPLETION_PART_MANIFEST_MISMATCH");
        ObjectNode allBefore = JsonNodeFactory.instance.objectNode(), allAfter = JsonNodeFactory.instance.objectNode();
        for (String table : tables) { allBefore.putArray(table); allAfter.putArray(table); }
        Set<Long> used = new HashSet<>(); long beforeCount = 0, afterCount = 0, lastLedger = 0; int index = 0;
        for (JsonNode item : manifest.get("parts")) {
            keys(item, Set.of("partIndex", "eventId", "ledgerSequence", "eventHash", "beforeRowCount", "afterRowCount"));
            eqNode(item, "partIndex", IntNode.valueOf(index)); long eventId = businessLongValue(item.get("eventId"), true);
            var part = events.get(eventId); require(part != null && used.add(eventId), "COMPLETION_PART_MANIFEST_MISMATCH");
            businessLong(item.get("ledgerSequence"), part.columns().ledgerSequence()); eqNode(item, "eventHash", TextNode.valueOf(part.eventHash()));
            require(part.columns().ledgerSequence() > lastLedger && part.columns().ledgerSequence() < main.columns().ledgerSequence()
                    && Objects.equals(part.columns().transactionId(), main.columns().transactionId()), "COMPLETION_PART_MANIFEST_MISMATCH");
            lastLedger = part.columns().ledgerSequence(); completionHeader(part, row, index);
            JsonNode b = strict(part.columns().beforeStateJson()), a = strict(part.columns().afterStateJson());
            long bc = partRows(b, row, index, tables, allBefore), ac = partRows(a, row, index, tables, allAfter);
            businessLong(item.get("beforeRowCount"), bc); businessLong(item.get("afterRowCount"), ac);
            try { beforeCount = Math.addExact(beforeCount, bc); afterCount = Math.addExact(afterCount, ac); }
            catch (ArithmeticException e) { throw new IllegalStateException("COMPLETION_PART_MANIFEST_MISMATCH", e); }
            exactRelations(part, completionRelations(row)); exactEvidence(part, completionEvidence(row)); index++;
        }
        require(used.equals(events.keySet()), "COMPLETION_PART_MANIFEST_MISMATCH");
        businessLong(manifest.get("beforeRowCount"), beforeCount); businessLong(manifest.get("afterRowCount"), afterCount);
        eqNode(manifest, "beforeHash", TextNode.valueOf(sha(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(allBefore.toString()))));
        eqNode(manifest, "afterHash", TextNode.valueOf(sha(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(allAfter.toString()))));
    }
    private static long partRows(JsonNode state, MesProcessPoolActiveOrderCompletionReceiptDO row, int index, List<String> tables, ObjectNode all) {
        keys(state, Set.of("schema", "partIndex", "completionReceiptId", "completionReceiptHash", "affectedRows"));
        eqNode(state, "schema", TextNode.valueOf("COMPLETION_AFFECTED_ROWS_V1")); eqNode(state, "partIndex", IntNode.valueOf(index));
        businessLong(state.get("completionReceiptId"), row.getId()); eqNode(state, "completionReceiptHash", TextNode.valueOf(row.getReceiptHash()));
        JsonNode affected = state.get("affectedRows"); keys(affected, new HashSet<>(tables));
        List<String> actual = new ArrayList<>(); affected.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(tables), "COMPLETION_PART_MANIFEST_MISMATCH"); long count = 0;
        for (String table : tables) {
            JsonNode rows = affected.get(table); require(rows.isArray(), "COMPLETION_PART_MANIFEST_MISMATCH");
            for (JsonNode value : rows) { require(value.isObject(), "COMPLETION_PART_MANIFEST_MISMATCH"); ((ArrayNode) all.get(table)).add(value); count++; }
        }
        return count;
    }
    private static List<GxpAuditRelation> completionRelations(MesProcessPoolActiveOrderCompletionReceiptDO row) {
        return List.of(new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER", row.getActiveOrderId().toString(), row.getCompletedVersion().toString(), null),
                anchor("COMPLETION_RECEIPT", row.getId(), row.getCompletedVersion().toString(), row.getReceiptHash()));
    }
    private static List<GxpAuditEvidence> completionEvidence(MesProcessPoolActiveOrderCompletionReceiptDO row) {
        return List.of(new GxpAuditEvidence("FORMAL_COMPLETION_RECEIPT", row.getId().toString(), row.getCompletedVersion().toString(), row.getReceiptHash(), "COMPLETION"));
    }
    private static void businessLong(JsonNode node, long expected) {
        require(node != null && node.equals(strict(JsonUtils.toJsonString(Long.valueOf(expected)))), "COMPLETION_PART_MANIFEST_MISMATCH");
    }
    private static long businessLongValue(JsonNode node, boolean positive) {
        require(node != null && (node.isIntegralNumber() || node.isTextual()), "COMPLETION_PART_MANIFEST_MISMATCH");
        String raw = node.isTextual() ? node.textValue() : node.toString();
        require(raw.matches("0|[1-9][0-9]*"), "COMPLETION_PART_MANIFEST_MISMATCH");
        try { long value = Long.parseLong(raw); require(!positive || value > 0, "COMPLETION_PART_MANIFEST_MISMATCH"); businessLong(node, value); return value; }
        catch (NumberFormatException e) { throw new IllegalStateException("COMPLETION_PART_MANIFEST_MISMATCH", e); }
    }
    private static void exactRelations(VerifiedAuditEventFact e, List<GxpAuditRelation> expected) {
        require(e.relations().size() == expected.size() && new HashSet<>(e.relations()).equals(new HashSet<>(expected)), "SOURCE_RELATION_MISMATCH");
    }
    private static void exactEvidence(VerifiedAuditEventFact e, List<GxpAuditEvidence> expected) {
        require(e.evidences().size() == expected.size() && new HashSet<>(e.evidences()).equals(new HashSet<>(expected)), "SOURCE_RELATION_MISMATCH");
    }
    private static GxpAuditRelation anchor(String target, Long row, String version, String hash) { return new GxpAuditRelation("SOURCE", target, row.toString(), version, hash); }
    private static MesEdhrSourceCreationEvidence proof(String protocol, Long row, String receipt, String version, String hash, VerifiedAuditEventFact event) {
        return new MesEdhrSourceCreationEvidence(protocol, row, receipt, version, hash, event.eventId(), event.eventHash());
    }
    private static void source(Long actualTenant, Long id, Long tenant) { require(Objects.equals(actualTenant, tenant), "SOURCE_OWNER_INTEGRITY_FAILED"); positiveStored(id); }
    private static void positiveStored(Number value) { require(value != null && value.longValue() > 0, "SOURCE_OWNER_INTEGRITY_FAILED"); }
    private static void eq(Object expected, Object actual) { require(Objects.equals(expected, actual), "SOURCE_OWNER_INTEGRITY_FAILED"); }
    private static void eqNode(JsonNode object, String field, JsonNode expected) { eq(expected, object.get(field)); }
    private static byte[] bytes(String raw) { require(raw != null, "SOURCE_OWNER_INTEGRITY_FAILED"); return raw.getBytes(StandardCharsets.UTF_8); }
    private static String second(LocalDateTime time) {
        require(time != null, "SOURCE_OWNER_INTEGRITY_FAILED");
        try { return MesEdhrSourceCreationCodec.formatSecond(time); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("SOURCE_OWNER_INTEGRITY_FAILED", e); }
    }
}
