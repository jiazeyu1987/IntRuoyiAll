package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.Set;

@Component
public final class EdhrReleaseContentSemanticValidator {
    private static final Set<String> CHECKS = Set.of("DHR_COMPLETENESS", "DOSSIER_INCOMING_INSPECTION_REPORT",
            "DOSSIER_STERILIZATION_REPORT", "DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT", "DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD",
            "INSPECTION_RESULT", "DEVIATION_CLOSED", "REWORK_CLOSED", "SCRAP_RECORDED", "INVENTORY_CONSISTENCY");
    private final EdhrReleaseContentPolicy policy;
    public EdhrReleaseContentSemanticValidator(EdhrReleaseContentPolicy policy) { this.policy = policy; }
    public void validate(JsonNode root) {
        policy.validate(root);
        JsonNode identity = root.get("identity"), task = root.get("workTask"), release = root.get("release"), finalization = root.get("finalization");
        equal(identity.get("sourceTaskId"), task.get("id"), "task identity");
        equal(identity.get("domainSubjectId"), release.get("id"), "release identity");
        equal(task.get("businessScopeId"), release.get("id"), "task release");
        equal(task.get("batchExecutionId"), release.at("/batch/id"), "task batch");
        equal(task.get("workOrderId"), release.at("/batch/workOrderId"), "task order");
        equal(task.get("assignmentBranch"), finalization.get("authorizationBranch"), "assignment branch");
        equal(identity.get("tenantId"), finalization.at("/origin/tenantId"), "origin tenant");
        equal(task.get("batchExecutionId"), finalization.at("/origin/batchExecutionId"), "origin batch");
        equal(task.get("workOrderId"), finalization.at("/origin/workOrderId"), "origin order");
        equal(finalization.get("sourceSnapshotHash"), finalization.at("/origin/sourceSnapshotHash"), "origin hash");
        equal(finalization.get("sourceSnapshotHash"), finalization.at("/trace/sourceSnapshotHash"), "trace hash");
        boolean managed = "MANAGED".equals(task.path("assignmentBranch").asText());
        if (managed == finalization.path("applicationId").isNull()) fail("application branch");
        long prior = 0; var candidate = task.get("candidate"); StringBuilder snapshot = new StringBuilder();
        for (var id : candidate.get("userIds")) { long current = Long.parseLong(id.textValue()); if (current <= prior) fail("candidate sorting"); prior = current; if (!snapshot.isEmpty()) snapshot.append(','); snapshot.append(id.textValue()); }
        if (!snapshot.toString().equals(candidate.get("userSnapshot").textValue())) fail("candidate snapshot");
        boolean activeFormal=Set.of("approval-content-active-order-v1", "approval-content-active-order-v2").contains(root.path("contentPolicyVersion").asText());
        Set<String> expectedChecks=activeFormal?Set.of("DHR_COMPLETENESS","INSPECTION_RESULT","DEVIATION_CLOSED","REWORK_CLOSED","SCRAP_RECORDED","INVENTORY_CONSISTENCY"):CHECKS;
        int required = 0, failed = 0, blocking = 0; var codes = new HashSet<String>(); String previous = null;
        for (var item : release.at("/precheckSnapshot/items")) {
            String code = item.get("code").textValue(); if (!codes.add(code) || previous != null && previous.compareTo(code) >= 0) fail("check ordering"); previous = code;
            String result = item.get("result").textValue(); required++; if ("FAIL".equals(result) || "BLOCKER".equals(result)) failed++; if ("BLOCKER".equals(result)) blocking++;
            var ref = item.get("evidenceRefs").get(0); equal(ref.get("releaseTransactionId"), release.get("id"), "check release");
        }
        if (!codes.equals(expectedChecks) || required != release.at("/checkCount/required").intValue()
                || failed > release.at("/checkCount/failed").intValue() || blocking > release.at("/checkCount/blocking").intValue()) fail("check count");
        boolean active = "ACTIVE_ORDER".equals(finalization.path("sourceKind").asText());
        var completion = finalization.get("completionBackfillReceipt"); var independent = finalization.get("independentPrerequisiteReceipt");
        if (active) {
            if (completion.isNull() || !independent.isNull()) fail("completion source branch");
            equal(completion.get("receiptId"), finalization.at("/origin/completionBackfillReceiptId"), "completion receipt");
            equal(completion.get("receiptHash"), finalization.at("/origin/completionBackfillReceiptHash"), "completion hash");
            equal(completion.get("tenantId"), identity.get("tenantId"), "completion tenant");
            var expectedVersion = completion.get("expectedActiveOrderVersion");
            if (!expectedVersion.isNull()) {
                try { if (Integer.parseInt(expectedVersion.textValue()) < 0) fail("completion expected version"); }
                catch (NumberFormatException e) { fail("completion expected version range"); }
            }
            requireRaw(completion.get("formalSourceSnapshotJsonRaw"), false); requireRaw(completion.get("signatureSnapshotJsonRaw"), false); requireRaw(completion.get("lossConditionFactsJsonRaw"), true);
            boolean loss = completion.path("hasActualLoss").asBoolean(); BigDecimal quantity = new BigDecimal(completion.path("lossQuantity").asText());
            if (loss) { if (quantity.signum() <= 0 || completion.get("lossRecordId").isNull() || !"SUCCESS".equals(completion.path("lossReportStatus").asText()) || !completion.get("zeroLossConfirmationSnapshotRaw").isNull()) fail("actual loss"); }
            else { if (quantity.signum() != 0 || !completion.get("lossRecordId").isNull() || !"NOT_REQUIRED".equals(completion.path("lossReportStatus").asText())) fail("zero loss"); requireRaw(completion.get("zeroLossConfirmationSnapshotRaw"), false); }
        } else {
            if (!completion.isNull() || independent.isNull()) fail("independent source branch");
            equal(independent.get("receiptId"), finalization.at("/origin/sourceCredentialId"), "independent receipt");
            equal(independent.get("receiptHash"), finalization.at("/origin/sourceCredentialHash"), "independent hash");
            equal(independent.get("tenantId"), identity.get("tenantId"), "independent tenant");
            validateEvidence(independent.get("sourceEvidence"));
        }
        if(activeFormal) {
            var facts=finalization.get("activeOrderFormalFacts");
            if(!managed || !active || "approval-content-active-order-v1".equals(root.path("contentPolicyVersion").asText()) && !finalization.path("materialManifest").isNull()) fail("active-order formal branch");
            equal(facts.get("applicationId"),finalization.get("applicationId"),"formal application");
            equal(facts.get("batchExecutionId"),task.get("batchExecutionId"),"formal batch");
            equal(facts.get("sourceSnapshotHash"),finalization.get("sourceSnapshotHash"),"formal source");
            equal(facts.get("completionReceiptId"),completion.get("receiptId"),"formal completion");
            equal(facts.get("completionReceiptHash"),completion.get("receiptHash"),"formal completion hash");
        } else {
        var materials = finalization.get("materialManifest"); equal(materials.get("batchExecutionId"), task.get("batchExecutionId"), "material batch");
        equal(materials.get("sourceSnapshotHash"), finalization.get("sourceSnapshotHash"), "material source");
        var types = new HashSet<String>(); for (var material : materials.get("materials")) { if (!types.add(material.get("type").textValue())) fail("duplicate material type"); equal(material.get("batchExecutionId"), task.get("batchExecutionId"), "attachment batch"); }
        if (types.size() != 4) fail("four materials required");
        }
        if ("approval-content-active-order-v2".equals(root.path("contentPolicyVersion").asText())) {
            var manifest = finalization.get("materialManifest");
            equal(manifest.get("tenantId"), identity.get("tenantId"), "dossier tenant");
            equal(manifest.get("activeOrderId"), finalization.at("/origin/activeOrderId"), "dossier order");
            equal(manifest.get("applicationId"), finalization.get("applicationId"), "dossier application");
            equal(manifest.get("batchExecutionId"), task.get("batchExecutionId"), "dossier batch");
            var unsigned = (ObjectNode) manifest.deepCopy(); unsigned.remove("manifestHash");
            if (!ApprovalContentJson.hash(unsigned).equals(manifest.path("manifestHash").asText())) fail("dossier manifest hash");
            long previousAttachment = 0;
            for (var file : manifest.get("files")) {
                long attachment = Long.parseLong(file.path("attachmentId").asText());
                if (attachment <= previousAttachment) fail("dossier file ordering"); previousAttachment = attachment;
                equal(file.get("activeOrderId"), manifest.get("activeOrderId"), "dossier file order");
                if (!file.get("applicationId").isNull()) equal(file.get("applicationId"), manifest.get("applicationId"), "dossier file application");
            }
        }
        dates(root);
    }
    public String subjectVersion(JsonNode root, JsonNode batchGuard) {
        policy.validateBatchGuard(batchGuard);
        validate(root); ObjectNode input = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        input.put("tokenFormatVersion", "EDHR_SUBJECT_V1"); input.set("contentPolicyVersion", root.get("contentPolicyVersion")); input.set("scopeCode", root.get("scopeCode")); input.set("identity", root.get("identity"));
        input.set("releaseVersion", root.at("/release/version")); input.put("workTaskHash", ApprovalContentJson.hash(root.get("workTask"))); input.put("responsibilityHash", ApprovalContentJson.hash(root.at("/workTask/responsibility")));
        input.put("precheckHash", ApprovalContentJson.hash(root.at("/release/precheckSnapshot"))); input.put("finalizationHash", ApprovalContentJson.hash(root.get("finalization"))); input.set("batchGuard", batchGuard); input.put("previewContentHash", ApprovalContentJson.hash(root));
        return "EDHR1:" + ApprovalContentJson.hash(input);
    }
    private static void validateEvidence(JsonNode evidence) {
        if (!evidence.isArray() || evidence.isEmpty()) fail("independent evidence missing"); String previous = null;
        for (var item : evidence) { String key = item.path("sourceType").asText() + '\u0000' + item.path("sourceId").asText(); if (previous != null && previous.compareTo(key) >= 0) fail("evidence ordering"); previous = key;
            for (var value : item) if (!value.isTextual() || value.textValue().isBlank()) fail("evidence value"); }
    }
    private static void dates(JsonNode node) {
        if (node.isObject()) { var it = node.fields(); while (it.hasNext()) { var field = it.next(); if (field.getValue().isTextual() && Set.of("createdAt", "issuedAt", "expiresAt", "revokedAt").contains(field.getKey())) {
            try { OffsetDateTime.parse(field.getValue().textValue(), DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX").withResolverStyle(ResolverStyle.STRICT)); } catch (java.time.DateTimeException e) { fail("invalid calendar"); }
        } dates(field.getValue()); } } else if (node.isArray()) node.forEach(EdhrReleaseContentSemanticValidator::dates);
    }
    private static void requireRaw(JsonNode value, boolean array) { if (!value.isTextual()) fail("raw snapshot missing"); var parsed = ApprovalContentJson.parse(value.textValue()); if (array ? !parsed.isArray() : !parsed.isObject()) fail("raw snapshot root"); }
    private static void equal(JsonNode a, JsonNode b, String name) { if (a == null || b == null || !a.equals(b)) fail(name); }
    private static void fail(String reason) { throw new IllegalArgumentException("EDHR_CONTENT_RELATION_INVALID: " + reason); }
}
